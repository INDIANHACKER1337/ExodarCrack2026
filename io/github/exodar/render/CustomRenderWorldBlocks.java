/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * CustomRenderWorldBlocks - Utilidad para renderizar bloques en el mundo
 * Proporciona métodos helper para calcular posiciones y dibujar cajas
 * Puede ser usado por cualquier módulo que necesite renderizar bloques
 */
public class CustomRenderWorldBlocks {

    private static CustomRenderWorldBlocks instance;

    // Render offset (camera position)
    private double renderPosX;
    private double renderPosY;
    private double renderPosZ;

    private CustomRenderWorldBlocks() {
        // Private constructor - use getInstance()
    }

    public static CustomRenderWorldBlocks getInstance() {
        if (instance == null) {
            instance = new CustomRenderWorldBlocks();
        }
        return instance;
    }

    /**
     * Actualiza la posición de la cámara - llamar antes de renderizar
     */
    public void updateCameraPosition() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        RenderManager rm = mc.getRenderManager();
        if (rm == null) return;

        this.renderPosX = rm.viewerPosX;
        this.renderPosY = rm.viewerPosY;
        this.renderPosZ = rm.viewerPosZ;
    }

    /**
     * Get render X offset for a block position
     */
    public double getRenderX(BlockPos pos) {
        return pos.getX() - renderPosX;
    }

    /**
     * Get render Y offset for a block position
     */
    public double getRenderY(BlockPos pos) {
        return pos.getY() - renderPosY;
    }

    /**
     * Get render Z offset for a block position
     */
    public double getRenderZ(BlockPos pos) {
        return pos.getZ() - renderPosZ;
    }

    /**
     * Get render X offset for world coordinates
     */
    public double getRenderX(double worldX) {
        return worldX - renderPosX;
    }

    /**
     * Get render Y offset for world coordinates
     */
    public double getRenderY(double worldY) {
        return worldY - renderPosY;
    }

    /**
     * Get render Z offset for world coordinates
     */
    public double getRenderZ(double worldZ) {
        return worldZ - renderPosZ;
    }

    /**
     * Helper method to render a block outline
     */
    public void renderBlockOutline(BlockPos pos, float r, float g, float b, float a, float lineWidth, boolean throughWalls) {
        double x = getRenderX(pos);
        double y = getRenderY(pos);
        double z = getRenderZ(pos);

        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(lineWidth);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        if (throughWalls) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        GlStateManager.depthMask(false);

        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0);

        GL11.glColor4f(r, g, b, a);
        RenderGlobal.drawSelectionBoundingBox(box);

        GlStateManager.depthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (throughWalls) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    /**
     * Helper method to render a filled block
     */
    public void renderBlockFilled(BlockPos pos, float r, float g, float b, float a, boolean throughWalls) {
        double x = getRenderX(pos);
        double y = getRenderY(pos);
        double z = getRenderZ(pos);

        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        if (throughWalls) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        GlStateManager.depthMask(false);

        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0);

        GL11.glColor4f(r, g, b, a);
        drawFilledBox(box);

        GlStateManager.depthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (throughWalls) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    /**
     * Draw a filled box
     */
    public static void drawFilledBox(AxisAlignedBB box) {
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
}
