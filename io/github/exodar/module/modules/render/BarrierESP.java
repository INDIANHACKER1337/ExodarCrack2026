/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.event.Render3DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * BarrierESP - Shows invisible barrier blocks as red crystal
 * Scans around player and renders barriers visibly
 */
public class BarrierESP extends Module {

    private final ColorSetting color;
    private final SliderSetting opacity;
    private final SliderSetting range;
    private final TickSetting outline;
    private final TickSetting filled;
    private final TickSetting tracers;

    // Cache barrier positions to avoid scanning every frame
    private final List<BlockPos> barrierCache = new ArrayList<>();
    private long lastScan = 0;
    private static final long SCAN_INTERVAL = 500; // Scan every 500ms

    public BarrierESP() {
        super("BarrierESP", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Shows barrier blocks"));
        this.registerSetting(color = new ColorSetting("Color", 255, 50, 50, 200)); // Red crystal
        this.registerSetting(opacity = new SliderSetting("Opacity", 60.0, 10.0, 100.0, 5.0));
        this.registerSetting(range = new SliderSetting("Range", 32.0, 8.0, 64.0, 4.0));
        this.registerSetting(outline = new TickSetting("Outline", true));
        this.registerSetting(filled = new TickSetting("Filled", true));
        this.registerSetting(tracers = new TickSetting("Tracers", false));
    }

    @Override
    public void onEnable() {
        barrierCache.clear();
        lastScan = 0;
    }

    @Override
    public void onDisable() {
        barrierCache.clear();
    }

    /**
     * Scan for barrier blocks around the player
     */
    private void scanForBarriers() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        barrierCache.clear();

        int scanRange = (int) range.getValue();
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        // Scan cube around player
        for (int x = -scanRange; x <= scanRange; x++) {
            for (int y = -scanRange; y <= scanRange; y++) {
                for (int z = -scanRange; z <= scanRange; z++) {
                    BlockPos pos = playerPos.add(x, y, z);

                    // Check if block is barrier
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    if (block == Blocks.barrier) {
                        barrierCache.add(pos);
                    }
                }
            }
        }

        lastScan = System.currentTimeMillis();
    }

    @Subscribe
    public void render3D(Render3DEvent event) {
        float partialTicks = event.getPartialTicks();
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        // Rescan if needed
        long now = System.currentTimeMillis();
        if (now - lastScan > SCAN_INTERVAL) {
            scanForBarriers();
        }

        if (barrierCache.isEmpty()) return;

        RenderManager rm = mc.getRenderManager();
        double renderX = rm.viewerPosX;
        double renderY = rm.viewerPosY;
        double renderZ = rm.viewerPosZ;

        // Setup GL state
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2.0f);

        float alpha = (float) (opacity.getValue() / 100.0);

        int c = color.getColor();
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;

        // Render each barrier
        for (BlockPos pos : barrierCache) {
            // Skip if too far (player might have moved)
            if (mc.thePlayer.getDistanceSq(pos) > range.getValue() * range.getValue()) {
                continue;
            }

            renderBarrierBlock(pos, renderX, renderY, renderZ, r, g, b, alpha);

            if (tracers.isEnabled()) {
                drawTracer(pos, renderX, renderY, renderZ, r, g, b, alpha, partialTicks);
            }
        }

        // Restore GL state
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderBarrierBlock(BlockPos pos, double renderX, double renderY, double renderZ,
                                    float r, float g, float b, float alpha) {
        // Full block bounding box
        double x1 = pos.getX() - renderX;
        double y1 = pos.getY() - renderY;
        double z1 = pos.getZ() - renderZ;
        double x2 = pos.getX() + 1 - renderX;
        double y2 = pos.getY() + 1 - renderY;
        double z2 = pos.getZ() + 1 - renderZ;

        AxisAlignedBB box = new AxisAlignedBB(x1, y1, z1, x2, y2, z2);

        // Draw filled box (crystal effect - semi-transparent)
        if (filled.isEnabled()) {
            GlStateManager.color(r, g, b, alpha * 0.4f);
            drawFilledBox(box);
        }

        // Draw outline
        if (outline.isEnabled()) {
            GlStateManager.color(r, g, b, alpha);
            RenderGlobal.drawSelectionBoundingBox(box);
        }
    }

    private void drawTracer(BlockPos pos, double renderX, double renderY, double renderZ,
                           float r, float g, float b, float alpha, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();

        // Target position (center of block)
        double targetX = pos.getX() + 0.5 - renderX;
        double targetY = pos.getY() + 0.5 - renderY;
        double targetZ = pos.getZ() + 0.5 - renderZ;

        // Start position (player eyes)
        double startX, startY, startZ;
        if (mc.gameSettings.thirdPersonView == 0) {
            float yaw = mc.thePlayer.rotationYaw;
            float pitch = mc.thePlayer.rotationPitch;
            double dirX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            double dirY = -Math.sin(Math.toRadians(pitch));
            double dirZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            startX = dirX;
            startY = dirY + mc.thePlayer.getEyeHeight();
            startZ = dirZ;
        } else {
            startX = 0;
            startY = mc.thePlayer.getEyeHeight();
            startZ = 0;
        }

        GL11.glColor4f(r, g, b, alpha * 0.8f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(startX, startY, startZ);
        GL11.glVertex3d(targetX, targetY, targetZ);
        GL11.glEnd();
    }

    private void drawFilledBox(AxisAlignedBB box) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        // Bottom
        wr.pos(box.minX, box.minY, box.minZ).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).endVertex();

        // Top
        wr.pos(box.minX, box.maxY, box.minZ).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).endVertex();

        // North
        wr.pos(box.minX, box.minY, box.minZ).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).endVertex();
        wr.pos(box.maxX, box.minY, box.minZ).endVertex();

        // South
        wr.pos(box.minX, box.minY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).endVertex();

        // West
        wr.pos(box.minX, box.minY, box.minZ).endVertex();
        wr.pos(box.minX, box.minY, box.maxZ).endVertex();
        wr.pos(box.minX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.minX, box.maxY, box.minZ).endVertex();

        // East
        wr.pos(box.maxX, box.minY, box.minZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.minZ).endVertex();
        wr.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        wr.pos(box.maxX, box.minY, box.maxZ).endVertex();

        tessellator.draw();
    }

    @Override
    public String getDisplaySuffix() {
        return barrierCache.isEmpty() ? "" : " §7" + barrierCache.size();
    }
}
