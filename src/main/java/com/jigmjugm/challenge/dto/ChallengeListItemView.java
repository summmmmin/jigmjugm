package com.jigmjugm.challenge.dto;

import org.springframework.beans.factory.annotation.Value;

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
    @Value("#{ T(java.time.OffsetDateTime).ofInstant(target.createdAt, T(java.time.ZoneId).of('Asia/Seoul')) }")
    OffsetDateTime getCreatedAt();
    String getThumbnailUrl();
    Integer getParticipantCount();
    Double getAvgCertRate();
    String getStatus();// PENDING | ACTIVE | COMPLETED

}
