package com.jigmjugm.challenge.controller;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.dto.ChallengeCreateRequest;
import com.jigmjugm.challenge.dto.ChallengeCreateResponse;
import com.jigmjugm.challenge.dto.ChallengeListItemView;
import com.jigmjugm.challenge.service.ChallengeDiscoverService;
import com.jigmjugm.challenge.service.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {
    private final ChallengeService challengeService;
    private final ChallengeDiscoverService challengeDiscoverService;
    // TODO: 실제 사용자 아이디로 수정
    private Long mockUserId() { return 1L; }

    @PostMapping
    @Operation(summary = "챌린지 생성")
    public ResponseEntity<ChallengeCreateResponse> create(@Valid @RequestBody ChallengeCreateRequest req) {
        ChallengeCreateResponse res = challengeService.create(mockUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/{challengeId}")
    @Operation(summary = "챌린지 상세 조회")
    public ResponseEntity<Map<String,Object>> get(@PathVariable Long challengeId) {
        Challenge ch = challengeService.getDetail(challengeId);
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("challengeId", ch.getChallengeId());
        body.put("title", ch.getTitle());
        body.put("categoryType", ch.getCategoryType());
        body.put("frequencyType", ch.getFrequencyType());
        body.put("startDate", ch.getStartDate());
        body.put("endDate", ch.getEndDate());
        body.put("perRoundAmount", ch.getPerRoundAmount());
        body.put("goalAmount", ch.getGoalAmount());
        body.put("rounds", ch.getRounds().stream()
                .sorted(Comparator.comparing(ChallengeRound::getRoundNo))
                .map(r -> Map.of(
                        "roundId", r.getRoundId(),
                        "roundNo", r.getRoundNo(),
                        "scheduledDate", r.getScheduledDate()
                )).toList());
        return ResponseEntity.ok(body);
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
                ChallengeDiscoverService.discover(categoryType, status, sort, page, size);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        return ResponseEntity.ok(body);
    }

}

