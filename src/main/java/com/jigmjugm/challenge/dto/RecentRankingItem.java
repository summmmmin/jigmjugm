package com.jigmjugm.challenge.dto;

import java.time.LocalDate;
import java.util.List;

public record RecentRankingItem(
        int weekIndex,                 // 1~4 (기간 길이(주))
        LocalDate periodStart,         // 조회 시작
        LocalDate periodEnd,           // 조회 끝(포함, today)
        List<RankingResponse.RankingItem> top,
        RankingResponse.RankingMe me
) {}