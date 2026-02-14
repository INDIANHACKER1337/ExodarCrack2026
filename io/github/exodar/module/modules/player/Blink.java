/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Blink - Delays outgoing/incoming packets
 * Normal: Holds packets until disable
 * FakeLag: Auto-releases packets after delay
 *
 * Directions:
 * - Out: Queue outgoing packets (movement, actions) - creates teleport/blink effect
 * - In: Queue incoming packets (entity updates, world state) - freeze other players
 * - Both: Queue both directions
 */
public class Blink extends Module {

    private static final String NORMAL = "Normal";
    private static final String FAKELAG = "FakeLag";

    private static final String DIR_OUT = "Out";
    private static final String DIR_BOTH = "Both";
    private static final String DIR_IN = "In";

    // Settings
    private final ModeSetting mode;
    private final ModeSetting direction;
    private final SliderSetting maxTime;
    private final SliderSetting pulseDelay; // FakeLag pulse interval
    private final TickSetting disableOnHit;
    private final TickSetting showPosition;

    // Pulse tracking for FakeLag
    private long lastPulseTime = 0;

    // Packet queues (Deque for proper FIFO ordering like Myau)
    private final Deque<TimedPacket> outgoingPackets = new ConcurrentLinkedDeque<>();
    private final Deque<TimedPacket> incomingPackets = new ConcurrentLinkedDeque<>();

    // State
    private long startTime = 0;
    private Vec3 startPos = null;
    private boolean isBlinking = false;

    // Flag to prevent recursion when sending/processing packets
    private static volatile boolean isSendingDirect = false;
    private static volatile boolean isProcessingIncoming = false;

    public Blink() {
        super("Blink", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Delays packets"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{NORMAL, FAKELAG})
            .onChange(this::onModeChange));
        this.registerSetting(direction = new ModeSetting("Direction", new String[]{DIR_OUT, DIR_BOTH, DIR_IN}));
        this.registerSetting(maxTime = new SliderSetting("Max Time", 5000, 1000, 30000, 500));
        this.registerSetting(pulseDelay = new SliderSetting("Pulse", 1000, 100, 5000, 100));
        this.registerSetting(disableOnHit = new TickSetting("Disable On Hit", true));
        this.registerSetting(showPosition = new TickSetting("Show Position", true));

        // Initially hide FakeLag settings (Normal mode is default)
        maxTime.setVisible(false);
        pulseDelay.setVisible(false);
    }

    private void onModeChange() {
        boolean isFakeLag = mode.getSelected().equals(FAKELAG);
        maxTime.setVisible(isFakeLag);
        pulseDelay.setVisible(isFakeLag);
    }

    @Override
    public void onEnable() {
        isSendingDirect = false;
        isProcessingIncoming = false;
        outgoingPackets.clear();
        incomingPackets.clear();
        startTime = System.currentTimeMillis();
        lastPulseTime = System.currentTimeMillis();
        isBlinking = true;

        EntityPlayerSP player = getPlayer();
        if (player != null) {
            startPos = new Vec3(player.posX, player.posY, player.posZ);
        }
    }

    @Override
    public void onDisable() {
        isBlinking = false;
        releaseAllPackets();
        isSendingDirect = false;
        isProcessingIncoming = false;
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        // Auto-disable on disconnect or death
        if (player == null || player.isDead || getWorld() == null) {
            setEnabled(false);
            return;
        }

        long now = System.currentTimeMillis();

        if (mode.getSelected().equals(NORMAL)) {
            // Normal mode - just hold packets until disable
        } else {
            // FakeLag mode - release old packets based on maxTime delay
            releaseOldPackets();

            // Pulse: release ALL packets every pulseDelay ms
            if (now - lastPulseTime >= (long) pulseDelay.getValue()) {
                releaseAllPackets();
                lastPulseTime = now;
            }
        }
    }

    /**
     * Called by Main.onSendPacket hook
     * @return true to allow packet, false to queue it
     */
    public boolean shouldAllowPacket(Object packet) {
        if (!enabled || !isBlinking || isSendingDirect) return true;
        if (packet == null) return true;

        // Check if we're holding outgoing packets
        String dir = direction.getSelected();
        if (dir.equals(DIR_IN)) {
            return true; // Only holding incoming, allow outgoing
        }

        // Don't intercept critical packets that could cause kicks (like Myau)
        // KeepAlive and Chat must always go through
        if (packet instanceof C00PacketKeepAlive || packet instanceof C01PacketChatMessage) {
            return true;
        }

        // ConfirmTransaction - only allow if queue is empty (Myau's approach)
        if (packet instanceof C0FPacketConfirmTransaction && outgoingPackets.isEmpty()) {
            return true;
        }

        // Critical handshake/login packets
        if (packet instanceof C00Handshake ||
            packet instanceof C00PacketLoginStart ||
            packet instanceof C00PacketServerQuery ||
            packet instanceof C01PacketEncryptionResponse) {
            return true;
        }

        // Check for attack packet (disable on hit)
        if (disableOnHit.isEnabled() && packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                setEnabled(false);
                return true;
            }
        }

        // Queue the packet
        outgoingPackets.add(new TimedPacket(packet, System.currentTimeMillis()));
        return false;
    }

    /**
     * Called by Main.onReceivePacket hook
     * @return true to allow packet, false to queue it
     */
    public boolean shouldAllowIncomingPacket(Object packet) {
        if (!enabled || !isBlinking || isProcessingIncoming) return true;
        if (packet == null) return true;

        // Check if we're holding incoming packets
        String dir = direction.getSelected();
        if (dir.equals(DIR_OUT)) {
            return true; // Only holding outgoing, allow incoming
        }

        // CRITICAL: Always allow packets that prevent kicks or are essential
        // KeepAlive - server kicks if not responded to
        // ConfirmTransaction - server kicks if transactions out of sync
        // These MUST go through to prevent "Unexpected pong response" disconnects
        if (packet instanceof S00PacketKeepAlive ||
            packet instanceof S32PacketConfirmTransaction) {
            return true;
        }

        // Also allow critical game state packets
        String className = packet.getClass().getSimpleName();
        if (className.contains("Disconnect") ||
            className.contains("JoinGame") ||
            className.contains("Respawn") ||
            className.contains("ResourcePack") ||
            className.contains("Login") ||
            className.contains("Compression")) {
            return true;
        }

        // Queue the packet
        incomingPackets.add(new TimedPacket(packet, System.currentTimeMillis()));
        return false;
    }

    private void releaseOldPackets() {
        long now = System.currentTimeMillis();
        long maxDelay = (long) maxTime.getValue();

        // Release old outgoing packets
        while (!outgoingPackets.isEmpty()) {
            TimedPacket tp = outgoingPackets.peek();
            if (tp == null) break;

            if (now - tp.timestamp >= maxDelay) {
                outgoingPackets.poll();
                sendPacketDirectInternal(tp.packet);
            } else {
                break;
            }
        }

        // Release old incoming packets
        while (!incomingPackets.isEmpty()) {
            TimedPacket tp = incomingPackets.peek();
            if (tp == null) break;

            if (now - tp.timestamp >= maxDelay) {
                incomingPackets.poll();
                processIncomingPacket(tp.packet);
            } else {
                break;
            }
        }
    }

    private void releaseAllPackets() {
        // Release all outgoing first
        while (!outgoingPackets.isEmpty()) {
            TimedPacket tp = outgoingPackets.poll();
            if (tp != null) {
                sendPacketDirectInternal(tp.packet);
            }
        }

        // Release all incoming - schedule on main thread for safety
        if (!incomingPackets.isEmpty()) {
            Minecraft mc = Minecraft.getMinecraft();
            // Process on main thread to avoid threading issues
            mc.addScheduledTask(() -> {
                while (!incomingPackets.isEmpty()) {
                    TimedPacket tp = incomingPackets.poll();
                    if (tp != null) {
                        processIncomingPacket(tp.packet);
                    }
                }
            });
        }
    }

    /**
     * Send packet directly, bypassing hooks
     */
    private void sendPacketDirectInternal(Object packet) {
        if (packet == null) return;

        try {
            isSendingDirect = true;

            EntityPlayerSP player = getPlayer();
            if (player != null && player.sendQueue != null) {
                player.sendQueue.addToSendQueue((Packet<?>) packet);
            }
        } catch (Exception e) {
            // Silent fail
        } finally {
            isSendingDirect = false;
        }
    }

    /**
     * Process a queued incoming packet by calling its processPacket method
     * with the NetHandlerPlayClient
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processIncomingPacket(Object packet) {
        if (packet == null) return;

        try {
            isProcessingIncoming = true;

            EntityPlayerSP player = getPlayer();
            if (player == null || player.sendQueue == null) return;

            NetHandlerPlayClient handler = player.sendQueue;

            // Cast to Packet and call processPacket with the handler
            if (packet instanceof Packet) {
                Packet pkt = (Packet) packet;
                try {
                    // The processPacket method takes the appropriate handler type
                    // NetHandlerPlayClient implements INetHandlerPlayClient
                    pkt.processPacket(handler);
                } catch (ClassCastException e) {
                    // Some packets might need different handler types
                    // Try reflection as fallback
                    try {
                        java.lang.reflect.Method processMethod = null;
                        for (java.lang.reflect.Method m : packet.getClass().getDeclaredMethods()) {
                            if (m.getName().equals("processPacket") && m.getParameterCount() == 1) {
                                processMethod = m;
                                break;
                            }
                        }
                        if (processMethod != null) {
                            processMethod.setAccessible(true);
                            processMethod.invoke(packet, handler);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            // Silent fail to prevent crashes
        } finally {
            isProcessingIncoming = false;
        }
    }

    /**
     * Check if currently processing incoming packets (bypass check)
     */
    public static boolean isProcessingIncomingPacket() {
        return isProcessingIncoming;
    }

    /**
     * Static method to check if we're sending packets directly (bypass hooks)
     */
    public static boolean isSendingDirectPacket() {
        return isSendingDirect;
    }

    /**
     * Count movement packets in queue (like Myau)
     * Useful for modules that need to know how many ticks we've blinked
     */
    public long countMovement() {
        return outgoingPackets.stream()
            .filter(tp -> tp.packet instanceof net.minecraft.network.play.client.C03PacketPlayer)
            .count();
    }

    /**
     * Check if currently blinking
     */
    public static boolean isBlinking() {
        Blink instance = getInstance();
        return instance != null && instance.enabled && instance.isBlinking;
    }

    /**
     * Get singleton instance
     */
    private static Blink getInstance() {
        try {
            return (Blink) io.github.exodar.Main.getModuleManager().getModuleByName("Blink");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Render initial position box - called from CustomRenderPlayer.doRender()
     * Uses entity coordinates for proper rendering like Skeleton
     */
    public void renderBlink(net.minecraft.entity.player.EntityPlayer player, double x, double y, double z) {
        if (!enabled || !showPosition.isEnabled() || startPos == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (player != mc.thePlayer) return; // Only render for local player

        try {
            // Calculate offset from current player position to start position
            double offsetX = startPos.xCoord - player.posX;
            double offsetY = startPos.yCoord - player.posY;
            double offsetZ = startPos.zCoord - player.posZ;

            // Setup GL state like Skeleton
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.disableTexture2D();
            net.minecraft.client.renderer.GlStateManager.disableLighting();
            net.minecraft.client.renderer.GlStateManager.disableDepth();
            net.minecraft.client.renderer.GlStateManager.enableBlend();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(2.0F);

            // White color
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.8F);

            // Translate to start position
            net.minecraft.client.renderer.GlStateManager.translate((float)(x + offsetX), (float)(y + offsetY), (float)(z + offsetZ));

            // Box dimensions (player hitbox)
            double w = 0.3;
            double h = 1.8;

            // Bottom face
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex3d(-w, 0, -w);
            GL11.glVertex3d(-w, 0, w);
            GL11.glVertex3d(w, 0, w);
            GL11.glVertex3d(w, 0, -w);
            GL11.glEnd();

            // Top face
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex3d(-w, h, -w);
            GL11.glVertex3d(-w, h, w);
            GL11.glVertex3d(w, h, w);
            GL11.glVertex3d(w, h, -w);
            GL11.glEnd();

            // Vertical lines
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3d(-w, 0, -w); GL11.glVertex3d(-w, h, -w);
            GL11.glVertex3d(-w, 0, w);  GL11.glVertex3d(-w, h, w);
            GL11.glVertex3d(w, 0, w);   GL11.glVertex3d(w, h, w);
            GL11.glVertex3d(w, 0, -w);  GL11.glVertex3d(w, h, -w);
            GL11.glEnd();

            // Restore GL state
            GL11.glLineWidth(1.0f);
            net.minecraft.client.renderer.GlStateManager.enableDepth();
            net.minecraft.client.renderer.GlStateManager.disableBlend();
            net.minecraft.client.renderer.GlStateManager.enableLighting();
            net.minecraft.client.renderer.GlStateManager.enableTexture2D();
            net.minecraft.client.renderer.GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            net.minecraft.client.renderer.GlStateManager.popMatrix();

        } catch (Exception e) {
            // System.out.println("[Blink] Render error: " + e.getMessage());
        }
    }

    /**
     * Legacy method for compatibility
     */
    public void onRender3D(float partialTicks) {
        // Rendering now done from CustomRenderPlayer.doRender()
    }

    @Override
    public String getDisplaySuffix() {
        int out = outgoingPackets.size();
        int in = incomingPackets.size();
        return " §7" + direction.getSelected() + " " + out + "/" + in;
    }

    private static class TimedPacket {
        final Object packet;
        final long timestamp;

        TimedPacket(Object packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}
