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
import io.github.exodar.setting.ColorSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * BedESP - Highlights beds in the world (useful for BedWars)
 * Shows bed box + icons of surrounding defense blocks
 */
public class BedESP extends Module {

    private final SliderSetting range;
    private final SliderSetting updateRate;
    private final TickSetting onlyFirstBed;
    private final TickSetting throughWalls;
    private final TickSetting outline;
    private final ColorSetting color;

    // Surround blocks settings
    private final TickSetting showSurround;
    private final SliderSetting surroundRange;
    private final SliderSetting iconScale;

    private BlockPos[] singleBed = null;
    private final List<BlockPos[]> beds = new ArrayList<>();
    private long lastCheck = 0;

    // Surround blocks - map bed position to defense block counts
    // Key format: "block_name" for normal blocks, "wool_meta" for wool colors
    private final Map<BlockPos, Map<String, Integer>> bedDefenseBlocks = new HashMap<>();

    // Wool color names by metadata
    private static final String[] WOOL_COLORS = {
        "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray",
        "Light Gray", "Cyan", "Purple", "Blue", "Brown", "Green", "Red", "Black"
    };

    public BedESP() {
        super("BedESP", ModuleCategory.VISUALS);
        this.registerSetting(new DescriptionSetting("Highlight beds"));
        this.registerSetting(range = new SliderSetting("Range", 15.0, 5.0, 50.0, 1.0));
        // Update rate is internal, not shown in GUI
        updateRate = new SliderSetting("Update Rate", 0.5, 0.1, 3.0, 0.1);
        // this.registerSetting(updateRate);
        this.registerSetting(onlyFirstBed = new TickSetting("Only First Bed", false));
        this.registerSetting(throughWalls = new TickSetting("Through Walls", true));
        this.registerSetting(outline = new TickSetting("Outline", true));
        this.registerSetting(color = new ColorSetting("Color", 255, 50, 50));

        // Surround blocks
        this.registerSetting(new DescriptionSetting("--- Defense Blocks ---"));
        this.registerSetting(showSurround = new TickSetting("Show Defense", true));
        this.registerSetting(surroundRange = new SliderSetting("Scan Range", 3.0, 1.0, 6.0, 1.0));
        this.registerSetting(iconScale = new SliderSetting("Icon Scale", 1.0, 0.5, 2.0, 0.1));
    }

    @Override
    public void onEnable() {
        singleBed = null;
        beds.clear();
        bedDefenseBlocks.clear();
        lastCheck = 0;
    }

    @Override
    public void onDisable() {
        singleBed = null;
        beds.clear();
        bedDefenseBlocks.clear();
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Check rate limit
        if (System.currentTimeMillis() - lastCheck < updateRate.getValue() * 1000) {
            return;
        }
        lastCheck = System.currentTimeMillis();

        int r = (int) range.getValue();

        // Clear defense blocks for refresh
        bedDefenseBlocks.clear();

        // Search for beds
        for (int y = r; y >= -r; y--) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = new BlockPos(
                        mc.thePlayer.posX + x,
                        mc.thePlayer.posY + y,
                        mc.thePlayer.posZ + z
                    );

                    IBlockState state = mc.theWorld.getBlockState(pos);

                    if (state.getBlock() == Blocks.bed) {
                        // Check if this is the foot part of the bed
                        try {
                            @SuppressWarnings("unchecked")
                            BlockBed.EnumPartType part = (BlockBed.EnumPartType) state.getValue((IProperty) BlockBed.PART);
                            if (part != BlockBed.EnumPartType.FOOT) continue;

                            @SuppressWarnings("unchecked")
                            EnumFacing facing = (EnumFacing) state.getValue((IProperty) BlockBed.FACING);
                            BlockPos headPos = pos.offset(facing);

                            if (onlyFirstBed.isEnabled()) {
                                if (singleBed != null && isSamePos(pos, singleBed[0])) {
                                    // Still scan surround for existing bed
                                    if (showSurround.isEnabled()) {
                                        scanDefenseBlocks(pos, headPos);
                                    }
                                    return;
                                }
                                singleBed = new BlockPos[]{pos, headPos};
                                if (showSurround.isEnabled()) {
                                    scanDefenseBlocks(pos, headPos);
                                }
                                return;
                            } else {
                                // Check if already tracked
                                boolean found = false;
                                for (BlockPos[] bed : beds) {
                                    if (isSamePos(pos, bed[0])) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    beds.add(new BlockPos[]{pos, headPos});
                                }
                                // Scan surround for this bed
                                if (showSurround.isEnabled()) {
                                    scanDefenseBlocks(pos, headPos);
                                }
                            }
                        } catch (Exception e) {
                            // Skip invalid bed blocks
                        }
                    }
                }
            }
        }
    }

    /**
     * Scan defense blocks around a bed - only blocks directly touching each face
     * Skips blocks below the bed (DOWN face)
     */
    private void scanDefenseBlocks(BlockPos footPos, BlockPos headPos) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        Map<String, Integer> blockCounts = new HashMap<>();
        Set<BlockPos> checkedPositions = new HashSet<>();

        // Check all faces of FOOT block (except DOWN)
        for (EnumFacing face : EnumFacing.values()) {
            // Skip blocks below the bed
            if (face == EnumFacing.DOWN) continue;

            BlockPos adjacent = footPos.offset(face);

            // Skip if it's the head position (part of bed)
            if (adjacent.equals(headPos)) continue;

            checkAndCountBlock(mc, adjacent, blockCounts, checkedPositions);
        }

        // Check all faces of HEAD block (except DOWN)
        for (EnumFacing face : EnumFacing.values()) {
            // Skip blocks below the bed
            if (face == EnumFacing.DOWN) continue;

            BlockPos adjacent = headPos.offset(face);

            // Skip if it's the foot position (part of bed)
            if (adjacent.equals(footPos)) continue;

            // Skip if already checked
            if (checkedPositions.contains(adjacent)) continue;

            checkAndCountBlock(mc, adjacent, blockCounts, checkedPositions);
        }

        if (!blockCounts.isEmpty()) {
            bedDefenseBlocks.put(footPos, blockCounts);
        }
    }

    /**
     * Check a position and count if it's a defense block
     */
    private void checkAndCountBlock(Minecraft mc, BlockPos pos, Map<String, Integer> blockCounts, Set<BlockPos> checkedPositions) {
        checkedPositions.add(pos);

        IBlockState state = mc.theWorld.getBlockState(pos);
        Block block = state.getBlock();

        // Skip air and bed blocks
        if (block == Blocks.air || block == Blocks.bed) return;

        // Check if it's a defense block
        if (isDefenseBlock(block)) {
            String key = getBlockKey(block, state);
            blockCounts.merge(key, 1, Integer::sum);
        }
    }

    /**
     * Get a unique key for a block, handling wool colors specially
     */
    private String getBlockKey(Block block, IBlockState state) {
        if (block == Blocks.wool) {
            int meta = block.getMetaFromState(state);
            return "wool_" + meta;
        }
        // For other blocks, use the normalized block's registry name
        Block normalized = normalizeBlock(block);
        return Block.blockRegistry.getNameForObject(normalized).toString();
    }

    /**
     * Normalize similar blocks to one type for cleaner display
     */
    private Block normalizeBlock(Block block) {
        // Wood types -> planks
        if (block == Blocks.log || block == Blocks.log2) {
            return Blocks.planks;
        }
        // Glass types -> glass
        if (block == Blocks.stained_glass) {
            return Blocks.glass;
        }
        // Water types -> water
        if (block == Blocks.flowing_water) {
            return Blocks.water;
        }
        // Clay types -> hardened clay
        if (block == Blocks.stained_hardened_clay) {
            return Blocks.hardened_clay;
        }
        return block;
    }

    /**
     * Check if block is a defense block
     */
    private boolean isDefenseBlock(Block block) {
        return block == Blocks.obsidian ||
               block == Blocks.end_stone ||
               block == Blocks.wool ||
               block == Blocks.stained_hardened_clay ||
               block == Blocks.hardened_clay ||
               block == Blocks.glass ||
               block == Blocks.stained_glass ||
               block == Blocks.planks ||
               block == Blocks.log ||
               block == Blocks.log2 ||
               block == Blocks.water ||
               block == Blocks.flowing_water ||
               block == Blocks.ladder ||
               block == Blocks.chest ||
               block == Blocks.trapped_chest ||
               block == Blocks.ender_chest ||
               block == Blocks.iron_block ||
               block == Blocks.diamond_block ||
               block == Blocks.emerald_block;
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

        // Don't render when GUI is open
        if (mc.currentScreen != null) {
            return;
        }

        // Get camera position from RenderManager (the actual render camera position)
        RenderManager rm = mc.getRenderManager();
        if (rm == null) return;

        // Use renderPosX/Y/Z - these are the official camera coordinates for rendering
        double camX = rm.viewerPosX;
        double camY = rm.viewerPosY;
        double camZ = rm.viewerPosZ;

        // Render beds
        if (onlyFirstBed.isEnabled() && singleBed != null) {
            if (!(mc.theWorld.getBlockState(singleBed[0]).getBlock() instanceof BlockBed)) {
                singleBed = null;
                return;
            }
            renderBed(singleBed, camX, camY, camZ, rm);
        } else {
            Iterator<BlockPos[]> iter = beds.iterator();
            while (iter.hasNext()) {
                BlockPos[] bed = iter.next();
                if (!(mc.theWorld.getBlockState(bed[0]).getBlock() instanceof BlockBed)) {
                    iter.remove();
                    continue;
                }
                renderBed(bed, camX, camY, camZ, rm);
            }
        }
    }

    /**
     * Render a single bed with its box and defense icons
     */
    private void renderBed(BlockPos[] bed, double camX, double camY, double camZ, RenderManager rm) {
        Minecraft mc = Minecraft.getMinecraft();

        double x = bed[0].getX() - camX;
        double y = bed[0].getY() - camY;
        double z = bed[0].getZ() - camZ;

        // Calculate bed center for icon positioning
        double centerX = (bed[0].getX() + bed[1].getX()) / 2.0 + 0.5 - camX;
        double centerY = bed[0].getY() + 1.5 - camY; // Above the bed
        double centerZ = (bed[0].getZ() + bed[1].getZ()) / 2.0 + 0.5 - camZ;

        // === RENDER BED BOX ===
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(2.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);

        if (throughWalls.isEnabled()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        GL11.glDepthMask(false);

        // Get color
        int c = color.getColor();
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;

        GL11.glColor4f(r, g, b, 0.8f);

        // Calculate bounding box
        AxisAlignedBB box;
        if (bed[0].getX() != bed[1].getX()) {
            if (bed[0].getX() > bed[1].getX()) {
                box = new AxisAlignedBB(x - 1.0, y, z, x + 1.0, y + 0.5625, z + 1.0);
            } else {
                box = new AxisAlignedBB(x, y, z, x + 2.0, y + 0.5625, z + 1.0);
            }
        } else {
            if (bed[0].getZ() > bed[1].getZ()) {
                box = new AxisAlignedBB(x, y, z - 1.0, x + 1.0, y + 0.5625, z + 1.0);
            } else {
                box = new AxisAlignedBB(x, y, z, x + 1.0, y + 0.5625, z + 2.0);
            }
        }

        // Draw outline
        if (outline.isEnabled()) {
            RenderGlobal.drawSelectionBoundingBox(box);
        }

        // Draw filled
        GL11.glColor4f(r, g, b, 0.25f);
        drawFilledBox(box);

        GL11.glPopAttrib();
        GL11.glPopMatrix();

        // === RENDER DEFENSE ICONS ===
        if (showSurround.isEnabled()) {
            Map<String, Integer> defenseBlocks = bedDefenseBlocks.get(bed[0]);
            if (defenseBlocks != null && !defenseBlocks.isEmpty()) {
                renderDefenseIcons(defenseBlocks, centerX, centerY, centerZ, rm, mc);
            }
        }
    }

    /**
     * Render defense block icons above the bed
     */
    private void renderDefenseIcons(Map<String, Integer> blocks, double x, double y, double z,
                                     RenderManager rm, Minecraft mc) {
        if (blocks.isEmpty()) return;

        // Consolidate wool colors - show only most common color with total count
        Map<String, Integer> displayBlocks = consolidateWool(blocks);

        float scale = 0.03f * (float) iconScale.getValue();
        int iconSize = 16;
        int spacing = 18;
        int totalWidth = displayBlocks.size() * spacing - 2;

        GlStateManager.pushMatrix();

        // Position at bed center, above it
        GlStateManager.translate(x, y, z);

        // Face the camera (billboard)
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);

        // Scale
        GlStateManager.scale(-scale, -scale, scale);

        // Disable lighting for flat icons
        GlStateManager.disableLighting();

        if (throughWalls.isEnabled()) {
            GlStateManager.disableDepth();
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int offsetX = -totalWidth / 2;
        FontRenderer fr = mc.fontRendererObj;

        // Sort by count (most first)
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(displayBlocks.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sorted) {
            String key = entry.getKey();
            int count = entry.getValue();

            // Render block icon
            renderBlockIconByKey(key, offsetX, 0, mc);

            // Render count below icon
            if (count > 1) {
                String countStr = String.valueOf(count);
                int textWidth = fr.getStringWidth(countStr);
                fr.drawStringWithShadow(countStr, offsetX + (iconSize - textWidth) / 2, iconSize + 2, 0xFFFFFF);
            }

            offsetX += spacing;
        }

        GlStateManager.enableLighting();
        if (throughWalls.isEnabled()) {
            GlStateManager.enableDepth();
        }
        GlStateManager.disableBlend();

        GlStateManager.popMatrix();
    }

    /**
     * Consolidate all wool colors into one entry with the most common color
     */
    private Map<String, Integer> consolidateWool(Map<String, Integer> blocks) {
        Map<String, Integer> result = new HashMap<>();
        int totalWool = 0;
        String mostCommonWoolKey = null;
        int mostCommonWoolCount = 0;

        for (Map.Entry<String, Integer> entry : blocks.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue();

            if (key.startsWith("wool_")) {
                totalWool += count;
                if (count > mostCommonWoolCount) {
                    mostCommonWoolCount = count;
                    mostCommonWoolKey = key;
                }
            } else {
                result.put(key, count);
            }
        }

        // Add consolidated wool entry with total count but most common color's key
        if (totalWool > 0 && mostCommonWoolKey != null) {
            result.put(mostCommonWoolKey, totalWool);
        }

        return result;
    }

    /**
     * Render a block icon by its string key (handles wool colors with metadata)
     */
    private void renderBlockIconByKey(String key, int x, int y, Minecraft mc) {
        try {
            Block block;
            int metadata = 0;

            if (key.startsWith("wool_")) {
                // Wool with color metadata
                block = Blocks.wool;
                metadata = Integer.parseInt(key.substring(5));
            } else {
                // Regular block by registry name
                block = Block.getBlockFromName(key);
                if (block == null) return;
            }

            // Get item from block
            Item item = Item.getItemFromBlock(block);
            if (item == null) return;

            // Get the sprite for this block with metadata
            TextureAtlasSprite sprite = mc.getRenderItem().getItemModelMesher()
                .getItemModel(new ItemStack(item, 1, metadata))
                .getParticleTexture();
            if (sprite == null) {
                // Fallback to particle icon
                sprite = mc.getRenderItem().getItemModelMesher().getParticleIcon(item, metadata);
            }
            if (sprite == null) return;

            // Bind texture atlas
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            // Get UV coordinates
            float minU = sprite.getMinU();
            float maxU = sprite.getMaxU();
            float minV = sprite.getMinV();
            float maxV = sprite.getMaxV();

            // Draw textured quad
            int size = 16;
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer wr = tessellator.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(x, y + size, 0).tex(minU, maxV).endVertex();
            wr.pos(x + size, y + size, 0).tex(maxU, maxV).endVertex();
            wr.pos(x + size, y, 0).tex(maxU, minV).endVertex();
            wr.pos(x, y, 0).tex(minU, minV).endVertex();
            tessellator.draw();

        } catch (Exception e) {
            // Silent fail - some blocks might not have icons
        }
    }

    /**
     * Render a single block icon as a 2D sprite (legacy method)
     */
    private void renderBlockIcon(Block block, int x, int y, Minecraft mc) {
        try {
            // Get item from block
            Item item = Item.getItemFromBlock(block);
            if (item == null) return;

            ItemStack stack = new ItemStack(item);

            // Get sprite texture
            TextureAtlasSprite sprite = mc.getRenderItem().getItemModelMesher().getParticleIcon(item);
            if (sprite == null) return;

            // Bind texture atlas
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            // Get UV coordinates
            float minU = sprite.getMinU();
            float maxU = sprite.getMaxU();
            float minV = sprite.getMinV();
            float maxV = sprite.getMaxV();

            // Draw textured quad
            int size = 16;
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer wr = tessellator.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(x, y + size, 0).tex(minU, maxV).endVertex();
            wr.pos(x + size, y + size, 0).tex(maxU, maxV).endVertex();
            wr.pos(x + size, y, 0).tex(maxU, minV).endVertex();
            wr.pos(x, y, 0).tex(minU, minV).endVertex();
            tessellator.draw();

        } catch (Exception e) {
            // Silent fail - some blocks might not have icons
        }
    }

    /**
     * Draw a filled box
     */
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

    private boolean isSamePos(BlockPos a, BlockPos b) {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
    }

    public int getBedCount() {
        if (onlyFirstBed.isEnabled()) {
            return singleBed != null ? 1 : 0;
        }
        return beds.size();
    }

    @Override
    public String getDisplaySuffix() {
        int count = getBedCount();
        return count > 0 ? " §7" + count : null;
    }
}
