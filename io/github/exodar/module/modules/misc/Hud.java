/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import io.github.exodar.ui.ArrayListColorMode;
import io.github.exodar.ui.ArrayListConfig;
import io.github.exodar.ui.ClientSkin;
import io.github.exodar.setting.TextSetting;

/**
 * HUD module para configurar ArrayList, Watermark y Notifications
 */
public class Hud extends Module {
    // HUD - ArrayList
    private TickSetting arrayListEnabled;
    private ModeSetting arrayListColorMode;
    private TickSetting arrayListSlideAnimation;

    // HUD - Watermark
    private TickSetting watermarkEnabled;
    private TextSetting clientName;
    private TickSetting watermarkRainbow;

    // HUD - Build Version
    private TickSetting buildVersionEnabled;

    // HUD - Notifications
    private TickSetting notificationsEnabled;

    // Menu settings
    private TickSetting invertMenuClicks;
    private KeybindSetting menuKeybind;

    // Exodar Theme (Rise-style)
    private TickSetting exodarThemeEnabled;
    private TickSetting exodarBackground;
    private TickSetting exodarSidebar;
    private TickSetting exodarGlow;
    private TickSetting exodarGlowShadow;
    private SliderSetting exodarBackgroundAlpha;

    public Hud() {
        super("HUD", ModuleCategory.SETTINGS);

        // ArrayList settings - with onChange callbacks for immediate updates
        this.registerSetting(arrayListEnabled = new TickSetting("ArrayList", true).onChange(this::updateConfig));
        this.registerSetting(arrayListColorMode = new ModeSetting("Color Mode",
            new String[]{"Rainbow", "Rainbow Pastel", "Transgender", "Color Fade", "White", "Purple", "Toxic", "Pink"}).onChange(this::updateConfig));
        this.registerSetting(arrayListSlideAnimation = new TickSetting("Slide Animation", true).onChange(this::updateConfig));

        // Watermark settings
        this.registerSetting(watermarkEnabled = new TickSetting("Watermark", true).onChange(this::updateConfig));
        this.registerSetting(clientName = new TextSetting("Client Name", "Exodar", 16).onChange(this::updateConfig));
        this.registerSetting(watermarkRainbow = new TickSetting("Rainbow Letter", true).onChange(this::updateConfig));

        // Build version
        this.registerSetting(buildVersionEnabled = new TickSetting("Build Version", true).onChange(this::updateConfig));

        // Notifications
        this.registerSetting(notificationsEnabled = new TickSetting("Notifications", true).onChange(this::updateConfig));

        // Menu settings
        this.registerSetting(invertMenuClicks = new TickSetting("Invert Menu Clicks", false).onChange(this::updateConfig));
        this.registerSetting(menuKeybind = new KeybindSetting("Menu Key", 210)); // Default: INSERT

        // Exodar Theme (Rise-style ArrayList)
        this.registerSetting(exodarThemeEnabled = new TickSetting("Exodar Theme", false).onChange(this::updateConfig));
        this.registerSetting(exodarBackground = new TickSetting("Theme Background", true).onChange(this::updateConfig));
        this.registerSetting(exodarSidebar = new TickSetting("Theme Sidebar", true).onChange(this::updateConfig));
        this.registerSetting(exodarGlow = new TickSetting("Theme Glow", true).onChange(this::updateConfig));
        this.registerSetting(exodarGlowShadow = new TickSetting("Glow Shadow", false).onChange(this::updateConfig));
        this.registerSetting(exodarBackgroundAlpha = new SliderSetting("Background Alpha", 100, 0, 255, 5).onChange(this::updateConfig));

        // Set initial values
        updateConfig();

        // HUD always enabled by default
        this.setEnabled(true);
    }

    @Override
    public void onEnable() {
        // System.out.println("[HUD] Enabled");
        updateConfig();
    }

    @Override
    public void onDisable() {
        // System.out.println("[HUD] Disabled");
    }

    @Override
    public void onUpdate() {
        // Update config each tick to reflect setting changes from GUI
        // This is lightweight (just boolean/enum assignments)
        updateConfig();
    }

    /**
     * Update ArrayListConfig based on settings
     */
    private void updateConfig() {
        // Update ArrayList enabled
        ArrayListConfig.arrayListEnabled = arrayListEnabled.isEnabled();

        // Update ArrayList color mode
        String mode = arrayListColorMode.getSelected();
        switch (mode) {
            case "Rainbow":
                ArrayListConfig.colorMode = ArrayListColorMode.RAINBOW;
                break;
            case "Rainbow Pastel":
                ArrayListConfig.colorMode = ArrayListColorMode.RAINBOW_PASTEL;
                break;
            case "Transgender":
                ArrayListConfig.colorMode = ArrayListColorMode.TRANSGENDER;
                break;
            case "Color Fade":
                ArrayListConfig.colorMode = ArrayListColorMode.COLOR_FADE;
                break;
            case "Purple":
                ArrayListConfig.colorMode = ArrayListColorMode.PURPLE;
                break;
            case "Toxic":
                ArrayListConfig.colorMode = ArrayListColorMode.TOXIC;
                break;
            case "Pink":
                ArrayListConfig.colorMode = ArrayListColorMode.PINK;
                break;
            case "White":
                ArrayListConfig.colorMode = ArrayListColorMode.WHITE;
                break;
        }

        // Update ArrayList animation
        ArrayListConfig.slideAnimation = arrayListSlideAnimation.isEnabled();

        // Update watermark
        ArrayListConfig.watermarkEnabled = watermarkEnabled.isEnabled();
        ArrayListConfig.watermarkText = clientName.getValue();
        ArrayListConfig.clientName = clientName.getValue();
        ArrayListConfig.watermarkRainbow = watermarkRainbow.isEnabled();

        // Update Easter egg skin based on client name
        ClientSkin.updateSkin(clientName.getValue());

        // Update build version
        ArrayListConfig.buildWatermarkEnabled = buildVersionEnabled.isEnabled();

        // Update notifications
        ArrayListConfig.notificationsEnabled = notificationsEnabled.isEnabled();

        // Update menu settings
        ArrayListConfig.invertMenuClicks = invertMenuClicks.isEnabled();
        ArrayListConfig.menuKeybind = menuKeybind.getKeyCode();

        // Update Exodar Theme settings
        ArrayListConfig.exodarThemeEnabled = exodarThemeEnabled.isEnabled();
        ArrayListConfig.exodarBackground = exodarBackground.isEnabled();
        ArrayListConfig.exodarSidebar = exodarSidebar.isEnabled();
        ArrayListConfig.exodarGlow = exodarGlow.isEnabled();
        ArrayListConfig.exodarGlowShadow = exodarGlowShadow.isEnabled();
        ArrayListConfig.exodarBackgroundAlpha = (int) exodarBackgroundAlpha.getValue();

        // Update visibility of theme sub-settings
        exodarBackground.setVisible(exodarThemeEnabled.isEnabled());
        exodarSidebar.setVisible(exodarThemeEnabled.isEnabled());
        exodarGlow.setVisible(exodarThemeEnabled.isEnabled());
        exodarGlowShadow.setVisible(exodarThemeEnabled.isEnabled() && exodarGlow.isEnabled());
        exodarBackgroundAlpha.setVisible(exodarThemeEnabled.isEnabled() && exodarBackground.isEnabled());
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + arrayListColorMode.getSelected();
    }
}
