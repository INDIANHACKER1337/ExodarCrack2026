/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui.modern;

import io.github.exodar.Main;
import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern ClickGUI - Client-master style with Exodar purple theme
 */
public class ModernClickGuiRenderer {

    private static ModernClickGuiRenderer instance;
    private static boolean visible = false;

    // Layout
    private static int guiX = -1, guiY = -1;
    private int guiWidth = 420;
    private int guiHeight = 280;

    // State
    private static ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private Module hoveredModule = null;
    private Module expandedModule = null;
    private Setting hoveredSetting = null;

    // Drag
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    // Scroll
    private int scroll = 0;
    private int maxScroll = 0;

    // Slider dragging
    private SliderSetting draggingSlider = null;
    private int sliderDragX = 0;

    // Mouse state
    private boolean wasMouseDown = false;
    private boolean wasRightMouseDown = false;

    // Animation
    private float openAnimation = 0f;
    private long lastFrame = System.currentTimeMillis();

    // ===== EXODAR PURPLE THEME (based on client-master cyan) =====
    // Backgrounds
    private static final Color BG_PRIMARY = new Color(13, 13, 18);      // Main background
    private static final Color BG_SECONDARY = new Color(18, 18, 26);    // Cards
    private static final Color BG_TERTIARY = new Color(26, 26, 36);     // Hover

    // Accent colors (Purple/Violet)
    private static final Color ACCENT_PRIMARY = new Color(139, 92, 246);   // #8B5CF6 violet-500
    private static final Color ACCENT_SECONDARY = new Color(167, 139, 250); // #A78BFA violet-400
    private static final Color ACCENT_GLOW = new Color(139, 92, 246, 80);

    // Status colors
    private static final Color STATUS_ON = new Color(34, 197, 94);      // Green
    private static final Color STATUS_OFF = new Color(100, 100, 120);   // Gray

    // Text colors
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(180, 180, 200);
    private static final Color TEXT_TERTIARY = new Color(120, 120, 140);

    // Border
    private static final Color BORDER_DEFAULT = new Color(45, 45, 60);
    private static final Color BORDER_ACCENT = new Color(139, 92, 246, 150);

    private ModernClickGuiRenderer() {}

    public static ModernClickGuiRenderer getInstance() {
        if (instance == null) {
            instance = new ModernClickGuiRenderer();
        }
        return instance;
    }

    public static void toggle() {
        visible = !visible;
        if (visible) {
            instance.openAnimation = 0f;
            if (guiX < 0 || guiY < 0) {
                Minecraft mc = Minecraft.getMinecraft();
                ScaledResolution sr = new ScaledResolution(mc);
                guiX = (sr.getScaledWidth() - 420) / 2;
                guiY = (sr.getScaledHeight() - 280) / 2;
            }
        }
    }

    public static void open() { visible = true; }
    public static void close() { visible = false; }
    public static boolean isVisible() { return visible; }

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!visible) {
            openAnimation = 0f;
            return;
        }

        // Delta time for smooth animation
        long now = System.currentTimeMillis();
        float delta = (now - lastFrame) / 1000f;
        lastFrame = now;

        // Smooth open animation
        openAnimation = Math.min(1f, openAnimation + delta * 8f);
        float alpha = openAnimation;
        float scale = 0.9f + openAnimation * 0.1f;

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        // Mouse position
        int mouseX = Mouse.getX() * screenWidth / mc.displayWidth;
        int mouseY = screenHeight - Mouse.getY() * screenHeight / mc.displayHeight - 1;

        // Handle input
        handleInput(mouseX, mouseY, fr);

        // ESC to close
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            close();
            return;
        }

        // Drag handling
        if (dragging) {
            guiX = mouseX - dragOffsetX;
            guiY = mouseY - dragOffsetY;
        }

        // Clamp position
        guiX = Math.max(0, Math.min(screenWidth - guiWidth, guiX));
        guiY = Math.max(0, Math.min(screenHeight - guiHeight, guiY));

        // Setup GL
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // === BACKGROUND OVERLAY ===
        drawRect(0, 0, screenWidth, screenHeight, new Color(0, 0, 0, (int)(100 * alpha)).getRGB());

        // === MAIN WINDOW ===
        // Shadow/glow
        drawRoundedRect(guiX - 1, guiY - 1, guiWidth + 2, guiHeight + 2, 12,
            withAlpha(ACCENT_GLOW, alpha));

        // Main background
        drawRoundedRect(guiX, guiY, guiWidth, guiHeight, 10, withAlpha(BG_PRIMARY, alpha));

        // Border
        drawRoundedRectOutline(guiX, guiY, guiWidth, guiHeight, 10, withAlpha(BORDER_DEFAULT, alpha));

        // === HEADER ===
        int headerHeight = 35;
        drawRoundedRectTop(guiX, guiY, guiWidth, headerHeight, 10, withAlpha(BG_SECONDARY, alpha));

        // Header bottom border
        drawRect(guiX, guiY + headerHeight - 1, guiWidth, 1, withAlpha(BORDER_DEFAULT, alpha));

        // Logo
        GlStateManager.enableTexture2D();
        fr.drawStringWithShadow("EXODAR", guiX + 12, guiY + 12, withAlpha(ACCENT_PRIMARY, alpha));

        // Version badge
        String version = "v1.0";
        int versionWidth = fr.getStringWidth(version) + 8;
        GlStateManager.disableTexture2D();
        drawRoundedRect(guiX + 55, guiY + 10, versionWidth, 14, 4, withAlpha(new Color(139, 92, 246, 30), alpha));
        GlStateManager.enableTexture2D();
        fr.drawString(version, guiX + 59, guiY + 13, withAlpha(TEXT_TERTIARY, alpha));
        GlStateManager.disableTexture2D();

        // === CATEGORY TABS ===
        int tabY = guiY + headerHeight + 8;
        int tabX = guiX + 10;
        ModuleCategory[] categories = {ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.PLAYER, ModuleCategory.RENDER, ModuleCategory.MISC};

        for (ModuleCategory cat : categories) {
            String name = cat.name().substring(0, 1) + cat.name().substring(1).toLowerCase();
            int tabWidth = fr.getStringWidth(name) + 16;
            boolean selected = cat == selectedCategory;
            boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + 20;

            if (selected) {
                // Selected tab - accent background with glow
                drawRoundedRect(tabX, tabY, tabWidth, 20, 6, withAlpha(ACCENT_PRIMARY, alpha));
            } else if (hovered) {
                // Hovered tab
                drawRoundedRect(tabX, tabY, tabWidth, 20, 6, withAlpha(BG_TERTIARY, alpha));
            }

            GlStateManager.enableTexture2D();
            int textColor = selected ? withAlpha(TEXT_PRIMARY, alpha) :
                           (hovered ? withAlpha(TEXT_SECONDARY, alpha) : withAlpha(TEXT_TERTIARY, alpha));
            fr.drawString(name, tabX + 8, tabY + 6, textColor);
            GlStateManager.disableTexture2D();

            tabX += tabWidth + 6;
        }

        // === MODULE LIST ===
        int listY = guiY + headerHeight + 36;
        int listHeight = guiHeight - headerHeight - 44;
        int listX = guiX + 10;
        int listWidth = guiWidth - 20;

        // Modules
        ModuleManager moduleManager = Main.getModuleManager();
        List<Module> modules = getModulesForCategory(selectedCategory, moduleManager);
        hoveredModule = null;

        int moduleY = listY - scroll;
        int totalHeight = 0;

        for (Module mod : modules) {
            int moduleHeight = 32;
            if (mod == expandedModule) {
                moduleHeight += getSettingsHeight(mod, fr);
            }

            // Only render if visible
            if (moduleY + moduleHeight > listY && moduleY < listY + listHeight) {
                boolean hovered = mouseX >= listX && mouseX < listX + listWidth &&
                                 mouseY >= Math.max(moduleY, listY) && mouseY < Math.min(moduleY + 32, listY + listHeight);

                if (hovered && mouseY >= listY && mouseY < listY + listHeight) {
                    hoveredModule = mod;
                }

                // Module card
                renderModuleCard(mod, listX, moduleY, listWidth, hovered, alpha, fr, mouseX, mouseY, listY, listHeight);
            }

            moduleY += moduleHeight + 6;
            totalHeight += moduleHeight + 6;
        }

        maxScroll = Math.max(0, totalHeight - listHeight);

        // Restore GL
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderModuleCard(Module mod, int x, int y, int width, boolean hovered, float alpha,
                                   FontRenderer fr, int mouseX, int mouseY, int clipTop, int clipHeight) {
        boolean enabled = mod.isEnabled();
        boolean expanded = mod == expandedModule;

        // Card background
        Color cardBg = enabled ? new Color(139, 92, 246, 15) : BG_SECONDARY;
        if (hovered) cardBg = enabled ? new Color(139, 92, 246, 25) : BG_TERTIARY;

        int cardHeight = 32;
        if (expanded) cardHeight += getSettingsHeight(mod, fr);

        drawRoundedRect(x, y, width, cardHeight, 8, withAlpha(cardBg, alpha));

        // Border (accent when enabled)
        Color borderColor = enabled ? BORDER_ACCENT : BORDER_DEFAULT;
        drawRoundedRectOutline(x, y, width, cardHeight, 8, withAlpha(borderColor, alpha));

        // Left accent bar when enabled
        if (enabled) {
            drawRoundedRect(x, y + 4, 3, 24, 2, withAlpha(ACCENT_PRIMARY, alpha));
        }

        // Module name
        GlStateManager.enableTexture2D();
        int nameColor = enabled ? withAlpha(TEXT_PRIMARY, alpha) : withAlpha(TEXT_SECONDARY, alpha);
        fr.drawString(mod.getName(), x + 12, y + 12, nameColor);
        GlStateManager.disableTexture2D();

        // Toggle switch
        int switchX = x + width - 50;
        int switchY = y + 9;
        int switchW = 38;
        int switchH = 14;

        // Switch background
        Color switchBg = enabled ? ACCENT_PRIMARY : new Color(60, 60, 75);
        drawRoundedRect(switchX, switchY, switchW, switchH, 7, withAlpha(switchBg, alpha));

        // Switch knob
        int knobX = enabled ? switchX + switchW - 13 : switchX + 3;
        drawCircle(knobX + 5, switchY + 7, 4, withAlpha(TEXT_PRIMARY, alpha));

        // Settings indicator (gear icon as ">")
        if (mod.getSettings().size() > 1) { // More than just description
            GlStateManager.enableTexture2D();
            String arrow = expanded ? "v" : ">";
            int arrowX = x + width - 62;
            boolean arrowHovered = mouseX >= arrowX - 5 && mouseX < arrowX + 10 &&
                                   mouseY >= y + 8 && mouseY < y + 24;
            int arrowColor = arrowHovered ? withAlpha(ACCENT_PRIMARY, alpha) : withAlpha(TEXT_TERTIARY, alpha);
            fr.drawString(arrow, arrowX, y + 12, arrowColor);
            GlStateManager.disableTexture2D();
        }

        // Expanded settings
        if (expanded) {
            int setY = y + 36;

            // Separator
            drawRect(x + 10, y + 32, width - 20, 1, withAlpha(BORDER_DEFAULT, alpha));

            for (Setting setting : mod.getSettings()) {
                if (setting instanceof DescriptionSetting) continue;
                if (!setting.isVisible()) continue;

                boolean setHovered = mouseX >= x + 5 && mouseX < x + width - 5 &&
                                    mouseY >= setY && mouseY < setY + 22;
                if (setHovered) hoveredSetting = setting;

                if (setting instanceof TickSetting) {
                    renderTickSetting((TickSetting) setting, x + 12, setY, width - 24, alpha, fr);
                    setY += 24;
                } else if (setting instanceof SliderSetting) {
                    renderSliderSetting((SliderSetting) setting, x + 12, setY, width - 24, alpha, fr, mouseX);
                    setY += 32;
                } else if (setting instanceof ModeSetting) {
                    renderModeSetting((ModeSetting) setting, x + 12, setY, width - 24, alpha, fr);
                    setY += 24;
                }
            }
        }
    }

    private void renderTickSetting(TickSetting setting, int x, int y, int width, float alpha, FontRenderer fr) {
        GlStateManager.enableTexture2D();
        fr.drawString(setting.getName(), x, y + 6, withAlpha(TEXT_SECONDARY, alpha));
        GlStateManager.disableTexture2D();

        // Mini toggle
        int switchX = x + width - 28;
        int switchY = y + 4;
        int switchW = 24;
        int switchH = 12;

        Color switchBg = setting.isEnabled() ? ACCENT_PRIMARY : new Color(50, 50, 65);
        drawRoundedRect(switchX, switchY, switchW, switchH, 6, withAlpha(switchBg, alpha));

        int knobX = setting.isEnabled() ? switchX + switchW - 10 : switchX + 2;
        drawCircle(knobX + 4, switchY + 6, 3, withAlpha(TEXT_PRIMARY, alpha));
    }

    private void renderSliderSetting(SliderSetting setting, int x, int y, int width, float alpha, FontRenderer fr, int mouseX) {
        // Label
        GlStateManager.enableTexture2D();
        fr.drawString(setting.getName(), x, y + 2, withAlpha(TEXT_SECONDARY, alpha));

        // Value
        String valueStr = String.format("%.1f", setting.getValue());
        int valueWidth = fr.getStringWidth(valueStr);
        fr.drawString(valueStr, x + width - valueWidth, y + 2, withAlpha(ACCENT_SECONDARY, alpha));
        GlStateManager.disableTexture2D();

        // Track
        int trackY = y + 18;
        int trackHeight = 6;
        drawRoundedRect(x, trackY, width, trackHeight, 3, withAlpha(new Color(40, 40, 55), alpha));

        // Fill
        double percent = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        int fillWidth = (int) (width * percent);
        if (fillWidth > 0) {
            drawRoundedRect(x, trackY, fillWidth, trackHeight, 3, withAlpha(ACCENT_PRIMARY, alpha));
        }

        // Thumb
        int thumbX = x + fillWidth - 4;
        drawCircle(thumbX + 4, trackY + 3, 5, withAlpha(TEXT_PRIMARY, alpha));

        // Glow on thumb
        drawCircle(thumbX + 4, trackY + 3, 7, withAlpha(ACCENT_GLOW, alpha));

        // Handle dragging
        if (draggingSlider == setting) {
            double newPercent = Math.max(0, Math.min(1, (double)(mouseX - x) / width));
            double newValue = setting.getMin() + newPercent * (setting.getMax() - setting.getMin());
            double interval = setting.getInterval();
            newValue = Math.round(newValue / interval) * interval;
            setting.setValue(Math.max(setting.getMin(), Math.min(setting.getMax(), newValue)));
        }
    }

    private void renderModeSetting(ModeSetting setting, int x, int y, int width, float alpha, FontRenderer fr) {
        GlStateManager.enableTexture2D();
        fr.drawString(setting.getName(), x, y + 6, withAlpha(TEXT_SECONDARY, alpha));

        // Mode badge
        String mode = setting.getSelected();
        int modeWidth = fr.getStringWidth(mode) + 12;
        int modeX = x + width - modeWidth;

        GlStateManager.disableTexture2D();
        drawRoundedRect(modeX, y + 2, modeWidth, 16, 4, withAlpha(new Color(139, 92, 246, 40), alpha));
        drawRoundedRectOutline(modeX, y + 2, modeWidth, 16, 4, withAlpha(BORDER_ACCENT, alpha));
        GlStateManager.enableTexture2D();
        fr.drawString(mode, modeX + 6, y + 6, withAlpha(ACCENT_SECONDARY, alpha));
        GlStateManager.disableTexture2D();
    }

    private int getSettingsHeight(Module mod, FontRenderer fr) {
        int height = 8; // Padding
        for (Setting setting : mod.getSettings()) {
            if (setting instanceof DescriptionSetting) continue;
            if (!setting.isVisible()) continue;
            if (setting instanceof SliderSetting) {
                height += 32;
            } else {
                height += 24;
            }
        }
        return height;
    }

    private void handleInput(int mouseX, int mouseY, FontRenderer fr) {
        boolean mouseDown = Mouse.isButtonDown(0);
        boolean rightMouseDown = Mouse.isButtonDown(1);

        // Slider drag release
        if (!mouseDown) {
            draggingSlider = null;
            dragging = false;
        }

        // Left click
        if (mouseDown && !wasMouseDown) {
            // Header drag
            if (mouseX >= guiX && mouseX < guiX + guiWidth && mouseY >= guiY && mouseY < guiY + 35) {
                dragging = true;
                dragOffsetX = mouseX - guiX;
                dragOffsetY = mouseY - guiY;
            }

            // Category tabs
            int tabY = guiY + 35 + 8;
            int tabX = guiX + 10;
            ModuleCategory[] categories = {ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.PLAYER, ModuleCategory.RENDER, ModuleCategory.MISC};
            for (ModuleCategory cat : categories) {
                String name = cat.name().substring(0, 1) + cat.name().substring(1).toLowerCase();
                int tabWidth = fr.getStringWidth(name) + 16;
                if (mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + 20) {
                    selectedCategory = cat;
                    scroll = 0;
                    expandedModule = null;
                    break;
                }
                tabX += tabWidth + 6;
            }

            // Module interactions
            if (hoveredModule != null) {
                // Check if clicking on settings arrow
                int listX = guiX + 10;
                int listWidth = guiWidth - 20;
                int arrowX = listX + listWidth - 62;

                if (mouseX >= arrowX - 5 && mouseX < arrowX + 10) {
                    // Toggle expanded
                    expandedModule = (expandedModule == hoveredModule) ? null : hoveredModule;
                } else if (mouseX >= listX + listWidth - 50) {
                    // Toggle module (clicked on switch)
                    hoveredModule.toggle();
                } else {
                    // Toggle module (clicked anywhere else on card)
                    hoveredModule.toggle();
                }
            }

            // Settings interactions
            if (hoveredSetting != null && expandedModule != null) {
                if (hoveredSetting instanceof TickSetting) {
                    ((TickSetting) hoveredSetting).toggle();
                } else if (hoveredSetting instanceof SliderSetting) {
                    draggingSlider = (SliderSetting) hoveredSetting;
                } else if (hoveredSetting instanceof ModeSetting) {
                    ((ModeSetting) hoveredSetting).cycle();
                }
            }
        }

        // Continuous slider dragging
        if (mouseDown && draggingSlider != null) {
            // Slider is updated in renderSliderSetting
        }

        // Scroll
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            scroll -= wheel > 0 ? 20 : -20;
            scroll = Math.max(0, Math.min(maxScroll, scroll));
        }

        wasMouseDown = mouseDown;
        wasRightMouseDown = rightMouseDown;
        hoveredSetting = null;
    }

    private List<Module> getModulesForCategory(ModuleCategory category, ModuleManager moduleManager) {
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

    // ===== DRAWING HELPERS =====

    private int withAlpha(Color color, float alpha) {
        int a = (int) (color.getAlpha() * alpha);
        a = Math.max(0, Math.min(255, a)); // Clamp to valid range
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a).getRGB();
    }

    private void drawRect(int x, int y, int width, int height, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y + height);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
    }

    private void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        // Simplified - just draw rect with slightly darker corners to simulate roundness
        drawRect(x, y, width, height, color);
    }

    private void drawRoundedRectTop(int x, int y, int width, int height, int radius, int color) {
        drawRect(x, y, width, height, color);
    }

    private void drawRoundedRectOutline(int x, int y, int width, int height, int radius, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    private void drawCircle(int cx, int cy, int radius, int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 360; i += 15) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f((float)(cx + Math.cos(angle) * radius), (float)(cy + Math.sin(angle) * radius));
        }
        GL11.glEnd();
    }
}
