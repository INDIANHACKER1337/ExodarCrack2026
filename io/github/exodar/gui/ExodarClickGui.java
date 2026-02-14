/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.*;
import io.github.exodar.gui.clickgui.RenderHelper;
import io.github.exodar.ui.ArrayListConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ExodarClickGui extends GuiScreen {
    private ModuleManager moduleManager;

    // Static variables to remember position and state across GUI instances
    private static int savedX = -1;
    private static int savedY = -1;
    private static ModuleCategory savedCategory = ModuleCategory.COMBAT;

    private int x;
    private int y;
    private int width = 520;
    private int height = 380;

    // Scroll for modules
    private int moduleScroll = 0;
    private int maxModuleScroll = 0;

    private boolean dragging = false;
    private int dragX;
    private int dragY;

    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private Setting draggedSetting = null;

    // Keybind system
    private Module bindingModule = null;

    // Module height cache for performance
    private Map<Module, Integer> cachedHeights = new HashMap<>();

    // Cached display dimension fields
    private static java.lang.reflect.Field cachedDisplayWidthField = null;
    private static java.lang.reflect.Field cachedDisplayHeightField = null;
    private static boolean fieldsInitialized = false;

    public ExodarClickGui(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @Override
    public void initGui() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = ScaledResolutionHelper.create(mc);

        // Use saved position if available, otherwise center
        if (savedX == -1 || savedY == -1) {
            x = (sr.getScaledWidth() - width) / 2;
            y = (sr.getScaledHeight() - height) / 2;
        } else {
            x = savedX;
            y = savedY;
        }

        // Restore saved category
        selectedCategory = savedCategory;

        super.initGui();
    }

    // COLORES MEJORADOS - Más contraste y modernos
    private static final int BACKGROUND = new Color(15, 15, 20, 245).getRGB();
    private static final int PANEL_BG = new Color(25, 25, 35, 255).getRGB();
    private static final int ACCENT = new Color(120, 80, 255, 255).getRGB();  // Púrpura más brillante
    private static final int TEXT_COLOR = new Color(220, 220, 220, 255).getRGB();  // Más claro
    private static final int BORDER = new Color(120, 80, 255, 180).getRGB();  // Más visible
    private static final int TITLE_BAR_BG = new Color(20, 20, 28, 255).getRGB();
    private static final int CHECKBOX_OFF = new Color(45, 45, 60, 255).getRGB();
    private static final int SLIDER_BG = new Color(35, 35, 50, 255).getRGB();
    private static final int KEYBIND_GRAY = new Color(180, 180, 190, 255).getRGB();  // Más claro
    private static final int HOVER_BG = new Color(35, 35, 50, 255).getRGB();  // Para hover effects

    // TAMAÑOS MEJORADOS - Más espacio, más legible
    private static final int CATEGORY_WIDTH = 90;
    private static final int MODULE_WIDTH = 150;
    private static final int MODULE_HEIGHT_BASE = 24;
    private static final int SETTING_HEIGHT = 20;  // Más espacio
    private static final int MODULES_PER_ROW = 3;
    private static final int MODULE_SPACING = 10;  // Más espacio entre módulos

    // Método mejorado para calcular altura de módulo con caché
    private int getModuleHeight(Module module) {
        if (cachedHeights.containsKey(module)) {
            return cachedHeights.get(module);
        }

        int height = 18 + 18 + 6; // title bar + enable checkbox + padding (más grande)

        for (Setting setting : module.getSettings()) {
            if (setting instanceof DescriptionSetting || !setting.isVisible()) {
                continue;
            }

            if (setting instanceof SliderSetting || setting instanceof DoubleSliderSetting) {
                height += 22; // Más altura para sliders
            } else if (setting instanceof TickSetting) {
                height += 16; // Más altura para checkboxes
            } else if (setting instanceof ModeSetting) {
                height += 16; // Altura para mode selector
            }
        }

        height += 6; // Extra padding

        cachedHeights.put(module, height);
        return height;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Manejar dragging (FIX LAG: mover de updateScreen a drawScreen)
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = ScaledResolutionHelper.create(mc);
            int screenWidth = sr.getScaledWidth();
            int screenHeight = sr.getScaledHeight();

            // Keep at least 50px of the GUI visible
            int minX = -width + 50;
            int maxX = screenWidth - 50;
            int minY = 0;
            int maxY = screenHeight - 30;

            x = Math.max(minX, Math.min(maxX, x));
            y = Math.max(minY, Math.min(maxY, y));

            // Save position while dragging
            savedX = x;
            savedY = y;
        }

        // Draw main panel with glow effect
        // Outer glow
        drawRect(x - 2, y - 2, x + width + 2, y + height + 2, new Color(120, 80, 255, 40).getRGB());
        drawRect(x - 1, y - 1, x + width + 1, y + height + 1, new Color(120, 80, 255, 60).getRGB());

        // Main background
        drawRect(x, y, x + width, y + height, BACKGROUND);

        // Purple borders (2px thick for visibility)
        int borderColor = new Color(140, 90, 255, 220).getRGB();
        drawRect(x, y, x + width, y + 2, borderColor); // Top border
        drawRect(x, y, x + 2, y + height, borderColor); // Left border
        drawRect(x + width - 2, y, x + width, y + height, borderColor); // Right border
        drawRect(x, y + height - 2, x + width, y + height, borderColor); // Bottom border

        // Draw title bar
        drawRect(x, y, x + width, y + 25, PANEL_BG);
        RenderHelper.drawString("EXODAR", x + 10, y + 8, ACCENT);
        RenderHelper.drawString("v2.5", x + width - 35, y + 8, new Color(100, 100, 100).getRGB());

        // Draw category panel
        drawCategoryPanel(mouseX, mouseY);

        // Draw modules panel
        drawModulesPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCategoryPanel(int mouseX, int mouseY) {
        int categoryX = x + 10;
        int categoryY = y + 35;
        int categoryHeight = 30;

        for (ModuleCategory category : ModuleCategory.values()) {
            boolean isSelected = category == selectedCategory;
            boolean isHovered = mouseX >= categoryX && mouseX <= categoryX + CATEGORY_WIDTH &&
                               mouseY >= categoryY && mouseY <= categoryY + categoryHeight;

            // Draw category background
            int bgColor = isSelected ? ACCENT : (isHovered ? PANEL_BG : BACKGROUND);
            drawRect(categoryX, categoryY, categoryX + CATEGORY_WIDTH, categoryY + categoryHeight, bgColor);

            // Draw category text
            int textColor = isSelected ? Color.WHITE.getRGB() : TEXT_COLOR;
            RenderHelper.drawCenteredString(category.getName(),
                             categoryX + CATEGORY_WIDTH / 2, categoryY + 10, textColor);

            categoryY += categoryHeight + 5;
        }
    }

    private void drawModulesPanel(int mouseX, int mouseY) {
        int modulesX = x + 10 + CATEGORY_WIDTH + 10;
        int modulesY = y + 35;
        int modulesHeight = height - 45; // Available height for modules

        List<Module> categoryModules = getModulesForCategory(selectedCategory);

        // Calculate total content height for scroll
        int totalContentHeight = 0;
        int col = 0;
        int rowHeight = 0;
        for (Module module : categoryModules) {
            int moduleHeight = getModuleHeight(module);
            rowHeight = Math.max(rowHeight, moduleHeight);
            col++;
            if (col >= MODULES_PER_ROW) {
                col = 0;
                totalContentHeight += rowHeight + MODULE_SPACING;
                rowHeight = 0;
            }
        }
        if (col > 0) {
            totalContentHeight += rowHeight + MODULE_SPACING;
        }

        maxModuleScroll = Math.max(0, totalContentHeight - modulesHeight + 20);
        moduleScroll = Math.max(0, Math.min(moduleScroll, maxModuleScroll));

        // Draw modules with scroll offset
        col = 0;
        int currentY = modulesY - moduleScroll;
        rowHeight = 0;

        for (Module module : categoryModules) {
            try {
                int moduleHeight = getModuleHeight(module);
                int moduleX = modulesX + col * (MODULE_WIDTH + MODULE_SPACING);
                int moduleY = currentY;

                // Only draw if visible
                if (moduleY + moduleHeight > modulesY - 10 && moduleY < y + height - 10) {
                    drawModulePanel(module, moduleX, moduleY, moduleHeight, mouseX, mouseY);
                }

                rowHeight = Math.max(rowHeight, moduleHeight);
                col++;
                if (col >= MODULES_PER_ROW) {
                    col = 0;
                    currentY += rowHeight + MODULE_SPACING;
                    rowHeight = 0;
                }
            } catch (Exception e) {
                // Silently ignore
            }
        }

        // Draw scroll indicator if needed
        if (maxModuleScroll > 0) {
            int scrollBarX = x + width - 8;
            int scrollBarY = modulesY;
            int scrollBarHeight = modulesHeight;

            // Scroll bar background
            drawRect(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, new Color(40, 40, 50).getRGB());

            // Scroll bar thumb
            float scrollPercent = (float) moduleScroll / maxModuleScroll;
            int thumbHeight = Math.max(20, (int) (scrollBarHeight * (float) modulesHeight / totalContentHeight));
            int thumbY = scrollBarY + (int) ((scrollBarHeight - thumbHeight) * scrollPercent);
            drawRect(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbHeight, ACCENT);
        }
    }

    // Método mejorado para dibujar el panel de módulo con sombras
    private void drawModulePanel(Module module, int panelX, int panelY, int moduleHeight, int mouseX, int mouseY) {
        try {
            boolean isHovered = mouseX >= panelX && mouseX <= panelX + MODULE_WIDTH &&
                    mouseY >= panelY && mouseY <= panelY + moduleHeight;

            // Sombra sutil
            drawRect(panelX + 2, panelY + 2, panelX + MODULE_WIDTH + 2, panelY + moduleHeight + 2,
                    new Color(0, 0, 0, 50).getRGB());

            // Draw panel background con hover effect
            int bgColor = isHovered ? HOVER_BG : PANEL_BG;
            drawRect(panelX, panelY, panelX + MODULE_WIDTH, panelY + moduleHeight, bgColor);

            // Bordes más visibles (2px)
            int borderColor = isHovered ? new Color(140, 100, 255, 200).getRGB() : BORDER;

            // Borde superior
            drawRect(panelX, panelY, panelX + MODULE_WIDTH, panelY + 2, borderColor);
            // Borde izquierdo
            drawRect(panelX, panelY, panelX + 2, panelY + moduleHeight, borderColor);
            // Borde derecho
            drawRect(panelX + MODULE_WIDTH - 2, panelY, panelX + MODULE_WIDTH, panelY + moduleHeight, borderColor);
            // Borde inferior
            drawRect(panelX, panelY + moduleHeight - 2, panelX + MODULE_WIDTH, panelY + moduleHeight, borderColor);

            // Title bar más grande
            int titleBarHeight = 18;
            drawRect(panelX + 2, panelY + 2, panelX + MODULE_WIDTH - 2, panelY + titleBarHeight, TITLE_BAR_BG);

            // Module name centrado y más visible
            String moduleName = module.getName();
            int nameWidth = RenderHelper.getStringWidth(moduleName);
            int nameX = panelX + (MODULE_WIDTH - nameWidth) / 2;

            // Sombra del texto
            RenderHelper.drawString(moduleName, nameX + 1, panelY + 5, new Color(0, 0, 0, 100).getRGB());
            RenderHelper.drawString(moduleName, nameX, panelY + 4, ACCENT);

            // Enable checkbox más grande
            int contentY = panelY + titleBarHeight + 4;
            int checkboxSize = 10;  // Más grande
            int checkboxX = panelX + 6;

            // Checkbox con borde
            int checkboxColor = module.isEnabled() ? ACCENT : CHECKBOX_OFF;
            drawRect(checkboxX, contentY, checkboxX + checkboxSize, contentY + checkboxSize, checkboxColor);

            // Borde del checkbox
            if (!module.isEnabled()) {
                drawRect(checkboxX, contentY, checkboxX + checkboxSize, contentY + 1, BORDER);
                drawRect(checkboxX, contentY, checkboxX + 1, contentY + checkboxSize, BORDER);
                drawRect(checkboxX + checkboxSize - 1, contentY, checkboxX + checkboxSize, contentY + checkboxSize, BORDER);
                drawRect(checkboxX, contentY + checkboxSize - 1, checkboxX + checkboxSize, contentY + checkboxSize, BORDER);
            }

            // "Enable" text más claro
            RenderHelper.drawString("Enable", checkboxX + checkboxSize + 4, contentY + 1, TEXT_COLOR);

            // Keybind button mejorado
            String keybindText = bindingModule == module ? "..." : getKeyName(module.getKeyCode());
            int keybindColor = bindingModule == module ? ACCENT : KEYBIND_GRAY;
            int keybindX = panelX + MODULE_WIDTH - 24;

            // Background para keybind
            drawRect(keybindX - 2, contentY - 1, keybindX + 22, contentY + 11, SLIDER_BG);
            RenderHelper.drawCenteredString(keybindText, keybindX + 10, contentY + 1, keybindColor);

            contentY += 18;

            // Draw settings con más espacio
            for (Setting setting : module.getSettings()) {
                if (setting instanceof DescriptionSetting || !setting.isVisible()) {
                    continue;
                }

                if (contentY + SETTING_HEIGHT > panelY + moduleHeight - 6) {
                    break;
                }

                contentY += drawSettingImproved(setting, panelX + 4, contentY, MODULE_WIDTH - 8, mouseX, mouseY);
            }
        } catch (Exception e) {
            // Silently ignore
        }
    }

    // Método mejorado para dibujar settings
    private int drawSettingImproved(Setting setting, int x, int y, int width, int mouseX, int mouseY) {
        if (setting instanceof SliderSetting) {
            SliderSetting slider = (SliderSetting) setting;

            // Nombre y valor más legibles
            String name = slider.getName();
            String value = String.format("%.1f", slider.getValue());
            RenderHelper.drawString(name, x + 2, y, TEXT_COLOR);
            int valueWidth = RenderHelper.getStringWidth(value);
            RenderHelper.drawString(value, x + width - valueWidth - 2, y, ACCENT);

            // Slider bar más grande y con hover
            int sliderY = y + 12;
            int sliderHeight = 4;  // Más alto
            boolean isHovered = mouseX >= x + 2 && mouseX <= x + width - 2 &&
                    mouseY >= sliderY && mouseY <= sliderY + sliderHeight;

            // Background del slider
            drawRect(x + 2, sliderY, x + width - 2, sliderY + sliderHeight, SLIDER_BG);

            // Borde si está en hover
            if (isHovered) {
                drawRect(x + 2, sliderY, x + width - 2, sliderY + 1, BORDER);
                drawRect(x + 2, sliderY + sliderHeight - 1, x + width - 2, sliderY + sliderHeight, BORDER);
            }

            // Filled portion
            double percentage = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin());
            int filledWidth = (int) ((width - 4) * percentage);
            drawRect(x + 2, sliderY, x + 2 + filledWidth, sliderY + sliderHeight, ACCENT);

            // Handle dragging
            if (draggedSetting == slider && Mouse.isButtonDown(0)) {
                double newPercentage = Math.max(0, Math.min(1, (double) (mouseX - (x + 2)) / (width - 4)));
                double newValue = slider.getMin() + newPercentage * (slider.getMax() - slider.getMin());
                double interval = slider.getInterval();
                newValue = Math.round(newValue / interval) * interval;
                slider.setValue(newValue);
            }

            return 22;  // Más altura

        } else if (setting instanceof DoubleSliderSetting) {
            DoubleSliderSetting slider = (DoubleSliderSetting) setting;

            String name = slider.getName();
            String value = String.format("%.1f-%.1f", slider.getValueMin(), slider.getValueMax());
            RenderHelper.drawString(name, x + 2, y, TEXT_COLOR);
            int valueWidth = RenderHelper.getStringWidth(value);
            RenderHelper.drawString(value, x + width - valueWidth - 2, y, ACCENT);

            int sliderY = y + 12;
            int sliderHeight = 4;
            drawRect(x + 2, sliderY, x + width - 2, sliderY + sliderHeight, SLIDER_BG);

            double minPercentage = (slider.getValueMin() - slider.getMin()) / (slider.getMax() - slider.getMin());
            double maxPercentage = (slider.getValueMax() - slider.getMin()) / (slider.getMax() - slider.getMin());

            int minX = (int) (x + 2 + (width - 4) * minPercentage);
            int maxX = (int) (x + 2 + (width - 4) * maxPercentage);

            drawRect(minX, sliderY, maxX, sliderY + sliderHeight, ACCENT);

            return 22;

        } else if (setting instanceof TickSetting) {
            TickSetting tick = (TickSetting) setting;

            int checkboxSize = 10;  // Más grande
            int checkboxColor = tick.isEnabled() ? ACCENT : CHECKBOX_OFF;

            // Checkbox con borde
            drawRect(x + 2, y, x + 2 + checkboxSize, y + checkboxSize, checkboxColor);

            if (!tick.isEnabled()) {
                drawRect(x + 2, y, x + 2 + checkboxSize, y + 1, BORDER);
                drawRect(x + 2, y, x + 3, y + checkboxSize, BORDER);
                drawRect(x + 2 + checkboxSize - 1, y, x + 2 + checkboxSize, y + checkboxSize, BORDER);
                drawRect(x + 2, y + checkboxSize - 1, x + 2 + checkboxSize, y + checkboxSize, BORDER);
            }

            RenderHelper.drawString(tick.getName(), x + 2 + checkboxSize + 4, y + 1, TEXT_COLOR);

            return 16;  // Más altura

        } else if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) setting;

            // Draw mode name
            String name = mode.getName() + ":";
            RenderHelper.drawString(name, x + 2, y, TEXT_COLOR);

            // Draw selected option with accent color (clickable area)
            String selected = mode.getSelected();
            int nameWidth = RenderHelper.getStringWidth(name);
            int selectedWidth = RenderHelper.getStringWidth(selected);

            // Draw background for selected option (clickable indicator)
            int optionX = x + nameWidth + 6;
            int optionWidth = selectedWidth + 8;
            boolean isHovered = mouseX >= optionX - 2 && mouseX <= optionX + optionWidth &&
                    mouseY >= y - 2 && mouseY <= y + 12;

            if (isHovered) {
                drawRect(optionX - 2, y - 1, optionX + optionWidth, y + 11, SLIDER_BG);
            }

            RenderHelper.drawString(selected, optionX + 2, y, isHovered ? Color.WHITE.getRGB() : ACCENT);

            // Draw arrows indicator
            RenderHelper.drawString("<>", x + width - 16, y, KEYBIND_GRAY);

            return 16;
        }

        return 0;
    }

    private String getKeyName(int keyCode) {
        if (keyCode == 0) return "-";
        if (keyCode < 0) {
            // Mouse button
            return "M" + (-keyCode - 100);
        }
        String name = Keyboard.getKeyName(keyCode);
        return name != null ? name : "-";
    }

    private List<Module> getModulesForCategory(ModuleCategory category) {
        List<Module> result = new ArrayList<>();
        for (Module module : moduleManager.getModules()) {
            if (module.getCategory() == category && !module.isHidden()) {
                result.add(module);
            }
        }
        return result;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Check if clicking title bar to drag
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 25) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
            return;
        }

        // Check category clicks
        int categoryX = x + 10;
        int categoryY = y + 35;
        int categoryHeight = 30;

        for (ModuleCategory category : ModuleCategory.values()) {
            if (mouseX >= categoryX && mouseX <= categoryX + CATEGORY_WIDTH &&
                mouseY >= categoryY && mouseY <= categoryY + categoryHeight) {
                selectedCategory = category;
                savedCategory = category; // Save selected category
                moduleScroll = 0; // Reset scroll when changing category
                return;
            }
            categoryY += categoryHeight + 5;
        }

        // Check module clicks
        int modulesX = x + 10 + CATEGORY_WIDTH + 10;
        int modulesY = y + 35;

        List<Module> categoryModules = getModulesForCategory(selectedCategory);

        int row = 0;
        int col = 0;

        int currentY = modulesY;
        for (Module module : categoryModules) {
            int moduleHeight = getModuleHeight(module);
            int moduleX = modulesX + col * (MODULE_WIDTH + MODULE_SPACING);
            int moduleY = currentY;

            if (moduleY + moduleHeight > y + height - 10) {
                break;
            }

            // Check keybind button click
            int titleBarHeight = 18;
            int contentY = moduleY + titleBarHeight + 4;
            int keybindX = moduleX + MODULE_WIDTH - 24;

            if (mouseX >= keybindX - 2 && mouseX <= keybindX + 22 &&
                mouseY >= contentY - 1 && mouseY <= contentY + 11) {
                bindingModule = module;
                return;
            }

            // Check module toggle click
            // Accept BOTH left and right click for better UX (fixes invertMenuClicks issues)
            int checkboxX = moduleX + 6;
            int checkboxSize = 10;

            if (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
                mouseY >= contentY && mouseY <= contentY + checkboxSize &&
                (mouseButton == 0 || mouseButton == 1)) {
                module.toggle();
                cachedHeights.remove(module); // Invalidate cache on toggle
                return;
            }

            // Check setting clicks
            int settingY = contentY + 18;
            for (Setting setting : module.getSettings()) {
                if (setting instanceof DescriptionSetting) {
                    continue;
                }

                // Skip invisible settings
                if (!setting.isVisible()) {
                    continue;
                }

                if (settingY + SETTING_HEIGHT > moduleY + moduleHeight - 6) {
                    break;
                }

                if (setting instanceof SliderSetting) {
                    int sliderY = settingY + 12;
                    int sliderHeight = 4;
                    if (mouseX >= moduleX + 4 + 2 && mouseX <= moduleX + MODULE_WIDTH - 4 - 2 &&
                        mouseY >= sliderY && mouseY <= sliderY + sliderHeight) {
                        draggedSetting = setting;
                        return;
                    }
                    settingY += 22;
                } else if (setting instanceof DoubleSliderSetting) {
                    settingY += 22;
                } else if (setting instanceof TickSetting) {
                    TickSetting tick = (TickSetting) setting;
                    int checkboxSize2 = 10;
                    // Accept BOTH left and right click for better UX (fixes invertMenuClicks issues)
                    if (mouseX >= moduleX + 4 + 2 && mouseX <= moduleX + 4 + 2 + checkboxSize2 &&
                        mouseY >= settingY && mouseY <= settingY + checkboxSize2 &&
                        (mouseButton == 0 || mouseButton == 1)) {
                        tick.toggle();
                        return;
                    }
                    settingY += 16;
                } else if (setting instanceof ModeSetting) {
                    ModeSetting mode = (ModeSetting) setting;
                    // Click anywhere on the setting row to cycle mode
                    // Accept both left and right click for better UX
                    if (mouseX >= moduleX + 4 && mouseX <= moduleX + MODULE_WIDTH - 4 &&
                        mouseY >= settingY - 2 && mouseY <= settingY + 12) {
                        // Left click = next, Right click = previous
                        if (mouseButton == 0) {
                            mode.cycle();
                        } else if (mouseButton == 1) {
                            mode.cycleBack();
                        }
                        cachedHeights.remove(module); // Invalidate cache
                        return;
                    }
                    settingY += 16;
                }
            }

            col++;
            if (col >= MODULES_PER_ROW) {
                col = 0;
                currentY += moduleHeight + MODULE_SPACING;
            }
        }

        try {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        } catch (Exception e) {}
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        draggedSetting = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // Handle keybind listening
        if (bindingModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                bindingModule.setKeyCode(0); // Clear keybind
            } else if (keyCode == Keyboard.KEY_BACK) {
                bindingModule.setKeyCode(0); // Clear keybind
            } else {
                bindingModule.setKeyCode(keyCode);
            }
            bindingModule = null;
            return;
        }

        if (keyCode == 1) { // ESC key
            onGuiClosed();
            Minecraft.getMinecraft().displayGuiScreen(null);
        } else if (keyCode == 210) { // INSERT key
            onGuiClosed();
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
        try {
            super.keyTyped(typedChar, keyCode);
        } catch (Exception e) {}
    }

    @Override
    public void handleMouseInput() {
        try {
            super.handleMouseInput();
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0) {
                // Scroll modules
                moduleScroll -= wheel > 0 ? 25 : -25;
                moduleScroll = Math.max(0, Math.min(moduleScroll, maxModuleScroll));
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        // Save current position and category when closing
        savedX = x;
        savedY = y;
        savedCategory = selectedCategory;
        bindingModule = null; // Clear keybind listening on close
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
