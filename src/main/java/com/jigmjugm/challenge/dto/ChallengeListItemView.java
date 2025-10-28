package com.jigmjugm.challenge.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface ChallengeListItemView {
    Long getChallengeId();
    String getTitle();
    String getCategoryType();
    String getFrequencyType();
    Integer getPerRoundAmount();
    Long getGoalAmount();
    LocalDate getStartDate();
    LocalDate getEndDate();
    OffsetDateTime getCreatedAt();
    String getThumbnailUrl();
    Integer getParticipantCount();
    Double getAvgCertRate();
    String getStatus();// PENDING | ACTIVE | COMPLETED

}
