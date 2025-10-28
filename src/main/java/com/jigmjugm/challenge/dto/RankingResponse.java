package com.jigmjugm.challenge.dto;

import java.util.List;

public record RankingResponse(
        List<RankingItem> top,
        RankingMe me
) {
    public static record RankingItem(int rank, long userId, String nickname, double certRate) {}
    public static record RankingMe(int rank, double certRate) {}
}