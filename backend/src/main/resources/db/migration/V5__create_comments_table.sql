CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    article_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,

    content     TEXT NOT NULL,

    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comments_article
        FOREIGN KEY (article_id)
        REFERENCES articles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_user_article_comment UNIQUE (user_id, article_id)
);