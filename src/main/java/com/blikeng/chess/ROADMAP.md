# Chess Backend — Build Plan

## Phase 0 — Minimal Domain Setup
Goal: avoid rewrites later without overengineering.

- Define core classes:
    - `Board` (8x8 array)
    - `Piece` (type, color)
    - `Position` (row, col) — immutable record
    - `Move` (from: Position, to: Position, promotion: PieceType nullable)
- Define **GameState (lightweight)**:
    - current turn
    - lastMove (nullable)
    - castling rights (simple flags)
- Internal coordinate system: `row 0–7`, `col 0–7`. Row 0 = rank 8, row 7 = rank 1.
- Algebraic notation (`e2`) is input/output only — isolate parsing in a `PositionMapper` class.
- Engine never sees strings. Controllers convert DTO strings → `Move` before calling engine.

Sketch these in 30 minutes. Don't perfect them — they'll evolve as you build Phase 1.

---

## Phase 1 — Board & Movement (Pseudo-Legal Only)
Goal: pieces move correctly, no game rules yet.

### 1.1 Board
- Initialize standard chess position
- Print/debug board (CLI or logs)

### 1.2 Piece Movement
- Each piece generates **pseudo-legal moves**
    - Pawn (forward, capture, double move)
    - Knight
    - Bishop (sliding)
    - Rook (sliding)
    - Queen
    - King (1 square)

Ignore:
- check
- pins
- castling
- en passant

### 1.3 Move Execution
- Apply move if destination is in generated list
- Capture handling
- Switch turn (optional here or next phase)

End state: you can freely move pieces and they behave correctly.

---

## Phase 2 — Turn + Basic Game Flow
Goal: make it feel like a game.

### 2.1 Turn Enforcement
- Track current player
- Reject moving opponent pieces

### 2.2 King Tracking
- Always know each king's position
- Utility: "is square attacked?"

### 2.3 Check Detection
- Detect if current player is in check
- DO NOT block illegal moves yet

Add to GameState:
- `whiteKingPos`, `blackKingPos`

End state: game knows when a king is under attack.

---

## Phase 3 — Legal Moves (Core Engine)
Goal: enforce real chess rules.

### 3.1 Move Validation
- Simulate move → reject if own king ends in check
- This automatically handles:
    - pinned pieces
    - discovered checks

### 3.2 Legal Move Generation
- Filter pseudo-legal moves → legal moves

### 3.3 Game End
- Checkmate (in check + no legal moves)
- Stalemate (not in check + no legal moves)

End state: fully playable chess (no special moves yet).

---

## Phase 4 — Special Moves
Goal: complete core chess rules.

### 4.1 Castling
- Track:
    - king/rook moved flags
- Validate:
    - not in check
    - path not attacked
    - path empty

### 4.2 En Passant
- Use `lastMove`
- Only valid immediately after double pawn move
- Must still pass king safety check

### 4.3 Pawn Promotion
- Support:
    - Queen, Rook, Bishop, Knight
- Require promotion choice in move

End state: full chess rules implemented.

---

## Phase 5 — State Robustness (Future-Proofing)
Goal: avoid painful refactors later.

- Introduce **move history list**
- Optional: store moves in simple notation
- Improve GameState:
    - halfmove clock (for future 50-move rule)
    - fullmove number

Do NOT implement draw rules yet—just prepare.

---

## Phase 6 — Persistence
Goal: save and restore games cleanly.

- Player entity
- Game entity:
    - serialized board (or FEN-like string)
    - current turn
    - status
- Move history entity (optional but recommended)

Tip:
- Keep engine independent of DB

---

## Phase 7 — REST API
- POST `/games`
- GET `/games/{id}`
- POST `/games/{id}/moves`
- GET `/games/{id}/moves`

### Validation
- Player is part of game
- Correct turn
- Move is legal

### Response
- Updated board/state
- Game status

---

## Phase 8 — Concurrency Safety
Goal: prevent broken games.

- Add version field (optimistic locking)
- Ensure:
    - move validation + save is atomic
- Prevent:
    - double moves
    - race conditions

---

## Phase 9 — WebSocket
- `/ws/game/{id}`

- Broadcast:
    - moves
    - updated state
    - game over

- Handle:
    - reconnect (client refetch via REST)

---

## Phase 10 — Matchmaking & ELO
- Queue endpoint
- Pair players → create game

### Add:
- Cancel queue
- Timeout handling

### ELO
- Simple formula (configurable K-factor)
- Update on game end

---

## Phase 11 — Polish
- Flyway migrations
- Docker Compose
- Integration tests
- Logging

---

## Phase X — Later (Optional)
Add only when core is stable:

- Threefold repetition
- 50-move rule
- Insufficient material
- Draw offers / resignation
- Time controls (clocks)
- FEN import/export
- PGN export
- Spectator mode
- Bot opponent

---

## Guiding Principles
- Each phase must be **runnable and visible**
- Never mix:
    - engine bugs
    - database bugs
- Build **pseudo-legal → legal**, not everything at once
- Keep engine pure (no framework dependencies)