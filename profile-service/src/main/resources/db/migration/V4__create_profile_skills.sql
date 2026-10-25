CREATE TABLE profile_skills (
    profile_id UUID NOT NULL,
    skill_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    display_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_profile_skills PRIMARY KEY (profile_id, skill_code),
    CONSTRAINT fk_profile_skills_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_profile_skills_code_normalized CHECK (
        skill_code = LOWER(TRIM(skill_code))
    ),
    CONSTRAINT ck_profile_skills_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_profile_skills_profile_order
    ON profile_skills (profile_id, display_order);
