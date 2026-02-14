/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.config;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.ui.ModuleNotification;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UserConfig - Stores user-specific settings that shouldn't be shared
 * - Hidden modules (for ArrayList)
 * - Passwords (AutoRegister)
 * - Other personal preferences
 *
 * Saved to: %APPDATA%\.exodar\USER (not synced by OneDrive)
 */
public class UserConfig {
    // Use AppData instead of Documents to avoid OneDrive sync issues
    private static final String CONFIG_DIR = getConfigDir();
    private static final String OLD_CONFIG_DIR = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Exodar";
    private static final String USER_FILE = "USER";

    // Increment this to force reset on update
    private static final int CONFIG_VERSION = 3;

    // Retry settings for file operations (OneDrive can lock files)
    private static final int MAX_SAVE_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 100;

    private static String getConfigDir() {
        // Try APPDATA first (Windows)
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            return appData + File.separator + ".exodar";
        }
        // Fallback to user home
        return System.getProperty("user.home") + File.separator + ".exodar";
    }

    private static UserConfig instance;

    // Hidden modules (won't show in ArrayList)
    private final Set<String> hiddenModules = new HashSet<>();

    // Stored passwords (moduleName -> password)
    private final Map<String, String> passwords = new HashMap<>();

    // Other user preferences
    private final Map<String, String> preferences = new HashMap<>();

    private UserConfig() {
        // Create config directory if needed
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        } catch (IOException e) {
            System.out.println("[UserConfig] Error creating config directory: " + e.getMessage());
        }

        // Migrate from old Documents location if exists
        migrateFromOldLocation();
    }

    /**
     * Migrate config from old Documents\Exodar location to new AppData\Exodar
     */
    private void migrateFromOldLocation() {
        try {
            Path oldFile = Paths.get(OLD_CONFIG_DIR + File.separator + USER_FILE);
            Path newFile = Paths.get(CONFIG_DIR + File.separator + USER_FILE);

            // If old file exists and new file doesn't, migrate
            if (Files.exists(oldFile) && !Files.exists(newFile)) {
                System.out.println("[UserConfig] Migrating from Documents to AppData...");
                Files.copy(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
                // Delete old file after successful copy
                Files.deleteIfExists(oldFile);
                System.out.println("[UserConfig] Migration complete");
            }
            // If both exist, prefer new location and delete old
            else if (Files.exists(oldFile) && Files.exists(newFile)) {
                Files.deleteIfExists(oldFile);
            }
        } catch (Exception e) {
            System.out.println("[UserConfig] Migration error (non-fatal): " + e.getMessage());
        }
    }

    public static UserConfig getInstance() {
        if (instance == null) {
            instance = new UserConfig();
            instance.load(); // Auto-load on first access
        }
        return instance;
    }

    // =====================================================
    // HIDDEN MODULES (ArrayList)
    // =====================================================

    /**
     * Hide a module from ArrayList
     */
    public void hideModule(String moduleName) {
        hiddenModules.add(moduleName.toLowerCase());
        save();
    }

    /**
     * Unhide a module from ArrayList
     */
    public void unhideModule(String moduleName) {
        hiddenModules.remove(moduleName.toLowerCase());
        save();
    }

    /**
     * Hide all modules
     */
    public void hideAllModules(ModuleManager mm) {
        if (mm == null) return;
        for (Module m : mm.getModules()) {
            hiddenModules.add(m.getName().toLowerCase());
        }
        save();
    }

    /**
     * Unhide all modules
     */
    public void unhideAllModules() {
        hiddenModules.clear();
        save();
    }

    /**
     * Check if module is hidden
     */
    public boolean isModuleHidden(String moduleName) {
        return hiddenModules.contains(moduleName.toLowerCase());
    }

    /**
     * Check if module is hidden (by Module object)
     */
    public boolean isModuleHidden(Module module) {
        return module != null && hiddenModules.contains(module.getName().toLowerCase());
    }

    /**
     * Get all hidden modules
     */
    public Set<String> getHiddenModules() {
        return new HashSet<>(hiddenModules);
    }

    /**
     * Get count of hidden modules
     */
    public int getHiddenCount() {
        return hiddenModules.size();
    }

    // =====================================================
    // PASSWORDS (AutoRegister, etc.)
    // =====================================================

    /**
     * Store password for a module
     */
    public void setPassword(String moduleName, String password) {
        if (password == null || password.isEmpty()) {
            passwords.remove(moduleName.toLowerCase());
        } else {
            passwords.put(moduleName.toLowerCase(), password);
        }
        save();
    }

    /**
     * Get password for a module
     */
    public String getPassword(String moduleName) {
        return passwords.get(moduleName.toLowerCase());
    }

    /**
     * Check if password exists for module
     */
    public boolean hasPassword(String moduleName) {
        return passwords.containsKey(moduleName.toLowerCase());
    }

    // =====================================================
    // PREFERENCES (other user settings)
    // =====================================================

    /**
     * Set a preference
     */
    public void setPreference(String key, String value) {
        if (value == null) {
            preferences.remove(key.toLowerCase());
        } else {
            preferences.put(key.toLowerCase(), value);
        }
        save();
    }

    /**
     * Get a preference
     */
    public String getPreference(String key) {
        return preferences.get(key.toLowerCase());
    }

    /**
     * Get preference with default value
     */
    public String getPreference(String key, String defaultValue) {
        return preferences.getOrDefault(key.toLowerCase(), defaultValue);
    }

    // =====================================================
    // SAVE / LOAD
    // =====================================================

    /**
     * Save user config to file
     */
    public void save() {
        try {
            StringBuilder data = new StringBuilder();

            // Save version first (for forced resets on updates)
            data.append("VERSION|").append(CONFIG_VERSION).append("\n");

            // Save hidden modules
            for (String module : hiddenModules) {
                data.append("HIDDEN|").append(module).append("\n");
            }

            // Save passwords (simple XOR obfuscation - not secure, just not plaintext)
            for (Map.Entry<String, String> entry : passwords.entrySet()) {
                String obfuscated = obfuscate(entry.getValue());
                data.append("PASSWORD|").append(entry.getKey()).append("|").append(obfuscated).append("\n");
            }

            // Save preferences
            for (Map.Entry<String, String> entry : preferences.entrySet()) {
                data.append("PREF|").append(entry.getKey()).append("|").append(entry.getValue()).append("\n");
            }

            // Encode to Base64
            String encoded = Base64.getEncoder().encodeToString(data.toString().getBytes("UTF-8"));

            // Write to file with retry logic (OneDrive can lock files temporarily)
            String filePath = CONFIG_DIR + File.separator + USER_FILE;
            boolean saved = false;
            Exception lastError = null;

            for (int attempt = 1; attempt <= MAX_SAVE_RETRIES; attempt++) {
                try {
                    Files.write(Paths.get(filePath), encoded.getBytes("UTF-8"));
                    saved = true;
                    break;
                } catch (Exception writeError) {
                    lastError = writeError;
                    if (attempt < MAX_SAVE_RETRIES) {
                        System.out.println("[UserConfig] Save attempt " + attempt + " failed, retrying...");
                        try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ignored) {}
                    }
                }
            }

            if (saved) {
                System.out.println("[UserConfig] Saved user config (v" + CONFIG_VERSION + ") to " + CONFIG_DIR);
            } else {
                System.out.println("[UserConfig] Failed to save after " + MAX_SAVE_RETRIES + " attempts: " +
                    (lastError != null ? lastError.getMessage() : "unknown error"));
            }

        } catch (Exception e) {
            System.out.println("[UserConfig] Error saving: " + e.getMessage());
        }
    }

    /**
     * Load user config from file
     * Robust loading: ignores invalid lines, doesn't fail on corrupt data
     * Forces reset if config version doesn't match
     */
    public void load() {
        String filePath = CONFIG_DIR + File.separator + USER_FILE;

        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("[UserConfig] No user config found, using defaults");
            return;
        }

        // Parse into temporary collections (don't touch current data until successful)
        Set<String> tempHidden = new HashSet<>();
        Map<String, String> tempPasswords = new HashMap<>();
        Map<String, String> tempPreferences = new HashMap<>();
        int fileVersion = 0;

        try {
            // Read file
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));

            // Try to decode Base64
            String decoded;
            try {
                decoded = new String(Base64.getDecoder().decode(fileBytes), "UTF-8");
            } catch (Exception e) {
                System.out.println("[UserConfig] Corrupt file (invalid Base64), resetting...");
                // File is corrupt, delete it and start fresh
                Files.deleteIfExists(Paths.get(filePath));
                return;
            }

            // Parse lines - each line in its own try-catch to handle partial corruption
            String[] lines = decoded.split("\n");
            int validLines = 0;
            int skippedLines = 0;

            for (String line : lines) {
                try {
                    if (line == null || line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|");
                    if (parts.length < 2) {
                        skippedLines++;
                        continue;
                    }

                    String type = parts[0];

                    // Check version first
                    if (type.equals("VERSION")) {
                        try {
                            fileVersion = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {
                            fileVersion = 0;
                        }
                        continue;
                    }

                    if (type.equals("HIDDEN")) {
                        String moduleName = parts[1].toLowerCase().trim();
                        if (!moduleName.isEmpty()) {
                            tempHidden.add(moduleName);
                            validLines++;
                        }

                    } else if (type.equals("PASSWORD") && parts.length >= 3) {
                        String moduleName = parts[1].toLowerCase().trim();
                        String obfuscated = parts[2];
                        if (!moduleName.isEmpty() && !obfuscated.isEmpty()) {
                            String password = deobfuscate(obfuscated);
                            if (password != null && !password.isEmpty()) {
                                tempPasswords.put(moduleName, password);
                                validLines++;
                            }
                        }

                    } else if (type.equals("PREF") && parts.length >= 3) {
                        String key = parts[1].toLowerCase().trim();
                        String value = parts[2];
                        if (!key.isEmpty()) {
                            tempPreferences.put(key, value);
                            validLines++;
                        }
                    }
                    // Unknown types are silently ignored (forward compatibility)

                } catch (Exception lineError) {
                    // Skip this line but continue loading
                    skippedLines++;
                    System.out.println("[UserConfig] Skipped invalid line: " + lineError.getMessage());
                }
            }

            // Check version - force reset if version mismatch
            if (fileVersion != CONFIG_VERSION) {
                System.out.println("[UserConfig] Version mismatch (file: " + fileVersion + ", current: " + CONFIG_VERSION + "), resetting config...");
                Files.deleteIfExists(Paths.get(filePath));
                // Save fresh config with new version
                save();
                return;
            }

            // Apply loaded data
            hiddenModules.clear();
            hiddenModules.addAll(tempHidden);

            passwords.clear();
            passwords.putAll(tempPasswords);

            preferences.clear();
            preferences.putAll(tempPreferences);

            System.out.println("[UserConfig] Loaded user config v" + fileVersion + " (" + validLines + " valid, " + skippedLines + " skipped)");

            // If we skipped lines, re-save to clean up the file
            if (skippedLines > 0) {
                System.out.println("[UserConfig] Cleaning up corrupt entries...");
                save();
            }

        } catch (Exception e) {
            System.out.println("[UserConfig] Error loading config: " + e.getMessage());
            // Don't clear existing data on load failure - keep what we have
        }
    }

    /**
     * Validate hidden modules against actual modules
     * Removes entries for modules that no longer exist
     * @param moduleManager The module manager to validate against
     */
    public void validateAndCleanup(io.github.exodar.module.ModuleManager moduleManager) {
        if (moduleManager == null) return;

        // Get all valid module names (lowercase)
        Set<String> validModuleNames = new HashSet<>();
        for (Module m : moduleManager.getModules()) {
            validModuleNames.add(m.getName().toLowerCase());
        }

        // Remove hidden modules that don't exist
        int removed = 0;
        Iterator<String> it = hiddenModules.iterator();
        while (it.hasNext()) {
            String moduleName = it.next();
            if (!validModuleNames.contains(moduleName)) {
                it.remove();
                removed++;
            }
        }

        // Remove passwords for modules that don't exist
        Iterator<String> pwIt = passwords.keySet().iterator();
        while (pwIt.hasNext()) {
            String moduleName = pwIt.next();
            if (!validModuleNames.contains(moduleName)) {
                pwIt.remove();
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("[UserConfig] Cleaned up " + removed + " obsolete entries");
            save();
        }
    }

    /**
     * Simple XOR obfuscation (not security, just not plaintext)
     */
    private String obfuscate(String input) {
        if (input == null) return "";
        byte[] bytes = input.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ 0x5A ^ i);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Simple XOR deobfuscation
     */
    private String deobfuscate(String input) {
        if (input == null || input.isEmpty()) return "";
        try {
            byte[] bytes = Base64.getDecoder().decode(input);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ 0x5A ^ i);
            }
            return new String(bytes);
        } catch (Exception e) {
            return "";
        }
    }
}
