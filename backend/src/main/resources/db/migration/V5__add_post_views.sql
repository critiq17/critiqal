CREATE TABLE post_views (
    post_id     BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    last_viewed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_post_views PRIMARY KEY (post_id, user_id),
    CONSTRAINT fk_post_views_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_views_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);