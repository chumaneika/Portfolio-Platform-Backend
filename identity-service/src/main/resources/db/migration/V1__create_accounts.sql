CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_accounts_email UNIQUE (email),
    CONSTRAINT uq_accounts_username UNIQUE (username),
    CONSTRAINT ck_accounts_email_normalized CHECK (email = LOWER(TRIM(email))),
    CONSTRAINT ck_accounts_username_normalized CHECK (username = LOWER(TRIM(username))),
    CONSTRAINT ck_accounts_status CHECK (
        status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED')
    ),
    CONSTRAINT ck_accounts_failed_login_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_accounts_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_accounts_status ON accounts (status);
