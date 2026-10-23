CREATE TABLE profile_links (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    label VARCHAR(100),
    url VARCHAR(2048) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_profile_links_profile
        FOREIGN KEY (profile_id) REFERENCES profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_profile_links_platform_normalized CHECK (
        platform = LOWER(TRIM(platform))
    ),
    CONSTRAINT ck_profile_links_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_profile_links_profile_order
    ON profile_links (profile_id, display_order);
