-- Phase 3: HR processing and deduction tracking
ALTER TABLE vacation_requests
    ADD COLUMN processed_at TIMESTAMPTZ,
    ADD COLUMN hr_id UUID REFERENCES users(id),
    ADD COLUMN external_deduction_status VARCHAR(30);

ALTER TABLE vacation_requests
    ADD CONSTRAINT ck_external_deduction_status
        CHECK (external_deduction_status IN ('SUCCESS', 'FAILED') OR external_deduction_status IS NULL);

CREATE INDEX IF NOT EXISTS idx_vacation_status_processed ON vacation_requests(status, processed_at);
CREATE INDEX IF NOT EXISTS idx_vacation_hr ON vacation_requests(hr_id);
