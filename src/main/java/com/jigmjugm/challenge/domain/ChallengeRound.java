package com.jigmjugm.challenge.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "challenge_round",
        uniqueConstraints = {
                @UniqueConstraint(name="uq_round_no", columnNames={"challenge_id","round_no"}),
                @UniqueConstraint(name="uq_round_date", columnNames={"challenge_id","scheduled_date"})
        })
@Getter
@Setter
public class ChallengeRound {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(nullable = false) private Integer roundNo;         // 1..N
    @Column(nullable = false) private LocalDate scheduledDate; // 인증 예정일
    @Column(nullable = false) private Long baseAmount;
    @Column(nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
}
