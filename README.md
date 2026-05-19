# Chess

A real-time multiplayer chess server built with Spring Boot and Java.
Full-rule chess gameplay over WebSockets, auto-matchmaking by Elo rating with selectable time controls, bot opponents at three difficulty levels, a friends system, resign and draw-by-agreement, PGN export, persistent game history, and JWT-based authentication —
with a complete chess engine written from scratch: move generation, legal move validation, all draw conditions
(50-move rule, threefold repetition, insufficient material, stalemate), SAN/PGN conversion, and a minimax evaluator.
No external chess libraries.

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=alert_status&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=reliability_rating&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=security_rating&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=sqale_rating&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=coverage&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
    - [Authentication & Security](#authentication--security)
    - [Matchmaking & Time Controls](#matchmaking--time-controls)
    - [Bot Opponents](#bot-opponents)
    - [Friends](#friends)
    - [Challenges](#challenges)
    - [Chess Engine](#chess-engine)
    - [WebSocket & Real-Time Gameplay](#websocket--real-time-gameplay)
    - [Evaluator](#evaluator)
    - [PGN & FEN Export](#pgn--fen-export)
    - [Elo Rating](#elo-rating)
    - [Database & Persistence](#database--persistence)
- [API Overview](#api-overview)
- [CI/CD Pipeline](#cicd-pipeline)
- [Testing](#testing)

---

## Tech Stack

| Layer            | Technology                                |
|------------------|-------------------------------------------|
| Language         | Java 25                                   |
| Framework        | Spring Boot                               |
| Real-time        | WebSockets (Spring WebSocket)             |
| Auth             | JWT (HTTP-only cookie, `SameSite=Strict`) |
| Password Hashing | BCrypt                                    |
| Database         | PostgreSQL (schema managed via Flyway)    |
| CI/CD            | GitHub Actions (self-hosted runner)       |
| Monitoring       | OpenTelemetry (OTLP), Grafana Cloud       |
| Code Quality     | SonarCloud                                |
| Rate Limiting    | Caffeine, Bucket4j                        |
| Testing          | JUnit 5, Mockito, AssertJ                 |

---

## Architecture

The server is structured around three main concerns:

- **HTTP layer** — REST endpoints for authentication, user profiles, queue management, and game history.
- **WebSocket layer** — a single `/ws` endpoint handles all real-time gameplay. Moves, resignations, and draw offers are sent and broadcast over the same connection.
- **Engine layer** — a self-contained chess engine with no external dependencies, responsible for move generation, legal move validation, game-state tracking, position evaluation, and PGN/SAN conversion.

Game state is kept in memory during a match (one `Game` object per active game, protected by a `ReentrantLock`). Completed games are persisted to PostgreSQL with their full move list in PGN format.

---

## Features

### Authentication & Security

**JWT Authentication**
- Registration and login issue a signed JWT stored as an `HttpOnly`, `SameSite=Strict` cookie with a 24-hour expiration.
- The token encodes `userID` as the subject and `username` as a claim.
- A `JwtAuthFilter` validates every HTTP request.

**Password Handling**
- Passwords are hashed with **BCrypt**; plaintext is never persisted.
- Minimum length is enforced for both usernames and passwords at registration.

**Rate Limiting**
- All API endpoints are protected by a token bucket rate limiter backed by **Caffeine** and **Bucket4j**.
- Login and register endpoints have tighter per-IP limits than general endpoints.
- Requests that exceed the limit receive a `429 Too Many Requests` response.

**WebSocket Handshake Authentication**
- An `AuthHandshakeInterceptor` validates the JWT cookie before any WebSocket connection is established.
- `userID` and `username` are injected into the WebSocket session attributes after validation. Unauthenticated connections are rejected.

---

### Matchmaking & Time Controls

- Players join the queue via `POST /api/queue` with a chosen time control (e.g. `BLITZ_5_0`). The queue finds the closest Elo match within a **200-point window** for the same time control.
- If no suitable opponent is found, the player waits until one connects.
- The queue is backed by a `ConcurrentHashMap` and all match decisions are made inside a `synchronized` block to prevent race conditions.
- Players can leave the queue via `DELETE /api/queue` or on disconnect if not yet matched.

**Supported time controls**

| Category | Name          | Initial | Increment |
|----------|---------------|---------|-----------|
| Bullet   | `BULLET_1_0`  | 1 min   | 0 s       |
| Bullet   | `BULLET_1_1`  | 1 min   | 1 s       |
| Bullet   | `BULLET_2_0`  | 2 min   | 0 s       |
| Blitz    | `BLITZ_3_0`   | 3 min   | 0 s       |
| Blitz    | `BLITZ_3_2`   | 3 min   | 2 s       |
| Blitz    | `BLITZ_5_0`   | 5 min   | 0 s       |
| Rapid    | `RAPID_10_0`  | 10 min  | 0 s       |
| Rapid    | `RAPID_10_5`  | 10 min  | 5 s       |
| Rapid    | `RAPID_15_0`  | 15 min  | 0 s       |
| Rapid    | `RAPID_15_10` | 15 min  | 10 s      |
| Rapid    | `RAPID_30_0`  | 30 min  | 0 s       |
| Rapid    | `RAPID_60_0`  | 60 min  | 0 s       |

A player who runs out of time loses; the game ends with `TIMEOUT` as the reason in the `GAME_ENDED` event.

---

### Bot Opponents

Players can start a game against a computer opponent at three difficulty levels via `POST /api/bot/{difficulty}`.

| Difficulty | Search Depth | Noise |
|------------|--------------|-------|
| `EASY`     | 1            | ±300  |
| `MEDIUM`   | 2            | ±100  |
| `HARD`     | 3            | none  |

- Bot moves are calculated by the built-in minimax evaluator and scheduled asynchronously on a thread pool, with a short artificial delay to avoid instant responses.
- The bot reacts to `MatchStartedEvent` (when playing white) and `MoveMadeEvent` (on each subsequent turn) via `@TransactionalEventListener`, so moves are never scheduled before the preceding transaction commits.
- Bot games use the same game flow as human games: all draw conditions, resignations, and PGN persistence apply normally.

---

### Friends

- Users can maintain a friends list: add and remove friends by username, and retrieve the full list.
- Backed by a `friends` join table with a composite primary key.

---

### Challenges

Players can challenge friends to a game directly over WebSocket, bypassing the matchmaking queue.

| Direction       | Type                  | Description                                                        |
|-----------------|-----------------------|--------------------------------------------------------------------|
| Client → Server | `CHALLENGE`           | Send a challenge to a friend with a chosen time control            |
| Client → Server | `CHALLENGE_RESPONSE`  | Accept or decline an incoming challenge                            |
| Client → Server | `CANCEL_CHALLENGE`    | Cancel a pending outgoing challenge                                |
| Server → Client | `CHALLENGE`           | Delivered to the challenged player                                 |
| Server → Client | `CHALLENGE_DECLINED`  | Notifies the challenger when their challenge is declined           |
| Server → Client | `CHALLENGE_CANCELLED` | Notifies the challenged player when the challenger cancels         |
| Server → Client | `CHALLENGE_EXPIRED`   | Sent to both parties when a challenge times out without a response |

- Duplicate challenges to the same player are rejected.
- Challenges expire automatically after a timeout.
- Accepted challenges start a game using the same flow as matched games.

---

### Chess Engine

The chess engine is written entirely from scratch with no external chess libraries.

**Move Generation**
- Pseudo-legal moves are generated per piece type: sliding moves (rook, bishop, queen), knight jumps, king adjacency, and pawn pushes/captures.
- Castling is included in king move generation: both kingside and queenside, for both colors, with correct occupancy checks.
- En passant targets are tracked on the `Game` object and included in pawn moves when applicable.

**Legal Move Filtering**
- A move is legal only if it does not leave the moving side's king in check.
- This is enforced by copying the board, applying the move, and testing whether the king's square is attacked.
- The copy-and-test approach handles pins, discovered checks, and king moves into attacked squares correctly.

**Special Rules**
- **En passant**: the captured pawn is removed from its original square, not the destination square.
- **Castling**: transit squares are checked for attack in addition to the king's source square. The rook is relocated only if `canCastle` passes.
- **Pawn promotion**: promotion piece must be supplied for any pawn reaching the back rank. Supported: queen, rook, bishop, knight.

**Draw Detection**
- **50-move rule**: the half-move clock increments on every non-pawn, non-capture move and resets on any pawn move or capture. At 100 half-moves the game ends in a draw.
- **Threefold repetition**: every position is hashed as board state + side to move + en passant target + castling rights (derived from king/rook `hasMoved` flags). When the same hash appears three times the game ends in a draw.
- **Insufficient material**: draw is declared immediately when neither side can force checkmate — K vs K, K+B vs K, K+N vs K, and K+B vs K+B with bishops on the same square color.
- **Stalemate**: detected as part of the legal-move scan; the side to move has no legal moves and is not in check.

**Game State**
- Each active game holds its board, king positions, en passant target, half-move clock, position history, turn, and full move list.
- All game-over detection runs after every move before the turn switches, so draws are awarded immediately on the move that causes them.
- Per-game `ReentrantLock` prevents concurrent move submissions from corrupting state.

---

### WebSocket & Real-Time Gameplay

Moves are submitted in **UCI notation** — a 4-character string for normal moves (`e2e4`) and 5 characters for promotions (`e7e8q`).

**WebSocket Events**

| Direction       | Type           | Description                                                                                                                                                                                    |
|-----------------|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Client → Server | `MOVE`         | Submit a move in UCI notation (e.g. `e2e4`, `e7e8q`)                                                                                                                                           |
| Client → Server | `RESIGN`       | Forfeit the game; the opponent is awarded the win                                                                                                                                              |
| Client → Server | `OFFER_DRAW`   | Propose a draw; the game ends when both players have sent this                                                                                                                                 |
| Server → Client | `GAME_STARTED` | Match found — includes game ID, player IDs, usernames, and Elo ratings                                                                                                                         |
| Server → Client | `MOVE`         | Move broadcast to both players after a valid move                                                                                                                                              |
| Server → Client | `OFFER_DRAW`   | Forwarded to the opponent when one player offers a draw                                                                                                                                        |
| Server → Client | `GAME_ENDED`   | Game over — includes result, how it ended (`CHECKMATE`, `STALEMATE`, `RESIGNATION`, `AGREEMENT`, `FIFTY_MOVE_RULE`, `REPETITION`, `INSUFFICIENT_MATERIAL`, `TIMEOUT`), and updated Elo ratings |
| Server → Client | `GAME_STATE`   | Full game state sent to a player who reconnects mid-game                                                                                                                                       |
| Server → Client | `ERROR`        | Structured error with HTTP status code and message                                                                                                                                             |

**Reconnection**
- On connect, if the user already has an active game, the full move history is sent as a `GAME_STATE` event so the client can reconstruct the board without server-side board serialization.
- Active game state is also available via `GET /api/games/active`.

**Keep-Alive**
- The server sends a WebSocket `PingMessage` to all open sessions every 20 seconds to keep connections alive and evict stale sessions.

**Presence**
- Sessions are tracked per user in a `ConcurrentHashMap`. A player is considered connected as long as at least one session is open.
- On disconnect, if the user has no remaining sessions, they are dequeued from matchmaking.

---

### Evaluator

The evaluator provides a static position score and a minimax search for finding the best move. All evaluation is relative to white (positive = white advantage).

**Static Evaluation**

Three components contribute to the score:

- **Material** — piece values based on Kaufman's revised values (P=100, N=B=350, R=525, Q=1000), with a +50 bishop pair bonus for holding both bishops.
- **Pawn structure** — penalizes doubled pawns (multiple pawns on the same file), isolated pawns (no friendly pawns on adjacent files), and blocked pawns (piece directly ahead). Each penalty is weighted at ×50.
- **Mobility** — pseudo-legal move count difference between white and black, weighted at ×10.

**Minimax Search**

- Standard minimax with configurable depth.
- White maximizes, black minimizes.
- Illegal moves (those that leave the king in check) are filtered out during search by checking `performMove` return value.
- All four promotion types are considered at promotion squares; the engine picks the best one.
- `getBestMove` returns the best root-level `Move` together with its promotion piece (if any) and the evaluated score, via the `MoveEval` record.

---

### PGN & FEN Export

Completed games are stored with their move list converted to **PGN** format using a built-in SAN converter.

- `SanConverter` translates UCI moves into Standard Algebraic Notation, including disambiguation for ambiguous pieces, check (`+`) and checkmate (`#`) markers, and promotion notation.
- `PgnConverter` wraps the SAN move list into a numbered PGN string (e.g. `1. e4 e5 2. Nf3 ...`).
- The full PGN is returned when fetching a completed game via `GET /api/games/{id}`.
- `FenConverter` serializes any game state to a standard **FEN** string, encoding the board, active color, castling rights, en passant target, half-move clock, and full move number.

---

### Elo Rating

- Elo is updated at the end of every game, regardless of how it ends (checkmate, stalemate, resignation, draw by agreement, 50-move rule, threefold repetition, or insufficient material).
- Win: +1, Draw: +0, Loss: −1.
- Both players' updated ratings are included in the `GAME_ENDED` WebSocket event.

---

### Database & Persistence

**Schema Management with Flyway**
- All tables are version-controlled through Flyway migration scripts, applied automatically on startup.

**Relational Modeling**
- `users` stores credentials, Elo, bio, and avatar URL.
- `games` stores the result, how the game ended, start time, white/black player references, and the full move list in PGN format.

**DTOs**
- All API responses use Data Transfer Objects to decouple the API surface from the internal entity structure.

---

## API Overview

| Method | Endpoint                      | Description                                              |
|--------|-------------------------------|----------------------------------------------------------|
| POST   | /api/auth/register            | Register                                                 |
| POST   | /api/auth/login               | Login                                                    |
| POST   | /api/auth/logout              | Logout                                                   |
| GET    | /api/auth                     | Check auth status                                        |
| GET    | /api/user/{username}          | Get user profile (username, Elo, bio)                    |
| PATCH  | /api/user/edit                | Update the current user's profile (bio, avatar URL)      |
| PATCH  | /api/user/edit-password       | Change the current user's password                       |
| POST   | /api/queue                    | Join the matchmaking queue (body: `{ timeControl }`)     |
| DELETE | /api/queue                    | Leave the matchmaking queue                              |
| POST   | /api/bot/{difficulty}         | Start a game against a bot (`EASY`, `MEDIUM`, `HARD`)    |
| GET    | /api/friends                  | Get the current user's friend list                       |
| POST   | /api/friends/add              | Add a friend by username                                 |
| DELETE | /api/friends/remove           | Remove a friend by username                              |
| GET    | /api/games/active             | Get the current user's active game state                 |
| GET    | /api/games/user/{username}    | Get game history for a user                              |
| GET    | /api/games/{id}               | Get a specific game by ID (includes PGN)                 |
| WS     | /ws                           | WebSocket endpoint                                       |

---

## Monitoring

Application metrics are collected via the **OpenTelemetry Java agent** (attached at startup as a `-javaagent`) and exported to **Grafana Cloud** using the OTLP protocol. This covers JVM metrics, HTTP request rates, latency, and error counts with no code changes required.

---

## CI/CD Pipeline

One GitHub Actions workflow runs on the repository on every push to `main`:

**`testing.yml`** — runs on a self-hosted runner:
- Sets up JDK 25
- Executes the full Maven test suite
- Updates SonarCloud analysis with JaCoCo coverage report

```
Push to main
    │
    ▼
Run Tests (mvn clean verify)
    │
    ▼
Update SonarCloud Analysis
```

---

## Testing

The project has near-complete unit test coverage (>99%) across all layers — engine, service, security, and controllers.

Tests use an in-memory H2 database, so no external dependencies are required to run the suite.
