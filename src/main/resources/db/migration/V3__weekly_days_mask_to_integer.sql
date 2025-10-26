ALTER TABLE challenge
ALTER COLUMN weekly_days_mask TYPE integer
  USING weekly_days_mask::integer;
