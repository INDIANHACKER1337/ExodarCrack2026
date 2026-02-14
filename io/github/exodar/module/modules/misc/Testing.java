/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Testing - Visual effects testing module with gradient shadow/glow
 */
public class Testing extends Module {

    private final TickSetting showShadowBox;
    private final TickSetting showGlowBox;
    private final TickSetting showRgbGlow;
    private final TickSetting showVerdanaText;
    private final SliderSetting glowSize;
    private final SliderSetting cornerRadius;

    private long startTime = 0;

    // Verdana font texture
    private int fontTextureId = -1;
    private Map<Character, int[]> charData = new HashMap<>(); // char -> [x, y, width, height]
    private int fontTexWidth = 512;
    private int fontTexHeight = 512;
    private int fontHeight = 18;
    private boolean fontLoaded = false;

    public Testing() {
        super("Testing", ModuleCategory.MISC);
        this.hidden = true; // Hidden from ClickGUI

        this.registerSetting(new DescriptionSetting("Visual effects testing"));
        this.registerSetting(showShadowBox = new TickSetting("Shadow Box", true));
        this.registerSetting(showGlowBox = new TickSetting("Glow Box", true));
        this.registerSetting(showRgbGlow = new TickSetting("RGB Glow", true));
        this.registerSetting(showVerdanaText = new TickSetting("Verdana Text", true));
        this.registerSetting(glowSize = new SliderSetting("Glow/Shadow Size", 15, 5, 40, 1));
        this.registerSetting(cornerRadius = new SliderSetting("Corner Radius", 8, 2, 20, 1));
    }

    @Override
    public void onEnable() {
        startTime = System.currentTimeMillis();
        if (!fontLoaded) {
            loadVerdanaFont();
        }
    }

    private void loadVerdanaFont() {
        try {
            Font verdana = new Font("Verdana", Font.PLAIN, 16);
            BufferedImage image = new BufferedImage(fontTexWidth, fontTexHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // Enable antialiasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(verdana);
            g2d.setColor(Color.WHITE);

            FontMetrics fm = g2d.getFontMetrics();
            fontHeight = fm.getHeight();

            int x = 2;
            int y = fm.getAscent() + 2;
            int rowHeight = fontHeight + 4;

            // Render ASCII characters 32-126
            for (char c = 32; c < 127; c++) {
                int charWidth = fm.charWidth(c);
                if (charWidth == 0) charWidth = 8;

                if (x + charWidth + 2 > fontTexWidth) {
                    x = 2;
                    y += rowHeight;
                }

                if (y + rowHeight > fontTexHeight) break;

                g2d.drawString(String.valueOf(c), x, y);
                charData.put(c, new int[]{x, y - fm.getAscent(), charWidth, fontHeight});
                x += charWidth + 2;
            }
            g2d.dispose();

            // Upload to OpenGL
            int[] pixels = new int[fontTexWidth * fontTexHeight];
            image.getRGB(0, 0, fontTexWidth, fontTexHeight, pixels, 0, fontTexWidth);

            ByteBuffer buffer = ByteBuffer.allocateDirect(fontTexWidth * fontTexHeight * 4);
            for (int py = 0; py < fontTexHeight; py++) {
                for (int px = 0; px < fontTexWidth; px++) {
                    int pixel = pixels[py * fontTexWidth + px];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                    buffer.put((byte) (pixel & 0xFF));         // B
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
                }
            }
            buffer.flip();

            fontTextureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, fontTexWidth, fontTexHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            fontLoaded = true;
        } catch (Exception e) {
            System.out.println("[Testing] Failed to load Verdana font: " + e.getMessage());
        }
    }

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenHeight = sr.getScaledHeight();

        int baseX = 10;
        int baseY = screenHeight / 2 - 80;
        long elapsed = System.currentTimeMillis() - startTime;

        int boxWidth = 120;
        int boxHeight = 30;
        int radius = (int) cornerRadius.getValue();
        int shadowSize = (int) glowSize.getValue();

        // 1. Shadow Box - gradient from center (100% black) to edges (0%)
        if (showShadowBox.isEnabled()) {
            // Draw gradient shadow
            drawGradientRoundedRect(baseX, baseY, boxWidth, boxHeight, radius, shadowSize, 0x000000, 0.6f);

            // Draw solid box on top
            drawSolidRoundedRect(baseX, baseY, boxWidth, boxHeight, radius, 0xFF1A1A2E);

            mc.fontRendererObj.drawStringWithShadow("Shadow Box", baseX + 10, baseY + 11, 0xFFFFFF);
            baseY += boxHeight + shadowSize + 15;
        }

        // 2. Glow Box - gradient from center (100% cyan) to edges (0%)
        if (showGlowBox.isEnabled()) {
            // Draw gradient glow
            drawGradientRoundedRect(baseX, baseY, boxWidth, boxHeight, radius, shadowSize, 0x00FFFF, 0.8f);

            // Draw solid box on top
            drawSolidRoundedRect(baseX, baseY, boxWidth, boxHeight, radius, 0xFF1A1A2E);

            mc.fontRendererObj.drawStringWithShadow("Glow Box", baseX + 10, baseY + 11, 0x00FFFF);
            baseY += boxHeight + shadowSize + 15;
        }

        // 3. RGB Glow Box
        if (showRgbGlow.isEnabled()) {
            int rgbColor = getRainbowColor(elapsed);

            // Draw gradient RGB glow
            drawGradientRoundedRect(baseX, baseY, boxWidth, boxHeight, radius, shadowSize, rgbColor, 0.9f);

            // Draw solid box on top
            drawSolidRoundedRect(baseX, baseY, boxWidth, boxHeight, radius, 0xFF1A1A2E);

            mc.fontRendererObj.drawStringWithShadow("RGB Glow", baseX + 10, baseY + 11, rgbColor | 0xFF000000);
            baseY += boxHeight + shadowSize + 15;
        }

        // 4. Verdana Text
        if (showVerdanaText.isEnabled() && fontLoaded) {
            drawVerdanaString("Verdana Font Test!", baseX, baseY, 0xFFFFFF);
            baseY += fontHeight + 5;
            drawVerdanaString("ABCDEFGHIJKLMNOPQRSTUVWXYZ", baseX, baseY, 0x00FFFF);
            baseY += fontHeight + 5;
            drawVerdanaString("abcdefghijklmnopqrstuvwxyz", baseX, baseY, 0xFF6600);
            baseY += fontHeight + 5;
            drawVerdanaString("0123456789 !@#$%", baseX, baseY, getRainbowColor(elapsed) | 0xFF000000);
        }
    }

    /**
     * Draws a rounded rectangle with gradient from center (full alpha) to edges (0 alpha)
     * Uses smooth shading with triangle fan from center
     */
    private void drawGradientRoundedRect(int x, int y, int width, int height, int radius, int gradientSize, int color, float centerAlpha) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;

        // Expanded bounds for gradient
        int ex = x - gradientSize;
        int ey = y - gradientSize;
        int ew = width + gradientSize * 2;
        int eh = height + gradientSize * 2;
        int er = radius + gradientSize;

        // Draw as triangle fan from center
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);

        // Center vertex - full alpha
        GL11.glColor4f(r, g, b, centerAlpha);
        GL11.glVertex2f(cx, cy);

        // Edge vertices - 0 alpha (rounded rectangle outline)
        GL11.glColor4f(r, g, b, 0.0f);

        int segments = 16;

        // Top-left corner arc
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI + (Math.PI / 2) * i / segments;
            float px = (float) (ex + er + Math.cos(angle) * er);
            float py = (float) (ey + er + Math.sin(angle) * er);
            GL11.glVertex2f(px, py);
        }

        // Top-right corner arc
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 1.5 + (Math.PI / 2) * i / segments;
            float px = (float) (ex + ew - er + Math.cos(angle) * er);
            float py = (float) (ey + er + Math.sin(angle) * er);
            GL11.glVertex2f(px, py);
        }

        // Bottom-right corner arc
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI / 2) * i / segments;
            float px = (float) (ex + ew - er + Math.cos(angle) * er);
            float py = (float) (ey + eh - er + Math.sin(angle) * er);
            GL11.glVertex2f(px, py);
        }

        // Bottom-left corner arc
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI / 2 + (Math.PI / 2) * i / segments;
            float px = (float) (ex + er + Math.cos(angle) * er);
            float py = (float) (ey + eh - er + Math.sin(angle) * er);
            GL11.glVertex2f(px, py);
        }

        // Close the fan (back to first edge point)
        double angle = Math.PI;
        float px = (float) (ex + er + Math.cos(angle) * er);
        float py = (float) (ey + er + Math.sin(angle) * er);
        GL11.glVertex2f(px, py);

        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Draws a solid rounded rectangle
     */
    private void drawSolidRoundedRect(int x, int y, int width, int height, int radius, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(r, g, b, a);

        // Center rectangle
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x + radius, y);
        GL11.glVertex2f(x + width - radius, y);
        GL11.glVertex2f(x + width - radius, y + height);
        GL11.glVertex2f(x + radius, y + height);
        GL11.glEnd();

        // Left rectangle
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y + radius);
        GL11.glVertex2f(x + radius, y + radius);
        GL11.glVertex2f(x + radius, y + height - radius);
        GL11.glVertex2f(x, y + height - radius);
        GL11.glEnd();

        // Right rectangle
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x + width - radius, y + radius);
        GL11.glVertex2f(x + width, y + radius);
        GL11.glVertex2f(x + width, y + height - radius);
        GL11.glVertex2f(x + width - radius, y + height - radius);
        GL11.glEnd();

        // Corner arcs
        int segments = 16;

        // Top-left
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x + radius, y + radius);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI + (Math.PI / 2) * i / segments;
            GL11.glVertex2f((float)(x + radius + Math.cos(angle) * radius), (float)(y + radius + Math.sin(angle) * radius));
        }
        GL11.glEnd();

        // Top-right
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x + width - radius, y + radius);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 1.5 + (Math.PI / 2) * i / segments;
            GL11.glVertex2f((float)(x + width - radius + Math.cos(angle) * radius), (float)(y + radius + Math.sin(angle) * radius));
        }
        GL11.glEnd();

        // Bottom-right
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x + width - radius, y + height - radius);
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI / 2) * i / segments;
            GL11.glVertex2f((float)(x + width - radius + Math.cos(angle) * radius), (float)(y + height - radius + Math.sin(angle) * radius));
        }
        GL11.glEnd();

        // Bottom-left
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x + radius, y + height - radius);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI / 2 + (Math.PI / 2) * i / segments;
            GL11.glVertex2f((float)(x + radius + Math.cos(angle) * radius), (float)(y + height - radius + Math.sin(angle) * radius));
        }
        GL11.glEnd();

        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Draw string using Verdana font texture
     */
    private void drawVerdanaString(String text, int x, int y, int color) {
        if (!fontLoaded || fontTextureId == -1) return;

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        if (a == 0) a = 1.0f; // Default to full alpha if not specified

        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
        GL11.glColor4f(r, g, b, a);

        float drawX = x;
        for (char c : text.toCharArray()) {
            int[] data = charData.get(c);
            if (data == null) {
                drawX += 8;
                continue;
            }

            float u1 = data[0] / (float) fontTexWidth;
            float v1 = data[1] / (float) fontTexHeight;
            float u2 = (data[0] + data[2]) / (float) fontTexWidth;
            float v2 = (data[1] + data[3]) / (float) fontTexHeight;

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(drawX, y);
            GL11.glTexCoord2f(u2, v1); GL11.glVertex2f(drawX + data[2], y);
            GL11.glTexCoord2f(u2, v2); GL11.glVertex2f(drawX + data[2], y + data[3]);
            GL11.glTexCoord2f(u1, v2); GL11.glVertex2f(drawX, y + data[3]);
            GL11.glEnd();

            drawX += data[2] + 1;
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private int getRainbowColor(long time) {
        float hue = (time % 3000) / 3000.0f;
        return Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
