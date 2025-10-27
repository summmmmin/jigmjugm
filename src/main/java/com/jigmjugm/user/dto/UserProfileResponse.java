package com.jigmjugm.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long userId;
    private String nickname;
    private String provider;
    private LocalDateTime createdAt;
    
    // 추가 정보 (향후 구현 예정)
    private Integer challengeCount;
    private Integer completedChallengeCount;
    private Integer points;
    private Integer level;
}