package com.jigmjugm.user.service;

import com.jigmjugm.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class NicknameService {
    private final UserAccountRepository userRepo;
    private final Random random = new Random();

    @Value("classpath:nickname/adjectives.txt")
    private Resource adjectivesRes;

    @Value("classpath:nickname/nouns.txt")
    private Resource nounsRes;

    private volatile List<String> adjectives;
    private volatile List<String> nouns;

    private void ensureLoaded() {
        try {
            if (adjectives == null) {
                adjectives = Files.readAllLines(adjectivesRes.getFile().toPath(), StandardCharsets.UTF_8)
                        .stream().map(String::trim).filter(s -> !s.isBlank()).toList();
            }
            if (nouns == null) {
                nouns = Files.readAllLines(nounsRes.getFile().toPath(), StandardCharsets.UTF_8)
                        .stream().map(String::trim).filter(s -> !s.isBlank()).toList();
            }
        } catch (Exception e) {
            throw new IllegalStateException("닉네임 사전 로딩 실패", e);
        }
    }

    public String generateUniqueNickname() {
        ensureLoaded();
        // 1) 기본 조합
        String base = adjectives.get(random.nextInt(adjectives.size()))
                + nouns.get(random.nextInt(nouns.size()));
        // 2) 2~16자 규칙 고려: 숫자 접미사 대비해 base를 잘라서 사용
        base = base.substring(0, Math.min(base.length(), 16));

        // 3) 중복 검사 + 접미사 부여
        if (!userRepo.existsByNickname(base)) return base;
        for (int i = 1; i <= 9999; i++) {
            String suffix = String.valueOf(i);
            int maxBase = 16 - suffix.length();
            String candidate = base.length() > maxBase ? base.substring(0, maxBase) + suffix : base + suffix;
            if (!userRepo.existsByNickname(candidate)) return candidate;
        }
        throw new IllegalStateException("닉네임 생성 실패(충돌 과다)");
    }
}

