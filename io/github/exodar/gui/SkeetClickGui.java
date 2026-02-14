/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui;

import io.github.exodar.config.UserConfig;
import io.github.exodar.config.CloudConfigManager;
import io.github.exodar.module.Module;
import io.github.exodar.ui.ArrayListConfig;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.*;
import io.github.exodar.gui.clickgui.RenderHelper;
import io.github.exodar.font.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * ClickGUI estilo Skeet - Simple y funcional
 */
public class SkeetClickGui extends GuiScreen {
    private ModuleManager moduleManager;

    // Posición (static para persistir entre sesiones)
    private static int x = -1, y = -1;
    private int width = 520;
    private int height = 400;

    // Estado (static para persistir entre sesiones)
    private static ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private static Module selectedModule = null;

    // Cooldown para INSERT key
    private static long lastCloseTime = 0;
    private static final long CLOSE_COOLDOWN = 200; // 200ms cooldown
    private Setting draggedSetting = null;
    private SliderSetting hoveredSlider = null;  // For mouse wheel adjustment
    private int colorPickerDragMode = 0;  // 0=none, 1=SV square, 2=Hue bar, 3=Alpha bar

    // Drag
    private boolean dragging = false;
    private int dragX, dragY;

    // Scroll
    private int moduleScroll = 0;
    private int settingScroll = 0;

    // Keybind state (waiting for module + mode)
    private KeybindSetting waitingKeybindSetting = null;
    private Module waitingForBind = null;
    private Module.BindMode waitingForBindMode = null;

    // Config state
    private String configNameInput = "";
    private String selectedConfig = null;
    private int configScroll = 0;
    private boolean configInputFocused = false;
    private long lastCursorBlink = System.currentTimeMillis();

    // Cloud config state
    private boolean cloudConfigMode = false; // false = local, true = cloud
    private int cloudConfigTab = 0; // 0=Official, 1=Public, 2=My Configs
    private int cloudConfigScroll = 0;
    private CloudConfigManager.CloudConfig selectedCloudConfig = null;
    private boolean cloudConfigsFetching = false;
    private String cloudConfigStatus = "";
    private String cloudImportCode = "";
    private boolean cloudImportFocused = false;

    // TextSetting editing state
    private TextSetting focusedTextSetting = null;
    private int textCursorPos = 0;  // Cursor position for text editing
    private long lastBackspaceTime = 0;  // For continuous backspace
    private boolean backspaceHeld = false;

    // Settings grid state (2x2 grid: Config, HUD, Friends, Empty)
    private int selectedSettingsTab = 0; // 0=Config, 1=HUD, 2=Friends, 3=Empty

    // Friends input field
    private String friendNameInput = "";
    private boolean friendInputFocused = false;

    // Colores (usando los que el usuario ya tiene)
    private static final Color BG = new Color(15, 15, 20, 245);
    private static final Color PANEL = new Color(25, 25, 35, 255);
    private static final Color LIGHT = new Color(35, 35, 50, 255);
    private static final Color ACCENT = new Color(120, 80, 255);
    private static final Color TEXT = new Color(220, 220, 220);
    private static final Color DIM = new Color(150, 150, 160);

    public SkeetClickGui(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    /**
     * Check if GUI can be opened (cooldown check)
     */
    public static boolean canOpen() {
        return System.currentTimeMillis() - lastCloseTime >= CLOSE_COOLDOWN;
    }

    /**
     * Called when GUI is closed - auto-saves UserConfig only
     */
    public static void onClose() {
        lastCloseTime = System.currentTimeMillis();

        // Auto-save UserConfig (hidden modules, passwords, preferences)
        try {
            io.github.exodar.config.UserConfig.getInstance().save();
        } catch (Exception e) {
            System.out.println("[SkeetClickGui] UserConfig auto-save failed: " + e.getMessage());
        }
    }

    /**
     * Debug helper - disabled in production
     */
    private static void debugMsg(String msg) {
        // Debug disabled - uncomment to enable:
        // System.out.println(msg);
    }

    @Override
    public void initGui() {
        Minecraft mc = Minecraft.getMinecraft();

        // Ensure mouse cursor is visible - use Minecraft's mouseHelper
        if (mc.mouseHelper != null) {
            mc.mouseHelper.ungrabMouseCursor();
        }
        mc.inGameHasFocus = false;
        Mouse.setGrabbed(false);

        ScaledResolution sr = null;

        try {
            sr = new ScaledResolution(mc);
        } catch (Exception e1) {
            x = 100;
            y = 100;
            return;
        }

        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();

        // Check if position not set yet OR if menu is outside screen bounds
        boolean needsReset = (x == -1 || y == -1);

        // If menu would be mostly off-screen, reset to center
        if (!needsReset) {
            // Menu is off-screen if less than 50px visible on any edge
            boolean offLeft = (x + width) < 50;
            boolean offRight = x > (screenW - 50);
            boolean offTop = (y + height) < 50;
            boolean offBottom = y > (screenH - 50);

            if (offLeft || offRight || offTop || offBottom) {
                needsReset = true;
            }
        }

        if (needsReset) {
            x = (screenW - width) / 2;
            y = (screenH - height) / 2;
        }

        // Also clamp position to ensure it's fully within bounds
        x = Math.max(0, Math.min(screenW - width, x));
        y = Math.max(0, Math.min(screenH - height, y));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Reset hovered slider each frame
        hoveredSlider = null;

        // Manejar dragging (FIX LAG: mover de updateScreen a drawScreen)
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = ScaledResolutionHelper.create(mc);
            x = Math.max(0, Math.min(sr.getScaledWidth() - width, x));
            y = Math.max(0, Math.min(sr.getScaledHeight() - height, y));
        }

        // Fondo oscuro
        drawRect(0, 0, width * 3, height * 3, new Color(0, 0, 0, 100).getRGB());

        // Panel principal
        drawRect(x, y, x + width, y + height, BG.getRGB());

        // Clean subtle border (dark outline with thin accent)
        int darkBorder = new Color(20, 20, 25).getRGB();
        int accentBorder = ACCENT.getRGB();
        // Dark outer border
        drawRect(x - 1, y - 1, x + width + 1, y, darkBorder); // Top
        drawRect(x - 1, y + height, x + width + 1, y + height + 1, darkBorder); // Bottom
        drawRect(x - 1, y, x, y + height, darkBorder); // Left
        drawRect(x + width, y, x + width + 1, y + height, darkBorder); // Right
        // Thin accent line at top only (elegant touch)
        drawRect(x, y - 2, x + width, y - 1, accentBorder);

        // Título with purple accent line
        drawRect(x, y, x + width, y + 25, PANEL.getRGB());
        drawRect(x, y + 25, x + width, y + 27, ACCENT.getRGB()); // Purple line under title
        RenderHelper.drawString("EXODAR", x + 8, y + 8, ACCENT.getRGB());

        // Username de Xenforo (derecha)
        String xfUsername = io.github.exodar.api.BuildInfo.getInstance().getUsername();
        if (xfUsername != null && !xfUsername.isEmpty()) {
            int usernameWidth = RenderHelper.getStringWidth(xfUsername);
            RenderHelper.drawString(xfUsername, x + width - usernameWidth - 8, y + 8, DIM.getRGB());
        }

        // Tabs horizontales (estilo Skeet)
        drawCategoryTabs(mouseX, mouseY);

        // Si es SETTINGS category, mostrar Settings Grid UI (2x2)
        if (selectedCategory == ModuleCategory.SETTINGS) {
            drawSettingsGrid(mouseX, mouseY);
        } else {
            // Lista de módulos (izquierda, más pequeña)
            drawModuleList(mouseX, mouseY);

            // Settings (derecha)
            if (selectedModule != null) {
                drawSettings(mouseX, mouseY);
            }
        }

        // Render account island on top of everything
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = ScaledResolutionHelper.create(mc);
        io.github.exodar.ui.AccountIsland.render(mc, sr.getScaledWidth(), sr.getScaledHeight());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCategoryTabs(int mouseX, int mouseY) {
        int tabY = y + 30;
        int tabW = width / 6;
        int tabH = 28;

        // Only show these categories (excluding RENDER)
        ModuleCategory[] cats = {ModuleCategory.COMBAT, ModuleCategory.PLAYER, ModuleCategory.MOVEMENT,
                                 ModuleCategory.VISUALS, ModuleCategory.MISC, ModuleCategory.SETTINGS};
        String[] icons = {"A", "B", "C", "D", "E", "F"}; // Combat, Player, Movement, Visuals, Misc, Settings

        for (int i = 0; i < cats.length; i++) {
            int tabX = x + (i * tabW);
            boolean selected = cats[i] == selectedCategory;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabY && mouseY <= tabY + tabH;

            // Background
            int bg = selected ? LIGHT.getRGB() : PANEL.getRGB();
            if (!selected && hovered) bg = new Color(30, 30, 40).getRGB();
            drawRect(tabX, tabY, tabX + tabW, tabY + tabH, bg);

            // Borde inferior si está seleccionado
            if (selected) {
                drawRect(tabX, tabY + tabH - 2, tabX + tabW, tabY + tabH, ACCENT.getRGB());
            }

            // Icono Exodar HD (renderizado a alta resolución, escalado a 0.5x para nitidez)
            FontManager fm = FontManager.getInstance();
            if (fm.exodarHD != null) {
                GL11.glPushMatrix();
                float scale = 0.5f;
                GL11.glScalef(scale, scale, 1.0f);
                int iconW = (int)(fm.exodarHD.getStringWidth(icons[i]) * scale);
                int drawX = (int)((tabX + (tabW - iconW) / 2) / scale);
                int drawY = (int)((tabY + 1) / scale);
                fm.exodarHD.drawString(icons[i], drawX, drawY, ACCENT.getRGB() | 0xFF000000);
                GL11.glPopMatrix();
            }

            // Texto invisible para resetear estado GL
            RenderHelper.drawString(" ", tabX, tabY + 20, 0x00000000);
        }
    }

    private void drawModuleList(int mouseX, int mouseY) {
        int listX = x + 4;
        int listY = y + 62;
        int listW = 140;  // MÁS PEQUEÑO
        int listH = height - 67;

        // Background
        drawRect(listX, listY, listX + listW, listY + listH, PANEL.getRGB());

        List<Module> modules = getModulesForCategory(selectedCategory);
        int modY = listY + 3 - moduleScroll;
        int modH = 18;  // MÁS PEQUEÑO (era 25)

        for (Module mod : modules) {
            if (modY > listY + listH) break;
            if (modY + modH < listY) {
                modY += modH;
                continue;
            }

            boolean selected = mod == selectedModule;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW &&
                            mouseY >= modY && mouseY <= modY + modH;

            // Background
            if (selected) {
                drawRect(listX + 2, modY, listX + listW - 2, modY + modH - 1, LIGHT.getRGB());
            } else if (hovered) {
                drawRect(listX + 2, modY, listX + listW - 2, modY + modH - 1, new Color(30, 30, 40).getRGB());
            }

            // Indicador de estado (círculo pequeño)
            int circleX = listX + 6;
            int circleY = modY + 6;
            drawRect(circleX, circleY, circleX + 6, circleY + 6,
                    mod.isEnabled() ? ACCENT.getRGB() : new Color(50, 50, 60).getRGB());

            // Nombre (texto más pequeño)
            RenderHelper.drawString(mod.getName(), listX + 16, modY + 5, TEXT.getRGB());

            // Keybind display (to the right) - only show if bound or waiting
            int toggleBind = mod.getToggleBind();
            int holdBind = mod.getHoldBind();

            // Only show keybind if waiting for bind or has a bind set
            if (waitingForBind == mod || toggleBind != 0 || holdBind != 0) {
                String keybindText;
                int keybindColor;

                if (waitingForBind == mod) {
                    // Waiting for key press - yellow
                    keybindText = waitingForBindMode == Module.BindMode.TOGGLE ? "[T...]" : "[H...]";
                    keybindColor = 0xFFFF55; // Yellow
                } else if (toggleBind != 0) {
                    // Toggle bind - purple
                    keybindText = "[" + getKeyName(toggleBind) + "]";
                    keybindColor = ACCENT.getRGB(); // Purple
                } else {
                    // Hold bind - light purple with H: prefix
                    keybindText = "[H:" + getKeyName(holdBind) + "]";
                    keybindColor = 0xDD88FF; // Light purple
                }

                // Draw keybind
                int keybindW = RenderHelper.getStringWidth(keybindText);
                int keybindX = listX + listW - keybindW - 6;
                RenderHelper.drawString(keybindText, keybindX, modY + 5, keybindColor);
            }

            modY += modH;
        }
    }

    private void drawSettings(int mouseX, int mouseY) {
        int setX = x + 150;
        int setY = y + 62;
        int setW = width - 156;
        int setH = height - 67;

        // Background
        drawRect(setX, setY, setX + setW, setY + setH, PANEL.getRGB());

        // Título con keybind buttons
        drawRect(setX, setY, setX + setW, setY + 20, LIGHT.getRGB());
        RenderHelper.drawString(selectedModule.getName(), setX + 6, setY + 6, ACCENT.getRGB());

        // Keybind buttons in header (three boxes on the right: Hide, Toggle, Hold)
        int btnW = 60;
        int btnH = 14;
        int btnY = setY + 3;
        int btnSpacing = 3;

        // Hold button (rightmost)
        int holdBtnX = setX + setW - btnW - 6;
        int holdBind = selectedModule.getHoldBind();
        boolean holdHovered = mouseX >= holdBtnX && mouseX <= holdBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        // Hold button background (no top border line)
        int holdBgColor = holdHovered ? new Color(50, 50, 65).getRGB() : new Color(35, 35, 50).getRGB();
        if (waitingForBind == selectedModule && waitingForBindMode == Module.BindMode.HOLD) {
            holdBgColor = new Color(80, 60, 120).getRGB(); // Purple when waiting
        }
        drawRect(holdBtnX, btnY, holdBtnX + btnW, btnY + btnH, holdBgColor);

        // Hold button text - format: "Hold [KEY]" or "Hold [-]"
        String holdText;
        int holdTextColor;
        if (waitingForBind == selectedModule && waitingForBindMode == Module.BindMode.HOLD) {
            holdText = "Hold [...]";
            holdTextColor = 0xFFFF55; // Yellow
        } else if (holdBind != 0) {
            holdText = "Hold [" + getKeyName(holdBind) + "]";
            holdTextColor = 0xDD88FF; // Light purple
        } else {
            holdText = "Hold [-]";
            holdTextColor = DIM.getRGB();
        }
        int holdTextW = RenderHelper.getStringWidth(holdText);
        RenderHelper.drawString(holdText, holdBtnX + (btnW - holdTextW) / 2, btnY + 3, holdTextColor);

        // Toggle button (left of hold button)
        int toggleBtnX = holdBtnX - btnW - btnSpacing;
        int toggleBind = selectedModule.getToggleBind();
        boolean toggleHovered = mouseX >= toggleBtnX && mouseX <= toggleBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        // Toggle button background (no top border line)
        int toggleBgColor = toggleHovered ? new Color(50, 50, 65).getRGB() : new Color(35, 35, 50).getRGB();
        if (waitingForBind == selectedModule && waitingForBindMode == Module.BindMode.TOGGLE) {
            toggleBgColor = new Color(80, 60, 120).getRGB(); // Purple when waiting
        }
        drawRect(toggleBtnX, btnY, toggleBtnX + btnW, btnY + btnH, toggleBgColor);

        // Toggle button text - format: "Toggle [KEY]" or "Toggle [-]"
        String toggleText;
        int toggleTextColor;
        if (waitingForBind == selectedModule && waitingForBindMode == Module.BindMode.TOGGLE) {
            toggleText = "Toggle [...]";
            toggleTextColor = 0xFFFF55; // Yellow
        } else if (toggleBind != 0) {
            toggleText = "Toggle [" + getKeyName(toggleBind) + "]";
            toggleTextColor = ACCENT.getRGB(); // Purple
        } else {
            toggleText = "Toggle [-]";
            toggleTextColor = DIM.getRGB();
        }
        int toggleTextW = RenderHelper.getStringWidth(toggleText);
        RenderHelper.drawString(toggleText, toggleBtnX + (btnW - toggleTextW) / 2, btnY + 3, toggleTextColor);

        // Hide button (left of toggle button)
        int hideBtnX = toggleBtnX - btnW - btnSpacing;
        boolean isHidden = UserConfig.getInstance().isModuleHidden(selectedModule);
        boolean hideHovered = mouseX >= hideBtnX && mouseX <= hideBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        // Hide button background
        int hideBgColor;
        if (isHidden) {
            hideBgColor = hideHovered ? new Color(180, 60, 60).getRGB() : new Color(120, 40, 40).getRGB(); // Red when hidden
        } else {
            hideBgColor = hideHovered ? new Color(50, 50, 65).getRGB() : new Color(35, 35, 50).getRGB();
        }
        drawRect(hideBtnX, btnY, hideBtnX + btnW, btnY + btnH, hideBgColor);

        // Hide button text
        String hideText = isHidden ? "Hidden" : "Visible";
        int hideTextColor = isHidden ? new Color(255, 100, 100).getRGB() : DIM.getRGB();
        int hideTextW = RenderHelper.getStringWidth(hideText);
        RenderHelper.drawString(hideText, hideBtnX + (btnW - hideTextW) / 2, btnY + 3, hideTextColor);

        // Settings (start directly after title)
        List<Setting> settings = selectedModule.getSettings();
        int sY = setY + 24 - settingScroll;

        // Enable scissor clipping to prevent settings from going off panel
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = ScaledResolutionHelper.create(mc);
        int scale = sr.getScaleFactor();
        int settingsTop = setY + 20;  // Below header
        int settingsBottom = setY + setH;
        // Convert to OpenGL coordinates (bottom-left origin, window pixels)
        int scissorX = setX * scale;
        int scissorY = (sr.getScaledHeight() - settingsBottom) * scale;
        int scissorW = setW * scale;
        int scissorH = (settingsBottom - settingsTop) * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);

        for (Setting set : settings) {
            if (set instanceof DescriptionSetting || !set.isVisible()) continue;

            if (sY > setY + setH) break;
            if (sY < setY + 20) {
                sY += getSettingHeight(set);
                continue;
            }

            sY += drawSetting(set, setX + 6, sY, setW - 12, mouseX, mouseY);
        }

        // Disable scissor clipping
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int drawSetting(Setting set, int x, int y, int w, int mX, int mY) {
        if (set instanceof ColorSetting) {
            ColorSetting color = (ColorSetting) set;

            // Nombre + Rainbow button + Preview en la misma linea
            RenderHelper.drawString(color.getName(), x, y, TEXT.getRGB());

            // Color preview box (right side)
            int previewSize = 10;
            int previewX = x + w - previewSize;
            int currentColor = color.getColor();
            drawRect(previewX - 1, y - 1, previewX + previewSize + 1, y + previewSize + 1, 0xFF3C3C46);
            drawRect(previewX, y, previewX + previewSize, y + previewSize, currentColor);

            // Rainbow toggle (R button)
            int rbtnX = previewX - 16;
            boolean rbtnHover = mX >= rbtnX && mX <= rbtnX + 12 && mY >= y && mY <= y + 10;
            int rbtnColor = color.isRainbow() ? 0xFFFF55FF : (rbtnHover ? 0xFF555555 : 0xFF333333);
            drawRect(rbtnX, y, rbtnX + 12, y + 10, rbtnColor);
            RenderHelper.drawString("R", rbtnX + 3, y + 1, color.isRainbow() ? 0xFF000000 : TEXT.getRGB());

            // === 2D Color Square (Saturation x Brightness) ===
            int squareY = y + 14;
            int squareSize = 50;  // 50x50 square
            int hueBarW = 10;
            int hueBarX = x + squareSize + 4;
            float currentHue = color.getHue() / 360f;

            // SV square border (draw BEFORE content)
            drawRect(x - 1, squareY - 1, x + squareSize + 1, squareY + squareSize + 1, 0xFF3C3C46);

            // Draw the SV square
            for (int sy = 0; sy < squareSize; sy++) {
                for (int sx = 0; sx < squareSize; sx++) {
                    float sat = (float) sx / squareSize;
                    float bri = 1.0f - (float) sy / squareSize;  // Top = bright, bottom = dark
                    int pixelColor = java.awt.Color.HSBtoRGB(currentHue, sat, bri);
                    drawRect(x + sx, squareY + sy, x + sx + 1, squareY + sy + 1, pixelColor | 0xFF000000);
                }
            }

            // Current position indicator on SV square
            int indicatorSX = x + (int)(color.getSaturation() * squareSize);
            int indicatorSY = squareY + (int)((1.0f - color.getBrightness()) * squareSize);
            // Draw crosshair
            drawRect(indicatorSX - 3, indicatorSY, indicatorSX + 4, indicatorSY + 1, 0xFFFFFFFF);
            drawRect(indicatorSX, indicatorSY - 3, indicatorSX + 1, indicatorSY + 4, 0xFFFFFFFF);
            // Black outline
            drawRect(indicatorSX - 4, indicatorSY - 1, indicatorSX + 5, indicatorSY, 0xFF000000);
            drawRect(indicatorSX - 4, indicatorSY + 1, indicatorSX + 5, indicatorSY + 2, 0xFF000000);
            drawRect(indicatorSX - 1, indicatorSY - 4, indicatorSX, indicatorSY + 5, 0xFF000000);
            drawRect(indicatorSX + 1, indicatorSY - 4, indicatorSX + 2, indicatorSY + 5, 0xFF000000);

            // === Vertical Hue Bar ===
            // Hue bar border (draw BEFORE content)
            drawRect(hueBarX - 1, squareY - 1, hueBarX + hueBarW + 1, squareY + squareSize + 1, 0xFF3C3C46);
            // Hue bar content
            for (int hy = 0; hy < squareSize; hy++) {
                float hue = (float) hy / squareSize;
                int hueColor = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
                drawRect(hueBarX, squareY + hy, hueBarX + hueBarW, squareY + hy + 1, hueColor | 0xFF000000);
            }

            // Hue indicator
            int hueIndicatorY = squareY + (int)(currentHue * squareSize);
            drawRect(hueBarX - 2, hueIndicatorY - 1, hueBarX + hueBarW + 2, hueIndicatorY + 2, 0xFFFFFFFF);

            int totalHeight = 14 + squareSize + 4;

            // === Alpha Bar (horizontal, below square) ===
            if (color.hasAlpha()) {
                int alphaBarY = squareY + squareSize + 4;
                int alphaBarH = 8;
                int alphaBarW = squareSize + 4 + hueBarW;

                // Alpha bar border (draw BEFORE content)
                drawRect(x - 1, alphaBarY - 1, x + alphaBarW + 1, alphaBarY + alphaBarH + 1, 0xFF3C3C46);

                // Checkerboard background for alpha
                for (int ax = 0; ax < alphaBarW; ax++) {
                    int checkColor = ((ax / 4) % 2 == 0) ? 0xFF666666 : 0xFF999999;
                    drawRect(x + ax, alphaBarY, x + ax + 1, alphaBarY + alphaBarH, checkColor);
                }
                // Alpha gradient overlay
                for (int ax = 0; ax < alphaBarW; ax++) {
                    int alphaVal = (int)((float)ax / alphaBarW * 255);
                    int alphaColor = (alphaVal << 24) | (color.getRGB());
                    drawRect(x + ax, alphaBarY, x + ax + 1, alphaBarY + alphaBarH, alphaColor);
                }

                // Alpha indicator
                int alphaIndicatorX = x + (int)((float)color.getAlpha() / 255 * alphaBarW);
                drawRect(alphaIndicatorX - 1, alphaBarY - 2, alphaIndicatorX + 2, alphaBarY + alphaBarH + 2, 0xFFFFFFFF);

                totalHeight += alphaBarH + 4;

                // Handle alpha dragging (mode 3) - drag anywhere once locked
                if (draggedSetting == color && Mouse.isButtonDown(0) && colorPickerDragMode == 3) {
                    float newAlpha = Math.max(0, Math.min(1, (float)(mX - x) / alphaBarW));
                    color.setAlpha((int)(newAlpha * 255));
                }
            }

            // Handle dragging - lock to the element that was initially clicked
            if (draggedSetting == color && Mouse.isButtonDown(0)) {
                // Determine drag mode on first click (mode 0 = not set yet)
                if (colorPickerDragMode == 0) {
                    if (mX >= x && mX <= x + squareSize && mY >= squareY && mY <= squareY + squareSize) {
                        colorPickerDragMode = 1;  // SV Square
                    } else if (mX >= hueBarX - 2 && mX <= hueBarX + hueBarW + 2 && mY >= squareY && mY <= squareY + squareSize) {
                        colorPickerDragMode = 2;  // Hue bar
                    } else if (color.hasAlpha()) {
                        int alphaBarY = squareY + squareSize + 4;
                        int alphaBarH = 8;
                        if (mY >= alphaBarY - 2 && mY <= alphaBarY + alphaBarH + 2) {
                            colorPickerDragMode = 3;  // Alpha bar
                        }
                    }
                }

                // Apply changes based on locked mode (can drag anywhere)
                if (colorPickerDragMode == 1) {
                    // SV Square - clamp to bounds
                    float newSat = Math.max(0, Math.min(1, (float)(mX - x) / squareSize));
                    float newBri = Math.max(0, Math.min(1, 1.0f - (float)(mY - squareY) / squareSize));
                    color.setSaturation(newSat);
                    color.setBrightness(newBri);
                } else if (colorPickerDragMode == 2) {
                    // Hue bar - clamp to bounds (no wrap-around)
                    float newHue = Math.max(0, Math.min(1, (float)(mY - squareY) / squareSize)) * 360;
                    color.setHue(newHue);
                }
                // Alpha (mode 3) is handled above
            }

            return totalHeight;

        } else if (set instanceof TickSetting) {
            TickSetting tick = (TickSetting) set;

            // Checkbox pequeño
            int size = 10;
            drawRect(x, y, x + size, y + size,
                    tick.isEnabled() ? ACCENT.getRGB() : new Color(40, 40, 50).getRGB());

            // Nombre
            RenderHelper.drawString(tick.getName(), x + size + 4, y + 1, TEXT.getRGB());

            return 16;  // Altura pequeña

        } else if (set instanceof SliderSetting) {
            SliderSetting slider = (SliderSetting) set;

            // Nombre y valor (texto más pequeño y compacto)
            String name = slider.getName();
            String val = String.format("%.2f", slider.getValue());
            RenderHelper.drawString(name, x, y, TEXT.getRGB());
            int valW = RenderHelper.getStringWidth(val);
            RenderHelper.drawString(val, x + w - valW, y, ACCENT.getRGB());

            // Slider bar (reducido a la mitad)
            int barY = y + 9;  // Reducido de 12 a 9
            int barH = 2;      // Reducido de 3 a 2

            // Background
            drawRect(x, barY, x + w, barY + barH, new Color(40, 40, 50).getRGB());

            // Fill
            double pct = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin());
            int fillW = (int) (w * pct);
            drawRect(x, barY, x + fillW, barY + barH, ACCENT.getRGB());

            // Detect hover for mouse wheel
            if (mX >= x && mX <= x + w && mY >= y && mY <= y + 14) {
                hoveredSlider = slider;
            }

            // Dragging
            if (draggedSetting == slider && Mouse.isButtonDown(0)) {
                double newPct = Math.max(0, Math.min(1, (double) (mX - x) / w));
                double newVal = slider.getMin() + newPct * (slider.getMax() - slider.getMin());
                double interval = slider.getInterval();
                newVal = Math.round(newVal / interval) * interval;
                slider.setValue(newVal);
            }

            return 14;  // Altura reducida a la mitad (de 20 a 14)

        } else if (set instanceof DoubleSliderSetting) {
            DoubleSliderSetting slider = (DoubleSliderSetting) set;

            // Nombre y valores (más compacto)
            String name = slider.getName();
            String val = String.format("%.2f-%.2f", slider.getValueMin(), slider.getValueMax());
            RenderHelper.drawString(name, x, y, TEXT.getRGB());
            int valW = RenderHelper.getStringWidth(val);
            RenderHelper.drawString(val, x + w - valW, y, ACCENT.getRGB());

            // Slider bar (reducido a la mitad)
            int barY = y + 9;  // Reducido de 12 a 9
            int barH = 2;      // Reducido de 3 a 2

            // Background
            drawRect(x, barY, x + w, barY + barH, new Color(40, 40, 50).getRGB());

            // Fill
            double minPct = (slider.getValueMin() - slider.getMin()) / (slider.getMax() - slider.getMin());
            double maxPct = (slider.getValueMax() - slider.getMin()) / (slider.getMax() - slider.getMin());
            int minX = (int) (x + w * minPct);
            int maxX = (int) (x + w * maxPct);
            drawRect(minX, barY, maxX, barY + barH, ACCENT.getRGB());

            return 14;  // Altura reducida a la mitad (de 20 a 14)

        } else if (set instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) set;

            // Nombre
            RenderHelper.drawString(mode.getName(), x, y, TEXT.getRGB());

            // Valor actual (a la derecha con flecha)
            String val = mode.getSelected() + " >";
            int valW = RenderHelper.getStringWidth(val);
            RenderHelper.drawString(val, x + w - valW, y, ACCENT.getRGB());

            return 16;  // Altura para ModeSetting

        } else if (set instanceof TextSetting) {
            TextSetting text = (TextSetting) set;
            boolean isFocused = (focusedTextSetting == text);

            // Nombre
            int nameWidth = RenderHelper.getStringWidth(text.getName() + ": ");
            RenderHelper.drawString(text.getName() + ":", x, y, TEXT.getRGB());

            // Campo de texto (ajustado para mejor visibilidad)
            int boxX = x + nameWidth + 2;
            int boxW = w - nameWidth - 4;
            if (boxW < 50) boxW = 50; // Mínimo 50px de ancho
            int boxH = 12;
            int bgColor = isFocused ? new Color(60, 60, 80).getRGB() : new Color(40, 40, 50).getRGB();
            int borderColor = isFocused ? ACCENT.getRGB() : new Color(80, 80, 100).getRGB();
            drawRect(boxX, y, boxX + boxW, y + boxH, bgColor);
            drawRect(boxX, y, boxX + boxW, y + 1, borderColor); // Borde superior
            drawRect(boxX, y + boxH - 1, boxX + boxW, y + boxH, borderColor); // Borde inferior

            // Valor actual o placeholder
            String val = text.getValue();
            int textColor = TEXT.getRGB();
            if (val.isEmpty() && !isFocused) {
                val = "Click to type...";
                textColor = DIM.getRGB();
            } else if (val.length() > 14 && !isFocused) {
                val = val.substring(0, 12) + "..";
            }

            // Añadir cursor si está enfocado
            if (isFocused && System.currentTimeMillis() % 1000 < 500) {
                val = val + "|";
            }
            RenderHelper.drawString(val, boxX + 3, y + 2, textColor);

            return 16;  // Altura para TextSetting

        } else if (set instanceof KeybindSetting) {
            KeybindSetting keybind = (KeybindSetting) set;

            // Nombre
            RenderHelper.drawString(keybind.getName(), x, y, TEXT.getRGB());

            // Botón de keybind (a la derecha)
            int btnW = 60;
            int btnH = 12;
            int btnX = x + w - btnW;
            boolean hovered = mX >= btnX && mX <= btnX + btnW && mY >= y && mY <= y + btnH;

            // Background color
            int bgColor;
            if (keybind.isWaitingForKey()) {
                bgColor = new Color(80, 60, 120).getRGB(); // Purple when waiting
            } else if (hovered) {
                bgColor = new Color(50, 50, 65).getRGB();
            } else {
                bgColor = new Color(35, 35, 50).getRGB();
            }
            drawRect(btnX, y, btnX + btnW, y + btnH, bgColor);

            // Button text
            String keyText = "[" + keybind.getKeyName() + "]";
            int textColor;
            if (keybind.isWaitingForKey()) {
                textColor = 0xFFFF55; // Yellow
            } else if (keybind.getKeyCode() != 0) {
                textColor = ACCENT.getRGB(); // Purple
            } else {
                textColor = DIM.getRGB(); // Gray
            }
            int textW = RenderHelper.getStringWidth(keyText);
            RenderHelper.drawString(keyText, btnX + (btnW - textW) / 2, y + 2, textColor);

            return 16;  // Altura para KeybindSetting
        }

        return 0;
    }

    private int getSettingHeight(Setting set) {
        if (set instanceof ColorSetting) {
            // 2D color picker: 14 (label) + 50 (square) + 4 (padding) = 68
            // With alpha: + 8 (alpha bar) + 4 (padding) = 80
            return ((ColorSetting) set).hasAlpha() ? 80 : 68;
        }
        if (set instanceof TickSetting) return 16;
        if (set instanceof SliderSetting) return 14;  // Reducido de 20 a 14
        if (set instanceof DoubleSliderSetting) return 14;  // Reducido de 20 a 14
        if (set instanceof ModeSetting) return 16;
        if (set instanceof TextSetting) return 16;
        if (set instanceof KeybindSetting) return 16;
        return 0;
    }

    private void drawSettingsGrid(int mX, int mY) {
        int panelX = x + 4;
        int panelY = y + 62;
        int panelW = width - 8;
        int panelH = height - 67;

        // 2x2 Grid of tabs
        String[] tabNames = {"Config", "HUD", "Friends", "Reset"};
        int tabW = panelW / 2;
        int tabH = 30;

        // Draw tab buttons
        for (int i = 0; i < 4; i++) {
            int gridX = i % 2; // 0 or 1
            int gridY = i / 2; // 0 or 1

            int tabX = panelX + (gridX * tabW);
            int tabY = panelY + (gridY * tabH);

            boolean selected = selectedSettingsTab == i;
            boolean hovered = mX >= tabX && mX <= tabX + tabW && mY >= tabY && mY <= tabY + tabH;

            // Background
            int bg = selected ? LIGHT.getRGB() : PANEL.getRGB();
            if (!selected && hovered) bg = new Color(30, 30, 40).getRGB();
            drawRect(tabX, tabY, tabX + tabW, tabY + tabH, bg);

            // Border
            drawRect(tabX, tabY, tabX + tabW, tabY + 1, new Color(40, 40, 50).getRGB());
            drawRect(tabX, tabY, tabX + 1, tabY + tabH, new Color(40, 40, 50).getRGB());

            // Tab name
            if (!tabNames[i].isEmpty()) {
                int nameW = RenderHelper.getStringWidth(tabNames[i]);
                RenderHelper.drawString(tabNames[i], tabX + (tabW - nameW) / 2, tabY + 10,
                        selected ? ACCENT.getRGB() : TEXT.getRGB());
            }
        }

        // Content area (below tabs)
        int contentY = panelY + tabH * 2;
        int contentH = panelH - tabH * 2;

        // Draw content based on selected tab
        if (selectedSettingsTab == 0) {
            // Config Manager
            drawConfigPanel(mX, mY, panelX, contentY, panelW, contentH);
        } else if (selectedSettingsTab == 1) {
            // HUD Configuration
            drawHUDPanel(mX, mY, panelX, contentY, panelW, contentH);
        } else if (selectedSettingsTab == 2) {
            // Friends Menu (placeholder)
            drawFriendsPanel(mX, mY, panelX, contentY, panelW, contentH);
        } else if (selectedSettingsTab == 3) {
            // Reset panel
            drawResetPanel(mX, mY, panelX, contentY, panelW, contentH);
        }
    }

    private void drawConfigPanel(int mX, int mY, int panelX, int panelY, int panelW, int panelH) {
        // Background
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, PANEL.getRGB());

        // Local/Cloud tabs at the top
        int tabY = panelY + 4;
        int tabW = (panelW - 16) / 2;
        int tabH = 18;

        // Local tab
        int localTabX = panelX + 6;
        boolean localHover = mX >= localTabX && mX <= localTabX + tabW && mY >= tabY && mY <= tabY + tabH;
        drawRect(localTabX, tabY, localTabX + tabW, tabY + tabH, !cloudConfigMode ? ACCENT.getRGB() : (localHover ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String localText = "Local";
        int localTextW = RenderHelper.getStringWidth(localText);
        RenderHelper.drawString(localText, localTabX + (tabW - localTextW) / 2, tabY + 5, !cloudConfigMode ? TEXT.getRGB() : DIM.getRGB());

        // Cloud tab
        int cloudTabX = localTabX + tabW + 4;
        boolean cloudHover = mX >= cloudTabX && mX <= cloudTabX + tabW && mY >= tabY && mY <= tabY + tabH;
        drawRect(cloudTabX, tabY, cloudTabX + tabW, tabY + tabH, cloudConfigMode ? ACCENT.getRGB() : (cloudHover ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String cloudText = "Cloud";
        int cloudTextW = RenderHelper.getStringWidth(cloudText);
        RenderHelper.drawString(cloudText, cloudTabX + (tabW - cloudTextW) / 2, tabY + 5, cloudConfigMode ? TEXT.getRGB() : DIM.getRGB());

        int contentY = tabY + tabH + 4;
        int contentH = panelH - (contentY - panelY);

        if (!cloudConfigMode) {
            // ===== LOCAL CONFIG MODE =====
            drawLocalConfigPanel(mX, mY, panelX, contentY, panelW, contentH);
        } else {
            // ===== CLOUD CONFIG MODE =====
            drawCloudConfigPanel(mX, mY, panelX, contentY, panelW, contentH);
        }
    }

    private void drawLocalConfigPanel(int mX, int mY, int panelX, int panelY, int panelW, int panelH) {
        // Input field for config name
        int inputY = panelY + 2;
        int inputW = panelW - 90;
        int inputH = 18;
        drawRect(panelX + 6, inputY, panelX + 6 + inputW, inputY + inputH, LIGHT.getRGB());

        // Display text with blinking cursor
        String displayText = configNameInput.isEmpty() ? "Enter config name..." : configNameInput;

        // Add blinking underscore when focused
        if (configInputFocused && !configNameInput.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            boolean showCursor = ((currentTime / 500) % 2) == 0;
            if (showCursor) {
                displayText += "_";
            }
        }

        RenderHelper.drawString(displayText, panelX + 10, inputY + 5, configNameInput.isEmpty() ? DIM.getRGB() : TEXT.getRGB());

        // Create button
        int createX = panelX + 6 + inputW + 4;
        int createW = 76;
        boolean createHover = mX >= createX && mX <= createX + createW && mY >= inputY && mY <= inputY + inputH;
        drawRect(createX, inputY, createX + createW, inputY + inputH, createHover ? ACCENT.getRGB() : LIGHT.getRGB());
        String createText = "Create";
        int createTextW = RenderHelper.getStringWidth(createText);
        RenderHelper.drawString(createText, createX + (createW - createTextW) / 2, inputY + 5, TEXT.getRGB());

        // Open Config Folder button (below input, full width)
        int folderY = inputY + inputH + 2;
        int folderW = panelW - 12;
        int folderH = 14;
        boolean folderHover = mX >= panelX + 6 && mX <= panelX + 6 + folderW && mY >= folderY && mY <= folderY + folderH;
        drawRect(panelX + 6, folderY, panelX + 6 + folderW, folderY + folderH, folderHover ? new Color(60, 140, 60).getRGB() : new Color(40, 100, 40).getRGB());
        String folderText = "Open Config Folder";
        int folderTextW = RenderHelper.getStringWidth(folderText);
        RenderHelper.drawString(folderText, panelX + 6 + (folderW - folderTextW) / 2, folderY + 3, new Color(150, 255, 150).getRGB());

        // Action buttons at the bottom (4 buttons now: Save, Load, Delete, Upload)
        int btnY = panelY + panelH - 26;
        int btnW = (panelW - 30) / 4;
        int btnH = 20;

        // Save button
        int saveX = panelX + 6;
        boolean saveHover = selectedConfig != null && mX >= saveX && mX <= saveX + btnW && mY >= btnY && mY <= btnY + btnH;
        drawRect(saveX, btnY, saveX + btnW, btnY + btnH, saveHover ? new Color(80, 200, 80).getRGB() : (selectedConfig != null ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String saveText = "Save";
        int saveTextW = RenderHelper.getStringWidth(saveText);
        RenderHelper.drawString(saveText, saveX + (btnW - saveTextW) / 2, btnY + 6, selectedConfig != null ? TEXT.getRGB() : DIM.getRGB());

        // Load button
        int loadX = saveX + btnW + 3;
        boolean loadHover = selectedConfig != null && mX >= loadX && mX <= loadX + btnW && mY >= btnY && mY <= btnY + btnH;
        drawRect(loadX, btnY, loadX + btnW, btnY + btnH, loadHover ? new Color(80, 120, 255).getRGB() : (selectedConfig != null ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String loadText = "Load";
        int loadTextW = RenderHelper.getStringWidth(loadText);
        RenderHelper.drawString(loadText, loadX + (btnW - loadTextW) / 2, btnY + 6, selectedConfig != null ? TEXT.getRGB() : DIM.getRGB());

        // Delete button
        int delX = loadX + btnW + 3;
        boolean delHover = selectedConfig != null && mX >= delX && mX <= delX + btnW && mY >= btnY && mY <= btnY + btnH;
        drawRect(delX, btnY, delX + btnW, btnY + btnH, delHover ? new Color(200, 80, 80).getRGB() : (selectedConfig != null ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String delText = "Delete";
        int delTextW = RenderHelper.getStringWidth(delText);
        RenderHelper.drawString(delText, delX + (btnW - delTextW) / 2, btnY + 6, selectedConfig != null ? TEXT.getRGB() : DIM.getRGB());

        // Upload/Update button (cloud upload or update existing)
        int uploadX = delX + btnW + 3;
        boolean canUpload = selectedConfig != null && CloudConfigManager.isAuthenticated();
        CloudConfigManager.CloudConfig existingCloudConfig = selectedConfig != null ? CloudConfigManager.getCloudConfigByName(selectedConfig) : null;
        boolean isUpdate = existingCloudConfig != null;
        boolean uploadHover = canUpload && mX >= uploadX && mX <= uploadX + btnW && mY >= btnY && mY <= btnY + btnH;
        int uploadColor = isUpdate ? new Color(200, 150, 80).getRGB() : new Color(80, 180, 200).getRGB();
        drawRect(uploadX, btnY, uploadX + btnW, btnY + btnH, uploadHover ? uploadColor : (canUpload ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String uploadText = isUpdate ? "Update" : "Upload";
        int uploadTextW = RenderHelper.getStringWidth(uploadText);
        RenderHelper.drawString(uploadText, uploadX + (btnW - uploadTextW) / 2, btnY + 6, canUpload ? TEXT.getRGB() : DIM.getRGB());

        // List of configs (between folder button and action buttons)
        int listY = folderY + folderH + 4;
        int listH = btnY - listY - 4;

        // List background
        drawRect(panelX + 6, listY, panelX + panelW - 6, listY + listH, new Color(20, 20, 28).getRGB());

        io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
        if (configManager != null) {
            java.util.List<String> configs = configManager.getConfigs();
            int confY = listY + 2 - configScroll;
            int confH = 20;
            int maxNameW = panelW - 50; // Max width for config name (leave space for cloud icon)

            for (String config : configs) {
                if (confY > listY + listH) break;
                if (confY + confH < listY) {
                    confY += confH;
                    continue;
                }

                // Only draw if within bounds
                if (confY >= listY && confY + confH <= listY + listH) {
                    boolean selected = config.equals(selectedConfig);
                    boolean hovered = mX >= panelX + 8 && mX <= panelX + panelW - 8 &&
                                    mY >= confY && mY <= confY + confH;

                    // Background
                    if (selected) {
                        drawRect(panelX + 8, confY, panelX + panelW - 8, confY + confH - 2, ACCENT.getRGB());
                    } else if (hovered) {
                        drawRect(panelX + 8, confY, panelX + panelW - 8, confY + confH - 2, LIGHT.getRGB());
                    }

                    // Cloud status icon
                    CloudConfigManager.CloudConfig cloudInfo = CloudConfigManager.getCloudConfigByName(config);
                    String icon = "";
                    int iconColor = DIM.getRGB();
                    if (cloudInfo != null) {
                        if (cloudInfo.isPublic) {
                            icon = "\u2601"; // Cloud icon (public)
                            iconColor = new Color(100, 200, 255).getRGB();
                        } else {
                            icon = "\u2601"; // Cloud icon (private)
                            iconColor = new Color(255, 200, 100).getRGB();
                        }
                    }

                    // Config name (truncate if too long)
                    String dispName = config;
                    int nameWidth = RenderHelper.getStringWidth(dispName);
                    if (nameWidth > maxNameW) {
                        while (nameWidth > maxNameW - 10 && dispName.length() > 3) {
                            dispName = dispName.substring(0, dispName.length() - 1);
                            nameWidth = RenderHelper.getStringWidth(dispName + "..");
                        }
                        dispName += "..";
                    }
                    RenderHelper.drawString(dispName, panelX + 12, confY + 5, TEXT.getRGB());

                    // Draw cloud icon if uploaded
                    if (!icon.isEmpty()) {
                        RenderHelper.drawString(icon, panelX + panelW - 22, confY + 5, iconColor);
                    }
                }

                confY += confH;
            }
        }
    }

    private void drawCloudConfigPanel(int mX, int mY, int panelX, int panelY, int panelW, int panelH) {
        // Check if authenticated
        if (!CloudConfigManager.isAuthenticated()) {
            String msg = "Not authenticated";
            int msgW = RenderHelper.getStringWidth(msg);
            RenderHelper.drawString(msg, panelX + (panelW - msgW) / 2, panelY + panelH / 2, DIM.getRGB());
            return;
        }

        // Import by code field at top
        int importY = panelY + 2;
        int importH = 16;
        int importBtnW = 50;
        int importFieldW = panelW - 18 - importBtnW;

        // Import text field
        drawRect(panelX + 6, importY, panelX + 6 + importFieldW, importY + importH,
                cloudImportFocused ? new Color(40, 40, 55).getRGB() : new Color(25, 25, 35).getRGB());
        drawRect(panelX + 6, importY, panelX + 6 + importFieldW, importY + 1,
                cloudImportFocused ? ACCENT.getRGB() : new Color(50, 50, 60).getRGB());

        String importDisplayText = cloudImportCode.isEmpty() ? "Enter config code..." : cloudImportCode;
        if (cloudImportFocused && !cloudImportCode.isEmpty()) {
            long blinkTime = (System.currentTimeMillis() - lastCursorBlink) % 1000;
            if (blinkTime < 500) {
                importDisplayText += "_";
            }
        }
        RenderHelper.drawString(importDisplayText, panelX + 10, importY + 4,
                cloudImportCode.isEmpty() ? DIM.getRGB() : TEXT.getRGB());

        // Import button
        int importBtnX = panelX + 6 + importFieldW + 4;
        boolean canImport = !cloudImportCode.isEmpty() && cloudImportCode.length() >= 6;
        boolean importHover = canImport && mX >= importBtnX && mX <= importBtnX + importBtnW && mY >= importY && mY <= importY + importH;
        drawRect(importBtnX, importY, importBtnX + importBtnW, importY + importH,
                importHover ? new Color(80, 180, 80).getRGB() : (canImport ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String importBtnText = "Import";
        int importBtnTextW = RenderHelper.getStringWidth(importBtnText);
        RenderHelper.drawString(importBtnText, importBtnX + (importBtnW - importBtnTextW) / 2, importY + 4,
                canImport ? TEXT.getRGB() : DIM.getRGB());

        // Cloud sub-tabs: Official, Public, My Configs + Refresh button
        int subTabY = importY + importH + 4;
        int subTabH = 16;
        int refreshW = 50;
        int availableW = panelW - 12 - refreshW - 4; // Space for 3 tabs
        int subTabW = (availableW - 4) / 3; // 3 tabs with 2px gaps
        String[] subTabs = {"Official", "Public", "My Configs"};

        for (int i = 0; i < 3; i++) {
            int subTabX = panelX + 6 + i * (subTabW + 2);
            boolean subHover = mX >= subTabX && mX <= subTabX + subTabW && mY >= subTabY && mY <= subTabY + subTabH;
            drawRect(subTabX, subTabY, subTabX + subTabW, subTabY + subTabH, cloudConfigTab == i ? ACCENT.getRGB() : (subHover ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
            String tabText = subTabs[i];
            int tabTextW = RenderHelper.getStringWidth(tabText);
            RenderHelper.drawString(tabText, subTabX + (subTabW - tabTextW) / 2, subTabY + 4, cloudConfigTab == i ? TEXT.getRGB() : DIM.getRGB());
        }

        // Refresh button (after the 3 tabs)
        int refreshX = panelX + 6 + 3 * (subTabW + 2);
        int refreshY = subTabY;
        boolean refreshHover = !cloudConfigsFetching && mX >= refreshX && mX <= refreshX + refreshW && mY >= refreshY && mY <= refreshY + subTabH;
        drawRect(refreshX, refreshY, refreshX + refreshW, refreshY + subTabH, refreshHover ? new Color(80, 120, 200).getRGB() : LIGHT.getRGB());
        String refreshText = cloudConfigsFetching ? "..." : "Refresh";
        int refreshTextW = RenderHelper.getStringWidth(refreshText);
        RenderHelper.drawString(refreshText, refreshX + (refreshW - refreshTextW) / 2, refreshY + 4, TEXT.getRGB());

        // Action buttons at bottom (5 buttons: Load, Download, Share, Toggle, Delete)
        int btnY = panelY + panelH - 26;
        int btnW = (panelW - 36) / 5;
        int btnH = 20;

        // Load button (apply directly without saving locally)
        int cloudLoadX = panelX + 6;
        boolean canCloudLoad = selectedCloudConfig != null;
        boolean cloudLoadHover = canCloudLoad && mX >= cloudLoadX && mX <= cloudLoadX + btnW && mY >= btnY && mY <= btnY + btnH;
        drawRect(cloudLoadX, btnY, cloudLoadX + btnW, btnY + btnH, cloudLoadHover ? new Color(80, 200, 80).getRGB() : (canCloudLoad ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String cloudLoadText = "Load";
        int cloudLoadTextW = RenderHelper.getStringWidth(cloudLoadText);
        RenderHelper.drawString(cloudLoadText, cloudLoadX + (btnW - cloudLoadTextW) / 2, btnY + 6, canCloudLoad ? TEXT.getRGB() : DIM.getRGB());

        // Download button (save to local configs) - Only for My Configs
        int downloadX = cloudLoadX + btnW + 6;
        boolean canDownload = selectedCloudConfig != null && cloudConfigTab == 2; // My Configs only
        boolean downloadHover = canDownload && mX >= downloadX && mX <= downloadX + btnW && mY >= btnY && mY <= btnY + btnH;
        drawRect(downloadX, btnY, downloadX + btnW, btnY + btnH, downloadHover ? new Color(80, 120, 255).getRGB() : (canDownload ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String downloadText = "Download";
        int downloadTextW = RenderHelper.getStringWidth(downloadText);
        RenderHelper.drawString(downloadText, downloadX + (btnW - downloadTextW) / 2, btnY + 6, canDownload ? TEXT.getRGB() : DIM.getRGB());

        // Share button
        int shareX = downloadX + btnW + 6;
        boolean canShare = selectedCloudConfig != null;
        boolean shareHover = canShare && mX >= shareX && mX <= shareX + btnW && mY >= btnY && mY <= btnY + btnH;
        drawRect(shareX, btnY, shareX + btnW, btnY + btnH, shareHover ? new Color(80, 200, 120).getRGB() : (canShare ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String shareText = "Share";
        int shareTextW = RenderHelper.getStringWidth(shareText);
        RenderHelper.drawString(shareText, shareX + (btnW - shareTextW) / 2, btnY + 6, canShare ? TEXT.getRGB() : DIM.getRGB());

        // Toggle button (only for My Configs - toggle public/private)
        int toggleX = shareX + btnW + 6;
        boolean canToggle = selectedCloudConfig != null && cloudConfigTab == 2; // My Configs only
        boolean toggleHover = canToggle && mX >= toggleX && mX <= toggleX + btnW && mY >= btnY && mY <= btnY + btnH;
        int toggleColor = selectedCloudConfig != null && selectedCloudConfig.isPublic ? new Color(200, 100, 80).getRGB() : new Color(80, 180, 100).getRGB();
        drawRect(toggleX, btnY, toggleX + btnW, btnY + btnH, toggleHover ? toggleColor : (canToggle ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String toggleText = selectedCloudConfig != null && selectedCloudConfig.isPublic ? "Private" : "Public";
        int toggleTextW = RenderHelper.getStringWidth(toggleText);
        RenderHelper.drawString(toggleText, toggleX + (btnW - toggleTextW) / 2, btnY + 6, canToggle ? TEXT.getRGB() : DIM.getRGB());

        // Delete button (only for My Configs)
        int delX = toggleX + btnW + 6;
        boolean canDelete = selectedCloudConfig != null && cloudConfigTab == 2; // My Configs
        boolean delHover = canDelete && mX >= delX && mX <= delX + btnW && mY >= btnY && mY <= btnY + btnH;
        drawRect(delX, btnY, delX + btnW, btnY + btnH, delHover ? new Color(200, 80, 80).getRGB() : (canDelete ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB()));
        String delText = "Delete";
        int delTextW = RenderHelper.getStringWidth(delText);
        RenderHelper.drawString(delText, delX + (btnW - delTextW) / 2, btnY + 6, canDelete ? TEXT.getRGB() : DIM.getRGB());

        // Config list
        int listY = subTabY + subTabH + 4;
        int listH = btnY - listY - 4;

        // List background
        drawRect(panelX + 6, listY, panelX + panelW - 6, listY + listH, new Color(20, 20, 28).getRGB());

        // Get configs based on selected tab
        java.util.List<CloudConfigManager.CloudConfig> configs;
        if (cloudConfigTab == 0) {
            configs = CloudConfigManager.getOfficialConfigs();
        } else if (cloudConfigTab == 1) {
            configs = CloudConfigManager.getPublicConfigs();
        } else {
            configs = CloudConfigManager.getOwnConfigs();
        }

        if (configs.isEmpty()) {
            String emptyMsg = cloudConfigsFetching ? "Loading..." : "No configs found";
            int emptyW = RenderHelper.getStringWidth(emptyMsg);
            RenderHelper.drawString(emptyMsg, panelX + (panelW - emptyW) / 2, listY + listH / 2 - 5, DIM.getRGB());
        } else {
            int confY = listY + 2 - cloudConfigScroll;
            int confH = 24;
            int maxNameW = panelW - 80;

            for (CloudConfigManager.CloudConfig config : configs) {
                if (confY > listY + listH) break;
                if (confY + confH < listY) {
                    confY += confH;
                    continue;
                }

                if (confY >= listY && confY + confH <= listY + listH) {
                    boolean selected = config.equals(selectedCloudConfig);
                    boolean hovered = mX >= panelX + 8 && mX <= panelX + panelW - 8 &&
                                    mY >= confY && mY <= confY + confH;

                    // Background
                    if (selected) {
                        drawRect(panelX + 8, confY, panelX + panelW - 8, confY + confH - 2, ACCENT.getRGB());
                    } else if (hovered) {
                        drawRect(panelX + 8, confY, panelX + panelW - 8, confY + confH - 2, LIGHT.getRGB());
                    }

                    // Config name
                    String dispName = config.configName;
                    int nameWidth = RenderHelper.getStringWidth(dispName);
                    if (nameWidth > maxNameW) {
                        while (nameWidth > maxNameW - 10 && dispName.length() > 3) {
                            dispName = dispName.substring(0, dispName.length() - 1);
                            nameWidth = RenderHelper.getStringWidth(dispName + "..");
                        }
                        dispName += "..";
                    }
                    RenderHelper.drawString(dispName, panelX + 12, confY + 3, TEXT.getRGB());

                    // Author and downloads
                    String info = "by " + config.username + " | " + config.downloads + " DL";
                    RenderHelper.drawString(info, panelX + 12, confY + 13, DIM.getRGB());

                    // Status icon
                    String icon = config.isOfficial ? "\u2605" : (config.isPublic ? "\u2601" : "\u26BF");
                    int iconColor = config.isOfficial ? new Color(255, 200, 50).getRGB() : (config.isPublic ? new Color(100, 200, 255).getRGB() : new Color(255, 200, 100).getRGB());
                    RenderHelper.drawString(icon, panelX + panelW - 22, confY + 7, iconColor);
                }

                confY += confH;
            }
        }

        // Status message at top right
        if (!cloudConfigStatus.isEmpty()) {
            int statusW = RenderHelper.getStringWidth(cloudConfigStatus);
            RenderHelper.drawString(cloudConfigStatus, panelX + panelW - statusW - 65, subTabY + 4, new Color(200, 200, 100).getRGB());
        }
    }

    private void handleLocalConfigClicks(int mX, int mY, int panelX, int panelW, int panelY, int panelH) {
        // Input field click (focus for typing)
        int inputY = panelY + 2;
        int inputW = panelW - 90;
        int inputH = 18;
        if (mX >= panelX + 6 && mX <= panelX + 6 + inputW && mY >= inputY && mY <= inputY + inputH) {
            configInputFocused = true;
            return;
        } else {
            configInputFocused = false;
        }

        // Create button click
        int createX = panelX + 6 + inputW + 4;
        int createW = 76;
        if (mX >= createX && mX <= createX + createW && mY >= inputY && mY <= inputY + inputH) {
            if (!configNameInput.isEmpty()) {
                io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                if (configManager != null && !configManager.configExists(configNameInput)) {
                    configManager.saveConfig(configNameInput);
                    selectedConfig = configNameInput;
                    configNameInput = "";
                }
            }
            return;
        }

        // Open Config Folder button click
        int folderY = inputY + inputH + 2;
        int folderW = panelW - 12;
        int folderH = 14;
        if (mX >= panelX + 6 && mX <= panelX + 6 + folderW && mY >= folderY && mY <= folderY + folderH) {
            io.github.exodar.config.ConfigManager cm = io.github.exodar.Main.getConfigManager();
            if (cm != null) {
                cm.openConfigFolder();
                io.github.exodar.ui.ModuleNotification.addNotification("Opening config folder...", true);
            }
            return;
        }

        // Config list clicks
        int btnY = panelY + panelH - 26;
        int configListY = folderY + folderH + 4;
        int configListH = btnY - configListY - 4;

        io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
        if (configManager != null) {
            java.util.List<String> configs = configManager.getConfigs();
            int confY = configListY + 2 - configScroll;
            int confH = 20;

            for (String config : configs) {
                if (confY > configListY + configListH) break;
                if (confY + confH < configListY) {
                    confY += confH;
                    continue;
                }
                // Check if at least part of the config is visible (relaxed bounds)
                if (confY + confH > configListY && confY < configListY + configListH) {
                    // Clamp click area to visible region
                    int clickTop = Math.max(confY, configListY);
                    int clickBottom = Math.min(confY + confH, configListY + configListH);
                    if (mX >= panelX + 8 && mX <= panelX + panelW - 8 && mY >= clickTop && mY <= clickBottom) {
                        selectedConfig = config;
                        return;
                    }
                }
                confY += confH;
            }
        }

        // Action buttons (4 buttons: Save, Load, Delete, Upload)
        int btnW = (panelW - 30) / 4;
        int btnH = 20;
        int saveX = panelX + 6;
        int loadX = saveX + btnW + 3;
        int delX = loadX + btnW + 3;
        int uploadX = delX + btnW + 3;

        if (selectedConfig != null && mY >= btnY && mY <= btnY + btnH) {
            // Save button
            if (mX >= saveX && mX <= saveX + btnW) {
                if (configManager != null) {
                    configManager.saveConfig(selectedConfig);
                    io.github.exodar.ui.ModuleNotification.addNotification("Config saved!", true);
                }
                return;
            }
            // Load button
            if (mX >= loadX && mX <= loadX + btnW) {
                if (configManager != null) {
                    configManager.loadConfig(selectedConfig);
                    io.github.exodar.ui.ModuleNotification.addNotification("Config loaded!", true);
                }
                return;
            }
            // Delete button
            if (mX >= delX && mX <= delX + btnW) {
                if (configManager != null) {
                    configManager.deleteConfig(selectedConfig);
                    selectedConfig = null;
                    io.github.exodar.ui.ModuleNotification.addNotification("Config deleted", false);
                }
                return;
            }
            // Upload/Update button
            if (mX >= uploadX && mX <= uploadX + btnW) {
                if (!CloudConfigManager.isAuthenticated()) {
                    io.github.exodar.ui.ModuleNotification.addNotification("Not authenticated for cloud", false);
                    return;
                }
                final String configToUpload = selectedConfig;
                if (configManager != null) {
                    String configData = configManager.getConfigData(configToUpload);
                    if (configData != null) {
                        // Check if config already exists in cloud
                        CloudConfigManager.CloudConfig existingCloud = CloudConfigManager.getCloudConfigByName(configToUpload);
                        if (existingCloud != null) {
                            // Update existing cloud config
                            cloudConfigStatus = "Updating...";
                            io.github.exodar.ui.ModuleNotification.addNotification("Updating " + configToUpload + "...", true);
                            CloudConfigManager.updateConfig(existingCloud.configId, configData).thenAccept(success -> {
                                if (success) {
                                    cloudConfigStatus = "";
                                } else {
                                    cloudConfigStatus = "Update failed";
                                }
                            });
                        } else {
                            // Upload as new config
                            cloudConfigStatus = "Uploading...";
                            io.github.exodar.ui.ModuleNotification.addNotification("Uploading " + configToUpload + "...", true);
                            CloudConfigManager.uploadConfig(configToUpload, configData, false).thenAccept(configId -> {
                                if (configId != null) {
                                    cloudConfigStatus = "";
                                } else {
                                    cloudConfigStatus = "Upload failed";
                                }
                            });
                        }
                    } else {
                        io.github.exodar.ui.ModuleNotification.addNotification("Failed to get config data", false);
                    }
                }
                return;
            }
        }
    }

    private void handleCloudConfigClicks(int mX, int mY, int panelX, int panelW, int panelY, int panelH) {
        if (!CloudConfigManager.isAuthenticated()) return;

        // Import field (at top)
        int importY = panelY + 2;
        int importH = 16;
        int importBtnW = 50;
        int importFieldW = panelW - 18 - importBtnW;

        // Import text field click
        if (mX >= panelX + 6 && mX <= panelX + 6 + importFieldW && mY >= importY && mY <= importY + importH) {
            cloudImportFocused = true;
            configInputFocused = false; // Unfocus local config input
            return;
        }

        // Import button click
        int importBtnX = panelX + 6 + importFieldW + 4;
        if (!cloudImportCode.isEmpty() && cloudImportCode.length() >= 6 &&
            mX >= importBtnX && mX <= importBtnX + importBtnW && mY >= importY && mY <= importY + importH) {
            final String codeToImport = cloudImportCode.toUpperCase().trim();
            cloudConfigStatus = "Importing...";
            cloudImportFocused = false;
            CloudConfigManager.downloadConfig(codeToImport).thenAccept(result -> {
                if (result != null) {
                    String configName = result[0];
                    String configData = result[1];
                    String username = result[2];
                    io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                    if (configManager != null) {
                        String localName = configName;
                        int counter = 1;
                        while (configManager.configExists(localName)) {
                            localName = configName + "_" + counter++;
                        }
                        configManager.saveConfigData(localName, configData);
                        configManager.loadConfig(localName);
                        io.github.exodar.ui.ModuleNotification.addNotification("Imported: " + localName + " by " + username, true);
                        cloudConfigStatus = "";
                        cloudImportCode = "";
                    }
                } else {
                    cloudConfigStatus = "Import failed";
                }
            });
            return;
        }

        // Unfocus import field if clicking elsewhere
        cloudImportFocused = false;

        // Cloud sub-tabs (same calculation as draw, adjusted for import field)
        int subTabY = importY + importH + 4;
        int subTabH = 16;
        int refreshW = 50;
        int availableW = panelW - 12 - refreshW - 4;
        int subTabW = (availableW - 4) / 3;

        if (mY >= subTabY && mY <= subTabY + subTabH) {
            for (int i = 0; i < 3; i++) {
                int subTabX = panelX + 6 + i * (subTabW + 2);
                if (mX >= subTabX && mX <= subTabX + subTabW) {
                    cloudConfigTab = i;
                    selectedCloudConfig = null;
                    cloudConfigScroll = 0;
                    return;
                }
            }
        }

        // Refresh button (after 3 tabs)
        int refreshX = panelX + 6 + 3 * (subTabW + 2);
        if (!cloudConfigsFetching && mX >= refreshX && mX <= refreshX + refreshW && mY >= subTabY && mY <= subTabY + subTabH) {
            cloudConfigsFetching = true;
            cloudConfigStatus = "";
            CloudConfigManager.fetchConfigs().thenAccept(success -> {
                cloudConfigsFetching = false;
                cloudConfigStatus = success ? "" : "Fetch failed";
            });
            return;
        }

        // Action buttons (5 buttons: Load, Download/Save, Share, Toggle, Delete)
        int btnY = panelY + panelH - 26;
        int btnW = (panelW - 36) / 5;
        int btnH = 20;
        int cloudLoadX = panelX + 6;
        int downloadX = cloudLoadX + btnW + 6;
        int shareX = downloadX + btnW + 6;
        int toggleX = shareX + btnW + 6;
        int delX = toggleX + btnW + 6;

        if (mY >= btnY && mY <= btnY + btnH) {
            // Load button - apply config directly without saving locally
            if (selectedCloudConfig != null && mX >= cloudLoadX && mX <= cloudLoadX + btnW) {
                final CloudConfigManager.CloudConfig configToLoad = selectedCloudConfig;
                cloudConfigStatus = "Loading...";
                CloudConfigManager.downloadConfig(configToLoad.configId).thenAccept(result -> {
                    if (result != null) {
                        String configData = result[1];
                        io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                        if (configManager != null) {
                            // Load directly without saving
                            configManager.loadConfigFromData(configData);
                            io.github.exodar.ui.ModuleNotification.addNotification("Loaded: " + configToLoad.configName, true);
                            cloudConfigStatus = "";
                        }
                    } else {
                        cloudConfigStatus = "Load failed";
                    }
                });
                return;
            }
            // Download/Save button - save to local configs (My Configs only)
            if (selectedCloudConfig != null && cloudConfigTab == 2 && mX >= downloadX && mX <= downloadX + btnW) {
                final CloudConfigManager.CloudConfig configToDownload = selectedCloudConfig;
                cloudConfigStatus = "Saving...";
                CloudConfigManager.downloadConfig(configToDownload.configId).thenAccept(result -> {
                    if (result != null) {
                        String configName = result[0];
                        String configData = result[1];
                        io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                        if (configManager != null) {
                            // Save locally with unique name
                            String localName = configName;
                            int counter = 1;
                            while (configManager.configExists(localName)) {
                                localName = configName + "_" + counter++;
                            }
                            configManager.saveConfigData(localName, configData);
                            io.github.exodar.ui.ModuleNotification.addNotification("Saved: " + localName, true);
                            cloudConfigStatus = "";
                        }
                    } else {
                        cloudConfigStatus = "Save failed";
                    }
                });
                return;
            }
            // Share button - copy config ID to clipboard
            if (selectedCloudConfig != null && mX >= shareX && mX <= shareX + btnW) {
                String configId = selectedCloudConfig.configId;
                try {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(configId);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    io.github.exodar.ui.ModuleNotification.addNotification("Copied: " + configId, true);
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc.thePlayer != null) {
                        mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText("\u00A7a[Exodar] \u00A7fConfig ID copied: \u00A7b" + configId));
                    }
                } catch (Exception e) {
                    io.github.exodar.ui.ModuleNotification.addNotification("Copy failed", false);
                }
                return;
            }
            // Toggle button - toggle public/private (only My Configs)
            if (selectedCloudConfig != null && cloudConfigTab == 2 && mX >= toggleX && mX <= toggleX + btnW) {
                final String configIdToToggle = selectedCloudConfig.configId;
                cloudConfigStatus = "Toggling...";
                CloudConfigManager.toggleVisibility(configIdToToggle).thenAccept(success -> {
                    if (success) {
                        cloudConfigStatus = "";
                    } else {
                        cloudConfigStatus = "Toggle failed";
                    }
                });
                return;
            }
            // Delete button (only My Configs)
            if (selectedCloudConfig != null && cloudConfigTab == 2 && mX >= delX && mX <= delX + btnW) {
                final String configIdToDelete = selectedCloudConfig.configId;
                cloudConfigStatus = "Deleting...";
                CloudConfigManager.deleteConfig(configIdToDelete).thenAccept(success -> {
                    if (success) {
                        selectedCloudConfig = null;
                        cloudConfigStatus = "";
                    } else {
                        cloudConfigStatus = "Delete failed";
                    }
                });
                return;
            }
        }

        // Cloud config list clicks
        int listY = subTabY + subTabH + 4;
        int listH = btnY - listY - 4;

        java.util.List<CloudConfigManager.CloudConfig> configs;
        if (cloudConfigTab == 0) {
            configs = CloudConfigManager.getOfficialConfigs();
        } else if (cloudConfigTab == 1) {
            configs = CloudConfigManager.getPublicConfigs();
        } else {
            configs = CloudConfigManager.getOwnConfigs();
        }

        int confY = listY + 2 - cloudConfigScroll;
        int confH = 24;

        for (CloudConfigManager.CloudConfig config : configs) {
            if (confY > listY + listH) break;
            if (confY + confH < listY) {
                confY += confH;
                continue;
            }
            if (confY >= listY && confY + confH <= listY + listH) {
                if (mX >= panelX + 8 && mX <= panelX + panelW - 8 && mY >= confY && mY <= confY + confH) {
                    selectedCloudConfig = config;
                    return;
                }
            }
            confY += confH;
        }
    }

    private void drawHUDPanel(int mX, int mY, int panelX, int panelY, int panelW, int panelH) {
        // Background
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, PANEL.getRGB());

        // Get HUD module
        Module hudModule = moduleManager.getModuleByName("HUD");
        if (hudModule == null) {
            String text = "HUD Module not found";
            int textW = RenderHelper.getStringWidth(text);
            RenderHelper.drawString(text, panelX + (panelW - textW) / 2, panelY + 20, DIM.getRGB());
            return;
        }

        // Title
        String title = "HUD Settings";
        int titleW = RenderHelper.getStringWidth(title);
        RenderHelper.drawString(title, panelX + (panelW - titleW) / 2, panelY + 8, ACCENT.getRGB());

        // Draw HUD settings
        List<Setting> settings = hudModule.getSettings();
        int sY = panelY + 28;
        int settingW = panelW - 20;

        for (Setting set : settings) {
            if (set instanceof DescriptionSetting) {
                // Draw section header
                String desc = ((DescriptionSetting) set).getName();
                RenderHelper.drawString(desc, panelX + 10, sY, DIM.getRGB());
                sY += 14;
                continue;
            }

            if (!set.isVisible()) continue;
            if (sY > panelY + panelH - 20) break;

            sY += drawSetting(set, panelX + 10, sY, settingW, mX, mY);
        }
    }

    private int friendsScroll = 0;

    private void drawFriendsPanel(int mX, int mY, int panelX, int panelY, int panelW, int panelH) {
        // Background
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, PANEL.getRGB());

        // Title
        RenderHelper.drawString("Friends List", panelX + 6, panelY + 6, ACCENT.getRGB());

        // Add friend input field
        int inputX = panelX + 6;
        int inputY = panelY + 22;
        int inputW = panelW - 60;
        int inputH = 16;

        // Input background
        drawRect(inputX, inputY, inputX + inputW, inputY + inputH, friendInputFocused ? new Color(45, 45, 60).getRGB() : new Color(35, 35, 50).getRGB());
        drawRect(inputX, inputY, inputX + inputW, inputY + 1, friendInputFocused ? ACCENT.getRGB() : new Color(60, 60, 80).getRGB());

        // Input text or placeholder
        String displayText = friendNameInput;
        if (friendInputFocused && System.currentTimeMillis() % 1000 < 500) {
            displayText += "|";
        }
        if (friendNameInput.isEmpty() && !friendInputFocused) {
            RenderHelper.drawString("Enter username...", inputX + 4, inputY + 4, DIM.getRGB());
        } else {
            RenderHelper.drawString(displayText, inputX + 4, inputY + 4, TEXT.getRGB());
        }

        // Add button
        int addBtnX = inputX + inputW + 4;
        int addBtnW = panelW - inputW - 16;
        boolean addHovered = mX >= addBtnX && mX <= addBtnX + addBtnW && mY >= inputY && mY <= inputY + inputH;
        drawRect(addBtnX, inputY, addBtnX + addBtnW, inputY + inputH, addHovered ? new Color(60, 120, 60).getRGB() : new Color(40, 80, 40).getRGB());
        RenderHelper.drawString("Add", addBtnX + (addBtnW - RenderHelper.getStringWidth("Add")) / 2, inputY + 4, TEXT.getRGB());

        // Friends list
        io.github.exodar.friend.FriendManager fm = io.github.exodar.friend.FriendManager.getInstance();
        java.util.List<String> friends = fm.getFriendsList();

        int listY = panelY + 44;
        int listH = panelH - 60;
        int entryH = 18;

        if (friends.isEmpty()) {
            String noFriends = "No friends added";
            int nfW = RenderHelper.getStringWidth(noFriends);
            RenderHelper.drawString(noFriends, panelX + (panelW - nfW) / 2, panelY + panelH / 2, DIM.getRGB());
        } else {
            int yOff = listY - friendsScroll;
            for (String friend : friends) {
                if (yOff + entryH < listY) {
                    yOff += entryH;
                    continue;
                }
                if (yOff > panelY + panelH - 20) break;

                // Entry background
                boolean hovered = mX >= panelX + 6 && mX <= panelX + panelW - 6 && mY >= yOff && mY <= yOff + entryH - 2;
                drawRect(panelX + 6, yOff, panelX + panelW - 6, yOff + entryH - 2, hovered ? LIGHT.getRGB() : new Color(30, 30, 40).getRGB());

                // Friend name (cyan color - always)
                RenderHelper.drawString(friend, panelX + 10, yOff + 4, 0x55FFFF);

                // X button to remove
                int xBtnX = panelX + panelW - 24;
                boolean xHovered = mX >= xBtnX && mX <= xBtnX + 14 && mY >= yOff + 2 && mY <= yOff + entryH - 4;
                drawRect(xBtnX, yOff + 2, xBtnX + 14, yOff + entryH - 4, xHovered ? new Color(200, 50, 50).getRGB() : new Color(100, 40, 40).getRGB());
                RenderHelper.drawString("X", xBtnX + 4, yOff + 4, TEXT.getRGB());

                yOff += entryH;
            }
        }

        // Friend count and hint
        String countText = friends.size() + " friend" + (friends.size() != 1 ? "s" : "");
        RenderHelper.drawString(countText, panelX + 6, panelY + panelH - 14, DIM.getRGB());

        String hint = "Middle-click players ingame";
        RenderHelper.drawString(hint, panelX + panelW - RenderHelper.getStringWidth(hint) - 6, panelY + panelH - 14, DIM.getRGB());
    }

    private void drawResetPanel(int mX, int mY, int panelX, int panelY, int panelW, int panelH) {
        // Background
        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, PANEL.getRGB());

        // Title
        String title = "Reset Options";
        int titleW = RenderHelper.getStringWidth(title);
        RenderHelper.drawString(title, panelX + (panelW - titleW) / 2, panelY + 8, ACCENT.getRGB());

        // Warning text
        String warning = "Use these options if settings are not working";
        int warningW = RenderHelper.getStringWidth(warning);
        RenderHelper.drawString(warning, panelX + (panelW - warningW) / 2, panelY + 28, DIM.getRGB());

        int btnW = 200;
        int btnH = 24;
        int btnX = panelX + (panelW - btnW) / 2;

        // Button 1: Reset Hidden Modules
        int btn1Y = panelY + 50;
        boolean btn1Hover = mX >= btnX && mX <= btnX + btnW && mY >= btn1Y && mY <= btn1Y + btnH;
        drawRect(btnX, btn1Y, btnX + btnW, btn1Y + btnH, btn1Hover ? new Color(60, 60, 80).getRGB() : LIGHT.getRGB());
        String btn1Text = "Unhide All Modules";
        int btn1TextW = RenderHelper.getStringWidth(btn1Text);
        RenderHelper.drawString(btn1Text, btnX + (btnW - btn1TextW) / 2, btn1Y + 8, TEXT.getRGB());

        // Button 2: Reset Keybinds
        int btn2Y = panelY + 80;
        boolean btn2Hover = mX >= btnX && mX <= btnX + btnW && mY >= btn2Y && mY <= btn2Y + btnH;
        drawRect(btnX, btn2Y, btnX + btnW, btn2Y + btnH, btn2Hover ? new Color(60, 60, 80).getRGB() : LIGHT.getRGB());
        String btn2Text = "Clear All Keybinds";
        int btn2TextW = RenderHelper.getStringWidth(btn2Text);
        RenderHelper.drawString(btn2Text, btnX + (btnW - btn2TextW) / 2, btn2Y + 8, TEXT.getRGB());

        // Button 3: Reset Invert Clicks (important for fixing click issues)
        int btn3Y = panelY + 110;
        boolean btn3Hover = mX >= btnX && mX <= btnX + btnW && mY >= btn3Y && mY <= btn3Y + btnH;
        boolean invertEnabled = ArrayListConfig.invertMenuClicks;
        drawRect(btnX, btn3Y, btnX + btnW, btn3Y + btnH, btn3Hover ? new Color(60, 60, 80).getRGB() : LIGHT.getRGB());
        String btn3Text = "Invert Clicks: " + (invertEnabled ? "ON (Right=Toggle)" : "OFF (Left=Toggle)");
        int btn3TextW = RenderHelper.getStringWidth(btn3Text);
        RenderHelper.drawString(btn3Text, btnX + (btnW - btn3TextW) / 2, btn3Y + 8, invertEnabled ? new Color(255, 200, 100).getRGB() : TEXT.getRGB());

        // Button 4: Delete ALL Configs (nuclear option)
        int btn4Y = panelY + 150;
        boolean btn4Hover = mX >= btnX && mX <= btnX + btnW && mY >= btn4Y && mY <= btn4Y + btnH;
        drawRect(btnX, btn4Y, btnX + btnW, btn4Y + btnH, btn4Hover ? new Color(180, 60, 60).getRGB() : new Color(120, 40, 40).getRGB());
        String btn4Text = "DELETE ALL CONFIGS";
        int btn4TextW = RenderHelper.getStringWidth(btn4Text);
        RenderHelper.drawString(btn4Text, btnX + (btnW - btn4TextW) / 2, btn4Y + 8, new Color(255, 100, 100).getRGB());

        // Button 5: Open Config Folder
        int btn5Y = panelY + 190;
        boolean btn5Hover = mX >= btnX && mX <= btnX + btnW && mY >= btn5Y && mY <= btn5Y + btnH;
        drawRect(btnX, btn5Y, btnX + btnW, btn5Y + btnH, btn5Hover ? new Color(60, 80, 60).getRGB() : new Color(40, 60, 40).getRGB());
        String btn5Text = "Open Config Folder";
        int btn5TextW = RenderHelper.getStringWidth(btn5Text);
        RenderHelper.drawString(btn5Text, btnX + (btnW - btn5TextW) / 2, btn5Y + 8, new Color(150, 255, 150).getRGB());

        // Help text
        String helpText = "Click 'Invert Clicks' if left-click doesn't work";
        int helpTextW = RenderHelper.getStringWidth(helpText);
        RenderHelper.drawString(helpText, panelX + (panelW - helpTextW) / 2, panelY + panelH - 30, DIM.getRGB());
    }

    private String getKeyName(int keyCode) {
        // Handle mouse buttons (negative values)
        if (keyCode < 0) {
            int mouseButton = keyCode + 100;
            if (mouseButton == 0) return "LMB";
            if (mouseButton == 1) return "RMB";
            if (mouseButton == 2) return "MMB";
            return "MB" + (mouseButton + 1);
        }

        // Handle keyboard keys
        String name = Keyboard.getKeyName(keyCode);
        if (name == null) return "KEY" + keyCode;
        return name;
    }

    private List<Module> getModulesForCategory(ModuleCategory cat) {
        List<Module> result = new ArrayList<>();
        for (Module mod : moduleManager.getModules()) {
            if (mod.getCategory() == cat && !mod.isHidden()) {
                result.add(mod);
            }
        }
        return result;
    }

    @Override
    protected void mouseClicked(int mX, int mY, int btn) {
        // Handle KeybindSetting waiting for mouse button (buttons 2+ are extra mouse buttons)
        if (waitingKeybindSetting != null && btn >= 2) {
            waitingKeybindSetting.setMouseButton(btn);
            waitingKeybindSetting = null;
            return;
        }

        // Drag
        if (mX >= x && mX <= x + width && mY >= y && mY <= y + 25) {
            dragging = true;
            dragX = mX - x;
            dragY = mY - y;
            return;
        }

        // Tabs
        int tabY = y + 30;
        int tabW = width / 6;
        int tabH = 28;
        // Only show these categories (excluding RENDER)
        ModuleCategory[] cats = {ModuleCategory.COMBAT, ModuleCategory.PLAYER, ModuleCategory.MOVEMENT,
                                 ModuleCategory.VISUALS, ModuleCategory.MISC, ModuleCategory.SETTINGS};

        for (int i = 0; i < cats.length; i++) {
            int tabX = x + (i * tabW);
            if (mX >= tabX && mX <= tabX + tabW && mY >= tabY && mY <= tabY + tabH) {
                selectedCategory = cats[i];
                selectedModule = null;
                moduleScroll = 0;
                settingScroll = 0;
                return;
            }
        }

        // Module list
        int listX = x + 4;
        int listY = y + 62;
        int listW = 140;
        int listH = height - 67;

        if (mX >= listX && mX <= listX + listW && mY >= listY && mY <= listY + listH) {
            List<Module> modules = getModulesForCategory(selectedCategory);
            int modY = listY + 3 - moduleScroll;
            int modH = 18;

            for (Module mod : modules) {
                if (modY > listY + listH) break;
                if (modY + modH < listY) {
                    modY += modH;
                    continue;
                }

                if (mY >= modY && mY <= modY + modH) {
                    // Check if clicking on keybind text
                    String keybindText = "";
                    int toggleBind = mod.getToggleBind();
                    int holdBind = mod.getHoldBind();

                    if (waitingForBind == mod) {
                        keybindText = waitingForBindMode == Module.BindMode.TOGGLE ? "[T...]" : "[H...]";
                    } else if (toggleBind != 0) {
                        keybindText = "[" + getKeyName(toggleBind) + "]";
                    } else if (holdBind != 0) {
                        keybindText = "[" + getKeyName(holdBind) + "]";
                    } else {
                        keybindText = "[...]";
                    }

                    // Always check keybind area
                    int keybindW = RenderHelper.getStringWidth(keybindText);
                    int keybindX = listX + listW - keybindW - 6;

                    // Check if clicking on keybind area
                    if (mX >= keybindX && mX <= keybindX + keybindW) {
                        if (btn == 0) {  // Left click - rebind (keeps current mode or defaults to TOGGLE)
                            if (toggleBind != 0) {
                                // Has TOGGLE, rebind as TOGGLE
                                waitingForBind = mod;
                                waitingForBindMode = Module.BindMode.TOGGLE;
                            } else if (holdBind != 0) {
                                // Has HOLD, rebind as HOLD
                                waitingForBind = mod;
                                waitingForBindMode = Module.BindMode.HOLD;
                            } else {
                                // No bind, default to TOGGLE
                                waitingForBind = mod;
                                waitingForBindMode = Module.BindMode.TOGGLE;
                            }
                        } else if (btn == 1) {  // Right click - delete bind
                            mod.setToggleBind(0);
                            mod.setHoldBind(0);
                            waitingForBind = null;
                            waitingForBindMode = null;
                        } else if (btn == 2) {  // Middle click - switch mode
                            if (toggleBind != 0) {
                                // Switch TOGGLE → HOLD
                                waitingForBind = mod;
                                waitingForBindMode = Module.BindMode.HOLD;
                                mod.setToggleBind(0);
                            } else if (holdBind != 0) {
                                // Switch HOLD → TOGGLE
                                waitingForBind = mod;
                                waitingForBindMode = Module.BindMode.TOGGLE;
                                mod.setHoldBind(0);
                            }
                        }
                        return;
                    }

                    // Click on module name area
                    // Check if clicks are inverted (but accept both buttons for better UX)
                    int toggleButton = ArrayListConfig.invertMenuClicks ? 1 : 0;
                    int settingsButton = ArrayListConfig.invertMenuClicks ? 0 : 1;

                    if (btn == toggleButton) {  // Toggle module (primary)
                        mod.toggle();
                    } else if (btn == settingsButton) {  // Select (open config)
                        selectedModule = mod;
                        settingScroll = 0;
                    } else if (btn == 2) {  // Middle click always toggles (fallback)
                        mod.toggle();
                    }
                    return;
                }
                modY += modH;
            }
        }

        // Settings
        if (selectedModule != null && selectedCategory != ModuleCategory.SETTINGS) {
            int setX = x + 150;
            int setY = y + 62;
            int setW = width - 156;
            int setH = height - 67;

            // Header keybind buttons click handling
            int btnW = 60;
            int btnH = 14;
            int btnY = setY + 3;
            int btnSpacing = 3;

            // Hold button
            int holdBtnX = setX + setW - btnW - 6;
            if (mX >= holdBtnX && mX <= holdBtnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                if (btn == 0) {  // Left click - set bind
                    waitingForBind = selectedModule;
                    waitingForBindMode = Module.BindMode.HOLD;
                } else if (btn == 1) {  // Right click - clear bind
                    selectedModule.setHoldBind(0);
                    waitingForBind = null;
                    waitingForBindMode = null;
                }
                return;
            }

            // Toggle button
            int toggleBtnX = holdBtnX - btnW - btnSpacing;
            if (mX >= toggleBtnX && mX <= toggleBtnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                if (btn == 0) {  // Left click - set bind
                    waitingForBind = selectedModule;
                    waitingForBindMode = Module.BindMode.TOGGLE;
                } else if (btn == 1) {  // Right click - clear bind
                    selectedModule.setToggleBind(0);
                    waitingForBind = null;
                    waitingForBindMode = null;
                }
                return;
            }

            // Hide button
            int hideBtnX = toggleBtnX - btnW - btnSpacing;
            if (mX >= hideBtnX && mX <= hideBtnX + btnW && mY >= btnY && mY <= btnY + btnH) {
                if (btn == 0) {  // Left click - toggle hide
                    UserConfig userConfig = UserConfig.getInstance();
                    if (userConfig.isModuleHidden(selectedModule)) {
                        userConfig.unhideModule(selectedModule.getName());
                    } else {
                        userConfig.hideModule(selectedModule.getName());
                    }
                }
                return;
            }

            if (mX >= setX + 6 && mX <= setX + setW - 6 && mY >= setY + 24 && mY <= setY + setH) {
                List<Setting> settings = selectedModule.getSettings();
                int sY = setY + 24 - settingScroll;

                for (Setting set : settings) {
                    if (set instanceof DescriptionSetting || !set.isVisible()) continue;

                    if (sY > setY + setH) break;
                    if (sY < setY + 20) {
                        sY += getSettingHeight(set);
                        continue;
                    }

                    int h = getSettingHeight(set);
                    if (mY >= sY && mY <= sY + h) {
                        if (set instanceof ColorSetting) {
                            ColorSetting color = (ColorSetting) set;
                            int barW = setW - 12 - 20;
                            int setStartX = setX + 6;

                            // Check rainbow button click
                            int previewX = setStartX + setW - 12 - 12;
                            int rbtnX = previewX - 18;
                            if (mX >= rbtnX && mX <= rbtnX + 14 && mY >= sY && mY <= sY + 12) {
                                color.toggleRainbow();
                                return;
                            }

                            // Otherwise start dragging color slider
                            draggedSetting = set;
                            focusedTextSetting = null;
                            return;
                        } else if (set instanceof TickSetting) {
                            ((TickSetting) set).toggle();
                            debugMsg("[GUI Debug] " + selectedModule.getName() + "." + set.getName() +
                                " toggled to " + ((TickSetting) set).isEnabled());
                            focusedTextSetting = null; // Unfocus text
                            return;
                        } else if (set instanceof SliderSetting || set instanceof DoubleSliderSetting) {
                            draggedSetting = set;
                            focusedTextSetting = null; // Unfocus text
                            return;
                        } else if (set instanceof ModeSetting) {
                            ModeSetting mode = (ModeSetting) set;
                            mode.cycle();
                            debugMsg("[GUI Debug] " + selectedModule.getName() + "." + set.getName() +
                                " mode changed to " + mode.getSelected());
                            focusedTextSetting = null; // Unfocus text
                            return;
                        } else if (set instanceof TextSetting) {
                            focusedTextSetting = (TextSetting) set;
                            textCursorPos = focusedTextSetting.getValue().length(); // Cursor at end
                            return;
                        } else if (set instanceof KeybindSetting) {
                            KeybindSetting keybind = (KeybindSetting) set;
                            // Check if clicking on the button area
                            int kbBtnW = 60;
                            int kbBtnX = setX + setW - 6 - kbBtnW;
                            if (mX >= kbBtnX && mX <= kbBtnX + kbBtnW) {
                                if (btn == 1) {
                                    // Right click - clear keybind
                                    keybind.clearKey();
                                } else {
                                    // Left click - start waiting for key
                                    keybind.startWaiting();
                                    waitingKeybindSetting = keybind;
                                }
                            }
                            focusedTextSetting = null;
                            return;
                        }
                    }
                    sY += h;
                }
            }
        }

        // Settings Grid clicks
        if (selectedCategory == ModuleCategory.SETTINGS) {
            int panelX = x + 4;
            int panelY = y + 62;
            int panelW = width - 8;
            int panelH = height - 67;

            // Grid tab clicks (2x2)
            int gridTabW = panelW / 2;
            int gridTabH = 30;
            for (int i = 0; i < 4; i++) {
                int gridX = i % 2;
                int gridY = i / 2;
                int gridTabX = panelX + (gridX * gridTabW);
                int gridTabY = panelY + (gridY * gridTabH);

                if (mX >= gridTabX && mX <= gridTabX + gridTabW && mY >= gridTabY && mY <= gridTabY + gridTabH) {
                    selectedSettingsTab = i;
                    return;
                }
            }

            // Content area clicks based on selected tab
            if (selectedSettingsTab == 1) {
                // HUD tab clicks
                int contentY = panelY + gridTabH * 2;
                int contentH = panelH - gridTabH * 2;

                // Get HUD module and handle setting clicks
                Module hudModule = moduleManager.getModuleByName("HUD");
                if (hudModule != null) {
                    List<Setting> settings = hudModule.getSettings();
                    int sY = contentY + 28;
                    int settingW = panelW - 20;

                    for (Setting set : settings) {
                        if (set instanceof DescriptionSetting) {
                            sY += 14;
                            continue;
                        }
                        if (!set.isVisible()) continue;
                        if (sY > contentY + contentH - 20) break;

                        int h = getSettingHeight(set);
                        if (mY >= sY && mY <= sY + h) {
                            if (set instanceof ColorSetting) {
                                // Start dragging color
                                draggedSetting = set;
                                debugMsg("[GUI Debug] HUD." + set.getName() + " color editing started");
                                focusedTextSetting = null;
                                return;
                            } else if (set instanceof TickSetting) {
                                ((TickSetting) set).toggle();
                                debugMsg("[GUI Debug] HUD." + set.getName() +
                                    " toggled to " + ((TickSetting) set).isEnabled());
                                focusedTextSetting = null;
                                return;
                            } else if (set instanceof SliderSetting || set instanceof DoubleSliderSetting) {
                                draggedSetting = set;
                                focusedTextSetting = null;
                                return;
                            } else if (set instanceof ModeSetting) {
                                ((ModeSetting) set).cycle();
                                debugMsg("[GUI Debug] HUD." + set.getName() +
                                    " mode changed to " + ((ModeSetting) set).getSelected());
                                focusedTextSetting = null;
                                return;
                            } else if (set instanceof TextSetting) {
                                focusedTextSetting = (TextSetting) set;
                                textCursorPos = focusedTextSetting.getValue().length(); // Cursor at end
                                debugMsg("[GUI Debug] HUD." + set.getName() + " focused for editing");
                                return;
                            } else if (set instanceof KeybindSetting) {
                                KeybindSetting keybind = (KeybindSetting) set;
                                // Check if clicking on the button area
                                int kbBtnW = 60;
                                int kbBtnX = panelX + panelW - 10 - kbBtnW;
                                if (mX >= kbBtnX && mX <= kbBtnX + kbBtnW) {
                                    if (btn == 1) {
                                        // Right click - clear keybind
                                        keybind.clearKey();
                                    } else {
                                        // Left click - start waiting for key
                                        keybind.startWaiting();
                                        waitingKeybindSetting = keybind;
                                    }
                                }
                                focusedTextSetting = null;
                                return;
                            }
                        }
                        sY += h;
                    }
                }
                return;
            }

            // Friends tab clicks
            if (selectedSettingsTab == 2) {
                int friendContentY = panelY + gridTabH * 2;
                int friendContentH = panelH - gridTabH * 2;

                // Input field click
                int inputX = panelX + 6;
                int inputY = friendContentY + 22;
                int inputW = panelW - 60;
                int inputH = 16;

                if (mX >= inputX && mX <= inputX + inputW && mY >= inputY && mY <= inputY + inputH) {
                    friendInputFocused = true;
                    configInputFocused = false;
                    return;
                } else {
                    friendInputFocused = false;
                }

                // Add button click
                int addBtnX = inputX + inputW + 4;
                int addBtnW = panelW - inputW - 16;
                if (mX >= addBtnX && mX <= addBtnX + addBtnW && mY >= inputY && mY <= inputY + inputH) {
                    if (!friendNameInput.trim().isEmpty()) {
                        io.github.exodar.friend.FriendManager.getInstance().addFriend(friendNameInput.trim());
                        friendNameInput = "";
                    }
                    return;
                }

                // Friends list clicks
                int friendListY = friendContentY + 44;
                int entryH = 18;

                io.github.exodar.friend.FriendManager fm = io.github.exodar.friend.FriendManager.getInstance();
                java.util.List<String> friends = fm.getFriendsList();

                int yOff = friendListY - friendsScroll;
                for (String friend : friends) {
                    if (yOff + entryH < friendListY) {
                        yOff += entryH;
                        continue;
                    }
                    if (yOff > friendContentY + friendContentH) break;

                    // Check X button click
                    int xBtnX = panelX + panelW - 24;
                    if (mX >= xBtnX && mX <= xBtnX + 14 && mY >= yOff + 2 && mY <= yOff + entryH - 4) {
                        fm.removeFriend(friend);
                        return;
                    }

                    yOff += entryH;
                }
                return;
            }

            // Reset tab clicks
            if (selectedSettingsTab == 3) {
                int resetContentY = panelY + gridTabH * 2;
                int resetContentH = panelH - gridTabH * 2;

                int btnW = 200;
                int btnH = 24;
                int btnX = panelX + (panelW - btnW) / 2;

                // Button 1: Unhide All Modules
                int btn1Y = resetContentY + 50;
                if (mX >= btnX && mX <= btnX + btnW && mY >= btn1Y && mY <= btn1Y + btnH) {
                    UserConfig.getInstance().unhideAllModules();
                    io.github.exodar.ui.ModuleNotification.addNotification("All modules unhidden", true);
                    return;
                }

                // Button 2: Clear All Keybinds
                int btn2Y = resetContentY + 80;
                if (mX >= btnX && mX <= btnX + btnW && mY >= btn2Y && mY <= btn2Y + btnH) {
                    for (Module mod : moduleManager.getModules()) {
                        mod.setToggleBind(0);
                        mod.setHoldBind(0);
                    }
                    io.github.exodar.ui.ModuleNotification.addNotification("All keybinds cleared", true);
                    return;
                }

                // Button 3: Toggle Invert Clicks
                int btn3Y = resetContentY + 110;
                if (mX >= btnX && mX <= btnX + btnW && mY >= btn3Y && mY <= btn3Y + btnH) {
                    // Toggle the invert clicks setting in HUD module
                    Module hudModule = moduleManager.getModuleByName("HUD");
                    if (hudModule != null) {
                        Setting invertSetting = hudModule.getSettingByName("Invert Menu Clicks");
                        if (invertSetting instanceof TickSetting) {
                            ((TickSetting) invertSetting).toggle();
                        }
                    }
                    // Also toggle directly in config for immediate effect
                    ArrayListConfig.invertMenuClicks = !ArrayListConfig.invertMenuClicks;
                    io.github.exodar.ui.ModuleNotification.addNotification("Invert Clicks: " + (ArrayListConfig.invertMenuClicks ? "ON" : "OFF"), true);
                    return;
                }

                // Button 4: Delete ALL Configs
                int btn4Y = resetContentY + 150;
                if (mX >= btnX && mX <= btnX + btnW && mY >= btn4Y && mY <= btn4Y + btnH) {
                    // Delete config directory contents (use ConfigManager path)
                    try {
                        io.github.exodar.config.ConfigManager cm = io.github.exodar.Main.getConfigManager();
                        String configDir = cm != null ? cm.getConfigDirectory() : null;
                        if (configDir == null) return;
                        java.io.File dir = new java.io.File(configDir);
                        if (dir.exists() && dir.isDirectory()) {
                            java.io.File[] files = dir.listFiles();
                            if (files != null) {
                                for (java.io.File file : files) {
                                    if (file.isFile()) {
                                        file.delete();
                                    }
                                }
                            }
                        }
                        io.github.exodar.ui.ModuleNotification.addNotification("All configs deleted!", false);
                    } catch (Exception e) {
                        io.github.exodar.ui.ModuleNotification.addNotification("Error deleting configs", false);
                    }
                    return;
                }

                // Button 5: Open Config Folder
                int btn5Y = resetContentY + 190;
                if (mX >= btnX && mX <= btnX + btnW && mY >= btn5Y && mY <= btn5Y + btnH) {
                    io.github.exodar.config.ConfigManager cm = io.github.exodar.Main.getConfigManager();
                    if (cm != null) {
                        cm.openConfigFolder();
                        io.github.exodar.ui.ModuleNotification.addNotification("Opening config folder...", true);
                    }
                    return;
                }
                return;
            }

            if (selectedSettingsTab != 0) {
                return; // Other tabs don't have additional clickable content
            }

            // Config panel content (adjust for content area)
            int contentY = panelY + gridTabH * 2;
            int contentH = panelH - gridTabH * 2;

            // Local/Cloud tab clicks
            int cfgTabY = contentY + 4;
            int cfgTabW = (panelW - 16) / 2;
            int cfgTabH = 18;
            int localTabX = panelX + 6;
            int cloudTabX = localTabX + cfgTabW + 4;

            if (mY >= cfgTabY && mY <= cfgTabY + cfgTabH) {
                if (mX >= localTabX && mX <= localTabX + cfgTabW) {
                    cloudConfigMode = false;
                    selectedCloudConfig = null;
                    return;
                }
                if (mX >= cloudTabX && mX <= cloudTabX + cfgTabW) {
                    cloudConfigMode = true;
                    selectedConfig = null;
                    // Fetch configs if needed
                    if (CloudConfigManager.isAuthenticated() && CloudConfigManager.needsRefresh()) {
                        cloudConfigsFetching = true;
                        CloudConfigManager.fetchConfigs().thenAccept(success -> {
                            cloudConfigsFetching = false;
                            cloudConfigStatus = success ? "" : "Fetch failed";
                        });
                    }
                    return;
                }
            }

            int actualContentY = cfgTabY + cfgTabH + 4;
            // Calculate height to match drawConfigPanel: contentH = panelH - (cfgTabH + 8)
            int actualContentH = contentH - cfgTabH - 8;

            if (!cloudConfigMode) {
                // ===== LOCAL CONFIG MODE CLICKS =====
                handleLocalConfigClicks(mX, mY, panelX, panelW, actualContentY, actualContentH);
            } else {
                // ===== CLOUD CONFIG MODE CLICKS =====
                handleCloudConfigClicks(mX, mY, panelX, panelW, actualContentY, actualContentH);
            }
        }

        try {
            super.mouseClicked(mX, mY, btn);
        } catch (Exception e) {}
    }

    @Override
    protected void mouseReleased(int mX, int mY, int state) {
        // Debug: print when setting is changed
        if (draggedSetting != null) {
            String moduleName = selectedModule != null ? selectedModule.getName() : "unknown";
            if (draggedSetting instanceof ColorSetting) {
                ColorSetting c = (ColorSetting) draggedSetting;
                debugMsg("[GUI Debug] " + moduleName + "." + draggedSetting.getName() +
                    " color changed to RGB(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")");
            } else if (draggedSetting instanceof SliderSetting) {
                SliderSetting s = (SliderSetting) draggedSetting;
                debugMsg("[GUI Debug] " + moduleName + "." + draggedSetting.getName() +
                    " slider changed to " + s.getValue());
            }
        }

        dragging = false;
        draggedSetting = null;
        colorPickerDragMode = 0;  // Reset color picker lock
        super.mouseReleased(mX, mY, state);
    }

    @Override
    public void handleMouseInput() {
        // Handle mouse button binds (skip btn 0 = left click)
        if (waitingForBind != null && waitingForBindMode != null) {
            for (int btn = 1; btn < 5; btn++) {
                if (Mouse.isButtonDown(btn)) {
                    if (waitingForBindMode == Module.BindMode.TOGGLE) {
                        waitingForBind.setToggleBind(-100 + btn);
                    } else {
                        waitingForBind.setHoldBind(-100 + btn);
                    }
                    waitingForBind = null;
                    waitingForBindMode = null;
                    return;
                }
            }
        }

        try {
            super.handleMouseInput();
        } catch (Exception e) {}

        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mX = Mouse.getX();
            int mY = Mouse.getY();

            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = ScaledResolutionHelper.create(mc);
            mX = mX * sr.getScaledWidth() / Display.getWidth();
            mY = sr.getScaledHeight() - mY * sr.getScaledHeight() / Display.getHeight() - 1;

            // Mouse wheel on slider: adjust by 0.01
            if (hoveredSlider != null) {
                double step = 0.01;
                double interval = hoveredSlider.getInterval();
                if (interval > step) step = interval;  // Use interval if larger

                double newVal = hoveredSlider.getValue() + (wheel > 0 ? step : -step);
                newVal = Math.max(hoveredSlider.getMin(), Math.min(hoveredSlider.getMax(), newVal));
                // Round to interval
                newVal = Math.round(newVal / interval) * interval;
                hoveredSlider.setValue(newVal);
                return;  // Don't scroll the panel
            }

            int listX = x + 4;
            int listY = y + 62;
            int listW = 140;
            int listH = height - 67;

            // Scroll en módulos
            if (mX >= listX && mX <= listX + listW && mY >= listY && mY <= listY + listH) {
                moduleScroll -= wheel > 0 ? 15 : -15;

                // Calculate max scroll for modules
                List<Module> modules = getModulesForCategory(selectedCategory);
                int totalModuleHeight = modules.size() * 18;
                int maxModuleScroll = Math.max(0, totalModuleHeight - listH + 10);

                moduleScroll = Math.max(0, Math.min(moduleScroll, maxModuleScroll));
            }

            // Scroll en settings
            int setX = x + 150;
            int setY = y + 62;
            int setW = width - 156;
            int setH = height - 67;

            if (selectedModule != null && mX >= setX && mX <= setX + setW && mY >= setY && mY <= setY + setH) {
                settingScroll -= wheel > 0 ? 15 : -15;

                // Calculate max scroll for settings
                int totalSettingHeight = 0;
                for (Setting set : selectedModule.getSettings()) {
                    if (set instanceof DescriptionSetting || !set.isVisible()) continue;
                    totalSettingHeight += getSettingHeight(set);
                }
                int maxSettingScroll = Math.max(0, totalSettingHeight - setH + 30);

                settingScroll = Math.max(0, Math.min(settingScroll, maxSettingScroll));
            }

            // Scroll in Friends panel (Settings > Friends tab)
            if (selectedCategory == ModuleCategory.SETTINGS && selectedSettingsTab == 2) {
                int panelX = x + 4;
                int panelY = y + 62;
                int panelW = width - 8;
                int panelH = height - 67;
                int gridTabH = 30;
                int contentY = panelY + gridTabH * 2;
                int contentH = panelH - gridTabH * 2;

                if (mX >= panelX && mX <= panelX + panelW && mY >= contentY && mY <= contentY + contentH) {
                    friendsScroll -= wheel > 0 ? 15 : -15;

                    // Calculate max scroll for friends
                    io.github.exodar.friend.FriendManager fm = io.github.exodar.friend.FriendManager.getInstance();
                    int totalFriendsHeight = fm.getFriendCount() * 18;
                    int maxFriendsScroll = Math.max(0, totalFriendsHeight - contentH + 30);

                    friendsScroll = Math.max(0, Math.min(friendsScroll, maxFriendsScroll));
                }
            }

            // Scroll in Config panel (Settings > Config tab)
            if (selectedCategory == ModuleCategory.SETTINGS && selectedSettingsTab == 0) {
                int panelX = x + 4;
                int panelY = y + 62;
                int panelW = width - 8;
                int panelH = height - 67;
                int gridTabH = 30;
                int contentY = panelY + gridTabH * 2;
                int contentH = panelH - gridTabH * 2;

                if (mX >= panelX && mX <= panelX + panelW && mY >= contentY && mY <= contentY + contentH) {
                    if (!cloudConfigMode) {
                        // Local config scroll
                        configScroll -= wheel > 0 ? 15 : -15;
                        io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                        int totalConfigsHeight = 0;
                        if (configManager != null) {
                            totalConfigsHeight = configManager.getConfigs().size() * 20;
                        }
                        // Account for: folder button (18), buttons (26), gaps (12), input area
                        int configListH = contentH - 90;
                        int maxConfigScroll = Math.max(0, totalConfigsHeight - configListH + 20);
                        configScroll = Math.max(0, Math.min(configScroll, maxConfigScroll));
                    } else {
                        // Cloud config scroll
                        cloudConfigScroll -= wheel > 0 ? 15 : -15;
                        java.util.List<CloudConfigManager.CloudConfig> configs;
                        if (cloudConfigTab == 0) {
                            configs = CloudConfigManager.getOfficialConfigs();
                        } else if (cloudConfigTab == 1) {
                            configs = CloudConfigManager.getPublicConfigs();
                        } else {
                            configs = CloudConfigManager.getOwnConfigs();
                        }
                        int totalCloudConfigsHeight = configs.size() * 24;
                        int cloudListH = contentH - 90;
                        int maxCloudScroll = Math.max(0, totalCloudConfigsHeight - cloudListH + 20);
                        cloudConfigScroll = Math.max(0, Math.min(cloudConfigScroll, maxCloudScroll));
                    }
                }
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) {
        // Handle KeybindSetting waiting
        if (waitingKeybindSetting != null) {
            if (key == Keyboard.KEY_ESCAPE) {
                // ESC cancels and clears
                waitingKeybindSetting.clearKey();
                waitingKeybindSetting = null;
            } else if (key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK) {
                // Delete/Backspace clears keybind
                waitingKeybindSetting.clearKey();
                waitingKeybindSetting = null;
            } else {
                // Set the key
                waitingKeybindSetting.setKeyCode(key);
                waitingKeybindSetting = null;
            }
            return;
        }

        // Handle keybind waiting
        if (waitingForBind != null && waitingForBindMode != null) {
            if (key == Keyboard.KEY_ESCAPE) {
                waitingForBind = null;
                waitingForBindMode = null;
            } else if (key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK) {
                waitingForBind.setToggleBind(0);
                waitingForBind.setHoldBind(0);
                waitingForBind = null;
                waitingForBindMode = null;
            } else {
                if (waitingForBindMode == Module.BindMode.TOGGLE) {
                    waitingForBind.setToggleBind(key);
                } else {
                    waitingForBind.setHoldBind(key);
                }
                waitingForBind = null;
                waitingForBindMode = null;
            }
            return;
        }

        // Handle TextSetting input
        if (focusedTextSetting != null) {
            String current = focusedTextSetting.getValue();

            if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN) {
                String moduleName = selectedModule != null ? selectedModule.getName() : "unknown";
                debugMsg("[GUI Debug] " + moduleName + "." + focusedTextSetting.getName() +
                    " text changed to \"" + focusedTextSetting.getValue() + "\"");
                focusedTextSetting = null;
                backspaceHeld = false;
                return;
            } else if (key == Keyboard.KEY_LEFT) {
                // Move cursor left
                if (textCursorPos > 0) textCursorPos--;
                return;
            } else if (key == Keyboard.KEY_RIGHT) {
                // Move cursor right
                if (textCursorPos < current.length()) textCursorPos++;
                return;
            } else if (key == Keyboard.KEY_HOME) {
                textCursorPos = 0;
                return;
            } else if (key == Keyboard.KEY_END) {
                textCursorPos = current.length();
                return;
            } else if (key == Keyboard.KEY_DELETE) {
                // Delete character after cursor
                if (textCursorPos < current.length()) {
                    String newVal = current.substring(0, textCursorPos) + current.substring(textCursorPos + 1);
                    focusedTextSetting.setValue(newVal);
                }
                return;
            } else if (key == Keyboard.KEY_BACK) {
                // Backspace - delete character before cursor
                if (textCursorPos > 0 && current.length() > 0) {
                    String newVal = current.substring(0, textCursorPos - 1) + current.substring(textCursorPos);
                    focusedTextSetting.setValue(newVal);
                    textCursorPos--;
                }
                backspaceHeld = true;
                lastBackspaceTime = System.currentTimeMillis();
                return;
            } else if (c >= 32 && c < 127 && current.length() < focusedTextSetting.getMaxLength()) {
                // Insert character at cursor position
                String newVal = current.substring(0, textCursorPos) + c + current.substring(textCursorPos);
                focusedTextSetting.setValue(newVal);
                textCursorPos++;
                return;
            }
            return; // Consume all keys when focused
        }

        // Handle config name input
        if (selectedCategory == ModuleCategory.SETTINGS && configInputFocused) {
            if (key == Keyboard.KEY_BACK) {
                if (configNameInput.length() > 0) {
                    configNameInput = configNameInput.substring(0, configNameInput.length() - 1);
                }
                return;
            } else if (key == Keyboard.KEY_RETURN) {
                // Enter key - create config
                if (!configNameInput.isEmpty()) {
                    io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                    if (configManager != null && !configManager.configExists(configNameInput)) {
                        configManager.saveConfig(configNameInput);
                        selectedConfig = configNameInput;
                        configNameInput = "";
                        configInputFocused = false;
                    }
                }
                return;
            } else if (c >= 32 && c < 127 && configNameInput.length() < 20) {
                configNameInput += c;
                return;
            }
        }

        // Handle cloud import code input
        if (selectedCategory == ModuleCategory.SETTINGS && cloudImportFocused) {
            if (key == Keyboard.KEY_BACK) {
                if (cloudImportCode.length() > 0) {
                    cloudImportCode = cloudImportCode.substring(0, cloudImportCode.length() - 1);
                }
                return;
            } else if (key == Keyboard.KEY_RETURN) {
                // Enter key - import config
                if (!cloudImportCode.isEmpty() && cloudImportCode.length() >= 6) {
                    final String codeToImport = cloudImportCode.toUpperCase().trim();
                    cloudConfigStatus = "Importing...";
                    cloudImportFocused = false;
                    CloudConfigManager.downloadConfig(codeToImport).thenAccept(result -> {
                        if (result != null) {
                            String configName = result[0];
                            String configData = result[1];
                            String username = result[2];
                            io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                            if (configManager != null) {
                                String localName = configName;
                                int counter = 1;
                                while (configManager.configExists(localName)) {
                                    localName = configName + "_" + counter++;
                                }
                                configManager.saveConfigData(localName, configData);
                                configManager.loadConfig(localName);
                                io.github.exodar.ui.ModuleNotification.addNotification("Imported: " + localName + " by " + username, true);
                                cloudConfigStatus = "";
                                cloudImportCode = "";
                            }
                        } else {
                            cloudConfigStatus = "Import failed";
                        }
                    });
                }
                return;
            } else if (key == Keyboard.KEY_ESCAPE) {
                cloudImportFocused = false;
                return;
            } else if (c >= 32 && c < 127 && cloudImportCode.length() < 10) {
                // Config codes are typically 8 chars, allow up to 10
                cloudImportCode += Character.toUpperCase(c);
                return;
            }
        }

        // Handle friend name input
        if (selectedCategory == ModuleCategory.SETTINGS && friendInputFocused) {
            if (key == Keyboard.KEY_BACK) {
                if (friendNameInput.length() > 0) {
                    friendNameInput = friendNameInput.substring(0, friendNameInput.length() - 1);
                }
                return;
            } else if (key == Keyboard.KEY_RETURN) {
                // Enter key - add friend
                if (!friendNameInput.trim().isEmpty()) {
                    io.github.exodar.friend.FriendManager.getInstance().addFriend(friendNameInput.trim());
                    friendNameInput = "";
                    friendInputFocused = false;
                }
                return;
            } else if (key == Keyboard.KEY_ESCAPE) {
                friendInputFocused = false;
                friendNameInput = "";
                return;
            } else if (c >= 32 && c < 127 && friendNameInput.length() < 16) {
                friendNameInput += c;
                return;
            }
        }

        // Arrow keys to adjust hovered slider
        if (hoveredSlider != null) {
            double step = hoveredSlider.getInterval();
            double bigStep = step * 10;  // Shift for bigger steps

            if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_DOWN) {
                double newVal = hoveredSlider.getValue() - step;
                newVal = Math.max(hoveredSlider.getMin(), Math.min(hoveredSlider.getMax(), newVal));
                newVal = Math.round(newVal / step) * step;
                hoveredSlider.setValue(newVal);
                return;
            } else if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_UP) {
                double newVal = hoveredSlider.getValue() + step;
                newVal = Math.max(hoveredSlider.getMin(), Math.min(hoveredSlider.getMax(), newVal));
                newVal = Math.round(newVal / step) * step;
                hoveredSlider.setValue(newVal);
                return;
            }
        }

        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_INSERT) {
            onClose(); // Set cooldown
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        try {
            super.keyTyped(c, key);
        } catch (Exception e) {}
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        // Handle continuous backspace for TextSetting
        if (focusedTextSetting != null && backspaceHeld && Keyboard.isKeyDown(Keyboard.KEY_BACK)) {
            long now = System.currentTimeMillis();
            // Start repeating after 400ms, then every 50ms
            if (now - lastBackspaceTime > (lastBackspaceTime == 0 ? 400 : 50)) {
                String current = focusedTextSetting.getValue();
                if (textCursorPos > 0 && current.length() > 0) {
                    String newVal = current.substring(0, textCursorPos - 1) + current.substring(textCursorPos);
                    focusedTextSetting.setValue(newVal);
                    textCursorPos--;
                }
                lastBackspaceTime = now;
            }
        } else {
            backspaceHeld = false;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
