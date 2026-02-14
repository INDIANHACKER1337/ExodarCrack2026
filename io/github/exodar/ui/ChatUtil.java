/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * Centralized chat messaging utility
 * All chat prefixes and message formatting in one place
 */
public class ChatUtil {

    // ============ GLOBAL PREFIX ============
    // Change this to change the prefix everywhere
    public static String PREFIX = "§8[§dExodar§8]";

    // ============ MESSAGE TYPES ============

    /**
     * Send a normal message with prefix
     */
    public static void send(String message) {
        sendRaw(PREFIX + " §f" + message);
    }

    /**
     * Send a success message (green)
     */
    public static void success(String message) {
        sendRaw(PREFIX + " §a" + message);
    }

    /**
     * Send an error message (red)
     */
    public static void error(String message) {
        sendRaw(PREFIX + " §c" + message);
    }

    /**
     * Send an info message (gray)
     */
    public static void info(String message) {
        sendRaw(PREFIX + " §7" + message);
    }

    /**
     * Send raw message without prefix
     */
    public static void sendRaw(String message) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(message));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Get the global prefix
     */
    public static String getPrefix() {
        return PREFIX;
    }

    /**
     * Set a custom prefix (for themes/easter eggs)
     */
    public static void setPrefix(String prefix) {
        PREFIX = prefix;
    }

    /**
     * Reset prefix to default
     */
    public static void resetPrefix() {
        PREFIX = "§8[§dExodar§8]";
    }
}
