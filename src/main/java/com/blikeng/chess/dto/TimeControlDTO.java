package com.blikeng.chess.dto;

import com.blikeng.chess.model.timecontrol.TimeControl;

public record TimeControlDTO(String timeControl) {
    public TimeControl resolved() {
        return TimeControl.fromName(timeControl);
    }
}
