package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.dto.ChallengeListItemView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChallengeRepository  extends JpaRepository<Challenge, Long> {
    @Query("""
        select
          c.challengeId as challengeId,
          c.title as title,
          c.categoryType as categoryType,
          c.frequencyType as frequencyType,
          c.startDate as startDate,
          c.endDate as endDate,
          c.createdAt as createdAt,
          c.thumbnailUrl as thumbnailUrl,
          (case
            when CURRENT_DATE < c.startDate then 'PENDING'
            when c.startDate <= CURRENT_DATE and c.endDate >= CURRENT_DATE then 'ACTIVE'
            else 'COMPLETED'
          end) as status
        from Challenge c
        where
          (:category = 'ALL' or c.categoryType = :category)
          and (
            :status is null
            or (:status = 'PENDING' and CURRENT_DATE < c.startDate)
            or (:status = 'ACTIVE' and c.startDate <= CURRENT_DATE and c.endDate >= CURRENT_DATE)
            or (:status = 'COMPLETED' and c.endDate < CURRENT_DATE)
          )
        """)
    Page<ChallengeListItemView> searchDiscover(
            @Param("category") String category,
            @Param("status") String status,
            Pageable pageable);
}
