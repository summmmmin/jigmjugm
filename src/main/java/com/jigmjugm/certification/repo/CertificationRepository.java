package com.jigmjugm.certification.repo;

import com.jigmjugm.certification.domain.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    boolean existsByParticipation_ParticipationIdAndRound_RoundId(Long participationId, Long roundId);

    List<Certification> findByParticipation_UserId(Long userId);

    Optional<Certification> findByCertificationIdAndParticipation_UserId(Long certificationId, Long userId);
}

