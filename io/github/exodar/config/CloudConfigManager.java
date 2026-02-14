/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.exodar.ui.ModuleNotification;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Cloud Config Manager - handles API calls for cloud configs
 */
public class CloudConfigManager {

    private static final String API_BASE = "https://api.exodar.cc";
    private static final String API_SECRET = "xK9#mP2$vL5nQ8@jR3wF6";
    private static final Gson GSON = new Gson();

    // Cached configs
    private static List<CloudConfig> officialConfigs = new ArrayList<>();
    private static List<CloudConfig> publicConfigs = new ArrayList<>();
    private static List<CloudConfig> ownConfigs = new ArrayList<>();
    private static long lastFetchTime = 0;
    private static final long CACHE_DURATION = 60000; // 1 minute

    // Session token from mod auth
    private static String sessionToken = null;

    /**
     * Cloud config data class
     */
    public static class CloudConfig {
        public String configId;
        public String configName;
        public String username;
        public int downloads;
        public boolean isOfficial;
        public boolean isPublic;
        public String createdAt;

        public CloudConfig(String configId, String configName, String username, int downloads,
                           boolean isOfficial, boolean isPublic, String createdAt) {
            this.configId = configId;
            this.configName = configName;
            this.username = username;
            this.downloads = downloads;
            this.isOfficial = isOfficial;
            this.isPublic = isPublic;
            this.createdAt = createdAt;
        }
    }

    /**
     * Set session token (called after mod auth)
     */
    public static void setSessionToken(String token) {
        sessionToken = token;
    }

    /**
     * Check if authenticated
     */
    public static boolean isAuthenticated() {
        return sessionToken != null && !sessionToken.isEmpty();
    }

    /**
     * Fetch configs from API (async)
     */
    public static CompletableFuture<Boolean> fetchConfigs() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("session_token", sessionToken);

                String response = postRequest("/configs/list", request.toString());
                if (response == null) return false;

                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                if (json.has("data")) {
                    json = json.getAsJsonObject("data");
                }
                if (!json.has("success") || !json.get("success").getAsBoolean()) {
                    return false;
                }

                // Parse official configs
                officialConfigs.clear();
                JsonArray official = json.getAsJsonArray("official");
                if (official != null) {
                    for (int i = 0; i < official.size(); i++) {
                        officialConfigs.add(parseConfig(official.get(i).getAsJsonObject()));
                    }
                }

                // Parse public configs
                publicConfigs.clear();
                JsonArray pub = json.getAsJsonArray("public");
                if (pub != null) {
                    for (int i = 0; i < pub.size(); i++) {
                        publicConfigs.add(parseConfig(pub.get(i).getAsJsonObject()));
                    }
                }

                // Parse own configs
                ownConfigs.clear();
                JsonArray own = json.getAsJsonArray("own");
                if (own != null) {
                    for (int i = 0; i < own.size(); i++) {
                        ownConfigs.add(parseConfig(own.get(i).getAsJsonObject()));
                    }
                }

                lastFetchTime = System.currentTimeMillis();
                return true;

            } catch (Exception e) {
                System.out.println("[CloudConfig] Error fetching configs: " + e.getMessage());
                return false;
            }
        });
    }

    private static CloudConfig parseConfig(JsonObject obj) {
        return new CloudConfig(
            obj.get("config_id").getAsString(),
            obj.get("config_name").getAsString(),
            obj.get("username").getAsString(),
            obj.get("downloads").getAsInt(),
            obj.get("is_official").getAsInt() == 1,
            obj.get("is_public").getAsInt() == 1,
            obj.has("created_at") ? obj.get("created_at").getAsString() : ""
        );
    }

    /**
     * Upload a config to cloud
     */
    public static CompletableFuture<String> uploadConfig(String configName, String configData, boolean isPublic) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (sessionToken == null || sessionToken.isEmpty()) {
                    ModuleNotification.addNotification("Not authenticated", false);
                    return null;
                }

                JsonObject request = new JsonObject();
                request.addProperty("session_token", sessionToken);
                request.addProperty("config_name", configName);
                request.addProperty("config_data", configData);
                request.addProperty("is_public", isPublic);

                String response = postRequest("/configs/upload", request.toString());
                if (response == null) {
                    ModuleNotification.addNotification("Upload failed: no response", false);
                    return null;
                }

                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                // Handle wrapped response format: {"encrypted":false,"data":{...}}
                if (json.has("data")) {
                    json = json.getAsJsonObject("data");
                }
                if (!json.has("success") || !json.get("success").getAsBoolean()) {
                    String error = json.has("error") ? json.get("error").getAsString() : "Upload failed";
                    ModuleNotification.addNotification(error, false);
                    return null;
                }

                String configId = json.get("config_id").getAsString();
                ModuleNotification.addNotification("Uploaded! ID: " + configId, true);

                // Refresh cache
                fetchConfigs();

                return configId;

            } catch (Exception e) {
                System.out.println("[CloudConfig] Error uploading: " + e.getMessage());
                e.printStackTrace();
                ModuleNotification.addNotification("Upload error", false);
                return null;
            }
        });
    }

    /**
     * Download a config by ID
     * Can be used without authentication for public/private configs with known ID
     */
    public static CompletableFuture<String[]> downloadConfig(String configId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                if (sessionToken != null) {
                    request.addProperty("session_token", sessionToken);
                }
                request.addProperty("config_id", configId.toUpperCase());

                String response = postRequest("/configs/download", request.toString());
                if (response == null) return null;

                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                if (json.has("data")) {
                    json = json.getAsJsonObject("data");
                }
                if (!json.has("success") || !json.get("success").getAsBoolean()) {
                    String error = json.has("error") ? json.get("error").getAsString() : "Download failed";
                    ModuleNotification.addNotification(error, false);
                    return null;
                }

                // Return [configName, configData, username]
                return new String[] {
                    json.get("config_name").getAsString(),
                    json.get("config_data").getAsString(),
                    json.get("username").getAsString()
                };

            } catch (Exception e) {
                System.out.println("[CloudConfig] Error downloading: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Delete a config
     */
    public static CompletableFuture<Boolean> deleteConfig(String configId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("session_token", sessionToken);
                request.addProperty("config_id", configId);

                String response = postRequest("/configs/delete", request.toString());
                if (response == null) return false;

                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                if (json.has("data")) {
                    json = json.getAsJsonObject("data");
                }
                boolean success = json.has("success") && json.get("success").getAsBoolean();

                if (success) {
                    ModuleNotification.addNotification("Config deleted", true);
                    fetchConfigs();
                } else {
                    String error = json.has("error") ? json.get("error").getAsString() : "Delete failed";
                    ModuleNotification.addNotification(error, false);
                }

                return success;

            } catch (Exception e) {
                System.out.println("[CloudConfig] Error deleting: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Toggle config visibility (public/private)
     */
    public static CompletableFuture<Boolean> toggleVisibility(String configId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("session_token", sessionToken);
                request.addProperty("config_id", configId);

                String response = postRequest("/configs/toggle", request.toString());
                if (response == null) return false;

                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                if (json.has("data")) {
                    json = json.getAsJsonObject("data");
                }
                boolean success = json.has("success") && json.get("success").getAsBoolean();

                if (success) {
                    boolean isPublic = json.get("is_public").getAsBoolean();
                    ModuleNotification.addNotification(isPublic ? "Config is now public" : "Config is now private", true);
                    fetchConfigs();
                }

                return success;

            } catch (Exception e) {
                System.out.println("[CloudConfig] Error toggling: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Regenerate config ID
     */
    public static CompletableFuture<String> regenerateId(String oldConfigId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("session_token", sessionToken);
                request.addProperty("config_id", oldConfigId);

                String response = postRequest("/configs/regen", request.toString());
                if (response == null) return null;

                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                if (json.has("data")) {
                    json = json.getAsJsonObject("data");
                }
                if (!json.has("success") || !json.get("success").getAsBoolean()) return null;

                String newId = json.get("new_config_id").getAsString();
                ModuleNotification.addNotification("New ID: " + newId, true);
                fetchConfigs();

                return newId;

            } catch (Exception e) {
                System.out.println("[CloudConfig] Error regenerating: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Update config data
     */
    public static CompletableFuture<Boolean> updateConfig(String configId, String configData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("session_token", sessionToken);
                request.addProperty("config_id", configId);
                request.addProperty("config_data", configData);

                String response = postRequest("/configs/update", request.toString());
                if (response == null) return false;

                JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                if (json.has("data")) {
                    json = json.getAsJsonObject("data");
                }
                boolean success = json.has("success") && json.get("success").getAsBoolean();

                if (success) {
                    ModuleNotification.addNotification("Config updated in cloud", true);
                }

                return success;

            } catch (Exception e) {
                System.out.println("[CloudConfig] Error updating: " + e.getMessage());
                return false;
            }
        });
    }

    // ==================== Getters ====================

    public static List<CloudConfig> getOfficialConfigs() {
        return officialConfigs;
    }

    public static List<CloudConfig> getPublicConfigs() {
        return publicConfigs;
    }

    public static List<CloudConfig> getOwnConfigs() {
        return ownConfigs;
    }

    public static boolean needsRefresh() {
        return System.currentTimeMillis() - lastFetchTime > CACHE_DURATION;
    }

    /**
     * Check if a local config is uploaded to cloud
     */
    public static CloudConfig getCloudConfigByName(String configName) {
        for (CloudConfig c : ownConfigs) {
            if (c.configName.equalsIgnoreCase(configName)) {
                return c;
            }
        }
        return null;
    }

    // ==================== HTTP Helper ====================

    private static String postRequest(String endpoint, String jsonBody) {
        try {
            URL url = new URL(API_BASE + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Exodar-Client", API_SECRET);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();

        } catch (Exception e) {
            return null;
        }
    }
}
