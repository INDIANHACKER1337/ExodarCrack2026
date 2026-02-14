/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.event.Render3DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.*;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * LagRange - Uses blink (packet queuing) to extend reach
 * Based on Myau's LagRange logic
 *
 * Queues position packets and only releases when hitting would benefit.
 * Shows your "lagged" position in third person.
 */
public class LagRange extends Module {

    private SliderSetting delay;
    private SliderSetting range;
    private TickSetting weaponsOnly;
    private TickSetting allowTools;
    private TickSetting releaseOnHurt;
    private TickSetting showPosition;

    // Packet queue
    private final Queue<TimedPacket> queuedPackets = new ConcurrentLinkedQueue<>();

    // Lag state
    private int tickDelay = 0;
    private long delayCounter = 0L;
    private boolean hasTarget = false;

    // Position tracking for rendering
    private Vec3 laggedPosition = null;
    private Vec3 lastLaggedPosition = null;

    // Flag to prevent recursion when sending packets
    private static boolean isSendingDirect = false;

    // Pending attack packet (sent next tick after releasing position packets)
    private Packet<?> pendingAttackPacket = null;
    private int pendingAttackDelay = 0;

    public LagRange() {
        super("LagRange", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Blink-based reach extend"));
        this.registerSetting(delay = new SliderSetting("Delay (ms)", 150.0, 0.0, 500.0, 10.0));
        this.registerSetting(range = new SliderSetting("Range", 10.0, 3.0, 30.0, 1.0));

        this.registerSetting(new DescriptionSetting("-- Filters --"));
        this.registerSetting(weaponsOnly = new TickSetting("Weapons Only", true));
        this.registerSetting(allowTools = new TickSetting("Allow Tools", false));
        this.registerSetting(releaseOnHurt = new TickSetting("Release On Hurt", true));

        this.registerSetting(new DescriptionSetting("-- Visual --"));
        this.registerSetting(showPosition = new TickSetting("Show Position", true));
    }

    @Override
    public void onEnable() {
        queuedPackets.clear();
        tickDelay = 0;
        delayCounter = 0L;
        hasTarget = false;
        laggedPosition = null;
        lastLaggedPosition = null;
        isSendingDirect = false;
        pendingAttackPacket = null;
        pendingAttackDelay = 0;
    }

    @Override
    public void onDisable() {
        releaseAllPackets();
        // Send any pending attack
        if (pendingAttackPacket != null) {
            sendPacketDirect(pendingAttackPacket);
            pendingAttackPacket = null;
        }
        tickDelay = 0;
        delayCounter = 0L;
        hasTarget = false;
        laggedPosition = null;
        lastLaggedPosition = null;
        isSendingDirect = false;
        pendingAttackDelay = 0;
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.isSingleplayer()) return;

        // Process pending attack packet (delayed to ensure position packets are processed first)
        if (pendingAttackPacket != null) {
            pendingAttackDelay--;
            if (pendingAttackDelay <= 0) {
                sendPacketDirect(pendingAttackPacket);
                pendingAttackPacket = null;
            }
        }

        // Release on hurt
        if (releaseOnHurt.isEnabled() && mc.thePlayer.hurtTime > 0) {
            releaseAllPackets();
            tickDelay = 0;
            // Also send any pending attack immediately
            if (pendingAttackPacket != null) {
                sendPacketDirect(pendingAttackPacket);
                pendingAttackPacket = null;
            }
            return;
        }

        // Reset state
        hasTarget = false;
        int newDelay = 0;

        // Check conditions
        if (!shouldActivate()) {
            tickDelay = 0;
            releaseAllPackets();
            return;
        }

        // Get positions
        double eyeHeight = mc.thePlayer.getEyeHeight();
        Vec3 currentEyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + eyeHeight, mc.thePlayer.posZ);
        Vec3 lastTickEyePos = new Vec3(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY + eyeHeight, mc.thePlayer.lastTickPosZ);

        // Get lagged position (from oldest queued packet or last tick)
        Vec3 laggedEyePos = getLaggedPosition();
        if (laggedEyePos == null) {
            laggedEyePos = lastTickEyePos;
        } else {
            laggedEyePos = laggedEyePos.addVector(0, eyeHeight, 0);
        }

        // Check all players
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isValidTarget(player)) continue;

            // Distance from current position
            double currentDist = distanceToEntityBox(player, currentEyePos);
            if (currentDist > range.getValue()) continue;

            // Distance from last tick position
            double lastTickDist = distanceToEntityBox(player, lastTickEyePos);

            // Distance from lagged position
            double laggedDist = distanceToEntityBox(player, laggedEyePos);

            // Only activate if current position is closer than lagged/lastTick
            // This means we've moved closer and lagging gives us advantage
            if (currentDist < lastTickDist || currentDist < laggedDist) {
                // Calculate tick delay from ms delay
                if (tickDelay <= 0) {
                    tickDelay = 0;
                    delayCounter += (long) delay.getValue();
                    while (delayCounter > 0L) {
                        tickDelay++;
                        delayCounter -= 50; // 50ms per tick
                    }
                }

                newDelay = tickDelay;
                hasTarget = true;
                break;
            }
        }

        // Update position tracking for rendering
        if (hasTarget) {
            lastLaggedPosition = laggedPosition;
            laggedPosition = getLaggedPosition();
            if (laggedPosition == null) {
                laggedPosition = new Vec3(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY, mc.thePlayer.lastTickPosZ);
            }
        }

        // If no target found, release packets
        if (!hasTarget) {
            tickDelay = 0;
            releaseAllPackets();
        }
    }

    /**
     * Check if we should activate lag range
     */
    private boolean shouldActivate() {
        if (mc.thePlayer.isUsingItem() && !mc.thePlayer.isBlocking()) {
            return false; // Using item (eating, drinking) but not blocking
        }

        // Weapons only check
        if (weaponsOnly.isEnabled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null) return false;

            boolean isSword = held.getItem() instanceof ItemSword;
            boolean isAxe = held.getItem() instanceof ItemAxe;
            boolean isTool = held.getItem() instanceof ItemPickaxe || held.getItem() instanceof ItemSpade;

            if (!isSword && !isAxe) {
                if (!allowTools.isEnabled() || !isTool) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if a player is a valid target
     */
    private boolean isValidTarget(EntityPlayer player) {
        if (player == mc.thePlayer) return false;
        if (player == mc.thePlayer.ridingEntity) return false;
        if (player.isDead || player.deathTime > 0) return false;

        // Friends check
        if (Friends.isFriend(player.getName())) return false;

        // Bot check - auto uses AntiBot module
        if (AntiBot.isBotForCombat(player)) return false;

        // Team check - auto uses Teams module
        if (isTeammate(player)) return false;

        return true;
    }

    /**
     * Check if player is on same team - auto uses Teams module
     */
    private boolean isTeammate(EntityPlayer player) {
        try {
            io.github.exodar.module.modules.misc.Teams teams = io.github.exodar.module.modules.misc.Teams.getInstance();
            if (teams != null && teams.isEnabled()) {
                return teams.isTeamMate(player);
            }
        } catch (Exception e) {
            // Silent
        }
        return false;
    }

    /**
     * Get distance from eye position to entity's bounding box
     */
    private double distanceToEntityBox(Entity entity, Vec3 eyePos) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double x = Math.max(box.minX, Math.min(eyePos.xCoord, box.maxX));
        double y = Math.max(box.minY, Math.min(eyePos.yCoord, box.maxY));
        double z = Math.max(box.minZ, Math.min(eyePos.zCoord, box.maxZ));
        return eyePos.distanceTo(new Vec3(x, y, z));
    }

    /**
     * Get the lagged position from queued packets
     */
    private Vec3 getLaggedPosition() {
        if (queuedPackets.isEmpty()) return null;

        // Get first (oldest) packet position
        for (TimedPacket tp : queuedPackets) {
            if (tp.packet instanceof C03PacketPlayer) {
                C03PacketPlayer posPacket = (C03PacketPlayer) tp.packet;
                if (posPacket.isMoving()) {
                    return new Vec3(posPacket.getPositionX(), posPacket.getPositionY(), posPacket.getPositionZ());
                }
            }
        }
        return null;
    }

    /**
     * Called from Main.onSendPacket hook
     * @return true to allow packet, false to queue it
     */
    public boolean shouldAllowPacket(Object packet) {
        if (!enabled || isSendingDirect) return true;
        if (mc == null || mc.thePlayer == null) return true;
        if (mc.isSingleplayer()) return true;
        if (packet == null) return true;

        // Check if this packet should reset the lag
        int resetType = shouldResetOnPacket(packet);
        if (resetType == 2) {
            // Attack packet - release all position packets first
            // Then delay the attack by 1 tick so server processes positions first
            if (!queuedPackets.isEmpty()) {
                releaseAllPackets();
                tickDelay = 0;
                // Queue the attack to be sent next tick
                pendingAttackPacket = (Packet<?>) packet;
                pendingAttackDelay = 1; // Wait 1 tick
                return false; // Cancel this packet, will be sent later
            }
            // No packets queued, send attack normally
            return true;
        } else if (resetType == 1) {
            releaseAllPackets();
            tickDelay = 0;
            return true;
        }

        // Don't queue if no target
        if (!hasTarget || tickDelay <= 0) return true;

        // Don't intercept critical packets
        if (packet instanceof C00Handshake ||
            packet instanceof C00PacketLoginStart ||
            packet instanceof C00PacketServerQuery ||
            packet instanceof C01PacketEncryptionResponse ||
            packet instanceof C01PacketChatMessage ||
            packet instanceof C0FPacketConfirmTransaction ||
            packet instanceof C00PacketKeepAlive) {
            return true;
        }

        // Only queue position packets
        if (packet instanceof C03PacketPlayer) {
            queuedPackets.add(new TimedPacket(packet, System.currentTimeMillis()));

            // Limit queue size based on tick delay
            while (queuedPackets.size() > tickDelay + 5) {
                TimedPacket old = queuedPackets.poll();
                if (old != null && old.packet != null) {
                    sendPacketDirect((Packet<?>) old.packet);
                }
            }

            return false; // Cancel the packet
        }

        return true;
    }

    /**
     * Check if packet should reset the lag state
     * Returns: 0 = don't reset, 1 = reset and allow, 2 = reset, release, then resend this packet
     */
    private int shouldResetOnPacket(Object packet) {
        // Attack packet - release lag first, then send attack
        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                return 2; // Special handling: release packets first, then send attack
            }
            return 1; // Other interactions, just reset
        }

        // Reset on dig (except release use item)
        if (packet instanceof C07PacketPlayerDigging) {
            if (((C07PacketPlayerDigging) packet).getStatus() != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                return 1;
            }
        }

        // Reset on block placement (except sword blocking)
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            ItemStack item = ((C08PacketPlayerBlockPlacement) packet).getStack();
            if (item == null || !(item.getItem() instanceof ItemSword)) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Send packet directly bypassing hooks
     */
    private void sendPacketDirect(Packet<?> packet) {
        if (mc == null || mc.getNetHandler() == null) return;
        try {
            isSendingDirect = true;
            mc.getNetHandler().addToSendQueue(packet);
        } finally {
            isSendingDirect = false;
        }
    }

    /**
     * Static method to check if we're sending packets directly
     */
    public static boolean isSendingDirectPacket() {
        return isSendingDirect;
    }

    private void releaseAllPackets() {
        if (mc == null || mc.getNetHandler() == null) return;

        try {
            isSendingDirect = true;

            while (!queuedPackets.isEmpty()) {
                TimedPacket tp = queuedPackets.poll();
                if (tp != null && tp.packet != null) {
                    mc.getNetHandler().addToSendQueue((Packet<?>) tp.packet);
                }
            }
        } catch (Exception e) {
            // Silent
        } finally {
            isSendingDirect = false;
        }

        queuedPackets.clear();
    }

    /**
     * Render lagged position in third person
     */
    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (!enabled) return;
        if (showPosition.isEnabled() &&
            mc.gameSettings.thirdPersonView != 0 &&
            hasTarget &&
            laggedPosition != null) {

            float partialTicks = event.getPartialTicks();
            RenderManager rm = mc.getRenderManager();

            // Interpolate position
            double x, y, z;
            if (lastLaggedPosition != null) {
                x = lerp(lastLaggedPosition.xCoord, laggedPosition.xCoord, partialTicks);
                y = lerp(lastLaggedPosition.yCoord, laggedPosition.yCoord, partialTicks);
                z = lerp(lastLaggedPosition.zCoord, laggedPosition.zCoord, partialTicks);
            } else {
                x = laggedPosition.xCoord;
                y = laggedPosition.yCoord;
                z = laggedPosition.zCoord;
            }

            // Offset by camera position
            x -= rm.viewerPosX;
            y -= rm.viewerPosY;
            z -= rm.viewerPosZ;

            // Create bounding box
            float width = mc.thePlayer.width;
            float height = mc.thePlayer.height;
            float border = mc.thePlayer.getCollisionBorderSize();

            AxisAlignedBB box = new AxisAlignedBB(
                x - width / 2.0, y, z - width / 2.0,
                x + width / 2.0, y + height, z + width / 2.0
            ).expand(border, border, border);

            // Get color
            int color = getPositionColor();
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // Render
            GL11.glPushMatrix();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glLineWidth(1.5f);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            // Filled box
            GL11.glColor4f(r, g, b, 0.25f);
            drawFilledBox(box);

            // Outline
            GL11.glColor4f(r, g, b, 1.0f);
            RenderGlobal.drawSelectionBoundingBox(box);

            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private int getPositionColor() {
        // Purple (§5)
        return 0xAA00AA;
    }

    private double lerp(double prev, double curr, float partialTicks) {
        return prev + (curr - prev) * partialTicks;
    }

    private void drawFilledBox(AxisAlignedBB box) {
        GL11.glBegin(GL11.GL_QUADS);

        // Bottom
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);

        // Top
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);

        // North
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);

        // South
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);

        // West
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);

        // East
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);

        GL11.glEnd();
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + (int) delay.getValue() + "ms";
    }

    @Override
    public int getDisplayColor() {
        int queued = queuedPackets.size();
        if (queued == 0) {
            // Purple (§5) when not charging
            return 0xAA00AA;
        }

        // Calculate max queue size
        int maxQueue = (int) (delay.getValue() / 50) + 5;
        if (maxQueue <= 0) maxQueue = 1;

        // Calculate progress (0.0 to 1.0)
        float progress = Math.min(1.0f, (float) queued / maxQueue);

        // Interpolate from gray (§7 = 0xAAAAAA) to red (§c = 0xFF5555)
        int grayR = 0xAA, grayG = 0xAA, grayB = 0xAA;
        int redR = 0xFF, redG = 0x55, redB = 0x55;

        int r = (int) (grayR + (redR - grayR) * progress);
        int g = (int) (grayG + (redG - grayG) * progress);
        int b = (int) (grayB + (redB - grayB) * progress);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Inner class to hold packets with timestamps
     */
    private static class TimedPacket {
        final Object packet;
        final long timestamp;

        TimedPacket(Object packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}
