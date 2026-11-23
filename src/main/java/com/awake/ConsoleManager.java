package com.awake;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Interactive console interface for CLI / headless execution.
 */
public class ConsoleManager implements Runnable {

    private final AwakeService service;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ConsoleManager(AwakeService service) {
        this.service = service;
    }

    public void start() {
        printBanner();

        service.addListener((active, lastAction) -> {
            String time = LocalTime.now().format(TIME_FORMAT);
            String state = active ? "ACTIVE" : "PAUSED";
            System.out.printf("[%s] [%s] %s%n", time, state, lastAction);
        });

        Thread readerThread = new Thread(this, "Console-Input-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void printBanner() {
        System.out.println("==============================================================");
        System.out.println("         SCREEN AWAKE & MOUSE JIGGLER (Standalone)           ");
        System.out.println("==============================================================");
        System.out.println(" Mode:       " + service.getConfig().getMode().getDisplayName());
        System.out.println(" Interval:   " + service.getConfig().getIntervalSeconds() + " seconds");
        System.out.println(" Idle Wait:  " + service.getConfig().getIdleThresholdSeconds() + " seconds");
        System.out.println(" Win Power:  " + (WindowsPowerManager.isAvailable() && service.getConfig().isNativePower() ? "ENABLED (SetThreadExecutionState)" : "Disabled / Non-Win"));
        System.out.println("--------------------------------------------------------------");
        System.out.println(" Commands:                                                    ");
        System.out.println("   [p] Pause / Resume                                         ");
        System.out.println("   [m] Cycle Jiggle Mode (Stealth -> Roam -> Key -> Dual)     ");
        System.out.println("   [j] Nudge / Jiggle Cursor Now                              ");
        System.out.println("   [s] Show Status                                            ");
        System.out.println("   [q] Quit and Exit                                          ");
        System.out.println("==============================================================");
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.isEmpty()) continue;

                switch (line) {
                    case "p" -> {
                        boolean newState = !service.isActive();
                        service.setActive(newState);
                        System.out.println(">> Service is now: " + (newState ? "ACTIVE" : "PAUSED"));
                    }
                    case "m" -> {
                        JiggleMode current = service.getConfig().getMode();
                        JiggleMode next = switch (current) {
                            case STEALTH -> JiggleMode.SMOOTH_ROAM;
                            case SMOOTH_ROAM -> JiggleMode.KEY_PULSE;
                            case KEY_PULSE -> JiggleMode.DUAL;
                            case DUAL -> JiggleMode.STEALTH;
                        };
                        service.getConfig().setMode(next);
                        System.out.println(">> Mode switched to: " + next.getDisplayName());
                    }
                    case "j" -> {
                        System.out.println(">> Triggering immediate nudge...");
                        service.triggerNudgeNow();
                    }
                    case "s" -> {
                        System.out.printf(">> Status: %s | Mode: %s | Interval: %ds | Last: %s%n",
                            service.isActive() ? "ACTIVE" : "PAUSED",
                            service.getConfig().getMode().name(),
                            service.getConfig().getIntervalSeconds(),
                            service.getLastActionDescription()
                        );
                    }
                    case "q", "exit", "quit" -> {
                        System.out.println(">> Exiting Screen Awake. Goodbye!");
                        service.stop();
                        System.exit(0);
                    }
                    default -> System.out.println("Unknown command. Use [p]ause, [m]ode, [j]iggle, [s]tatus, [q]uit");
                }
            }
        } catch (Exception e) {
            // Stream closed or running in background without stdin
        }
    }
}
