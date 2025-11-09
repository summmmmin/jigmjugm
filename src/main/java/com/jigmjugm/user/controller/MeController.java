package com.jigmjugm.user.controller;

import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.security.UserPrincipal;
import com.jigmjugm.user.dto.NicknameResponse;
import com.jigmjugm.user.dto.UserProfileResponse;
import com.jigmjugm.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MeController {

    private final UserAccountRepository userAccountRepository;
    private final ChallengeParticipationRepository challengeParticipationRepository;
    private final ChallengeRepository challengeRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        var user = userAccountRepository.findById(principal.getUserId()).orElseThrow();

        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        long challengeCount = challengeParticipationRepository
                .countMyActiveOrUpcoming(user.getUserId(), today);

        long completedChallengeCount = challengeParticipationRepository
                .countMyCompleted(user.getUserId(), today);

        long createdChallengeCount = challengeRepository
                .countByCreatorUserIdAndIsDeletedFalse(user.getUserId());

        return ResponseEntity.ok(UserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .challengeCount(challengeCount)
                .completedChallengeCount(completedChallengeCount)
                .createdChallengeCount(createdChallengeCount)
                .build());
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
        var user = userAccountRepository.findById(principal.getUserId()).orElseThrow();
        user.softDelete();
        userAccountRepository.save(user);
        return ResponseEntity.noContent().build();
    }


}

