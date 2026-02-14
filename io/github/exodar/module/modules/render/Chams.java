/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.friend.FriendManager;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.spoof.SpoofManager;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.awt.Color;
import java.lang.reflect.Field;

/**
 * Chams - Makes players visible through walls
 * Supports multiple color modes like Exhibition:
 * - Friend: Different colors for friends vs enemies
 * - TeamColor: Uses team color from player name (§a, §c, etc.)
 * - Team: Uses Teams module detection
 * - Custom: User-defined colors
 * - Rainbow: Animated rainbow effect
 */
public class Chams extends Module {

    private TickSetting colored;
    private ModeSetting renderMode; // Flat or Texture
    private ModeSetting colorMode;
    private ColorSetting visibleColor;
    private ColorSetting invisibleColor;

    // Rainbow animation
    private float rainbowHue = 0;
    private long lastRainbowUpdate = 0;

    // Cached colors to avoid recalculating multiple times per entity
    private int cachedVisibleColor = 0;
    private int cachedInvisibleColor = 0;
    private int cachedEntityId = -1;

    // Color presets
    private static final int FRIEND_VISIBLE = 0xFF55FF55;    // Green
    private static final int FRIEND_INVISIBLE = 0xFF228B22;  // Darker green
    private static final int ENEMY_VISIBLE = 0xFFFF5555;     // Red
    private static final int ENEMY_INVISIBLE = 0xFF8B0000;   // Darker red

    // Cached colorCode field for reflection
    private static Field colorCodeField = null;
    private static int[] colorCodeCache = null;

    public Chams() {
        super("Chams", ModuleCategory.VISUALS);
        this.registerSetting(new DescriptionSetting("See players through walls"));
        this.registerSetting(colored = new TickSetting("Colored", true));
        this.registerSetting(renderMode = new ModeSetting("Render", new String[]{"Flat", "Texture"}));
        this.registerSetting(colorMode = new ModeSetting("Color", new String[]{"Custom", "Friend", "TeamColor", "Team", "Rainbow"}));
        this.registerSetting(visibleColor = new ColorSetting("Visible Color", 255, 50, 50, 255));
        this.registerSetting(invisibleColor = new ColorSetting("Invisible Color", 150, 30, 30, 180));

        // Show/hide color settings based on mode
        colorMode.onChange(() -> {
            boolean isCustom = colorMode.getSelected().equals("Custom");
            visibleColor.setVisible(isCustom);
            invisibleColor.setVisible(isCustom);
        });

        // Initial visibility
        boolean isCustom = colorMode.getSelected().equals("Custom");
        visibleColor.setVisible(isCustom);
        invisibleColor.setVisible(isCustom);
    }

    /**
     * Update rainbow animation - call this from render tick
     */
    public void updateRainbow() {
        // Slower speed - was 0.5f, now 0.1f for smoother animation
        rainbowHue += 0.1f;
        if (rainbowHue >= 255) {
            rainbowHue -= 255;
        }
    }

    /**
     * Check if chams should be applied to this entity
     */
    public boolean shouldApplyForEntity(Entity entity) {
        if (!enabled) return false;
        if (AntiBot.isBotForVisuals(entity)) return false;
        return true;
    }

    public boolean isColored() {
        return colored.isEnabled();
    }

    public boolean isFlat() {
        return renderMode.getSelected().equals("Flat");
    }

    public boolean isTextureMode() {
        return renderMode.getSelected().equals("Texture");
    }

    public String getRenderMode() {
        return renderMode.getSelected();
    }

    public String getColorMode() {
        return colorMode.getSelected();
    }

    /**
     * Get the visible color for an entity based on current mode
     * Returns ARGB color
     */
    public int getVisibleColorForEntity(Entity entity) {
        // Check MurderMystery first (takes priority in MM games)
        if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
            int mmColor = io.github.exodar.module.modules.misc.MurderMystery.getESPColor(entity.getName());
            if (mmColor != -1) {
                return mmColor;
            }
        }

        String mode = colorMode.getSelected();

        switch (mode) {
            case "Friend":
                return isFriendOrSelf(entity) ? FRIEND_VISIBLE : ENEMY_VISIBLE;

            case "TeamColor":
                return getTeamColorFromName(entity, true);

            case "Team":
                return isTeammate(entity) ? FRIEND_VISIBLE : ENEMY_VISIBLE;

            case "Rainbow":
                return getRainbowColor(entity, true);

            case "Custom":
            default:
                return visibleColor.getColor();
        }
    }

    /**
     * Get the invisible (through-wall) color for an entity based on current mode
     * Returns ARGB color
     */
    public int getInvisibleColorForEntity(Entity entity) {
        // Check MurderMystery first (takes priority in MM games)
        if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
            int mmColor = io.github.exodar.module.modules.misc.MurderMystery.getESPColor(entity.getName());
            if (mmColor != -1) {
                // Return darker version for invisible pass
                int r = (int) ((mmColor >> 16 & 0xFF) / 1.5);
                int g = (int) ((mmColor >> 8 & 0xFF) / 1.5);
                int b = (int) ((mmColor & 0xFF) / 1.5);
                return 0xB0000000 | (r << 16) | (g << 8) | b;
            }
        }

        String mode = colorMode.getSelected();

        switch (mode) {
            case "Friend":
                return isFriendOrSelf(entity) ? FRIEND_INVISIBLE : ENEMY_INVISIBLE;

            case "TeamColor":
                return getTeamColorFromName(entity, false);

            case "Team":
                return isTeammate(entity) ? FRIEND_INVISIBLE : ENEMY_INVISIBLE;

            case "Rainbow":
                return getRainbowColor(entity, false);

            case "Custom":
            default:
                return invisibleColor.getColor();
        }
    }

    /**
     * Get color components for visible pass
     */
    public float getVisibleRed(Entity entity) {
        return ((getVisibleColorForEntity(entity) >> 16) & 0xFF) / 255.0f;
    }

    public float getVisibleGreen(Entity entity) {
        return ((getVisibleColorForEntity(entity) >> 8) & 0xFF) / 255.0f;
    }

    public float getVisibleBlue(Entity entity) {
        return (getVisibleColorForEntity(entity) & 0xFF) / 255.0f;
    }

    public float getVisibleAlpha(Entity entity) {
        return ((getVisibleColorForEntity(entity) >> 24) & 0xFF) / 255.0f;
    }

    /**
     * Get color components for invisible pass
     */
    public float getInvisibleRed(Entity entity) {
        return ((getInvisibleColorForEntity(entity) >> 16) & 0xFF) / 255.0f;
    }

    public float getInvisibleGreen(Entity entity) {
        return ((getInvisibleColorForEntity(entity) >> 8) & 0xFF) / 255.0f;
    }

    public float getInvisibleBlue(Entity entity) {
        return (getInvisibleColorForEntity(entity) & 0xFF) / 255.0f;
    }

    public float getInvisibleAlpha(Entity entity) {
        int alpha = ((getInvisibleColorForEntity(entity) >> 24) & 0xFF);
        return alpha > 0 ? alpha / 255.0f : 0.6f; // Default 60% alpha for invisible
    }

    // ===== Helper methods =====

    private boolean isFriendOrSelf(Entity entity) {
        if (entity == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && entity == mc.thePlayer) return true;
        return FriendManager.getInstance().isFriend(entity.getName());
    }

    private boolean isTeammate(Entity entity) {
        if (entity == null) return false;
        Teams teamsModule = Teams.getInstance();
        if (teamsModule != null && teamsModule.isEnabled()) {
            return teamsModule.isTeamMate(entity);
        }
        return false;
    }

    /**
     * Get the colorCode array from FontRenderer using reflection
     */
    private int[] getColorCodeArray() {
        if (colorCodeCache != null) {
            return colorCodeCache;
        }

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.fontRendererObj == null) return null;

            if (colorCodeField == null) {
                // Try to find the colorCode field
                for (Field field : mc.fontRendererObj.getClass().getDeclaredFields()) {
                    if (field.getType() == int[].class) {
                        field.setAccessible(true);
                        int[] arr = (int[]) field.get(mc.fontRendererObj);
                        if (arr != null && arr.length == 32) { // colorCode has 32 entries
                            colorCodeField = field;
                            colorCodeCache = arr;
                            return arr;
                        }
                    }
                }
            } else {
                colorCodeCache = (int[]) colorCodeField.get(mc.fontRendererObj);
                return colorCodeCache;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Get team color from player's display name color code
     * Like Exhibition's TeamColor mode
     */
    private int getTeamColorFromName(Entity entity, boolean visible) {
        if (!(entity instanceof EntityPlayer)) {
            return visible ? visibleColor.getColor() : invisibleColor.getColor();
        }

        EntityPlayer player = (EntityPlayer) entity;
        try {
            String text = null;
            String playerName = player.getName();

            // For spoofed players, get CURRENT team from scoreboard (real-time, not cached)
            if (SpoofManager.isSpoofed(playerName)) {
                // Try to get team color from scoreboard directly (most accurate)
                if (player.getTeam() != null) {
                    text = net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName(player.getTeam(), playerName);
                }
                // Fallback: try cached team info
                if (text == null || text.equals(playerName)) {
                    SpoofManager.TeamInfo teamInfo = SpoofManager.getTeamInfo(playerName);
                    if (teamInfo != null && !teamInfo.prefix.isEmpty()) {
                        text = teamInfo.prefix + playerName + teamInfo.suffix;
                    }
                }
            }

            // Default: use display name directly
            if (text == null || text.isEmpty()) {
                text = player.getDisplayName().getFormattedText();
            }

            // Find first color code
            for (int i = 0; i < text.length() - 1; i++) {
                if (text.charAt(i) == '\u00A7') { // § character
                    char colorChar = Character.toLowerCase(text.charAt(i + 1));
                    int colorIndex = "0123456789abcdef".indexOf(colorChar);

                    if (colorIndex >= 0 && colorIndex < 16) {
                        int[] colorCodes = getColorCodeArray();
                        if (colorCodes != null && colorIndex < colorCodes.length) {
                            int baseColor = colorCodes[colorIndex];

                            if (visible) {
                                // Full brightness
                                return 0xFF000000 | baseColor;
                            } else {
                                // Darker version (divide by 1.5)
                                int r = (int) ((baseColor >> 16 & 0xFF) / 1.5);
                                int g = (int) ((baseColor >> 8 & 0xFF) / 1.5);
                                int b = (int) ((baseColor & 0xFF) / 1.5);
                                return 0xB0000000 | (r << 16) | (g << 8) | b; // ~70% alpha
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback to custom color
        return visible ? visibleColor.getColor() : invisibleColor.getColor();
    }

    /**
     * Get rainbow color with entity-based offset
     * Like Exhibition's Rainbow mode
     */
    private int getRainbowColor(Entity entity, boolean visible) {
        // Calculate hue with entity offset for wave effect
        float hue = rainbowHue;
        if (entity != null) {
            hue += entity.getEntityId() / 5.0f;
            hue += Math.abs(entity.getName().hashCode()) / 300.0f;
        }
        hue = hue % 255;

        // Convert to RGB
        int rgb;
        if (visible) {
            rgb = MathHelper.hsvToRGB(hue / 255.0f, 0.8f, 1.0f);
            return 0xFF000000 | (rgb & 0x00FFFFFF);
        } else {
            // Inverted hue for invisible pass (like Exhibition)
            rgb = MathHelper.hsvToRGB((1.0f - hue / 255.0f) % 1.0f, 0.8f, 1.0f);
            return 0xB0000000 | (rgb & 0x00FFFFFF);
        }
    }
}
