package com.jigmjugm.home.repo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface HomeChallengeItemView {
    Long getChallengeId();
    String getTitle();
    String getCategoryType();
    String getFrequencyType();
    LocalDate getStartDate();
    LocalDate getEndDate();
    OffsetDateTime getCreatedAt();
    String getThumbnailUrl();
    Integer getParticipantCount();
    Double getAvgCertRate();
    String getStatus();
    Long getPerRoundAmount();
}
