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
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xray - Highlights ores and special blocks through walls
 * Based on keystrokesmod Xray
 */
public class Xray extends Module {

    private SliderSetting range;
    private SliderSetting rate;
    private TickSetting coal;
    private TickSetting iron;
    private TickSetting gold;
    private TickSetting diamond;
    private TickSetting emerald;
    private TickSetting lapis;
    private TickSetting redstone;
    private TickSetting spawner;
    private TickSetting obsidian;

    private final Set<BlockPos> blocks = ConcurrentHashMap.newKeySet();
    private long lastCheck = 0;
    private Thread scanThread = null;

    public Xray() {
        super("Xray", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Highlights ores"));

        this.registerSetting(range = new SliderSetting("Range", 20.0, 5.0, 50.0, 1.0));
        this.registerSetting(rate = new SliderSetting("Rate (sec)", 0.5, 0.1, 3.0, 0.1));

        this.registerSetting(coal = new TickSetting("Coal", false));
        this.registerSetting(iron = new TickSetting("Iron", true));
        this.registerSetting(gold = new TickSetting("Gold", true));
        this.registerSetting(diamond = new TickSetting("Diamond", true));
        this.registerSetting(emerald = new TickSetting("Emerald", true));
        this.registerSetting(lapis = new TickSetting("Lapis", true));
        this.registerSetting(redstone = new TickSetting("Redstone", true));
        this.registerSetting(spawner = new TickSetting("Spawner", true));
        this.registerSetting(obsidian = new TickSetting("Obsidian", false));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        blocks.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        blocks.clear();
    }

    @Override
    public void onUpdate() {
        if (!enabled || mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();
        if (now - lastCheck < rate.getValue() * 1000) return;
        lastCheck = now;

        // Run scan in background thread
        if (scanThread == null || !scanThread.isAlive()) {
            scanThread = new Thread(() -> {
                try {
                    scanForBlocks();
                } catch (Exception e) {
                    // Silent fail
                }
            }, "Exodar-XrayScan");
            scanThread.setDaemon(true);
            scanThread.start();
        }
    }

    private void scanForBlocks() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        int r = (int) range.getValue();
        int playerX = (int) mc.thePlayer.posX;
        int playerY = (int) mc.thePlayer.posY;
        int playerZ = (int) mc.thePlayer.posZ;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = new BlockPos(playerX + x, playerY + y, playerZ + z);
                    if (blocks.contains(pos)) continue;

                    try {
                        Block block = mc.theWorld.getBlockState(pos).getBlock();
                        if (block != null && canHighlight(block)) {
                            blocks.add(pos);
                        }
                    } catch (Exception e) {
                        // Silent fail
                    }
                }
            }
        }
    }

    /**
     * Render highlighted blocks - called from Render3DManager
     */
    @Subscribe
    public void render3D(Render3DEvent event) {
     float partialTicks = event.getPartialTicks();
        if (!enabled || blocks.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        double renderX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double renderY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double renderZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;

        // Setup GL state
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(1.5f);

        Iterator<BlockPos> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();

            try {
                Block block = mc.theWorld.getBlockState(pos).getBlock();
                if (block == null || !canHighlight(block)) {
                    iterator.remove();
                    continue;
                }

                int[] rgb = getBlockColor(block);
                if (rgb[0] + rgb[1] + rgb[2] == 0) continue;

                float r = rgb[0] / 255.0f;
                float g = rgb[1] / 255.0f;
                float b = rgb[2] / 255.0f;

                double x = pos.getX() - renderX;
                double y = pos.getY() - renderY;
                double z = pos.getZ() - renderZ;

                AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);

                // Draw filled box
                GlStateManager.color(r, g, b, 0.3f);
                drawFilledBox(box);

                // Draw outline
                GlStateManager.color(r, g, b, 1.0f);
                RenderGlobal.drawSelectionBoundingBox(box);

            } catch (Exception e) {
                // Silent fail
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

    private int[] getBlockColor(Block block) {
        if (block == Blocks.iron_ore) return new int[]{255, 255, 255};
        if (block == Blocks.gold_ore) return new int[]{255, 215, 0};
        if (block == Blocks.diamond_ore) return new int[]{0, 220, 255};
        if (block == Blocks.emerald_ore) return new int[]{35, 255, 35};
        if (block == Blocks.lapis_ore) return new int[]{0, 50, 255};
        if (block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore) return new int[]{255, 0, 0};
        if (block == Blocks.coal_ore) return new int[]{50, 50, 50};
        if (block == Blocks.mob_spawner) return new int[]{128, 0, 128};
        if (block == Blocks.obsidian) return new int[]{20, 20, 50};
        return new int[]{0, 0, 0};
    }

    private boolean canHighlight(Block block) {
        return (iron.isEnabled() && block == Blocks.iron_ore) ||
               (gold.isEnabled() && block == Blocks.gold_ore) ||
               (diamond.isEnabled() && block == Blocks.diamond_ore) ||
               (emerald.isEnabled() && block == Blocks.emerald_ore) ||
               (lapis.isEnabled() && block == Blocks.lapis_ore) ||
               (redstone.isEnabled() && (block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore)) ||
               (coal.isEnabled() && block == Blocks.coal_ore) ||
               (spawner.isEnabled() && block == Blocks.mob_spawner) ||
               (obsidian.isEnabled() && block == Blocks.obsidian);
    }
}
