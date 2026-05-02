CREATE table users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    bio TEXT,
    email VARCHAR(255),
    avatarUrl VARCHAR(255),
    elo INT NOT NULL DEFAULT 800
);