/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.ColorSetting;
import net.minecraft.entity.Entity;

/**
 * Hitbox - Render player hitboxes
 * Based on client-master implementation
 * Supports team colors when Teams module has Affect Visuals enabled
 */
public class Hitbox extends Module {

    private SliderSetting lineWidth;
    private ColorSetting color;

    public Hitbox() {
        super("Hitbox", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Render player hitboxes"));
        this.registerSetting(lineWidth = new SliderSetting("Line Width", 2.0, 1.0, 5.0, 0.5));
        this.registerSetting(color = new ColorSetting("Color", 255, 0, 0, 128, true));
    }

    public float getLineWidth() {
        return (float) lineWidth.getValue();
    }

    public float getRed() {
        return color.getRed() / 255.0f;
    }

    public float getGreen() {
        return color.getGreen() / 255.0f;
    }

    public float getBlue() {
        return color.getBlue() / 255.0f;
    }

    public float getAlpha() {
        return color.getAlpha() / 255.0f;
    }

    public int getColor() {
        return color.getColor();
    }

    /**
     * Get color for entity - uses team color from display name if Teams.affectVisuals is enabled and entity is teammate
     */
    public int getColorForEntity(Entity entity) {
        Teams teams = Teams.getInstance();
        if (teams != null && teams.shouldAffectVisuals()) {
            // Check if entity is a teammate - use their team color
            if (teams.isTeamMate(entity)) {
                int teamColor = getTeamColorFromName(entity);
                if (teamColor != -1) {
                    // Combine team color RGB with our alpha
                    int alpha = color.getAlpha();
                    return (alpha << 24) | (teamColor & 0x00FFFFFF);
                }
            }
        }
        return color.getColor();
    }

    /**
     * Get team color from entity's display name (like Tracers)
     */
    private int getTeamColorFromName(Entity entity) {
        if (!(entity instanceof net.minecraft.entity.player.EntityPlayer)) return -1;

        try {
            String displayName = entity.getDisplayName().getFormattedText();
            if (displayName.contains("§")) {
                for (int i = 0; i < displayName.length() - 1; i++) {
                    if (displayName.charAt(i) == '§') {
                        char code = Character.toLowerCase(displayName.charAt(i + 1));
                        if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                            return getMinecraftColor(code);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private int getMinecraftColor(char code) {
        switch (code) {
            case '0': return 0x000000; // Black
            case '1': return 0x0000AA; // Dark Blue
            case '2': return 0x00AA00; // Dark Green
            case '3': return 0x00AAAA; // Dark Aqua
            case '4': return 0xAA0000; // Dark Red
            case '5': return 0xAA00AA; // Dark Purple
            case '6': return 0xFFAA00; // Gold
            case '7': return 0xAAAAAA; // Gray
            case '8': return 0x555555; // Dark Gray
            case '9': return 0x5555FF; // Blue
            case 'a': return 0x55FF55; // Green
            case 'b': return 0x55FFFF; // Aqua
            case 'c': return 0xFF5555; // Red
            case 'd': return 0xFF55FF; // Light Purple
            case 'e': return 0xFFFF55; // Yellow
            case 'f': return 0xFFFFFF; // White
            default: return 0xFFFFFF;
        }
    }

    /**
     * Get RGB components for entity (team-aware)
     */
    public float getRedForEntity(Entity entity) {
        int c = getColorForEntity(entity);
        return ((c >> 16) & 0xFF) / 255.0f;
    }

    public float getGreenForEntity(Entity entity) {
        int c = getColorForEntity(entity);
        return ((c >> 8) & 0xFF) / 255.0f;
    }

    public float getBlueForEntity(Entity entity) {
        int c = getColorForEntity(entity);
        return (c & 0xFF) / 255.0f;
    }

    /**
     * Check if hitbox should be rendered for this entity
     * Returns false for bots when AntiBot is enabled
     */
    public boolean shouldRenderForEntity(Entity entity) {
        if (!enabled) return false;
        if (AntiBot.isBotForVisuals(entity)) return false;
        return true;
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
