/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * CustomRenderBlocks - Utilidad para renderizar bloques en el mundo
 * Proporciona métodos helper para calcular posiciones y dibujar cajas
 * Usa Tessellator para mejor rendimiento (alternativa a GL11 directo)
 *
 * Para nuevos módulos de bloques/items, usa esta clase así:
 *
 * 1. En tu módulo, crea un método render3D(float partialTicks)
 * 2. Obtén la cámara: RenderManager rm = mc.getRenderManager();
 * 3. Calcula offset: double x = blockPos.getX() - rm.viewerPosX;
 * 4. Usa los métodos estáticos de esta clase para dibujar
 */
public class CustomRenderBlocks {

    private static CustomRenderBlocks instance;

    // Render position (camera offset)
    private double renderPosX, renderPosY, renderPosZ;

    private CustomRenderBlocks() {}

    public static CustomRenderBlocks getInstance() {
        if (instance == null) {
            instance = new CustomRenderBlocks();
        }
        return instance;
    }

    /**
     * Actualiza la posición de la cámara desde RenderManager
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

    // =====================================================
    // MÉTODOS ESTÁTICOS DE UTILIDAD PARA DIBUJAR
    // =====================================================

    /**
     * Dibuja una caja rellena usando Tessellator (mejor rendimiento)
     */
    public static void drawFilledBox(AxisAlignedBB box) {
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

    /**
     * Dibuja una caja rellena usando GL11 directo (más simple, menos eficiente)
     */
    public static void drawFilledBoxGL(AxisAlignedBB box) {
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

    /**
     * Prepara el estado GL para renderizar bloques
     * Llama a esto antes de dibujar
     */
    public static void setupBlockRenderState(float lineWidth, boolean throughWalls) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(lineWidth);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);

        if (throughWalls) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        GL11.glDepthMask(false);
    }

    /**
     * Restaura el estado GL después de renderizar
     * Llama a esto después de dibujar
     */
    public static void restoreBlockRenderState() {
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /**
     * Método helper completo para renderizar un bloque con outline y fill
     */
    public static void renderBlock(BlockPos pos, double camX, double camY, double camZ,
                                   float r, float g, float b, float outlineAlpha, float fillAlpha,
                                   float lineWidth, boolean throughWalls) {
        double x = pos.getX() - camX;
        double y = pos.getY() - camY;
        double z = pos.getZ() - camZ;

        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0);

        setupBlockRenderState(lineWidth, throughWalls);

        // Draw outline
        GL11.glColor4f(r, g, b, outlineAlpha);
        RenderGlobal.drawSelectionBoundingBox(box);

        // Draw fill
        if (fillAlpha > 0) {
            GL11.glColor4f(r, g, b, fillAlpha);
            drawFilledBoxGL(box);
        }

        restoreBlockRenderState();
    }
}
