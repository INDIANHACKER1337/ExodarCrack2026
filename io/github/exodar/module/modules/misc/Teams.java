/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;

import io.github.exodar.spoof.SpoofManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

/**
 * Teams - Detecta compañeros de equipo
 * Usa lógica simple y confiable como la versión original
 */
public class Teams extends Module {

    private TickSetting useScoreboardTeam;
    private TickSetting useNameColor;
    private TickSetting useArmorColor;
    private TickSetting affectCombat;
    private TickSetting affectVisuals;
    private TickSetting debug;

    private static Teams instance;

    // Debug throttle
    private long lastDebugTime = 0;
    private static final long DEBUG_COOLDOWN = 1000; // 1 second between debug messages

    public Teams() {
        super("Teams", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Team detection"));
        this.registerSetting(useScoreboardTeam = new TickSetting("Scoreboard Team", true));
        this.registerSetting(useNameColor = new TickSetting("Name Color", true));
        this.registerSetting(useArmorColor = new TickSetting("Armor Color", false));
        this.registerSetting(new DescriptionSetting("--- Affect ---"));
        this.registerSetting(affectCombat = new TickSetting("Affect Combat", true));
        this.registerSetting(affectVisuals = new TickSetting("Affect Visuals", false));
        this.registerSetting(debug = new TickSetting("Debug", false));
    }

    public static Teams getInstance() {
        return instance;
    }

    /**
     * Check if entity is on same team as local player
     * Simple and reliable like original version
     */
    public boolean isTeamMate(Entity entity) {
        if (!isEnabled()) return false;
        if (entity == null) return false;
        if (!(entity instanceof EntityPlayer)) return false;

        EntityPlayer target = (EntityPlayer) entity;
        EntityPlayer player = getPlayer();
        if (player == null) return false;
        if (target == player) return true;

        // Method 1: Native Minecraft scoreboard team (most reliable)
        if (useScoreboardTeam.isEnabled()) {
            try {
                if (player.isOnSameTeam((EntityLivingBase) target)) {
                    if (debug.isEnabled()) debug("§aScoreboard match!");
                    return true;
                }
            } catch (Exception ignored) {}
        }

        // Method 2: Team color (using original display name before spoof)
        if (useNameColor.isEnabled()) {
            try {
                // Get color - priority: scoreboard team > original display name > current display name (non-spoofed only)
                char playerColor = getOriginalTeamColor(player);
                char targetColor = getOriginalTeamColor(target);

                if (debug.isEnabled()) {
                    String playerInfo = playerColor != 0 ? "§" + playerColor + playerColor : "§7none";
                    String targetInfo = targetColor != 0 ? "§" + targetColor + targetColor : "§7none";
                    boolean playerSpoofed = SpoofManager.isSpoofed(player.getName());
                    boolean targetSpoofed = SpoofManager.isSpoofed(target.getName());
                    debug("§7You: " + playerInfo + (playerSpoofed ? "§8(spoof)" : "") +
                          " §7Target: " + targetInfo + (targetSpoofed ? "§8(spoof)" : ""));
                }

                if (playerColor != 0 && targetColor != 0 && playerColor == targetColor) {
                    if (debug.isEnabled()) debug("§aColor match!");
                    return true;
                }
            } catch (Exception ignored) {}
        }

        // Method 3: Armor color (leather armor)
        if (useArmorColor.isEnabled()) {
            try {
                int playerArmorColor = getArmorColor(player);
                int targetArmorColor = getArmorColor(target);

                if (playerArmorColor != -1 && targetArmorColor != -1 &&
                    playerArmorColor == targetArmorColor) {
                    if (debug.isEnabled()) debug("§aArmor match!");
                    return true;
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

    /**
     * Get ORIGINAL team color (before spoof was applied)
     * Priority: 1. Scoreboard team (most reliable, not affected by spoof)
     *           2. Cached team info from S3EPacketTeams
     *           3. Cached original display name (for spoofed players)
     *           4. Current display name (only for non-spoofed players)
     */
    private char getOriginalTeamColor(EntityPlayer player) {
        String playerName = player.getName();
        boolean isSpoofed = SpoofManager.isSpoofed(playerName);

        // 1. Try scoreboard team FIRST (not affected by spoof, most reliable)
        try {
            if (player.getTeam() != null) {
                // Try to get the team color prefix
                String teamPrefix = player.getTeam().formatString("");
                if (teamPrefix != null && !teamPrefix.isEmpty()) {
                    char color = findColorCode(teamPrefix);
                    if (color != 0) return color;
                }
                // Also try getting color from team-formatted player name
                String teamFormatted = player.getTeam().formatString(playerName);
                if (teamFormatted != null && !teamFormatted.equals(playerName)) {
                    char color = findColorCode(teamFormatted);
                    if (color != 0) return color;
                }
            }
        } catch (Exception ignored) {}

        // 2. Try cached team info from S3EPacketTeams (captured before spoof modifies it)
        SpoofManager.TeamInfo teamInfo = SpoofManager.getTeamInfo(playerName);
        if (teamInfo != null && !teamInfo.prefix.isEmpty()) {
            char color = findColorCode(teamInfo.prefix);
            if (color != 0) return color;
        }

        // 3. For spoofed players, check cached original display name
        if (isSpoofed) {
            String originalDisplay = SpoofManager.getOriginalDisplayName(playerName);
            if (originalDisplay != null && !originalDisplay.isEmpty()) {
                char color = findColorCode(originalDisplay);
                if (color != 0) return color;
            }
            // If spoofed but no original cached and no team, return 0 (don't use spoofed display name!)
            return 0;
        }

        // 4. For non-spoofed players only, fallback to current display name
        return findColorCode(getDisplayName(player));
    }

    /**
     * Find color code in display name
     * Skips rank prefix brackets and finds team color
     */
    private char findColorCode(String text) {
        if (text == null || text.length() < 2) return 0;

        // Find position after last bracket (skip rank prefix)
        int startPos = 0;
        int bracketPos = text.lastIndexOf(']');
        if (bracketPos >= 0 && bracketPos < text.length() - 2) {
            startPos = bracketPos + 1;
        }

        // Find first color code after bracket
        for (int i = startPos; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (c == '§' || c == '\u00a7') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    return code;
                }
            }
        }

        // Fallback: first color code anywhere
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (c == '§' || c == '\u00a7') {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    return code;
                }
            }
        }

        return 0;
    }

    /**
     * Get display name with colors from tab list or entity
     */
    private String getDisplayName(EntityPlayer player) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return null;

            // Try tab list first
            NetHandlerPlayClient netHandler = mc.getNetHandler();
            if (netHandler != null) {
                NetworkPlayerInfo info = netHandler.getPlayerInfo(player.getUniqueID());
                if (info != null) {
                    IChatComponent displayName = info.getDisplayName();
                    if (displayName != null) {
                        return displayName.getFormattedText();
                    }
                }
            }

            // Fallback to entity display name
            IChatComponent displayName = player.getDisplayName();
            if (displayName != null) {
                return displayName.getFormattedText();
            }

            // Fallback to scoreboard team formatting
            if (player.getTeam() != null) {
                return player.getTeam().formatString(player.getName());
            }

            return player.getName();
        } catch (Exception e) {
            return player.getName();
        }
    }

    /**
     * Get armor color from leather chestplate
     */
    private int getArmorColor(EntityPlayer player) {
        try {
            net.minecraft.item.ItemStack chestplate = player.getCurrentArmor(2);
            if (chestplate == null) return -1;

            if (!(chestplate.getItem() instanceof net.minecraft.item.ItemArmor)) return -1;

            String armorName = chestplate.getUnlocalizedName().toLowerCase();
            if (!armorName.contains("leather")) return -1;

            if (chestplate.hasTagCompound() && chestplate.getTagCompound().hasKey("display")) {
                net.minecraft.nbt.NBTTagCompound display = chestplate.getTagCompound().getCompoundTag("display");
                if (display.hasKey("color")) {
                    return display.getInteger("color");
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    public boolean shouldAffectCombat() {
        return isEnabled() && affectCombat.isEnabled();
    }

    public boolean shouldAffectVisuals() {
        return isEnabled() && affectVisuals.isEnabled();
    }

    /**
     * Get team color RGB for visuals (uses scoreboard, bypasses spoof)
     */
    public int getTeamColor(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return -1;

        try {
            EntityPlayer player = (EntityPlayer) entity;
            String playerName = player.getName();

            // Try scoreboard first (not affected by spoof)
            char colorCode = 0;
            if (player.getTeam() != null) {
                String teamPrefix = player.getTeam().formatString("");
                if (teamPrefix != null && !teamPrefix.isEmpty()) {
                    colorCode = findColorCode(teamPrefix);
                }
                if (colorCode == 0) {
                    // Try team formatted name
                    String teamFormatted = player.getTeam().formatString(playerName);
                    if (teamFormatted != null) {
                        colorCode = findColorCode(teamFormatted);
                    }
                }
            }

            // Try cached team info from S3EPacketTeams
            if (colorCode == 0) {
                SpoofManager.TeamInfo teamInfo = SpoofManager.getTeamInfo(playerName);
                if (teamInfo != null && !teamInfo.prefix.isEmpty()) {
                    colorCode = findColorCode(teamInfo.prefix);
                }
            }

            // Fallback to display name (for non-spoofed players only)
            if (colorCode == 0 && !SpoofManager.isSpoofed(playerName)) {
                String displayName = getDisplayName(player);
                colorCode = findColorCode(displayName);
            }

            // For spoofed players, use cached ORIGINAL display name
            if (colorCode == 0 && SpoofManager.isSpoofed(playerName)) {
                String originalDisplay = SpoofManager.getOriginalDisplayName(playerName);
                if (originalDisplay != null) {
                    colorCode = findColorCode(originalDisplay);
                }
            }

            if (colorCode != 0) {
                return getMinecraftColor(colorCode);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private int getMinecraftColor(char code) {
        switch (code) {
            case '0': return 0x000000;
            case '1': return 0x0000AA;
            case '2': return 0x00AA00;
            case '3': return 0x00AAAA;
            case '4': return 0xAA0000;
            case '5': return 0xAA00AA;
            case '6': return 0xFFAA00;
            case '7': return 0xAAAAAA;
            case '8': return 0x555555;
            case '9': return 0x5555FF;
            case 'a': return 0x55FF55;
            case 'b': return 0x55FFFF;
            case 'c': return 0xFF5555;
            case 'd': return 0xFF55FF;
            case 'e': return 0xFFFF55;
            case 'f': return 0xFFFFFF;
            default: return 0xFFFFFF;
        }
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }

    private void debug(String message) {
        if (debug.isEnabled()) {
            long now = System.currentTimeMillis();
            if (now - lastDebugTime < DEBUG_COOLDOWN) return;
            lastDebugTime = now;

            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§7[§eTeams§7] §f" + message));
                }
            } catch (Exception ignored) {}
        }
    }
}
