CREATE TABLE projects (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    slug VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    summary VARCHAR(500),
    description TEXT,
    source_code_url VARCHAR(2048),
    live_url VARCHAR(2048),
    cover_image_url VARCHAR(2048),
    started_on DATE,
    completed_on DATE,
    display_order INTEGER NOT NULL DEFAULT 0,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_projects_portfolio_slug UNIQUE (portfolio_id, slug),
    CONSTRAINT fk_projects_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
    CONSTRAINT ck_projects_slug_normalized CHECK (slug = LOWER(TRIM(slug))),
    CONSTRAINT ck_projects_dates CHECK (
        started_on IS NULL OR completed_on IS NULL OR completed_on >= started_on
    ),
    CONSTRAINT ck_projects_display_order CHECK (display_order >= 0),
    CONSTRAINT ck_projects_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_projects_portfolio_order ON projects (portfolio_id, display_order);
CREATE INDEX idx_projects_portfolio_featured ON projects (portfolio_id, featured);
