package com.jigmjugm.home.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class HomeResponse {
    private List<ChallengeListItem> startSoon;
    private List<ChallengeListItem> newest;
    private List<ChallengeListItem> active;
    private List<MyUpcomingItem> myUpcoming;

    @Getter @Builder
    public static class ChallengeListItem {
        private Long challengeId;
        private String title;
        private String categoryType;      // SAVING/INSTALLMENT/OTHER
        private String frequencyType;     // DAILY/WEEKLY
        private LocalDate startDate;
        private LocalDate endDate;
        private OffsetDateTime createdAt;
        private String thumbnailUrl;
        private Integer participantCount; // 활성 참여자
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Double avgCertRate;       // active 섹션: 최근 기간 기준 인증률
        private String status;            // PENDING/ACTIVE/COMPLETED
        private Long perRoundAmount;
        private String creatorNickname;
    }

    @Getter @Builder
    public static class MyUpcomingItem {
        private Long challengeId;
        private String title;
        private String status;
        private Integer nextRoundNo;
        private LocalDate nextScheduledDate;
        private Integer remainingRounds;
        private Long perRoundAmount;
        private String thumbnailUrl;
    }
}
