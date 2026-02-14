/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.event.Render3DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
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

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Ping - 1:1 Sakura port
 * Delays outgoing packets using drift system
 */
public class Ping extends Module {

    // Settings
    private final SliderSetting speed;
    private final SliderSetting maxMs;
    private final TickSetting autoDisable;

    // Packet queue
    private final ConcurrentLinkedQueue<PacketData> packets = new ConcurrentLinkedQueue<>();

    // Position tracking
    private Vec3 position = new Vec3(0, 0, 0);
    private double animX, animY, animZ;

    // Sakura drift system - ACCUMULATES each tick
    private int drift = 0;

    // Real time tracking for Max MS
    private long enableTime = 0;

    // Flag to prevent recursion
    private static boolean isSendingDirect = false;

    public Ping() {
        super("Ping", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Delays outgoing packets"));
        this.registerSetting(speed = new SliderSetting("Speed", 24.0, 1.0, 100.0, 1.0));
        this.registerSetting(maxMs = new SliderSetting("Max MS", 1000.0, 100.0, 3000.0, 50.0));
        this.registerSetting(autoDisable = new TickSetting("Auto Disable", true));
    }

    @Override
    public void onEnable() {
        packets.clear();
        drift = 0;
        enableTime = System.currentTimeMillis();
        isSendingDirect = false;

        if (mc.thePlayer != null) {
            position = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            animX = position.xCoord;
            animY = position.yCoord;
            animZ = position.zCoord;
        }
    }

    @Override
    public void onDisable() {
        // Sakura: flush on disable
        flush();
        packets.clear();
        drift = 0;
        isSendingDirect = false;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Sakura: skip in singleplayer or first 20 ticks
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) {
            flush();
            return;
        }

        // Sakura: flush if eating
        if (mc.thePlayer.isEating()) {
            flush();
            return;
        }

        // Check max MS limit (REAL TIME)
        long elapsed = System.currentTimeMillis() - enableTime;
        if (elapsed >= (long) maxMs.getValue()) {
            flush();
            if (autoDisable.isEnabled()) {
                setEnabled(false);
            } else {
                // Reset timer for next cycle
                enableTime = System.currentTimeMillis();
                drift = 0;
            }
            return;
        }

        // Sakura: drift += speed * 2 (ACCUMULATE each tick!)
        drift += (int) (speed.getValue() * 2);

        // Sakura: packets().handle(drift)
        handlePackets(drift);
    }

    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled) return true;
        if (isSendingDirect) return true;
        if (mc.thePlayer == null || mc.theWorld == null) return true;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) return true;

        // Sakura: packets().stall(event) - queue ALL outgoing packets
        if (packet instanceof Packet) {
            packets.offer(new PacketData((Packet<?>) packet, System.currentTimeMillis()));
            return false; // Cancel original send
        }

        return true;
    }

    /**
     * Sakura: packets().flush() - send all packets immediately
     */
    public void flush() {
        handlePackets(0);
        drift = 0;
    }

    /**
     * Sakura: packets().handle(drift)
     * Release packets where elapsed time >= drift
     * If drift == 0, release ALL packets (flush)
     */
    private void handlePackets(int drift) {
        while (!packets.isEmpty()) {
            PacketData data = packets.peek();
            if (data == null) break;

            long elapsed = System.currentTimeMillis() - data.timestamp;

            // Sakura logic: if drift is 0 (flush) or elapsed >= drift, send packet
            if (drift == 0 || elapsed >= drift) {
                packets.poll();
                updatePositionFromPacket(data.packet);

                try {
                    isSendingDirect = true;
                    if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
                        mc.thePlayer.sendQueue.getNetworkManager().sendPacket(data.packet);
                    }
                } catch (Exception e) {
                } finally {
                    isSendingDirect = false;
                }
            } else {
                // Sakura: stop processing when we hit a packet that's not ready yet
                break;
            }
        }
    }

    /**
     * Extract position from C03PacketPlayer variants for visual tracking
     */
    private void updatePositionFromPacket(Packet<?> packet) {
        try {
            String className = packet.getClass().getSimpleName();
            if (className.contains("C03") || className.contains("PacketPlayer")) {
                for (java.lang.reflect.Field field : packet.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    String name = field.getName().toLowerCase();
                    if (name.contains("x") || name.equals("a")) {
                        Object val = field.get(packet);
                        if (val instanceof Double) {
                            double x = (Double) val;
                            if (x != 0) {
                                for (java.lang.reflect.Field f2 : packet.getClass().getDeclaredFields()) {
                                    f2.setAccessible(true);
                                    String n2 = f2.getName().toLowerCase();
                                    Object v2 = f2.get(packet);
                                    if (v2 instanceof Double) {
                                        double d = (Double) v2;
                                        if (n2.contains("x") || n2.equals("a")) {
                                            position = new Vec3(d, position.yCoord, position.zCoord);
                                        } else if (n2.contains("y") || n2.equals("b")) {
                                            position = new Vec3(position.xCoord, d, position.zCoord);
                                        } else if (n2.contains("z") || n2.equals("c")) {
                                            position = new Vec3(position.xCoord, position.yCoord, d);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    @Subscribe
    public void render3D(Render3DEvent event) {
        if (!enabled) return;
        if (mc.thePlayer == null) return;

        // Sakura: only show in third person
        if (mc.gameSettings.thirdPersonView == 0) return;

        // Animate position smoothly
        double animSpeed = 0.2;
        animX += (position.xCoord - animX) * animSpeed;
        animY += (position.yCoord - animY) * animSpeed;
        animZ += (position.zCoord - animZ) * animSpeed;

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

        GlStateManager.color(0.0f, 0.8f, 1.0f, 0.3f);
        drawFilledBox(box);

        GlStateManager.color(0.0f, 0.8f, 1.0f, 0.8f);
        RenderGlobal.drawSelectionBoundingBox(box);

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
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

    public boolean shouldAllowOutgoingPacket(Object packet) {
        return onSendPacket(packet);
    }

    public boolean shouldAllowIncomingPacket(Object packet) {
        return true;
    }

    @Override
    public String getDisplaySuffix() {
        long elapsed = System.currentTimeMillis() - enableTime;
        int max = (int) maxMs.getValue();
        return " §7" + elapsed + "/" + max + "ms §8[" + packets.size() + "]";
    }

    private static class PacketData {
        final Packet<?> packet;
        final long timestamp;

        PacketData(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}
