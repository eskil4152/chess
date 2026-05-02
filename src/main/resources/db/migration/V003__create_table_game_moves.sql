CREATE TABLE game_moves (
    game_id UUID NOT NULL REFERENCES games(id),
    moves VARCHAR(5) NOT NULL
);