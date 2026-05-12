CREATE TYPE article_status AS ENUM ('DRAFT', 'PUBLISHED','ARCHIVED');

CREATE TABLE articles(
    id          BIGSERIAL       PRIMARY KEY,
    heading     VARCHAR(255)    NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    content     TEXT            NOT NULL,
    summary     TEXT,
    slug        VARCHAR(255)    UNIQUE,
    author_id   BIGINT          NOT NULL,
    status      article_status  NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT fk_articles_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE SET NULL
    
);

CREATE INDEX idx_articles_author ON articles(author_id);
CREATE INDEX idx_articles_created_at ON articles(created_at);
