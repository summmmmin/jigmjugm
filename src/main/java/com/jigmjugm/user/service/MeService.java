package com.jigmjugm.user.service;

import com.jigmjugm.certification.repo.CertificationRepository;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.user.dto.UserProfileResponse;
import com.jigmjugm.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserAccountRepository userAccountRepository;
    private final ChallengeParticipationRepository challengeParticipationRepository;
    private final ChallengeRepository challengeRepository;
    private final CertificationRepository certificationRepository;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public UserProfileResponse getMyProfile(Long userId) {
        var user = userAccountRepository.findById(userId).orElseThrow();
        var today = LocalDate.now(SEOUL);

        long challengeCount = challengeParticipationRepository
                .countMyActiveOrUpcoming(userId, today);

        long completedChallengeCount = challengeParticipationRepository
                .countMyCompleted(userId, today);

        long createdChallengeCount = challengeRepository
                .countByCreatorUserIdAndIsDeletedFalse(userId);

        // ====== 누적 저금 통계 ======

        // 기간별
        LocalDate thisWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisYearStart = today.withDayOfYear(1);

        long thisWeekSaving = sumSavingByDateRange(userId, thisWeekStart, today);
        long thisMonthSaving = sumSavingByDateRange(userId, thisMonthStart, today);
        long thisYearSaving = sumSavingByDateRange(userId, thisYearStart, today);

        // 최근 5주
        List<Long> recentWeeklySavingAmounts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate weekStart = thisWeekStart.minusWeeks(i);
            LocalDate weekEnd;
            if (i == 0) {
                // 이번 주: 월 ~ 오늘
                weekEnd = today;
            } else {
                // 지난 주들: 월 ~ 일
                weekEnd = weekStart.plusDays(6);
            }
            long amount = sumSavingByDateRange(userId, weekStart, weekEnd);
            recentWeeklySavingAmounts.add(amount);
        }

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .challengeCount(challengeCount)
                .completedChallengeCount(completedChallengeCount)
                .createdChallengeCount(createdChallengeCount)
                .thisYearSavingAmount(thisYearSaving)
                .thisMonthSavingAmount(thisMonthSaving)
                .thisWeekSavingAmount(thisWeekSaving)
                .recentWeeklySavingAmounts(recentWeeklySavingAmounts)
                .build();
    }

    private long sumSavingByDateRange(Long userId, LocalDate startDate, LocalDate endDateInclusive) {
        // 서울 날짜 범위를 UTC OffsetDateTime으로 변환
        ZonedDateTime startZdtSeoul = startDate.atStartOfDay(SEOUL);
        OffsetDateTime startUtc = startZdtSeoul.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();

        ZonedDateTime endExclusiveZdtSeoul = endDateInclusive.plusDays(1).atStartOfDay(SEOUL);
        OffsetDateTime endUtcExclusive = endExclusiveZdtSeoul.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();

        Long value = certificationRepository
                .sumAmountByUserAndCertifiedAtBetween(userId, startUtc, endUtcExclusive);

        return value != null ? value : 0L;
    }
}

