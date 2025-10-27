create table if not exists auth_refresh_token (
    id          bigserial primary key,
    user_id     bigint not null,
    token       varchar(512) not null unique,
    expires_at  timestamp not null,
    revoked_at  timestamp null
    );

create index if not exists idx_rt_user on auth_refresh_token(user_id);
