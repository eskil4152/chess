package com.blikeng.chess.dto.websocket;

public record WsErrorDTO(String type, int status, String message) {
    public WsErrorDTO(int status, String message) { this("ERROR", status, message); }
}