-- USER
create table user_account (
                              user_id bigserial primary key,
                              provider varchar(16) not null default 'KAKAO',
                              provider_user_id varchar(64) not null unique,
                              nickname varchar(40) not null,
                              is_deleted boolean not null default false,
                              created_at timestamptz not null default now(),
                              updated_at timestamptz not null default now()
);

-- CHALLENGE
create table challenge (
                           challenge_id bigserial primary key,
                           creator_user_id bigint not null references user_account(user_id),
                           title varchar(50) not null,
                           description varchar(500),
                           category_type varchar(16) not null, -- SAVING/INSTALLMENT/OTHER
                           start_date date not null,
                           end_date date not null,
                           frequency_type varchar(16) not null, -- DAILY/WEEKLY
                           weekly_days_mask smallint not null default 0,
                           per_round_amount bigint not null,
                           goal_amount bigint not null,
                           thumbnail_url varchar(255),
                           is_deleted boolean not null default false,
                           created_at timestamptz not null default now(),
                           updated_at timestamptz not null default now(),
                           check (start_date <= end_date)
);
create index idx_ch_period on challenge(start_date, end_date);
create index idx_ch_category on challenge(category_type);

-- ROUND
create table challenge_round (
                                 round_id bigserial primary key,
                                 challenge_id bigint not null references challenge(challenge_id) on delete cascade,
                                 round_no int not null,
                                 scheduled_date date not null,
                                 base_amount bigint not null,
                                 created_at timestamptz not null default now(),
                                 unique (challenge_id, round_no),
                                 unique (challenge_id, scheduled_date)
);
create index idx_round_schedule on challenge_round(challenge_id, scheduled_date);

-- PARTICIPATION
create table challenge_participation (
                                         participation_id bigserial primary key,
                                         challenge_id bigint not null references challenge(challenge_id) on delete cascade,
                                         user_id bigint not null references user_account(user_id),
                                         role_type varchar(16) not null default 'MEMBER',
                                         joined_at timestamptz not null default now(),
                                         left_at timestamptz null,
                                         created_at timestamptz not null default now(),
                                         updated_at timestamptz not null default now()
);
create index idx_part_user_ch on challenge_participation(challenge_id, user_id);

-- CERTIFICATION
create table certification (
                               certification_id bigserial primary key,
                               participation_id bigint not null references challenge_participation(participation_id) on delete cascade,
                               round_id bigint not null references challenge_round(round_id) on delete cascade,
                               amount bigint not null,
                               comment varchar(200),
                               image_url varchar(255),
                               double_yn varchar(1) not null default 'N',
                               certification_status varchar(16) not null default 'SUBMITTED',
                               certified_at timestamptz not null default now(),
                               created_at timestamptz not null default now(),
                               updated_at timestamptz not null default now(),
                               unique (participation_id, round_id)
);
create index idx_cert_part on certification(participation_id, certified_at);
create index idx_cert_round on certification(round_id, certification_status);
