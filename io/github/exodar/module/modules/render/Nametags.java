/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.AntiObfuscate;
import io.github.exodar.module.modules.misc.MurderMystery;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nametags - 3D World-space nametags
 * Based on client-master implementation
 */
public class Nametags extends Module {

    private SliderSetting scale;
    private TickSetting autoScale;
    private TickSetting showRect;
    private SliderSetting bgOpacity;
    private TickSetting bgBorder;
    private TickSetting showGlow;
    private ColorSetting glowColor;
    private TickSetting glowUseTeamColor;
    private SliderSetting glowSize;
    private TickSetting showHealth;
    private TickSetting absorptionSeparately;
    private TickSetting showDistance;
    private TickSetting showInvis;
    private TickSetting showArmor;
    private TickSetting showEnchants;
    private TickSetting hideVanilla;

    // Health memory system - remembers player health when they go out of range
    private static final Map<UUID, HealthMemory> healthMemory = new HashMap<>();
    private static final long HEALTH_MEMORY_TIMEOUT = 60000; // 60 seconds before clearing memory

    // Track if server uses HP format (0-20) instead of hearts format (0-10)
    // If we ever see a belowname value > 10, this server uses HP format
    private static boolean serverUsesHPFormat = false;

    /**
     * Check if server uses HP format (0-20) for belowname
     * Call this from CustomRenderPlayer to maintain consistent format detection
     */
    public static boolean isServerUsingHPFormat() {
        return serverUsesHPFormat;
    }

    /**
     * Mark that server uses HP format (called when we see a value > 10)
     */
    public static void setServerUsesHPFormat() {
        serverUsesHPFormat = true;
    }

    /**
     * Health memory data class
     */
    private static class HealthMemory {
        int rawScoreboardHealth; // Raw value from scoreboard (not converted)
        long lastSeenTime;

        HealthMemory(int rawHealth) {
            this.rawScoreboardHealth = rawHealth;
            this.lastSeenTime = System.currentTimeMillis();
        }
    }

    public Nametags() {
        super("Nametags", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("3D World-space nametags"));
        this.registerSetting(scale = new SliderSetting("Scale", 1.0, 0.5, 5.0, 0.1));
        this.registerSetting(autoScale = new TickSetting("Auto Scale", true));
        this.registerSetting(showRect = new TickSetting("Background", true));
        this.registerSetting(bgOpacity = new SliderSetting("BG Opacity", 0.25, 0.0, 1.0, 0.05));
        this.registerSetting(bgBorder = new TickSetting("BG Border", true));
        this.registerSetting(showGlow = new TickSetting("Glow", false));
        this.registerSetting(glowColor = new ColorSetting("Glow Color", 255, 100, 100, 200));
        this.registerSetting(glowUseTeamColor = new TickSetting("Glow Use Team Color", true));
        this.registerSetting(glowSize = new SliderSetting("Glow Size", 6.0, 2.0, 15.0, 1.0));
        this.registerSetting(showHealth = new TickSetting("Show Health", true));
        this.registerSetting(absorptionSeparately = new TickSetting("Absorption Separately", true));
        this.registerSetting(showDistance = new TickSetting("Show Distance", false));
        this.registerSetting(showInvis = new TickSetting("Show Invis", false));
        this.registerSetting(showArmor = new TickSetting("Show Armor", true));
        this.registerSetting(showEnchants = new TickSetting("Show Enchants", true));
        this.registerSetting(hideVanilla = new TickSetting("Hide Vanilla", true));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        setAllPlayersNametagVisibility(false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        setAllPlayersNametagVisibility(true);
    }

    /**
     * Set native nametag visibility for all players in the world (except local player)
     */
    private void setAllPlayersNametagVisibility(boolean visible) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.theWorld != null) {
                for (EntityPlayer player : mc.theWorld.playerEntities) {
                    // Don't hide local player's nametag (for F5 view)
                    if (player == mc.thePlayer) continue;
                    player.setAlwaysRenderNameTag(visible);
                }
            }
        } catch (Throwable ignored) {}
    }

    public boolean shouldHideVanillaNametags() {
        return enabled && hideVanilla.isEnabled();
    }

    /**
     * Get scale value for external renderers (like CustomRenderPlayer)
     * Scale 1-5 becomes 0.02F-0.10F for good visibility (like client-master)
     */
    public float getScale() {
        return (float) scale.getValue() * 0.02F;
    }

    public float getYOffset() {
        return 0.2F; // Fixed value (lowered by 0.3)
    }

    public boolean isShowBackground() {
        return showRect.isEnabled();
    }

    public float getBgOpacity() {
        return (float) bgOpacity.getValue();
    }

    public boolean isShowHealth() {
        return showHealth.isEnabled();
    }

    public boolean isShowDistance() {
        return showDistance.isEnabled();
    }

    public boolean isShowInvis() {
        return showInvis.isEnabled();
    }

    public boolean isShowArmor() {
        return showArmor.isEnabled();
    }

    public boolean isShowEnchants() {
        return showEnchants.isEnabled();
    }

    public boolean isAutoScale() {
        return autoScale.isEnabled();
    }

    public boolean isAbsorptionSeparately() {
        return absorptionSeparately.isEnabled();
    }

    public void renderNametags(float partialTicks) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        RenderManager rm = mc.getRenderManager();
        if (rm == null) return;

        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        EntityPlayer thePlayer = mc.thePlayer;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == thePlayer) continue;

            String playerName = player.getName();

            // Check if this player is a known murderer or detective - ALWAYS render them
            boolean isMurdererOrDetective = MurderMystery.isMurderer(playerName) || MurderMystery.isDetective(playerName);

            // Skip corpses (sleeping players in MM) - even for murderers
            if (player.isPlayerSleeping() && MurderMystery.isInMurderMysteryGame()) continue;

            // For murderers/detectives, skip most filters to ensure they always render
            if (!isMurdererOrDetective) {
                if (player.isDead || player.deathTime > 0) continue;
                if (!showInvis.isEnabled() && player.isInvisible()) continue;
                if (isTeamMate(player)) continue;
                if (AntiBot.isBotForVisuals(player)) continue;
            }

            // Calculate interpolated position (client-master style)
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - rm.viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - rm.viewerPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - rm.viewerPosZ;

            renderCustomName(player, x, y, z, partialTicks, rm, fr, mc);
        }
    }

    private void renderCustomName(EntityPlayer entity, double x, double y, double z, float partial,
                                   RenderManager rm, FontRenderer fr, Minecraft mc) {

        // Get team and apply team color formatting (like client-master)
        ScorePlayerTeam team = (ScorePlayerTeam) entity.getTeam();
        String name = entity.getName();

        // Get team color for border from Teams module
        int teamColor = -1;
        Teams teamsModule = Teams.getInstance();
        if (teamsModule != null && teamsModule.isEnabled()) {
            teamColor = teamsModule.getTeamColor(entity);
        }

        // Apply team color prefix if player is in a team
        if (team != null) {
            name = ScorePlayerTeam.formatPlayerName(team, name);
        }

        // Apply AntiObfuscate (remove §k scrambled text)
        name = AntiObfuscate.stripObfuscated(name);

        // Add MurderMystery prefix if applicable
        String mmPrefix = MurderMystery.getNametagPrefix(entity.getName());
        if (mmPrefix != null) {
            name = mmPrefix + name;
        }

        // Add health
        if (showHealth.isEnabled()) {
            float health = -1;
            float maxHealth = 20.0f;
            UUID playerUUID = entity.getUniqueID();
            boolean fromEntity = false;
            boolean fromMemory = false;

            // PRIMARY: Try scoreboard belowname health first (most accurate on servers)
            int scoreboardHealth = getScoreboardHealth(entity);
            int originalScoreboardHealth = scoreboardHealth; // For debug

            // If no current belowname, try to use remembered value
            if (scoreboardHealth <= 0) {
                HealthMemory memory = healthMemory.get(playerUUID);
                if (memory != null) {
                    long timeSinceLastSeen = System.currentTimeMillis() - memory.lastSeenTime;
                    if (timeSinceLastSeen < HEALTH_MEMORY_TIMEOUT) {
                        // Use remembered health
                        scoreboardHealth = memory.rawScoreboardHealth;
                        fromMemory = true;
                    } else {
                        // Memory expired, clear it
                        healthMemory.remove(playerUUID);
                    }
                }
            } else {
                // Update memory with current value
                healthMemory.put(playerUUID, new HealthMemory(scoreboardHealth));
            }

            // Convert scoreboard health to display value
            if (scoreboardHealth > 0) {
                // Detect if server uses HP format (0-20) - if we ever see > 10, it's HP format
                if (scoreboardHealth > 10) {
                    serverUsesHPFormat = true;
                }

                // Convert based on detected server format
                if (serverUsesHPFormat) {
                    // Server uses HP format (0-20), divide by 2 for hearts
                    health = scoreboardHealth / 2.0f;
                    maxHealth = 10.0f;
                } else {
                    // Server uses hearts format (0-10), use directly
                    health = scoreboardHealth;
                    maxHealth = 10.0f;
                }
            }

            // FALLBACK: Use direct entity health if nothing else worked
            if (health < 0) {
                health = entity.getHealth();
                maxHealth = entity.getMaxHealth();
                if (maxHealth <= 0) maxHealth = 20.0f;
                if (health < 0) health = 0;
                fromEntity = true;
            }

            // Calculate display value (round up to fix 0.5 offset from server)
            float hearts;
            double ratio;
            if (fromEntity) {
                // Entity health: convert HP to hearts and round up
                hearts = (float) Math.ceil(health / 2.0f);
                ratio = (maxHealth <= 0.0f) ? 0.0 : (health / maxHealth);
            } else {
                // Belowname/memory: already converted to hearts format, round up
                hearts = (float) Math.ceil(health);
                ratio = (maxHealth <= 0.0f) ? 0.0 : (health / maxHealth);
            }

            String color = ratio < 0.3 ? "\u00a7c" : (ratio < 0.5 ? "\u00a76" : (ratio < 0.7 ? "\u00a7e" : "\u00a7a"));
            // Show as integer if whole number, otherwise show 1 decimal
            String heartStr = (hearts == (int) hearts) ? String.valueOf((int) hearts) : String.format("%.1f", hearts);
            name = name + " " + color + heartStr + " \u2764";

            // Add absorption separately in gold if enabled
            if (absorptionSeparately.isEnabled()) {
                float absorption = entity.getAbsorptionAmount();
                if (absorption > 0) {
                    // Convert absorption to hearts (divide by 2)
                    float absorptionHearts = absorption / 2.0f;
                    String absStr = (absorptionHearts == (int) absorptionHearts) ?
                        String.valueOf((int) absorptionHearts) : String.format("%.1f", absorptionHearts);
                    // Gold color: §6
                    name = name + " \u00a76+" + absStr + " \u2764";
                }
            }
            name = name + "\u00a7r";
        }

        // Add distance
        if (showDistance.isEnabled()) {
            int dist = (int) mc.thePlayer.getDistanceToEntity(entity);
            String distColor = dist <= 8 ? "\u00a7c" : (dist <= 15 ? "\u00a76" : (dist <= 25 ? "\u00a7e" : "\u00a77"));
            name = distColor + dist + "m\u00a7r " + name;
        }

        // Calculate distance to player for auto scale
        double distanceToPlayer = Math.sqrt(x * x + y * y + z * z);
        if (distanceToPlayer < 1.0) distanceToPlayer = 1.0;

        // Base scale from setting
        float scaleVal = getScale();

        // Auto scale: multiply scale by distance so nametag appears SAME SIZE on screen regardless of distance
        if (autoScale.isEnabled()) {
            // Multiply by distance to compensate for perspective division
            scaleVal = scaleVal * (float) distanceToPlayer;
        }

        // Adjust height if sneaking (like client-master)
        float yOff = entity.height + getYOffset();
        if (entity.isSneaking()) {
            yOff -= 0.25F;
        }

        GlStateManager.pushMatrix();

        // Translate to entity position (like client-master)
        GlStateManager.translate((float)x, (float)y + yOff, (float)z);

        // Face the camera (like client-master)
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);

        // Scale (like client-master) - negative Y to flip text right-side up
        GlStateManager.scale(-scaleVal, -scaleVal, scaleVal);

        // DO NOT translate after scale - we want text to grow UPWARD from base
        // Text is drawn at y=0, grows downward (positive Y in screen space after flip)
        // So we DON'T offset - the text bottom stays anchored at entity head position

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int stringWidth = fr.getStringWidth(name) / 2;

        // Collect armor for width calculation
        List<ItemStack> armorItems = new ArrayList<>();
        ItemStack heldItem = null;
        if (showArmor.isEnabled()) {
            heldItem = entity.getEquipmentInSlot(0);
            for (int i = 4; i >= 1; i--) {
                ItemStack stack = entity.getEquipmentInSlot(i);
                if (stack != null) armorItems.add(stack);
            }
        }

        int totalItems = armorItems.size() + (heldItem != null ? 1 : 0);
        int itemSpacing = 14;
        int armorTotalWidth = totalItems * itemSpacing;
        int maxWidth = Math.max(stringWidth, armorTotalWidth / 2);

        // Text Y position - draw at y=0 so it grows upward (due to negative scale)
        int textY = 0;

        // Draw glow effect around nametag (before background so it's behind)
        if (showGlow.isEnabled()) {
            // Get glow color - use team color if enabled, otherwise custom color
            float gr, gg, gb;
            if (glowUseTeamColor.isEnabled() && teamColor != -1) {
                gr = ((teamColor >> 16) & 255) / 255.0F;
                gg = ((teamColor >> 8) & 255) / 255.0F;
                gb = (teamColor & 255) / 255.0F;
            } else {
                int gc = glowColor.getColor();
                gr = ((gc >> 16) & 255) / 255.0F;
                gg = ((gc >> 8) & 255) / 255.0F;
                gb = (gc & 255) / 255.0F;
            }

            int gs = (int) glowSize.getValue();
            float glowZ = 0.001F; // Behind the background

            // Multi-layer glow using Tessellator (more compatible with MC's rendering)
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableAlpha();

            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();

            for (int layer = 3; layer >= 0; layer--) {
                int expand = gs + layer * 2;
                float alpha = 0.08f + (3 - layer) * 0.07f;

                float x1 = -stringWidth - 3 - expand;
                float y1 = textY - 3 - expand;
                float x2 = stringWidth + 3 + expand;
                float y2 = textY + 10 + expand;

                wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(x1, y2, glowZ).color(gr, gg, gb, alpha).endVertex();
                wr.pos(x2, y2, glowZ).color(gr, gg, gb, alpha).endVertex();
                wr.pos(x2, y1, glowZ).color(gr, gg, gb, alpha).endVertex();
                wr.pos(x1, y1, glowZ).color(gr, gg, gb, alpha).endVertex();
                tess.draw();
            }

            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // Draw background for name (1px larger on each side)
        if (showRect.isEnabled() && bgOpacity.getValue() > 0.01) {
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            float alpha = (float) bgOpacity.getValue();

            // Background rect - 1px larger on each side (was -2/+2, now -3/+3)
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(-stringWidth - 3, textY - 3, 0).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            worldRenderer.pos(-stringWidth - 3, textY + 10, 0).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            worldRenderer.pos(stringWidth + 3, textY + 10, 0).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            worldRenderer.pos(stringWidth + 3, textY - 3, 0).color(0.0F, 0.0F, 0.0F, alpha).endVertex();
            tessellator.draw();

            // Draw border around the background
            if (bgBorder.isEnabled()) {
                float r, g, b;
                if (teamColor != -1) {
                    r = ((teamColor >> 16) & 255) / 255.0F;
                    g = ((teamColor >> 8) & 255) / 255.0F;
                    b = (teamColor & 255) / 255.0F;
                } else {
                    // Default border color (light gray)
                    r = 0.6F;
                    g = 0.6F;
                    b = 0.6F;
                }

                // Draw border slightly in front to avoid z-fighting
                float borderZ = -0.001F;

                // Top border (1px line above background)
                worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                worldRenderer.pos(-stringWidth - 4, textY - 4, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(-stringWidth - 4, textY - 3, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(stringWidth + 4, textY - 3, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(stringWidth + 4, textY - 4, borderZ).color(r, g, b, 1.0F).endVertex();
                tessellator.draw();

                // Bottom border (1px line below background)
                worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                worldRenderer.pos(-stringWidth - 4, textY + 10, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(-stringWidth - 4, textY + 11, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(stringWidth + 4, textY + 11, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(stringWidth + 4, textY + 10, borderZ).color(r, g, b, 1.0F).endVertex();
                tessellator.draw();

                // Left border (1px line to left of background)
                worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                worldRenderer.pos(-stringWidth - 4, textY - 4, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(-stringWidth - 4, textY + 11, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(-stringWidth - 3, textY + 11, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(-stringWidth - 3, textY - 4, borderZ).color(r, g, b, 1.0F).endVertex();
                tessellator.draw();

                // Right border (1px line to right of background)
                worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
                worldRenderer.pos(stringWidth + 3, textY - 4, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(stringWidth + 3, textY + 11, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(stringWidth + 4, textY + 11, borderZ).color(r, g, b, 1.0F).endVertex();
                worldRenderer.pos(stringWidth + 4, textY - 4, borderZ).color(r, g, b, 1.0F).endVertex();
                tessellator.draw();
            }

            GlStateManager.enableTexture2D();
        }

        // Draw the name - cyan for friends, white for others
        int nameColor = 0xFFFFFFFF;
        if (io.github.exodar.module.modules.misc.Friends.isFriend(entity.getName())) {
            nameColor = 0xFF55FFFF; // Cyan for friends
        }

        fr.drawString(name, -stringWidth, textY, nameColor, true);

        // Draw armor icons above name
        if (totalItems > 0) {
            int iconX = -armorTotalWidth / 2;
            int iconY = textY - 20; // Above the text

            if (heldItem != null) {
                renderItemStack(heldItem, iconX, iconY, fr, mc);
                iconX += itemSpacing;
            }

            for (ItemStack armorStack : armorItems) {
                renderItemStack(armorStack, iconX, iconY, fr, mc);
                iconX += itemSpacing;
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        // Restore default blend function for shaders compatibility
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();
    }

    /**
     * Render item as a simple 2D textured quad - NO matrix operations inside
     * Uses same coordinate system as text rendering for stable positioning
     */
    private void renderItemStack(ItemStack stack, int xPos, int yPos, FontRenderer fr, Minecraft mc) {
        if (stack == null) return;

        // Get item sprite texture - same texture atlas as blocks/items
        TextureAtlasSprite sprite = null;
        try {
            sprite = mc.getRenderItem().getItemModelMesher().getParticleIcon(stack.getItem());
        } catch (Throwable ignored) {}

        if (sprite != null) {
            // Bind the texture atlas (same as blocks)
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            // Apply color for leather armor or wool
            int color = getItemColor(stack);
            if (color != -1) {
                float r = (color >> 16 & 255) / 255.0F;
                float g = (color >> 8 & 255) / 255.0F;
                float b = (color & 255) / 255.0F;
                GlStateManager.color(r, g, b, 1.0F);
            } else {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            // Get UV coordinates from sprite
            float minU = sprite.getMinU();
            float maxU = sprite.getMaxU();
            float minV = sprite.getMinV();
            float maxV = sprite.getMaxV();

            // Draw 16x16 textured quad - NO pushMatrix, NO rotate, just like drawString
            int size = 16;
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer wr = tessellator.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(xPos, yPos + size, 0).tex(minU, maxV).endVertex();
            wr.pos(xPos + size, yPos + size, 0).tex(maxU, maxV).endVertex();
            wr.pos(xPos + size, yPos, 0).tex(maxU, minV).endVertex();
            wr.pos(xPos, yPos, 0).tex(minU, minV).endVertex();
            tessellator.draw();

            // Reset color after leather
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // Enchant text on top of the item (much lower)
        if (showEnchants.isEnabled()) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5, 0.5, 0.5);
            renderEnchantText(stack, xPos + 2, yPos + 16, fr);
            GlStateManager.popMatrix();
        }

        // Show stack count for stackable items (golden apples, enderpearls, etc.)
        if (stack.stackSize > 1) {
            String countStr = String.valueOf(stack.stackSize);
            // Draw at bottom right of item
            fr.drawStringWithShadow(countStr, xPos + 17 - fr.getStringWidth(countStr), yPos + 9, 0xFFFFFF);
        }

        // Show durability bar for damageable items
        if (stack.isItemStackDamageable() && stack.getItemDamage() > 0) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getItemDamage();
            float durabilityRatio = 1.0F - (float) currentDamage / (float) maxDamage;
            int barWidth = (int) (durabilityRatio * 13);
            int barColor = getDurabilityColor(durabilityRatio);

            // Draw durability bar at bottom of item
            GlStateManager.disableTexture2D();
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();

            // Black background (1px height)
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(xPos + 2, yPos + 15, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            wr.pos(xPos + 2, yPos + 16, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            wr.pos(xPos + 15, yPos + 16, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            wr.pos(xPos + 15, yPos + 15, 0).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            tess.draw();

            // Colored bar (1px height)
            float r = ((barColor >> 16) & 255) / 255.0F;
            float g = ((barColor >> 8) & 255) / 255.0F;
            float b = (barColor & 255) / 255.0F;
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(xPos + 2, yPos + 15, 0).color(r, g, b, 1.0F).endVertex();
            wr.pos(xPos + 2, yPos + 16, 0).color(r, g, b, 1.0F).endVertex();
            wr.pos(xPos + 2 + barWidth, yPos + 16, 0).color(r, g, b, 1.0F).endVertex();
            wr.pos(xPos + 2 + barWidth, yPos + 15, 0).color(r, g, b, 1.0F).endVertex();
            tess.draw();

            GlStateManager.enableTexture2D();
        }
    }

    private int getDurabilityColor(float ratio) {
        if (ratio > 0.6F) return 0x00FF00;  // Green
        if (ratio > 0.3F) return 0xFFFF00;  // Yellow
        return 0xFF0000;  // Red
    }

    /**
     * Get color for items that need tinting (leather armor, wool, etc.)
     * Returns -1 if no color should be applied
     */
    private int getItemColor(ItemStack stack) {
        if (stack == null) return -1;

        // Leather armor color
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER) {
                return armor.getColor(stack);
            }
        }

        // Wool color based on metadata
        if (stack.getItem() instanceof ItemBlock) {
            ItemBlock itemBlock = (ItemBlock) stack.getItem();
            if (itemBlock.getBlock() == Blocks.wool) {
                return getWoolColor(stack.getMetadata());
            }
        }

        return -1;
    }

    /**
     * Get RGB color for wool based on metadata
     */
    private int getWoolColor(int meta) {
        switch (meta) {
            case 0: return 0xFFFFFF;  // White
            case 1: return 0xD87F33;  // Orange
            case 2: return 0xB24CD8;  // Magenta
            case 3: return 0x6699D8;  // Light Blue
            case 4: return 0xE5E533;  // Yellow
            case 5: return 0x7FCC19;  // Lime
            case 6: return 0xF27FA5;  // Pink
            case 7: return 0x4C4C4C;  // Gray
            case 8: return 0x999999;  // Light Gray
            case 9: return 0x4C7F99;  // Cyan
            case 10: return 0x7F3FB2; // Purple
            case 11: return 0x334CB2; // Blue
            case 12: return 0x664C33; // Brown
            case 13: return 0x667F33; // Green
            case 14: return 0x993333; // Red
            case 15: return 0x191919; // Black
            default: return -1;
        }
    }

    private int getEnchantColor(int level) {
        switch (level) {
            case 1: return 0xFFFFFF;  // White
            case 2: return 0x55FFFF;  // &b Aqua
            case 3: return 0x00AAAA;  // &3 Dark Aqua
            case 4: return 0xAA00AA;  // &5 Dark Purple
            case 5: return 0xFFAA00;  // Orange
            case 10: return 0xFF55FF; // &d Light Purple
            default: return level > 5 ? 0xFF55FF : 0xFFFFFF;
        }
    }

    private int drawEnchantWithColor(FontRenderer fr, String letter, int level, int x, int y) {
        // Draw letter in white
        int letterWidth = fr.drawStringWithShadow(letter, x, y, 0xFFFFFF);
        // Draw number in color
        fr.drawStringWithShadow(String.valueOf(level), letterWidth, y, getEnchantColor(level));
        return letterWidth;
    }

    private void renderEnchantText(ItemStack stack, int xPos, int yPos, FontRenderer fr) {
        int newYPos = yPos - 24;
        int x = xPos * 2;

        if (stack.getItem() instanceof ItemArmor) {
            int prot = EnchantmentHelper.getEnchantmentLevel(0, stack);
            int thorns = EnchantmentHelper.getEnchantmentLevel(7, stack);
            int unbreak = EnchantmentHelper.getEnchantmentLevel(34, stack);

            if (prot > 0) { drawEnchantWithColor(fr, "P", prot, x, newYPos); newYPos += 8; }
            if (thorns > 0) { drawEnchantWithColor(fr, "T", thorns, x, newYPos); newYPos += 8; }
            if (unbreak > 0) { drawEnchantWithColor(fr, "U", unbreak, x, newYPos); }
        } else if (stack.getItem() instanceof ItemSword) {
            int sharp = EnchantmentHelper.getEnchantmentLevel(16, stack);
            int fire = EnchantmentHelper.getEnchantmentLevel(20, stack);
            int kb = EnchantmentHelper.getEnchantmentLevel(19, stack);

            if (sharp > 0) { drawEnchantWithColor(fr, "S", sharp, x, newYPos); newYPos += 8; }
            if (fire > 0) { drawEnchantWithColor(fr, "F", fire, x, newYPos); newYPos += 8; }
            if (kb > 0) { drawEnchantWithColor(fr, "K", kb, x, newYPos); }
        } else if (stack.getItem() instanceof ItemBow) {
            int power = EnchantmentHelper.getEnchantmentLevel(48, stack);
            int punch = EnchantmentHelper.getEnchantmentLevel(49, stack);
            int flame = EnchantmentHelper.getEnchantmentLevel(50, stack);

            if (power > 0) { drawEnchantWithColor(fr, "Pw", power, x, newYPos); newYPos += 8; }
            if (punch > 0) { drawEnchantWithColor(fr, "Pu", punch, x, newYPos); newYPos += 8; }
            if (flame > 0) { drawEnchantWithColor(fr, "Fl", flame, x, newYPos); }
        } else if (stack.getItem() instanceof ItemTool) {
            int eff = EnchantmentHelper.getEnchantmentLevel(32, stack);
            int unbreak = EnchantmentHelper.getEnchantmentLevel(34, stack);
            int fortune = EnchantmentHelper.getEnchantmentLevel(35, stack);

            if (eff > 0) { drawEnchantWithColor(fr, "E", eff, x, newYPos); newYPos += 8; }
            if (fortune > 0) { drawEnchantWithColor(fr, "Fo", fortune, x, newYPos); newYPos += 8; }
            if (unbreak > 0) { drawEnchantWithColor(fr, "U", unbreak, x, newYPos); }
        } else {
            // For any other item (like sticks), show KB if present
            int kb = EnchantmentHelper.getEnchantmentLevel(19, stack);
            if (kb > 0) { drawEnchantWithColor(fr, "K", kb, x, newYPos); }
        }
    }

    private boolean isTeamMate(EntityPlayer player) {
        try {
            ModuleManager manager = io.github.exodar.Main.getModuleManager();
            Module teamsModule = manager != null ? manager.getModuleByName("Teams") : null;
            if (teamsModule != null && teamsModule.isEnabled() && teamsModule instanceof Teams) {
                return ((Teams) teamsModule).isTeamMate(player);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /**
     * Get health from scoreboard belowname display (used by some servers like Hypixel)
     * Servers show health as "10 ♥" below the player name using scoreboard slot 2
     * @return health value from scoreboard, or -1 if not found
     */
    private int getHealthFromScoreboard(EntityPlayer player, Minecraft mc) {
        return getScoreboardHealth(player);
    }

    /**
     * Public method for other modules (like ESP) to get health from scoreboard
     * Uses multiple lookup methods to handle different server implementations
     */
    public static int getScoreboardHealth(EntityPlayer player) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return -1;

            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) return -1;

            // Get the objective in display slot 2 (belowname)
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);
            if (objective == null) return -1;

            String playerName = player.getName();

            // Method 1: Direct lookup by player name
            if (scoreboard.entityHasObjective(playerName, objective)) {
                Score score = scoreboard.getValueFromObjective(playerName, objective);
                if (score != null) {
                    return score.getScorePoints();
                }
            }

            // Get all scores for more flexible matching
            java.util.Collection<Score> allScores = scoreboard.getSortedScores(objective);

            // Method 2: Search through all scores for exact match
            for (Score score : allScores) {
                if (score.getPlayerName().equals(playerName)) {
                    return score.getScorePoints();
                }
            }

            // Method 3: Search for score name containing the player name (for team-formatted names)
            for (Score score : allScores) {
                String scoreName = score.getPlayerName();
                // Strip color codes and check
                String strippedScoreName = stripColorCodes(scoreName);
                if (strippedScoreName.equals(playerName) || strippedScoreName.contains(playerName)) {
                    return score.getScorePoints();
                }
            }

            // Method 4: Get team formatted name and search
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            if (team != null) {
                String teamFormattedName = ScorePlayerTeam.formatPlayerName(team, playerName);
                for (Score score : allScores) {
                    if (score.getPlayerName().equals(teamFormattedName)) {
                        return score.getScorePoints();
                    }
                }
            }

            // Method 5: Check display name
            String displayName = player.getDisplayName().getFormattedText();
            for (Score score : allScores) {
                if (score.getPlayerName().equals(displayName)) {
                    return score.getScorePoints();
                }
            }

        } catch (Throwable ignored) {}
        return -1;
    }

    /**
     * Strip Minecraft color codes from a string
     */
    private static String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    /**
     * Clear health memory for a specific player (call when player dies or leaves)
     */
    public static void clearHealthMemory(UUID playerUUID) {
        healthMemory.remove(playerUUID);
    }

    /**
     * Clear all health memory (call on world change)
     */
    public static void clearAllHealthMemory() {
        healthMemory.clear();
        serverUsesHPFormat = false; // Reset format detection for new server
    }

    /**
     * Called when player attacks an entity - prints health debug info
     * Can be called from anywhere (combat modules, packet hooks, etc.)
     */
    public static void onAttack(EntityPlayer target) {
        if (target == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        // Get scoreboard health
        int scoreboardHP = getScoreboardHealth(target);

        // Get memory health
        HealthMemory memory = healthMemory.get(target.getUniqueID());
        int memoryHP = (memory != null) ? memory.rawScoreboardHealth : -1;

        // Get entity health
        float entityHP = target.getHealth();

        // Calculate what nametag will show using same logic as render
        float displayHearts;
        String source;
        int rawValue;

        if (scoreboardHP > 0) {
            rawValue = scoreboardHP;
            source = "SB";
        } else if (memoryHP > 0) {
            rawValue = memoryHP;
            source = "MEM";
        } else {
            displayHearts = entityHP / 2.0f;
            source = "ENT";
            rawValue = -1;
        }

        if (rawValue > 0) {
            // Check format detection
            if (rawValue > 10) {
                serverUsesHPFormat = true;
            }
            displayHearts = serverUsesHPFormat ? rawValue / 2.0f : rawValue;
        } else {
            displayHearts = entityHP / 2.0f;
        }

        // Print debug
        String formatStr = serverUsesHPFormat ? "HP" : "Hearts";
        String debug = "\u00a7b[Nametags Debug] \u00a7f" + target.getName() +
                       " \u00a77| \u00a7eSB: \u00a7f" + scoreboardHP +
                       " \u00a77| \u00a7aMEM: \u00a7f" + memoryHP +
                       " \u00a77| \u00a7cENT: \u00a7f" + String.format("%.1f", entityHP) +
                       " \u00a77| \u00a76Display: \u00a7f" + String.format("%.1f", displayHearts) +
                       " \u00a77(" + source + ", " + formatStr + ")";
        mc.thePlayer.addChatMessage(new ChatComponentText(debug));
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
