/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Time-based notification system for modules
 * Shows notifications in bottom-right corner with smooth slide animations
 * Uses AnimationUtils for smooth delta-based animations
 *
 * Notification Types:
 * - MODULE: Green/Red for module on/off
 * - ALERT: Light blue for alerts
 * - WARNING: Pastel yellow for warnings
 */
public class ModuleNotification {
    private static final List<Notification> notifications = new ArrayList<>();
    private static final int NOTIFICATION_DURATION = 2000; // 2 seconds
    private static final int NOTIFICATION_FADE_DURATION = 300; // 300ms fade out
    private static final int NOTIFICATION_WIDTH = 160;
    private static final int NOTIFICATION_HEIGHT = 30;
    private static final float ANIMATION_SPEED = 12f; // Speed for AnimationUtils (lower = smoother)

    // Notification types
    public enum NotifType {
        MODULE,   // Green/Red for module toggle
        ALERT,    // Light cyan/blue for alerts
        WARNING   // Pastel yellow for warnings
    }

    // Colors for notification types
    private static final int COLOR_MODULE_ON = 0x00FF00;      // Green
    private static final int COLOR_MODULE_OFF = 0xFF0000;     // Red
    private static final int COLOR_ALERT = 0x7DD3FC;          // Light sky blue
    private static final int COLOR_WARNING = 0xFDE68A;        // Pastel yellow

    public static class Notification {
        public String text;
        public NotifType type;
        public boolean enabled; // Only used for MODULE type
        public long startTime;
        public AnimationUtils animationX;
        public AnimationUtils animationY;
        public boolean exiting = false;

        // Cached values
        public int cachedTextWidth = -1;

        public Notification(String text, NotifType type, boolean enabled, int screenWidth, int initialY, FontRenderer fr) {
            this.text = text;
            this.type = type;
            this.enabled = enabled;
            this.startTime = System.currentTimeMillis();

            // Initialize animations - start off-screen to the right
            this.animationX = new AnimationUtils(screenWidth + 50);
            this.animationY = new AnimationUtils(initialY);

            // Cache text width once
            if (fr != null) {
                this.cachedTextWidth = fr.getStringWidth(text);
            }
        }

        public int getBorderColor() {
            switch (type) {
                case ALERT:
                    return COLOR_ALERT;
                case WARNING:
                    return COLOR_WARNING;
                case MODULE:
                default:
                    return enabled ? COLOR_MODULE_ON : COLOR_MODULE_OFF;
            }
        }

        public String getSymbol() {
            switch (type) {
                case ALERT:
                    return "!";
                case WARNING:
                    return "⚠";
                case MODULE:
                default:
                    return enabled ? "+" : "-";
            }
        }

        public boolean isExpired(long currentTime) {
            return currentTime - startTime > NOTIFICATION_DURATION + NOTIFICATION_FADE_DURATION;
        }

        public float getAlpha(long currentTime) {
            long elapsed = currentTime - startTime;

            // Fade in (first 150ms)
            if (elapsed < 150) {
                return elapsed / 150.0f;
            }

            // Full opacity
            if (elapsed < NOTIFICATION_DURATION) {
                return 1.0f;
            }

            // Fade out (last 300ms)
            long fadeElapsed = elapsed - NOTIFICATION_DURATION;
            return Math.max(0.0f, 1.0f - (fadeElapsed / (float) NOTIFICATION_FADE_DURATION));
        }

        public void update(int targetX, int targetY) {
            long elapsed = System.currentTimeMillis() - startTime;

            // Start exit animation when duration is reached
            if (!exiting && elapsed >= NOTIFICATION_DURATION) {
                exiting = true;
            }

            // Animate X position
            int actualTargetX = exiting ? targetX + NOTIFICATION_WIDTH + 50 : targetX;
            animationX.setAnimation(actualTargetX, ANIMATION_SPEED);

            // Animate Y position
            animationY.setAnimation(targetY, ANIMATION_SPEED);
        }

        public int getX() {
            return (int) animationX.getValue();
        }

        public int getY() {
            return (int) animationY.getValue();
        }
    }

    /**
     * Add a module toggle notification (legacy method)
     */
    public static void addNotification(String moduleName, boolean enabled) {
        addNotification(moduleName, NotifType.MODULE, enabled);
    }

    /**
     * Add an alert notification (light blue)
     */
    public static void alert(String message) {
        addNotification(message, NotifType.ALERT, true);
    }

    /**
     * Add a warning notification (pastel yellow)
     */
    public static void warning(String message) {
        addNotification(message, NotifType.WARNING, true);
    }

    /**
     * Add a notification with specific type
     */
    public static void addNotification(String text, NotifType type, boolean enabled) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;

        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;

        try {
            net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
            screenWidth = sr.getScaledWidth();
            screenHeight = sr.getScaledHeight();
        } catch (Exception e) {
            // Use display size as fallback
        }

        // Remove old notification for the same text if exists (for MODULE type only)
        if (type == NotifType.MODULE) {
            notifications.removeIf(n -> n.text.equals(text) && n.type == NotifType.MODULE);
        }

        // Calculate initial Y position (bottom of screen)
        int baseY = screenHeight - 50;

        // Add new notification at the top of the list
        Notification newNotif = new Notification(text, type, enabled, screenWidth, baseY, fr);
        notifications.add(0, newNotif);
    }

    /**
     * Render all notifications
     */
    public static void render(Minecraft mc, int screenWidth, int screenHeight) {
        // Early exit if no notifications
        if (notifications.isEmpty()) return;

        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        // Cache time once
        long currentTime = System.currentTimeMillis();

        // Remove expired notifications
        Iterator<Notification> iter = notifications.iterator();
        while (iter.hasNext()) {
            Notification notif = iter.next();
            if (notif.isExpired(currentTime)) {
                iter.remove();
            }
        }

        // Early exit if all were expired
        if (notifications.isEmpty()) return;

        // Calculate positions
        int baseX = screenWidth - NOTIFICATION_WIDTH - 10;
        int baseY = screenHeight - 50;

        // Update all notification positions
        for (int i = 0; i < notifications.size(); i++) {
            Notification notif = notifications.get(i);
            int targetY = baseY - (i * (NOTIFICATION_HEIGHT + 5));
            notif.update(baseX, targetY);
        }

        // Setup GL state once for all notifications
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // Render all backgrounds
        for (Notification notif : notifications) {
            int x = notif.getX();
            int y = notif.getY();
            float alpha = notif.getAlpha(currentTime);

            if (alpha <= 0) continue;

            int alphaInt = (int) (alpha * 180);

            // Background
            int bgColor = (alphaInt << 24) | 0x202020;
            drawRectFast(x, y, x + NOTIFICATION_WIDTH, y + NOTIFICATION_HEIGHT, bgColor);

            // Left border (color based on notification type)
            int borderAlpha = (int) (alpha * 255);
            int borderColor = (borderAlpha << 24) | notif.getBorderColor();
            drawRectFast(x, y, x + 3, y + NOTIFICATION_HEIGHT, borderColor);
        }

        // Enable texture for text
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Render all text
        for (Notification notif : notifications) {
            int x = notif.getX();
            int y = notif.getY();
            float alpha = notif.getAlpha(currentTime);

            if (alpha <= 0) continue;

            int alphaInt = (int) (alpha * 255);

            // Symbol and colors based on type
            String symbol = notif.getSymbol();
            int symbolColor = (alphaInt << 24) | notif.getBorderColor();
            int textColor = (alphaInt << 24) | 0xFFFFFF;

            fr.drawStringWithShadow(symbol, x + 8, y + 11, symbolColor);
            fr.drawStringWithShadow(notif.text, x + 22, y + 11, textColor);
        }

        // Restore GL state
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void drawRectFast(int left, int top, int right, int bottom, int color) {
        // Swap if needed
        if (left > right) {
            int temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(left, bottom);
        GL11.glVertex2d(right, bottom);
        GL11.glVertex2d(right, top);
        GL11.glVertex2d(left, top);
        GL11.glEnd();
    }

    /**
     * Clear all notifications
     */
    public static void clearAll() {
        notifications.clear();
    }
}
