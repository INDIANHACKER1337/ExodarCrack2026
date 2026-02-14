/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui.modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Modern rendering utilities for the new ClickGUI
 * Provides methods for rounded rects, gradients, shadows, glow effects, etc.
 */
public class RenderUtils {

    // ============ THEME COLORS ============
    // Matches the website style
    public static final Color PRIMARY = new Color(160, 32, 240);        // #a020f0
    public static final Color SECONDARY = new Color(200, 80, 255);      // #c850ff
    public static final Color ACCENT = new Color(135, 52, 244);         // #8734f4
    public static final Color ACCENT_BLUE = new Color(50, 52, 255);     // #3234ff
    public static final Color BACKGROUND = new Color(10, 10, 15);       // #0a0a0f
    public static final Color SURFACE = new Color(20, 20, 32);          // #141420
    public static final Color SURFACE_LIGHT = new Color(30, 30, 45);
    public static final Color TEXT = new Color(240, 240, 240);          // #f0f0f0
    public static final Color TEXT_DIM = new Color(150, 150, 160);
    public static final Color TEXT_MUTED = new Color(100, 100, 115);
    public static final Color SUCCESS = new Color(46, 204, 113);
    public static final Color WARNING = new Color(241, 196, 15);
    public static final Color ERROR = new Color(231, 76, 60);

    // ============ BASIC SHAPES ============

    /**
     * Draw a filled rectangle
     */
    public static void drawRect(float x, float y, float width, float height, Color color) {
        Gui.drawRect((int)x, (int)y, (int)(x + width), (int)(y + height), color.getRGB());
    }

    /**
     * Draw a filled rectangle with custom alpha
     */
    public static void drawRect(float x, float y, float width, float height, Color color, int alpha) {
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        Gui.drawRect((int)x, (int)y, (int)(x + width), (int)(y + height), c.getRGB());
    }

    /**
     * Draw a rounded rectangle using GL
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float radius, Color color) {
        drawRoundedRect(x, y, width, height, radius, color.getRGB());
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        // Extract color components
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(red, green, blue, alpha);

        // For simplicity, draw a regular rect with simulated corners
        // In a full implementation, you'd use GL_TRIANGLE_FAN for actual rounded corners

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        // Main body
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x + radius, y, 0).endVertex();
        worldRenderer.pos(x + radius, y + height, 0).endVertex();
        worldRenderer.pos(x + width - radius, y + height, 0).endVertex();
        worldRenderer.pos(x + width - radius, y, 0).endVertex();
        tessellator.draw();

        // Left side
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x, y + radius, 0).endVertex();
        worldRenderer.pos(x, y + height - radius, 0).endVertex();
        worldRenderer.pos(x + radius, y + height - radius, 0).endVertex();
        worldRenderer.pos(x + radius, y + radius, 0).endVertex();
        tessellator.draw();

        // Right side
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x + width - radius, y + radius, 0).endVertex();
        worldRenderer.pos(x + width - radius, y + height - radius, 0).endVertex();
        worldRenderer.pos(x + width, y + height - radius, 0).endVertex();
        worldRenderer.pos(x + width, y + radius, 0).endVertex();
        tessellator.draw();

        // Top strip
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x + radius, y, 0).endVertex();
        worldRenderer.pos(x + radius, y + radius, 0).endVertex();
        worldRenderer.pos(x + width - radius, y + radius, 0).endVertex();
        worldRenderer.pos(x + width - radius, y, 0).endVertex();
        tessellator.draw();

        // Bottom strip
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x + radius, y + height - radius, 0).endVertex();
        worldRenderer.pos(x + radius, y + height, 0).endVertex();
        worldRenderer.pos(x + width - radius, y + height, 0).endVertex();
        worldRenderer.pos(x + width - radius, y + height - radius, 0).endVertex();
        tessellator.draw();

        // Draw corners as circles
        drawFilledCircle(x + radius, y + radius, radius, color);
        drawFilledCircle(x + width - radius, y + radius, radius, color);
        drawFilledCircle(x + radius, y + height - radius, radius, color);
        drawFilledCircle(x + width - radius, y + height - radius, radius, color);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Draw a filled circle
     */
    public static void drawFilledCircle(float cx, float cy, float radius, int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        worldRenderer.pos(cx, cy, 0).endVertex();

        int segments = 16;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            worldRenderer.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();
    }

    // ============ GRADIENTS ============

    /**
     * Draw a vertical gradient rectangle
     */
    public static void drawGradientRect(float x, float y, float width, float height, Color colorTop, Color colorBottom) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(x, y + height, 0).color(colorBottom.getRed(), colorBottom.getGreen(), colorBottom.getBlue(), colorBottom.getAlpha()).endVertex();
        worldRenderer.pos(x + width, y + height, 0).color(colorBottom.getRed(), colorBottom.getGreen(), colorBottom.getBlue(), colorBottom.getAlpha()).endVertex();
        worldRenderer.pos(x + width, y, 0).color(colorTop.getRed(), colorTop.getGreen(), colorTop.getBlue(), colorTop.getAlpha()).endVertex();
        worldRenderer.pos(x, y, 0).color(colorTop.getRed(), colorTop.getGreen(), colorTop.getBlue(), colorTop.getAlpha()).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    /**
     * Draw a horizontal gradient rectangle
     */
    public static void drawGradientRectH(float x, float y, float width, float height, Color colorLeft, Color colorRight) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(x, y, 0).color(colorLeft.getRed(), colorLeft.getGreen(), colorLeft.getBlue(), colorLeft.getAlpha()).endVertex();
        worldRenderer.pos(x, y + height, 0).color(colorLeft.getRed(), colorLeft.getGreen(), colorLeft.getBlue(), colorLeft.getAlpha()).endVertex();
        worldRenderer.pos(x + width, y + height, 0).color(colorRight.getRed(), colorRight.getGreen(), colorRight.getBlue(), colorRight.getAlpha()).endVertex();
        worldRenderer.pos(x + width, y, 0).color(colorRight.getRed(), colorRight.getGreen(), colorRight.getBlue(), colorRight.getAlpha()).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    // ============ GLOW / SHADOW EFFECTS ============

    /**
     * Draw a glow effect around a rectangle (soft shadow)
     */
    public static void drawGlow(float x, float y, float width, float height, float spread, Color color) {
        // Draw multiple layers with decreasing alpha to create glow
        for (int i = (int)spread; i > 0; i--) {
            float alpha = (1.0f - (float)i / spread) * 0.3f * (color.getAlpha() / 255f);
            Color glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
            drawRoundedRect(x - i, y - i, width + i * 2, height + i * 2, i + 4, glowColor);
        }
    }

    /**
     * Draw a shadow under a rectangle
     */
    public static void drawShadow(float x, float y, float width, float height, float blur) {
        for (int i = (int)blur; i > 0; i--) {
            float alpha = (1.0f - (float)i / blur) * 0.15f;
            Color shadowColor = new Color(0, 0, 0, (int)(alpha * 255));
            drawRect(x - i + 2, y - i + 3, width + i * 2, height + i * 2, shadowColor);
        }
    }

    // ============ BORDER ============

    /**
     * Draw a rectangle outline
     */
    public static void drawOutline(float x, float y, float width, float height, float lineWidth, Color color) {
        // Top
        drawRect(x, y, width, lineWidth, color);
        // Bottom
        drawRect(x, y + height - lineWidth, width, lineWidth, color);
        // Left
        drawRect(x, y, lineWidth, height, color);
        // Right
        drawRect(x + width - lineWidth, y, lineWidth, height, color);
    }

    /**
     * Draw a rounded rectangle outline
     */
    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float lineWidth, Color color) {
        // Simplified - just draw outline parts
        // Top
        drawRect(x + radius, y, width - radius * 2, lineWidth, color);
        // Bottom
        drawRect(x + radius, y + height - lineWidth, width - radius * 2, lineWidth, color);
        // Left
        drawRect(x, y + radius, lineWidth, height - radius * 2, color);
        // Right
        drawRect(x + width - lineWidth, y + radius, lineWidth, height - radius * 2, color);
    }

    // ============ UTILITY ============

    /**
     * Linear interpolation between two colors
     */
    public static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, bl)),
            Math.max(0, Math.min(255, al))
        );
    }

    /**
     * Linear interpolation
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    /**
     * Get rainbow color at current time
     */
    public static Color getRainbow(float speed, float saturation, float brightness, long offset) {
        float hue = ((System.currentTimeMillis() + offset) % (long)(1000 / speed)) / (1000 / speed);
        return Color.getHSBColor(hue, saturation, brightness);
    }

    /**
     * Get a gradient color between purple and pink (Exodar theme)
     */
    public static Color getThemeGradient(float t) {
        return lerpColor(PRIMARY, SECONDARY, t);
    }

    // ============ SCISSOR ============

    /**
     * Enable scissor test for clipping
     */
    public static void enableScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, mc.displayHeight - (y + height) * scale, width * scale, height * scale);
    }

    /**
     * Disable scissor test
     */
    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
