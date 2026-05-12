CREATE TABLE ratings (
    id          BIGSERIAL PRIMARY KEY,
    article_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,

    rating      SMALLINT NOT NULL CHECK (rating >=0 AND rating <=5),

    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ratings_article
        FOREIGN KEY (article_id)
        REFERENCES articles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ratings_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_user_article_rating UNIQUE (user_id, article_id)
);