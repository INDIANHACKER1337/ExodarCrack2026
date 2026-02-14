/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import io.github.exodar.font.FontManager;
import io.github.exodar.font.CustomFont;
import io.github.exodar.utils.FontRendererHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.lwjgl.opengl.GL11;

/**
 * ItemTest - Debug module to test custom fonts
 * Shows text with different fonts: Minecraft, Verdana, Exodar
 */
public class ItemTest extends Module {

    // Position settings
    private SliderSetting posX;
    private SliderSetting posY;

    // Font test options
    private TickSetting showMinecraft;
    private TickSetting showVerdana;
    private TickSetting showExodar;
    private TickSetting showExodarIcons;
    private TickSetting showESP;

    // Render style options
    private TickSetting showBackground;

    public ItemTest() {
        super("ItemTest", ModuleCategory.VISUALS);
        this.hidden = true; // Hidden from ClickGUI

        this.registerSetting(new DescriptionSetting("Test custom fonts"));
        this.registerSetting(posX = new SliderSetting("X Position", 100.0, 10.0, 500.0, 10.0));
        this.registerSetting(posY = new SliderSetting("Y Position", 100.0, 10.0, 500.0, 10.0));

        this.registerSetting(new DescriptionSetting("--- Fonts to Test ---"));
        this.registerSetting(showMinecraft = new TickSetting("Minecraft Font", true));
        this.registerSetting(showVerdana = new TickSetting("Verdana Font", true));
        this.registerSetting(showExodar = new TickSetting("Exodar Font", true));
        this.registerSetting(showExodarIcons = new TickSetting("Exodar Icons", true));
        this.registerSetting(showESP = new TickSetting("ESP Font", true));

        this.registerSetting(new DescriptionSetting("--- Options ---"));
        this.registerSetting(showBackground = new TickSetting("Show Background", true));
    }

    @Override
    public void onEnable() {
        System.out.println("[ItemTest] Enabled - testing custom fonts");
        // Initialize FontManager
        FontManager.getInstance();
    }

    /**
     * Called from Main.java to render the test
     */
    @Subscribe
    public void renderTest(Render2DEvent event) {
        if (!enabled || mc == null) return;

        try {
            int x = (int) posX.getValue();
            int y = (int) posY.getValue();

            FontRenderer fr = mc.fontRendererObj;
            if (fr == null) return;

            FontManager fm = FontManager.getInstance();

            // Save GL state
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();

            // Ensure proper 2D state
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // Background box
            if (showBackground.isEnabled()) {
                int bgWidth = 250;
                int bgHeight = 250;
                drawRectSimple(x - 5, y - 15, x + bgWidth, y + bgHeight, 0x80000000);
                drawRectSimple(x - 5, y - 15, x + bgWidth, y - 14, 0xFF7850FF); // Purple top border
            }

            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // Title
            FontRendererHelper.drawStringWithShadow(fr, "Custom Font Test", x, y - 10, 0x7850FF);

            int itemY = y + 10;
            int spacing = 20;

            // Minecraft Font (default)
            if (showMinecraft.isEnabled()) {
                FontRendererHelper.drawStringWithShadow(fr, "Minecraft: Hello World 123", x, itemY, 0xFFFFFF);
                itemY += spacing;
            }

            // Verdana Font
            if (showVerdana.isEnabled() && fm.verdana != null) {
                fm.verdana.drawStringWithShadow("Verdana: Hello World 123", x, itemY, 0xFF55FFFF);
                itemY += spacing;
            }

            // Exodar Font (text)
            if (showExodar.isEnabled() && fm.exodar != null) {
                fm.exodar.drawStringWithShadow("Exodar: Hello World 123", x, itemY, 0xFFFFAA00);
                itemY += spacing;
            }

            // ESP Font
            if (showESP.isEnabled() && fm.esp != null) {
                fm.esp.drawStringWithShadow("ESP: Hello World 123", x, itemY, 0xFFFF55FF);
                itemY += spacing;
                // Also show different sizes
                fm.espSmall.drawStringWithShadow("ESP Small: Test", x, itemY, 0xFFFF55FF);
                itemY += 16;
                fm.espLarge.drawStringWithShadow("ESP Large: Test", x, itemY, 0xFFFF55FF);
                itemY += 30;
            }

            // Exodar Icons (A B C D E F)
            if (showExodarIcons.isEnabled() && fm.exodar != null) {
                FontRendererHelper.drawStringWithShadow(fr, "Exodar Icons:", x, itemY, 0xAAAAAA);
                itemY += 12;

                // Draw each icon with different colors
                float iconX = x;
                int[] colors = {0xFFFF5555, 0xFF55FF55, 0xFF5555FF, 0xFFFFFF55, 0xFFFF55FF, 0xFF55FFFF};
                char[] icons = {'A', 'B', 'C', 'D', 'E', 'F'};

                for (int i = 0; i < icons.length; i++) {
                    fm.exodarLarge.drawString(String.valueOf(icons[i]), iconX, itemY, colors[i]);
                    iconX += 30;
                }
                itemY += 40;

                // Also show smaller icons
                FontRendererHelper.drawStringWithShadow(fr, "Small icons:", x, itemY, 0xAAAAAA);
                itemY += 12;
                iconX = x;
                for (int i = 0; i < icons.length; i++) {
                    fm.exodarSmall.drawString(String.valueOf(icons[i]), iconX, itemY, colors[i]);
                    iconX += 20;
                }
            }

            // Restore GL state
            GL11.glPopMatrix();
            GL11.glPopAttrib();

        } catch (Exception e) {
            System.out.println("[ItemTest] Error: " + e.getMessage());
            try {
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Simple rect drawing
     */
    private void drawRectSimple(int left, int top, int right, int bottom, int color) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        float a = (color >> 24 & 255) / 255.0f;
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;

        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
