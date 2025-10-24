package com.jigmjugm.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class ParticipationResponse {
    private Long participationId;
    private Long challengeId;
    private Long userId;
    private OffsetDateTime joinedAt;
}
