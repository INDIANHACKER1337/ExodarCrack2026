/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.api;

/**
 * Stores build information retrieved from the Exodar API
 * This info is displayed in the bottom-right watermark
 */
public class BuildInfo {
    private static BuildInfo instance;

    // Build info fields
    private String buildType = "release";
    private String buildName = "Release Build";
    private String username = null;
    private String compilationDate = null;
    private boolean showUsername = true;
    private boolean showDate = true;
    private boolean isCustom = false;
    private boolean loaded = false;

    // Session info (from login)
    private String token = null;
    private String hwidHash = null;

    private BuildInfo() {}

    public static BuildInfo getInstance() {
        if (instance == null) {
            instance = new BuildInfo();
        }
        return instance;
    }

    /**
     * Update build info from API response
     */
    public void update(String buildType, String buildName, String username,
                       String compilationDate, boolean showUsername, boolean showDate, boolean isCustom) {
        this.buildType = buildType;
        this.buildName = buildName;
        this.username = username;
        this.compilationDate = compilationDate;
        this.showUsername = showUsername;
        this.showDate = showDate;
        this.isCustom = isCustom;
        this.loaded = true;

        // System.out.println("[Exodar] BuildInfo updated: " + getFormattedString());
    }

    /**
     * Set session credentials for API calls
     */
    public void setSession(String token, String hwidHash) {
        this.token = token;
        this.hwidHash = hwidHash;
    }

    /**
     * Get the formatted build string for display
     * Format: "§7BuildName - §rDATE§7 - User"
     * Example: "§7Developer Build - §r130126§7 - Shauna"
     */
    public String getFormattedString() {
        if (!loaded) {
            return "\u00A77Connecting...";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\u00A77").append(buildName);

        if (showDate && compilationDate != null && !compilationDate.isEmpty()) {
            sb.append(" - \u00A7r").append(compilationDate);
        }

        if (showUsername && username != null && !username.isEmpty()) {
            sb.append("\u00A77 - ").append(username);
        }

        return sb.toString();
    }

    // Getters
    public String getBuildType() { return buildType; }
    public String getBuildName() { return buildName; }
    public String getUsername() { return username; }
    public String getCompilationDate() { return compilationDate; }
    public boolean isShowUsername() { return showUsername; }
    public boolean isShowDate() { return showDate; }
    public boolean isCustom() { return isCustom; }
    public boolean isLoaded() { return loaded; }
    public String getToken() { return token; }
    public String getHwidHash() { return hwidHash; }

    /**
     * Check if current user is a developer (Shauna)
     * Used to show/hide debug options in modules
     * Checks both BuildInfo username and Minecraft session as fallback
     */
    public boolean isDeveloper() {
        // Check BuildInfo username first
        if (username != null && username.equalsIgnoreCase("Shauna")) {
            return true;
        }

        // Fallback: check Minecraft session username
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null && mc.getSession() != null) {
                String sessionName = mc.getSession().getUsername();
                if (sessionName != null && sessionName.equalsIgnoreCase("Shauna")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Reset to default values
     */
    public void reset() {
        buildType = "release";
        buildName = "Release Build";
        username = null;
        compilationDate = null;
        showUsername = true;
        showDate = true;
        isCustom = false;
        loaded = false;
        token = null;
        hwidHash = null;
    }
}
