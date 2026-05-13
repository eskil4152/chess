package com.blikeng.chess.model.timecontrol;

public record TimeControl(int initialSeconds, int incrementSeconds) {
    public TimeControl(int initialSeconds, int incrementSeconds) {
        this.initialSeconds = initialSeconds * 60;
        this.incrementSeconds = incrementSeconds;
    }
}
