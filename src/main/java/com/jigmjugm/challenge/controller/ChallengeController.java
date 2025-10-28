package com.jigmjugm.challenge.controller;

import com.jigmjugm.certification.repo.CertificationRepository;
import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.dto.*;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import com.jigmjugm.challenge.service.ChallengeDiscoverService;
import com.jigmjugm.challenge.service.ChallengeService;
import com.jigmjugm.challenge.service.ChallengeStatsService;
import com.jigmjugm.common.util.ChallengePolicy;
import com.jigmjugm.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {
    private final ChallengeService challengeService;
    private final ChallengeDiscoverService challengeDiscoverService;
    private final ChallengeParticipationRepository challengeParticipationRepository;
    private final ChallengeStatsService challengeStatsService;
    private final ChallengePolicy policy;
    private final ChallengeRoundRepository challengeRoundRepository;
    private final CertificationRepository certificationRepository;

    @PostMapping
    @Operation(summary = "챌린지 생성")
    public ResponseEntity<ChallengeCreateResponse> create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ChallengeCreateRequest req) {
        ChallengeCreateResponse res = challengeService.create(principal.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/{challengeId}")
    @Operation(summary = "챌린지 상세 조회")
    public ResponseEntity<ChallengeDetailResponse> get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long challengeId) {
        Challenge challenge = challengeService.getDetail(challengeId);

        String status = policy.computeStatus(challenge.getStartDate(), challenge.getEndDate(),
                LocalDate.now(ZoneId.of("Asia/Seoul")));

        var stats = challengeStatsService.stats(challengeId);
        var recent = challengeStatsService.recentRanking(challengeId, principal);

        ChallengeDetailResponse.MyParticipation myParticipation = null;
        LocalDate myNextScheduledDate = null;
        ChallengeDetailResponse.MyStats myStats = null;

        boolean canEdit = false, canRebuildRounds = false, canDelete = false;

        if (principal != null) {
            var myOpt = challengeParticipationRepository
                    .findFirstByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(challengeId, principal.getUserId());
            if (myOpt.isPresent()) {
                var p = myOpt.get();

                // 내 승인/제출/거절 카운트
                long approved = certificationRepository.countByParticipation_ParticipationIdAndCertificationStatus(p.getParticipationId(),"APPROVED");
                long submitted = certificationRepository.countByParticipation_ParticipationIdAndCertificationStatus(p.getParticipationId(),"SUBMITTED");
                long rejected = certificationRepository.countByParticipation_ParticipationIdAndCertificationStatus(p.getParticipationId(),"REJECTED");

                // 내 인증률 = (시작~오늘 사이 예정 회차 수 대비 승인 수)
                int scheduled = challengeRoundRepository.countByChallenge_ChallengeIdAndScheduledDateBetween(
                        challengeId, challenge.getStartDate(), java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
                double myRate = (scheduled > 0) ? ((double) approved / (double) scheduled) : 0.0;

                myParticipation = new ChallengeDetailResponse.MyParticipation(
                        p.getParticipationId(),
                        "ACTIVE",
                        p.getJoinedAt(),
                        p.getLeftAt(),
                        (int) approved,
                        myRate
                );
                myStats = new ChallengeDetailResponse.MyStats((int) approved, (int) submitted, (int) rejected);

                var nextOpt = challengeRoundRepository
                        .findFirstByChallenge_ChallengeIdAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(
                                challengeId, LocalDate.now(ZoneId.of("Asia/Seoul")));
                myNextScheduledDate = nextOpt.map(ChallengeRound::getScheduledDate).orElse(null);

                canEdit = "OWNER".equals(p.getRoleType());
                canRebuildRounds = canEdit;
                canDelete = canEdit;
            }
        }

        var body = ChallengeDetailResponse.builder()
                .challengeId(challenge.getChallengeId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .categoryType(challenge.getCategoryType())
                .frequencyType(challenge.getFrequencyType())
                .perRoundAmount(challenge.getPerRoundAmount())
                .goalAmount(challenge.getGoalAmount())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .createdAt(challenge.getCreatedAt())
                .thumbnailUrl(challenge.getThumbnailUrl())
                .totalRounds(challenge.getRounds().size())
                .status(status)
                .participantCount(stats.totalParticipants())
                .avgCertRate(stats.avgCertRate())
                .myParticipation(myParticipation)          // 참여 안했거나 비로그인이면 null
                .myNextScheduledDate(myNextScheduledDate)  // 참여 안했거나 비로그인이면 null
                .myStats(myStats)                          // 참여 안했거나 비로그인이면 null
                .recentRankings(recent == null? List.of()
                        : List.of(ChallengeDetailResponse.RecentRanking.from(recent)))
                .permissions(new ChallengeDetailResponse.Permissions(canEdit, canRebuildRounds, canDelete))
                .build();

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(body);
    }

    @GetMapping("/discover")
    @Operation(summary = "챌린지 목록 조회(전체)")
    public ResponseEntity<Map<String, Object>> discover(
            @RequestParam(defaultValue = "ALL") String categoryType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "startDate,asc") String sort
    ) {
        Page<ChallengeListItemView> result =
                challengeDiscoverService.discover(categoryType, status, sort, page, size);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/check-title")
    @Operation(summary = "챌린지 제목 중복 체크")
    public ResponseEntity<Map<String,Object>> checkTitle(
            @RequestParam String title,
            @RequestParam(required = false) Long excludeId
    ) {
        ChallengeService.TitleCheckResult result = challengeService.check(title, excludeId);
        return ResponseEntity.ok(Map.of(
                "available", result.available(),
                "normalizedTitle", result.normalizedTitle()
        ));
    }

    @PatchMapping("/{challengeId}")
    @Operation(summary = "챌린지 수정")
    public ResponseEntity<Map<String,Object>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long challengeId,
            @Valid @RequestBody ChallengeCreateRequest req) {
        boolean updated = challengeService.update(challengeId, principal.getUserId(), req);
        return ResponseEntity.ok(Map.of("challengeId", challengeId, "updated", updated));
    }

    @DeleteMapping("/{challengeId}")
    @Operation(summary = "챌린지 삭제")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long challengeId) {
        challengeService.softDelete(challengeId, principal.getUserId());
    }

    @GetMapping("/{challengeId}/stats")
    @Operation(summary = "챌린지 통계")
    public ResponseEntity<StatsResponse> stats(
            @PathVariable Long challengeId
    ) {
        var body = challengeStatsService.stats(challengeId);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{challengeId}/ranking")
    @io.swagger.v3.oas.annotations.Operation(summary = "기간별 인증률 랭킹")
    public ResponseEntity<RankingResponse> ranking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long challengeId,
            @RequestParam(required = false, defaultValue = "28") Integer periodDays,
            @RequestParam(required = false, defaultValue = "3") Integer limit
    ) {
        var body = challengeStatsService.ranking(challengeId, periodDays, limit, principal);
        return ResponseEntity.ok(body);
    }

}

