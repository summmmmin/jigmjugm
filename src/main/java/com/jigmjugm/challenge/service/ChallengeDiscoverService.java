package com.jigmjugm.challenge.service;

import com.jigmjugm.challenge.dto.ChallengeListItemView;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChallengeDiscoverService {
    private final ChallengeRepository challengeRepository;

    public Page<ChallengeListItemView> discover(String category, String status, String sortParam, int page, int size) {
        Pageable pageable = buildPageable(sortParam, page, size);
        String safeCategory = normalizeCategory(category);
        String safeStatus = normalizeStatus(status);
        LocalDate todaySeoul = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        return challengeRepository.searchDiscover(safeCategory, safeStatus, todaySeoul, pageable);
    }

    private Pageable buildPageable(String sortParam, int page, int size) {
        Map<String, String> allow = Map.of(
                "startdate", "startDate",
                "createdat", "createdAt"
        );
        String prop = "startDate"; Sort.Direction dir = Sort.Direction.ASC;

        if (sortParam != null) {
            String[] p = sortParam.split(",", 2);
            String key = p[0].trim().toLowerCase();
            if (allow.containsKey(key)) prop = allow.get(key);
            if (p.length == 2) {
                String d = p[1].trim().toLowerCase();
                if ("desc".equals(d)) dir = Sort.Direction.DESC;
            }
        }
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(dir, prop));
    }

    private String normalizeCategory(String c) {
        if (c == null) return "ALL";
        return switch (c.toUpperCase()) {
            case "ALL", "SAVING", "INSTALLMENT", "OTHER" -> c.toUpperCase();
            default -> "ALL";
        };
    }
    private String normalizeStatus(String s) {
        if (s == null) return null;
        return switch (s.toUpperCase()) {
            case "PENDING", "ACTIVE", "COMPLETED" -> s.toUpperCase();
            default -> null;
        };
    }
}
