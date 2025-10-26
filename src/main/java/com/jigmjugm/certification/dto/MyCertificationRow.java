package com.jigmjugm.certification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class MyCertificationRow {
    private Long roundId;
    private Integer roundNo;
    private LocalDate scheduledDate;
    private Boolean certified;
    private Long certificationId;
    private Long amount;
    private String doubleYn;
    private String comment;
    private String imageUrl;
    private OffsetDateTime certifiedAt;
}
