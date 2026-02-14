/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * LunarUnlock - Unlocks Lunar Client features
 * - Freelook bypass (prevents servers from disabling it)
 * - Cosmetics unlock (shows all cosmetics as owned)
 *
 * Based on lcqt2 patches by Nilsen84
 */
public class LunarUnlock extends Module {

    private final TickSetting freelookBypass;
    private final TickSetting cosmeticsUnlock;

    // Lunar Client detection
    private static Boolean isLunarClient = null;
    private static boolean checkedLunar = false;

    // RPC channel hook for cosmetics
    private static Object rpcChannelInstance = null;
    private static boolean rpcHooked = false;

    public LunarUnlock() {
        super("LunarUnlock", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Unlocks Lunar Client features"));
        this.registerSetting(freelookBypass = new TickSetting("Freelook Bypass", true));
        this.registerSetting(cosmeticsUnlock = new TickSetting("Cosmetics Unlock", true));

        // Hide module if not on Lunar Client
        updateVisibility();
    }

    /**
     * Update module visibility based on Lunar Client detection
     */
    public void updateVisibility() {
        setHidden(!isLunarClient());
    }

    @Override
    public void onEnable() {
        if (!isLunarClient()) {
            addChatMessage("§c[LunarUnlock] Not running on Lunar Client!");
            setEnabled(false);
            return;
        }

        if (cosmeticsUnlock.isEnabled()) {
            hookCosmetics();
        }

        addChatMessage("§a[LunarUnlock] Enabled! Freelook: " + freelookBypass.isEnabled() + ", Cosmetics: " + cosmeticsUnlock.isEnabled());
    }

    @Override
    public void onDisable() {
        // Nothing to cleanup
    }

    /**
     * Check if running on Lunar Client
     */
    public static boolean isLunarClient() {
        if (checkedLunar) return isLunarClient != null && isLunarClient;
        checkedLunar = true;

        try {
            // Check for Lunar Client specific classes
            Class.forName("com.lunarclient.bukkitapi.nethandler.client.LCNetHandlerClient");
            isLunarClient = true;
            return true;
        } catch (ClassNotFoundException e) {
            // Try alternative detection
        }

        try {
            // Check for Genesis (Lunar's internal name)
            Class.forName("com.moonsworth.lunar.genesis.Genesis");
            isLunarClient = true;
            return true;
        } catch (ClassNotFoundException e) {
            // Try another approach
        }

        try {
            // Check for Lunar's mod system
            Class.forName("com.lunarclient.client.LunarClient");
            isLunarClient = true;
            return true;
        } catch (ClassNotFoundException e) {
            // Not found
        }

        try {
            // Check class loader for Lunar indicators
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null && cl.getClass().getName().toLowerCase().contains("lunar")) {
                isLunarClient = true;
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }

        // Check if any loaded class contains lunar in package
        try {
            // Look for lunar-specific packages in the stack
            for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
                String className = elem.getClassName().toLowerCase();
                if (className.contains("lunar") || className.contains("genesis") || className.contains("moonsworth")) {
                    isLunarClient = true;
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        isLunarClient = false;
        return false;
    }

    /**
     * Called from Main.onReceivePacket to filter mod settings
     */
    public boolean shouldBlockPacket(Object packet) {
        if (!enabled) return false;

        // Handle custom payload packets (mod settings come through here)
        if (packet instanceof S3FPacketCustomPayload) {
            S3FPacketCustomPayload customPayload = (S3FPacketCustomPayload) packet;
            String channel = customPayload.getChannelName();

            // Lunar Client mod settings channels
            if (freelookBypass.isEnabled()) {
                if (channel != null && (
                    channel.contains("lunarclient") ||
                    channel.contains("lunar") ||
                    channel.contains("LC") ||
                    channel.equals("REGISTER") ||
                    channel.equals("UNREGISTER")
                )) {
                    // Check if it's a mod settings packet
                    try {
                        byte[] data = getPayloadData(customPayload);
                        if (data != null && containsModSettings(data)) {
                            // Modify the packet to not disable freelook
                            return handleModSettingsPacket(customPayload, data);
                        }
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get payload data from custom payload packet
     */
    private byte[] getPayloadData(S3FPacketCustomPayload packet) {
        try {
            // Try to get the data field
            for (Field f : packet.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(packet);
                if (val instanceof byte[]) {
                    return (byte[]) val;
                }
                // PacketBuffer
                if (val != null && val.getClass().getSimpleName().contains("PacketBuffer")) {
                    try {
                        Method arrayMethod = val.getClass().getMethod("array");
                        return (byte[]) arrayMethod.invoke(val);
                    } catch (Exception e) {
                        // Try readableBytes approach
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Check if data contains mod settings
     */
    private boolean containsModSettings(byte[] data) {
        if (data == null || data.length < 10) return false;

        String dataStr = new String(data, StandardCharsets.UTF_8);
        return dataStr.contains("modSettings") ||
               dataStr.contains("freelook") ||
               dataStr.contains("Freelook") ||
               dataStr.contains("mod_settings");
    }

    /**
     * Handle mod settings packet - block or modify
     */
    private boolean handleModSettingsPacket(S3FPacketCustomPayload packet, byte[] data) {
        // For now, just block packets that try to disable freelook
        String dataStr = new String(data, StandardCharsets.UTF_8);

        // If it contains freelook settings, block it
        if (dataStr.toLowerCase().contains("freelook")) {
            return true; // Block this packet
        }

        // If it's a general mod settings packet, we could modify it
        // but for safety, let's just block those that affect freelook
        if (dataStr.contains("modSettings")) {
            return true; // Block mod settings
        }

        return false;
    }

    /**
     * Hook into cosmetics system
     */
    private void hookCosmetics() {
        if (rpcHooked) return;

        try {
            // Find the RPC channel class that implements protobuf RpcChannel
            Class<?> rpcChannelInterface = null;
            try {
                rpcChannelInterface = Class.forName("com.google.protobuf.RpcChannel");
            } catch (ClassNotFoundException e) {
                // Protobuf not found, can't hook cosmetics
                return;
            }

            // Search for implementing class
            // This is complex and version-dependent
            // For now, we'll try a different approach - hook at network level

            // Alternative: Spoof cosmetic ownership by modifying websocket messages
            hookCosmeticsWebSocket();

        } catch (Exception e) {
            // Ignore errors
        }

        rpcHooked = true;
    }

    /**
     * Hook cosmetics via WebSocket (Lunar uses WebSocket for cosmetics)
     */
    private void hookCosmeticsWebSocket() {
        // Lunar Client uses WebSocket to communicate with their servers
        // Cosmetic data comes through these channels
        // We would need to intercept and modify the responses

        // This is handled in the packet interception for now
        // Full implementation would require hooking the WebSocket handler
    }

    /**
     * Modify cosmetic response to show all as owned
     * Called when we intercept a cosmetic-related packet
     */
    public byte[] modifyCosmeticResponse(byte[] data) {
        if (!cosmeticsUnlock.isEnabled()) return data;

        try {
            String dataStr = new String(data, StandardCharsets.UTF_8);

            // Look for cosmetic ownership patterns and modify them
            // This is highly dependent on the protocol format

            // For protobuf responses, we would need to:
            // 1. Parse the protobuf message
            // 2. Modify the "owned" fields to true
            // 3. Re-serialize

            // Simplified approach: look for common patterns
            if (dataStr.contains("owned\":false") || dataStr.contains("owned\": false")) {
                dataStr = dataStr.replace("owned\":false", "owned\":true");
                dataStr = dataStr.replace("owned\": false", "owned\": true");
                return dataStr.getBytes(StandardCharsets.UTF_8);
            }

        } catch (Exception e) {
            // Ignore
        }

        return data;
    }

    private void addChatMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(message));
        }
    }

    @Override
    public String getDisplaySuffix() {
        if (isLunarClient()) {
            return " §aLunar";
        }
        return " §cN/A";
    }

}
