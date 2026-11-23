package com.awake;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * System Tray manager providing tray icon, contextual menu, and notification balloons.
 */
public class TrayManager {

    private final AwakeService service;
    private TrayIcon trayIcon;
    private MenuItem statusItem;
    private MenuItem toggleItem;
    private CheckboxMenuItem stealthModeItem;
    private CheckboxMenuItem roamModeItem;
    private CheckboxMenuItem keyModeItem;
    private CheckboxMenuItem dualModeItem;

    public TrayManager(AwakeService service) {
        this.service = service;
    }

    public boolean install() {
        if (!SystemTray.isSupported()) {
            return false;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Dimension traySize = tray.getTrayIconSize();
            int iconSize = Math.max(traySize.width, 16);

            Image iconImage = IconGenerator.createTrayIcon(service.isActive(), iconSize);
            PopupMenu popup = buildPopupMenu();

            trayIcon = new TrayIcon(iconImage, "Screen Awake (Active)", popup);
            trayIcon.setImageAutoSize(true);

            // Left-click / double-click toggles Pause / Resume
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        service.setActive(!service.isActive());
                    }
                }
            });

            tray.add(trayIcon);

            // Register listener for status updates
            service.addListener((active, lastAction) -> updateTrayState(active, lastAction, iconSize));

            trayIcon.displayMessage(
                "Screen Awake Active",
                "Screen will stay awake (" + service.getConfig().getMode().getDisplayName() + ", every " + service.getConfig().getIntervalSeconds() + "s)",
                TrayIcon.MessageType.INFO
            );

            return true;
        } catch (Exception e) {
            System.err.println("Could not initialize System Tray: " + e.getMessage());
            return false;
        }
    }

    private PopupMenu buildPopupMenu() {
        PopupMenu popup = new PopupMenu();

        // Status Header (disabled label)
        statusItem = new MenuItem("● Status: Active (Awake)");
        statusItem.setEnabled(false);
        popup.add(statusItem);

        popup.addSeparator();

        // Pause / Resume Toggle
        toggleItem = new MenuItem("Pause Keeping Awake");
        toggleItem.addActionListener(e -> service.setActive(!service.isActive()));
        popup.add(toggleItem);

        // Immediate Action
        MenuItem triggerNow = new MenuItem("Jiggle / Nudge Now");
        triggerNow.addActionListener(e -> service.triggerNudgeNow());
        popup.add(triggerNow);

        popup.addSeparator();

        // Mode Submenu
        Menu modeMenu = new Menu("Jiggle Mode");
        stealthModeItem = new CheckboxMenuItem("Stealth (1px micro-nudge)", service.getConfig().getMode() == JiggleMode.STEALTH);
        roamModeItem = new CheckboxMenuItem("Smooth Roam (visible glide)", service.getConfig().getMode() == JiggleMode.SMOOTH_ROAM);
        keyModeItem = new CheckboxMenuItem("Key Pulse (safe F15)", service.getConfig().getMode() == JiggleMode.KEY_PULSE);
        dualModeItem = new CheckboxMenuItem("Dual (Mouse + Key)", service.getConfig().getMode() == JiggleMode.DUAL);

        stealthModeItem.addItemListener(e -> setMode(JiggleMode.STEALTH));
        roamModeItem.addItemListener(e -> setMode(JiggleMode.SMOOTH_ROAM));
        keyModeItem.addItemListener(e -> setMode(JiggleMode.KEY_PULSE));
        dualModeItem.addItemListener(e -> setMode(JiggleMode.DUAL));

        modeMenu.add(stealthModeItem);
        modeMenu.add(roamModeItem);
        modeMenu.add(keyModeItem);
        modeMenu.add(dualModeItem);
        popup.add(modeMenu);

        // Interval Submenu
        Menu intervalMenu = new Menu("Interval");
        addIntervalOption(intervalMenu, "15 seconds", 15);
        addIntervalOption(intervalMenu, "30 seconds (Default)", 30);
        addIntervalOption(intervalMenu, "60 seconds (1 min)", 60);
        addIntervalOption(intervalMenu, "2 minutes", 120);
        addIntervalOption(intervalMenu, "5 minutes", 300);
        popup.add(intervalMenu);

        popup.addSeparator();

        // Exit
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> {
            service.stop();
            SystemTray.getSystemTray().remove(trayIcon);
            System.exit(0);
        });
        popup.add(exitItem);

        return popup;
    }

    private void addIntervalOption(Menu menu, String label, int seconds) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(e -> {
            service.getConfig().setIntervalSeconds(seconds);
            updateTooltip();
        });
        menu.add(item);
    }

    private void setMode(JiggleMode mode) {
        service.getConfig().setMode(mode);
        stealthModeItem.setState(mode == JiggleMode.STEALTH);
        roamModeItem.setState(mode == JiggleMode.SMOOTH_ROAM);
        keyModeItem.setState(mode == JiggleMode.KEY_PULSE);
        dualModeItem.setState(mode == JiggleMode.DUAL);
        updateTooltip();
    }

    private void updateTrayState(boolean active, String lastAction, int iconSize) {
        if (trayIcon == null) return;

        trayIcon.setImage(IconGenerator.createTrayIcon(active, iconSize));

        if (statusItem != null) {
            statusItem.setLabel(active ? "● Status: Active (Awake)" : "○ Status: Paused");
        }
        if (toggleItem != null) {
            toggleItem.setLabel(active ? "Pause Keeping Awake" : "Resume Keeping Awake");
        }

        updateTooltip();
    }

    private void updateTooltip() {
        if (trayIcon == null) return;
        boolean active = service.isActive();
        String tooltip = String.format(
            "Screen Awake - %s\nMode: %s | %ds\n%s",
            active ? "Active" : "Paused",
            service.getConfig().getMode().name(),
            service.getConfig().getIntervalSeconds(),
            service.getLastActionDescription()
        );
        if (tooltip.length() > 63) {
            // Windows tray tooltip length constraint safety
            tooltip = tooltip.substring(0, 63);
        }
        trayIcon.setToolTip(tooltip);
    }
}
