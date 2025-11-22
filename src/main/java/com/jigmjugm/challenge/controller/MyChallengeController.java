package com.jigmjugm.challenge.controller;

import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.service.ChallengeDiscoverService;
import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MyChallengeController {

    private final ChallengeDiscoverService challengeDiscoverService;
    private final ChallengeParticipationRepository challengeParticipationRepository;

    @Operation(
            summary = "나의 챌린지 목록 조회",
            description = """
        내가 참여한 챌린지 목록을 조회합니다.
        
        정렬 옵션:
        - startDate,asc  : 시작일 가까운 순
        - createdAt,desc : 최신 생성순
        - avgCertRate,desc : 나의 인증률 높은 순
        - totalAmount,desc : 누적 저금액 많은 순
        
        카테고리 옵션:
        - ALL, SAVING, INSTALLMENT, OTHER, MY_CREATED
        """
    )
    @GetMapping("/me/challenges")
    public ResponseEntity<Map<String,Object>> myChallenges(
            @AuthenticationPrincipal UserPrincipal principal,

            @Parameter(
                    description = "카테고리 필터 (ALL, SAVING, INSTALLMENT, OTHER, MY_CREATED)",
                    example = "ALL"
            )
            @RequestParam(defaultValue="ALL") String categoryType,

            @Parameter(
                    description = "챌린지 상태 필터 (PENDING, ACTIVE, COMPLETED)",
                    example = "ACTIVE"
            )
            @RequestParam(required=false) String status,

            @Parameter(
                    description = "탈퇴한 참여 포함 여부 (true=포함, false=제외)",
                    example = "false"
            )
            @RequestParam(defaultValue="false") boolean includeWithdrawn,

            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue="0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue="20") int size,

            @Parameter(
                    description = """
                정렬 기준.
                - startDate,asc (기본)
                - createdAt,desc
                - avgCertRate,desc
                - totalAmount,desc
                """,
                    example = "startDate,asc"
            )
            @RequestParam(required=false) String sort
    ) {
        if (principal == null) {
            throw new BusinessException(ApiErrorCode.UNAUTHORIZED);
        }
        var result = challengeDiscoverService.myChallenges(
                principal.getUserId(), categoryType, status, includeWithdrawn, sort, page, size);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", result.getContent());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        return ResponseEntity.ok(body);
    }

}
