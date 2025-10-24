package com.jigmjugm.challenge.service;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.dto.ChallengeCreateRequest;
import com.jigmjugm.challenge.dto.ChallengeCreateResponse;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import com.jigmjugm.common.util.ChallengePolicy;
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
    public ChallengeCreateResponse create(Long creatorUserId, ChallengeCreateRequest req) {
        validateDates(req.getStartDate(), req.getEndDate());
        int weeklyMask = "WEEKLY".equalsIgnoreCase(req.getFrequencyType())
                ? policy.toWeeklyMask(req.getWeeklyDays())
                : 0;

        Challenge ch = new Challenge();
        ch.setCreatorUserId(creatorUserId);
        ch.setTitle(normalize(req.getTitle()));
        ch.setDescription(req.getDescription());
        ch.setCategoryType(req.getCategoryType());
        ch.setFrequencyType(req.getFrequencyType());
        ch.setWeeklyDaysMask(weeklyMask);
        ch.setStartDate(req.getStartDate());
        ch.setEndDate(req.getEndDate());
        ch.setPerRoundAmount(req.getPerRoundAmount());
        ch.setGoalAmount(req.getGoalAmount());
        ch.setThumbnailUrl(req.getThumbnailUrl());

        Challenge saved = challengeRepo.save(ch);

        // 회차 생성
        List<LocalDate> schedule = policy.buildSchedule(
                saved.getFrequencyType(), saved.getWeeklyDaysMask(),
                saved.getStartDate(), saved.getEndDate());

        int idx = 1;
        for (LocalDate d : schedule) {
            ChallengeRound r = new ChallengeRound();
            r.setChallenge(saved);
            r.setRoundNo(idx++);
            r.setScheduledDate(d);
            r.setBaseAmount(saved.getPerRoundAmount());
            saved.getRounds().add(r);
        }

        String status = policy.computeStatus(saved.getStartDate(), saved.getEndDate(), LocalDate.now(ZoneId.of("Asia/Seoul")));
        return new ChallengeCreateResponse(saved.getChallengeId(), schedule.size(), status);
    }

    @Transactional(readOnly = true)
    public Challenge getDetail(Long challengeId) {
        return challengeRepo.findById(challengeId)
                .orElseThrow(() -> new NoSuchElementException("challenge not found"));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("기간이 올바르지 않습니다.");
        }
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) throw new IllegalArgumentException("제목은 비어있을 수 없습니다.");
        return t;
    }
}

