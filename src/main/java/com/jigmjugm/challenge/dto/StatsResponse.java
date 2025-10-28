package com.jigmjugm.challenge.dto;

public record StatsResponse(
        long   avgTotalAmount,
        double avgCertRate,
        int    totalParticipants
) {}
