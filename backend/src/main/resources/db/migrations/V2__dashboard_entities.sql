-- Dashboard-related entities for Phase 1
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS integration_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(60) NOT NULL,
    state VARCHAR(30) NOT NULL DEFAULT 'CONFIGURED',
    endpoint_url TEXT NOT NULL,
    auth_token TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_integration_state CHECK (state IN ('CONFIGURED', 'DISABLED'))
);
CREATE INDEX IF NOT EXISTS idx_integration_type_state ON integration_configs(type, state);

CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(140) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_team_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE TABLE IF NOT EXISTS team_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_membership UNIQUE (team_id, user_id),
    CONSTRAINT ck_team_membership_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX IF NOT EXISTS idx_team_membership_user ON team_memberships(user_id, status);

CREATE TABLE IF NOT EXISTS vacation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    manager_notes TEXT,
    hr_notes TEXT,
    CONSTRAINT ck_vacation_request_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'DENIED', 'CANCELED', 'PROCESSED'))
);
CREATE INDEX IF NOT EXISTS idx_vacation_user_dates ON vacation_requests(user_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_vacation_status_dates ON vacation_requests(status, start_date, end_date);

CREATE TABLE IF NOT EXISTS holidays (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holiday_date DATE NOT NULL,
    name VARCHAR(160) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'IMPORTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_holiday_date_name UNIQUE (holiday_date, name),
    CONSTRAINT ck_holiday_status CHECK (status IN ('IMPORTED', 'DEPRECATED'))
);
CREATE INDEX IF NOT EXISTS idx_holiday_date ON holidays(holiday_date);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID,
    action_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(120),
    entity_id VARCHAR(120),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action_type, created_at);
