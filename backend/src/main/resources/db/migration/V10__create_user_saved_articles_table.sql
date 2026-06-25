CREATE TABLE user_saved_articles(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    article_id BIGINT NOT NULL REFERENCES articles(id),
    saved_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_saved_article UNIQUE(user_id, article_id)
);

CREATE INDEX idx_user_saved_articles_user_id ON user_saved_articles(user_id);