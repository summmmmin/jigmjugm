package com.jigmjugm.challenge.service;

import com.jigmjugm.challenge.dto.RankingResponse;
import com.jigmjugm.challenge.dto.RecentRankingItem;
import com.jigmjugm.challenge.dto.StatsResponse;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeStatsService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    public StatsResponse stats(Long challengeId) {
        var exists = challengeRepository.existsById(challengeId);
        if (!exists) throw new BusinessException(ApiErrorCode.NOT_FOUND, "챌린지를 찾을 수 없습니다.");
        ChallengeParticipationRepository.StatsRow row = participationRepository.statsByChallenge(challengeId, today());
        long avgTotalAmount = row != null && row.getAvgTotalAmount() != null ? row.getAvgTotalAmount() : 0L;
        double avgCertRate = row != null && row.getAvgCertRate() != null ? row.getAvgCertRate() : 0.0;
        int totalParticipants = row != null && row.getTotalParticipants() != null ? row.getTotalParticipants() : 0;
        return new StatsResponse(avgTotalAmount, avgCertRate, totalParticipants);
    }

    public RankingResponse ranking(Long challengeId, Integer periodDays, Integer limit, UserPrincipal principal) {
        var exists = challengeRepository.existsById(challengeId);
        if (!exists) throw new BusinessException(ApiErrorCode.NOT_FOUND, "챌린지를 찾을 수 없습니다.");

        int pd = (periodDays == null ? 28 : Math.max(7, Math.min(90, periodDays)));
        int lim = (limit == null ? 3 : Math.max(1, Math.min(50, limit)));
        var end = today();
        var start = end.minusDays(pd - 1); // inclusive window

        List<ChallengeParticipationRepository.RankedRow> topRows = participationRepository.rankingTopRange(challengeId, start, end, lim);
        var top = topRows.stream()
                .map(r -> new RankingResponse.RankingItem(
                        r.getRank() == null ? 0 : r.getRank(),
                        r.getUserId(),
                        r.getNickname(),
                        r.getCertRate() == null ? 0.0 : r.getCertRate()))
                .toList();

        RankingResponse.RankingMe me = null;
        if (principal != null) {
            var mine = participationRepository.rankingMeRange(challengeId, principal.getUserId(), start, end);
            if (!mine.isEmpty()) {
                var r = mine.get(0);
                me = new RankingResponse.RankingMe(
                        r.getRank() == null ? 0 : r.getRank(),
                        r.getCertRate() == null ? 0.0 : r.getCertRate()
                );
            }
        }
        return new RankingResponse(top, me);
    }

    public RecentRankingItem recentRanking(Long challengeId, UserPrincipal principal) {
        var ch = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.NOT_FOUND, "챌린지를 찾을 수 없습니다."));

        var end = today();
        int elapsedDays = (int) ChronoUnit.DAYS.between(ch.getStartDate(), end);
        int periodDays;
        if (elapsedDays < 14)       periodDays = 7;
        else if (elapsedDays < 21)  periodDays = 14;
        else if (elapsedDays < 28)  periodDays = 21;
        else                        periodDays = 28;

        var start = end.minusDays(periodDays - 1);
        var topRows = participationRepository.rankingTopRange(challengeId, start, end, 3);
        var top = topRows.stream()
                .map(r -> new RankingResponse.RankingItem(
                        r.getRank() == null ? 0 : r.getRank(),
                        r.getUserId(),
                        r.getNickname(),
                        r.getCertRate() == null ? 0.0 : r.getCertRate()))
                .toList();

        RankingResponse.RankingMe me = null;
        if (principal != null) {
            var mine = participationRepository.rankingMeRange(challengeId, principal.getUserId(), start, end);
            if (!mine.isEmpty()) {
                var r = mine.getFirst();
                me = new RankingResponse.RankingMe(
                        r.getRank() == null ? 0 : r.getRank(),
                        r.getCertRate() == null ? 0.0 : r.getCertRate()
                );
            }
        }

        int weekIndex = periodDays / 7; // 1~4
        return new RecentRankingItem(weekIndex, start, end, top, me);
    }
}
