package com.jigmjugm.challenge.controller;

import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.service.ChallengeDiscoverService;
import com.jigmjugm.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MyChallengeController {

    private final ChallengeDiscoverService challengeDiscoverService;
    private final ChallengeParticipationRepository challengeParticipationRepository;

    // 내가 참여한 챌린지 목록
    @GetMapping("/me/challenges")
    public ResponseEntity<Map<String,Object>> myChallenges(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue="ALL") String categoryType,
            @RequestParam(required=false) String status,
            @RequestParam(defaultValue="false") boolean includeWithdrawn,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String sort
    ) {
        var pageable = challengeDiscoverService.buildPageable(sort, page, size); // 기존 메서드 public으로
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var result = challengeParticipationRepository.findMyChallenges(
                principal.getUserId(), challengeDiscoverService.normalizeCategory(categoryType),
                challengeDiscoverService.normalizeStatus(status), today, includeWithdrawn, pageable);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        return ResponseEntity.ok(body);
    }
}
