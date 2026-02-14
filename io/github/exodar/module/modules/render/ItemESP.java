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
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.List;

/**
 * ItemESP - Highlights valuable items on the ground
 * Shows colored boxes around emeralds, diamonds, gold, iron
 */
public class ItemESP extends Module {

    private final SliderSetting opacity;
    private final TickSetting outline;
    private final TickSetting itemCount;
    private final TickSetting coloredText;
    private final TickSetting textOutline;
    private final TickSetting autoScale;
    private final TickSetting throughWalls;

    // Item filters
    private final TickSetting emeralds;
    private final TickSetting diamonds;
    private final TickSetting gold;
    private final TickSetting iron;

    // Called from CustomRenderPlayer.doRender() with frame tracking there

    public ItemESP() {
        super("ItemESP", ModuleCategory.VISUALS);
        this.registerSetting(new DescriptionSetting("Highlight valuable items"));

        this.registerSetting(opacity = new SliderSetting("Opacity", 25.0, 0.0, 100.0, 5.0));
        this.registerSetting(outline = new TickSetting("Outline", true));
        this.registerSetting(itemCount = new TickSetting("Show Count", true));
        this.registerSetting(coloredText = new TickSetting("Colored Text", true));
        this.registerSetting(textOutline = new TickSetting("Text Outline", true));
        this.registerSetting(autoScale = new TickSetting("Auto Scale", true));
        this.registerSetting(throughWalls = new TickSetting("Through Walls", true));

        this.registerSetting(new DescriptionSetting("--- Item Filters ---"));
        this.registerSetting(emeralds = new TickSetting("Emeralds", true));
        this.registerSetting(diamonds = new TickSetting("Diamonds", true));
        this.registerSetting(gold = new TickSetting("Gold", true));
        this.registerSetting(iron = new TickSetting("Iron", true));
    }

    // Colors as float arrays [r, g, b] to avoid object creation
    private static final float[] COLOR_EMERALD = {85/255f, 1f, 85/255f};
    private static final float[] COLOR_DIAMOND = {85/255f, 1f, 1f};
    private static final float[] COLOR_GOLD = {1f, 1f, 85/255f};
    private static final float[] COLOR_IRON = {1f, 1f, 1f};
    private static final float[] COLOR_DEFAULT = {170/255f, 170/255f, 170/255f};

    /**
     * Check if an item should be highlighted based on filters
     */
    private boolean shouldHighlightItem(int itemId) {
        return (emeralds.isEnabled() && isEmeraldItem(itemId))
            || (diamonds.isEnabled() && isDiamondItem(itemId))
            || (gold.isEnabled() && isGoldItem(itemId))
            || (iron.isEnabled() && isIronItem(itemId));
    }

    private boolean isEmeraldItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.emerald || block == Blocks.emerald_block || block == Blocks.emerald_ore;
    }

    private boolean isDiamondItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.diamond
            || item == Items.diamond_sword
            || item == Items.diamond_pickaxe
            || item == Items.diamond_shovel
            || item == Items.diamond_axe
            || item == Items.diamond_hoe
            || item == Items.diamond_helmet
            || item == Items.diamond_chestplate
            || item == Items.diamond_leggings
            || item == Items.diamond_boots
            || block == Blocks.diamond_block
            || block == Blocks.diamond_ore;
    }

    private boolean isGoldItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.gold_ingot
            || item == Items.gold_nugget
            || item == Items.golden_apple
            || item == Items.golden_sword
            || item == Items.golden_pickaxe
            || item == Items.golden_shovel
            || item == Items.golden_axe
            || item == Items.golden_hoe
            || item == Items.golden_helmet
            || item == Items.golden_chestplate
            || item == Items.golden_leggings
            || item == Items.golden_boots
            || block == Blocks.gold_block
            || block == Blocks.gold_ore;
    }

    private boolean isIronItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return item == Items.iron_ingot
            || item == Items.iron_sword
            || item == Items.iron_pickaxe
            || item == Items.iron_shovel
            || item == Items.iron_axe
            || item == Items.iron_hoe
            || item == Items.iron_helmet
            || item == Items.iron_chestplate
            || item == Items.iron_leggings
            || item == Items.iron_boots
            || block == Blocks.iron_block
            || block == Blocks.iron_ore;
    }

    private float[] getItemColor(int itemId) {
        if (isEmeraldItem(itemId)) return COLOR_EMERALD;
        if (isDiamondItem(itemId)) return COLOR_DIAMOND;
        if (isGoldItem(itemId)) return COLOR_GOLD;
        if (isIronItem(itemId)) return COLOR_IRON;
        return COLOR_DEFAULT;
    }

    /**
     * Get item color as ARGB int for text rendering
     */
    private int getItemColorInt(int itemId) {
        if (isEmeraldItem(itemId)) return 0xFF55FF55; // Green
        if (isDiamondItem(itemId)) return 0xFF55FFFF; // Aqua
        if (isGoldItem(itemId)) return 0xFFFFFF55;    // Yellow
        if (isIronItem(itemId)) return 0xFFFFFFFF;    // White
        return 0xFFAAAAAA;                            // Gray
    }

    private int getItemPriority(int itemId) {
        if (isEmeraldItem(itemId)) return 4;
        if (isDiamondItem(itemId)) return 3;
        if (isGoldItem(itemId)) return 2;
        if (isIronItem(itemId)) return 1;
        return 0;
    }

    /**
     * Check if item is a block (not raw material/ingot/tool)
     * Blocks with count 1 won't show the number
     */
    private boolean isBlockItem(int itemId) {
        Item item = Item.getItemById(itemId);
        Block block = Block.getBlockFromItem(item);
        return block == Blocks.emerald_block
            || block == Blocks.diamond_block
            || block == Blocks.gold_block
            || block == Blocks.iron_block
            || block == Blocks.emerald_ore
            || block == Blocks.diamond_ore
            || block == Blocks.gold_ore
            || block == Blocks.iron_ore;
    }

    /**
     * Main 3D render method - called from Main.onRenderWorld()
     */
    @Subscribe
    public void render3D(Render3DEvent event) {
        float partialTicks = event.getPartialTicks();
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        // Don't render when GUI is open (already checked in Main.onRenderWorld)
        if (mc.currentScreen != null) return;

        RenderManager rm = mc.getRenderManager();
        if (rm == null) return;

        // Use RenderManager camera position
        double camX = rm.viewerPosX;
        double camY = rm.viewerPosY;
        double camZ = rm.viewerPosZ;

        // Collect items and group by position + type
        LinkedHashMap<ItemData, Integer> itemMap = new LinkedHashMap<>();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityItem)) continue;
            if (entity.ticksExisted < 3) continue; // Skip newly spawned items

            EntityItem entityItem = (EntityItem) entity;
            ItemStack stack = entityItem.getEntityItem();
            if (stack == null || stack.stackSize <= 0) continue;

            int itemId = Item.getIdFromItem(stack.getItem());
            if (!shouldHighlightItem(itemId)) continue;

            // Interpolate position
            double x = lerp(entityItem.lastTickPosX, entityItem.posX, partialTicks);
            double y = lerp(entityItem.lastTickPosY, entityItem.posY, partialTicks);
            double z = lerp(entityItem.lastTickPosZ, entityItem.posZ, partialTicks);

            ItemData data = new ItemData(itemId, x, y, z);
            Integer existing = itemMap.get(data);
            itemMap.put(data, stack.stackSize + (existing == null ? 0 : existing));
        }

        // Sort by priority and render (limit to 30 items to prevent lag)
        List<Map.Entry<ItemData, Integer>> sorted = new ArrayList<>(itemMap.entrySet());
        sorted.sort((e1, e2) -> Integer.compare(getItemPriority(e2.getKey().itemId), getItemPriority(e1.getKey().itemId))); // Higher priority first

        int rendered = 0;
        for (Map.Entry<ItemData, Integer> entry : sorted) {
            if (rendered++ >= 30) break;
            ItemData data = entry.getKey();
            int count = entry.getValue();
            float[] color = getItemColor(data.itemId);

            double x = data.x - camX;
            double y = data.y - camY;
            double z = data.z - camZ;

            double distance = mc.thePlayer.getDistance(data.x, data.y, data.z);
            double scale = autoScale.isEnabled()
                ? 0.5 + 0.375 * ((Math.max(6.0, distance) - 6.0) / 28.0)
                : 0.5;

            // Create bounding box
            AxisAlignedBB box = new AxisAlignedBB(
                x - scale * 0.5, y, z - scale * 0.5,
                x + scale * 0.5, y + scale, z + scale * 0.5
            );

            // Render box
            renderItemBox(box, color[0], color[1], color[2]);

            // Render count text (skip for blocks with count 1)
            if (itemCount.isEnabled() && count > 0) {
                boolean skipCount = (count == 1 && isBlockItem(data.itemId));
                if (!skipCount) {
                    renderItemCount(x, y + scale * 0.5, z, count, data.itemId, distance, rm, mc);
                }
            }
        }
    }

    private void renderItemBox(AxisAlignedBB box, float r, float g, float b) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(1.5f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);

        if (throughWalls.isEnabled()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        GL11.glDepthMask(false);

        // Filled box
        if (opacity.getValue() > 0) {
            float alpha = (float)(opacity.getValue() / 100.0);
            GL11.glColor4f(r, g, b, alpha);
            drawFilledBox(box);
        }

        // Outline
        if (outline.isEnabled()) {
            GL11.glColor4f(r, g, b, 1.0f);
            RenderGlobal.drawSelectionBoundingBox(box);
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderItemCount(double x, double y, double z, int count, int itemId, double distance, RenderManager rm, Minecraft mc) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        // Face camera (billboard)
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        float flip = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
        GlStateManager.rotate(rm.playerViewX * flip, 1.0F, 0.0F, 0.0F);

        // Scale based on distance
        double fontScale = autoScale.isEnabled()
            ? -0.04375 - 0.0328125 * ((Math.max(6.0, distance) - 6.0) / 28.0)
            : -0.04375;
        GlStateManager.scale(fontScale, fontScale, 1.0);

        GlStateManager.disableLighting();
        if (throughWalls.isEnabled()) {
            GlStateManager.disableDepth();
        }

        String countText = String.valueOf(count);
        FontRenderer fr = mc.fontRendererObj;
        int textWidth = fr.getStringWidth(countText);
        int textHeight = fr.FONT_HEIGHT;

        float drawX = -textWidth / 2.0f;
        float drawY = -textHeight / 2.0f;

        // Get text color
        int textColor = coloredText.isEnabled() ? getItemColorInt(itemId) : 0xFFFFFFFF;

        // Draw with outline or shadow
        if (textOutline.isEnabled()) {
            // Draw black outline (4 directions)
            int outlineColor = 0xFF000000;
            int ix = (int) drawX;
            int iy = (int) drawY;
            fr.drawString(countText, ix - 1, iy, outlineColor);
            fr.drawString(countText, ix + 1, iy, outlineColor);
            fr.drawString(countText, ix, iy - 1, outlineColor);
            fr.drawString(countText, ix, iy + 1, outlineColor);
            // Draw main text on top
            fr.drawString(countText, ix, iy, textColor);
        } else {
            // Draw with shadow
            fr.drawStringWithShadow(countText, drawX, drawY, textColor);
        }

        GlStateManager.enableLighting();
        if (throughWalls.isEnabled()) {
            GlStateManager.enableDepth();
        }

        GlStateManager.popMatrix();
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

    private double lerp(double prev, double curr, float partialTicks) {
        return prev + (curr - prev) * partialTicks;
    }

    /**
     * Data class for grouping items by position and type
     */
    public static class ItemData {
        private final int hashCode;
        public final int itemId;
        public final double x;
        public final double y;
        public final double z;

        public ItemData(int id, double x, double y, double z) {
            this.itemId = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.hashCode = Objects.hash(id, (int) x, (int) y, (int) z);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            ItemData itemData = (ItemData) object;
            return itemId == itemData.itemId
                && (int) x == (int) itemData.x
                && (int) y == (int) itemData.y
                && (int) z == (int) itemData.z;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
