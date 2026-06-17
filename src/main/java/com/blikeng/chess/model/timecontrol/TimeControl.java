package com.blikeng.chess.model.timecontrol;

/**
 * Concrete time controls as {@code NAME(initialMinutes, incrementSeconds)}.
 *
 * <p>{@link #initialMs}/{@link #incrementMs} give the values in milliseconds, {@link #type}
 * maps to the broad {@link TcType} category, and {@link #label} is a display string like
 * {@code "Rapid 10+0"}.
 */
public enum TimeControl {
    BULLET_1_0(1, 0),
    BULLET_1_1(1, 1),
    BULLET_2_0(2, 0),

    BLITZ_3_0(3, 0),
    BLITZ_3_2(3, 2),
    BLITZ_5_0(5, 0),
    BLITZ_5_3(5, 3),

    RAPID_10_0(10, 0),
    RAPID_10_5(10, 5),
    RAPID_15_0(15, 0),
    RAPID_15_10(15, 10),
    RAPID_30_0(30, 0),

    CLASSICAL_60_0(60, 0),
    CLASSICAL_90_30(90, 30),
    CLASSICAL_120_0(120, 0);

    private final int initialMinutes;
    private final int increment;

    TimeControl(int initialMinutes, int increment) {
        this.initialMinutes = initialMinutes;
        this.increment = increment;
    }

    public int initialMs() {
        return initialMinutes * 60 * 1000;
    }

    public int incrementMs() {
        return increment * 1000;
    }

    public TcType type() {
        if (name().startsWith("BULLET")) return TcType.BULLET;
        if (name().startsWith("BLITZ"))  return TcType.BLITZ;
        if (name().startsWith("CLASSICAL")) return TcType.CLASSICAL;
        return TcType.RAPID;
    }

    public String label() {
        String category = switch (type()) {
            case BULLET -> "Bullet";
            case BLITZ  -> "Blitz";
            case RAPID  -> "Rapid";
            case CLASSICAL -> "Classical";
        };
        return category + " " + initialMinutes + "+" + increment;
    }

    public static TimeControl fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown time control: " + name);
        }
    }
}