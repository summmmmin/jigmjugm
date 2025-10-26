package com.jigmjugm.challenge.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ChallengeCreateRequest {
    @NotBlank
    @Size(max=50) private String title;
    @Size(max=500) private String description;

    @NotBlank private String categoryType;  // SAVING/INSTALLMENT/OTHER
    @NotBlank private String frequencyType; // DAILY/WEEKLY

    @NotNull
    private LocalDate startDate;
    @NotNull private LocalDate endDate;

    @NotNull @Min(1) private Long perRoundAmount;
    @NotNull @Min(1) private Long goalAmount;

    private List<@Pattern(regexp="MON|TUE|WED|THU|FRI|SAT|SUN") String> weeklyDays; // WEEKLY일 때 필수
    private String thumbnailUrl;
}
