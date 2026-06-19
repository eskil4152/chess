package com.blikeng.chess.dto;

import com.blikeng.chess.model.timecontrol.TimeControl;

/** A requested time control by name; Mapped to the {@link com.blikeng.chess.model.timecontrol.TimeControl} enum.
 * <p>Used for queuing the correct time control for a game.
 * */
public record TimeControlDTO(String timeControl) {
    public TimeControl resolved() {
        return TimeControl.fromName(timeControl);
    }
}
