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
import io.github.exodar.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.util.ChatComponentText;

import java.io.File;
import java.util.Arrays;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * PacketLogger - Logs all packets sent/received
 * Useful for debugging and understanding server communication
 */
public class PacketLogger extends Module {

    private static PacketLogger instance;

    // Settings
    private final TickSetting logSend;
    private final TickSetting logReceive;
    private final TickSetting logToChat;
    private final TickSetting logToFile;
    private final TickSetting showPacketData;
    private final TextSetting filterPackets;

    // Logging state
    private PrintWriter fileWriter = null;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private long packetCount = 0;

    // Filtered packet names (common spam packets)
    private final Set<String> filteredPackets = new HashSet<>();
    private static final String[] DEFAULT_FILTERS = {
        "S32PacketConfirmTransaction",
        "C00PacketKeepAlive",
        "S00PacketKeepAlive",
        "S03PacketTimeUpdate",
        "S19PacketEntityHeadLook",
        "S14PacketEntity",
        "S12PacketEntityVelocity",
        "S18PacketEntityTeleport"
    };

    public PacketLogger() {
        super("PacketLogger", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Logs packets sent/received"));

        // Log settings
        this.registerSetting(logSend = new TickSetting("Log Outgoing", true));
        this.registerSetting(logReceive = new TickSetting("Log Incoming", true));
        this.registerSetting(showPacketData = new TickSetting("Show Packet Data", false));

        // Output settings
        this.registerSetting(new DescriptionSetting("--- Output ---"));
        this.registerSetting(logToChat = new TickSetting("Log to Chat", false));
        this.registerSetting(logToFile = new TickSetting("Log to File", true));

        // Filter settings
        this.registerSetting(new DescriptionSetting("--- Filter (comma-sep) ---"));
        this.registerSetting(filterPackets = new TextSetting("Filter Packets", String.join(",", DEFAULT_FILTERS)));
    }

    @Override
    public void onEnable() {
        packetCount = 0;
        updateFilteredPackets();

        // Setup file logging if enabled
        if (logToFile.isEnabled()) {
            setupFileLogging();
        }

        log("§aPacketLogger enabled");
    }

    @Override
    public void onDisable() {
        log("§cPacketLogger disabled (logged " + packetCount + " packets)");
        closeFileLogging();
    }

    /**
     * Called when an outgoing packet is sent
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled || !logSend.isEnabled()) return true;
        if (!(packet instanceof Packet)) return true;

        logPacket((Packet<?>) packet, "OUT", "§b");
        return true; // Don't cancel packet
    }

    /**
     * Called when an incoming packet is received
     */
    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled || !logReceive.isEnabled()) return true;
        if (!(packet instanceof Packet)) return true;

        logPacket((Packet<?>) packet, "IN", "§a");
        return true; // Don't cancel packet
    }

    /**
     * Log a packet
     */
    private void logPacket(Packet<?> packet, String direction, String color) {
        String packetName = packet.getClass().getSimpleName();

        // Check filter
        if (isFiltered(packetName)) return;

        packetCount++;
        String time = timeFormat.format(new Date());

        // Build log message
        StringBuilder msg = new StringBuilder();
        msg.append(color).append("[").append(direction).append("] ");
        msg.append("§f").append(packetName);

        // Add packet data if enabled
        if (showPacketData.isEnabled()) {
            String data = getPacketData(packet);
            if (!data.isEmpty()) {
                msg.append(" §7").append(data);
            }
        }

        // Log to chat
        if (logToChat.isEnabled()) {
            log(msg.toString());
        }

        // Log to file
        if (logToFile.isEnabled() && fileWriter != null) {
            fileWriter.println("[" + time + "] [" + direction + "] " + packetName +
                (showPacketData.isEnabled() ? " " + getPacketData(packet) : ""));
            fileWriter.flush();
        }
    }

    /**
     * Get packet data as string (simplified view)
     */
    private String getPacketData(Packet<?> packet) {
        try {
            // Special handling for CustomPayload packets
            if (packet instanceof S3FPacketCustomPayload) {
                return getCustomPayloadData((S3FPacketCustomPayload) packet);
            }

            // Use toString but limit length
            String data = packet.toString();

            // Remove class name prefix if present
            int atIndex = data.indexOf('@');
            if (atIndex > 0) {
                data = data.substring(atIndex + 1);
            }

            // Limit length
            if (data.length() > 100) {
                data = data.substring(0, 100) + "...";
            }

            return data;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get CustomPayload packet data (channel + raw bytes)
     */
    private String getCustomPayloadData(S3FPacketCustomPayload payload) {
        try {
            String channel = payload.getChannelName();
            PacketBuffer buffer = payload.getBufferData();

            if (buffer == null) {
                return "channel=" + channel + " data=null";
            }

            // Read raw bytes without consuming the buffer
            int readable = buffer.readableBytes();
            byte[] raw = new byte[Math.min(readable, 64)]; // Limit to 64 bytes
            buffer.getBytes(buffer.readerIndex(), raw);

            // Convert to readable format
            StringBuilder hexData = new StringBuilder();
            for (int i = 0; i < raw.length; i++) {
                hexData.append(String.format("%02X", raw[i]));
                if (i < raw.length - 1) hexData.append(" ");
            }
            if (readable > 64) {
                hexData.append("... (").append(readable).append(" bytes total)");
            }

            return "channel=" + channel + " size=" + readable + " hex=[" + hexData + "]";
        } catch (Exception e) {
            return "error reading payload: " + e.getMessage();
        }
    }

    /**
     * Check if packet name is filtered
     */
    private boolean isFiltered(String packetName) {
        return filteredPackets.contains(packetName) ||
               filteredPackets.contains(packetName.replace("Packet", ""));
    }

    /**
     * Update filtered packets from setting
     */
    private void updateFilteredPackets() {
        filteredPackets.clear();
        String filter = filterPackets.getValue();
        if (filter != null && !filter.isEmpty()) {
            for (String name : filter.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    filteredPackets.add(trimmed);
                }
            }
        }
    }

    /**
     * Setup file logging
     */
    private void setupFileLogging() {
        try {
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home");
            }

            File logDir = new File(appData + File.separator + ".exodar" + File.separator + "logs");
            logDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File logFile = new File(logDir, "packets_" + timestamp + ".log");

            fileWriter = new PrintWriter(new FileWriter(logFile, true));
            fileWriter.println("=== PacketLogger started at " + new Date() + " ===");
            fileWriter.flush();

            log("§7Logging to: " + logFile.getPath());
        } catch (Exception e) {
            log("§cFailed to setup file logging: " + e.getMessage());
        }
    }

    /**
     * Close file logging
     */
    private void closeFileLogging() {
        if (fileWriter != null) {
            try {
                fileWriter.println("=== PacketLogger stopped at " + new Date() + " ===");
                fileWriter.close();
            } catch (Exception ignored) {}
            fileWriter = null;
        }
    }

    /**
     * Log message to chat
     */
    private void log(String message) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(io.github.exodar.ui.ChatUtil.PREFIX + " §8[§dPackets§8] " + message));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public String getDisplaySuffix() {
        if (packetCount > 0) {
            return " §7" + packetCount;
        }
        return "";
    }

    public static PacketLogger getInstance() {
        return instance;
    }
}
