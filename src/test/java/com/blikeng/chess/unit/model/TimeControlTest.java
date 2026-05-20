package com.blikeng.chess.unit.model;

import com.blikeng.chess.model.timecontrol.TimeControl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeControlTest {

    @Test
    void fromNameShouldReturnCorrectPresetForEachName() {
        assertThat(TimeControl.fromName("BULLET_1_0")).isEqualTo(TimeControl.BULLET_1_0);
        assertThat(TimeControl.fromName("BULLET_1_1")).isEqualTo(TimeControl.BULLET_1_1);
        assertThat(TimeControl.fromName("BULLET_2_0")).isEqualTo(TimeControl.BULLET_2_0);
        assertThat(TimeControl.fromName("BLITZ_3_0")).isEqualTo(TimeControl.BLITZ_3_0);
        assertThat(TimeControl.fromName("BLITZ_3_2")).isEqualTo(TimeControl.BLITZ_3_2);
        assertThat(TimeControl.fromName("BLITZ_5_0")).isEqualTo(TimeControl.BLITZ_5_0);
        assertThat(TimeControl.fromName("RAPID_10_0")).isEqualTo(TimeControl.RAPID_10_0);
        assertThat(TimeControl.fromName("RAPID_10_5")).isEqualTo(TimeControl.RAPID_10_5);
        assertThat(TimeControl.fromName("RAPID_15_0")).isEqualTo(TimeControl.RAPID_15_0);
        assertThat(TimeControl.fromName("RAPID_15_10")).isEqualTo(TimeControl.RAPID_15_10);
        assertThat(TimeControl.fromName("RAPID_30_0")).isEqualTo(TimeControl.RAPID_30_0);
        assertThat(TimeControl.fromName("CLASSICAL_60_0")).isEqualTo(TimeControl.CLASSICAL_60_0);
    }

    @Test
    void fromNameShouldThrowForUnknownName() {
        assertThatThrownBy(() -> TimeControl.fromName("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initialSecondsShouldBeMinutesTimedSixty() {
        assertThat(TimeControl.BLITZ_5_0.initialSeconds()).isEqualTo(300);
        assertThat(TimeControl.RAPID_10_0.initialSeconds()).isEqualTo(600);
    }
}
