/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.config.UserConfig;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import io.github.exodar.ui.ArrayListColorMode;
import io.github.exodar.ui.ArrayListConfig;

/**
 * Settings module para configurar ArrayList y Watermark
 */
public class Settings extends Module {
    // HUD - ArrayList
    private DescriptionSetting hudSection;
    private ModeSetting arrayListColorMode;
    private TickSetting arrayListSlideAnimation;
    private SliderSetting arrayListSpeed;

    // HUD - Watermark
    private DescriptionSetting watermarkSection;
    private TickSetting watermarkRainbow;
    private TickSetting watermarkEnabled;

    // Menu settings
    private DescriptionSetting menuSection;
    private TickSetting invertMenuClicks;
    private KeybindSetting menuKeybind;

    public Settings() {
        super("HUD", ModuleCategory.HUD);

        // === HUD SECTION ===
        this.registerSetting(hudSection = new DescriptionSetting("=== HUD SETTINGS ==="));

        // ArrayList settings
        this.registerSetting(new DescriptionSetting("ArrayList:"));
        this.registerSetting(arrayListColorMode = new ModeSetting("Color Mode",
            new String[]{"Rainbow", "Rainbow Pastel", "Transgender", "Color Fade", "White", "Purple"}));
        this.registerSetting(arrayListSlideAnimation = new TickSetting("Slide Animation", true));
        this.registerSetting(arrayListSpeed = new SliderSetting("Animation Speed", 0.3, 0.1, 1.0, 0.05));

        // Watermark settings
        this.registerSetting(watermarkSection = new DescriptionSetting("Watermark:"));
        this.registerSetting(watermarkEnabled = new TickSetting("Show Watermark", true));
        this.registerSetting(watermarkRainbow = new TickSetting("Rainbow Letter", true));

        // Menu settings
        this.registerSetting(menuSection = new DescriptionSetting("Menu:"));
        this.registerSetting(menuKeybind = new KeybindSetting("Menu Keybind", 210)); // 210 = INSERT
        this.registerSetting(invertMenuClicks = new TickSetting("Invert Menu Clicks", false));

        // Load saved preferences
        loadPreferences();

        // Set initial values
        updateConfig();
    }

    /**
     * Load preferences from UserConfig
     */
    private void loadPreferences() {
        try {
            UserConfig config = UserConfig.getInstance();
            String invertClicks = config.getPreference("invert_menu_clicks", "false");
            invertMenuClicks.setEnabled(invertClicks.equals("true"));

            String menuKey = config.getPreference("menu_keybind", "210");
            try {
                menuKeybind.setKeyCode(Integer.parseInt(menuKey));
            } catch (NumberFormatException e) {
                menuKeybind.setKeyCode(210); // Default to INSERT
            }
        } catch (Exception e) {
            System.out.println("[Settings] Error loading preferences: " + e.getMessage());
        }
    }

    /**
     * Save preferences to UserConfig
     */
    private void savePreferences() {
        try {
            UserConfig config = UserConfig.getInstance();
            config.setPreference("invert_menu_clicks", invertMenuClicks.isEnabled() ? "true" : "false");
            config.setPreference("menu_keybind", String.valueOf(menuKeybind.getKeyCode()));
        } catch (Exception e) {
            System.out.println("[Settings] Error saving preferences: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        System.out.println("[Settings] Enabled");
        updateConfig();
    }

    @Override
    public void onDisable() {
        System.out.println("[Settings] Disabled");
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        updateConfig();
    }

    /**
     * Update ArrayListConfig based on settings
     */
    private void updateConfig() {
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
            case "White":
                ArrayListConfig.colorMode = ArrayListColorMode.WHITE;
                break;
        }

        // Update ArrayList animation
        ArrayListConfig.slideAnimation = arrayListSlideAnimation.isEnabled();

        // Update watermark
        ArrayListConfig.watermarkRainbow = watermarkRainbow.isEnabled();

        // Update menu settings
        ArrayListConfig.invertMenuClicks = invertMenuClicks.isEnabled();
        ArrayListConfig.menuKeybind = menuKeybind.getKeyCode();
        savePreferences();
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + arrayListColorMode.getSelected();
    }
}
