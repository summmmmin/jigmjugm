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
    @Getter @AllArgsConstructor
    public static class RecentRanking { Integer weekIndex; LocalDate periodStart; LocalDate periodEnd; /* top/me 생략 */ }
    @Getter @AllArgsConstructor
    public static class Permissions { boolean canEdit; boolean canRebuildRounds; boolean canDelete; }
}

