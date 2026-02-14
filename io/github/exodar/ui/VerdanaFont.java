/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * VerdanaFont - Custom Verdana font renderer for Rise-style ArrayList
 * Renders smooth antialiased Verdana text using texture atlas
 */
public class VerdanaFont {
    private static VerdanaFont instance;
    private static int currentSize = 18;

    private int textureId = -1;
    private final int fontSize;
    private final Map<Character, CharData> charMap = new HashMap<>();
    private final int texWidth = 512;
    private final int texHeight = 512;
    private int fontHeight = 18;
    private boolean loaded = false;

    // Cache for different font sizes
    private static final Map<Integer, VerdanaFont> fontCache = new HashMap<>();

    private static class CharData {
        int x, y, width, height;
        CharData(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Get or create a VerdanaFont instance for the specified size
     */
    public static VerdanaFont get(int size) {
        VerdanaFont font = fontCache.get(size);
        if (font == null) {
            font = new VerdanaFont(size);
            fontCache.put(size, font);
        }
        return font;
    }

    /**
     * Get the default instance (size 18)
     */
    public static VerdanaFont getInstance() {
        return get(18);
    }

    /**
     * Check if custom font is loaded
     */
    public static boolean isLoaded() {
        return getInstance().loaded;
    }

    private VerdanaFont(int size) {
        this.fontSize = size;
        // Don't create texture in constructor - defer to first use
    }

    /**
     * Ensure the font texture is created (lazy loading)
     */
    private void ensureLoaded() {
        if (loaded || textureId != -1) return;
        createTexture();
    }

    private void createTexture() {
        try {
            // Check if we're on the render thread with valid OpenGL context
            if (org.lwjgl.opengl.GLContext.getCapabilities() == null) {
                return; // OpenGL not ready yet
            }
            Font font = new Font("Verdana", Font.PLAIN, fontSize);

            BufferedImage image = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();

            // Enable antialiasing for smooth text
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g.setFont(font);
            g.setColor(Color.WHITE);

            FontMetrics fm = g.getFontMetrics();
            fontHeight = fm.getHeight();

            int x = 2;
            int y = fm.getAscent() + 2;
            int rowHeight = fontHeight + 4;

            // Render ASCII characters 32-126
            for (char c = 32; c < 127; c++) {
                int charWidth = fm.charWidth(c);
                if (charWidth == 0) charWidth = fontSize / 2;

                // Wrap to next row if needed
                if (x + charWidth + 4 > texWidth) {
                    x = 2;
                    y += rowHeight;
                }

                if (y + rowHeight > texHeight) break;

                g.drawString(String.valueOf(c), x, y);
                charMap.put(c, new CharData(x, y - fm.getAscent(), charWidth, fontHeight));
                x += charWidth + 4;
            }
            g.dispose();

            // Upload to OpenGL
            int[] pixels = new int[texWidth * texHeight];
            image.getRGB(0, 0, texWidth, texHeight, pixels, 0, texWidth);

            ByteBuffer buffer = ByteBuffer.allocateDirect(texWidth * texHeight * 4);
            for (int py = 0; py < texHeight; py++) {
                for (int px = 0; px < texWidth; px++) {
                    int pixel = pixels[py * texWidth + px];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                    buffer.put((byte) (pixel & 0xFF));          // B
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
                }
            }
            buffer.flip();

            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, texWidth, texHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            loaded = true;
        } catch (Exception e) {
            System.out.println("[VerdanaFont] Error creating font: " + e.getMessage());
            loaded = false;
        }
    }

    /**
     * Draw string at position
     */
    public void drawString(String text, float x, float y, int color) {
        ensureLoaded();
        if (!loaded || textureId == -1 || text == null || text.isEmpty()) return;

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_QUADS);
        float drawX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            CharData data = charMap.get(c);
            if (data == null) {
                drawX += fontSize / 2;
                continue;
            }

            float u1 = data.x / (float) texWidth;
            float v1 = data.y / (float) texHeight;
            float u2 = (data.x + data.width) / (float) texWidth;
            float v2 = (data.y + data.height) / (float) texHeight;

            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(drawX, y);
            GL11.glTexCoord2f(u2, v1); GL11.glVertex2f(drawX + data.width, y);
            GL11.glTexCoord2f(u2, v2); GL11.glVertex2f(drawX + data.width, y + data.height);
            GL11.glTexCoord2f(u1, v2); GL11.glVertex2f(drawX, y + data.height);

            drawX += data.width + 1;
        }
        GL11.glEnd();

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Draw string with shadow (1 pixel offset, darker)
     */
    public void drawStringWithShadow(String text, float x, float y, int color) {
        if (!loaded || text == null || text.isEmpty()) return;

        // Draw shadow (25% brightness of original, offset by 1 pixel)
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        if (a == 0) a = 255;

        int shadowColor = ((a / 4) << 24) | ((r / 4) << 16) | ((g / 4) << 8) | (b / 4);
        drawString(text, x + 1, y + 1, shadowColor);

        // Draw main text
        drawString(text, x, y, color);
    }

    /**
     * Check if this font instance is loaded
     */
    public boolean isFontLoaded() {
        ensureLoaded();
        return loaded;
    }

    /**
     * Get width of string in pixels
     */
    public int getStringWidth(String text) {
        ensureLoaded();
        if (!loaded || text == null || text.isEmpty()) return 0;

        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            CharData data = charMap.get(c);
            if (data != null) {
                width += data.width + 1;
            } else {
                width += fontSize / 2;
            }
        }
        return width;
    }

    /**
     * Get font height
     */
    public int getHeight() {
        return fontHeight;
    }

    /**
     * Cleanup OpenGL resources
     */
    public void cleanup() {
        if (textureId != -1) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }
        loaded = false;
    }

    /**
     * Cleanup all cached fonts
     */
    public static void cleanupAll() {
        for (VerdanaFont font : fontCache.values()) {
            font.cleanup();
        }
        fontCache.clear();
    }
}
