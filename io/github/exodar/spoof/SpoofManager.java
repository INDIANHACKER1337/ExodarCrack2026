/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.spoof;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages name spoofing - replaces player names with custom names and optionally skins
 */
public class SpoofManager {

    private static final Map<String, String> spoofedNames = new HashMap<>();
    private static final Map<String, SpoofData> spoofData = new ConcurrentHashMap<>();

    // Cache of ORIGINAL display names before spoofing (for Teams module)
    // Key: lowercase player name, Value: original formatted display name from server
    private static final Map<String, String> originalDisplayNames = new ConcurrentHashMap<>();

    // Cache of team info (prefix + suffix) from S3EPacketTeams
    // Key: lowercase player name, Value: TeamInfo with prefix and suffix
    private static final Map<String, TeamInfo> playerTeamInfo = new ConcurrentHashMap<>();

    /**
     * Team info data class
     */
    public static class TeamInfo {
        public final String prefix;
        public final String suffix;
        public final String teamName;

        public TeamInfo(String prefix, String suffix, String teamName) {
            this.prefix = prefix != null ? prefix : "";
            this.suffix = suffix != null ? suffix : "";
            this.teamName = teamName != null ? teamName : "";
        }
    }

    // Developer display name format (purple Dev tag with Shauna name)
    public static final String DEV_DISPLAY_NAME = "&7[&5&oDev&7]&d &oShauna";

    // Fallback spoofs - always work offline
    static {
        // Shauna's main account - ALWAYS works as fallback
        addSpoof("shawt1e", DEV_DISPLAY_NAME, true, "Shauna");
        // System.out.println("[SpoofManager] Fallback spoof loaded for shawt1e");
        // Additional spoofs loaded from API on startup
    }

    // Developer accounts - loaded from API
    private static final Map<String, String> developerAccounts = new ConcurrentHashMap<>();
    private static boolean developerAccountsLoaded = false;

    /**
     * Add a developer account spoof
     * These are accounts that belong to Exodar developers
     * @param accountName The Minecraft username of the developer account
     * @param displayName The name to display (e.g. "&7[&cDev&7]&c Shauna")
     */
    public static void addDeveloperAccount(String accountName, String displayName) {
        developerAccounts.put(accountName.toLowerCase(), displayName);
        addSpoof(accountName, displayName, true, "Shauna"); // Use Shauna's skin
        // System.out.println("[SpoofManager] Added developer account: " + accountName + " -> " + displayName);
    }

    /**
     * Load developer accounts from API response
     * Expected format: array of {account: "name", display: "formatted name"}
     */
    public static void loadDeveloperAccounts(java.util.List<String[]> accounts) {
        for (String[] acc : accounts) {
            if (acc.length >= 2) {
                addDeveloperAccount(acc[0], acc[1]);
            }
        }
        developerAccountsLoaded = true;
        // System.out.println("[SpoofManager] Loaded " + accounts.size() + " developer accounts");
    }

    /**
     * Check if developer accounts have been loaded
     */
    public static boolean areDeveloperAccountsLoaded() {
        return developerAccountsLoaded;
    }

    /**
     * Check if a player is a developer account
     */
    public static boolean isDeveloperAccount(String playerName) {
        if (playerName == null) return false;
        return developerAccounts.containsKey(playerName.toLowerCase());
    }

    /**
     * Data class to hold spoof information including skin
     */
    public static class SpoofData {
        public final String customName;
        public final boolean useSkin;
        public String skinUrl = null;
        public ResourceLocation skinLocation = null;
        public boolean skinLoaded = false;
        public boolean skinLoading = false;
        public String skinPlayerName = null; // Name to fetch skin from

        public SpoofData(String customName, boolean useSkin, String skinPlayerName) {
            this.customName = customName;
            this.useSkin = useSkin;
            this.skinPlayerName = skinPlayerName;
        }
    }

    /**
     * Add a name spoof
     * @param originalName The original player name (case insensitive)
     * @param customName The custom name to display (supports color codes with &)
     */
    public static void addSpoof(String originalName, String customName) {
        addSpoof(originalName, customName, false, null);
    }

    /**
     * Add a name spoof with optional skin
     * @param originalName The original player name (case insensitive)
     * @param customName The custom name to display (supports color codes with &)
     * @param useSkin Whether to also spoof the skin
     * @param skinPlayerName The player name to fetch skin from (null = extract from customName)
     */
    public static void addSpoof(String originalName, String customName, boolean useSkin, String skinPlayerName) {
        // Convert & to § for color codes
        String formattedName = customName.replace("&", "\u00a7");
        spoofedNames.put(originalName.toLowerCase(), formattedName);

        // If useSkin but no skinPlayerName provided, try to extract from customName (strip color codes)
        if (useSkin && skinPlayerName == null) {
            skinPlayerName = customName.replaceAll("&[0-9a-fk-or]", "").trim();
        }

        SpoofData data = new SpoofData(formattedName, useSkin, skinPlayerName);
        spoofData.put(originalName.toLowerCase(), data);

        // System.out.println("[SpoofManager] Added spoof: " + originalName + " -> " + customName + (useSkin ? " (with skin from " + skinPlayerName + ")" : ""));

        // If skin is requested, fetch it asynchronously
        if (useSkin && skinPlayerName != null && !skinPlayerName.isEmpty()) {
            fetchSkinAsync(originalName.toLowerCase(), skinPlayerName);
        }
    }

    /**
     * Fetch skin from Mojang API asynchronously
     */
    private static void fetchSkinAsync(String originalNameKey, String skinPlayerName) {
        SpoofData data = spoofData.get(originalNameKey);
        if (data == null || data.skinLoading || data.skinLoaded) return;

        data.skinLoading = true;

        CompletableFuture.runAsync(() -> {
            try {
                // System.out.println("[SpoofManager] Fetching skin for: " + skinPlayerName);

                // Step 1: Get UUID from username
                String uuid = getUUIDFromUsername(skinPlayerName);
                if (uuid == null) {
                    // System.out.println("[SpoofManager] Could not find UUID for: " + skinPlayerName);
                    data.skinLoading = false;
                    return;
                }

                // Step 2: Get profile with skin URL
                String skinUrl = getSkinUrlFromUUID(uuid);
                if (skinUrl == null) {
                    // System.out.println("[SpoofManager] Could not find skin for: " + skinPlayerName);
                    data.skinLoading = false;
                    return;
                }

                data.skinUrl = skinUrl;
                // System.out.println("[SpoofManager] Found skin URL: " + skinUrl);

                // Step 3: Load the skin texture on the main thread
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null) {
                    mc.addScheduledTask(() -> {
                        try {
                            // Create a fake GameProfile to load the skin
                            GameProfile profile = new GameProfile(UUID.randomUUID(), skinPlayerName);

                            // Download and cache the skin
                            ResourceLocation skinLoc = mc.getSkinManager().loadSkin(
                                new MinecraftProfileTexture(skinUrl, null),
                                MinecraftProfileTexture.Type.SKIN
                            );

                            data.skinLocation = skinLoc;
                            data.skinLoaded = true;
                            data.skinLoading = false;
                            // System.out.println("[SpoofManager] Skin loaded successfully: " + skinLoc);
                        } catch (Exception e) {
                            // System.out.println("[SpoofManager] Error loading skin texture: " + e.getMessage());
                            data.skinLoading = false;
                        }
                    });
                }
            } catch (Exception e) {
                // System.out.println("[SpoofManager] Error fetching skin: " + e.getMessage());
                data.skinLoading = false;
            }
        });
    }

    /**
     * Get UUID from Mojang API
     */
    private static String getUUIDFromUsername(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
            return json.get("id").getAsString();
        } catch (Exception e) {
            // System.out.println("[SpoofManager] Error getting UUID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get skin URL from session server
     */
    private static String getSkinUrlFromUUID(String uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
            JsonObject properties = json.getAsJsonArray("properties").get(0).getAsJsonObject();
            String value = properties.get("value").getAsString();

            // Decode base64
            String decoded = new String(Base64.getDecoder().decode(value));
            JsonObject textureJson = new JsonParser().parse(decoded).getAsJsonObject();
            JsonObject textures = textureJson.getAsJsonObject("textures");

            if (textures.has("SKIN")) {
                return textures.getAsJsonObject("SKIN").get("url").getAsString();
            }
            return null;
        } catch (Exception e) {
            // System.out.println("[SpoofManager] Error getting skin URL: " + e.getMessage());
            return null;
        }
    }

    /**
     * Remove a name spoof
     * @param originalName The original player name
     */
    public static void removeSpoof(String originalName) {
        spoofedNames.remove(originalName.toLowerCase());
        spoofData.remove(originalName.toLowerCase());
        // System.out.println("[SpoofManager] Removed spoof: " + originalName);
    }

    /**
     * Clear all spoofs
     */
    public static void clearAll() {
        spoofedNames.clear();
        spoofData.clear();
        // System.out.println("[SpoofManager] Cleared all spoofs");
    }

    /**
     * Get the spoofed name for a player
     * @param originalName The original player name
     * @return The spoofed name, or null if not spoofed
     */
    public static String getSpoofedName(String originalName) {
        if (originalName == null) return null;
        return spoofedNames.get(originalName.toLowerCase());
    }

    /**
     * Check if a name is spoofed
     * @param originalName The original player name
     * @return true if the name is spoofed
     */
    public static boolean isSpoofed(String originalName) {
        if (originalName == null) return false;
        return spoofedNames.containsKey(originalName.toLowerCase());
    }

    /**
     * Save original display name before spoofing
     * Called by Spoof module when processing tab list packets
     * @param playerName The player's actual username
     * @param displayName The original formatted display name from server
     */
    public static void saveOriginalDisplayName(String playerName, String displayName) {
        if (playerName != null && displayName != null) {
            originalDisplayNames.put(playerName.toLowerCase(), displayName);
        }
    }

    /**
     * Get original display name (before spoofing)
     * Used by Teams module to get real team colors
     * @param playerName The player's actual username
     * @return Original display name, or null if not cached
     */
    public static String getOriginalDisplayName(String playerName) {
        if (playerName == null) return null;
        return originalDisplayNames.get(playerName.toLowerCase());
    }

    /**
     * Clear cached display names (call when leaving server)
     */
    public static void clearDisplayNameCache() {
        originalDisplayNames.clear();
        playerTeamInfo.clear();
    }

    /**
     * Save team info for a player from S3EPacketTeams
     * @param playerName The player's username
     * @param prefix Team prefix (may contain color codes)
     * @param suffix Team suffix
     * @param teamName Team name
     */
    public static void saveTeamInfo(String playerName, String prefix, String suffix, String teamName) {
        if (playerName != null) {
            playerTeamInfo.put(playerName.toLowerCase(), new TeamInfo(prefix, suffix, teamName));
        }
    }

    /**
     * Get saved team info for a player
     * @param playerName The player's username
     * @return TeamInfo or null if not saved
     */
    public static TeamInfo getTeamInfo(String playerName) {
        if (playerName == null) return null;
        return playerTeamInfo.get(playerName.toLowerCase());
    }

    /**
     * Remove team info for a player (when they leave a team)
     */
    public static void removeTeamInfo(String playerName) {
        if (playerName != null) {
            playerTeamInfo.remove(playerName.toLowerCase());
        }
    }

    /**
     * Apply spoof to a text if it contains a spoofed name
     * Preserves prefixes like [MVP], team colors, etc.
     * Uses word boundaries to avoid matching partial words (e.g., "cul" in "MAYUSCULAS")
     * @param text The text that may contain a spoofed name
     * @return The text with spoofed name applied, or original if no spoof found
     */
    public static String applySpoof(String text) {
        if (text == null) return null;

        String result = text;
        for (Map.Entry<String, String> entry : spoofedNames.entrySet()) {
            String original = entry.getKey();
            String replacement = entry.getValue();
            String lowerOriginal = original.toLowerCase();

            // Case insensitive search with word boundary check
            int idx = result.toLowerCase().indexOf(lowerOriginal);

            while (idx >= 0) {
                // Check if this is a whole word match (not part of another word)
                // Allow color codes (§) as valid boundaries
                char charBefore = (idx > 0) ? result.charAt(idx - 1) : ' ';
                char charAfter = (idx + original.length() < result.length()) ? result.charAt(idx + original.length()) : ' ';

                boolean validStart = !Character.isLetterOrDigit(charBefore);
                boolean validEnd = !Character.isLetterOrDigit(charAfter);

                if (validStart && validEnd) {
                    // Replace preserving surrounding text
                    result = result.substring(0, idx) + replacement + result.substring(idx + original.length());
                    break; // Only replace first occurrence per spoof
                }

                // Search for next occurrence (update search after current position)
                int nextSearch = idx + 1;
                if (nextSearch >= result.length()) break;
                idx = result.toLowerCase().indexOf(lowerOriginal, nextSearch);
            }
        }
        return result;
    }

    /**
     * Get all current spoofs
     * @return Map of original names to spoofed names
     */
    public static Map<String, String> getAllSpoofs() {
        return new HashMap<>(spoofedNames);
    }

    /**
     * Get count of spoofed names
     * @return Number of spoofed names
     */
    public static int getSpoofCount() {
        return spoofedNames.size();
    }

    /**
     * Get spoof data for a player
     * @param originalName The original player name
     * @return SpoofData or null if not spoofed
     */
    public static SpoofData getSpoofData(String originalName) {
        if (originalName == null) return null;
        return spoofData.get(originalName.toLowerCase());
    }

    /**
     * Get spoofed skin location for a player
     * @param originalName The original player name
     * @return ResourceLocation for the skin, or null if not available
     */
    public static ResourceLocation getSpoofedSkin(String originalName) {
        if (originalName == null) return null;
        SpoofData data = spoofData.get(originalName.toLowerCase());
        if (data != null && data.useSkin && data.skinLoaded) {
            return data.skinLocation;
        }
        return null;
    }

    /**
     * Check if a player has a spoofed skin ready
     * @param originalName The original player name
     * @return true if skin is loaded and ready
     */
    public static boolean hasSpoofedSkin(String originalName) {
        if (originalName == null) return false;
        SpoofData data = spoofData.get(originalName.toLowerCase());
        return data != null && data.useSkin && data.skinLoaded && data.skinLocation != null;
    }

    /**
     * Check if skin is currently loading for a player
     * @param originalName The original player name
     * @return true if skin is being loaded
     */
    public static boolean isSkinLoading(String originalName) {
        if (originalName == null) return false;
        SpoofData data = spoofData.get(originalName.toLowerCase());
        return data != null && data.skinLoading;
    }
}
