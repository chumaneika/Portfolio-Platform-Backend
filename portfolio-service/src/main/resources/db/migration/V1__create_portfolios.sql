CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    owner_account_id UUID NOT NULL,
    slug VARCHAR(100) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_portfolios_slug UNIQUE (slug),
    CONSTRAINT ck_portfolios_slug_normalized CHECK (slug = LOWER(TRIM(slug))),
    CONSTRAINT ck_portfolios_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_portfolios_publication CHECK (
        status <> 'PUBLISHED' OR published_at IS NOT NULL
    ),
    CONSTRAINT ck_portfolios_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_portfolios_owner_account_id ON portfolios (owner_account_id);
CREATE INDEX idx_portfolios_status ON portfolios (status);
