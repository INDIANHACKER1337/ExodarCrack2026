/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui.modern;

import io.github.exodar.Main;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern ClickGUI - Using direct GL rendering
 */
public class ModernClickGui extends GuiScreen {

    private ModuleManager moduleManager;

    // Position
    private static int guiX = -1, guiY = -1;
    private int guiWidth = 450;
    private int guiHeight = 300;

    // State
    private static ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private Module hoveredModule = null;

    // Drag
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // Scroll
    private int scroll = 0;

    // Colors
    private static final int COL_BG = new Color(20, 20, 28).getRGB();
    private static final int COL_HEADER = new Color(30, 30, 42).getRGB();
    private static final int COL_ACCENT = new Color(140, 80, 230).getRGB();
    private static final int COL_TEXT = new Color(220, 220, 230).getRGB();
    private static final int COL_GRAY = new Color(120, 120, 140).getRGB();
    private static final int COL_HOVER = new Color(40, 40, 55).getRGB();

    public ModernClickGui() {
        this.moduleManager = Main.getModuleManager();
        System.out.println("[ModernClickGui] Constructor called");
    }

    @Override
    public void initGui() {
        System.out.println("[ModernClickGui] initGui called - width=" + width + ", height=" + height);
        // Center if first time or invalid position
        if (guiX < 0 || guiY < 0 || width == 0 || height == 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            guiX = (sr.getScaledWidth() - guiWidth) / 2;
            guiY = (sr.getScaledHeight() - guiHeight) / 2;
        }
        System.out.println("[ModernClickGui] initGui - guiX=" + guiX + ", guiY=" + guiY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        System.out.println("[ModernClickGui] drawScreen CALLED!");

        try {
            // Reset GL state
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            ScaledResolution sr = new ScaledResolution(mc);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();

            // Recalculate position if needed
            if (guiX < 0 || guiY < 0) {
                guiX = (screenWidth - guiWidth) / 2;
                guiY = (screenHeight - guiHeight) / 2;
            }

            // Dim background - draw a dark overlay
            drawRect(0, 0, screenWidth, screenHeight, new Color(0, 0, 0, 150).getRGB());

            // Handle drag
            if (dragging) {
                guiX = mouseX - dragOffsetX;
                guiY = mouseY - dragOffsetY;
            }

            // Main background
            drawRect(guiX, guiY, guiX + guiWidth, guiY + guiHeight, COL_BG);

            // Header
            drawRect(guiX, guiY, guiX + guiWidth, guiY + 25, COL_HEADER);

            GlStateManager.enableTexture2D();
            mc.fontRendererObj.drawString("EXODAR", guiX + 8, guiY + 8, COL_ACCENT);
            GlStateManager.disableTexture2D();

            // Category buttons
            int catX = guiX + 5;
            int catY = guiY + 30;
            ModuleCategory[] cats = {ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.PLAYER, ModuleCategory.RENDER, ModuleCategory.MISC};

            for (ModuleCategory cat : cats) {
                boolean selected = cat == selectedCategory;
                boolean hovered = mouseX >= catX && mouseX < catX + 85 && mouseY >= catY && mouseY < catY + 18;

                if (selected) {
                    drawRect(catX, catY, catX + 85, catY + 18, COL_ACCENT);
                } else if (hovered) {
                    drawRect(catX, catY, catX + 85, catY + 18, COL_HOVER);
                }

                String name = cat.name().charAt(0) + cat.name().substring(1).toLowerCase();
                GlStateManager.enableTexture2D();
                mc.fontRendererObj.drawString(name, catX + 5, catY + 5, selected ? 0xFFFFFF : COL_GRAY);
                GlStateManager.disableTexture2D();
                catX += 88;
            }

            // Modules list
            int listY = guiY + 55;
            int listHeight = guiHeight - 60;

            List<Module> modules = getModulesForCategory(selectedCategory);
            hoveredModule = null;

            int modY = listY - scroll;
            for (Module mod : modules) {
                if (modY >= listY && modY < listY + listHeight - 20) {
                    boolean hovered = mouseX >= guiX + 5 && mouseX < guiX + guiWidth - 5 &&
                                     mouseY >= modY && mouseY < modY + 22;

                    if (hovered) {
                        hoveredModule = mod;
                        drawRect(guiX + 5, modY, guiX + guiWidth - 5, modY + 22, COL_HOVER);
                    }

                    // Enabled indicator
                    if (mod.isEnabled()) {
                        drawRect(guiX + 5, modY, guiX + 8, modY + 22, COL_ACCENT);
                    }

                    GlStateManager.enableTexture2D();
                    mc.fontRendererObj.drawString(mod.getName(), guiX + 15, modY + 7, mod.isEnabled() ? COL_TEXT : COL_GRAY);

                    // ON/OFF
                    String status = mod.isEnabled() ? "ON" : "OFF";
                    int statusColor = mod.isEnabled() ? 0x55FF55 : COL_GRAY;
                    mc.fontRendererObj.drawString(status, guiX + guiWidth - 30, modY + 7, statusColor);
                    GlStateManager.disableTexture2D();
                }
                modY += 24;
            }

            // Restore GL state
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();

        } catch (Exception e) {
            System.err.println("[ModernClickGui] Error in drawScreen: " + e.getMessage());
            e.printStackTrace();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        System.out.println("[ModernClickGui] mouseClicked at " + mouseX + ", " + mouseY);

        // Header drag
        if (mouseX >= guiX && mouseX < guiX + guiWidth && mouseY >= guiY && mouseY < guiY + 25) {
            dragging = true;
            dragOffsetX = mouseX - guiX;
            dragOffsetY = mouseY - guiY;
        }

        // Category click
        int catX = guiX + 5;
        int catY = guiY + 30;
        ModuleCategory[] cats = {ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.PLAYER, ModuleCategory.RENDER, ModuleCategory.MISC};

        for (ModuleCategory cat : cats) {
            if (mouseX >= catX && mouseX < catX + 85 && mouseY >= catY && mouseY < catY + 18) {
                selectedCategory = cat;
                scroll = 0;
                break;
            }
            catX += 88;
        }

        // Module toggle
        if (hoveredModule != null && mouseButton == 0) {
            hoveredModule.toggle();
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll += wheel > 0 ? -20 : 20;
            if (scroll < 0) scroll = 0;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        System.out.println("[ModernClickGui] keyTyped: " + keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private List<Module> getModulesForCategory(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        if (moduleManager != null) {
            for (Module m : moduleManager.getModules()) {
                if (m.getCategory() == category) {
                    result.add(m);
                }
            }
        }
        return result;
    }
}
