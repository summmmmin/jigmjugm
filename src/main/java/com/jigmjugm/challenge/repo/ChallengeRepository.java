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
              c.perRoundAmount as perRoundAmount,
              c.goalAmount      as goalAmount,
              c.startDate as startDate,
              c.endDate as endDate,
              c.createdAt as createdAt,
              c.thumbnailUrl as thumbnailUrl,
              case
                when :today < c.startDate then 'PENDING'
                when :today > c.endDate then 'COMPLETED'
                else 'ACTIVE'
              end as status,
              (select count(p) from ChallengeParticipation p
                where p.challenge.challengeId = c.challengeId and p.leftAt is null) as participantCount,
              null as avgCertRate
            from Challenge c
            where c.isDeleted = false
              and (:category = 'ALL' or c.categoryType = :category)
              and (
                :status is null
                or (:status = 'PENDING'  and :today < c.startDate)
                or (:status = 'ACTIVE'   and c.startDate <= :today and c.endDate >= :today)
                or (:status = 'COMPLETED' and c.endDate < :today)
              )
            """)
    Page<ChallengeListItemView> searchDiscover(
            String category, String status, LocalDate today, Pageable pageable);

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

    @Query(value = """
            with period as (
              select :today::date as today, (:today::date - (:periodDays - 1) * interval '1 day')::date as since
            ),
            approved_in_period as (
              select p.user_id, r.challenge_id, count(*) as approved_cnt
              from certification cf
              join challenge_participation p on p.participation_id = cf.participation_id and p.left_at is null
              join challenge_round r on r.round_id = cf.round_id
              where cf.certification_status = 'APPROVED'
                and r.scheduled_date between (select since from period) and (select today from period)
              group by p.user_id, r.challenge_id
            ),
            scheduled_in_period as (
              select r.challenge_id, count(*) as scheduled_cnt
              from challenge_round r
              where r.scheduled_date between (select since from period) and (select today from period)
              group by r.challenge_id
            ),
            rate as (
              select
                c.challenge_id,
                coalesce(avg(case when s.scheduled_cnt > 0 then a.approved_cnt::float / s.scheduled_cnt else 0 end),0) as avgCertRate
              from challenge c
              left join scheduled_in_period s on s.challenge_id = c.challenge_id
              left join approved_in_period a on a.challenge_id = c.challenge_id
              where c.is_deleted = false
              group by c.challenge_id
            )
            select
              c.challenge_id as challengeId,
              c.title as title,
              c.category_type as categoryType,
              c.frequency_type as frequencyType,
              c.per_round_amount as perRoundAmount, 
              c.goal_amount      as goalAmount,
              c.start_date as startDate,
              c.end_date as endDate,
              c.created_at as createdAt,
              c.thumbnail_url as thumbnailUrl,
              case
                when :today < c.start_date then 'PENDING'
                when :today > c.end_date then 'COMPLETED'
                else 'ACTIVE'
              end as status,
              (select count(*) from challenge_participation p
                where p.challenge_id = c.challenge_id and p.left_at is null) as participantCount,
              rate.avgCertRate as avgCertRate
            from challenge c
            left join rate on rate.challenge_id = c.challenge_id
            where c.is_deleted = false
              and (:category = 'ALL' or c.category_type = :category)
              and (
                :status is null
                or (:status = 'PENDING'  and :today < c.start_date)
                or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
                or (:status = 'COMPLETED' and c.end_date < :today)
              )
            order by rate.avgCertRate desc nulls last, c.created_at desc
            """,
            countQuery = """
                      select count(*) from challenge c
                      where c.is_deleted = false
                        and (:category = 'ALL' or c.category_type = :category)
                        and (
                          :status is null
                          or (:status = 'PENDING'  and :today < c.start_date)
                          or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
                          or (:status = 'COMPLETED' and c.end_date < :today)
                        )
                    """,
            nativeQuery = true)
    Page<ChallengeListItemView> searchDiscoverOrderByAvgCertRate(
            String category, String status, LocalDate today, int periodDays, Pageable pageable);

    @Query(value = """
            with sum_cf as (
              select p.challenge_id, coalesce(sum(case when cf.certification_status = 'APPROVED' then cf.amount end),0) as total_amount
              from challenge_participation p
              left join certification cf on cf.participation_id = p.participation_id
              group by p.challenge_id
            )
            select
              c.challenge_id   as challengeId,
              c.title          as title,
              c.category_type  as categoryType,
              c.frequency_type as frequencyType,
              c.per_round_amount as perRoundAmount,
              c.goal_amount      as goalAmount,           -- 추가
              c.start_date     as startDate,
              c.end_date       as endDate,
              c.created_at     as createdAt,
              c.thumbnail_url  as thumbnailUrl,
              (select count(*) from challenge_participation p
                 where p.challenge_id = c.challenge_id and p.left_at is null) as participantCount,
              case
                when c.start_date > :today then 'PENDING'
                when c.end_date  < :today then 'COMPLETED'
                else 'ACTIVE'
              end as status,
              coalesce(sum_cf.total_amount,0) as totalAmount
            from challenge c
            left join sum_cf on sum_cf.challenge_id = c.challenge_id
            where c.is_deleted = false
              and (:category = 'ALL' or c.category_type = :category)
              and (
                :status is null
                or (:status = 'PENDING'  and :today < c.start_date)
                or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
                or (:status = 'COMPLETED' and c.end_date < :today)
              )
            order by totalAmount desc, createdAt desc
            """,
            countQuery = """
                      select count(*)
                      from challenge c
                      where c.is_deleted = false
                        and (:category = 'ALL' or c.category_type = :category)
                        and (
                          :status is null
                          or (:status = 'PENDING'  and :today < c.start_date)
                          or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
                          or (:status = 'COMPLETED' and c.end_date < :today)
                        )
                    """,
            nativeQuery = true)
    Page<ChallengeListItemView> searchDiscoverOrderByTotalAmount(
            String category, String status, java.time.LocalDate today, Pageable pageable);

}
