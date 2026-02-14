/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class RenderHelper {
    // Cached FontRenderer to avoid reflection every frame
    private static net.minecraft.client.gui.FontRenderer cachedFontRenderer = null;
    private static boolean fontRendererInitialized = false;

    public static void drawRect(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void drawRoundedRect(double x, double y, double x2, double y2, double radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glColor4f(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_POLYGON);

        // Top left corner
        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 180);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        // Top right corner
        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 270);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }

        // Bottom right corner
        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y2 - radius + Math.cos(angle) * radius);
        }

        // Bottom left corner
        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 90);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius, y2 - radius + Math.cos(angle) * radius);
        }

        GL11.glEnd();

        GL11.glPopAttrib();
    }

    public static void drawBorderedRoundedRect(double x, double y, double x2, double y2, double radius, int borderColor, int fillColor) {
        drawRoundedRect(x, y, x2, y2, radius, fillColor);

        // Draw border
        float alpha = (borderColor >> 24 & 0xFF) / 255.0F;
        float red = (borderColor >> 16 & 0xFF) / 255.0F;
        float green = (borderColor >> 8 & 0xFF) / 255.0F;
        float blue = (borderColor & 0xFF) / 255.0F;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2);

        GL11.glColor4f(red, green, blue, alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 180);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }
        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 270);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y + radius + Math.cos(angle) * radius);
        }
        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x2 - radius + Math.sin(angle) * radius, y2 - radius + Math.cos(angle) * radius);
        }
        for (int i = 0; i <= 90; i += 3) {
            double angle = Math.toRadians(i + 90);
            GL11.glVertex2d(x + radius + Math.sin(angle) * radius, y2 - radius + Math.cos(angle) * radius);
        }

        GL11.glEnd();
        GL11.glPopAttrib();
    }

    public static void drawGradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >> 8 & 255) / 255.0F;
        float startBlue = (float)(startColor & 255) / 255.0F;
        float endAlpha = (float)(endColor >> 24 & 255) / 255.0F;
        float endRed = (float)(endColor >> 16 & 255) / 255.0F;
        float endGreen = (float)(endColor >> 8 & 255) / 255.0F;
        float endBlue = (float)(endColor & 255) / 255.0F;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(startRed, startGreen, startBlue, startAlpha);
        GL11.glVertex2d(right, top);
        GL11.glVertex2d(left, top);
        GL11.glColor4f(endRed, endGreen, endBlue, endAlpha);
        GL11.glVertex2d(left, bottom);
        GL11.glVertex2d(right, bottom);
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static net.minecraft.client.gui.FontRenderer getFontRenderer() {
        // Use cached FontRenderer to avoid reflection every call
        if (fontRendererInitialized) {
            return cachedFontRenderer;
        }

        try {
            Minecraft mc = Minecraft.getMinecraft();
            for (java.lang.reflect.Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("FontRenderer")) {
                    f.setAccessible(true);
                    cachedFontRenderer = (net.minecraft.client.gui.FontRenderer) f.get(mc);
                    fontRendererInitialized = true;
                    return cachedFontRenderer;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        fontRendererInitialized = true; // Mark as initialized even if failed to avoid retrying
        return null;
    }

    public static void drawString(String text, int x, int y, int color) {
        net.minecraft.client.gui.FontRenderer fr = getFontRenderer();
        if (fr != null) {
            try {
                // Try with float params (Lunar Client)
                java.lang.reflect.Method method = fr.getClass().getMethod("drawString",
                    String.class, float.class, float.class, int.class, boolean.class);
                method.invoke(fr, text, (float)x, (float)y, color, false);
            } catch (Exception e) {
                // Fallback: try direct call (will work if signatures match)
                try {
                    fr.drawString(text, x, y, color, false);
                } catch (Exception ignored) {
                    // Silent fail
                }
            }
        }
    }

    public static void drawStringWithShadow(String text, float x, float y, int color) {
        net.minecraft.client.gui.FontRenderer fr = getFontRenderer();
        if (fr != null) io.github.exodar.utils.FontRendererHelper.drawStringWithShadow(fr, text, x, y, color);
    }

    public static void drawCenteredString(String text, int x, int y, int color) {
        net.minecraft.client.gui.FontRenderer fr = getFontRenderer();
        if (fr != null) {
            int width = fr.getStringWidth(text);
            fr.drawString(text, x - width / 2, y, color, false);
        }
    }

    public static int getStringWidth(String text) {
        net.minecraft.client.gui.FontRenderer fr = getFontRenderer();
        return fr != null ? fr.getStringWidth(text) : 0;
    }

    public static int getFontHeight() {
        net.minecraft.client.gui.FontRenderer fr = getFontRenderer();
        if (fr == null) return 9;

        // Try to get font height using reflection
        try {
            for (java.lang.reflect.Field f : fr.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    int value = (int) f.get(fr);
                    // Font height is typically 9
                    if (value == 9 || value == 8 || value == 10) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {}

        return 9; // Default font height
    }

    // Custom font rendering using AWT + OpenGL
    public static void drawIconString(String text, int x, int y, int color, float size) {
        try {
            java.awt.Font font = io.github.exodar.gui.FontLoader.getExodarFont(size);

            // Create buffered image to render text
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = img.createGraphics();
            g2d.setFont(font);
            java.awt.FontMetrics fm = g2d.getFontMetrics();

            int width = fm.stringWidth(text);
            int height = fm.getHeight();
            g2d.dispose();

            if (width == 0 || height == 0) {
                // Fallback if no text
                GL11.glPushMatrix();
                float scale = size / 8.0f;
                GL11.glScalef(scale, scale, 1.0f);
                drawString(text, (int)(x / scale), (int)(y / scale), color);
                GL11.glPopMatrix();
                return;
            }

            // Create actual image with proper size
            img = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            g2d = img.createGraphics();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS, java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setFont(font);
            g2d.setColor(new java.awt.Color(color));
            g2d.drawString(text, 0, fm.getAscent());
            g2d.dispose();

            // Convert to OpenGL texture and render
            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(width * height * 4);
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {
                    int pixel = img.getRGB(px, py);
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            // Draw texture
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(x + width, y);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(x + width, y + height);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(x, y + height);
            GL11.glEnd();

            GL11.glPopMatrix();
            GL11.glDeleteTextures(textureId);

        } catch (Exception e) {
            // Fallback to scaled regular font
            GL11.glPushMatrix();
            float scale = size / 8.0f;
            GL11.glScalef(scale, scale, 1.0f);
            drawString(text, (int)(x / scale), (int)(y / scale), color);
            GL11.glPopMatrix();
        }
    }

    public static int getIconStringWidth(String text, float size) {
        try {
            java.awt.Font font = io.github.exodar.gui.FontLoader.getExodarFont(size);
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = img.createGraphics();
            g2d.setFont(font);
            java.awt.FontMetrics fm = g2d.getFontMetrics();
            int width = fm.stringWidth(text);
            g2d.dispose();
            return width;
        } catch (Exception e) {
            // Fallback estimation
            return (int)(getStringWidth(text) * (size / 8.0f));
        }
    }
}
