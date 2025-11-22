package com.jigmjugm.user.controller;

import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.security.UserPrincipal;
import com.jigmjugm.user.dto.NicknameResponse;
import com.jigmjugm.user.dto.UserProfileResponse;
import com.jigmjugm.user.repository.UserAccountRepository;
import com.jigmjugm.user.service.MeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MeController {

    private final UserAccountRepository userAccountRepository;
    private final MeService meService;

    @Operation(
            summary = "마이페이지 조회",
            description = """
                내 기본 정보와 누적 저금 통계를 조회

                - 내챌린지개수통계(완료된 개수, 진행중 개수, 내가만든 개수)
                - 주차별 누적 저금액 6개 포함 (0주차~5주전까지 리스트에 순서대로)**
                - 이번주·이번달·올해 누적 저금액
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        var response = meService.getMyProfile(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<NicknameResponse> updateNickname(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String,String> req) {
        String nickname = (req.get("nickname")==null? "" : req.get("nickname")).trim();
        if (!nickname.matches("^[가-힣A-Za-z0-9]{2,16}$"))
            throw new BusinessException(ApiErrorCode.INVALID_INPUT_VALUE, "2~16자, 공백/특수문자 불가");

        if (userAccountRepository.existsByNickname(nickname))
            throw new BusinessException(ApiErrorCode.DUPLICATE_NICKNAME, "중복 닉네임");

        var user = userAccountRepository.findById(principal.getUserId()).orElseThrow();
        user.updateNickname(nickname);
        userAccountRepository.save(user);

        return ResponseEntity.ok(NicknameResponse.builder()
                .nickname(nickname)
                .changedAt(LocalDateTime.now()) // 저장된 updatedAt을 반환해도 무방
                .build());
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        userAccountRepository.deleteById(principal.getUserId());
        return ResponseEntity.noContent().build();
    }


}

