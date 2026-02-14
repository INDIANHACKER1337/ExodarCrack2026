/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.api;

import io.github.exodar.config.CloudConfigManager;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/**
 * Exodar API Client using reflection for HTTP connections
 * Uses wrappers to avoid direct class dependencies
 */
public class ExodarAPI {
    private static final String API_BASE_URL = "https://api.exodar.cc";
    private static final String API_SECRET_HEADER = "X-Exodar-Client";
    private static final String API_SECRET_VALUE = "xK9#mP2$vL5nQ8@jR3wF6";

    // Cached credentials from launcher
    private String cachedUsername = null;
    private String cachedPassword = null;
    private String cachedHwidHash = null;

    // Independent mod session state (separate from launcher)
    private String modSessionToken = null;
    private String currentNonce = null;
    private long tokenExpiresAt = 0;
    private boolean isAuthenticated = false;
    private int authRetryCount = 0;
    private static final int MAX_AUTH_RETRIES = 3;

    private static ExodarAPI instance;
    private final ExecutorService executor;

    // Reflection cached classes/methods
    private Class<?> urlClass;
    private Class<?> httpUrlConnectionClass;
    private Method openConnectionMethod;
    private Method setRequestMethodMethod;
    private Method setRequestPropertyMethod;
    private Method setDoOutputMethod;
    private Method getOutputStreamMethod;
    private Method getInputStreamMethod;
    private Method getResponseCodeMethod;
    private Method disconnectMethod;

    private ExodarAPI() {
        executor = Executors.newSingleThreadExecutor();
        initReflection();
    }

    public static ExodarAPI getInstance() {
        if (instance == null) {
            instance = new ExodarAPI();
        }
        return instance;
    }

    /**
     * Initialize reflection for HTTP classes
     */
    private void initReflection() {
        try {
            urlClass = Class.forName("java.net.URL");
            httpUrlConnectionClass = Class.forName("java.net.HttpURLConnection");

            openConnectionMethod = urlClass.getMethod("openConnection");
            setRequestMethodMethod = httpUrlConnectionClass.getMethod("setRequestMethod", String.class);
            setRequestPropertyMethod = httpUrlConnectionClass.getMethod("setRequestProperty", String.class, String.class);
            setDoOutputMethod = httpUrlConnectionClass.getMethod("setDoOutput", boolean.class);
            getOutputStreamMethod = httpUrlConnectionClass.getMethod("getOutputStream");
            getInputStreamMethod = httpUrlConnectionClass.getMethod("getInputStream");
            getResponseCodeMethod = httpUrlConnectionClass.getMethod("getResponseCode");
            disconnectMethod = httpUrlConnectionClass.getMethod("disconnect");

            // System.out.println("[ExodarAPI] Reflection initialized successfully");
        } catch (Exception e) {
            // System.out.println("[ExodarAPI] Failed to initialize reflection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a URL object using reflection
     */
    private Object createURL(String urlString) throws Exception {
        Constructor<?> constructor = urlClass.getConstructor(String.class);
        return constructor.newInstance(urlString);
    }

    /**
     * Perform HTTP POST request using reflection
     */
    private String httpPost(String endpoint, String jsonBody, String token) {
        Object connection = null;
        try {
            Object url = createURL(API_BASE_URL + endpoint);
            connection = openConnectionMethod.invoke(url);

            // Set request method to POST
            setRequestMethodMethod.invoke(connection, "POST");

            // Set headers
            setRequestPropertyMethod.invoke(connection, "Content-Type", "application/json");
            setRequestPropertyMethod.invoke(connection, API_SECRET_HEADER, API_SECRET_VALUE);

            if (token != null && !token.isEmpty()) {
                setRequestPropertyMethod.invoke(connection, "X-Exodar-Token", token);
            }

            // Enable output
            setDoOutputMethod.invoke(connection, true);

            // Write body
            OutputStream os = (OutputStream) getOutputStreamMethod.invoke(connection);
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            os.close();

            // Get response
            int responseCode = (Integer) getResponseCodeMethod.invoke(connection);

            InputStream is;
            if (responseCode >= 200 && responseCode < 300) {
                is = (InputStream) getInputStreamMethod.invoke(connection);
            } else {
                // Try to get error stream
                Method getErrorStreamMethod = httpUrlConnectionClass.getMethod("getErrorStream");
                is = (InputStream) getErrorStreamMethod.invoke(connection);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return response.toString();

        } catch (Exception e) {
            // System.out.println("[ExodarAPI] HTTP POST error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                try {
                    disconnectMethod.invoke(connection);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Fetch build info from API asynchronously
     */
    public CompletableFuture<Boolean> fetchBuildInfo() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // System.out.println("[ExodarAPI] >>> BUILD-INFO: Fetching build info from API...");
                BuildInfo info = BuildInfo.getInstance();
                String token = info.getToken();
                String hwidHash = info.getHwidHash();

                if (token == null || token.isEmpty()) {
                    // System.out.println("[ExodarAPI] No token available, cannot fetch build info");
                    return false;
                }

                String json = String.format("{\"token\":\"%s\",\"hwid_hash\":\"%s\"}",
                        escapeJson(token),
                        escapeJson(hwidHash != null ? hwidHash : ""));

                String response = httpPost("/build-info", json, token);

                // System.out.println("[ExodarAPI] <<< BUILD-INFO RESPONSE: " + (response != null ? response.substring(0, Math.min(300, response.length())) : "null"));

                if (response == null || response.isEmpty()) {
                    // System.out.println("[ExodarAPI]     ERROR: Empty response from build-info");
                    return false;
                }

                // Parse JSON response manually (no external JSON library)
                if (response.contains("\"success\":true") || response.contains("\"build_name\"")) {
                    String buildType = extractJsonString(response, "build_type");
                    String buildName = extractJsonString(response, "build_name");
                    String username = extractJsonString(response, "username");
                    String compilationDate = extractJsonString(response, "compilation_date");
                    boolean showUsername = extractJsonBoolean(response, "show_username", true);
                    boolean showDate = extractJsonBoolean(response, "show_date", true);
                    boolean isCustom = extractJsonBoolean(response, "is_custom", false);

                    info.update(buildType, buildName, username, compilationDate, showUsername, showDate, isCustom);
                    return true;
                } else {
                    // System.out.println("[ExodarAPI] Build info request failed: " + response);
                    return false;
                }

            } catch (Exception e) {
                // System.out.println("[ExodarAPI] Error fetching build info: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }

    /**
     * Simple JSON string extraction (no library dependency)
     */
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int startIndex = keyIndex + searchKey.length();

        // Skip whitespace
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        if (startIndex >= json.length()) return null;

        // Check if value is null
        if (json.substring(startIndex).startsWith("null")) {
            return null;
        }

        // Check if value is a string (starts with ")
        if (json.charAt(startIndex) != '"') {
            return null;
        }

        startIndex++; // Skip opening quote
        int endIndex = startIndex;

        // Find closing quote (handle escaped quotes)
        while (endIndex < json.length()) {
            char c = json.charAt(endIndex);
            if (c == '"' && (endIndex == startIndex || json.charAt(endIndex - 1) != '\\')) {
                break;
            }
            endIndex++;
        }

        return json.substring(startIndex, endIndex);
    }

    /**
     * Simple JSON boolean extraction
     */
    private boolean extractJsonBoolean(String json, String key, boolean defaultValue) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return defaultValue;

        int startIndex = keyIndex + searchKey.length();

        // Skip whitespace
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        if (json.substring(startIndex).startsWith("true")) {
            return true;
        } else if (json.substring(startIndex).startsWith("false")) {
            return false;
        } else if (json.substring(startIndex).startsWith("1")) {
            return true;
        } else if (json.substring(startIndex).startsWith("0")) {
            return false;
        }

        return defaultValue;
    }

    /**
     * Escape string for JSON
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Shutdown the executor
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    // ===============================================
    // HWID2 VERIFICATION
    // ===============================================

    /**
     * Verify HWID2 with the API
     * This checks if the mod is running on the same PC as the launcher
     *
     * @param userId The user ID from BuildInfo
     * @return true if HWID2 matches, false otherwise
     */
    public CompletableFuture<Boolean> verifyHwid2(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String hwid2 = HWIDGenerator.getHWID2();
                if (hwid2 == null || hwid2.isEmpty()) {
                    // System.out.println("[ExodarAPI] Failed to generate HWID2 for verification");
                    return false;
                }

                // System.out.println("[ExodarAPI] Verifying HWID2: " + hwid2.substring(0, 16) + "...");

                String json = String.format(
                    "{\"user_id\":%d,\"hwid2\":\"%s\"}",
                    userId,
                    escapeJson(hwid2)
                );

                String response = httpPost("/hwid2/verify", json, null);

                if (response == null || response.isEmpty()) {
                    // System.out.println("[ExodarAPI] Empty response from HWID2 verify");
                    return false;
                }

                // System.out.println("[ExodarAPI] HWID2 verify response: " + response);

                if (response.contains("\"valid\":true")) {
                    // System.out.println("[ExodarAPI] HWID2 verified successfully!");
                    return true;
                } else {
                    String error = extractJsonString(response, "error");
                    // System.out.println("[ExodarAPI] HWID2 verification failed: " + (error != null ? error : "unknown"));
                    return false;
                }

            } catch (Exception e) {
                // System.out.println("[ExodarAPI] HWID2 verify error: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    /**
     * Verify HWID2 synchronously (blocking)
     */
    public boolean verifyHwid2Sync(int userId) {
        try {
            return verifyHwid2(userId).get();
        } catch (Exception e) {
            // System.out.println("[ExodarAPI] HWID2 sync verify error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get HWID2 for this machine
     */
    public String getHwid2() {
        return HWIDGenerator.getHWID2();
    }

    /**
     * Initialize using direct HWID authentication
     * No launcher dependency - generates HWID locally and authenticates with API
     */
    public CompletableFuture<Boolean> initFromLauncherCredentials() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // System.out.println("[ExodarAPI] >>> INIT: Starting HWID2 authentication...");

                // Generate HWID2 (simple) - SHA256 of C: drive volume serial
                // This matches the launcher's GenerateSimpleHWID()
                // System.out.println("[ExodarAPI] Generating HWID2...");
                String hwid2 = HWIDGenerator.getHWID2();

                if (hwid2 == null || hwid2.isEmpty()) {
                    // System.out.println("[ExodarAPI] Failed to generate HWID2");
                    return false;
                }

                cachedHwidHash = hwid2; // Store HWID2 as the cached hash
                // System.out.println("[ExodarAPI] HWID2 generated: " + hwid2.substring(0, 32) + "...");

                // Authenticate directly with API using HWID2
                String json = String.format("{\"hwid2\":\"%s\"}", escapeJson(hwid2));
                // System.out.println("[ExodarAPI] Calling /mod/hwid-direct...");

                String response = httpPost("/mod/hwid-direct", json, null);

                if (response == null || response.isEmpty()) {
                    // System.out.println("[ExodarAPI] Empty response from HWID auth");
                    return false;
                }

                // System.out.println("[ExodarAPI] Response: " + response.substring(0, Math.min(200, response.length())));

                // Check for kill signal first
                if (response.contains("\"kill\":true")) {
                    // System.out.println("[ExodarAPI] KILL SIGNAL received!");
                    if (commandCallback != null) {
                        commandCallback.onCloseCommand();
                    }
                    return false;
                }

                // Check if HWID not registered - try to link it
                if (response.contains("not registered")) {
                    // System.out.println("[ExodarAPI] HWID not registered, attempting to link...");

                    // Get minecraft name if available
                    String mcName = currentMinecraftNick;
                    if (mcName == null || mcName.isEmpty()) {
                        try {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                            if (mc != null && mc.getSession() != null) {
                                mcName = mc.getSession().getUsername();
                            }
                        } catch (Exception ignored) {}
                    }

                    // Try to link HWID2 - include forum_username if known
                    String forumUser = cachedUsername; // From previous auth or config
                    if (forumUser == null || forumUser.isEmpty()) {
                        forumUser = System.getProperty("exodar.forum.username", "");
                    }

                    String linkJson = String.format(
                        "{\"hwid2\":\"%s\",\"minecraft_name\":\"%s\",\"forum_username\":\"%s\"}",
                        escapeJson(hwid2),
                        escapeJson(mcName != null ? mcName : ""),
                        escapeJson(forumUser)
                    );

                    // System.out.println("[ExodarAPI] Calling /mod/link-hwid with mc=" + mcName + ", forum=" + forumUser);
                    String linkResponse = httpPost("/mod/link-hwid", linkJson, null);
                    // System.out.println("[ExodarAPI] Link response: " + (linkResponse != null ? linkResponse.substring(0, Math.min(150, linkResponse.length())) : "null"));

                    if (linkResponse != null && linkResponse.contains("\"linked\":true")) {
                        // System.out.println("[ExodarAPI] HWID linked successfully! Retrying auth...");

                        // Retry authentication
                        response = httpPost("/mod/hwid-direct", json, null);
                        // System.out.println("[ExodarAPI] Retry response: " + (response != null ? response.substring(0, Math.min(200, response.length())) : "null"));

                        if (response == null || response.contains("\"error\"")) {
                            String error = extractJsonString(response, "error");
                            // System.out.println("[ExodarAPI] Auth still failed after linking: " + error);
                            return false;
                        }
                    } else {
                        // System.out.println("[ExodarAPI] Could not link HWID. Make sure launcher is open or was used recently.");
                        return false;
                    }
                } else if (response.contains("\"error\"")) {
                    String error = extractJsonString(response, "error");
                    // System.out.println("[ExodarAPI] HWID auth error: " + error);
                    return false;
                }

                // Parse success response
                String token = extractJsonString(response, "token");
                String username = extractJsonString(response, "username");
                boolean isDev = extractJsonBoolean(response, "is_dev", false);
                boolean isBeta = extractJsonBoolean(response, "is_beta", false);

                if (token == null || token.isEmpty()) {
                    // System.out.println("[ExodarAPI] No token received");
                    return false;
                }

                // Store session
                modSessionToken = token;
                cachedUsername = username;
                isAuthenticated = true;
                tokenExpiresAt = System.currentTimeMillis() + 3600000; // 1 hour

                // System.out.println("[ExodarAPI] Authenticated as: " + username);
                // System.out.println("[ExodarAPI] isDev=" + isDev + ", isBeta=" + isBeta);

                // Set up BuildInfo
                BuildInfo info = BuildInfo.getInstance();
                info.setSession(token, hwid2);

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyy");
                String date = sdf.format(new java.util.Date());

                // Check for build_config from API response
                String buildType = "release";
                String buildName = "Release Build";
                boolean showUsername = true;
                boolean showDate = true;
                boolean isCustom = false;

                if (response.contains("\"build_config\"") && !response.contains("\"build_config\":null")) {
                    String customBuildText = extractJsonString(response, "custom_build_text");
                    String configBuildType = extractJsonString(response, "build_type");
                    showUsername = extractJsonBoolean(response, "show_username", true);
                    showDate = extractJsonBoolean(response, "show_date", true);

                    if (customBuildText != null && !customBuildText.isEmpty()) {
                        buildName = customBuildText;
                        buildType = configBuildType != null ? configBuildType : "custom";
                        isCustom = true;
                        // System.out.println("[ExodarAPI] Custom build: " + buildName);
                    } else if (configBuildType != null) {
                        buildType = configBuildType;
                        buildName = getBuildNameFromType(buildType);
                    }
                } else {
                    // Fallback to is_dev/is_beta
                    buildType = isDev ? "dev" : (isBeta ? "beta" : "release");
                    buildName = isDev ? "Developer Build" : (isBeta ? "Beta Build" : "Release Build");
                }

                info.update(buildType, buildName, username, date, showUsername, showDate, isCustom);

                // Set session token for cloud configs
                CloudConfigManager.setSessionToken(token);

                // System.out.println("[ExodarAPI] Session initialized: " + username + " (" + buildName + ")");

                // Check for user_display (auto-spoof for this user)
                if (response.contains("\"user_display\"")) {
                    String displayName = extractJsonString(response, "display_name");
                    boolean useSkin = extractJsonBoolean(response, "use_skin", false);
                    String skinPlayer = extractJsonString(response, "skin_player");

                    if (displayName != null && !displayName.isEmpty()) {
                        // System.out.println("[ExodarAPI] User has display config: " + displayName);
                        // This will be applied when we know the minecraft nick
                    }
                }

                // Fetch custom names for spoofing
                fetchCustomNames();

                return true;

            } catch (Exception e) {
                // System.out.println("[ExodarAPI] ERROR in HWID auth: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }

    /**
     * Authenticate with API using cached credentials
     */
    private boolean authenticate() {
        try {
            // Build HWID JSON (simplified - just need the hash)
            String hwidJson = String.format(
                "{\"cpu_id\":\"\",\"cpu_brand\":\"\",\"ram_amount\":\"\",\"disk_serial\":\"\",\"motherboard_serial\":\"\",\"gpu_serial\":\"\",\"ram_serial\":\"\"}");

            String json = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"hwid\":%s,\"pc_name\":\"Java\",\"os_version\":\"Minecraft\"}",
                escapeJson(cachedUsername),
                escapeJson(cachedPassword),
                hwidJson);

            String response = httpPost("/auth/simple-login", json, null);

            if (response == null || response.isEmpty()) {
                // System.out.println("[ExodarAPI] Empty auth response");
                return false;
            }

            // System.out.println("[ExodarAPI] Auth response: " + response.substring(0, Math.min(200, response.length())) + "...");

            // Check for success
            if (response.contains("\"status\":\"APPROVED\"")) {
                String token = extractJsonString(response, "token");
                String username = extractJsonString(response, "username");

                if (token != null && !token.isEmpty()) {
                    BuildInfo info = BuildInfo.getInstance();
                    info.setSession(token, cachedHwidHash);

                    // Set initial build info
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyy");
                    String date = sdf.format(new java.util.Date());

                    // Determine build type from response
                    boolean isDev = extractJsonBoolean(response, "is_dev", false);
                    boolean isBeta = extractJsonBoolean(response, "is_beta", false);

                    String buildType = isDev ? "dev" : (isBeta ? "beta" : "release");
                    String buildName = isDev ? "Developer Build" : (isBeta ? "Beta Build" : "Release Build");

                    info.update(buildType, buildName, username != null ? username : cachedUsername, date, true, true, false);

                    // System.out.println("[ExodarAPI] Authenticated as: " + username + " (" + buildName + ")");

                    // Now fetch custom build info from API
                    fetchBuildInfo();

                    return true;
                }
            } else if (response.contains("\"status\":\"PENDING\"")) {
                // System.out.println("[ExodarAPI] HWID pending approval");
            } else {
                // System.out.println("[ExodarAPI] Auth failed: " + response);
            }

            return false;

        } catch (Exception e) {
            // System.out.println("[ExodarAPI] Auth error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Read file bytes
     */
    private byte[] readFileBytes(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * XOR decrypt (same algorithm as launcher)
     */
    private byte[] xorDecrypt(byte[] data, byte[] key) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }

    /**
     * Generate HWID hash (simplified version for Java)
     * Uses same components as launcher: CPU, RAM, Disk
     */
    private String generateHWIDHash() {
        try {
            StringBuilder sb = new StringBuilder();

            // Get system properties as HWID components
            sb.append(System.getProperty("os.name", ""));
            sb.append(System.getProperty("os.arch", ""));
            sb.append(System.getProperty("user.name", ""));

            // Get computer name
            String computerName = System.getenv("COMPUTERNAME");
            if (computerName != null) sb.append(computerName);

            // Get processor identifier
            String procId = System.getenv("PROCESSOR_IDENTIFIER");
            if (procId != null) sb.append(procId);

            // Hash it
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            // Convert to hex
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            // System.out.println("[ExodarAPI] HWID generation error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get cached username
     */
    public String getCachedUsername() {
        return cachedUsername;
    }

    // ===============================================
    // CHALLENGE-RESPONSE AUTHENTICATION SYSTEM
    // ===============================================

    /**
     * Authenticate using direct HWID (independent of launcher)
     * If HWID not registered, tries to link it to user account
     */
    public CompletableFuture<Boolean> authenticateWithChallenge() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // System.out.println("[ExodarAPI] >>> AUTH: HWID2 authentication...");

                // Generate HWID2 (simple) - SHA256 of C: drive volume serial
                String hwid2 = cachedHwidHash;
                if (hwid2 == null || hwid2.isEmpty()) {
                    hwid2 = HWIDGenerator.getHWID2();
                    cachedHwidHash = hwid2;
                }

                if (hwid2 == null || hwid2.isEmpty()) {
                    // System.out.println("[ExodarAPI] Failed to generate HWID2");
                    return false;
                }

                // System.out.println("[ExodarAPI] HWID2: " + hwid2.substring(0, 32) + "...");

                // Try to authenticate with HWID2
                String response = tryHwid2Auth(hwid2);

                // If not registered, try to link the HWID2
                if (response != null && response.contains("not registered")) {
                    // System.out.println("[ExodarAPI] HWID2 not registered, attempting to link...");

                    // Get minecraft name if available
                    String mcName = currentMinecraftNick;
                    if (mcName == null || mcName.isEmpty()) {
                        try {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                            if (mc != null && mc.getSession() != null) {
                                mcName = mc.getSession().getUsername();
                            }
                        } catch (Exception ignored) {}
                    }

                    // Try to link HWID2
                    String linkJson = String.format(
                        "{\"hwid2\":\"%s\",\"minecraft_name\":\"%s\"}",
                        escapeJson(hwid2),
                        escapeJson(mcName != null ? mcName : "")
                    );

                    String linkResponse = httpPost("/mod/link-hwid", linkJson, null);
                    // System.out.println("[ExodarAPI] Link response: " + (linkResponse != null ? linkResponse.substring(0, Math.min(150, linkResponse.length())) : "null"));

                    if (linkResponse != null && linkResponse.contains("\"linked\":true")) {
                        // System.out.println("[ExodarAPI] HWID2 linked successfully! Retrying auth...");
                        response = tryHwid2Auth(hwid2);
                    } else {
                        // System.out.println("[ExodarAPI] Could not link HWID2. Open launcher first.");
                        return false;
                    }
                }

                if (response == null || response.isEmpty()) {
                    // System.out.println("[ExodarAPI] Empty auth response");
                    return false;
                }

                if (response.contains("\"kill\":true")) {
                    // System.out.println("[ExodarAPI] KILL SIGNAL during auth!");
                    if (commandCallback != null) {
                        commandCallback.onCloseCommand();
                    }
                    return false;
                }

                if (response.contains("\"error\"")) {
                    String error = extractJsonString(response, "error");
                    // System.out.println("[ExodarAPI] Auth error: " + error);
                    return false;
                }

                // Extract session info
                String token = extractJsonString(response, "token");
                String username = extractJsonString(response, "username");
                String nonce = extractJsonString(response, "nonce");
                int expiresIn = extractJsonInt(response, "expires_in", 3600);
                boolean isDev = extractJsonBoolean(response, "is_dev", false);
                boolean isBeta = extractJsonBoolean(response, "is_beta", false);

                if (token == null || token.isEmpty()) {
                    // System.out.println("[ExodarAPI] No token received");
                    return false;
                }

                // Store session
                modSessionToken = token;
                currentNonce = nonce;
                tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 60000;
                isAuthenticated = true;
                authRetryCount = 0;
                cachedUsername = username;

                // Update BuildInfo
                BuildInfo info = BuildInfo.getInstance();
                info.setSession(token, hwid2);

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyy");
                String date = sdf.format(new java.util.Date());

                // Check for build_config from API response (same as initFromLauncherCredentials)
                String buildType = "release";
                String buildName = "Release Build";
                boolean showUsername = true;
                boolean showDate = true;
                boolean isCustom = false;

                if (response.contains("\"build_config\"") && !response.contains("\"build_config\":null")) {
                    String customBuildText = extractJsonString(response, "custom_build_text");
                    String configBuildType = extractJsonString(response, "build_type");
                    showUsername = extractJsonBoolean(response, "show_username", true);
                    showDate = extractJsonBoolean(response, "show_date", true);

                    if (customBuildText != null && !customBuildText.isEmpty()) {
                        buildName = customBuildText;
                        buildType = configBuildType != null ? configBuildType : "custom";
                        isCustom = true;
                        // System.out.println("[ExodarAPI] Custom build: " + buildName);
                    } else if (configBuildType != null) {
                        buildType = configBuildType;
                        buildName = getBuildNameFromType(buildType);
                    }
                } else {
                    // Fallback to is_dev/is_beta
                    buildType = isDev ? "dev" : (isBeta ? "beta" : "release");
                    buildName = isDev ? "Developer Build" : (isBeta ? "Beta Build" : "Release Build");
                }

                info.update(buildType, buildName, username, date, showUsername, showDate, isCustom);

                // Set session token for cloud configs
                CloudConfigManager.setSessionToken(token);

                // System.out.println("[ExodarAPI] Authenticated: " + username + " (" + buildName + ")");
                // System.out.println("[ExodarAPI] Token expires in: " + expiresIn + "s");

                // Fetch custom names for spoofing
                fetchCustomNames();

                return true;

            } catch (Exception e) {
                // System.out.println("[ExodarAPI] Auth error: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }

    /**
     * Try HWID2 authentication (simple HWID - SHA256 of C: volume serial)
     */
    private String tryHwid2Auth(String hwid2) {
        String json = String.format(
            "{\"hwid2\":\"%s\",\"client_version\":\"%s\"}",
            escapeJson(hwid2),
            escapeJson(currentClientVersion)
        );
        String response = httpPost("/mod/hwid-direct", json, null);
        // System.out.println("[ExodarAPI] Auth response: " + (response != null ? response.substring(0, Math.min(150, response.length())) : "null"));
        return response;
    }

    /**
     * Check if current session is valid
     */
    public boolean isSessionValid() {
        return isAuthenticated &&
               modSessionToken != null &&
               System.currentTimeMillis() < tokenExpiresAt;
    }

    /**
     * Re-authenticate if session expired
     */
    private boolean ensureAuthenticated() {
        if (isSessionValid()) {
            return true;
        }

        if (authRetryCount >= MAX_AUTH_RETRIES) {
            // System.out.println("[ExodarAPI] Max auth retries reached, waiting...");
            authRetryCount = 0; // Reset after some time
            return false;
        }

        // System.out.println("[ExodarAPI] Session expired or invalid, re-authenticating...");
        authRetryCount++;

        try {
            return authenticateWithChallenge().get();
        } catch (Exception e) {
            // System.out.println("[ExodarAPI] Re-auth failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * SHA256 hash
     */
    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extract integer from JSON
     */
    private int extractJsonInt(String json, String key, int defaultValue) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return defaultValue;

        int startIndex = keyIndex + searchKey.length();
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        int endIndex = startIndex;
        while (endIndex < json.length() && (Character.isDigit(json.charAt(endIndex)) || json.charAt(endIndex) == '-')) {
            endIndex++;
        }

        try {
            return Integer.parseInt(json.substring(startIndex, endIndex));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get current session token
     */
    public String getSessionToken() {
        return modSessionToken;
    }

    /**
     * Fetch custom names from API and load into SpoofManager
     * Custom names allow admins to set display names for any player
     */
    public void fetchCustomNames() {
        executor.submit(() -> {
            try {
                String json = "{}";
                String response = httpPost("/custom-names", json, null);

                if (response == null || response.isEmpty()) {
                    // System.out.println("[ExodarAPI] Empty custom names response");
                    return;
                }

                // Parse custom_names array
                if (response.contains("\"custom_names\":[")) {
                    int start = response.indexOf("\"custom_names\":[") + 16;
                    // Use lastIndexOf to find the closing bracket (fixes issue with ] in display names)
                    int end = response.lastIndexOf("]");
                    if (start > 15 && end > start) {
                        String namesJson = response.substring(start, end);

                        if (namesJson.trim().isEmpty()) {
                            // System.out.println("[ExodarAPI] No custom names configured");
                            return;
                        }

                        // Parse individual entries
                        String[] entries = namesJson.split("\\},\\{");
                        int count = 0;

                        for (String entry : entries) {
                            entry = entry.replace("{", "").replace("}", "");

                            String original = extractJsonString("{" + entry + "}", "original");
                            String display = extractJsonString("{" + entry + "}", "display");
                            boolean useSkin = extractJsonBoolean("{" + entry + "}", "use_skin", false);
                            String skinPlayer = extractJsonString("{" + entry + "}", "skin_player");

                            if (original != null && !original.isEmpty() && display != null && !display.isEmpty()) {
                                // Add to SpoofManager
                                io.github.exodar.spoof.SpoofManager.addSpoof(original, display, useSkin, skinPlayer);
                                count++;
                            }
                        }

                        // System.out.println("[ExodarAPI] Loaded " + count + " custom names from API");
                    }
                }
            } catch (Exception e) {
                // System.out.println("[ExodarAPI] Error fetching custom names: " + e.getMessage());
            }
        });
    }

    // ===============================================
    // HEARTBEAT AND COMMAND SYSTEM
    // ===============================================

    private java.util.concurrent.ScheduledExecutorService heartbeatExecutor;
    private String currentMinecraftNick = null;
    private String currentMinecraftUuid = null;
    private int heartbeatCounter = 0;
    private int heartbeatFailureCount = 0;
    private static final int MAX_HEARTBEAT_FAILURES = 10; // Only kill after 10 consecutive failures
    private static final int CUSTOM_NAMES_REFRESH_INTERVAL = 1; // Refresh every heartbeat for fast spoof updates // Refresh every 30 heartbeats (2.5 min with 5s interval)
    private String currentServerIp = null;
    private String currentServerName = null;
    private String currentClientVersion = "1.0.0";
    private CommandCallback commandCallback = null;

    /**
     * Callback interface for executing commands from the server
     */
    public interface CommandCallback {
        void onChatCommand(String message);
        void onCloseCommand();
        void onKickCommand(String reason);
        void onCustomCommand(String command);
    }

    /**
     * Set the callback for command execution
     */
    public void setCommandCallback(CommandCallback callback) {
        this.commandCallback = callback;
    }

    /**
     * Update session info (call this when player joins a server)
     */
    public void updateSessionInfo(String minecraftNick, String minecraftUuid, String serverIp, String serverName) {
        this.currentMinecraftNick = minecraftNick;
        this.currentMinecraftUuid = minecraftUuid;
        this.currentServerIp = serverIp;
        this.currentServerName = serverName;
        // System.out.println("[ExodarAPI] Session info updated: " + minecraftNick + " on " + serverIp);
    }

    /**
     * Clear session info (call this when player disconnects)
     */
    public void clearSessionInfo() {
        this.currentMinecraftNick = null;
        this.currentMinecraftUuid = null;
        this.currentServerIp = null;
        this.currentServerName = null;
    }

    /**
     * Start the heartbeat loop (sends heartbeat every 30 seconds)
     * First authenticates with challenge-response, then starts periodic heartbeat
     */
    public void startHeartbeat() {
        if (heartbeatExecutor != null) {
            return; // Already running
        }

        // System.out.println("[ExodarAPI] Starting heartbeat system...");

        // First authenticate with challenge-response
        authenticateWithChallenge().thenAccept(success -> {
            if (!success) {
                // System.out.println("[ExodarAPI] Initial auth failed, heartbeat will retry on first tick");
            }

            // Start heartbeat loop regardless (it will handle re-auth)
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
            heartbeatExecutor.scheduleAtFixedRate(() -> {
                try {
                    sendHeartbeat();
                    fetchAndExecuteCommands();

                    // Refresh custom names periodically so all users see updated spoofs
                    heartbeatCounter++;
                    if (heartbeatCounter >= CUSTOM_NAMES_REFRESH_INTERVAL) {
                        heartbeatCounter = 0;
                        fetchCustomNames();
                    }
                } catch (Exception e) {
                    // System.out.println("[ExodarAPI] Heartbeat error: " + e.getMessage());
                }
            }, success ? 0 : 2, 5, java.util.concurrent.TimeUnit.SECONDS);

            // System.out.println("[ExodarAPI] Heartbeat started (5s interval)");
        });
    }

    /**
     * Stop the heartbeat loop
     */
    public void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            heartbeatExecutor = null;
            // System.out.println("[ExodarAPI] Heartbeat stopped");
        }
    }

    /**
     * Send heartbeat to API with session info
     * Uses independent mod tokens with dynamic nonces for replay protection
     */
    private void sendHeartbeat() {
        try {
            // Ensure we're authenticated with independent mod session
            if (!ensureAuthenticated()) {
                // System.out.println("[ExodarAPI] Not authenticated, skipping heartbeat");
                return;
            }

            String token = modSessionToken;
            String hwidHash = cachedHwidHash != null ? cachedHwidHash : HWIDGenerator.getHWIDHash();

            if (token == null || token.isEmpty()) {
                // System.out.println("[ExodarAPI] No token for heartbeat");
                return;
            }

            // Get current nick/uuid from Minecraft (in case player changed account)
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                if (mc != null && mc.getSession() != null) {
                    String newNick = mc.getSession().getUsername();
                    String newUuid = mc.getSession().getPlayerID();
                    if (newNick != null && !newNick.isEmpty()) {
                        if (!newNick.equals(currentMinecraftNick)) {
                            // System.out.println("[ExodarAPI] Nick changed: " + currentMinecraftNick + " -> " + newNick);
                        }
                        currentMinecraftNick = newNick;
                    }
                    if (newUuid != null && !newUuid.isEmpty()) {
                        currentMinecraftUuid = newUuid;
                    }
                }
            } catch (Exception e) {
                // Ignore - use cached values
            }

            // Use timestamp and current nonce for replay protection
            long timestamp = System.currentTimeMillis();

            String json = String.format(
                "{\"token\":\"%s\",\"hwid_hash\":\"%s\",\"minecraft_nick\":%s,\"minecraft_uuid\":%s,\"server_ip\":%s,\"server_name\":%s,\"client_version\":\"%s\",\"nonce\":\"%s\",\"timestamp\":%d}",
                escapeJson(token),
                escapeJson(hwidHash),
                currentMinecraftNick != null ? "\"" + escapeJson(currentMinecraftNick) + "\"" : "null",
                currentMinecraftUuid != null ? "\"" + escapeJson(currentMinecraftUuid) + "\"" : "null",
                currentServerIp != null ? "\"" + escapeJson(currentServerIp) + "\"" : "null",
                currentServerName != null ? "\"" + escapeJson(currentServerName) + "\"" : "null",
                escapeJson(currentClientVersion),
                escapeJson(currentNonce != null ? currentNonce : ""),
                timestamp
            );

            // System.out.println("[ExodarAPI] >>> HEARTBEAT nick=" + currentMinecraftNick + ", server=" + currentServerIp);

            String response = httpPost("/mod-heartbeat", json, token);

            if (response == null || response.isEmpty()) {
                heartbeatFailureCount++;
                // System.out.println("[ExodarAPI] <<< HEARTBEAT: Empty response (failure " + heartbeatFailureCount + "/" + MAX_HEARTBEAT_FAILURES + ")");
                return;
            }

            // System.out.println("[ExodarAPI] <<< HEARTBEAT OK");

            if (response.contains("\"valid\":true")) {
                // Reset failure counter on success
                heartbeatFailureCount = 0;

                // Update nonce for next heartbeat (anti-replay)
                String newNonce = extractJsonString(response, "nonce");
                if (newNonce != null && !newNonce.isEmpty()) {
                    currentNonce = newNonce;
                }

                // Check for auto-spoof
                if (response.contains("\"user_display\"")) {
                    String displayNick = extractJsonString(response, "minecraft_nick");
                    String displayName = extractJsonString(response, "display_name");
                    boolean useSkin = extractJsonBoolean(response, "use_skin", false);
                    String skinPlayer = extractJsonString(response, "skin_player");

                    if (displayNick != null && displayName != null && !displayNick.isEmpty() && !displayName.isEmpty()) {
                        io.github.exodar.spoof.SpoofManager.addSpoof(displayNick, displayName, useSkin, skinPlayer);
                        // System.out.println("[ExodarAPI] Auto-spoof: " + displayNick + " -> " + displayName);
                    }
                }

                // Check for build config update from server
                if (response.contains("\"build_config\"") && !response.contains("\"build_config\":null")) {
                    String customBuildText = extractJsonString(response, "custom_build_text");
                    String buildType = extractJsonString(response, "build_type");
                    boolean showUsername = extractJsonBoolean(response, "show_username", true);
                    boolean showDate = extractJsonBoolean(response, "show_date", true);

                    // System.out.println("[ExodarAPI] Build config received: type=" + buildType + ", text=" + customBuildText);

                    if (buildType != null && !buildType.isEmpty()) {
                        String buildName = (customBuildText != null && !customBuildText.isEmpty())
                            ? customBuildText
                            : getBuildNameFromType(buildType);
                        boolean isCustom = customBuildText != null && !customBuildText.isEmpty();

                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("ddMMyy");
                        String date = sdf.format(new java.util.Date());

                        BuildInfo.getInstance().update(buildType, buildName, cachedUsername, date, showUsername, showDate, isCustom);
                        // System.out.println("[ExodarAPI] Build config updated: " + buildName);
                    }
                }
            } else if (response.contains("\"kill\":true")) {
                heartbeatFailureCount++;
                // System.out.println("[ExodarAPI] KILL SIGNAL from server! (failure " + heartbeatFailureCount + "/" + MAX_HEARTBEAT_FAILURES + ")");
                if (heartbeatFailureCount >= MAX_HEARTBEAT_FAILURES && commandCallback != null) {
                    // System.out.println("[ExodarAPI] Max failures reached, executing kill!");
                    commandCallback.onCloseCommand();
                }
            } else if (response.contains("\"error\"") || response.contains("Invalid") || response.contains("expired")) {
                heartbeatFailureCount++;
                // System.out.println("[ExodarAPI] Session invalid (failure " + heartbeatFailureCount + "/" + MAX_HEARTBEAT_FAILURES + "), forcing re-auth");
                isAuthenticated = false;
                modSessionToken = null;
                currentNonce = null;
            }
        } catch (Exception e) {
            // System.out.println("[ExodarAPI] Heartbeat error: " + e.getMessage());
        }
    }

    /**
     * Fetch pending commands from API and execute them
     */
    private void fetchAndExecuteCommands() {
        try {
            if (!isSessionValid()) {
                return; // Skip if not authenticated
            }

            BuildInfo info = BuildInfo.getInstance();
            String token = modSessionToken != null ? modSessionToken : info.getToken();
            String hwidHash = cachedHwidHash != null ? cachedHwidHash : info.getHwidHash();

            if (token == null || token.isEmpty()) {
                return;
            }

            String json = String.format(
                "{\"token\":\"%s\",\"hwid_hash\":\"%s\"}",
                escapeJson(token),
                escapeJson(hwidHash != null ? hwidHash : "")
            );

            String response = httpPost("/commands", json, token);

            if (response == null || response.isEmpty()) {
                return;
            }

            // Check for kill signal in response
            if (response.contains("\"kill\":true")) {
                // System.out.println("[ExodarAPI] Kill signal in commands response!");
                if (commandCallback != null) {
                    commandCallback.onCloseCommand();
                }
                return;
            }

            // Parse commands array
            if (response.contains("\"commands\":[") && commandCallback != null) {
                int start = response.indexOf("\"commands\":[") + 12;
                int end = response.lastIndexOf("]");
                if (start > 11 && end > start) {
                    String commandsJson = response.substring(start, end);

                    // Skip if empty array
                    if (commandsJson.trim().isEmpty()) {
                        return;
                    }

                    // Parse individual commands (simple parsing)
                    String[] commands = commandsJson.split("\\},\\{");
                    for (String cmd : commands) {
                        cmd = cmd.replace("{", "").replace("}", "");

                        String commandType = extractJsonString("{" + cmd + "}", "command_type");
                        String commandData = extractJsonString("{" + cmd + "}", "command_data");

                        if (commandType != null && !commandType.isEmpty()) {
                            // System.out.println("[ExodarAPI] Received command: " + commandType + " data=" + commandData);
                            executeCommand(commandType, commandData);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // System.out.println("[ExodarAPI] Command fetch error: " + e.getMessage());
        }
    }

    /**
     * Execute a command from the server
     */
    private void executeCommand(String type, String data) {
        if (commandCallback == null) {
            // System.out.println("[ExodarAPI] No command callback set, ignoring: " + type);
            return;
        }

        // System.out.println("[ExodarAPI] Executing command: " + type + " - " + data);

        switch (type) {
            case "chat":
                if (data != null && !data.isEmpty()) {
                    commandCallback.onChatCommand(data);
                }
                break;
            case "close":
                commandCallback.onCloseCommand();
                break;
            case "kick":
                commandCallback.onKickCommand(data != null ? data : "Kicked by admin");
                break;
            case "custom":
                if (data != null && !data.isEmpty()) {
                    commandCallback.onCustomCommand(data);
                }
                break;
            default:
                // System.out.println("[ExodarAPI] Unknown command type: " + type);
        }
    }

    /**
     * Get build name from build type for display
     */
    private String getBuildNameFromType(String buildType) {
        if (buildType == null) return "Unknown";
        switch (buildType.toLowerCase()) {
            case "release": return "Release";
            case "beta": return "Beta";
            case "alpha": return "Alpha";
            case "dev": return "Dev";
            case "private": return "Private";
            case "vip": return "VIP";
            case "staff": return "Staff";
            default: return buildType;
        }
    }
}
