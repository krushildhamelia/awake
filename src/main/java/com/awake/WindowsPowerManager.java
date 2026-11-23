package com.awake;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Native Windows Power Management using SetThreadExecutionState.
 * Prevents Windows from turning off screen, entering sleep, or locking session.
 */
public class WindowsPowerManager {

    private interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        int SetThreadExecutionState(int esFlags);
    }

    // Windows API Constants
    private static final int ES_CONTINUOUS        = 0x80000000;
    private static final int ES_SYSTEM_REQUIRED   = 0x00000001;
    private static final int ES_DISPLAY_REQUIRED  = 0x00000002;
    private static final int ES_AWAYMODE_REQUIRED = 0x00000040;

    private static boolean isWindows = false;
    private static boolean available = false;
    private static boolean active = false;

    static {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            isWindows = os.contains("win");
            if (isWindows) {
                // Test availability
                Kernel32.INSTANCE.SetThreadExecutionState(ES_CONTINUOUS);
                available = true;
            }
        } catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static synchronized void activate() {
        if (!available) return;
        try {
            int flags = ES_CONTINUOUS | ES_SYSTEM_REQUIRED | ES_DISPLAY_REQUIRED | ES_AWAYMODE_REQUIRED;
            Kernel32.INSTANCE.SetThreadExecutionState(flags);
            active = true;
        } catch (Throwable ignored) {
            active = false;
        }
    }

    public static synchronized void deactivate() {
        if (!available) return;
        try {
            Kernel32.INSTANCE.SetThreadExecutionState(ES_CONTINUOUS);
            active = false;
        } catch (Throwable ignored) {}
    }

    public static boolean isActive() {
        return active;
    }
}
