CREATE TABLE educations (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    institution_name VARCHAR(200) NOT NULL,
    degree VARCHAR(150),
    field_of_study VARCHAR(150),
    description TEXT,
    started_on DATE,
    ended_on DATE,
    current_education BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_educations_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_educations_dates CHECK (
        started_on IS NULL OR ended_on IS NULL OR ended_on >= started_on
    ),
    CONSTRAINT ck_educations_current CHECK (
        NOT current_education OR ended_on IS NULL
    ),
    CONSTRAINT ck_educations_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_educations_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_educations_profile_order
    ON educations (profile_id, display_order);
