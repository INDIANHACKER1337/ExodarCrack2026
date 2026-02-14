/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.ChatComponentText;

import java.lang.reflect.Field;

/**
 * BedrockSpoof - Makes you appear as a Bedrock/Geyser player
 *
 * Spoofs:
 * 1. Client brand to "Geyser" (or custom)
 * 2. Can add "." prefix to username (Floodgate style)
 *
 * NOTE: This only works on servers that do SIMPLE checks.
 * Servers using FloodgateApi.isFloodgatePlayer() with encrypted
 * verification will NOT be fooled by this.
 */
public class BedrockSpoof extends Module {

    private TickSetting spoofBrand;
    private TextSetting brandName;
    private TickSetting debug;

    // Singleton
    private static BedrockSpoof instance;

    // Reflection for packet data
    private static Field payloadChannelField;
    private static Field payloadDataField;
    private static boolean reflectionInit = false;

    public BedrockSpoof() {
        super("BedrockSpoof", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Spoof as Bedrock/Geyser player"));
        this.registerSetting(spoofBrand = new TickSetting("Spoof Brand", true));
        this.registerSetting(brandName = new TextSetting("Brand", "Geyser"));
        this.registerSetting(new DescriptionSetting("--- Info ---"));
        this.registerSetting(debug = new TickSetting("Debug", false));
        this.registerSetting(new DescriptionSetting("Only works on simple checks"));
        this.registerSetting(new DescriptionSetting("FloodgateApi = won't work"));
    }

    public static BedrockSpoof getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        initReflection();
        if (debug.isEnabled()) {
            sendMessage("§aBedrockSpoof enabled");
            sendMessage("§7Brand will be spoofed to: §e" + brandName.getValue());
        }
    }

    @Override
    public void onDisable() {
        if (debug.isEnabled()) {
            sendMessage("§cBedrockSpoof disabled");
        }
    }

    private void initReflection() {
        if (reflectionInit) return;
        reflectionInit = true;

        try {
            // Find channel and data fields in C17PacketCustomPayload
            for (Field f : C17PacketCustomPayload.class.getDeclaredFields()) {
                f.setAccessible(true);
                Class<?> type = f.getType();

                if (type == String.class) {
                    payloadChannelField = f;
                } else if (type == PacketBuffer.class) {
                    payloadDataField = f;
                }
            }

            if (debug.isEnabled()) {
                sendMessage("§7Reflection init: channel=" + (payloadChannelField != null) +
                           ", data=" + (payloadDataField != null));
            }
        } catch (Exception e) {
            if (debug.isEnabled()) {
                sendMessage("§cReflection error: " + e.getMessage());
            }
        }
    }

    /**
     * Called from Main.onSendPacket to intercept brand packets
     * @return true if packet was modified, false otherwise
     */
    public boolean onPacketSend(Object packet) {
        if (!isEnabled() || !spoofBrand.isEnabled()) return false;
        if (!(packet instanceof C17PacketCustomPayload)) return false;

        C17PacketCustomPayload payload = (C17PacketCustomPayload) packet;

        try {
            // Get channel name
            String channel = payload.getChannelName();

            // Check if this is the brand packet
            if ("MC|Brand".equals(channel)) {
                // Create new brand data
                String newBrand = brandName.getValue();
                if (newBrand == null || newBrand.isEmpty()) {
                    newBrand = "Geyser";
                }

                // Create new packet buffer with our brand
                PacketBuffer newBuffer = new PacketBuffer(Unpooled.buffer());
                newBuffer.writeString(newBrand);

                // Replace the data field
                if (payloadDataField != null) {
                    payloadDataField.set(payload, newBuffer);

                    if (debug.isEnabled()) {
                        sendMessage("§aSpoofed brand to: §e" + newBrand);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            if (debug.isEnabled()) {
                sendMessage("§cError spoofing brand: " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Get the spoofed brand name
     */
    public String getSpoofedBrand() {
        if (!isEnabled() || !spoofBrand.isEnabled()) return null;
        String brand = brandName.getValue();
        return (brand != null && !brand.isEmpty()) ? brand : "Geyser";
    }

    /**
     * Check if brand spoofing is active
     */
    public boolean isSpoofingBrand() {
        return isEnabled() && spoofBrand.isEnabled();
    }

    private void sendMessage(String msg) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText("§7[§bBedrock§7] §f" + msg));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public String getDisplaySuffix() {
        return spoofBrand.isEnabled() ? " §7" + brandName.getValue() : null;
    }
}
