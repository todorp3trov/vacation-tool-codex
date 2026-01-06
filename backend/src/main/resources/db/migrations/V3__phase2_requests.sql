-- Phase 2: request lifecycle fields and day counts
ALTER TABLE vacation_requests
    ADD COLUMN number_of_days INT NOT NULL DEFAULT 0,
    ADD COLUMN submitted_at TIMESTAMPTZ,
    ADD COLUMN request_code VARCHAR(80);

UPDATE vacation_requests
SET number_of_days = GREATEST((end_date - start_date) + 1, 0);

ALTER TABLE vacation_requests
    ALTER COLUMN number_of_days DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_vacation_user_status ON vacation_requests(user_id, status);
