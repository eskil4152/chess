-- Back the player-stats / game-history lookups, which filter games by participant.
-- Composite (player, status) lets the stats query (white/black id + status <> 'ONGOING')
-- be served from the index instead of scanning the games table.
CREATE INDEX IF NOT EXISTS idx_games_white_status ON games (white, status);
CREATE INDEX IF NOT EXISTS idx_games_black_status ON games (black, status);