package com.jigmjugm.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChallengeCreateResponse {
    private Long challengeId;
    private Integer roundCount;
    private String status; // PENDING/ACTIVE/COMPLETED
}
