package com.awake;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background service executing intelligent mouse jiggling and input simulation.
 */
public class AwakeService implements Runnable {

    public interface StatusListener {
        void onStatusChanged(boolean active, String lastAction);
    }

    private final AwakeConfig config;
    private Robot robot;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Random random = new Random();

    private Point lastObservedUserPos = new Point(0, 0);
    private long lastUserActivityTime = System.currentTimeMillis();
    private long lastNudgeTime = 0;
    private String lastActionDescription = "Ready";

    private final CopyOnWriteArrayList<StatusListener> listeners = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AwakeService(AwakeConfig config) {
        this.config = config;
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(5);
        } catch (AWTException e) {
            System.err.println("Error initializing java.awt.Robot: " + e.getMessage());
        }

        Point current = getCursorPosition();
        if (current != null) {
            this.lastObservedUserPos = current;
        }
    }

    public void addListener(StatusListener listener) {
        listeners.add(listener);
    }

    public void removeListener(StatusListener listener) {
        listeners.remove(listener);
    }

    public void setActive(boolean active) {
        this.active.set(active);
        if (active && config.isNativePower()) {
            WindowsPowerManager.activate();
        } else {
            WindowsPowerManager.deactivate();
        }
        notifyListeners(active ? "Resumed" : "Paused");
    }

    public boolean isActive() {
        return active.get();
    }

    public AwakeConfig getConfig() {
        return config;
    }

    public String getLastActionDescription() {
        return lastActionDescription;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            if (config.isNativePower()) {
                WindowsPowerManager.activate();
            }
            Thread worker = new Thread(this, "ScreenAwake-Worker");
            worker.setDaemon(true);
            worker.start();
        }
    }

    public void stop() {
        running.set(false);
        WindowsPowerManager.deactivate();
    }

    @Override
    public void run() {
        while (running.get()) {
            try {
                Thread.sleep(500);

                if (!active.get() || robot == null) {
                    continue;
                }

                Point currentPos = getCursorPosition();
                long now = System.currentTimeMillis();

                if (currentPos != null) {
                    // Check if human user moved cursor
                    if (Math.abs(currentPos.x - lastObservedUserPos.x) > 2 || Math.abs(currentPos.y - lastObservedUserPos.y) > 2) {
                        lastUserActivityTime = now;
                        lastObservedUserPos = currentPos;
                    }
                }

                long idleDuration = now - lastUserActivityTime;
                long sinceLastNudge = now - lastNudgeTime;

                long idleThresholdMs = config.getIdleThresholdSeconds() * 1000L;
                long intervalMs = config.getIntervalSeconds() * 1000L;

                // Trigger nudge if user is idle and interval elapsed
                if (idleDuration >= idleThresholdMs && sinceLastNudge >= intervalMs) {
                    performAwakeAction();
                    lastNudgeTime = System.currentTimeMillis();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Prevent thread termination on unexpected exception
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public synchronized void triggerNudgeNow() {
        if (robot == null) return;
        performAwakeAction();
        lastNudgeTime = System.currentTimeMillis();
    }

    private void performAwakeAction() {
        if (robot == null) return;

        JiggleMode mode = config.getMode();
        Point current = getCursorPosition();
        if (current == null) {
            current = new Point(500, 500);
        }

        String timeStr = LocalTime.now().format(TIME_FORMATTER);

        switch (mode) {
            case STEALTH -> {
                doStealthNudge(current);
                lastActionDescription = "[" + timeStr + "] Stealth 1px Nudge";
            }
            case SMOOTH_ROAM -> {
                doSmoothRoam(current);
                lastActionDescription = "[" + timeStr + "] Smooth Cursor Roam";
            }
            case KEY_PULSE -> {
                doKeyPulse();
                lastActionDescription = "[" + timeStr + "] Safe Key Pulse (F15)";
            }
            case DUAL -> {
                doStealthNudge(current);
                doKeyPulse();
                lastActionDescription = "[" + timeStr + "] Dual Nudge (Mouse + F15)";
            }
        }

        // Update known position so the robot's own nudge isn't mistaken for human user activity
        Point after = getCursorPosition();
        if (after != null) {
            lastObservedUserPos = after;
        }

        notifyListeners(lastActionDescription);
    }

    /**
     * Micro-nudge cursor by 1 pixel and immediately restore it.
     */
    private void doStealthNudge(Point origin) {
        Rectangle bounds = getScreenBoundsAt(origin);
        int dx = (origin.x + 1 < bounds.x + bounds.width) ? 1 : -1;
        int dy = (origin.y + 1 < bounds.y + bounds.height) ? 1 : -1;

        robot.mouseMove(origin.x + dx, origin.y + dy);
        robot.delay(30);
        robot.mouseMove(origin.x, origin.y);
    }

    /**
     * Gently glides cursor in a smooth arc within a small radius (~25px) and returns.
     */
    private void doSmoothRoam(Point origin) {
        Rectangle bounds = getScreenBoundsAt(origin);
        int radius = 20 + random.nextInt(20);
        double angle = random.nextDouble() * 2 * Math.PI;

        int targetX = (int) (origin.x + radius * Math.cos(angle));
        int targetY = (int) (origin.y + radius * Math.sin(angle));

        // Clamp to screen
        targetX = Math.max(bounds.x + 10, Math.min(bounds.x + bounds.width - 10, targetX));
        targetY = Math.max(bounds.y + 10, Math.min(bounds.y + bounds.height - 10, targetY));

        // Smooth glide outward
        smoothGlide(origin.x, origin.y, targetX, targetY, 8);
        robot.delay(80);
        // Smooth glide back to original location
        smoothGlide(targetX, targetY, origin.x, origin.y, 8);
    }

    private void smoothGlide(int x1, int y1, int x2, int y2, int steps) {
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            // Smooth ease in-out
            double ease = t * t * (3 - 2 * t);
            int curX = (int) (x1 + (x2 - x1) * ease);
            int curY = (int) (y1 + (y2 - y1) * ease);
            robot.mouseMove(curX, curY);
            robot.delay(12);
        }
    }

    /**
     * Pulses virtual key F15. Invisible in standard applications.
     */
    private void doKeyPulse() {
        try {
            robot.keyPress(KeyEvent.VK_F15);
            robot.delay(10);
            robot.keyRelease(KeyEvent.VK_F15);
        } catch (IllegalArgumentException e) {
            // Some platforms may not support VK_F15, fallback to safe Shift press/release or nothing
        }
    }

    private Point getCursorPosition() {
        try {
            PointerInfo pi = MouseInfo.getPointerInfo();
            return (pi != null) ? pi.getLocation() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Rectangle getScreenBoundsAt(Point p) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        for (GraphicsDevice device : devices) {
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            if (bounds.contains(p)) {
                return bounds;
            }
        }
        return new Rectangle(0, 0, 1920, 1080);
    }

    private void notifyListeners(String action) {
        for (StatusListener listener : listeners) {
            try {
                listener.onStatusChanged(active.get(), action);
            } catch (Exception ignored) {}
        }
    }
}
