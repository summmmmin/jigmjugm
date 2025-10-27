package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.dto.ChallengeListItemView;
import com.jigmjugm.home.repo.HomeChallengeItemView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

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
            when :today < c.startDate then 'PENDING'
            when c.startDate <= :today and c.endDate >= :today then 'ACTIVE'
            else 'COMPLETED'
          end) as status
        from Challenge c
        where c.isDeleted = false
          and (:category = 'ALL' or c.categoryType = :category)
          and (
            :status is null
            or (:status = 'PENDING' and :today < c.startDate)
            or (:status = 'ACTIVE' and c.startDate <= :today and c.endDate >= :today)
            or (:status = 'COMPLETED' and c.endDate < :today)
          )
        """)
    Page<ChallengeListItemView> searchDiscover(
            @Param("category") String category,
            @Param("status") String status,
            @Param("today") LocalDate today,
            Pageable pageable);

    @Query(value = """
      select exists(
        select 1
        from challenge c
        where c.is_deleted = false
          and regexp_replace(trim(c.title), '\\s+', ' ', 'g') = :normalizedTitle
      )
      """, nativeQuery = true)
    boolean existsnormalizedTitle(@Param("normalizedTitle") String normalizedTitle);

    @Query(value = """
      select exists(
        select 1
        from challenge c
        where c.is_deleted = false
          and c.challenge_id <> :excludeId
          and regexp_replace(trim(c.title), '\\s+', ' ', 'g') = :normalizedTitle
      )
      """, nativeQuery = true)
    boolean existsNormalizedTitleExceptId(@Param("normalizedTitle") String normalizedTitle, @Param("excludeId") Long excludeId);

    // 시작일 가까운
    @Query(value = """
        select 
          c.challenge_id as challengeId,
          c.title as title,
          c.category_type as categoryType,
          c.frequency_type as frequencyType,
          c.start_date as startDate,
          c.end_date as endDate,
          c.created_at as createdAt,
          c.thumbnail_url as thumbnailUrl,
          (select count(*) from challenge_participation p 
             where p.challenge_id = c.challenge_id and p.left_at is null) as participantCount,
          null as avgCertRate,
          (case 
             when :today < c.start_date then 'PENDING'
             when c.start_date <= :today and c.end_date >= :today then 'ACTIVE'
             else 'COMPLETED'
           end) as status,
          c.per_round_amount as perRoundAmount
        from challenge c
        where c.is_deleted = false
          and (:category = 'ALL' or c.category_type = :category)
          and c.start_date > :today
        order by c.start_date asc, c.challenge_id asc
        """, nativeQuery = true)
    List<HomeChallengeItemView> homeStartSoon(String category, LocalDate today, Pageable pageable);

    // 최근 생성
    @Query(value = """
        select 
          c.challenge_id as challengeId,
          c.title as title,
          c.category_type as categoryType,
          c.frequency_type as frequencyType,
          c.start_date as startDate,
          c.end_date as endDate,
          c.created_at as createdAt,
          c.thumbnail_url as thumbnailUrl,
          (select count(*) from challenge_participation p 
             where p.challenge_id = c.challenge_id and p.left_at is null) as participantCount,
          null as avgCertRate,
          (case 
             when :today < c.start_date then 'PENDING'
             when c.start_date <= :today and c.end_date >= :today then 'ACTIVE'
             else 'COMPLETED'
           end) as status,
          c.per_round_amount as perRoundAmount
        from challenge c
        where c.is_deleted = false
          and (:category = 'ALL' or c.category_type = :category)
        order by c.created_at desc, c.challenge_id desc
        """, nativeQuery = true)
    List<HomeChallengeItemView> homeNewest(String category, LocalDate today, Pageable pageable);

    // 최근 인증 활발
    @Query(value = """
        with period as (
          select :today::date as today, (:today::date - (:periodDays||' days')::interval)::date as since
        )
        select 
          c.challenge_id as challengeId,
          c.title as title,
          c.category_type as categoryType,
          c.frequency_type as frequencyType,
          c.start_date as startDate,
          c.end_date as endDate,
          c.created_at as createdAt,
          c.thumbnail_url as thumbnailUrl,
          (select count(*) from challenge_participation p 
             where p.challenge_id = c.challenge_id and p.left_at is null) as participantCount,
          case 
            when rounds_in_period.cnt_rounds = 0 then 0
            else (approved_in_period.cnt_approved::double precision / rounds_in_period.cnt_rounds)
          end as avgCertRate,
          (case 
             when :today < c.start_date then 'PENDING'
             when c.start_date <= :today and c.end_date >= :today then 'ACTIVE'
             else 'COMPLETED'
           end) as status,
          c.per_round_amount as perRoundAmount
        from challenge c
        cross join period
        left join (
          select r.challenge_id, count(*) as cnt_rounds
          from challenge_round r, period
          where r.scheduled_date between period.since and period.today
          group by r.challenge_id
        ) rounds_in_period on rounds_in_period.challenge_id = c.challenge_id
        left join (
          select r.challenge_id, count(*) as cnt_approved
          from certification cf
          join challenge_round r on r.round_id = cf.round_id
          , period
          where r.scheduled_date between period.since and period.today
            and cf.certification_status = 'APPROVED'
          group by r.challenge_id
        ) approved_in_period on approved_in_period.challenge_id = c.challenge_id
        where c.is_deleted = false
          and (:category = 'ALL' or c.category_type = :category)
          and (select count(*) from challenge_participation p 
                 where p.challenge_id = c.challenge_id and p.left_at is null) >= :minParticipants
        order by avgCertRate desc nulls last, participantCount desc, c.created_at desc
        """, nativeQuery = true)
    List<HomeChallengeItemView> homeActive(String category, LocalDate today, int periodDays, int minParticipants, Pageable pageable);
}
