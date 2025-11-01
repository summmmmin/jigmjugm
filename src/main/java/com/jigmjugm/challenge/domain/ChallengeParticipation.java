package com.jigmjugm.challenge.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "challenge_participation",
        indexes = @Index(name="idx_part_user_ch", columnList = "challenge_id,user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long participationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(nullable = false)
    private Long userId;

    @Builder.Default
    @Column(nullable = false, length = 16)
    private String roleType = "MEMBER"; // OWNER / MEMBER

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime joinedAt = OffsetDateTime.now(ZoneOffset.UTC);

    private OffsetDateTime leftAt;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
}

