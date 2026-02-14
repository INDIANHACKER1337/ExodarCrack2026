/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.*;
import io.github.exodar.gui.clickgui.RenderHelper;
import io.github.exodar.ui.ArrayListConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NewClickGui extends GuiScreen {
    private ModuleManager moduleManager;
    private int selectedModuleIndex = 0;
    private Module selectedModule = null;
    private boolean settingsOpen = false;

    // Scroll para settings
    private int settingsScroll = 0;

    // Colores del tema
    private static final int BG_COLOR = new Color(20, 20, 25, 230).getRGB();
    private static final int MODULE_BG = new Color(30, 30, 35, 200).getRGB();
    private static final int MODULE_SELECTED = new Color(120, 60, 255, 100).getRGB();
    private static final int MODULE_ENABLED = new Color(80, 200, 120, 255).getRGB();
    private static final int MODULE_DISABLED = new Color(200, 80, 80, 255).getRGB();
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SLIDER_COLOR = new Color(120, 60, 255, 255).getRGB();

    // Componentes drag
    private int dragX, dragY;
    private boolean dragging = false;
    private int panelX = 100, panelY = 50;

    // Para settings interactivas
    private Setting draggedSetting = null;
    private int draggedSettingIndex = -1;

    public NewClickGui(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        System.out.println("[Exodar] NewClickGui created with " + moduleManager.getModules().size() + " modules");
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        System.out.println("[Exodar] NewClickGui initialized");
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        System.out.println("[Exodar] NewClickGui closed");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Actualizar drag
        if (dragging) {
            panelX = mouseX + dragX;
            panelY = mouseY + dragY;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = ScaledResolutionHelper.create(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        // Fondo oscuro semi-transparente
        drawRect(0, 0, screenWidth, screenHeight, new Color(0, 0, 0, 100).getRGB());

        // Panel principal
        int panelWidth = 400;
        int panelHeight = 300;

        // Fondo del panel con bordes redondeados
        RenderHelper.drawRoundedRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 12, BG_COLOR);

        // Título con barra arrastrable
        int titleHeight = 30;
        RenderHelper.drawGradient(panelX, panelY, panelX + panelWidth, panelY + titleHeight,
                new Color(120, 60, 255, 200).getRGB(),
                new Color(80, 40, 200, 200).getRGB());

        RenderHelper.drawCenteredString("§lEXODAR CLIENT", panelX + panelWidth / 2, panelY + 10, TEXT_COLOR);

        // División: Lista de módulos a la izquierda, settings a la derecha
        int moduleListWidth = 180;
        int settingsWidth = panelWidth - moduleListWidth;

        // Dibujar lista de módulos
        drawModuleList(panelX, panelY + titleHeight, moduleListWidth, panelHeight - titleHeight, mouseX, mouseY);

        // Dibujar settings si un módulo está seleccionado
        if (selectedModule != null && settingsOpen) {
            drawSettings(panelX + moduleListWidth, panelY + titleHeight, settingsWidth,
                    panelHeight - titleHeight, mouseX, mouseY);
        } else {
            // Mensaje de ayuda
            String help = "Click derecho para abrir settings";
            RenderHelper.drawCenteredString(help,
                    panelX + moduleListWidth + settingsWidth / 2,
                    panelY + titleHeight + 20,
                    new Color(150, 150, 150, 255).getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawModuleList(int x, int y, int width, int height, int mouseX, int mouseY) {
        List<Module> modules = moduleManager.getModules();

        int yOffset = 0;
        int moduleHeight = 25;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int moduleY = y + yOffset;

            // Skip si está fuera del área visible
            if (moduleY + moduleHeight < y || moduleY > y + height) {
                yOffset += moduleHeight;
                continue;
            }

            // Color de fondo
            int bgColor = MODULE_BG;
            if (i == selectedModuleIndex) {
                bgColor = MODULE_SELECTED;
            }

            // Dibuja fondo del módulo
            RenderHelper.drawRect(x + 2, moduleY, x + width - 2, moduleY + moduleHeight - 2, bgColor);

            // Indicador de estado (habilitado/deshabilitado)
            int statusColor = module.isEnabled() ? MODULE_ENABLED : MODULE_DISABLED;
            RenderHelper.drawRect(x + 4, moduleY + 2, x + 8, moduleY + moduleHeight - 4, statusColor);

            // Nombre del módulo
            String name = module.getName();
            RenderHelper.drawString(name, x + 12, moduleY + 8, TEXT_COLOR);

            // Settings count
            if (!module.getSettings().isEmpty()) {
                String settingsCount = "(" + module.getSettings().size() + ")";
                int settingsWidth = RenderHelper.getStringWidth(settingsCount);
                RenderHelper.drawString(settingsCount, x + width - settingsWidth - 8, moduleY + 8,
                        new Color(150, 150, 150, 255).getRGB());
            }

            yOffset += moduleHeight;
        }
    }

    private void drawSettings(int x, int y, int width, int height, int mouseX, int mouseY) {
        if (selectedModule == null) return;

        // Título
        RenderHelper.drawString("§l" + selectedModule.getName() + " Settings", x + 8, y + 5, TEXT_COLOR);

        // Botón de cerrar
        int closeX = x + width - 20;
        int closeY = y + 5;
        RenderHelper.drawString("§cX", closeX, closeY, TEXT_COLOR);

        // Línea divisoria
        RenderHelper.drawRect(x, y + 22, x + width, y + 24, new Color(60, 60, 65, 255).getRGB());

        // Área de settings con scroll
        int settingsY = y + 25;
        int settingsHeight = height - 30;

        List<Setting> settings = selectedModule.getSettings();
        if (settings.isEmpty()) {
            RenderHelper.drawCenteredString("No settings available",
                    x + width / 2, y + height / 2, new Color(150, 150, 150, 255).getRGB());
            return;
        }

        // Habilitar scissor test para clipping
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = ScaledResolutionHelper.create(mc);
        int scale = sr.getScaleFactor();

        // Obtener displayHeight usando reflection
        int displayHeight = 1080; // Default
        try {
            for (java.lang.reflect.Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    if (f.getName().toLowerCase().contains("height")) {
                        displayHeight = (int) f.get(mc);
                        break;
                    }
                }
            }
        } catch (Exception e) {}

        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(
            x * scale,
            displayHeight - (settingsY + settingsHeight) * scale,
            width * scale,
            settingsHeight * scale
        );

        int yOffset = settingsScroll;
        int settingHeight = 30;

        for (int i = 0; i < settings.size(); i++) {
            Setting setting = settings.get(i);
            if (!setting.isVisible()) continue;

            int settingY = settingsY + yOffset;

            // Dibujar setting según su tipo
            if (setting instanceof SliderSetting) {
                drawSliderSetting((SliderSetting) setting, x + 8, settingY, width - 16, settingHeight, mouseX, mouseY, i);
            } else if (setting instanceof TickSetting) {
                drawTickSetting((TickSetting) setting, x + 8, settingY, width - 16, settingHeight, mouseX, mouseY, i);
            } else if (setting instanceof DoubleSliderSetting) {
                drawDoubleSliderSetting((DoubleSliderSetting) setting, x + 8, settingY, width - 16, settingHeight, mouseX, mouseY, i);
            } else if (setting instanceof DescriptionSetting) {
                drawDescriptionSetting((DescriptionSetting) setting, x + 8, settingY, width - 16, settingHeight);
            }

            yOffset += settingHeight;
        }

        // Deshabilitar scissor test
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
    }

    private void drawSliderSetting(SliderSetting setting, int x, int y, int width, int height, int mouseX, int mouseY, int index) {
        // Actualizar valor si se está arrastrando
        if (draggedSetting == setting && draggedSettingIndex == index) {
            float percent = (float) (mouseX - x) / (float)width;
            percent = Math.max(0, Math.min(1, percent));
            double value = setting.getMin() + (setting.getMax() - setting.getMin()) * percent;
            setting.setValue(value);
        }

        // Nombre y valor
        String text = setting.getName() + ": " + setting.getValue();
        RenderHelper.drawString(text, x, y + 2, TEXT_COLOR);

        // Barra de slider
        int barY = y + RenderHelper.getFontHeight() + 4;
        int barHeight = 4;

        // Fondo de la barra
        RenderHelper.drawRect(x, barY, x + width, barY + barHeight,
                new Color(50, 50, 55, 255).getRGB());

        // Barra de progreso
        double percent = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        int progressWidth = (int) (width * percent);

        if (progressWidth > 0) {
            RenderHelper.drawRect(x, barY, x + progressWidth, barY + barHeight, SLIDER_COLOR);
        }
    }

    private void drawTickSetting(TickSetting setting, int x, int y, int width, int height, int mouseX, int mouseY, int index) {
        // Checkbox
        int checkboxSize = 10;
        int checkboxX = x;
        int checkboxY = y + 4;

        int checkboxColor = setting.isEnabled() ? MODULE_ENABLED : new Color(50, 50, 55, 255).getRGB();
        RenderHelper.drawRect(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, checkboxColor);

        // Checkmark si está habilitado
        if (setting.isEnabled()) {
            RenderHelper.drawString("x", checkboxX + 2, checkboxY, new Color(255, 255, 255, 255).getRGB());
        }

        // Nombre
        RenderHelper.drawString(setting.getName(), x + checkboxSize + 4, y + 4, TEXT_COLOR);
    }

    private void drawDoubleSliderSetting(DoubleSliderSetting setting, int x, int y, int width, int height, int mouseX, int mouseY, int index) {
        // Nombre y valores
        String text = setting.getName() + ": " + setting.getValueMin() + " - " + setting.getValueMax();
        RenderHelper.drawString(text, x, y + 2, TEXT_COLOR);

        // Barra de slider
        int barY = y + RenderHelper.getFontHeight() + 4;
        int barHeight = 4;

        // Fondo de la barra
        RenderHelper.drawRect(x, barY, x + width, barY + barHeight,
                new Color(50, 50, 55, 255).getRGB());

        // Barra de rango
        double percentMin = (setting.getValueMin() - setting.getMin()) / (setting.getMax() - setting.getMin());
        double percentMax = (setting.getValueMax() - setting.getMin()) / (setting.getMax() - setting.getMin());
        int minX = (int) (width * percentMin);
        int maxX = (int) (width * percentMax);

        if (maxX > minX) {
            RenderHelper.drawRect(x + minX, barY, x + maxX, barY + barHeight, SLIDER_COLOR);
        }
    }

    private void drawDescriptionSetting(DescriptionSetting setting, int x, int y, int width, int height) {
        RenderHelper.drawString("§7" + setting.getDescription(), x, y + 4,
                new Color(150, 150, 150, 255).getRGB());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int panelWidth = 400;
        int panelHeight = 300;
        int titleHeight = 30;
        int moduleListWidth = 180;

        // Check si se hace click en la barra de título para arrastrar
        if (mouseX >= panelX && mouseX <= panelX + panelWidth &&
            mouseY >= panelY && mouseY <= panelY + titleHeight) {
            dragging = true;
            dragX = panelX - mouseX;
            dragY = panelY - mouseY;
            return;
        }

        // Check click en lista de módulos
        if (mouseX >= panelX && mouseX <= panelX + moduleListWidth &&
            mouseY >= panelY + titleHeight && mouseY <= panelY + panelHeight) {

            int relativeY = mouseY - (panelY + titleHeight);
            int moduleHeight = 25;
            int clickedIndex = relativeY / moduleHeight;

            if (clickedIndex >= 0 && clickedIndex < moduleManager.getModules().size()) {
                // Check if clicks are inverted
                int toggleButton = ArrayListConfig.invertMenuClicks ? 1 : 0;
                int settingsButton = ArrayListConfig.invertMenuClicks ? 0 : 1;

                if (mouseButton == toggleButton) {
                    // Toggle módulo
                    moduleManager.getModules().get(clickedIndex).toggle();
                } else if (mouseButton == settingsButton) {
                    // Abrir settings
                    selectedModuleIndex = clickedIndex;
                    selectedModule = moduleManager.getModules().get(clickedIndex);
                    settingsOpen = true;
                    settingsScroll = 0;
                }
            }
            return;
        }

        // Check click en settings
        if (settingsOpen && selectedModule != null) {
            int settingsX = panelX + moduleListWidth;
            int settingsY = panelY + titleHeight;
            int settingsWidth = panelWidth - moduleListWidth;
            int settingsHeight = panelHeight - titleHeight;

            // Check botón de cerrar
            int closeX = settingsX + settingsWidth - 20;
            int closeY = settingsY + 5;
            if (mouseX >= closeX && mouseX <= closeX + 15 &&
                mouseY >= closeY && mouseY <= closeY + 15) {
                settingsOpen = false;
                return;
            }

            // Check click en settings
            if (mouseX >= settingsX && mouseX <= settingsX + settingsWidth &&
                mouseY >= settingsY + 25 && mouseY <= settingsY + settingsHeight) {

                int relativeY = mouseY - (settingsY + 25) - settingsScroll;
                int settingHeight = 30;
                int clickedIndex = relativeY / settingHeight;

                List<Setting> settings = selectedModule.getSettings();
                int visibleIndex = 0;
                for (int i = 0; i < settings.size(); i++) {
                    if (!settings.get(i).isVisible()) continue;

                    if (visibleIndex == clickedIndex) {
                        Setting setting = settings.get(i);

                        if (setting instanceof TickSetting) {
                            ((TickSetting) setting).toggle();
                        } else if (setting instanceof SliderSetting || setting instanceof DoubleSliderSetting) {
                            draggedSetting = setting;
                            draggedSettingIndex = i;
                        }
                        break;
                    }
                    visibleIndex++;
                }
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
        draggedSettingIndex = -1;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() {
        try {
            super.handleMouseInput();
        } catch (Exception e) {}

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && settingsOpen && selectedModule != null) {
            // Calcular altura total de settings
            int totalHeight = 0;
            for (Setting setting : selectedModule.getSettings()) {
                if (setting.isVisible()) {
                    totalHeight += 30;
                }
            }

            // Scroll en settings
            if (dWheel > 0) {
                settingsScroll += 15;
            } else {
                settingsScroll -= 15;
            }

            // Limitar scroll entre 0 y -maxScroll
            int maxScroll = Math.max(0, totalHeight - 240); // 240 es aprox la altura del área de settings
            settingsScroll = Math.max(-maxScroll, Math.min(0, settingsScroll));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        Minecraft mc = Minecraft.getMinecraft();
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_INSERT || keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
            return;
        }

        // Navegación con flechas
        if (keyCode == Keyboard.KEY_UP) {
            selectedModuleIndex--;
            if (selectedModuleIndex < 0) {
                selectedModuleIndex = moduleManager.getModules().size() - 1;
            }
        } else if (keyCode == Keyboard.KEY_DOWN) {
            selectedModuleIndex++;
            if (selectedModuleIndex >= moduleManager.getModules().size()) {
                selectedModuleIndex = 0;
            }
        } else if (keyCode == Keyboard.KEY_RETURN) {
            // Toggle módulo seleccionado
            if (selectedModuleIndex >= 0 && selectedModuleIndex < moduleManager.getModules().size()) {
                moduleManager.getModules().get(selectedModuleIndex).toggle();
            }
        }

        try {
            super.keyTyped(typedChar, keyCode);
        } catch (Exception e) {}
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
