/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.font;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * Custom TrueType Font renderer for Minecraft
 * Supports custom .ttf fonts including icon fonts like Exodar.ttf
 */
public class CustomFont {
    private final Font font;
    private final Map<Character, CharData> charDataMap = new HashMap<>();
    private DynamicTexture texture;
    private ResourceLocation textureLocation;
    private int fontHeight = -1;

    // Character data holder
    private static class CharData {
        public int x, y, width, height;
        public CharData(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Create a CustomFont from a resource path
     * @param fontPath Path to .ttf file in resources (e.g., "/fonts/exodar.ttf")
     * @param size Font size
     */
    public CustomFont(String fontPath, int size) {
        Font tempFont = null;

        try {
            InputStream is = CustomFont.class.getResourceAsStream(fontPath);
            if (is == null) {
                // Fallback to default font
                tempFont = new Font("Arial", Font.PLAIN, size);
            } else {
                tempFont = Font.createFont(Font.TRUETYPE_FONT, is);
                tempFont = tempFont.deriveFont(Font.PLAIN, (float) size);
            }
        } catch (Exception e) {
            tempFont = new Font("Arial", Font.PLAIN, size);
        }

        this.font = tempFont;
        generateTexture();
    }

    /**
     * Generate texture atlas for all printable characters
     */
    private void generateTexture() {
        // Characters to include (ASCII printable + some extras for icons)
        StringBuilder chars = new StringBuilder();
        for (int i = 32; i < 127; i++) {
            chars.append((char) i);
        }
        // Add icon characters (A-F for Exodar icons)
        chars.append("ABCDEF");

        // Calculate texture size (larger = better quality)
        int imgSize = 1024;
        BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();

        // Enable high quality anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setFont(font);
        g.setColor(Color.WHITE);

        FontMetrics fontMetrics = g.getFontMetrics();
        fontHeight = fontMetrics.getHeight();

        int x = 2, y = 2;
        int rowHeight = 0;

        // Draw each character and store its position
        for (int i = 0; i < chars.length(); i++) {
            char ch = chars.charAt(i);
            Rectangle2D bounds = fontMetrics.getStringBounds(String.valueOf(ch), g);

            int charWidth = (int) bounds.getWidth() + 2;
            int charHeight = (int) bounds.getHeight();

            // Move to next row if needed
            if (x + charWidth >= imgSize) {
                x = 2;
                y += rowHeight + 2;
                rowHeight = 0;
            }

            // Draw character
            g.drawString(String.valueOf(ch), x, y + fontMetrics.getAscent());

            // Store character data
            charDataMap.put(ch, new CharData(x, y, charWidth, charHeight));

            x += charWidth + 2;
            rowHeight = Math.max(rowHeight, charHeight);
        }

        g.dispose();

        // Upload texture to Minecraft
        try {
            texture = new DynamicTexture(bufferedImage);
            textureLocation = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("customfont", texture);
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Draw a string at the specified position
     * @param text Text to draw
     * @param x X position
     * @param y Y position
     * @param color Color in ARGB format
     */
    public void drawString(String text, float x, float y, int color) {
        if (text == null || text.isEmpty()) return;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Bind our texture
        Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation);

        // Enable linear filtering for smooth antialiased text
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        // Extract color components
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        GL11.glColor4f(red, green, blue, alpha);

        float currentX = x;

        GL11.glBegin(GL11.GL_QUADS);

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            CharData charData = charDataMap.get(ch);

            if (charData == null) continue;

            float texX = (float) charData.x / 1024.0F;
            float texY = (float) charData.y / 1024.0F;
            float texX2 = (float) (charData.x + charData.width) / 1024.0F;
            float texY2 = (float) (charData.y + charData.height) / 1024.0F;

            GL11.glTexCoord2f(texX, texY);
            GL11.glVertex3f(currentX, y, 0);

            GL11.glTexCoord2f(texX, texY2);
            GL11.glVertex3f(currentX, y + charData.height, 0);

            GL11.glTexCoord2f(texX2, texY2);
            GL11.glVertex3f(currentX + charData.width, y + charData.height, 0);

            GL11.glTexCoord2f(texX2, texY);
            GL11.glVertex3f(currentX + charData.width, y, 0);

            currentX += charData.width;
        }

        GL11.glEnd();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    /**
     * Draw string with shadow
     */
    public void drawStringWithShadow(String text, float x, float y, int color) {
        // Draw shadow
        drawString(text, x + 1, y + 1, 0xFF000000);
        // Draw main text
        drawString(text, x, y, color);
    }

    /**
     * Get the width of a string
     */
    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;

        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            CharData charData = charDataMap.get(text.charAt(i));
            if (charData != null) {
                width += charData.width;
            }
        }
        return width;
    }

    /**
     * Get the font height
     */
    public int getHeight() {
        return fontHeight;
    }

    /**
     * Get the underlying AWT Font
     */
    public Font getFont() {
        return font;
    }
}
