CREATE TABLE work_experiences (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    position_title VARCHAR(150) NOT NULL,
    employment_type VARCHAR(30),
    location VARCHAR(200),
    description TEXT,
    started_on DATE NOT NULL,
    ended_on DATE,
    current_position BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_work_experiences_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_work_experiences_employment_type CHECK (
        employment_type IS NULL OR employment_type IN (
            'FULL_TIME', 'PART_TIME', 'CONTRACT', 'FREELANCE', 'INTERNSHIP'
        )
    ),
    CONSTRAINT ck_work_experiences_dates CHECK (
        ended_on IS NULL OR ended_on >= started_on
    ),
    CONSTRAINT ck_work_experiences_current_position CHECK (
        NOT current_position OR ended_on IS NULL
    ),
    CONSTRAINT ck_work_experiences_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_work_experiences_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_work_experiences_profile_order
    ON work_experiences (profile_id, display_order);
