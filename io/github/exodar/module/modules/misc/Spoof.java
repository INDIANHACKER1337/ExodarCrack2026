/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiObfuscate;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.spoof.SpoofManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Spoof - Replaces player names in chat, tab list, and scoreboard
 *
 * Uses SpoofManager to get spoofed names.
 * Use commands to add spoofs: /spoof add <original> <newname>
 */
public class Spoof extends Module {

    private static Spoof instance;

    private final TickSetting spoofChat;
    private final TickSetting spoofTab;
    private final TickSetting spoofScoreboard;
    private final TickSetting blastProtectionFilter;
    private final TickSetting hideMyPrefix;

    // Reflection fields for modifying packets
    private Field chatComponentField;
    private Field playerListDataField;
    private Field teamPlayersField;
    private Field teamPrefixField;
    private Field teamSuffixField;

    public Spoof() {
        super("Spoof", ModuleCategory.MISC);
        instance = this;

        // Always enabled by default
        this.enabled = true;

        // Hide from arraylist
        try {
            io.github.exodar.config.UserConfig.getInstance().hideModule("Spoof");
        } catch (Exception e) {
            // UserConfig not ready yet, will be hidden later
        }

        this.registerSetting(new DescriptionSetting("Spoof player names"));
        this.registerSetting(spoofChat = new TickSetting("Spoof Chat", true));
        this.registerSetting(spoofTab = new TickSetting("Spoof Tab", true));
        this.registerSetting(spoofScoreboard = new TickSetting("Spoof Scoreboard", true));
        this.registerSetting(hideMyPrefix = new TickSetting("Hide My Prefix", false));
        this.registerSetting(blastProtectionFilter = new TickSetting("Blast Protection Filter (TNTTag)", false));

        initReflection();
    }

    private void initReflection() {
        try {
            // S02PacketChat - chatComponent field
            for (Field f : S02PacketChat.class.getDeclaredFields()) {
                if (IChatComponent.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    chatComponentField = f;
                    break;
                }
            }

            // S38PacketPlayerListItem - players field (List<AddPlayerData>)
            for (Field f : S38PacketPlayerListItem.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    playerListDataField = f;
                    break;
                }
            }

            // S3EPacketTeams - players, prefix, suffix fields
            for (Field f : S3EPacketTeams.class.getDeclaredFields()) {
                f.setAccessible(true);
                String name = f.getName();
                Class<?> type = f.getType();

                if (type == String.class) {
                    // Could be prefix or suffix
                    if (name.contains("prefix") || name.contains("Prefix") || name.equals("d")) {
                        teamPrefixField = f;
                    } else if (name.contains("suffix") || name.contains("Suffix") || name.equals("e")) {
                        teamSuffixField = f;
                    }
                } else if (List.class.isAssignableFrom(type) || java.util.Collection.class.isAssignableFrom(type)) {
                    teamPlayersField = f;
                }
            }
        } catch (Exception e) {
            System.out.println("[Spoof] Error initializing reflection: " + e.getMessage());
        }
    }

    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;

        try {
            // === BLAST PROTECTION FILTER (TNTTag) ===
            if (blastProtectionFilter.isEnabled() && packet instanceof S02PacketChat) {
                S02PacketChat chatPacket = (S02PacketChat) packet;
                IChatComponent component = chatPacket.getChatComponent();
                if (component != null) {
                    String text = component.getUnformattedText();
                    // Filter "You got lucky and survived X's explosion!"
                    if (text != null && text.contains("You got lucky and survived") && text.contains("explosion!")) {
                        return false; // Cancel this packet - don't show in chat
                    }
                }
            }

            // === CHAT SPOOF ===
            if (spoofChat.isEnabled() && packet instanceof S02PacketChat) {
                spoofChatPacket((S02PacketChat) packet);
            }

            // === TAB LIST SPOOF ===
            if (spoofTab.isEnabled() && packet instanceof S38PacketPlayerListItem) {
                spoofTabPacket((S38PacketPlayerListItem) packet);
            }

            // === SCOREBOARD/TEAMS SPOOF ===
            if (spoofScoreboard.isEnabled()) {
                if (packet instanceof S3EPacketTeams) {
                    spoofTeamsPacket((S3EPacketTeams) packet);
                } else if (packet instanceof S3CPacketUpdateScore) {
                    spoofScorePacket((S3CPacketUpdateScore) packet);
                }
            }

        } catch (Exception e) {
            // Silent fail
        }

        return true; // Don't cancel packet
    }

    /**
     * Spoof names in chat messages
     * Modifies components in-place to preserve hover/click events
     */
    private void spoofChatPacket(S02PacketChat packet) {
        if (chatComponentField == null) return;

        try {
            IChatComponent original = packet.getChatComponent();
            if (original == null) return;

            // Modify the component tree in-place
            spoofComponentRecursive(original);
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Recursively spoof text in chat components while preserving structure
     */
    private void spoofComponentRecursive(IChatComponent component) {
        if (component == null) return;

        try {
            // Spoof the unformatted text of this component (not siblings)
            if (component instanceof ChatComponentText) {
                ChatComponentText textComponent = (ChatComponentText) component;
                String text = textComponent.getUnformattedTextForChat();

                if (text != null && !text.isEmpty()) {
                    String spoofed = SpoofManager.applySpoof(text);
                    if (!text.equals(spoofed)) {
                        // Use reflection to modify the text field
                        for (Field f : ChatComponentText.class.getDeclaredFields()) {
                            if (f.getType() == String.class) {
                                f.setAccessible(true);
                                String fieldValue = (String) f.get(textComponent);
                                if (text.equals(fieldValue)) {
                                    f.set(textComponent, spoofed);
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Recursively process siblings
            for (IChatComponent sibling : component.getSiblings()) {
                spoofComponentRecursive(sibling);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Spoof names in tab list
     * Modifies the displayName field in AddPlayerData
     */
    private void spoofTabPacket(S38PacketPlayerListItem packet) {
        try {
            // Get the list of player data entries
            List<?> entries = packet.getEntries();
            if (entries == null || entries.isEmpty()) return;

            // Get the AddPlayerData class (inner class)
            Class<?> addPlayerDataClass = null;
            for (Class<?> innerClass : S38PacketPlayerListItem.class.getDeclaredClasses()) {
                if (innerClass.getSimpleName().equals("AddPlayerData")) {
                    addPlayerDataClass = innerClass;
                    break;
                }
            }
            if (addPlayerDataClass == null) return;

            // Find the displayName and profile fields in AddPlayerData
            Field displayNameField = null;
            Field profileField = null;
            for (Field f : addPlayerDataClass.getDeclaredFields()) {
                f.setAccessible(true);
                if (IChatComponent.class.isAssignableFrom(f.getType())) {
                    displayNameField = f;
                } else if (GameProfile.class.isAssignableFrom(f.getType())) {
                    profileField = f;
                }
            }

            if (profileField == null) return;

            // Process each entry
            for (Object entry : entries) {
                try {
                    // Get the GameProfile to check the name
                    GameProfile profile = (GameProfile) profileField.get(entry);
                    if (profile == null) continue;

                    String playerName = profile.getName();
                    if (playerName == null) continue;

                    // Check if this player is spoofed
                    boolean isSpoofed = SpoofManager.isSpoofed(playerName);

                    // Check if this is the local player and hideMyPrefix is enabled
                    boolean isLocalPlayer = false;
                    try {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                        if (mc != null && mc.thePlayer != null) {
                            isLocalPlayer = playerName.equalsIgnoreCase(mc.thePlayer.getName());
                        }
                    } catch (Exception ignored) {}

                    boolean shouldHidePrefix = isLocalPlayer && hideMyPrefix.isEnabled();

                    // Get display name from the packet
                    String originalFormatted = null;
                    IChatComponent originalDisplay = (IChatComponent) displayNameField.get(entry);

                    if (originalDisplay != null) {
                        originalFormatted = originalDisplay.getFormattedText();
                    }

                    // If packet display is null/empty, try to get from NetworkPlayerInfo cache
                    // This happens for UPDATE_LATENCY and other packets that don't include display name
                    if ((originalFormatted == null || originalFormatted.isEmpty()) && isSpoofed) {
                        try {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                            if (mc != null && mc.getNetHandler() != null) {
                                net.minecraft.client.network.NetworkPlayerInfo cachedInfo =
                                    mc.getNetHandler().getPlayerInfo(profile.getId());
                                if (cachedInfo != null && cachedInfo.getDisplayName() != null) {
                                    // Get cached display - but check if it contains original name (not already spoofed)
                                    String cached = cachedInfo.getDisplayName().getFormattedText();
                                    if (cached != null && cached.toLowerCase().contains(playerName.toLowerCase())) {
                                        originalFormatted = cached;
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // If hideMyPrefix is on for local player, just use player name
                    if (shouldHidePrefix) {
                        originalFormatted = playerName;
                    }

                    // If still null and not spoofed, skip this entry (don't modify)
                    if (originalFormatted == null || originalFormatted.isEmpty()) {
                        if (!isSpoofed) continue;
                        // For spoofed players, fallback to player name
                        originalFormatted = playerName;
                    }

                    // Save original for Teams module (only save if contains original name, not spoofed)
                    if (!shouldHidePrefix && originalFormatted.toLowerCase().contains(playerName.toLowerCase())) {
                        SpoofManager.saveOriginalDisplayName(playerName, originalFormatted);
                    }

                    // Apply spoof if this player is spoofed
                    if (isSpoofed) {
                        String finalDisplayName = SpoofManager.applySpoof(originalFormatted);
                        IChatComponent newDisplayName = createFormattedComponent(finalDisplayName);
                        displayNameField.set(entry, newDisplayName);
                    }

                    // Apply AntiObfuscate to all players
                    if (displayNameField != null) {
                        IChatComponent displayName = (IChatComponent) displayNameField.get(entry);
                        if (displayName != null) {
                            String text = displayName.getFormattedText();
                            String cleaned = AntiObfuscate.stripObfuscated(text);
                            if (cleaned != null && !cleaned.equals(text)) {
                                displayNameField.set(entry, createFormattedComponent(cleaned));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Silent fail for individual entry
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Spoof names in teams (scoreboard)
     * Captures team info BEFORE spoofing (for tab prefix restoration)
     */
    private void spoofTeamsPacket(S3EPacketTeams packet) {
        try {
            // First, capture original team info BEFORE spoofing
            String teamName = null;
            String prefix = null;
            String suffix = null;
            java.util.Collection<String> players = null;
            int action = -1;

            // Get packet action (0=create, 1=remove, 2=update, 3=add players, 4=remove players)
            try {
                action = packet.getAction();
            } catch (Exception e) {
                // Try reflection if method is obfuscated
                for (Field f : S3EPacketTeams.class.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (f.getType() == int.class) {
                        int val = f.getInt(packet);
                        if (val >= 0 && val <= 4) {
                            action = val;
                            break;
                        }
                    }
                }
            }

            // Extract team info via reflection (handles obfuscated names)
            for (Field f : S3EPacketTeams.class.getDeclaredFields()) {
                f.setAccessible(true);
                Class<?> type = f.getType();
                String fieldName = f.getName().toLowerCase();

                if (type == String.class) {
                    String value = (String) f.get(packet);
                    if (value != null) {
                        // Try to identify which field this is
                        if (fieldName.contains("name") || fieldName.equals("a") || fieldName.equals("b")) {
                            if (teamName == null) teamName = value;
                        }
                        if (fieldName.contains("prefix") || fieldName.equals("d")) {
                            prefix = value;
                        }
                        if (fieldName.contains("suffix") || fieldName.equals("e")) {
                            suffix = value;
                        }
                    }
                } else if (java.util.Collection.class.isAssignableFrom(type)) {
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.Collection<String> col = (java.util.Collection<String>) f.get(packet);
                        if (col != null && !col.isEmpty()) {
                            players = col;
                        }
                    } catch (ClassCastException ignored) {}
                }
            }

            // Save team info for each player BEFORE spoofing (for actions that add players to teams)
            if (players != null && (action == 0 || action == 3)) { // create team or add players
                for (Object obj : players) {
                    if (obj instanceof String) {
                        String playerName = (String) obj;
                        // Save original team info (prefix, suffix) for this player
                        SpoofManager.saveTeamInfo(playerName, prefix, suffix, teamName);
                    }
                }
            }

            // Remove team info when players leave team
            if (players != null && (action == 1 || action == 4)) { // remove team or remove players
                for (Object obj : players) {
                    if (obj instanceof String) {
                        String playerName = (String) obj;
                        SpoofManager.removeTeamInfo(playerName);
                    }
                }
            }

            // Now apply spoofing to the packet
            for (Field f : S3EPacketTeams.class.getDeclaredFields()) {
                f.setAccessible(true);
                Class<?> type = f.getType();

                // Spoof String fields (prefix, suffix, display name, etc.)
                if (type == String.class) {
                    String value = (String) f.get(packet);
                    if (value != null && !value.isEmpty()) {
                        String spoofed = SpoofManager.applySpoof(value);
                        if (!value.equals(spoofed)) {
                            f.set(packet, spoofed);
                        }
                    }
                }
                // Spoof Collection<String> fields (player names)
                else if (java.util.Collection.class.isAssignableFrom(type)) {
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.Collection<String> col = (java.util.Collection<String>) f.get(packet);
                        if (col != null && !col.isEmpty()) {
                            java.util.List<String> newPlayers = new java.util.ArrayList<>();
                            boolean changed = false;

                            for (Object obj : col) {
                                if (obj instanceof String) {
                                    String player = (String) obj;
                                    String spoofed = SpoofManager.applySpoof(player);
                                    newPlayers.add(spoofed);
                                    if (!player.equals(spoofed)) {
                                        changed = true;
                                    }
                                }
                            }

                            if (changed) {
                                col.clear();
                                col.addAll(newPlayers);
                            }
                        }
                    } catch (ClassCastException e) {
                        // Not a Collection<String>, ignore
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Spoof names in score updates
     */
    private void spoofScorePacket(S3CPacketUpdateScore packet) {
        try {
            // The score packet has a playerName field
            for (Field f : S3CPacketUpdateScore.class.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == String.class) {
                    String value = (String) f.get(packet);
                    if (value != null && !value.isEmpty()) {
                        String spoofed = SpoofManager.applySpoof(value);
                        if (!value.equals(spoofed)) {
                            f.set(packet, spoofed);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    @Override
    public String getDisplaySuffix() {
        int count = SpoofManager.getSpoofCount();
        return count > 0 ? String.valueOf(count) : "";
    }

    public static Spoof getInstance() {
        return instance;
    }

    /**
     * Strip color/format codes from text (§X where X is 0-9, a-f, k-o, r)
     */
    private String stripColorCodes(String text) {
        if (text == null) return null;
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    /**
     * Check if display name is essentially just the player name (with only color codes, no prefix like VIP)
     */
    private boolean isJustPlayerName(String displayName, String playerName) {
        if (displayName == null || playerName == null) return true;
        String stripped = stripColorCodes(displayName).trim();
        return stripped.equalsIgnoreCase(playerName);
    }

    /**
     * Create a properly formatted IChatComponent from a string with § color codes.
     * This ensures colors display correctly in tab list.
     */
    private IChatComponent createFormattedComponent(String text) {
        if (text == null || text.isEmpty()) {
            return new ChatComponentText("");
        }

        // Base component
        ChatComponentText base = new ChatComponentText("");

        // Current formatting state
        EnumChatFormatting currentColor = null;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        StringBuilder currentText = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c == '§' || c == '\u00a7') && i + 1 < text.length()) {
                // Flush current text with current formatting
                if (currentText.length() > 0) {
                    ChatComponentText segment = new ChatComponentText(currentText.toString());
                    ChatStyle style = new ChatStyle();
                    if (currentColor != null) style.setColor(currentColor);
                    if (bold) style.setBold(true);
                    if (italic) style.setItalic(true);
                    if (underline) style.setUnderlined(true);
                    if (strikethrough) style.setStrikethrough(true);
                    if (obfuscated) style.setObfuscated(true);
                    segment.setChatStyle(style);
                    base.appendSibling(segment);
                    currentText = new StringBuilder();
                }

                // Parse format code
                char code = Character.toLowerCase(text.charAt(i + 1));
                i++; // Skip the code character

                switch (code) {
                    // Color codes (0-9, a-f) - these RESET all formatting in Minecraft
                    case '0': currentColor = EnumChatFormatting.BLACK; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '1': currentColor = EnumChatFormatting.DARK_BLUE; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '2': currentColor = EnumChatFormatting.DARK_GREEN; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '3': currentColor = EnumChatFormatting.DARK_AQUA; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '4': currentColor = EnumChatFormatting.DARK_RED; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '5': currentColor = EnumChatFormatting.DARK_PURPLE; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '6': currentColor = EnumChatFormatting.GOLD; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '7': currentColor = EnumChatFormatting.GRAY; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '8': currentColor = EnumChatFormatting.DARK_GRAY; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case '9': currentColor = EnumChatFormatting.BLUE; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case 'a': currentColor = EnumChatFormatting.GREEN; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case 'b': currentColor = EnumChatFormatting.AQUA; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case 'c': currentColor = EnumChatFormatting.RED; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case 'd': currentColor = EnumChatFormatting.LIGHT_PURPLE; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case 'e': currentColor = EnumChatFormatting.YELLOW; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    case 'f': currentColor = EnumChatFormatting.WHITE; bold = false; italic = false; underline = false; strikethrough = false; obfuscated = false; break;
                    // Format codes - these ADD formatting without resetting color
                    case 'k': obfuscated = true; break;
                    case 'l': bold = true; break;
                    case 'm': strikethrough = true; break;
                    case 'n': underline = true; break;
                    case 'o': italic = true; break;
                    case 'r': // Reset - clears everything
                        currentColor = null;
                        bold = false;
                        italic = false;
                        underline = false;
                        strikethrough = false;
                        obfuscated = false;
                        break;
                }
            } else {
                currentText.append(c);
            }
        }

        // Flush remaining text
        if (currentText.length() > 0) {
            ChatComponentText segment = new ChatComponentText(currentText.toString());
            ChatStyle style = new ChatStyle();
            if (currentColor != null) style.setColor(currentColor);
            if (bold) style.setBold(true);
            if (italic) style.setItalic(true);
            if (underline) style.setUnderlined(true);
            if (strikethrough) style.setStrikethrough(true);
            if (obfuscated) style.setObfuscated(true);
            segment.setChatStyle(style);
            base.appendSibling(segment);
        }

        return base;
    }
}
