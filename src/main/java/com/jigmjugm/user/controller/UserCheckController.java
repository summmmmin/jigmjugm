package com.jigmjugm.user.controller;

import com.jigmjugm.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserCheckController {
    private final UserAccountRepository userAccountRepository;

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String,Object>> checkNickname(@RequestParam String nickname) {
        String n = nickname == null? "" : nickname.trim();
        if (!n.matches("^[가-힣A-Za-z0-9]{2,16}$"))
            return ResponseEntity.badRequest().body(Map.of("code","VALIDATION_ERROR","message","2~16자, 공백/특수문자 불가"));

        boolean dup = userAccountRepository.existsByProviderUserId(n);
        return ResponseEntity.ok(Map.of("available", !dup));
    }
}

