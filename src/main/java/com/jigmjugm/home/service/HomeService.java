package com.jigmjugm.home.service;

import com.jigmjugm.certification.domain.Certification;
import com.jigmjugm.certification.repo.CertificationRepository;
import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import com.jigmjugm.common.util.ChallengePolicy;
import com.jigmjugm.home.dto.HomeResponse;
import com.jigmjugm.home.repo.HomeChallengeItemView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;
    private final ChallengeRoundRepository roundRepository;
    private final CertificationRepository certificationRepository;
    private final ChallengePolicy policy;

    @Transactional(readOnly = true)
    public HomeResponse buildHome(Long userId, String categoryType, int limit,
                                  int periodDays, int minParticipants, int myUpcomingLimit) {

        String category = normalizeCategory(categoryType);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var topN = PageRequest.of(0, limit);

        List<HomeResponse.ChallengeListItem> startSoon =
                mapToDto(challengeRepository.homeStartSoon(category, today, topN));
        List<HomeResponse.ChallengeListItem> newest =
                mapToDto(challengeRepository.homeNewest(category, today, topN));
        List<HomeResponse.ChallengeListItem> active =
                mapToDto(challengeRepository.homeActive(category, today, periodDays, minParticipants, topN));

        // 로그인 상태일 때만 myUpcoming 조회
        List<HomeResponse.MyUpcomingItem> myUpcoming = null;
        if (userId != null) {
            myUpcoming = buildMyUpcoming(userId, myUpcomingLimit, today);
        }

        return HomeResponse.builder()
                .startSoon(startSoon)
                .newest(newest)
                .active(active)
                .myUpcoming(myUpcoming)
                .build();
    }

    private String normalizeCategory(String c) {
        if (c == null) return "ALL";
        return switch (c.toUpperCase()) {
            case "ALL", "SAVING", "INSTALLMENT", "OTHER" -> c.toUpperCase();
            default -> "ALL";
        };
    }

    private List<HomeResponse.ChallengeListItem> mapToDto(List<HomeChallengeItemView> views) {
        List<HomeResponse.ChallengeListItem> list = new ArrayList<>();
        for (var v : views) {
            list.add(HomeResponse.ChallengeListItem.builder()
                    .challengeId(v.getChallengeId())
                    .title(v.getTitle())
                    .categoryType(v.getCategoryType())
                    .frequencyType(v.getFrequencyType())
                    .startDate(v.getStartDate())
                    .endDate(v.getEndDate())
                    .createdAt(v.getCreatedAt())
                    .thumbnailUrl(v.getThumbnailUrl())
                    .participantCount(Optional.ofNullable(v.getParticipantCount()).orElse(0))
                    .avgCertRate(v.getAvgCertRate())
                    .status(v.getStatus())
                    .perRoundAmount(v.getPerRoundAmount())
                    .build());
        }
        return list;
    }

    private List<HomeResponse.MyUpcomingItem> buildMyUpcoming(Long userId, int limit, LocalDate today) {
        var parts = participationRepository.findByUserIdAndLeftAtIsNull(userId);
        if (parts.isEmpty()) return List.of();

        var result = new ArrayList<HomeResponse.MyUpcomingItem>();

        for (var p : parts) {
            Challenge ch = p.getChallenge();

            // 상태 계산
            String status = policy.computeStatus(ch.getStartDate(), ch.getEndDate(), today);

            // 오늘 이후 회차
            List<ChallengeRound> futureRounds =
                    roundRepository.findByChallenge_ChallengeIdAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(
                            ch.getChallengeId(), today);

            if (futureRounds.isEmpty()) continue;

            // 내 인증들(활성 참여자 기준)
            List<Certification> myCerts = certificationRepository.findActiveByUserAndChallenge(userId, ch.getChallengeId());
            var certifiedRoundIds = myCerts.stream()
                    .map(c -> c.getRound().getRoundId())
                    .collect(Collectors.toSet());

            // 아직 인증 안 한 남은 회차
            ChallengeRound next = futureRounds.stream()
                    .filter(r -> !certifiedRoundIds.contains(r.getRoundId()))
                    .findFirst()
                    .orElse(null);

            if (next == null) continue;

            // 남은(오늘 이후 미인증) 회차수
            int remaining = (int) futureRounds.stream()
                    .filter(r -> !certifiedRoundIds.contains(r.getRoundId()))
                    .count();

            result.add(HomeResponse.MyUpcomingItem.builder()
                    .challengeId(ch.getChallengeId())
                    .title(ch.getTitle())
                    .status(status)
                    .nextRoundNo(next.getRoundNo())
                    .nextScheduledDate(next.getScheduledDate())
                    .remainingRounds(remaining)
                    .perRoundAmount(ch.getPerRoundAmount())
                    .thumbnailUrl(ch.getThumbnailUrl())
                    .build());
        }

        // 다음 인증일 오름차순 정렬 후 상위 N개
        result.sort(Comparator.comparing(HomeResponse.MyUpcomingItem::getNextScheduledDate)
                .thenComparing(HomeResponse.MyUpcomingItem::getChallengeId));
        if (result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }
}
