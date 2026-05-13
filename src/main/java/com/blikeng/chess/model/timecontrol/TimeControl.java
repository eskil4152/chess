package com.blikeng.chess.model.timecontrol;

public record TimeControl(int initialSeconds, int incrementSeconds) {
    public TimeControl(int initialSeconds, int incrementSeconds) {
        this.initialSeconds = initialSeconds * 60;
        this.incrementSeconds = incrementSeconds;
    }

    public static TimeControl fromName(String name) {
        return switch (name) {
            case "BULLET_1_0" -> BULLET_1_0;
            case "BULLET_1_1" -> BULLET_1_1;
            case "BULLET_2_0" -> BULLET_2_0;
            case "BLITZ_3_0"  -> BLITZ_3_0;
            case "BLITZ_3_2"  -> BLITZ_3_2;
            case "BLITZ_5_0"  -> BLITZ_5_0;
            case "RAPID_10_0" -> RAPID_10_0;
            case "RAPID_10_5" -> RAPID_10_5;
            case "RAPID_15_0" -> RAPID_15_0;
            case "RAPID_15_10" -> RAPID_15_10;
            case "RAPID_30_0" -> RAPID_30_0;
            case "RAPID_60_0" -> RAPID_60_0;
            default -> throw new IllegalArgumentException("Unknown time control: " + name);
        };
    }

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
