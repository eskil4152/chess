CREATE TABLE friend_requests (
    id UUID PRIMARY KEY NOT NULL,
    from_user UUID NOT NULL REFERENCES users(id),
    to_user UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);