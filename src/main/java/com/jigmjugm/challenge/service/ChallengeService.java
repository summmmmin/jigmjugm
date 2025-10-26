package com.jigmjugm.challenge.service;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.dto.ChallengeCreateRequest;
import com.jigmjugm.challenge.dto.ChallengeCreateResponse;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import com.jigmjugm.common.util.ChallengePolicy;
import com.jigmjugm.common.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ChallengeService {
    private final ChallengeRepository challengeRepo;
    private final ChallengeRoundRepository roundRepo;
    private final ChallengePolicy policy;

    @Transactional
    public ChallengeCreateResponse create(Long creatorUserId, ChallengeCreateRequest challengeCreateRequest) {
        validateBusiness(challengeCreateRequest);
        int weeklyMask = "WEEKLY".equalsIgnoreCase(challengeCreateRequest.getFrequencyType())
                ? policy.toWeeklyMask(challengeCreateRequest.getWeeklyDays())
                : 0;

        // 중복제목 확인
        String normalizedTitle = com.jigmjugm.common.util.TextNormalizer.normalizeTitle(challengeCreateRequest.getTitle());
        if (challengeRepo.existsnormalizedTitle(normalizedTitle)) {
            //throw new DuplicateTitleException("중복되는 챌린지명입니다.");
        }

        Challenge challenge = new Challenge();
        challenge.setCreatorUserId(creatorUserId);
        challenge.setTitle(challengeCreateRequest.getTitle());
        challenge.setDescription(challengeCreateRequest.getDescription());
        challenge.setCategoryType(challengeCreateRequest.getCategoryType());
        challenge.setFrequencyType(challengeCreateRequest.getFrequencyType());
        challenge.setWeeklyDaysMask(weeklyMask);
        challenge.setStartDate(challengeCreateRequest.getStartDate());
        challenge.setEndDate(challengeCreateRequest.getEndDate());
        challenge.setPerRoundAmount(challengeCreateRequest.getPerRoundAmount());
        challenge.setGoalAmount(challengeCreateRequest.getGoalAmount());
        challenge.setThumbnailUrl(challengeCreateRequest.getThumbnailUrl());

        Challenge saved = challengeRepo.save(challenge);

        // 회차 생성
        List<LocalDate> schedule = policy.buildSchedule(
                saved.getFrequencyType(), saved.getWeeklyDaysMask(),
                saved.getStartDate(), saved.getEndDate());
        makeRounds(saved, schedule);

        String status = policy.computeStatus(saved.getStartDate(), saved.getEndDate(), LocalDate.now(ZoneId.of("Asia/Seoul")));
        return new ChallengeCreateResponse(saved.getChallengeId(), schedule.size(), status);
    }

    @Transactional(readOnly = true)
    public Challenge getDetail(Long challengeId) {
        Challenge challenge = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new NoSuchElementException("challenge not found"));
        if (Boolean.TRUE.equals(challenge.getIsDeleted())) {
            throw new NoSuchElementException("challenge not found");
        }
        return challenge;
    }

    @Transactional(readOnly = true)
    public TitleCheckResult check(String title, Long excludeId) {
        String normalizedTitle = TextNormalizer.normalizeTitle(title);
        boolean duplicate = (excludeId == null)
                ? challengeRepo.existsnormalizedTitle(normalizedTitle)
                : challengeRepo.existsNormalizedTitleExceptId(normalizedTitle, excludeId);
        return new TitleCheckResult(!duplicate, normalizedTitle);
    }
    public record TitleCheckResult(boolean available, String normalizedTitle) {}

    private void validateBusiness(ChallengeCreateRequest req) {
        if (!req.getStartDate().isBefore(req.getEndDate())) {
            throw new IllegalArgumentException("시작일은 종료일 이전이어야 합니다.");
        }
        if (req.getGoalAmount() <= req.getPerRoundAmount()) {
            throw new IllegalArgumentException("목표금액은 회차금액보다 커야 합니다.");
        }
        if ("WEEKLY".equalsIgnoreCase(req.getFrequencyType())) {
            if (req.getWeeklyDays() == null || req.getWeeklyDays().isEmpty()) {
                throw new IllegalArgumentException("WEEKLY의 경우 요일(weeklyDays)은 필수입니다.");
            }
        }
    }

    private void makeRounds(Challenge saved, List<LocalDate> schedule) {
        int idx = 1;
        for (LocalDate localDate : schedule) {
            var challengeRound = new ChallengeRound();
            challengeRound.setChallenge(saved);
            challengeRound.setRoundNo(idx++);
            challengeRound.setScheduledDate(localDate);
            challengeRound.setBaseAmount(saved.getPerRoundAmount());
            saved.getRounds().add(challengeRound);
        }
    }
}

