CREATE TABLE profiles (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    headline VARCHAR(200),
    bio TEXT,
    location VARCHAR(200),
    avatar_url VARCHAR(2048),
    resume_url VARCHAR(2048),
    available_for_work BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_profiles_account_id UNIQUE (account_id),
    CONSTRAINT ck_profiles_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_profiles_available_for_work ON profiles (available_for_work);
