/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Backtrack - Delays incoming entity position packets
 * Allows hitting opponents at their past positions
 *
 * This module lags other players when they are about to move out of range,
 * allowing you to get additional hits on them.
 */
public class Backtrack extends Module {

    // Main settings
    private final SliderSetting minDistance;
    private final SliderSetting maxDistance;
    private final SliderSetting maxDelay;
    private final SliderSetting maxHurtTime;
    private final SliderSetting cooldown;

    // Conditions
    private final TickSetting disableOnHit;
    private final TickSetting weaponsOnly;

    // Real position indicator
    private final TickSetting drawRealPosition;
    private final SliderSetting lineWidth;
    private final TickSetting filled;

    private final Queue<TimedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final List<Object> skipPackets = new ArrayList<>();

    private Vec3 targetPos = null;
    private EntityPlayer target = null;
    private int currentDelay = 0;
    private long lastDeactivationTime = 0;
    private boolean wasActive = false;

    // Flag to prevent recursion
    private static boolean isProcessingPacket = false;

    public Backtrack() {
        super("Backtrack", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Lag players to hit them"));

        // Main settings
        this.registerSetting(minDistance = new SliderSetting("Min Distance", 0.0, 0.0, 3.0, 0.1));
        this.registerSetting(maxDistance = new SliderSetting("Max Distance", 4.0, 1.0, 6.0, 0.1));
        this.registerSetting(maxDelay = new SliderSetting("Max Delay", 200.0, 50.0, 500.0, 10.0));
        this.registerSetting(maxHurtTime = new SliderSetting("Max Hurt Time", 500.0, 0.0, 500.0, 10.0));
        this.registerSetting(cooldown = new SliderSetting("Cooldown", 0.0, 0.0, 2000.0, 50.0));

        // Conditions
        this.registerSetting(disableOnHit = new TickSetting("Disable on Hit", true));
        this.registerSetting(weaponsOnly = new TickSetting("Weapons Only", false));

        // Real position indicator
        this.registerSetting(drawRealPosition = new TickSetting("Show Real Position", true));
        this.registerSetting(lineWidth = new SliderSetting("Line Width", 2.0, 1.0, 5.0, 0.5));
        this.registerSetting(filled = new TickSetting("Filled", false));
    }

    @Override
    public void onEnable() {
        packetQueue.clear();
        skipPackets.clear();
        targetPos = null;
        target = null;
        currentDelay = 0;
        lastDeactivationTime = 0;
        wasActive = false;
    }

    @Override
    public void onDisable() {
        releaseAll();
        target = null;
        targetPos = null;
        currentDelay = 0;
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            releaseAll();
            return;
        }

        // Check weapons only condition
        if (weaponsOnly.isEnabled() && !isHoldingWeapon()) {
            releaseAll();
            return;
        }

        // Check if we were hit (disable on hit)
        if (disableOnHit.isEnabled() && mc.thePlayer.hurtTime > 0) {
            releaseAll();
            return;
        }

        // Check distance to target position
        if (targetPos != null && target != null) {
            double distance = mc.thePlayer.getDistance(targetPos.xCoord, targetPos.yCoord, targetPos.zCoord);
            if (distance > maxDistance.getValue() || distance < minDistance.getValue()) {
                if (wasActive) {
                    releaseAll();
                    lastDeactivationTime = System.currentTimeMillis();
                    wasActive = false;
                }
            }
        }

        // Process delayed packets that have reached their delay
        processDelayedPackets();

        // Update targetPos to current target position if queue is empty
        if (packetQueue.isEmpty() && target != null) {
            targetPos = target.getPositionVector();
        }

        // Track if we're active
        wasActive = currentDelay > 0 && !packetQueue.isEmpty();
    }

    /**
     * Check if player is holding a weapon (sword or axe)
     */
    private boolean isHoldingWeapon() {
        if (mc.thePlayer == null) return false;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        return held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe;
    }

    /**
     * Called when player attacks an entity - sets the backtrack target
     */
    public void onAttackEntity(EntityPlayer attacked) {
        if (!enabled) return;

        // Check weapons only
        if (weaponsOnly.isEnabled() && !isHoldingWeapon()) return;

        // Check cooldown
        if (cooldown.getValue() > 0 && lastDeactivationTime > 0) {
            long elapsed = System.currentTimeMillis() - lastDeactivationTime;
            if (elapsed < cooldown.getValue()) {
                return; // Still in cooldown
            }
        }

        Vec3 attackedPos = attacked.getPositionVector();

        if (target == null || attacked != target) {
            targetPos = attackedPos;
        }

        target = attacked;

        // Check distance
        double distance = mc.thePlayer.getDistanceToEntity(attacked);
        if (distance > maxDistance.getValue() || distance < minDistance.getValue()) {
            return;
        }

        // Check target hurt time (in ms, convert to ticks approximately)
        // 1 tick = 50ms, so hurtTime * 50 gives approximate ms
        if (maxHurtTime.getValue() < 500) {
            int hurtTimeMs = attacked.hurtTime * 50;
            if (hurtTimeMs > maxHurtTime.getValue()) {
                return; // Target was hit too recently
            }
        }

        // Set delay
        currentDelay = (int) maxDelay.getValue();
    }

    /**
     * Called by Main.onReceivePacket hook
     * @return true to allow packet, false to delay it
     */
    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled || isProcessingPacket) return true;
        if (mc.thePlayer == null || mc.theWorld == null) return true;
        if (mc.thePlayer.ticksExisted < 20) {
            packetQueue.clear();
            return true;
        }

        // Check if this packet should be skipped (already processed)
        if (skipPackets.contains(packet)) {
            skipPackets.remove(packet);
            return true;
        }

        // Check weapons only
        if (weaponsOnly.isEnabled() && !isHoldingWeapon()) {
            releaseAll();
            return true;
        }

        // Check disable on hit
        if (disableOnHit.isEnabled() && mc.thePlayer.hurtTime > 0) {
            releaseAll();
            return true;
        }

        // No target = release all
        if (target == null) {
            releaseAll();
            return true;
        }

        String packetName = packet.getClass().getSimpleName();

        // CRITICAL: Always allow these packets through to avoid kick
        if (packetName.contains("S19PacketEntityStatus") ||
            packetName.contains("S02PacketChat") ||
            packetName.contains("S0BPacketAnimation") ||
            packetName.contains("S06PacketUpdateHealth") ||
            packetName.contains("S00PacketKeepAlive") ||
            packetName.contains("S32PacketConfirmTransaction") ||
            packetName.contains("ConfirmTransaction") ||
            packetName.contains("S3FPacketCustomPayload") ||
            packetName.contains("CustomPayload") ||
            packetName.contains("S01PacketJoinGame") ||
            packetName.contains("S07PacketRespawn")) {
            return true;
        }

        // Release all on teleport or disconnect
        if (packetName.contains("S08PacketPlayerPosLook") ||
            packetName.contains("S40PacketDisconnect")) {
            releaseAll();
            target = null;
            targetPos = null;
            return true;
        }

        // Handle entity destroy
        if (packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities destroyPacket = (S13PacketDestroyEntities) packet;
            for (int id : destroyPacket.getEntityIDs()) {
                if (target != null && id == target.getEntityId()) {
                    target = null;
                    targetPos = null;
                    releaseAll();
                    return true;
                }
            }
        }

        // Track entity movement - update targetPos
        if (packet instanceof S14PacketEntity && target != null) {
            S14PacketEntity movePacket = (S14PacketEntity) packet;
            if (getEntityId(movePacket) == target.getEntityId()) {
                if (targetPos != null) {
                    targetPos = targetPos.addVector(
                        movePacket.func_149062_c() / 32.0D,
                        movePacket.func_149061_d() / 32.0D,
                        movePacket.func_149064_e() / 32.0D
                    );
                }
            }
        }

        // Handle entity teleport
        if (packet instanceof S18PacketEntityTeleport && target != null) {
            S18PacketEntityTeleport teleportPacket = (S18PacketEntityTeleport) packet;
            if (teleportPacket.getEntityId() == target.getEntityId()) {
                targetPos = new Vec3(
                    teleportPacket.getX() / 32.0D,
                    teleportPacket.getY() / 32.0D,
                    teleportPacket.getZ() / 32.0D
                );
            }
        }

        // Queue the packet
        packetQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
        return false; // Cancel the packet (delay it)
    }

    private int getEntityId(S14PacketEntity packet) {
        try {
            for (java.lang.reflect.Field f : packet.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    return f.getInt(packet);
                }
            }
            for (java.lang.reflect.Field f : packet.getClass().getSuperclass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    return f.getInt(packet);
                }
            }
        } catch (Exception e) {
            // Silent
        }
        return -1;
    }

    private void processDelayedPackets() {
        while (!packetQueue.isEmpty()) {
            TimedPacket tp = packetQueue.peek();
            if (tp == null) break;

            long elapsed = System.currentTimeMillis() - tp.timestamp;
            if (elapsed >= currentDelay) {
                packetQueue.poll();
                processPacket(tp.packet);
            } else {
                break;
            }
        }
    }

    private void releaseAll() {
        while (!packetQueue.isEmpty()) {
            TimedPacket tp = packetQueue.poll();
            if (tp != null) {
                processPacket(tp.packet);
            }
        }
        currentDelay = 0;
    }

    @SuppressWarnings("unchecked")
    private void processPacket(Object packet) {
        if (packet == null) return;

        try {
            isProcessingPacket = true;
            skipPackets.add(packet);

            if (packet instanceof Packet && mc.getNetHandler() != null) {
                ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler());
            }
        } catch (Exception e) {
            // Silent
        } finally {
            isProcessingPacket = false;
        }
    }

    /**
     * Render the target's real position indicator
     */
    public void renderBacktrack(EntityPlayer player, double x, double y, double z) {
        if (!enabled || !drawRealPosition.isEnabled()) return;
        if (target == null || targetPos == null || target.isDead) return;
        if (player != target) return;
        if (currentDelay <= 0) return;

        try {
            // Calculate offset from current player position to real position
            double offsetX = targetPos.xCoord - player.posX;
            double offsetY = targetPos.yCoord - player.posY;
            double offsetZ = targetPos.zCoord - player.posZ;

            // Purple color (hardcoded)
            float r = 0.6f;
            float g = 0.2f;
            float b = 0.8f;
            float a = filled.isEnabled() ? 0.3f : 0.9f;

            // Setup GL state
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth((float) lineWidth.getValue());

            GL11.glColor4f(r, g, b, a);

            // Translate to real position
            GlStateManager.translate((float)(x + offsetX), (float)(y + offsetY), (float)(z + offsetZ));

            // Box dimensions (player hitbox)
            double w = 0.3;
            double h = 1.8;

            if (filled.isEnabled()) {
                // Draw filled box
                GL11.glBegin(GL11.GL_QUADS);

                // Bottom face
                GL11.glVertex3d(-w, 0, -w);
                GL11.glVertex3d(-w, 0, w);
                GL11.glVertex3d(w, 0, w);
                GL11.glVertex3d(w, 0, -w);

                // Top face
                GL11.glVertex3d(-w, h, -w);
                GL11.glVertex3d(w, h, -w);
                GL11.glVertex3d(w, h, w);
                GL11.glVertex3d(-w, h, w);

                // Front face
                GL11.glVertex3d(-w, 0, w);
                GL11.glVertex3d(-w, h, w);
                GL11.glVertex3d(w, h, w);
                GL11.glVertex3d(w, 0, w);

                // Back face
                GL11.glVertex3d(-w, 0, -w);
                GL11.glVertex3d(w, 0, -w);
                GL11.glVertex3d(w, h, -w);
                GL11.glVertex3d(-w, h, -w);

                // Left face
                GL11.glVertex3d(-w, 0, -w);
                GL11.glVertex3d(-w, h, -w);
                GL11.glVertex3d(-w, h, w);
                GL11.glVertex3d(-w, 0, w);

                // Right face
                GL11.glVertex3d(w, 0, -w);
                GL11.glVertex3d(w, 0, w);
                GL11.glVertex3d(w, h, w);
                GL11.glVertex3d(w, h, -w);

                GL11.glEnd();
            }

            // Draw outline
            GL11.glColor4f(r, g, b, 0.9f);

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
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();

        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Legacy method for compatibility
     */
    public void onRender3D(float partialTicks) {
        // Rendering now done from CustomRenderPlayer.doRender()
    }

    /**
     * Check if backtrack is actively delaying packets
     */
    public boolean isActive() {
        return enabled && currentDelay > 0 && !packetQueue.isEmpty();
    }

    /**
     * Check if we're processing a packet (bypass hooks)
     */
    public static boolean isProcessingDirectPacket() {
        return isProcessingPacket;
    }

    @Override
    public String getDisplaySuffix() {
        if (currentDelay > 0 && !packetQueue.isEmpty()) {
            return " §7" + currentDelay + "ms";
        }
        return " §7" + (int) maxDelay.getValue() + "ms";
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
