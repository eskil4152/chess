CREATE TABLE games (
    id UUID PRIMARY KEY NOT NULL,
    white UUID NOT NULL REFERENCES users(id),
    black UUID NOT NULL REFERENCES users(id),
    status VARCHAR(255) NOT NULL,
    moves TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);