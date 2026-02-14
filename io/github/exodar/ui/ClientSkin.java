/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import io.github.exodar.module.ModuleCategory;

/**
 * Easter Egg System - Changes HUD style based on client name
 *
 * Supported skins:
 * - "Huzuni" -> Huzuni style (white text, pastel colors, left ArrayList)
 * - "Wurst" -> Wurst style (orange theme)
 * - Default -> Normal Exodar style
 */
public class ClientSkin {

    // Current active skin
    private static SkinType currentSkin = SkinType.DEFAULT;
    private static String lastClientName = "";

    public enum SkinType {
        DEFAULT,
        HUZUNI,
        WURST,
        VIRTUE
    }

    /**
     * Update skin based on client name
     * Called from HUD module when name changes
     */
    public static void updateSkin(String clientName) {
        if (clientName == null) clientName = "";

        // Only update if name changed
        if (clientName.equals(lastClientName)) return;
        lastClientName = clientName;

        String nameLower = clientName.toLowerCase().trim();

        if (nameLower.contains("huzuni")) {
            currentSkin = SkinType.HUZUNI;
        } else if (nameLower.contains("wurst")) {
            currentSkin = SkinType.WURST;
        } else if (nameLower.contains("virtue")) {
            currentSkin = SkinType.VIRTUE;
        } else {
            currentSkin = SkinType.DEFAULT;
        }
    }

    /**
     * Get current skin type
     */
    public static SkinType getCurrentSkin() {
        return currentSkin;
    }

    /**
     * Check if Easter egg skin is active
     */
    public static boolean isEasterEggActive() {
        return currentSkin != SkinType.DEFAULT;
    }

    // ==================== ARRAYLIST POSITION ====================

    /**
     * Should ArrayList be on the left side?
     * Virtue stays on the right like default
     */
    public static boolean isArrayListOnLeft() {
        return currentSkin == SkinType.HUZUNI || currentSkin == SkinType.WURST;
    }

    // ==================== WATERMARK ====================

    /**
     * Get formatted watermark text
     */
    public static String getWatermarkText(String originalName) {
        switch (currentSkin) {
            case HUZUNI:
                return "Huzuni";
            case WURST:
                return "WURST";
            case VIRTUE:
                return "Virtue";
            default:
                return originalName;
        }
    }

    /**
     * Get watermark color (first letter)
     */
    public static int getWatermarkFirstColor() {
        switch (currentSkin) {
            case HUZUNI:
                return 0xFFFFFF; // White (Huzuni all white)
            case WURST:
                return 0xD4740F; // Orange (Wurst logo color)
            case VIRTUE:
                return 0xFFFFFF; // White
            default:
                return ArrayListConfig.getWatermarkFirstLetterColor();
        }
    }

    /**
     * Get watermark rest color
     */
    public static int getWatermarkRestColor() {
        switch (currentSkin) {
            case HUZUNI:
                return 0xFFFFFF; // White (all white for Huzuni)
            case WURST:
                return 0xD4740F; // Orange
            case VIRTUE:
                return 0xFFFFFF; // White
            default:
                return 0xFFFFFF; // White
        }
    }

    /**
     * Should use rainbow for watermark?
     */
    public static boolean useRainbowWatermark() {
        switch (currentSkin) {
            case HUZUNI:
            case WURST:
            case VIRTUE:
                return false; // Easter eggs use solid colors
            default:
                return ArrayListConfig.watermarkRainbow;
        }
    }

    // ==================== ARRAYLIST COLORS ====================

    /**
     * Get color for module in ArrayList
     * Huzuni uses pastel category-based colors
     * Wurst uses all white
     */
    public static int getModuleColor(ModuleCategory category, int yPos, int screenHeight) {
        switch (currentSkin) {
            case HUZUNI:
                return getHuzuniCategoryColor(category);
            case WURST:
                return 0xFFFFFF; // White for Wurst
            case VIRTUE:
                return 0xFFFFFF; // White for Virtue
            default:
                return ArrayListConfig.getColorForY(yPos, screenHeight);
        }
    }

    /**
     * Huzuni-style pastel category colors (very light)
     */
    private static int getHuzuniCategoryColor(ModuleCategory category) {
        if (category == null) return 0xFFFFFF;

        switch (category) {
            case COMBAT:
                return 0xFFAAAA; // Pastel red
            case PLAYER:
                return 0xFFFFAA; // Pastel yellow
            case MOVEMENT:
                return 0xAAFFAA; // Pastel green
            case VISUALS:
                return 0xAAFFFF; // Pastel cyan
            case MISC:
                return 0xDDDDDD; // Light gray
            case SETTINGS:
                return 0xFFAAFF; // Pastel magenta
            default:
                return 0xFFFFFF; // White
        }
    }

    // ==================== SUFFIX ====================

    /**
     * Get suffix color
     */
    public static int getSuffixColor() {
        switch (currentSkin) {
            case HUZUNI:
                return 0xCCCCCC; // Light gray suffix
            case WURST:
                return 0xAAAAAA; // Gray suffix
            case VIRTUE:
                return 0xAAAAAA; // Gray suffix
            default:
                return 0x888888; // Gray (§7)
        }
    }
}
