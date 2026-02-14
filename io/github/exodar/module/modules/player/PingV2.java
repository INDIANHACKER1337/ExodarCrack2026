/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Render3DEvent;
import io.github.exodar.event.Subscribe;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.Packet;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PingV2 extends Module {

    // Configurable settings
    private final SliderSetting blocks;
    private final SliderSetting speed;

    // Textures for visual indicators
    private static ResourceLocation laggingTexture = null;
    private static ResourceLocation connectedTexture = null;
    private static boolean texturesLoaded = false;

    // Hardcoded values
    private static final double MIN_PING_RECALCULATE_DELAY = 0;
    private static final double MAX_PING_RECALCULATE_DELAY = 0;
    private static final double MIN_ACCELERATION_PER_SECOND = 1510;
    private static final double MAX_ACCELERATION_PER_SECOND = 1490;
    private static final double MIN_ACCEL_APPLY_DELAY = 5;
    private static final double MAX_ACCEL_APPLY_DELAY = 5;
    private static final double MIN_ACCEL_RECALC_DELAY = 0;
    private static final double MAX_ACCEL_RECALC_DELAY = 0;
    private static final double MIN_DECELERATION_TIME = 1420;
    private static final double MAX_DECELERATION_TIME = 1410;
    private static final boolean COMBAT_MODE = false;
    private static final boolean LIMIT_DISTANCE_TO_TARGET = false;
    private static final double MAX_DISTANCE_TO_TARGET = 29;
    private static final double MS_PER_BLOCK = 125.0; // 5000ms = 40 blocks

    private final ConcurrentLinkedQueue<PacketData> packets = new ConcurrentLinkedQueue<>();
    private final Random random = new Random();

    // Ghost position (where server thinks we are)
    private Vec3 ghostPosition = new Vec3(0, 0, 0);
    // Animated position for smooth rendering
    private double animX, animY, animZ;
    // Starting position when module enabled
    private Vec3 startPosition = new Vec3(0, 0, 0);

    private double currentPing = 0;
    private double targetPing = 0;
    private double currentAcceleration = 0;

    private long lastAccelerationApply = 0;
    private long lastPingRecalculate = 0;
    private long lastAccelerationRecalculate = 0;

    // Release state
    private boolean isReleasing = false;
    private boolean hasStartedReleasing = false; // Track if we ever started releasing
    private int maxQueueSize = 0; // Track max queue size for percentage calculation

    private static boolean isSendingDirect = false;

    public PingV2() {
        super("PingV2", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Ping spoof with ghost box"));
        this.registerSetting(blocks = new SliderSetting("Blocks", 40.0, 1.0, 100.0, 1.0));
        this.registerSetting(speed = new SliderSetting("Speed", 20.0, 10.0, 30.0, 1.0));
    }

    private double blocksToMs(double blocksValue) {
        return blocksValue * MS_PER_BLOCK;
    }

    private void loadTextures() {
        if (texturesLoaded) return;
        try {
            // Load lagging texture
            InputStream laggingStream = getClass().getResourceAsStream("/assets/exodar/textures/lagging.png");
            if (laggingStream != null) {
                BufferedImage laggingImg = ImageIO.read(laggingStream);
                DynamicTexture laggingDynamic = new DynamicTexture(laggingImg);
                laggingTexture = mc.getTextureManager().getDynamicTextureLocation("pingv2_lagging", laggingDynamic);
                laggingStream.close();
            }

            // Load connected texture
            InputStream connectedStream = getClass().getResourceAsStream("/assets/exodar/textures/connected.png");
            if (connectedStream != null) {
                BufferedImage connectedImg = ImageIO.read(connectedStream);
                DynamicTexture connectedDynamic = new DynamicTexture(connectedImg);
                connectedTexture = mc.getTextureManager().getDynamicTextureLocation("pingv2_connected", connectedDynamic);
                connectedStream.close();
            }

            texturesLoaded = true;
        } catch (Exception e) {
            // Silent fail - will show nothing if textures don't load
        }
    }

    @Override
    public void onEnable() {
        packets.clear();
        currentPing = 0;
        targetPing = calculateTargetPing();
        currentAcceleration = calculateAcceleration();
        lastAccelerationApply = System.currentTimeMillis();
        lastPingRecalculate = System.currentTimeMillis();
        lastAccelerationRecalculate = System.currentTimeMillis();
        isSendingDirect = false;
        isReleasing = false;
        hasStartedReleasing = false;
        maxQueueSize = 0;

        if (mc.thePlayer != null) {
            startPosition = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            ghostPosition = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            animX = ghostPosition.xCoord;
            animY = ghostPosition.yCoord;
            animZ = ghostPosition.zCoord;
        }
    }

    @Override
    public void onDisable() {
        flush();
        packets.clear();
        currentPing = 0;
        isSendingDirect = false;
        isReleasing = false;
        hasStartedReleasing = false;
        maxQueueSize = 0;
    }

    private double randomBetween(double min, double max) {
        if (min >= max) return min;
        return min + random.nextDouble() * (max - min);
    }

    private double calculateTargetPing() {
        double ms = blocksToMs(blocks.getValue());
        return ms; // Single value, no random range
    }

    private double calculateAcceleration() {
        return randomBetween(MIN_ACCELERATION_PER_SECOND, MAX_ACCELERATION_PER_SECOND);
    }

    private long calculateAccelerationApplyDelay() {
        return (long) randomBetween(MIN_ACCEL_APPLY_DELAY, MAX_ACCEL_APPLY_DELAY);
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) {
            flush();
            return;
        }

        if (mc.thePlayer.isEating()) {
            flush();
            return;
        }

        long now = System.currentTimeMillis();

        // Recalculate target ping (in case slider changed)
        targetPing = calculateTargetPing();

        // Smooth acceleration mode
        if (now - lastAccelerationApply >= calculateAccelerationApplyDelay()) {
            double deltaTime = (now - lastAccelerationApply) / 1000.0;
            double change = currentAcceleration * deltaTime;

            if (currentPing < targetPing) {
                currentPing = Math.min(currentPing + change, targetPing);
            } else if (currentPing > targetPing) {
                currentPing = Math.max(currentPing - change, targetPing);
            }
            lastAccelerationApply = now;
        }

        handlePackets();
    }

    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled) return true;
        if (isSendingDirect) return true;
        if (mc.thePlayer == null || mc.theWorld == null) return true;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) return true;

        if (packet instanceof Packet) {
            // Store player position when packet is queued for accurate ghost trail
            double px = mc.thePlayer.posX;
            double py = mc.thePlayer.posY;
            double pz = mc.thePlayer.posZ;
            packets.offer(new PacketData((Packet<?>) packet, System.currentTimeMillis(), px, py, pz));
            return false;
        }

        return true;
    }

    public void flush() {
        while (!packets.isEmpty()) {
            PacketData data = packets.poll();
            if (data != null) {
                sendPacketDirect(data.packet);
            }
        }
    }

    private void handlePackets() {
        long now = System.currentTimeMillis();
        long delay = (long) currentPing;

        if (packets.isEmpty()) {
            return;
        }

        PacketData oldest = packets.peek();
        if (oldest == null) return;

        // Track max queue size while building up
        if (!isReleasing) {
            int currentSize = packets.size();
            if (currentSize > maxQueueSize) {
                maxQueueSize = currentSize;
            }
        }

        // Start release mode when delay expires
        if (!isReleasing) {
            long elapsed = now - oldest.timestamp;
            if (elapsed < delay) return;

            isReleasing = true;
            hasStartedReleasing = true;
            // Lock in max queue size when release starts
            maxQueueSize = packets.size();
        }

        // Release packets at speed per tick
        int released = 0;
        int releaseSpeed = (int) speed.getValue();

        while (!packets.isEmpty() && released < releaseSpeed) {
            // Auto-disable when queue reaches 1
            if (packets.size() <= 1) {
                PacketData data = packets.poll();
                if (data != null) {
                    // Update ghost to stored position
                    ghostPosition = new Vec3(data.posX, data.posY, data.posZ);
                    sendPacketDirect(data.packet);
                }
                isReleasing = false;
                hasStartedReleasing = false;
                this.setEnabled(false);
                return;
            }

            PacketData data = packets.poll();
            if (data != null) {
                // Update ghost to stored position - follows the trail
                ghostPosition = new Vec3(data.posX, data.posY, data.posZ);
                sendPacketDirect(data.packet);
                released++;
            }
        }

        if (packets.isEmpty()) {
            isReleasing = false;
            hasStartedReleasing = false;
            this.setEnabled(false);
        }
    }

    private void sendPacketDirect(Packet<?> packet) {
        try {
            isSendingDirect = true;
            if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
                mc.thePlayer.sendQueue.getNetworkManager().sendPacket(packet);
            }
        } catch (Exception e) {
        } finally {
            isSendingDirect = false;
        }
    }

    @Subscribe
    public void render3D(Render3DEvent event) {
        if (!enabled) return;
        if (mc.thePlayer == null) return;

        // Smooth animation - ghost moves toward its actual position
        double animSpeed = 0.3;
        animX += (ghostPosition.xCoord - animX) * animSpeed;
        animY += (ghostPosition.yCoord - animY) * animSpeed;
        animZ += (ghostPosition.zCoord - animZ) * animSpeed;

        RenderManager rm = mc.getRenderManager();
        double x = animX - rm.viewerPosX;
        double y = animY - rm.viewerPosY;
        double z = animZ - rm.viewerPosZ;

        double width = 0.3;
        double height = 1.8;

        AxisAlignedBB box = new AxisAlignedBB(
            x - width, y, z - width,
            x + width, y + height, z + width
        );

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0f);

        // Purple color for the ghost box
        // Fill: semi-transparent purple
        GlStateManager.color(0.6f, 0.2f, 0.8f, 0.25f);
        drawFilledBox(box);

        // Outline: brighter purple
        GlStateManager.color(0.7f, 0.3f, 1.0f, 0.8f);
        RenderGlobal.drawSelectionBoundingBox(box);

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    @Subscribe
    public void render2D(Render2DEvent event) {
        if (!enabled) return;
        if (mc.thePlayer == null) return;

        // Load textures on first use (avoids lag on enable)
        if (!texturesLoaded) {
            loadTextures();
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        // Image dimensions (20% smaller: 64 * 0.8 = ~51)
        int imgWidth = 51;
        int imgHeight = 51;

        // Position slightly above center (moved up 10px more)
        int imgX = centerX - imgWidth / 2;
        int imgY = centerY - 70;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        if (isReleasing && connectedTexture != null) {
            // Show CONNECTED the entire time while releasing packets
            mc.getTextureManager().bindTexture(connectedTexture);
            drawTexturedRect(imgX, imgY, imgWidth, imgHeight);
        } else if (packets.size() > 0 && laggingTexture != null) {
            // Show LAGGING while building up packets
            mc.getTextureManager().bindTexture(laggingTexture);
            drawTexturedRect(imgX, imgY, imgWidth, imgHeight);
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void drawTexturedRect(int x, int y, int width, int height) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + height, 0).tex(0, 1).endVertex();
        wr.pos(x + width, y + height, 0).tex(1, 1).endVertex();
        wr.pos(x + width, y, 0).tex(1, 0).endVertex();
        wr.pos(x, y, 0).tex(0, 0).endVertex();
        tessellator.draw();
    }

    private void drawFilledBox(AxisAlignedBB box) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        wr.pos(box.minX, box.minY, box.minZ).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).endVertex();

        wr.pos(box.minX, box.maxY, box.minZ).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).endVertex();

        wr.pos(box.minX, box.minY, box.minZ).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).endVertex();

        wr.pos(box.minX, box.minY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).endVertex();

        wr.pos(box.minX, box.minY, box.minZ).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).endVertex();

        wr.pos(box.maxX, box.minY, box.minZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).endVertex();

        tessellator.draw();
    }

    public static boolean isSendingDirectPacket() {
        return isSendingDirect;
    }

    @Override
    public String getDisplaySuffix() {
        int blocksVal = (int) blocks.getValue();
        int queueSize = packets.size();
        return " §7" + blocksVal + "B §8[" + queueSize + "]";
    }

    private static class PacketData {
        final Packet<?> packet;
        final long timestamp;
        final double posX, posY, posZ; // Store player position when packet was queued

        PacketData(Packet<?> packet, long timestamp, double posX, double posY, double posZ) {
            this.packet = packet;
            this.timestamp = timestamp;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }
    }
}
