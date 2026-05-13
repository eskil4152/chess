package com.blikeng.chess.model.timecontrol;

public class TimeControls {
    public static final TimeControl BULLET_1_0 = new TimeControl(1, 0);
    public static final TimeControl BULLET_1_1 = new TimeControl(1, 1);
    public static final TimeControl BULLET_2_0 = new TimeControl(2, 0);

    public static final TimeControl BLITZ_3_0 = new TimeControl(3, 0);
    public static final TimeControl BLITZ_3_2 = new TimeControl(3, 2);
    public static final TimeControl BLITZ_5_0 = new TimeControl(5, 0);

    public static final TimeControl RAPID_10_0 = new TimeControl(10, 0);
    public static final TimeControl RAPID_10_5 = new TimeControl(10, 5);
    public static final TimeControl RAPID_15_0 = new TimeControl(15, 0);
    public static final TimeControl RAPID_15_10 = new TimeControl(15, 10);
    public static final TimeControl RAPID_30_0 = new TimeControl(30, 0);
    public static final TimeControl RAPID_60_0 = new TimeControl(50, 0);
}
