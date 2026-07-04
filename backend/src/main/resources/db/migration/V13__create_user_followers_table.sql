CREATE TABLE user_followers(
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Ensure a user can follow another user only once
    CONSTRAINT uq_follower_following UNIQUE(follower_id, following_id),

    -- Ensure a user doesn't follow themselves (constraint at DB level)
    CONSTRAINT chk_not_self_follow CHECK (follower_id != following_id)
);

-- Indexes for efficient queries
CREATE INDEX idx_user_followers_follower_id ON user_followers(follower_id);
CREATE INDEX idx_user_followers_following_id ON user_followers(following_id);