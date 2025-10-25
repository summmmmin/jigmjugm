-- V2: Add email column to user_account table
-- Author: System
-- Date: 2025-10-25
-- Description: 사용자 이메일 정보를 저장하기 위한 컬럼 추가

-- 이메일 컬럼 추가 (nullable로 시작 - 기존 사용자 데이터 보호)
ALTER TABLE user_account 
ADD COLUMN email varchar(100);

-- 이메일 유니크 인덱스 추가 (null 허용)
CREATE UNIQUE INDEX idx_user_email 
ON user_account(email) 
WHERE email IS NOT NULL;

-- 이메일 인증 관련 컬럼 추가 (향후 이메일 인증 기능용)
ALTER TABLE user_account 
ADD COLUMN email_verified boolean NOT NULL DEFAULT false;

ALTER TABLE user_account 
ADD COLUMN email_verified_at timestamptz;

-- 코멘트 추가
COMMENT ON COLUMN user_account.email IS '사용자 이메일 주소';
COMMENT ON COLUMN user_account.email_verified IS '이메일 인증 여부';
COMMENT ON COLUMN user_account.email_verified_at IS '이메일 인증 일시';