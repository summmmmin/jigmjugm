package com.jigmjugm.challenge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "challenge")
@Getter
@Setter
public class Challenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long challengeId;

    @Column(nullable = false) private Long creatorUserId;
    @Column(nullable = false, length = 50) private String title;
    @Column(length = 500) private String description;

    @Column(nullable = false, length = 16) private String categoryType;  // SAVING/INSTALLMENT/OTHER
    @Column(nullable = false, length = 16) private String frequencyType; // DAILY/WEEKLY
    @Column(nullable = false) private Integer weeklyDaysMask;             // 0~127

    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;

    @Column(nullable = false) private Long perRoundAmount;
    @Column(nullable = false) private Long goalAmount;

    private String thumbnailUrl;

    @Column(nullable = false) private Boolean isDeleted = false;
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    @Column(nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChallengeRound> rounds = new ArrayList<>();
}