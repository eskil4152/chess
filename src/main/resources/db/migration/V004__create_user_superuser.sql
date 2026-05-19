INSERT INTO users
    (id, username, password, bio, email, avatarUrl, elo, role, games, been2400)
VALUES
    (
        gen_random_uuid(),
        'superuser',
        '${admin_password}',
        'There can only be one.',
        null,
        null,
        1000,
        'SUPERUSER',
        0,
        false
    )
;