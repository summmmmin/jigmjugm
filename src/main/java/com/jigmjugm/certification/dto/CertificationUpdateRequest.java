package com.jigmjugm.certification.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificationUpdateRequest {
    private Long amount;
    private String comment;
    private String imageUrl;
    private String doubleYn;
}
