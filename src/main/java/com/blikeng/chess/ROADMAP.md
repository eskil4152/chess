# Chess Backend — Build Plan

## Current
- Add games controller. Should handle game retrieval, history etc.
- Add user controller. 
- Add some form of ELO. Simple +-1 would suffice for now.
- Convert MoveHistory to an algebraic list

---

## Phase 1 — Board & Movement ✓
- Standard board initialization
- Pseudo-legal move generation for all pieces
- Move execution and capture handling

---

## Phase 2 — Turn + Basic Game Flow ✓
- Turn enforcement
- King position tracking
- Square attacked detection

---

## Phase 3 — Legal Moves ✓
- Move simulation (reject if own king in check)
- Checkmate and stalemate detection

---

## Phase 4 — Special Moves ✓
- Castling (path clear, not in check, rooks/king unmoved)
- En passant
- Pawn promotion

---

## Phase 5 — Move History (Pending)
- Store moves as algebraic list on `Game`
- Enables reconnect state replay and game history endpoint

---

## Phase 6 — Persistence ✓ (partial)
- `UserEntity`, `GameEntity` with Flyway migrations
- Move history entity — pending Phase 5

---

## Phase 7 — REST API (partial)
- Auth endpoints ✓
- Games controller — pending (see Current)
- User controller — pending (see Current)
- Move submission is over WebSocket, not REST

---

## Phase 8 — Concurrency Safety ✓
- `ReentrantLock` per game for atomic move validation

---

## Phase 9 — WebSocket ✓
- Single `/ws` endpoint
- Broadcasts: moves, game start, game over
- Reconnect: sends `WsGameStarted` — full state replay pending Phase 5
- Session management via `PresenceService`
- Notifications via `NotificationService` with `@TransactionalEventListener`

---

## Phase 10 — Matchmaking & ELO (partial)
- Matchmaking with ELO-based pairing ✓
- Queue/dequeue on connect/disconnect ✓
- ELO update on game end — pending (see Current)

---

## Phase 11 — Polish (in progress)
- Flyway migrations ✓
- Integration + unit tests — in progress
- Docker Compose — pending
- Logging — pending

---

## Phase X — Later (Optional)
- Threefold repetition
- 50-move rule
- Insufficient material
- Draw offers / resignation
- Time controls
- FEN import/export
- PGN export
- Spectator mode
- Bot opponent

---

## Guiding Principles
- Each phase must be **runnable and visible**
- Never mix engine bugs and database bugs
- Build **pseudo-legal → legal**, not everything at once
- Keep engine pure (no framework dependencies)