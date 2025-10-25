package com.jigmjugm.controller;

import com.jigmjugm.domain.user.entity.UserAccount;
import com.jigmjugm.domain.user.repository.UserAccountRepository;
import com.jigmjugm.dto.UserProfileResponse;
import com.jigmjugm.security.dto.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAccountRepository userAccountRepository;

    /**
     * 현재 로그인한 사용자의 상세 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(401).build();
        }

        UserAccount user = userAccountRepository.findById(oAuth2User.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .challengeCount(0) // TODO: 챌린지 기능 구현 후 실제 데이터로 변경
                .completedChallengeCount(0) // TODO: 챌린지 기능 구현 후 실제 데이터로 변경
                .points(0) // TODO: 포인트 기능 구현 후 실제 데이터로 변경
                .level(1) // TODO: 레벨 기능 구현 후 실제 데이터로 변경
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 닉네임 수정
     */
    @PatchMapping("/me/nickname")
    public ResponseEntity<Map<String, String>> updateNickname(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User,
            @RequestBody Map<String, String> request) {
        
        if (oAuth2User == null) {
            return ResponseEntity.status(401).build();
        }

        String newNickname = request.get("nickname");
        if (newNickname == null || newNickname.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nickname is required"));
        }

        UserAccount user = userAccountRepository.findById(oAuth2User.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.updateNickname(newNickname.trim());
        userAccountRepository.save(user);

        return ResponseEntity.ok(Map.of("nickname", newNickname));
    }

    /**
     * 회원 탈퇴 (소프트 삭제)
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        
        if (oAuth2User == null) {
            return ResponseEntity.status(401).build();
        }

        UserAccount user = userAccountRepository.findById(oAuth2User.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.softDelete();
        userAccountRepository.save(user);

        // TODO: 세션 무효화 처리
        
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    /**
     * 사용자 통계 정보 조회
     */
    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(
            @AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        
        if (oAuth2User == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", oAuth2User.getUserId());
        stats.put("totalChallenges", 0); // TODO: 실제 데이터로 변경
        stats.put("completedChallenges", 0);
        stats.put("successRate", 0.0);
        stats.put("currentStreak", 0);
        stats.put("longestStreak", 0);
        stats.put("totalPoints", 0);
        stats.put("rank", 0);
        
        return ResponseEntity.ok(stats);
    }
}