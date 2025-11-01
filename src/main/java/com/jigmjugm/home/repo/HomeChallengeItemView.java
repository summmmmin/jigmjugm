package com.jigmjugm.home.repo;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface HomeChallengeItemView {
    Long getChallengeId();
    String getTitle();
    String getCategoryType();
    String getFrequencyType();
    LocalDate getStartDate();
    LocalDate getEndDate();
    @Value("#{ T(java.time.OffsetDateTime).ofInstant(target.createdAt, T(java.time.ZoneId).of('Asia/Seoul')) }")
    OffsetDateTime getCreatedAt();
    String getThumbnailUrl();
    Integer getParticipantCount();
    Double getAvgCertRate();
    String getStatus();
    Long getPerRoundAmount();
}
