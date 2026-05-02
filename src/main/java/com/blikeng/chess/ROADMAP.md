# Chess Backend — Build Plan

## Current
- Add Dockerfile
- Add docker-compose file
- Add unit tests
- Add integration tests
- Add JaCoCo
- Add SonarCloud integration
- Add GitHub Actions workflow for testing and server hosting

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

## Phase 5 — Move History ✓
- Store moves as algebraic list on `Game`
- Enables reconnect state replay and game history endpoint

---

## Phase 6 — Persistence ✓
- `UserEntity`, `GameEntity` with Flyway migrations
- Move history entity — pending Phase 5

---

## Phase 7 — REST API ✓
- Auth endpoints ✓
- Games controller ✓
- User controller ✓

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

## Phase 10 — Matchmaking & ELO ✓
- Matchmaking with ELO-based pairing
- Queue/dequeue on connect/disconnect
- ELO update on game end — pending

---

## Phase 11 — Polish (in progress)
- Flyway migrations – pending
- Integration + unit tests — in progress
- Docker Compose — pending
- Logging ✓

---

## Phase 12 — Monitoring
- Prometheus metrics
- Micrometer metrics
- Sentry

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