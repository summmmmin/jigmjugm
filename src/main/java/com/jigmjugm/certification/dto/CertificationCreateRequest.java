package com.jigmjugm.certification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificationCreateRequest {
    @NotNull
    @Min(1)
    private Long amount;
    @Size(max=200)
    private String comment;
    private String imageUrl;
    @Pattern(regexp = "Y|N")
    private String doubleYn = "N";
}
