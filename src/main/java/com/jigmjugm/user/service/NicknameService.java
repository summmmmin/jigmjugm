package com.jigmjugm.user.service;

import com.jigmjugm.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
            if (adjectives == null || nouns == null) {
                adjectives = readLines(adjectivesRes);
                nouns = readLines(nounsRes);
            }
        } catch (Exception e) {
            throw new IllegalStateException("닉네임 사전 로딩 실패", e);
        }
    }

    private List<String> readLines(Resource res) throws Exception {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            return br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

    public String generateUniqueNickname() {
        ensureLoaded();

        String base = adjectives.get(random.nextInt(adjectives.size()))
                + nouns.get(random.nextInt(nouns.size()));
        base = base.substring(0, Math.min(base.length(), 16));

        if (!userRepo.existsByNickname(base)) return base;
        for (int i = 1; i <= 9999; i++) {
            String suffix = String.valueOf(i);
            int maxBase = 16 - suffix.length();
            String candidate = base.length() > maxBase
                    ? base.substring(0, maxBase) + suffix
                    : base + suffix;
            if (!userRepo.existsByNickname(candidate)) return candidate;
        }
        throw new IllegalStateException("닉네임 생성 실패(충돌 과다)");
    }
}
