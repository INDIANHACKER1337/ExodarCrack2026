/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.config;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.*;
import io.github.exodar.ui.ModuleNotification;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Config manager - saves/loads configs with Base64 encoding
 * Configs saved to: %APPDATA%\.exodar\configs\ (not synced by OneDrive)
 */
public class ConfigManager {
    private static final String CONFIG_DIR = getConfigDir();
    private static final String OLD_CONFIG_DIR = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Exodar";
    private ModuleManager moduleManager;

    private static String getConfigDir() {
        // Try APPDATA first (Windows)
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            return appData + File.separator + ".exodar" + File.separator + "configs";
        }
        // Fallback to user home
        return System.getProperty("user.home") + File.separator + ".exodar" + File.separator + "configs";
    }

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;

        // Create config directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            System.out.println("[ConfigManager] Config directory: " + CONFIG_DIR);
        } catch (IOException e) {
            System.out.println("[ConfigManager] Error creating config directory: " + e.getMessage());
        }

        // Migrate configs from old Documents location
        migrateFromOldLocation();

        // Create Default config if it doesn't exist (first-time setup)
        createDefaultConfigIfNeeded();
    }

    /**
     * Create "Default" config if it doesn't exist
     * This serves as a base config users can always fall back to
     */
    private void createDefaultConfigIfNeeded() {
        if (!configExists("Default")) {
            System.out.println("[ConfigManager] Creating Default config (first-time setup)...");
            saveConfig("Default");
            System.out.println("[ConfigManager] Default config created");
        }
    }

    /**
     * Migrate configs from old locations to new .exodar\configs\ folder
     */
    private void migrateFromOldLocation() {
        // Migrate from Documents\Exodar (very old location)
        migrateFromDirectory(OLD_CONFIG_DIR, "Documents");

        // Migrate from .exodar root (previous location before configs subfolder)
        String exodarRoot = CONFIG_DIR.replace(File.separator + "configs", "");
        migrateFromDirectory(exodarRoot, ".exodar root");
    }

    /**
     * Migrate config files from a directory to the new configs folder
     */
    private void migrateFromDirectory(String sourceDir, String sourceName) {
        try {
            File oldDir = new File(sourceDir);
            if (!oldDir.exists() || !oldDir.isDirectory()) return;

            // Don't migrate from ourselves
            if (oldDir.getAbsolutePath().equals(new File(CONFIG_DIR).getAbsolutePath())) return;

            File[] oldFiles = oldDir.listFiles();
            if (oldFiles == null || oldFiles.length == 0) return;

            boolean migrated = false;
            for (File oldFile : oldFiles) {
                if (!oldFile.isFile()) continue;
                // Skip system files
                String name = oldFile.getName();
                if (name.equals("USER") || name.equals("SESSION") || name.equals("HWID")) continue;
                if (name.equalsIgnoreCase("user") || name.equalsIgnoreCase("session") || name.equalsIgnoreCase("hwid")) continue;
                // Skip directories (like configs, logs)
                if (oldFile.isDirectory()) continue;

                Path newPath = Paths.get(CONFIG_DIR + File.separator + name);
                if (!Files.exists(newPath)) {
                    if (!migrated) {
                        System.out.println("[ConfigManager] Migrating configs from " + sourceName + "...");
                        migrated = true;
                    }
                    Files.copy(oldFile.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[ConfigManager] Migrated: " + name);
                }
                // Delete old file after migration
                oldFile.delete();
            }

            // Try to delete old directory if empty (only for Documents\Exodar)
            if (sourceDir.equals(OLD_CONFIG_DIR)) {
                String[] remaining = oldDir.list();
                if (remaining != null && remaining.length == 0) {
                    oldDir.delete();
                }
            }

            if (migrated) {
                System.out.println("[ConfigManager] Migration from " + sourceName + " complete");
            }
        } catch (Exception e) {
            System.out.println("[ConfigManager] Migration error (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Save current config to file
     * @param configName Name of config (without extension)
     * @return true if successful
     */
    public boolean saveConfig(String configName) {
        try {
            StringBuilder data = new StringBuilder();

            // Save each module
            for (Module module : moduleManager.getModules()) {
                // Format: MODULE|name|enabled|toggleBind|holdBind
                data.append("MODULE|");
                data.append(module.getName()).append("|");
                data.append(module.isEnabled() ? "1" : "0").append("|");
                data.append(module.getToggleBind()).append("|");
                data.append(module.getHoldBind()).append("\n");

                // Save settings for this module
                for (Setting setting : module.getSettings()) {
                    if (setting instanceof TickSetting) {
                        TickSetting tick = (TickSetting) setting;
                        data.append("TICK|");
                        data.append(module.getName()).append("|");
                        data.append(tick.getName()).append("|");
                        data.append(tick.isEnabled() ? "1" : "0").append("\n");

                    } else if (setting instanceof SliderSetting) {
                        SliderSetting slider = (SliderSetting) setting;
                        data.append("SLIDER|");
                        data.append(module.getName()).append("|");
                        data.append(slider.getName()).append("|");
                        data.append(slider.getValue()).append("\n");

                    } else if (setting instanceof DoubleSliderSetting) {
                        DoubleSliderSetting dslider = (DoubleSliderSetting) setting;
                        data.append("DSLIDER|");
                        data.append(module.getName()).append("|");
                        data.append(dslider.getName()).append("|");
                        data.append(dslider.getValueMin()).append("|");
                        data.append(dslider.getValueMax()).append("\n");

                    } else if (setting instanceof ModeSetting) {
                        ModeSetting mode = (ModeSetting) setting;
                        data.append("MODE|");
                        data.append(module.getName()).append("|");
                        data.append(mode.getName()).append("|");
                        data.append(mode.getSelected()).append("\n");

                    } else if (setting instanceof KeybindSetting) {
                        KeybindSetting keybind = (KeybindSetting) setting;
                        data.append("KEYBIND|");
                        data.append(module.getName()).append("|");
                        data.append(keybind.getName()).append("|");
                        data.append(keybind.getKeyCode()).append("\n");

                    } else if (setting instanceof ColorSetting) {
                        ColorSetting color = (ColorSetting) setting;
                        // Format: COLOR|moduleName|settingName|red|green|blue|alpha|rainbow
                        data.append("COLOR|");
                        data.append(module.getName()).append("|");
                        data.append(color.getName()).append("|");
                        data.append(color.getRed()).append("|");
                        data.append(color.getGreen()).append("|");
                        data.append(color.getBlue()).append("|");
                        data.append(color.getAlpha()).append("|");
                        data.append(color.isRainbow() ? "1" : "0").append("\n");

                    } else if (setting instanceof TextSetting) {
                        TextSetting text = (TextSetting) setting;
                        // Format: TEXT|moduleName|settingName|value (Base64 encoded to handle special chars)
                        String encodedValue = Base64.getEncoder().encodeToString(text.getValue().getBytes("UTF-8"));
                        data.append("TEXT|");
                        data.append(module.getName()).append("|");
                        data.append(text.getName()).append("|");
                        data.append(encodedValue).append("\n");
                    }
                }
            }

            // Encode to Base64 (single line, hard to read)
            String encoded = Base64.getEncoder().encodeToString(data.toString().getBytes("UTF-8"));

            // Write to file
            String filePath = CONFIG_DIR + File.separator + configName;
            boolean isNew = !Files.exists(Paths.get(filePath));
            Files.write(Paths.get(filePath), encoded.getBytes("UTF-8"));

            System.out.println("[ConfigManager] Saved config: " + configName);

            // Show notification
            if (isNew) {
                ModuleNotification.addNotification("Config Created: " + configName, true);
            } else {
                ModuleNotification.addNotification("Config Saved: " + configName, true);
            }

            return true;

        } catch (Exception e) {
            System.out.println("[ConfigManager] Error saving config: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load config from file
     * Robust loading: each line is parsed in its own try-catch
     * Missing modules/settings are silently ignored (forward compatibility)
     * @param configName Name of config (without extension)
     * @return true if successful
     */
    public boolean loadConfig(String configName) {
        String filePath = CONFIG_DIR + File.separator + configName;

        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("[ConfigManager] Config not found: " + configName);
            return false;
        }

        int loadedSettings = 0;
        int skippedSettings = 0;

        try {
            // Read and decode Base64
            byte[] encoded = Files.readAllBytes(Paths.get(filePath));
            String decoded;
            try {
                decoded = new String(Base64.getDecoder().decode(encoded), "UTF-8");
            } catch (Exception e) {
                System.out.println("[ConfigManager] Corrupt config file (invalid Base64): " + configName);
                ModuleNotification.addNotification("Config corrupt: " + configName, false);
                return false;
            }

            // IMPORTANT: First disable ALL modules to ensure clean state when switching configs
            // This prevents modules from staying enabled when they should be disabled
            System.out.println("[ConfigManager] Disabling all modules before loading config...");
            for (Module module : moduleManager.getModules()) {
                if (module.isEnabled()) {
                    try {
                        module.setEnabled(false);
                    } catch (Exception e) {
                        // Ignore disable errors
                    }
                }
            }

            // Parse lines - each in its own try-catch
            String[] lines = decoded.split("\n");

            for (String line : lines) {
                try {
                    if (line == null || line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|");
                    if (parts.length < 2) {
                        skippedSettings++;
                        continue;
                    }

                    String type = parts[0];

                    if (type.equals("MODULE")) {
                        // Format: MODULE|name|enabled|toggleBind|holdBind
                        if (parts.length >= 5) {
                            String moduleName = parts[1];
                            Module module = moduleManager.getModuleByName(moduleName);
                            if (module != null) {
                                boolean enabled = parts[2].equals("1");
                                int toggleBind = Integer.parseInt(parts[3]);
                                int holdBind = Integer.parseInt(parts[4]);
                                module.setEnabled(enabled);
                                module.setToggleBind(toggleBind);
                                module.setHoldBind(holdBind);
                                loadedSettings++;
                            } else {
                                skippedSettings++; // Module no longer exists
                            }
                        }

                    } else if (type.equals("TICK")) {
                        // Format: TICK|moduleName|settingName|enabled
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof TickSetting) {
                                    ((TickSetting) setting).setEnabled(parts[3].equals("1"));
                                    loadedSettings++;
                                } else {
                                    skippedSettings++; // Setting no longer exists or changed type
                                }
                            } else {
                                skippedSettings++;
                            }
                        }

                    } else if (type.equals("SLIDER")) {
                        // Format: SLIDER|moduleName|settingName|value
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof SliderSetting) {
                                    double value = Double.parseDouble(parts[3]);
                                    SliderSetting slider = (SliderSetting) setting;
                                    // Clamp value to current min/max (in case bounds changed)
                                    value = Math.max(slider.getMin(), Math.min(slider.getMax(), value));
                                    slider.setValue(value);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }

                    } else if (type.equals("DSLIDER")) {
                        // Format: DSLIDER|moduleName|settingName|minValue|maxValue
                        if (parts.length >= 5) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof DoubleSliderSetting) {
                                    double minValue = Double.parseDouble(parts[3]);
                                    double maxValue = Double.parseDouble(parts[4]);
                                    DoubleSliderSetting dslider = (DoubleSliderSetting) setting;
                                    // Clamp values to current bounds
                                    minValue = Math.max(dslider.getMin(), Math.min(dslider.getMax(), minValue));
                                    maxValue = Math.max(dslider.getMin(), Math.min(dslider.getMax(), maxValue));
                                    dslider.setValueMin(minValue);
                                    dslider.setValueMax(maxValue);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }

                    } else if (type.equals("MODE")) {
                        // Format: MODE|moduleName|settingName|selected
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof ModeSetting) {
                                    ModeSetting mode = (ModeSetting) setting;
                                    String selected = parts[3];
                                    // Only set if the mode option still exists
                                    boolean validMode = false;
                                    for (String m : mode.getModes()) {
                                        if (m.equals(selected)) {
                                            validMode = true;
                                            break;
                                        }
                                    }
                                    if (validMode) {
                                        mode.setSelected(selected);
                                        loadedSettings++;
                                    } else {
                                        skippedSettings++; // Mode option was removed
                                    }
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }

                    } else if (type.equals("KEYBIND")) {
                        // Format: KEYBIND|moduleName|settingName|keyCode
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof KeybindSetting) {
                                    ((KeybindSetting) setting).setKeyCode(Integer.parseInt(parts[3]));
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }

                    } else if (type.equals("COLOR")) {
                        // Format: COLOR|moduleName|settingName|red|green|blue|alpha|rainbow
                        if (parts.length >= 8) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof ColorSetting) {
                                    int red = Math.max(0, Math.min(255, Integer.parseInt(parts[3])));
                                    int green = Math.max(0, Math.min(255, Integer.parseInt(parts[4])));
                                    int blue = Math.max(0, Math.min(255, Integer.parseInt(parts[5])));
                                    int alpha = Math.max(0, Math.min(255, Integer.parseInt(parts[6])));
                                    boolean rainbow = parts[7].equals("1");
                                    ColorSetting color = (ColorSetting) setting;
                                    color.setColor(red, green, blue, alpha);
                                    color.setRainbow(rainbow);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }

                    } else if (type.equals("TEXT")) {
                        // Format: TEXT|moduleName|settingName|value (Base64 encoded)
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof TextSetting) {
                                    // Decode Base64 value
                                    String decodedValue = new String(Base64.getDecoder().decode(parts[3]), "UTF-8");
                                    ((TextSetting) setting).setValue(decodedValue);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    }
                    // Unknown types are silently ignored (forward compatibility)

                } catch (Exception lineError) {
                    // Skip this line but continue loading
                    skippedSettings++;
                    // System.out.println("[ConfigManager] Skipped invalid line: " + lineError.getMessage());
                }
            }

            System.out.println("[ConfigManager] Loaded config: " + configName + " (" + loadedSettings + " loaded, " + skippedSettings + " skipped)");

            // Show notification
            if (skippedSettings > 0) {
                ModuleNotification.addNotification("Config Loaded (some settings skipped)", true);
            } else {
                ModuleNotification.addNotification("Config Loaded: " + configName, true);
            }

            return true;

        } catch (Exception e) {
            System.out.println("[ConfigManager] Error loading config: " + e.getMessage());
            e.printStackTrace();
            ModuleNotification.addNotification("Error loading config", false);
            return false;
        }
    }

    /**
     * Load config directly from Base64-encoded data string
     * Does not save to local files - applies settings directly
     * @param configData Base64 encoded config data
     * @return true if successful
     */
    public boolean loadConfigFromData(String configData) {
        int loadedSettings = 0;
        int skippedSettings = 0;

        try {
            // Decode Base64
            String decoded;
            try {
                decoded = new String(Base64.getDecoder().decode(configData), "UTF-8");
            } catch (Exception e) {
                System.out.println("[ConfigManager] Invalid config data (Base64 decode failed)");
                ModuleNotification.addNotification("Invalid config data", false);
                return false;
            }

            // IMPORTANT: First disable ALL modules to ensure clean state when switching configs
            System.out.println("[ConfigManager] Disabling all modules before loading cloud config...");
            for (Module module : moduleManager.getModules()) {
                if (module.isEnabled()) {
                    try {
                        module.setEnabled(false);
                    } catch (Exception e) {
                        // Ignore disable errors
                    }
                }
            }

            // Parse lines - each in its own try-catch
            String[] lines = decoded.split("\n");

            for (String line : lines) {
                try {
                    if (line == null || line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|");
                    if (parts.length < 2) {
                        skippedSettings++;
                        continue;
                    }

                    String type = parts[0];

                    if (type.equals("MODULE")) {
                        if (parts.length >= 5) {
                            String moduleName = parts[1];
                            Module module = moduleManager.getModuleByName(moduleName);
                            if (module != null) {
                                boolean enabled = parts[2].equals("1");
                                int toggleBind = Integer.parseInt(parts[3]);
                                int holdBind = Integer.parseInt(parts[4]);
                                module.setEnabled(enabled);
                                module.setToggleBind(toggleBind);
                                module.setHoldBind(holdBind);
                                loadedSettings++;
                            } else {
                                skippedSettings++;
                            }
                        }
                    } else if (type.equals("TICK")) {
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof TickSetting) {
                                    ((TickSetting) setting).setEnabled(parts[3].equals("1"));
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    } else if (type.equals("SLIDER")) {
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof SliderSetting) {
                                    double value = Double.parseDouble(parts[3]);
                                    SliderSetting slider = (SliderSetting) setting;
                                    value = Math.max(slider.getMin(), Math.min(slider.getMax(), value));
                                    slider.setValue(value);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    } else if (type.equals("DSLIDER")) {
                        if (parts.length >= 5) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof DoubleSliderSetting) {
                                    double minValue = Double.parseDouble(parts[3]);
                                    double maxValue = Double.parseDouble(parts[4]);
                                    DoubleSliderSetting dslider = (DoubleSliderSetting) setting;
                                    minValue = Math.max(dslider.getMin(), Math.min(dslider.getMax(), minValue));
                                    maxValue = Math.max(dslider.getMin(), Math.min(dslider.getMax(), maxValue));
                                    dslider.setValueMin(minValue);
                                    dslider.setValueMax(maxValue);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    } else if (type.equals("MODE")) {
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof ModeSetting) {
                                    ModeSetting mode = (ModeSetting) setting;
                                    String selected = parts[3];
                                    boolean validMode = false;
                                    for (String m : mode.getModes()) {
                                        if (m.equals(selected)) {
                                            validMode = true;
                                            break;
                                        }
                                    }
                                    if (validMode) {
                                        mode.setSelected(selected);
                                        loadedSettings++;
                                    } else {
                                        skippedSettings++;
                                    }
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    } else if (type.equals("KEYBIND")) {
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof KeybindSetting) {
                                    ((KeybindSetting) setting).setKeyCode(Integer.parseInt(parts[3]));
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    } else if (type.equals("COLOR")) {
                        if (parts.length >= 8) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof ColorSetting) {
                                    int red = Math.max(0, Math.min(255, Integer.parseInt(parts[3])));
                                    int green = Math.max(0, Math.min(255, Integer.parseInt(parts[4])));
                                    int blue = Math.max(0, Math.min(255, Integer.parseInt(parts[5])));
                                    int alpha = Math.max(0, Math.min(255, Integer.parseInt(parts[6])));
                                    boolean rainbow = parts[7].equals("1");
                                    ColorSetting color = (ColorSetting) setting;
                                    color.setColor(red, green, blue, alpha);
                                    color.setRainbow(rainbow);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    } else if (type.equals("TEXT")) {
                        if (parts.length >= 4) {
                            Module module = moduleManager.getModuleByName(parts[1]);
                            if (module != null) {
                                Setting setting = module.getSettingByName(parts[2]);
                                if (setting instanceof TextSetting) {
                                    String decodedValue = new String(Base64.getDecoder().decode(parts[3]), "UTF-8");
                                    ((TextSetting) setting).setValue(decodedValue);
                                    loadedSettings++;
                                } else {
                                    skippedSettings++;
                                }
                            } else {
                                skippedSettings++;
                            }
                        }
                    }
                } catch (Exception lineError) {
                    skippedSettings++;
                }
            }

            System.out.println("[ConfigManager] Loaded cloud config (" + loadedSettings + " loaded, " + skippedSettings + " skipped)");
            return true;

        } catch (Exception e) {
            System.out.println("[ConfigManager] Error loading cloud config: " + e.getMessage());
            e.printStackTrace();
            ModuleNotification.addNotification("Error loading config", false);
            return false;
        }
    }

    /**
     * Delete config file
     * @param configName Name of config (without extension)
     * @return true if successful
     */
    public boolean deleteConfig(String configName) {
        try {
            String filePath = CONFIG_DIR + File.separator + configName;
            boolean deleted = Files.deleteIfExists(Paths.get(filePath));

            if (deleted) {
                System.out.println("[ConfigManager] Deleted config: " + configName);

                // Show notification
                ModuleNotification.addNotification("Config Deleted: " + configName, false);
            }

            return deleted;
        } catch (Exception e) {
            System.out.println("[ConfigManager] Error deleting config: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get raw config data (Base64 encoded) for cloud upload
     * @param configName Name of config
     * @return Base64 encoded config data, or null if not found
     */
    public String getConfigData(String configName) {
        try {
            String filePath = CONFIG_DIR + File.separator + configName;
            if (!Files.exists(Paths.get(filePath))) {
                return null;
            }
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            return new String(data, "UTF-8");
        } catch (Exception e) {
            System.out.println("[ConfigManager] Error reading config data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Save raw config data (Base64 encoded) from cloud download
     * @param configName Name of config
     * @param data Base64 encoded config data
     * @return true if successful
     */
    public boolean saveConfigData(String configName, String data) {
        try {
            String filePath = CONFIG_DIR + File.separator + configName;
            Files.write(Paths.get(filePath), data.getBytes("UTF-8"));
            System.out.println("[ConfigManager] Saved cloud config: " + configName);
            return true;
        } catch (Exception e) {
            System.out.println("[ConfigManager] Error saving config data: " + e.getMessage());
            return false;
        }
    }

    // Files to exclude from config listing (internal system files)
    private static final Set<String> EXCLUDED_FILES = new HashSet<>(Arrays.asList(
        "SESSION", "USER", "HWID", "session", "user", "hwid"
    ));

    /**
     * Get list of all saved configs
     * @return List of config names
     */
    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();

        try {
            File dir = new File(CONFIG_DIR);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            String fileName = file.getName();
                            // Exclude internal system files
                            if (!EXCLUDED_FILES.contains(fileName) &&
                                !fileName.toUpperCase().equals("SESSION") &&
                                !fileName.toUpperCase().equals("USER") &&
                                !fileName.toUpperCase().equals("HWID")) {
                                configs.add(fileName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[ConfigManager] Error listing configs: " + e.getMessage());
        }

        return configs;
    }

    /**
     * Get the config directory path
     */
    public String getConfigDirectory() {
        return CONFIG_DIR;
    }

    /**
     * Open config folder in file explorer
     */
    public void openConfigFolder() {
        try {
            java.awt.Desktop.getDesktop().open(new File(CONFIG_DIR));
        } catch (Exception e) {
            System.out.println("[ConfigManager] Could not open folder: " + e.getMessage());
        }
    }

    /**
     * Check if config exists
     * @param configName Name of config (without extension)
     * @return true if exists
     */
    public boolean configExists(String configName) {
        String filePath = CONFIG_DIR + File.separator + configName;
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Generate a share code for a config
     * The code is a compressed base64 string that contains config name + data
     * @param configName Name of config to share
     * @return Share code string, or null if failed
     */
    public String generateShareCode(String configName) {
        try {
            String filePath = CONFIG_DIR + File.separator + configName;

            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("[ConfigManager] Config not found: " + configName);
                return null;
            }

            // Read config data (already base64 encoded)
            byte[] configData = Files.readAllBytes(Paths.get(filePath));
            String configB64 = new String(configData, "UTF-8");

            // Prepend config name with separator
            String fullData = configName + "|" + configB64;

            // Compress with GZIP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(baos);
            gzip.write(fullData.getBytes("UTF-8"));
            gzip.close();

            // Encode to URL-safe base64 (shorter)
            String shareCode = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());

            System.out.println("[ConfigManager] Generated share code for: " + configName + " (length: " + shareCode.length() + ")");
            return shareCode;

        } catch (Exception e) {
            System.out.println("[ConfigManager] Error generating share code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Import a config from a share code
     * @param shareCode The share code to import
     * @return Name of imported config, or null if failed
     */
    public String importFromShareCode(String shareCode) {
        try {
            // Decode base64
            byte[] compressed = Base64.getUrlDecoder().decode(shareCode);

            // Decompress GZIP
            ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
            GZIPInputStream gzip = new GZIPInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            gzip.close();

            String fullData = new String(baos.toByteArray(), "UTF-8");

            // Parse name and data
            int separator = fullData.indexOf("|");
            if (separator == -1) {
                System.out.println("[ConfigManager] Invalid share code format");
                return null;
            }

            String originalName = fullData.substring(0, separator);
            String configB64 = fullData.substring(separator + 1);

            // Find unique name (add (2), (3), etc. if exists)
            String finalName = getUniqueName(originalName);

            // Save to file
            String filePath = CONFIG_DIR + File.separator + finalName;
            Files.write(Paths.get(filePath), configB64.getBytes("UTF-8"));

            System.out.println("[ConfigManager] Imported config: " + finalName);
            ModuleNotification.addNotification("Config Imported: " + finalName, true);

            return finalName;

        } catch (Exception e) {
            System.out.println("[ConfigManager] Error importing share code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a unique config name (adds (2), (3), etc. if name exists)
     */
    private String getUniqueName(String baseName) {
        if (!configExists(baseName)) {
            return baseName;
        }

        int counter = 2;
        String newName;
        do {
            newName = baseName + "(" + counter + ")";
            counter++;
        } while (configExists(newName) && counter < 100);

        return newName;
    }
}
