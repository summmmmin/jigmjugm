package com.jigmjugm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "message", "Welcome to Jigmjugm API",
            "login", "Please visit /oauth2/authorization/kakao to login with Kakao"
        );
    }
}