package com.jigmjugm.certification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class CertificationResponse {
    private Long certificationId;
    private Long participationId;
    private Long roundId;
    private Long amount;
    private String doubleYn;
    private String certificationStatus;
    private OffsetDateTime certifiedAt;
}
