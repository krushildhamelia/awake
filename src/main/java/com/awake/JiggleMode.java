package com.awake;

/**
 * Operating modes for keeping screen and status awake.
 */
public enum JiggleMode {
    /**
     * Micro-nudge: moves cursor by 1 pixel and immediately restores it.
     * Invisible to human eyes, causes zero distraction, but resets OS and app idle timers.
     */
    STEALTH("Stealth (1px Micro-nudge)"),

    /**
     * Smooth Roam: gently glides cursor in a small random radius over ~150ms.
     * Provides clear visible proof that activity is simulated.
     */
    SMOOTH_ROAM("Smooth Roam (Gentle Glide)"),

    /**
     * Key Pulse: pulses harmless unassigned virtual key (F15).
     * Does not move cursor at all.
     */
    KEY_PULSE("Key Pulse (Safe F15 Key)"),

    /**
     * Dual Mode: micro-nudge + F15 key pulse.
     * Maximum coverage against all corporate idle detection heuristics.
     */
    DUAL("Dual (Stealth Mouse + F15 Key)");

    private final String displayName;

    JiggleMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static JiggleMode fromString(String text) {
        if (text == null) return STEALTH;
        String lower = text.trim().toLowerCase();
        return switch (lower) {
            case "roam", "smooth", "smooth_roam", "random" -> SMOOTH_ROAM;
            case "key", "key_pulse", "f15" -> KEY_PULSE;
            case "dual", "both" -> DUAL;
            default -> STEALTH;
        };
    }
}
