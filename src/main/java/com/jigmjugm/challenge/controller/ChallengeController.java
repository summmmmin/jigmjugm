package com.jigmjugm.challenge.controller;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.dto.ChallengeCreateRequest;
import com.jigmjugm.challenge.dto.ChallengeCreateResponse;
import com.jigmjugm.challenge.dto.ChallengeDetailResponse;
import com.jigmjugm.challenge.dto.ChallengeListItemView;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.service.ChallengeDiscoverService;
import com.jigmjugm.challenge.service.ChallengeService;
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

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {
    private final ChallengeService challengeService;
    private final ChallengeDiscoverService challengeDiscoverService;
    private final ChallengeParticipationRepository challengeParticipationRepository;
    private final ChallengePolicy policy;

    @PostMapping
    @Operation(summary = "챌린지 생성")
    public ResponseEntity<ChallengeCreateResponse> create(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ChallengeCreateRequest req) {
        ChallengeCreateResponse res = challengeService.create(principal.getUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/{challengeId}")
    @Operation(summary = "챌린지 상세 조회")
    public ResponseEntity<Map<String,Object>> get(@PathVariable Long challengeId) {
        Challenge challenge = challengeService.getDetail(challengeId);
        String status = policy.computeStatus(challenge.getStartDate(), challenge.getEndDate(),
                java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
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
                // TODO: 집계데이터
//                .participantCount()
//                .avgCertRate()
//                .myParticipation()
//                .myNextScheduledDate()
//                .myStats()
//                .recentRankings()
                .build();
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body((Map<String, Object>) body);
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

}

