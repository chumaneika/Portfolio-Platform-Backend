CREATE TABLE project_technologies (
    project_id UUID NOT NULL,
    technology_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_project_technologies PRIMARY KEY (project_id, technology_code),
    CONSTRAINT fk_project_technologies_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT ck_project_technologies_code_normalized CHECK (
        technology_code = LOWER(TRIM(technology_code))
    ),
    CONSTRAINT ck_project_technologies_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_project_technologies_order
    ON project_technologies (project_id, display_order);
