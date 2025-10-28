package com.jigmjugm.challenge.dto;

// 패키지: com.jigmjugm.challenge.dto

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class ChallengeDetailResponse {
    private Long challengeId;
    private String title;
    private String description;
    private String categoryType;
    private String frequencyType;
    @Schema(type="integer", format="int64") private Long perRoundAmount;
    @Schema(type="integer", format="int64") private Long goalAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private OffsetDateTime createdAt;
    private String thumbnailUrl;
    private Creator creator;
    private Integer participantCount;
    private Double avgCertRate;
    private Integer totalRounds;
    private String status;
    private MyParticipation myParticipation;
    private LocalDate myNextScheduledDate;
    private MyStats myStats;
    private List<RecentRanking> recentRankings;
    private Permissions permissions;

    @Getter @AllArgsConstructor
    public static class Creator { Long userId; String nickname; }
    @Getter @AllArgsConstructor
    public static class MyParticipation { Long participationId; String status; OffsetDateTime joinedAt; OffsetDateTime leftAt; Integer myCertifiedCount; Double myCertRate; }
    @Getter @AllArgsConstructor
    public static class MyStats { Integer approvedCount; Integer submittedCount; Integer rejectedCount; }
    @Getter @Builder
    public static class RecentRanking {
        private Integer weekIndex;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private List<TopUser> top; // 최대 3명
        private Me me;             // 나의 순위/인증률 또는 null

        @Getter @Builder
        public static class TopUser {
            private Integer rank;
            private Long userId;
            private String nickname;
            private Double certRate;
        }

        @Getter @Builder
        public static class Me {
            private Integer rank;
            private Double certRate;
        }

        public static RecentRanking from(RecentRankingItem src) {
            return RecentRanking.builder()
                    .weekIndex(src.weekIndex())
                    .periodStart(src.periodStart())
                    .periodEnd(src.periodEnd())
                    .top(src.top() == null ? java.util.List.of()
                            : src.top().stream()
                            .map(t -> TopUser.builder()
                                    .rank(t.rank())
                                    .userId(t.userId())
                                    .nickname(t.nickname())
                                    .certRate(t.certRate())
                                    .build())
                            .toList())
                    .me(src.me() == null ? null
                            : Me.builder()
                            .rank(src.me().rank())
                            .certRate(src.me().certRate())
                            .build())
                    .build();
        }
    }
    @Getter @AllArgsConstructor
    public static class Permissions { boolean canEdit; boolean canRebuildRounds; boolean canDelete; }
}

