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
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ChallengeDiscoverService {
    private final ChallengeRepository challengeRepository;

    public Page<ChallengeListItemView> discover(String categoryType, String status, String sort, int page, int size) {
        String category = normalizeCategory(categoryType);
        String st = normalizeStatus(status);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        if ("avgCertRate,desc".equalsIgnoreCase(sort)) {
            Pageable pageable = PageRequest.of(page, size);
            return challengeRepository.searchDiscoverOrderByAvgCertRate(category, st, today, 28, pageable);
        }
        if ("totalAmount,desc".equalsIgnoreCase(sort)) {
            Pageable pageable = PageRequest.of(page, size);
            return challengeRepository.searchDiscoverOrderByTotalAmount(category, st, today, pageable);
        }
        Pageable pageable = buildPageable(sort, page, size);
        return challengeRepository.searchDiscover(category, st, today, pageable);
    }

    public Pageable buildPageable(String sort, int page, int size) {
        if ("createdAt,desc".equalsIgnoreCase(sort)) {
            return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt").nullsLast()));
        }
        return PageRequest.of(page, size, Sort.by(Sort.Order.asc("startDate")));
    }

    public String normalizeCategory(String c) {
        if (c == null) return "ALL";
        return switch (c.toUpperCase()) {
            case "ALL", "SAVING", "INSTALLMENT", "OTHER" -> c.toUpperCase();
            default -> "ALL";
        };
    }
    public String normalizeStatus(String s) {
        if (s == null) return null;
        return switch (s.toUpperCase()) {
            case "PENDING", "ACTIVE", "COMPLETED" -> s.toUpperCase();
            default -> null;
        };
    }
}
