package com.awake;

/**
 * Configuration settings for Awake service.
 */
public class AwakeConfig {
    private int intervalSeconds = 30;
    private int idleThresholdSeconds = 10;
    private JiggleMode mode = JiggleMode.STEALTH;
    private boolean nativePower = true;
    private boolean consoleMode = false;

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = Math.max(5, intervalSeconds);
    }

    public int getIdleThresholdSeconds() {
        return idleThresholdSeconds;
    }

    public void setIdleThresholdSeconds(int idleThresholdSeconds) {
        this.idleThresholdSeconds = Math.max(0, idleThresholdSeconds);
    }

    public JiggleMode getMode() {
        return mode;
    }

    public void setMode(JiggleMode mode) {
        if (mode != null) {
            this.mode = mode;
        }
    }

    public boolean isNativePower() {
        return nativePower;
    }

    public void setNativePower(boolean nativePower) {
        this.nativePower = nativePower;
    }

    public boolean isConsoleMode() {
        return consoleMode;
    }

    public void setConsoleMode(boolean consoleMode) {
        this.consoleMode = consoleMode;
    }

    public static AwakeConfig parse(String[] args) {
        AwakeConfig config = new AwakeConfig();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            switch (arg) {
                case "--interval", "-i" -> {
                    if (i + 1 < args.length) {
                        try {
                            config.setIntervalSeconds(Integer.parseInt(args[++i]));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                case "--idle" -> {
                    if (i + 1 < args.length) {
                        try {
                            config.setIdleThresholdSeconds(Integer.parseInt(args[++i]));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                case "--mode", "-m" -> {
                    if (i + 1 < args.length) {
                        config.setMode(JiggleMode.fromString(args[++i]));
                    }
                }
                case "--console", "--cli", "--headless" -> config.setConsoleMode(true);
                case "--no-native" -> config.setNativePower(false);
                case "--help", "-h", "/?" -> {
                    printUsage();
                    System.exit(0);
                }
            }
        }
        return config;
    }

    public static void printUsage() {
        System.out.println("Screen Awake Tool - Standalone Executable JAR");
        System.out.println("Usage: java -jar screen-awake.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -i, --interval <sec>   Interval between awake actions in seconds (default: 30)");
        System.out.println("      --idle <sec>       User idle threshold before nudging in seconds (default: 10)");
        System.out.println("  -m, --mode <mode>      Awake mode: stealth | roam | key | dual (default: stealth)");
        System.out.println("      --console, --cli   Run in interactive console mode (no System Tray icon)");
        System.out.println("      --no-native        Disable Windows Native SetThreadExecutionState power lock");
        System.out.println("  -h, --help             Show this help screen");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  stealth : Imperceptible 1-pixel mouse nudge and restore (Default)");
        System.out.println("  roam    : Gentle smooth mouse roam across a small area");
        System.out.println("  key     : Virtual F15 key pulse (unobtrusive, no mouse movement)");
        System.out.println("  dual    : Stealth 1-pixel nudge + virtual F15 key pulse");
    }
}
