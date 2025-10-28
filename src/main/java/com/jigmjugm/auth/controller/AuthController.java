package com.jigmjugm.auth.controller;

import com.jigmjugm.auth.domain.LogoutRequest;
import com.jigmjugm.auth.dto.AuthResponse;
import com.jigmjugm.auth.dto.KakaoTokenExchangeRequest;
import com.jigmjugm.auth.service.AuthService;
import com.jigmjugm.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal UserPrincipal oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(401).body(Map.of("message", "User not authenticated"));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", oAuth2User.getUserId());
        userInfo.put("nickname", oAuth2User.getNickname());
        userInfo.put("provider", "KAKAO");

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/logout-success")
    public ResponseEntity<Map<String, String>> logoutSuccess() {
        return ResponseEntity.ok(Map.of("message", "Successfully logged out"));
    }

    @PostMapping("/kakao")
    public ResponseEntity<AuthResponse> exchange(@Valid @RequestBody KakaoTokenExchangeRequest req) {
        var res = authService.exchangeKakaoToken(req.getKakaoAccessToken());
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LogoutRequest body) {
        if (principal == null) return ResponseEntity.status(401).build();
        authService.logout(principal.getUserId(), body.getRefreshToken(), Boolean.TRUE.equals(body.getAllDevices()));
        return ResponseEntity.noContent().build();
    }
}