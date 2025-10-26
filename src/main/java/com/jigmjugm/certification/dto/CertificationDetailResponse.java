package com.jigmjugm.certification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CertificationDetailResponse {
    @Getter @Builder
    public static class Certification {
        private Long certificationId;
        private Long challengeId;
        private String challengeTitle;
        private Integer roundNo;
        private java.time.OffsetDateTime certifiedAt;
        private Long amount;
        private String doubleYn;
        private Long accumulatedAmount;
    }
    @Getter @Builder
    public static class RoundStatusRow {
        private Integer roundNo;
        private java.time.LocalDate scheduledDate;
        private String status;
    }
    private Certification certification;
    private List<RoundStatusRow> rounds;
}
