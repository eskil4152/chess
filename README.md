# Chess

A chess backend built with Java and Spring Boot. Features a custom chess engine with full move validation, check/checkmate/stalemate detection, 
castling, en passant, and pawn promotion. Real-time gameplay is handled over WebSockets, with JWT-based authentication via HTTP-only cookies. 
Players are matched through a built-in matchmaking queue, and game history is persisted in PostgreSQL using Flyway migrations.

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=alert_status&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=reliability_rating&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=security_rating&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=sqale_rating&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=eskil4152_chess&metric=coverage&token=469754be27b6275c7320c03b903fba6df45ee983)](https://sonarcloud.io/summary/new_code?id=eskil4152_chess)