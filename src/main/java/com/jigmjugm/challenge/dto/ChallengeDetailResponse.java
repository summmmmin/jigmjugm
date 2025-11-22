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
@Schema(name = "ChallengeDetailResponse", description = "챌린지 상세 조회 응답")
public class ChallengeDetailResponse {
    @Schema(description = "챌린지 아이디")
    private Long challengeId;

    @Schema(description = "챌린지 제목")
    private String title;

    @Schema(description = "챌린지 상세 설명")
    private String description;

    @Schema(description = "챌린지 카테고리 (SAVING/INSTALLMENT/OTHER)")
    private String categoryType;

    @Schema(description = "인증 주기 타입 (DAILY, WEEKLY)")
    private String frequencyType;

    @Schema(description = "WEEKLY일때 요일 목록 (예: [\"MON\",\"WED\"])")
    private List<String> weeklyDays;

    @Schema(type="integer", format="int64", description = "회차당 인증 금액")
    private Long perRoundAmount;

    @Schema(type="integer", format="int64", description = "챌린지 목표 금액")
    private Long goalAmount;

    @Schema(description = "챌린지 시작일")
    private LocalDate startDate;

    @Schema(description = "챌린지 종료일")
    private LocalDate endDate;

    @Schema(description = "챌린지 생성일시")
    private OffsetDateTime createdAt;

    @Schema(description = "썸네일URL(S3)")
    private String thumbnailUrl;

    @Schema(description = "챌린지 생성자 정보")
    private Creator creator;

    @Schema(description = "현재 참여 중인 인원 수")
    private Integer participantCount;

    @Schema(description = "평균 인증률")
    private Long avgCertRate;

    @Schema(description = "평균누적저금액 (모든 참여자의 total_amount 평균)")
    private Long avgTotalAmount;

    @Schema(description = "총 회차 수")
    private Integer totalRounds;

    @Schema(description = "상태")
    private String status;

    @Schema(description = "나의 참여정보")
    private MyParticipation myParticipation;

    @Schema(description = "나의 다음 인증일(인증여부 상관없이 현재부터 가장 가까운 인증일(현재날짜포함))")
    private LocalDate myNextScheduledDate;

    @Schema(description = "나의 다음 인증 회차아이디 (인증여부 상관없이 현재로부터 가장 가까운 회차(현재날짜포함))")
    private Long myNextRoundId;

    @Schema(description = "myNextRoundId를 인증했는지안했는지 true/false")
    private boolean certifiedForNextRound;

    @Schema(description = "내 통계")
    private MyStats myStats;

    @Schema(description = "n주간 인증률 순위")
    private List<RecentRanking> recentRankings;

    @Schema(description = "권한")
    private Permissions permissions;

    @Getter @AllArgsConstructor
    public static class Creator { Long userId; String nickname; }

    @Getter @AllArgsConstructor
    @Schema(description = "나의 챌린지 참여 정보")
    public static class MyParticipation {
        @Schema(description = "참여아이디")
        Long participationId;

        @Schema(description = "참여상태 (ACTIVE: 참여 중, LEFT: 중도 탈퇴)")
        String status;

        @Schema(description = "참여 일시")
        OffsetDateTime joinedAt;

        @Schema(description = "탈퇴 일시 (탈퇴 전이면 null)")
        OffsetDateTime leftAt;

        @Schema(description = "인증횟수")
        Integer myCertifiedCount;

        @Schema(description = "인증률")
        Long myCertRate;

        @Schema(description = "총 인증 금액")
        Long myTotalAmount;
    }

    @Getter @AllArgsConstructor
    public static class MyStats {
        @Schema(description = "승인된 인증 횟수")
        Integer approvedCount;

        @Schema(description = "제출 상태 인증 횟수 (사용안함)")
        Integer submittedCount;

        @Schema(description = "거절된 인증 횟수 (사용안함)")
        Integer rejectedCount;
    }

    @Getter @Builder
    @Schema(description = "구간 랭킹 정보")
    public static class RecentRanking {
        @Schema(description = "기준 주차 인덱스 (1=최근 1주, 2=최근 2주, ...)")
        private Integer weekIndex;
        @Schema(description = "랭킹 집계 시작일")
        private LocalDate periodStart;
        @Schema(description = "랭킹 집계 종료일")
        private LocalDate periodEnd;
        @Schema(description = "상위3명 랭킹 정보")
        private List<TopUser> top; // 최대 3명
        @Schema(description = "나의 랭킹 (참여/로그인 안 되어 있으면 null)")
        private Me me;             // 나의 순위/인증률 또는 null

        @Getter @Builder
        @Schema(description = "상위인증률순위정보")
        public static class TopUser {
            @Schema(description = "순위")
            private Integer rank;
            @Schema(description = "사용자아이디")
            private Long userId;
            @Schema(description = "닉네임")
            private String nickname;
            @Schema(description = "인증률")
            private Long certRate;
        }

        @Getter @Builder
        @Schema(description = "나의 순위 정보")
        public static class Me {
            @Schema(description = "순위")
            private Integer rank;
            @Schema(description = "인증률")
            private Long certRate;
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

