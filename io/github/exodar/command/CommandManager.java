/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.command;

import io.github.exodar.config.UserConfig;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.spoof.SpoofManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * Handles chat commands starting with "."
 */
public class CommandManager {

    private static final String PREFIX = ".";

    /**
     * Check if a message is a command and handle it
     * @param message The chat message
     * @return true if the message was handled as a command (should cancel sending to server)
     */
    public static boolean handleCommand(String message) {
        if (message == null || !message.startsWith(PREFIX)) {
            return false;
        }

        String command = message.substring(PREFIX.length()).trim();

        if (command.isEmpty()) {
            return false;
        }

        // Parse command and arguments
        String[] parts = parseCommand(command);
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "spoof":
                    handleSpoofCommand(parts);
                    return true;

                case "spooflist":
                case "spoofs":
                    handleSpoofListCommand();
                    return true;

                case "spoofclear":
                case "clearspoof":
                    handleSpoofClearCommand();
                    return true;

                case "vclip":
                    handleVClipCommand(parts);
                    return true;

                case "hclip":
                    handleHClipCommand(parts);
                    return true;

                case "help":
                    handleHelpCommand();
                    return true;

                case "config":
                    handleConfigCommand(parts);
                    return true;

                case "hide":
                    handleHideCommand(parts);
                    return true;

                case "unhide":
                    handleUnhideCommand(parts);
                    return true;

                case "hidden":
                    handleHiddenCommand();
                    return true;

                case "rename":
                case "name":
                    handleRenameCommand(parts);
                    return true;

                case "panic":
                    handlePanicCommand(parts);
                    return true;

                case "newgui":
                case "newclickgui":
                case "moderngui":
                    handleModernGuiCommand();
                    return true;

                case "invsee":
                case "inv":
                    handleInvseeCommand(parts);
                    return true;

                default:
                    sendMessage("§cUnknown command: " + cmd + ". Use .help for commands.");
                    return true;
            }
        } catch (Exception e) {
            sendMessage("§cError executing command: " + e.getMessage());
            return true;
        }
    }

    /**
     * Parse command respecting quotes for spaces
     * Example: .spoof Furra "&7[&eBeta&7]&e Furritah" -> ["spoof", "Furra", "&7[&eBeta&7]&e Furritah"]
     */
    private static String[] parseCommand(String command) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }

    /**
     * Handle .spoof command
     * Usage: .spoof <originalName> <customName> [true/skinName]
     * Example: .spoof Furra "&7[&eBeta&7]&e Furritah"
     * Example: .spoof Furra "Furritah" true    <- uses skin from "Furritah"
     * Example: .spoof Furra "CustomName" Notch <- uses skin from "Notch"
     */
    private static void handleSpoofCommand(String[] parts) {
        if (parts.length < 3) {
            sendMessage("§eUsage: §f.spoof <originalName> <customName> [skin]");
            sendMessage("§7Example: §f.spoof Furra \"&7[&eBeta&7]&e Furritah\"");
            sendMessage("§7With skin: §f.spoof Furra \"Furritah\" true");
            sendMessage("§7Custom skin: §f.spoof Furra \"Display\" Notch");
            return;
        }

        String originalName = parts[1];
        String customName = parts[2];
        boolean useSkin = false;
        String skinName = null;

        // Check for skin parameter
        int customNameEndIndex = 2;

        // Check if last argument is "true" or a potential skin name
        if (parts.length >= 4) {
            String lastArg = parts[parts.length - 1].toLowerCase();
            if (lastArg.equals("true")) {
                useSkin = true;
                customNameEndIndex = parts.length - 2;
            } else if (!lastArg.startsWith("&") && !lastArg.contains(" ")) {
                // If it looks like a username (no color codes, no spaces), treat as skin source
                // Check if it's a valid MC username format
                if (lastArg.matches("[a-zA-Z0-9_]{1,16}")) {
                    useSkin = true;
                    skinName = parts[parts.length - 1];
                    customNameEndIndex = parts.length - 2;
                }
            }
        }

        // If there are more parts for customName (user didn't use quotes), join them
        if (customNameEndIndex > 2) {
            StringBuilder sb = new StringBuilder(parts[2]);
            for (int i = 3; i <= customNameEndIndex; i++) {
                sb.append(" ").append(parts[i]);
            }
            customName = sb.toString();
        }

        SpoofManager.addSpoof(originalName, customName, useSkin, skinName);

        // Show preview with color codes converted
        String preview = customName.replace("&", "§");
        String skinInfo = useSkin ? (skinName != null ? " §7(skin: " + skinName + ")" : " §7(with skin)") : "";
        sendMessage("§aSpoof added: §f" + originalName + " §a-> " + preview + skinInfo);

        if (useSkin) {
            sendMessage("§7Skin is loading in the background...");
        }
    }

    /**
     * Handle .spooflist command
     */
    private static void handleSpoofListCommand() {
        java.util.Map<String, String> spoofs = SpoofManager.getAllSpoofs();

        if (spoofs.isEmpty()) {
            sendMessage("§eNo spoofs active. Use §f.spoof <name> <customName> §eto add one.");
            return;
        }

        sendMessage("§a§lActive Spoofs (" + spoofs.size() + "):");
        for (java.util.Map.Entry<String, String> entry : spoofs.entrySet()) {
            String skinStatus = "";
            SpoofManager.SpoofData data = SpoofManager.getSpoofData(entry.getKey());
            if (data != null && data.useSkin) {
                if (data.skinLoaded) {
                    skinStatus = " §a[SKIN]";
                } else if (data.skinLoading) {
                    skinStatus = " §e[LOADING...]";
                } else {
                    skinStatus = " §c[SKIN FAILED]";
                }
            }
            sendMessage("§f" + entry.getKey() + " §7-> " + entry.getValue() + skinStatus);
        }
    }

    /**
     * Handle .spoofclear command
     */
    private static void handleSpoofClearCommand() {
        int count = SpoofManager.getSpoofCount();
        SpoofManager.clearAll();
        sendMessage("§aCleared " + count + " spoof(s).");
    }

    /**
     * Handle .vclip command - Vertical teleport
     * Usage: .vclip <distance>
     * Example: .vclip +5 (up 5 blocks), .vclip -3 (down 3 blocks)
     */
    private static void handleVClipCommand(String[] parts) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            sendMessage("§cNot in game!");
            return;
        }

        if (parts.length < 2) {
            sendMessage("§eUsage: §f.vclip <distance>");
            sendMessage("§7Example: §f.vclip +5 §7(up 5 blocks)");
            sendMessage("§7Example: §f.vclip -3 §7(down 3 blocks)");
            return;
        }

        try {
            String distStr = parts[1];
            double distance = Double.parseDouble(distStr);

            double newY = mc.thePlayer.posY + distance;
            mc.thePlayer.setPosition(mc.thePlayer.posX, newY, mc.thePlayer.posZ);

            String direction = distance > 0 ? "up" : "down";
            sendMessage("§aTeleported " + Math.abs(distance) + " blocks " + direction);
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid number: " + parts[1]);
        }
    }

    /**
     * Handle .hclip command - Horizontal teleport (forward)
     * Usage: .hclip <distance>
     * Example: .hclip 5 (forward 5 blocks), .hclip -3 (backward 3 blocks)
     */
    private static void handleHClipCommand(String[] parts) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            sendMessage("§cNot in game!");
            return;
        }

        if (parts.length < 2) {
            sendMessage("§eUsage: §f.hclip <distance>");
            sendMessage("§7Example: §f.hclip 5 §7(forward 5 blocks)");
            sendMessage("§7Example: §f.hclip -3 §7(backward 3 blocks)");
            return;
        }

        try {
            String distStr = parts[1];
            double distance = Double.parseDouble(distStr);

            // Calculate direction based on player yaw
            float yaw = mc.thePlayer.rotationYaw * 0.017453292F; // Convert to radians
            double newX = mc.thePlayer.posX - Math.sin(yaw) * distance;
            double newZ = mc.thePlayer.posZ + Math.cos(yaw) * distance;

            mc.thePlayer.setPosition(newX, mc.thePlayer.posY, newZ);

            String direction = distance > 0 ? "forward" : "backward";
            sendMessage("§aTeleported " + Math.abs(distance) + " blocks " + direction);
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid number: " + parts[1]);
        }
    }

    /**
     * Handle .config command
     * Usage: .config share <configName> - Generate shareable code
     *        .config import <code> - Import config from code or cloud ID
     *        .config upload <configName> - Upload config to cloud
     */
    private static void handleConfigCommand(String[] parts) {
        if (parts.length < 2) {
            sendMessage("§eUsage:");
            sendMessage("§f.config share <name> §7- Generate share code");
            sendMessage("§f.config import <code> §7- Import from code or cloud ID");
            sendMessage("§f.config upload <name> §7- Upload to cloud");
            return;
        }

        String subCmd = parts[1].toLowerCase();

        if (subCmd.equals("share")) {
            if (parts.length < 3) {
                sendMessage("§eUsage: §f.config share <configName>");
                return;
            }

            String configName = parts[2];
            io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();

            if (configManager == null) {
                sendMessage("§cConfig manager not available");
                return;
            }

            if (!configManager.configExists(configName)) {
                sendMessage("§cConfig not found: " + configName);
                return;
            }

            String shareCode = configManager.generateShareCode(configName);
            if (shareCode != null) {
                sendMessage("§aShare code generated for: §f" + configName);
                sendMessage("§7Code length: " + shareCode.length() + " chars");
                sendMessage("§e§lCode: §r§f" + shareCode);
                sendMessage("§7Copy the code above and share it!");

                // Copy to clipboard
                try {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(shareCode);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                    sendMessage("§a✔ Code copied to clipboard!");
                } catch (Exception e) {
                    sendMessage("§7(Could not copy to clipboard automatically)");
                }
            } else {
                sendMessage("§cFailed to generate share code");
            }

        } else if (subCmd.equals("import")) {
            if (parts.length < 3) {
                sendMessage("§eUsage: §f.config import <code>");
                sendMessage("§7Cloud ID: §f.config import ABC123DEF456");
                return;
            }

            String code = parts[2].trim().toUpperCase();

            // Check if it's a cloud config ID (12 uppercase alphanumeric)
            if (code.matches("^[A-Z0-9]{12}$")) {
                sendMessage("§7Downloading cloud config: §f" + code + "§7...");

                io.github.exodar.config.CloudConfigManager.downloadConfig(code).thenAccept(result -> {
                    if (result == null) {
                        net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                            sendMessage("§cCloud config not found: " + code);
                        });
                        return;
                    }

                    String configName = result[0];
                    String configData = result[1];
                    String author = result[2];

                    // Save the config locally
                    io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();
                    if (configManager != null) {
                        // Find unique name
                        String finalName = configName;
                        int counter = 2;
                        while (configManager.configExists(finalName) && counter < 100) {
                            finalName = configName + "(" + counter + ")";
                            counter++;
                        }

                        // Write config data to file
                        try {
                            String filePath = configManager.getConfigDirectory() + java.io.File.separator + finalName;
                            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), configData.getBytes("UTF-8"));

                            final String savedName = finalName;
                            final String authorName = author;
                            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                                sendMessage("§aCloud config imported: §f" + savedName);
                                sendMessage("§7Author: §f" + authorName);
                                sendMessage("§7Use ClickGUI > Settings > Config to load it");
                            });
                        } catch (Exception e) {
                            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                                sendMessage("§cError saving config: " + e.getMessage());
                            });
                        }
                    }
                });

            } else {
                // Try as local share code
                StringBuilder codeBuilder = new StringBuilder();
                for (int i = 2; i < parts.length; i++) {
                    codeBuilder.append(parts[i]);
                }
                String shareCode = codeBuilder.toString().trim();

                io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();

                if (configManager == null) {
                    sendMessage("§cConfig manager not available");
                    return;
                }

                String importedName = configManager.importFromShareCode(shareCode);
                if (importedName != null) {
                    sendMessage("§aConfig imported successfully: §f" + importedName);
                    sendMessage("§7Use ClickGUI > Settings > Config to load it");
                } else {
                    sendMessage("§cFailed to import config. Invalid code?");
                }
            }

        } else if (subCmd.equals("upload")) {
            if (parts.length < 3) {
                sendMessage("§eUsage: §f.config upload <configName>");
                return;
            }

            if (!io.github.exodar.config.CloudConfigManager.isAuthenticated()) {
                sendMessage("§cNot authenticated. Login first.");
                return;
            }

            String configName = parts[2];
            io.github.exodar.config.ConfigManager configManager = io.github.exodar.Main.getConfigManager();

            if (configManager == null) {
                sendMessage("§cConfig manager not available");
                return;
            }

            if (!configManager.configExists(configName)) {
                sendMessage("§cConfig not found: " + configName);
                return;
            }

            // Read config data
            try {
                String filePath = configManager.getConfigDirectory() + java.io.File.separator + configName;
                byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
                String configData = new String(data, "UTF-8");

                sendMessage("§7Uploading config: §f" + configName + "§7...");

                io.github.exodar.config.CloudConfigManager.uploadConfig(configName, configData, false).thenAccept(configId -> {
                    if (configId != null) {
                        net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                            sendMessage("§aConfig uploaded! ID: §f" + configId);
                            sendMessage("§7Share: §f.config import " + configId);

                            // Copy to clipboard
                            try {
                                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(configId);
                                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                                sendMessage("§a✔ ID copied to clipboard!");
                            } catch (Exception e) {}
                        });
                    }
                });

            } catch (Exception e) {
                sendMessage("§cError reading config: " + e.getMessage());
            }

        } else {
            sendMessage("§cUnknown subcommand: " + subCmd);
            sendMessage("§eUsage: §f.config share/import/upload");
        }
    }

    /**
     * Handle .hide command
     * Usage: .hide <module> - Hide module from ArrayList
     *        .hide all - Hide all modules
     */
    private static void handleHideCommand(String[] parts) {
        if (parts.length < 2) {
            sendMessage("§eUsage: §f.hide <module> §7or §f.hide all");
            return;
        }

        String arg = parts[1].toLowerCase();
        UserConfig userConfig = UserConfig.getInstance();
        ModuleManager mm = io.github.exodar.Main.getModuleManager();

        if (arg.equals("all")) {
            if (mm != null) {
                userConfig.hideAllModules(mm);
                sendMessage("§aHidden all modules from ArrayList (" + mm.getModules().size() + " modules)");
            } else {
                sendMessage("§cModule manager not available");
            }
            return;
        }

        // Find module by name
        if (mm != null) {
            Module module = mm.getModuleByName(parts[1]);
            if (module != null) {
                userConfig.hideModule(module.getName());
                sendMessage("§aHidden §f" + module.getName() + " §afrom ArrayList");
            } else {
                sendMessage("§cModule not found: " + parts[1]);
            }
        } else {
            // Hide by name anyway
            userConfig.hideModule(parts[1]);
            sendMessage("§aHidden §f" + parts[1] + " §afrom ArrayList");
        }
    }

    /**
     * Handle .unhide command
     * Usage: .unhide <module> - Unhide module from ArrayList
     *        .unhide all - Unhide all modules
     */
    private static void handleUnhideCommand(String[] parts) {
        if (parts.length < 2) {
            sendMessage("§eUsage: §f.unhide <module> §7or §f.unhide all");
            return;
        }

        String arg = parts[1].toLowerCase();
        UserConfig userConfig = UserConfig.getInstance();

        if (arg.equals("all")) {
            int count = userConfig.getHiddenCount();
            userConfig.unhideAllModules();
            sendMessage("§aUnhidden all modules (" + count + " modules)");
            return;
        }

        // Find module by name
        ModuleManager mm = io.github.exodar.Main.getModuleManager();
        if (mm != null) {
            Module module = mm.getModuleByName(parts[1]);
            if (module != null) {
                userConfig.unhideModule(module.getName());
                sendMessage("§aUnhidden §f" + module.getName() + " §afrom ArrayList");
                return;
            }
        }

        // Unhide by name anyway (in case module name changed)
        userConfig.unhideModule(parts[1]);
        sendMessage("§aUnhidden §f" + parts[1] + " §afrom ArrayList");
    }

    /**
     * Handle .hidden command - List all hidden modules
     */
    private static void handleHiddenCommand() {
        UserConfig userConfig = UserConfig.getInstance();
        java.util.Set<String> hidden = userConfig.getHiddenModules();

        if (hidden.isEmpty()) {
            sendMessage("§eNo hidden modules. Use §f.hide <module> §eto hide.");
            return;
        }

        sendMessage("§a§lHidden Modules (" + hidden.size() + "):");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String name : hidden) {
            if (count > 0) sb.append("§7, ");
            sb.append("§f").append(name);
            count++;
            if (count % 5 == 0) {
                sendMessage(sb.toString());
                sb = new StringBuilder();
            }
        }
        if (sb.length() > 0) {
            sendMessage(sb.toString());
        }
        sendMessage("§7Use §f.unhide <module> §7or §f.unhide all §7to show them");
    }

    /**
     * Handle .panic command
     * Usage: .panic - Disable all modules
     *        .panic reset - Reset all settings to factory defaults (doesn't affect saved configs)
     */
    private static void handlePanicCommand(String[] parts) {
        ModuleManager mm = io.github.exodar.Main.getModuleManager();

        if (mm == null) {
            sendMessage("§cModule manager not available");
            return;
        }

        // Check for .panic reset
        if (parts.length >= 2 && parts[1].equalsIgnoreCase("reset")) {
            // Reset all settings to their default values (from constructors)
            int resetCount = 0;
            for (Module m : mm.getModules()) {
                // Disable module first
                if (m.isEnabled()) {
                    m.setEnabled(false);
                }
                // Reset all settings to default
                for (io.github.exodar.setting.Setting setting : m.getSettings()) {
                    try {
                        setting.resetToDefault();
                        resetCount++;
                    } catch (Exception ignored) {}
                }
                // Reset keybinds
                m.setToggleBind(0);
                m.setHoldBind(0);
            }
            sendMessage("§aReset §f" + resetCount + " §asettings to factory defaults!");
            sendMessage("§7(Your saved configs are NOT affected)");
            return;
        }

        // .panic - Disable all modules
        int disabledCount = 0;
        for (Module m : mm.getModules()) {
            if (m.isEnabled()) {
                m.setEnabled(false);
                disabledCount++;
            }
        }

        sendMessage("§aDisabled §f" + disabledCount + " §amodules!");
    }

    /**
     * Handle .rename command
     * Usage: .rename <name> - Change client name/watermark (triggers Easter eggs for virtue, huzuni, wurst)
     */
    private static void handleRenameCommand(String[] parts) {
        if (parts.length < 2) {
            sendMessage("§eUsage: §f.rename <name>");
            sendMessage("§7Example: §f.rename Virtue");
            sendMessage("§7Easter eggs: Virtue, Huzuni, Wurst");
            return;
        }

        // Join all parts for names with spaces
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) nameBuilder.append(" ");
            nameBuilder.append(parts[i]);
        }
        String newName = nameBuilder.toString();

        // Update ArrayListConfig directly
        io.github.exodar.ui.ArrayListConfig.watermarkText = newName;
        io.github.exodar.ui.ArrayListConfig.clientName = newName;

        // Trigger Easter egg skin update
        io.github.exodar.ui.ClientSkin.updateSkin(newName);

        // Also update the HUD module's setting if possible
        ModuleManager mm = io.github.exodar.Main.getModuleManager();
        if (mm != null) {
            Module hudModule = mm.getModuleByName("HUD");
            if (hudModule != null) {
                io.github.exodar.setting.Setting setting = hudModule.getSettingByName("Client Name");
                if (setting instanceof io.github.exodar.setting.TextSetting) {
                    ((io.github.exodar.setting.TextSetting) setting).setValue(newName);
                }
            }
        }

        // Check if Easter egg was triggered
        if (io.github.exodar.ui.ClientSkin.isEasterEggActive()) {
            io.github.exodar.ui.ClientSkin.SkinType skin = io.github.exodar.ui.ClientSkin.getCurrentSkin();
            sendMessage("§aClient name changed to: §f" + newName + " §7(" + skin.name() + " style)");
        } else {
            sendMessage("§aClient name changed to: §f" + newName);
        }
    }

    /**
     * Handle .newgui command - Opens the new modern ClickGUI
     * TODO: GUI disabled - needs redesign
     */
    private static void handleModernGuiCommand() {
        sendMessage("§cModern GUI is currently disabled.");
        // try {
        //     io.github.exodar.gui.modern.ModernClickGuiRenderer.toggle();
        // } catch (Exception e) {
        //     sendMessage("§cError: " + e.getMessage());
        // }
    }

    /**
     * Handle .invsee command - View tracked inventory of a player
     * Usage: .invsee <playerName>
     */
    private static void handleInvseeCommand(String[] parts) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            sendMessage("§cNot in game!");
            return;
        }

        // Check if InventoryTracker is enabled
        io.github.exodar.module.modules.misc.InventoryTracker tracker =
            io.github.exodar.module.modules.misc.InventoryTracker.getInstance();

        if (tracker == null || !tracker.isEnabled()) {
            sendMessage("§cInventoryTracker module is not enabled!");
            sendMessage("§7Enable it first to start tracking player equipment.");
            return;
        }

        if (parts.length < 2) {
            // Show list of tracked players
            java.util.Map<java.util.UUID, String> tracked =
                io.github.exodar.module.modules.misc.InventoryTracker.getTrackedPlayerNames();

            if (tracked.isEmpty()) {
                sendMessage("§eNo players tracked yet.");
                sendMessage("§7Players are tracked when you see them change equipment.");
            } else {
                sendMessage("§a§lTracked Players (" + tracked.size() + "):");
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String name : tracked.values()) {
                    if (count > 0) sb.append("§7, ");
                    sb.append("§f").append(name);
                    count++;
                    if (count % 5 == 0) {
                        sendMessage(sb.toString());
                        sb = new StringBuilder();
                    }
                }
                if (sb.length() > 0) {
                    sendMessage(sb.toString());
                }
            }
            sendMessage("§eUsage: §f.invsee <playerName>");
            return;
        }

        String targetName = parts[1];

        // Try to find player in tab list first
        String foundName = null;
        for (net.minecraft.client.network.NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equalsIgnoreCase(targetName)) {
                foundName = info.getGameProfile().getName();
                break;
            }
        }

        // If not in tab, check if we have tracked data
        if (foundName == null) {
            if (io.github.exodar.module.modules.misc.InventoryTracker.hasData(targetName)) {
                foundName = targetName;
            }
        }

        if (foundName == null) {
            sendMessage("§cPlayer not found: " + targetName);
            sendMessage("§7Player must be in the server or have been tracked previously.");
            return;
        }

        // Check if we have data for this player
        if (!io.github.exodar.module.modules.misc.InventoryTracker.hasData(foundName)) {
            sendMessage("§eNo inventory data for: §f" + foundName);
            sendMessage("§7Wait until they change equipment (swap items, put on armor).");
            return;
        }

        // Open the invsee GUI
        final String finalName = foundName;
        mc.addScheduledTask(() -> {
            mc.displayGuiScreen(new io.github.exodar.gui.InvseeGui(finalName));
        });

        sendMessage("§aOpening inventory view for: §f" + foundName);
    }

    /**
     * Handle .help command
     */
    private static void handleHelpCommand() {
        sendMessage("§a§l=== Exodar Commands ===");
        sendMessage("§e.spoof <name> <customName> [skin] §7- Spoof player name/skin");
        sendMessage("§e.spooflist §7- List all active spoofs");
        sendMessage("§e.spoofclear §7- Clear all spoofs");
        sendMessage("§e.vclip <dist> §7- Vertical teleport (+up, -down)");
        sendMessage("§e.hclip <dist> §7- Horizontal teleport (forward)");
        sendMessage("§e.config share/import §7- Share or import configs");
        sendMessage("§e.hide <module> §7- Hide module from ArrayList");
        sendMessage("§e.unhide <module> §7- Unhide module from ArrayList");
        sendMessage("§e.hidden §7- List hidden modules");
        sendMessage("§e.rename <name> §7- Change client name (Easter eggs: Virtue, Huzuni, Wurst)");
        sendMessage("§e.panic §7- Disable all modules");
        sendMessage("§e.panic reset §7- Reset all settings to factory defaults");
        sendMessage("§e.newgui §7- Open modern ClickGUI (preview)");
        sendMessage("§e.invsee <player> §7- View tracked player inventory");
        sendMessage("§e.help §7- Show this help");
        sendMessage("");
        sendMessage("§7Tip: Use quotes for names with spaces:");
        sendMessage("§f.spoof Furra \"&7[&eBeta&7]&e Furritah\"");
        sendMessage("");
        sendMessage("§7Config sharing:");
        sendMessage("§f.config share MyConfig §7- Get share code");
        sendMessage("§f.config import <code> §7- Import shared config");
    }

    /**
     * Send a chat message to the player (client-side only)
     */
    private static void sendMessage(String message) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(io.github.exodar.ui.ChatUtil.PREFIX + " " + message));
            }
        } catch (Exception e) {
            System.out.println("[Exodar] Error sending message: " + e.getMessage());
        }
    }
}
