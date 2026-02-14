/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.event.Subscribe;
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
 * TestFont - Tests Verdana font rendering
 */
public class TestFont extends Module {

    private SliderSetting fontSize;
    private VerdanaRenderer fontRenderer;
    private int lastFontSize = -1;

    public TestFont() {
        super("TestFont", ModuleCategory.MISC);
        this.hidden = true; // Hidden from ClickGUI
        this.registerSetting(new DescriptionSetting("Tests Verdana font"));
        this.registerSetting(fontSize = new SliderSetting("Font Size", 18.0, 10.0, 32.0, 1.0));
    }

    @Override
    public void onEnable() {
        fontRenderer = null; // Reset to recreate with current size
    }

    @Override
    public void onDisable() {
        if (fontRenderer != null) {
            fontRenderer.cleanup();
            fontRenderer = null;
        }
    }

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!enabled) return;
        if (mc == null || mc.theWorld == null) return;

        int currentSize = (int) fontSize.getValue();

        // Recreate font renderer if size changed
        if (fontRenderer == null || currentSize != lastFontSize) {
            if (fontRenderer != null) {
                fontRenderer.cleanup();
            }
            fontRenderer = new VerdanaRenderer(currentSize);
            lastFontSize = currentSize;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        // Draw test text in center of screen
        String testText = "Verdana Font Test - ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String testText2 = "abcdefghijklmnopqrstuvwxyz 0123456789";
        String testText3 = "Size: " + currentSize + "px";

        int x = 10;
        int y = screenHeight / 2 - 30;

        // Draw with Verdana
        fontRenderer.drawString(testText, x, y, 0xFFFFFFFF);
        fontRenderer.drawString(testText2, x, y + currentSize + 5, 0xFF55FFFF);
        fontRenderer.drawString(testText3, x, y + (currentSize + 5) * 2, 0xFFFF55);

        // Also draw with vanilla for comparison
        mc.fontRendererObj.drawStringWithShadow("Vanilla: " + testText, x, y + (currentSize + 5) * 3, 0xAAAAAA);
    }

    /**
     * Simple Verdana font renderer using texture atlas
     */
    private static class VerdanaRenderer {
        private int textureId = -1;
        private final int fontSize;
        private final int[] charWidths = new int[256];
        private final int charHeight;
        private final int texWidth = 512;
        private final int texHeight = 512;
        private static final int CHARS_PER_ROW = 16;

        public VerdanaRenderer(int size) {
            this.fontSize = size;
            this.charHeight = size + 4;
            createTexture();
        }

        private void createTexture() {
            try {
                // Create font
                Font font = new Font("Verdana", Font.PLAIN, fontSize);

                // Create image for texture atlas
                BufferedImage image = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();

                // Enable antialiasing
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g.setFont(font);
                FontMetrics fm = g.getFontMetrics();

                int cellWidth = texWidth / CHARS_PER_ROW;
                int cellHeight = texHeight / CHARS_PER_ROW;

                // Draw each character
                for (int i = 0; i < 256; i++) {
                    char c = (char) i;
                    int cx = (i % CHARS_PER_ROW) * cellWidth;
                    int cy = (i / CHARS_PER_ROW) * cellHeight;

                    // Store char width
                    charWidths[i] = fm.charWidth(c);
                    if (charWidths[i] <= 0) charWidths[i] = fontSize / 2;

                    // Draw character
                    g.setColor(Color.WHITE);
                    g.drawString(String.valueOf(c), cx + 2, cy + fm.getAscent() + 2);
                }

                g.dispose();

                // Upload to OpenGL
                int[] pixels = new int[texWidth * texHeight];
                image.getRGB(0, 0, texWidth, texHeight, pixels, 0, texWidth);

                ByteBuffer buffer = ByteBuffer.allocateDirect(texWidth * texHeight * 4);
                for (int y = 0; y < texHeight; y++) {
                    for (int x = 0; x < texWidth; x++) {
                        int pixel = pixels[y * texWidth + x];
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

            } catch (Exception e) {
                System.out.println("[TestFont] Error creating font texture: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void drawString(String text, float x, float y, int color) {
            if (textureId == -1 || text == null || text.isEmpty()) return;

            float alpha = ((color >> 24) & 0xFF) / 255.0f;
            float red = ((color >> 16) & 0xFF) / 255.0f;
            float green = ((color >> 8) & 0xFF) / 255.0f;
            float blue = (color & 0xFF) / 255.0f;

            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GlStateManager.color(red, green, blue, alpha);

            int cellWidth = texWidth / CHARS_PER_ROW;
            int cellHeight = texHeight / CHARS_PER_ROW;

            GL11.glBegin(GL11.GL_QUADS);
            float currentX = x;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c > 255) c = '?';

                int cx = (c % CHARS_PER_ROW) * cellWidth;
                int cy = (c / CHARS_PER_ROW) * cellHeight;

                float u1 = (float) cx / texWidth;
                float v1 = (float) cy / texHeight;
                float u2 = (float) (cx + cellWidth) / texWidth;
                float v2 = (float) (cy + cellHeight) / texHeight;

                float charW = cellWidth;
                float charH = cellHeight;

                GL11.glTexCoord2f(u1, v1);
                GL11.glVertex2f(currentX, y);
                GL11.glTexCoord2f(u1, v2);
                GL11.glVertex2f(currentX, y + charH);
                GL11.glTexCoord2f(u2, v2);
                GL11.glVertex2f(currentX + charW, y + charH);
                GL11.glTexCoord2f(u2, v1);
                GL11.glVertex2f(currentX + charW, y);

                currentX += charWidths[c] + 1;
            }

            GL11.glEnd();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        public int getStringWidth(String text) {
            if (text == null || text.isEmpty()) return 0;
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c > 255) c = '?';
                width += charWidths[c] + 1;
            }
            return width;
        }

        public void cleanup() {
            if (textureId != -1) {
                GL11.glDeleteTextures(textureId);
                textureId = -1;
            }
        }
    }
}
