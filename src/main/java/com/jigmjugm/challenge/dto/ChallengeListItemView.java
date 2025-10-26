package com.jigmjugm.challenge.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface ChallengeListItemView {
    Long getChallengeId();
    String getTitle();
    String getCategoryType();
    String getFrequencyType();
    LocalDate getStartDate();
    LocalDate getEndDate();
    OffsetDateTime getCreatedAt();
    String getThumbnailUrl();
    String getStatus(); // PENDING | ACTIVE | COMPLETED
}
