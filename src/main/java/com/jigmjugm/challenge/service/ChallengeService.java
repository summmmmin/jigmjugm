package com.jigmjugm.challenge.service;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.domain.ChallengeParticipation;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.dto.ChallengeCreateRequest;
import com.jigmjugm.challenge.dto.ChallengeCreateResponse;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.common.util.ChallengePolicy;
import com.jigmjugm.common.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChallengeService {
    private final ChallengeRepository challengeRepo;
    private final ChallengeRoundRepository roundRepo;
    private final ChallengeParticipationRepository participationRepo;
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
            throw new BusinessException(ApiErrorCode.DUPLICATE_TITLE, "중복되는 챌린지명입니다.");
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
        makeRoundsFrom(saved, schedule, 1);

        var owner = ChallengeParticipation.builder()
                .challenge(saved)
                .userId(creatorUserId)
                .roleType("OWNER")
                .joinedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        participationRepo.save(owner);

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

    @Transactional
    public boolean update(Long challengeId, Long userId, ChallengeCreateRequest challengeCreateRequest) {
        validateBusiness(challengeCreateRequest);
        Challenge challenge = getDetail(challengeId);
        if (!challenge.getCreatorUserId().equals(userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "권한이 없습니다.");
        }
        String normalizedTitle = TextNormalizer.normalizeTitle(challengeCreateRequest.getTitle());
        boolean dup = challengeRepo.existsNormalizedTitleExceptId(normalizedTitle, challengeId);
        if (dup) throw new BusinessException(ApiErrorCode.DUPLICATE_TITLE, "중복되는 챌린지명입니다.");

        challenge.setTitle(challengeCreateRequest.getTitle().trim().replaceAll("\\s+", " "));
        challenge.setDescription(challengeCreateRequest.getDescription());
        challenge.setCategoryType(challengeCreateRequest.getCategoryType());
        challenge.setFrequencyType(challengeCreateRequest.getFrequencyType());
        challenge.setWeeklyDaysMask("WEEKLY".equalsIgnoreCase(challengeCreateRequest.getFrequencyType())
                ? policy.toWeeklyMask(challengeCreateRequest.getWeeklyDays())
                : 0);
        challenge.setStartDate(challengeCreateRequest.getStartDate());
        challenge.setEndDate(challengeCreateRequest.getEndDate());
        challenge.setPerRoundAmount(challengeCreateRequest.getPerRoundAmount());
        challenge.setGoalAmount(challengeCreateRequest.getGoalAmount());
        challenge.setThumbnailUrl(challengeCreateRequest.getThumbnailUrl());

        // 회차 재생성
        // === 미래 회차만 갱신 ===
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 1) 미래 회차 선삭제 (DB)
        roundRepo.deleteFutureRounds(challengeId, today);

        // 2) 영속 컬렉션에서도 제거 (정합성 유지)
        challenge.getRounds().removeIf(r -> r.getScheduledDate().isAfter(today));

        // 3) 현재까지의 최댓 roundNo 조회
        int baseRoundNo = roundRepo.findMaxRoundNoUpTo(challengeId, today);

        // 4) 새 스케줄 생성 후 미래 날짜만 필터
        List<LocalDate> raw = policy.buildSchedule(
                challenge.getFrequencyType(),
                challenge.getWeeklyDaysMask(),
                challenge.getStartDate(),
                challenge.getEndDate()
        );

        List<LocalDate> future = raw.stream()
                .filter(d -> d.isAfter(today))
                .distinct()
                .sorted()
                .toList();

        // 5) 미래 회차 재생성(번호 이어붙이기)
        makeRoundsFrom(challenge, future, baseRoundNo + 1);
        return true;
    }

    @Transactional
    public void softDelete(Long challengeId, Long userId) {
        Challenge challenge = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.NOT_FOUND, "챌린지를 찾을 수 없습니다."));

        if (!Objects.equals(challenge.getCreatorUserId(), userId)) {
            throw new BusinessException(ApiErrorCode.FORBIDDEN, "권한이 없습니다.");
        }

        challenge.setIsDeleted(true); // 변경감지 → UPDATE 발생
    }

    private void validateBusiness(ChallengeCreateRequest req) {
        if (!req.getStartDate().isBefore(req.getEndDate())) {
            throw new BusinessException(ApiErrorCode.INVALID_DATE_RANGE, "시작일은 종료일 이전이어야 합니다.");
        }
        if (req.getGoalAmount() < req.getPerRoundAmount()) {
            throw new BusinessException(ApiErrorCode.INVALID_AMOUNT, "목표금액은 회차금액보다 커야 합니다.");
        }
        if ("WEEKLY".equalsIgnoreCase(req.getFrequencyType())) {
            if (req.getWeeklyDays() == null || req.getWeeklyDays().isEmpty()) {
                throw new BusinessException(ApiErrorCode.INVALID_INPUT_VALUE, "WEEKLY의 경우 요일(weeklyDays)은 필수입니다.");
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

    // idx포함해서 수정할때도 지난건두고 이후만 수정할수있도록
    private void makeRoundsFrom(Challenge challenge, List<LocalDate> schedule, int startRoundNo) {
        int idx = startRoundNo;
        for (LocalDate date : schedule) {
            ChallengeRound round = new ChallengeRound();
            round.setChallenge(challenge);
            round.setRoundNo(idx++);
            round.setScheduledDate(date);
            round.setBaseAmount(challenge.getPerRoundAmount());
            challenge.getRounds().add(round);
        }
    }
}

