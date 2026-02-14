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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

/**
 * ChestESP - Highlights chests through walls
 * Based on Myau ChestESP
 */
public class ChestESP extends Module {

    private ColorSetting chestColor;
    private ColorSetting enderColor;
    private SliderSetting opacity;
    private TickSetting outline;
    private TickSetting tracers;
    private TickSetting showEnderChests;

    public ChestESP() {
        super("ChestESP", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Highlights chests"));
        this.registerSetting(chestColor = new ColorSetting("Chest Color", 255, 170, 0));
        this.registerSetting(enderColor = new ColorSetting("Ender Color", 128, 0, 128));
        this.registerSetting(opacity = new SliderSetting("Opacity", 80.0, 10.0, 100.0, 5.0));
        this.registerSetting(outline = new TickSetting("Outline", true));
        this.registerSetting(tracers = new TickSetting("Tracers", false));
        this.registerSetting(showEnderChests = new TickSetting("Ender Chests", true));
    }

    /**
     * Render ChestESP - called from Render3DManager
     */
    @Subscribe
    public void render3D(Render3DEvent event) {
        float partialTicks = event.getPartialTicks();
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

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
        GL11.glLineWidth(1.5f);

        float alpha = (float) (opacity.getValue() / 100.0);

        // Limit to 100 chests to prevent lag
        int rendered = 0;
        for (TileEntity te : mc.theWorld.loadedTileEntityList) {
            if (rendered >= 100) break;
            if (te instanceof TileEntityChest) {
                rendered++;
                renderChestBox(te, renderX, renderY, renderZ, chestColor, alpha);
                if (tracers.isEnabled()) {
                    drawTracer(te, renderX, renderY, renderZ, chestColor, alpha, partialTicks);
                }
            } else if (te instanceof TileEntityEnderChest && showEnderChests.isEnabled()) {
                rendered++;
                renderChestBox(te, renderX, renderY, renderZ, enderColor, alpha);
                if (tracers.isEnabled()) {
                    drawTracer(te, renderX, renderY, renderZ, enderColor, alpha, partialTicks);
                }
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

    private void renderChestBox(TileEntity chest, double renderX, double renderY, double renderZ, ColorSetting color, float alpha) {
        // Chest bounding box (slightly inset from full block)
        double x1 = chest.getPos().getX() + 0.0625 - renderX;
        double y1 = chest.getPos().getY() - renderY;
        double z1 = chest.getPos().getZ() + 0.0625 - renderZ;
        double x2 = chest.getPos().getX() + 0.9375 - renderX;
        double y2 = chest.getPos().getY() + 0.875 - renderY;
        double z2 = chest.getPos().getZ() + 0.9375 - renderZ;

        AxisAlignedBB box = new AxisAlignedBB(x1, y1, z1, x2, y2, z2);

        int c = color.getColor();
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;

        // Draw filled box
        GlStateManager.color(r, g, b, alpha * 0.3f);
        drawFilledBox(box);

        // Draw outline
        if (outline.isEnabled()) {
            GlStateManager.color(r, g, b, alpha);
            RenderGlobal.drawSelectionBoundingBox(box);
        }
    }

    private void drawTracer(TileEntity chest, double renderX, double renderY, double renderZ, ColorSetting color, float alpha, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();

        // Target position (center of chest)
        double targetX = chest.getPos().getX() + 0.5 - renderX;
        double targetY = chest.getPos().getY() + 0.5 - renderY;
        double targetZ = chest.getPos().getZ() + 0.5 - renderZ;

        // Start position (player eyes, relative to camera)
        double startX, startY, startZ;
        if (mc.gameSettings.thirdPersonView == 0) {
            // First person - from camera direction
            float yaw = mc.thePlayer.rotationYaw;
            float pitch = mc.thePlayer.rotationPitch;
            double dirX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            double dirY = -Math.sin(Math.toRadians(pitch));
            double dirZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            startX = dirX;
            startY = dirY + mc.thePlayer.getEyeHeight();
            startZ = dirZ;
        } else {
            // Third person
            startX = 0;
            startY = mc.thePlayer.getEyeHeight();
            startZ = 0;
        }

        int c = color.getColor();
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;

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
}
