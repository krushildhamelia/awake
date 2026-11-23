package com.awake;

import java.awt.GraphicsEnvironment;

/**
 * Main application entry point for Standalone Screen Awake executable JAR.
 */
public class Main {

    public static void main(String[] args) {
        // Parse CLI arguments
        AwakeConfig config = AwakeConfig.parse(args);

        // Initialize Awake Engine
        AwakeService service = new AwakeService(config);

        // Register JVM Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Awake] Shutting down, restoring system power state...");
            service.stop();
        }, "Awake-Shutdown-Hook"));

        // Start background worker
        service.start();

        boolean trayInstalled = false;
        boolean headless = GraphicsEnvironment.isHeadless() || config.isConsoleMode();

        if (!headless) {
            TrayManager trayManager = new TrayManager(service);
            trayInstalled = trayManager.install();
        }

        if (trayInstalled && !config.isConsoleMode()) {
            System.out.println("[Awake] Running in System Tray. Double-click tray icon or right-click for options.");
        } else {
            // Fallback or explicit CLI mode
            ConsoleManager console = new ConsoleManager(service);
            console.start();
        }

        // Keep main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {}
    }
}
