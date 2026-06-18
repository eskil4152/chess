package com.blikeng.chess.dto.websocket;

/** Outbound ERROR message: an HTTP-style status and message for a failed action. */
public record WsErrorDTO(String type, int status, String message) {
    public WsErrorDTO(int status, String message) { this("ERROR", status, message); }
}