package com.jigmjugm.certification.domain;

import com.jigmjugm.challenge.domain.ChallengeParticipation;
import com.jigmjugm.challenge.domain.ChallengeRound;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "certification",
        uniqueConstraints = @UniqueConstraint(name="uq_user_round", columnNames={"participation_id","round_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long certificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private ChallengeParticipation participation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private ChallengeRound round;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 200)
    private String comment;

    private String imageUrl; // S3 URL

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String doubleYn = "N"; // Y/N

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String certificationStatus = "APPROVED"; // 일단 항상 APPROVED로 처리

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime certifiedAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
}

