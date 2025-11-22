package com.jigmjugm.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long userId;
    private String nickname;
    private String provider;
    private LocalDateTime createdAt;
    
    // 나의 챌린지 현황 정보
    private long challengeCount;
    private long completedChallengeCount;
    private long createdChallengeCount;

    private List<Long> recentWeeklySavingAmounts;

    private long thisYearSavingAmount;
    private long thisMonthSavingAmount;
    private long thisWeekSavingAmount;
}