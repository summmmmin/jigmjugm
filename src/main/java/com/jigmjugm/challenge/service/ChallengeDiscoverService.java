package com.jigmjugm.challenge.service;

import com.jigmjugm.challenge.dto.ChallengeListItemView;
import com.jigmjugm.challenge.dto.MyChallengeListItemView;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
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
    private final ChallengeParticipationRepository challengeParticipationRepository;
    public Page<ChallengeListItemView> discover(String categoryType, String status, String sort, int page, int size) {
        String category = normalizeCategory(categoryType);
        String st = normalizeStatus(status);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        if ("avgCertRate,desc".equalsIgnoreCase(sort)) {
            // 인증률 높은순
            Pageable pageable = PageRequest.of(page, size);
            return challengeRepository.searchDiscoverOrderByAvgCertRate(category, st, today, 28, pageable);
        }
        if ("totalAmount,desc".equalsIgnoreCase(sort)) {
            // 누적 저금액 많은 순
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

    public Page<MyChallengeListItemView> myChallenges(
            Long userId,
            String categoryType,
            String status,
            boolean includeWithdrawn,
            String sort,
            int page,
            int size
    ) {
        String raw = categoryType == null ? "ALL" : categoryType.toUpperCase();
        boolean myCreatedOnly = "MY_CREATED".equals(raw);      // 추가
        String category = myCreatedOnly ? "ALL" : normalizeCategory(raw);
        String st = normalizeStatus(status);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String s = sort == null ? "" : sort.trim();

        // 1) 나의 인증률 높은 순
        if ("avgCertRate,desc".equalsIgnoreCase(s)) {
            Pageable pageable = PageRequest.of(page, size);
            return challengeParticipationRepository.searchMyChallengesOrderByMyCertRate(
                    userId, category, st, today, 28, includeWithdrawn, myCreatedOnly, pageable
            );
        }

        // 2) 누적 저금액 많은 순
        if ("totalAmount,desc".equalsIgnoreCase(s)) {
            Pageable pageable = PageRequest.of(page, size);
            return challengeParticipationRepository.searchMyChallengesOrderByTotalAmount(
                    userId, category, st, today, includeWithdrawn, myCreatedOnly, pageable
            );
        }

        // 3) 최신 생성순 / 4) 시작일 순
        Pageable pageable = buildMyChallengesPageable(s, page, size);
        return challengeParticipationRepository.findMyChallenges(
                userId, category, st, today, includeWithdrawn, myCreatedOnly, pageable
        );
    }

    public Pageable buildMyChallengesPageable(String sort, int page, int size) {
        String s = sort == null ? "" : sort.trim();
        if ("createdAt,desc".equalsIgnoreCase(s)) {
            // root = ChallengeParticipation, createdAt/startDate는 challenge의 필드
            return PageRequest.of(page, size,
                    Sort.by(Sort.Order.desc("challenge.createdAt").nullsLast()));
        }
        // 기본: 시작일 가까운 순
        return PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("challenge.startDate")));
    }

}
