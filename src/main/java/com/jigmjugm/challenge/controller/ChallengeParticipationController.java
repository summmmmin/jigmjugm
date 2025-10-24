package com.jigmjugm.challenge.controller;

import com.jigmjugm.challenge.dto.ParticipationResponse;
import com.jigmjugm.challenge.service.ChallengeParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChallengeParticipationController {

    private final ChallengeParticipationService participationService;

    // TODO: 실제 사용자아이디로
    private Long mockUserId() { return 1L; }

    // 참여하기
    @PostMapping("/challenges/{challengeId}/participants")
    @Operation(summary = "챌린지 참여하기")
    public ResponseEntity<ParticipationResponse> join(@PathVariable Long challengeId) {
        ParticipationResponse res = participationService.joinChallenge(mockUserId(), challengeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // 나가기
    @DeleteMapping("/participants/{participationId}")
    @Operation(summary = "챌린지 나가기")
    public ResponseEntity<Void> leave(@PathVariable Long participationId) {
        participationService.leaveChallenge(mockUserId(), participationId);
        return ResponseEntity.noContent().build();
    }

    // 참여자 목록
    @GetMapping("/challenges/{challengeId}/participants")
    @Operation(summary = "참여자 목록 조회")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable Long challengeId) {
        return ResponseEntity.ok(participationService.getParticipants(challengeId));
    }
}

