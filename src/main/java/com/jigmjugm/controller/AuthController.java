package com.jigmjugm.controller;

import com.jigmjugm.security.dto.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal CustomOAuth2User oAuth2User) {
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
}