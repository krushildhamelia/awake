package com.awake;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Creates dynamic, high-DPI tray icons showing Awake status (Active / Paused).
 */
public class IconGenerator {

    /**
     * Generates a modern coffee cup / awake icon with dynamic status dot.
     *
     * @param active true if awake is running, false if paused
     * @param size icon width/height (usually 16 or 32)
     * @return BufferedImage of the icon
     */
    public static Image createTrayIcon(boolean active, int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int pad = Math.max(1, size / 8);
        int w = size - 2 * pad;
        int h = size - 2 * pad;

        if (active) {
            // Active background badge: Deep Navy / Slate
            g2.setColor(new Color(24, 28, 36));
            g2.fillRoundRect(pad, pad, w, h, size / 3, size / 3);

            // Cup Body: Crisp White / Ice Blue
            g2.setColor(new Color(230, 245, 255));
            int cupX = pad + (int) (w * 0.2);
            int cupY = pad + (int) (h * 0.35);
            int cupW = (int) (w * 0.55);
            int cupH = (int) (h * 0.45);
            g2.fillRoundRect(cupX, cupY, cupW, cupH, 4, 4);

            // Cup Handle
            g2.setColor(new Color(230, 245, 255));
            g2.setStroke(new BasicStroke(Math.max(1.5f, size * 0.08f)));
            g2.drawArc(cupX + cupW - 2, cupY + 2, (int) (w * 0.25), cupH - 4, 270, 180);

            // Steam lines (showing hot / awake)
            g2.setColor(new Color(56, 189, 248)); // Cyan steam
            g2.setStroke(new BasicStroke(Math.max(1.2f, size * 0.06f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int steamY = cupY - 2;
            g2.drawLine(cupX + (int)(cupW * 0.3), steamY, cupX + (int)(cupW * 0.35), steamY - (int)(h * 0.15));
            g2.drawLine(cupX + (int)(cupW * 0.7), steamY, cupX + (int)(cupW * 0.65), steamY - (int)(h * 0.15));

            // Green Pulse Dot (Active status indicator in bottom right)
            int dotSize = Math.max(4, (int) (size * 0.32));
            int dotX = size - dotSize - 1;
            int dotY = size - dotSize - 1;

            g2.setColor(new Color(24, 28, 36));
            g2.fillOval(dotX - 1, dotY - 1, dotSize + 2, dotSize + 2); // border

            g2.setColor(new Color(34, 197, 94)); // Vivid emerald green
            g2.fillOval(dotX, dotY, dotSize, dotSize);

        } else {
            // Paused background badge: Dark Charcoal
            g2.setColor(new Color(30, 30, 35));
            g2.fillRoundRect(pad, pad, w, h, size / 3, size / 3);

            // Cup Body: Dimmed Grey
            g2.setColor(new Color(140, 145, 155));
            int cupX = pad + (int) (w * 0.2);
            int cupY = pad + (int) (h * 0.35);
            int cupW = (int) (w * 0.55);
            int cupH = (int) (h * 0.45);
            g2.fillRoundRect(cupX, cupY, cupW, cupH, 4, 4);

            // Handle
            g2.setStroke(new BasicStroke(Math.max(1.5f, size * 0.08f)));
            g2.drawArc(cupX + cupW - 2, cupY + 2, (int) (w * 0.25), cupH - 4, 270, 180);

            // Amber / Orange Dot (Paused status indicator in bottom right)
            int dotSize = Math.max(4, (int) (size * 0.32));
            int dotX = size - dotSize - 1;
            int dotY = size - dotSize - 1;

            g2.setColor(new Color(30, 30, 35));
            g2.fillOval(dotX - 1, dotY - 1, dotSize + 2, dotSize + 2);

            g2.setColor(new Color(245, 158, 11)); // Amber orange
            g2.fillOval(dotX, dotY, dotSize, dotSize);
        }

        g2.dispose();
        return image;
    }
}
