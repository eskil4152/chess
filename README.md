# Chess — with Evaluator

A real-time multiplayer chess server built with Spring Boot and Java.
It supports auto-matchmaking by Elo rating, full-rule chess gameplay over WebSockets,
persistent game history, and JWT-based authentication — with a chess engine and minimax evaluator
written from scratch, no external chess libraries.

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
  - [Matchmaking](#matchmaking)
  - [Chess Engine](#chess-engine)
  - [WebSocket & Real-Time Gameplay](#websocket--real-time-gameplay)
  - [Evaluator](#evaluator)
  - [Elo Rating](#elo-rating)
  - [Database & Persistence](#database--persistence)
- [API Overview](#api-overview)
- [CI/CD Pipeline](#cicd-pipeline)
- [Testing](#testing)
- [License](#license)

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
| Code Quality     | SonarCloud                                |
| Testing          | JUnit 5, Mockito, AssertJ                 |

---

## Architecture

The server is structured around three main concerns:

- **HTTP layer** — REST endpoints for authentication, user profiles, and game history.
- **WebSocket layer** — a single `/ws` endpoint handles all real-time gameplay. On connect, players are automatically queued for matchmaking. Moves are sent and broadcast over the same connection.
- **Engine layer** — a self-contained chess engine with no external dependencies, responsible for move generation, legal move validation, game-state tracking, and position evaluation.

Game state is kept in memory during a match (one `Game` object per active game, protected by a `ReentrantLock`). Completed games are persisted to PostgreSQL with their full move list and result.

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

**WebSocket Handshake Authentication**
- An `AuthHandshakeInterceptor` validates the JWT cookie before any WebSocket connection is established.
- `userID` and `username` are injected into the WebSocket session attributes after validation. Unauthenticated connections are rejected.

---

### Matchmaking

- On WebSocket connect, a player is automatically queued if they are not already in an active game.
- The queue finds the closest Elo match within a **200-point window**.
- If no suitable opponent is found, the player waits in the queue until one connects.
- The queue is backed by a `ConcurrentHashMap` and all match decisions are made inside a `synchronized` block to prevent race conditions.
- On disconnect, the player is removed from the queue if not yet matched.

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

**Game State**
- Each active game holds its board, king positions, en passant target, turn, and move history.
- Game-over detection runs after every move: if the opponent has no legal moves, the result is checkmate or stalemate depending on whether their king is in check.
- Per-game `ReentrantLock` prevents concurrent move submissions from corrupting state.

---

### WebSocket & Real-Time Gameplay

Moves are submitted in **UCI notation** — a 4-character string for normal moves (`e2e4`) and 5 characters for promotions (`e7e8q`).

**WebSocket Events**

| Direction       | Type           | Description                                                             |
|-----------------|----------------|-------------------------------------------------------------------------|
| Client → Server | `MOVE`         | Submit a move in UCI notation (e.g. `e2e4`, `e7e8q`)                    |
| Server → Client | `GAME_STARTED` | Match found — includes game ID, white/black player IDs and usernames    |
| Server → Client | `MOVE`         | Move broadcast to both players after a valid move                       |
| Server → Client | `GAME_ENDED`   | Game over — includes result (`WHITE_WIN`, `BLACK_WIN`, `DRAW`)          |
| Server → Client | `GAME_STATE`   | Full game state (move history) sent to a player who reconnects mid-game |
| Server → Client | `ERROR`        | Structured error with HTTP status code and message                      |

**Reconnection**
- On connect, if the user already has an active game, the full move history is sent as a `GAME_STATE` event so the client can reconstruct the board without server-side board serialization.

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

### Elo Rating

- Elo is updated at the end of every game.
- Win: +1, Draw: +0, Loss: −1.
- Both players' ratings are updated atomically after the game result is confirmed.

---

### Database & Persistence

**Schema Management with Flyway**
- All tables are version-controlled through Flyway migration scripts, applied automatically on startup.

**Relational Modeling**
- `users` stores credentials, Elo, bio, and avatar URL.
- `games` stores the result, start time, white/black player references, and the full move list as a serialized string.

**DTOs**
- All API responses use Data Transfer Objects to decouple the API surface from the internal entity structure.

---

## API Overview

| Method | Endpoint                   | Description                           |
|--------|----------------------------|---------------------------------------|
| POST   | /api/auth/register         | Register                              |
| POST   | /api/auth/login            | Login                                 |
| POST   | /api/auth/logout           | Logout                                |
| GET    | /api/auth                  | Check auth status                     |
| GET    | /api/user/{username}       | Get user profile (username, Elo, bio) |
| GET    | /api/games/user/{username} | Get game history for a user           |
| GET    | /api/games/{id}            | Get a specific game by ID             |
| WS     | /ws                        | WebSocket endpoint                    |

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

The project has comprehensive unit test coverage across all layers — engine, service, security, and controllers.

Tests use an in-memory H2 database so no external dependencies are required to run the suite.