-- 1) 필드 추가
ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS username VARCHAR(50) UNIQUE,
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100),
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- 2) 인덱스
CREATE INDEX IF NOT EXISTS idx_user_account_username ON user_account(username);

-- 3) pgcrypto 확장 (bcrypt 해시용)
CREATE EXTENSION IF NOT EXISTS pgcrypto;