/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.render;

import io.github.exodar.Main;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.render.Hitbox;
import io.github.exodar.module.modules.render.Nametags;
import io.github.exodar.module.modules.render.Skeleton;
import io.github.exodar.module.modules.render.Glow;
import io.github.exodar.module.modules.render.ESP;
import io.github.exodar.module.modules.render.Chams;
import io.github.exodar.module.modules.player.Blink;
import io.github.exodar.module.modules.combat.Backtrack;
import io.github.exodar.module.modules.combat.LegitAura;
import io.github.exodar.module.modules.combat.LegitAura3;
import io.github.exodar.module.modules.combat.LegitAura6;
import io.github.exodar.module.modules.misc.AntiAim;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.spoof.SpoofManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomRenderPlayer - Based on client-master implementation
 * Renders nametags and hitboxes using coordinates provided by Minecraft's entity rendering
 */
public class CustomRenderPlayer extends RenderPlayer {

    // Developer accounts now handled by SpoofManager
    // No longer need hardcoded replacement - uses SpoofManager.isDeveloperAccount()

    // World renders (BedESP, ItemESP, Trajectories, Tracers) now handled by
    // Render3DManager via EntityRenderHook's Render3DEvent for proper GL matrices

    // Debug throttling for health source
    private static final Map<String, Long> lastDebugTime = new HashMap<>();
    private static final long DEBUG_INTERVAL = 3000; // 3 seconds

    // Cached module references for performance (avoid getModuleByName every frame)
    private static Nametags cachedNametags;
    private static Hitbox cachedHitbox;
    private static Skeleton cachedSkeleton;
    private static Glow cachedGlow;
    private static ESP cachedESP;
    private static Chams cachedChams;
    private static Blink cachedBlink;
    private static Backtrack cachedBacktrack;
    private static Module cachedNoNametags;
    private static io.github.exodar.module.modules.misc.MurderMystery cachedMurderMystery;
    private static long lastModuleCacheTime = 0;
    private static final long MODULE_CACHE_INTERVAL = 1000; // Refresh every 1 second

    /**
     * Refresh cached module references periodically
     */
    private static void refreshModuleCache(ModuleManager mm) {
        long now = System.currentTimeMillis();
        if (now - lastModuleCacheTime < MODULE_CACHE_INTERVAL) return;
        lastModuleCacheTime = now;

        if (mm == null) return;

        Module m = mm.getModuleByName("Nametags");
        cachedNametags = (m instanceof Nametags) ? (Nametags) m : null;

        m = mm.getModuleByName("Hitbox");
        cachedHitbox = (m instanceof Hitbox) ? (Hitbox) m : null;

        m = mm.getModuleByName("Skeleton");
        cachedSkeleton = (m instanceof Skeleton) ? (Skeleton) m : null;

        m = mm.getModuleByName("Glow");
        cachedGlow = (m instanceof Glow) ? (Glow) m : null;

        m = mm.getModuleByName("ESP");
        cachedESP = (m instanceof ESP) ? (ESP) m : null;

        m = mm.getModuleByName("Chams");
        cachedChams = (m instanceof Chams) ? (Chams) m : null;

        m = mm.getModuleByName("Blink");
        cachedBlink = (m instanceof Blink) ? (Blink) m : null;

        m = mm.getModuleByName("Backtrack");
        cachedBacktrack = (m instanceof Backtrack) ? (Backtrack) m : null;

        cachedNoNametags = mm.getModuleByName("NoVanillaNametags");

        m = mm.getModuleByName("MurderMystery");
        cachedMurderMystery = (m instanceof io.github.exodar.module.modules.misc.MurderMystery) ?
            (io.github.exodar.module.modules.misc.MurderMystery) m : null;
    }

    public CustomRenderPlayer(RenderManager manager, boolean useSmallArms, RenderPlayer original) {
        super(manager, useSmallArms);
        copyPlayerState(original);
    }

    private void copyPlayerState(RenderPlayer original) {
        try {
            // Recursively copy all fields from this class and all parent classes
            Class<?> currentClass = RenderPlayer.class;
            while (currentClass != null && currentClass != Object.class) {
                Field[] fields = currentClass.getDeclaredFields();
                for (Field field : fields) {
                    try {
                        // Skip static and transient fields
                        int modifiers = field.getModifiers();
                        if (java.lang.reflect.Modifier.isStatic(modifiers) ||
                                java.lang.reflect.Modifier.isTransient(modifiers) ||
                                java.lang.reflect.Modifier.isFinal(modifiers)) {
                            continue;
                        }

                        field.setAccessible(true);
                        Object value = field.get(original);
                        field.set(this, value);
                    } catch (Exception ignored) {
                        // Skip fields that can't be copied
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            System.out.println("✓ Successfully copied all fields from original player");
        } catch (Exception e) {
            System.err.println("Error copying player state: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Override to return spoofed skin if available
     */
    @Override
    protected ResourceLocation getEntityTexture(AbstractClientPlayer entity) {
        // Check if this player has a spoofed skin
        String playerName = entity.getName();
        ResourceLocation spoofedSkin = SpoofManager.getSpoofedSkin(playerName);
        if (spoofedSkin != null) {
            return spoofedSkin;
        }
        // Return normal skin
        return entity.getLocationSkin();
    }

    /**
     * Check if this entity is a developer/special account using SpoofManager
     */
    private boolean isSpecialAccount(AbstractClientPlayer entity) {
        String name = entity.getName();
        return name != null && (SpoofManager.isDeveloperAccount(name) || SpoofManager.isSpoofed(name));
    }

    /**
     * Check if text contains a special account name
     */
    private boolean containsSpecialAccount(String text) {
        if (text == null) return false;
        // Check all spoofed names
        for (String name : SpoofManager.getAllSpoofs().keySet()) {
            if (text.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void renderName(AbstractClientPlayer entity, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();

        // Never hide local player's nametag (for F5 view)
        boolean isLocalPlayer = (mc != null && entity == mc.thePlayer);

        // Check if custom Nametags module will handle rendering
        ModuleManager mm = Main.getModuleManager();
        if (mm != null && !isLocalPlayer) {
            // Refresh cache periodically
            refreshModuleCache(mm);

            // If Nametags module is enabled, skip ALL vanilla/spoofed rendering here
            // The custom nametags will be rendered by renderCustomName() in doRender()
            if (cachedNametags != null && cachedNametags.isEnabled()) {
                return; // Nametags module handles all nametag rendering
            }
            // Also check NoVanillaNametags module for backwards compatibility
            if (cachedNoNametags != null && cachedNoNametags.isEnabled()) {
                return; // Skip vanilla nametag rendering
            }
        }

        // If this player is a special account (developer/spoofed), render spoofed name
        // Only reaches here if Nametags module is NOT enabled
        if (isSpecialAccount(entity)) {
            renderForcedCustomName(entity, x, y, z);
            return;
        }

        super.renderName(entity, x, y, z);
    }

    /**
     * Render forced custom name for special players (developer/spoofed accounts)
     * This renders as vanilla-style nametag but with custom name
     * Preserves team prefixes like [MVP], [VIP], etc.
     */
    private void renderForcedCustomName(AbstractClientPlayer entity, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = rm.getFontRenderer();
        if (fr == null) return;

        // Get team-formatted name (includes [MVP], etc)
        ScorePlayerTeam team = (ScorePlayerTeam) entity.getTeam();
        String originalName = entity.getName();
        String name = originalName;
        if (team != null) {
            name = ScorePlayerTeam.formatPlayerName(team, name);
        }
        // Apply spoof from SpoofManager (preserves prefix)
        name = SpoofManager.applySpoof(name);

        // Adjust height like vanilla
        float yOffset = entity.height + 0.5F;
        if (entity.isSneaking()) {
            yOffset -= 0.25F;
        }

        GlStateManager.pushMatrix();

        // Translate to entity position
        GlStateManager.translate((float) x, (float) y + yOffset, (float) z);

        // Face the camera
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);

        // Scale like vanilla (0.02666667F = 1/37.5)
        float scale = 0.02666667F;
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);

        // Render behind walls with transparency
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int stringWidth = fr.getStringWidth(name) / 2;

        // Draw background (vanilla style)
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(-stringWidth - 1, -1, 0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldRenderer.pos(-stringWidth - 1, 8, 0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldRenderer.pos(stringWidth + 1, 8, 0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldRenderer.pos(stringWidth + 1, -1, 0).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        // Draw name through walls (faded)
        GlStateManager.disableDepth();
        fr.drawString(name, -stringWidth, 0, 0x20FFFFFF, false);

        // Draw name normally
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        fr.drawString(name, -stringWidth, 0, -1, true);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();
    }

    @Override
    public void doRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks) {
        ModuleManager mm = Main.getModuleManager();
        Chams chamsModule = null;
        boolean usePolygonOffset = false;
        Minecraft mc = Minecraft.getMinecraft();

        // Refresh module cache
        if (mm != null) {
            refreshModuleCache(mm);
        }

        // =====================================================
        // MURDER MYSTERY - Skip rendering sleeping players (corpses)
        // When MurderMystery module is enabled and player is "sleeping"
        // (lying on ground without actual bed), skip all visuals
        // =====================================================
        if (entity != mc.thePlayer && entity.isPlayerSleeping()) {
            if (cachedMurderMystery != null && cachedMurderMystery.isEnabled()) {
                // Player is a corpse - skip rendering entirely
                return;
            }
        }

        // =====================================================
        // ANTIBOT - Skip custom visuals for bot/NPC players
        // (still render player model, just skip ESP/Nametags/etc)
        // =====================================================
        boolean isBot = (entity != mc.thePlayer) && AntiBot.isBotForVisuals(entity);

        // =====================================================
        // LEGITAURA - Silent rotation for LOCAL PLAYER ONLY
        // Modifies model rotation without affecting camera
        // =====================================================
        boolean legitAuraActive = false;
        float savedRenderYawOffset = 0;
        float savedPrevRenderYawOffset = 0;
        float savedRotationYawHead = 0;
        float savedPrevRotationYawHead = 0;
        float savedRotationPitch = 0;
        float savedPrevRotationPitch = 0;

        // Only apply to LOCAL player in third person view (F5)
        if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView != 0) {
            // Check LegitAura first (all-in-one silent aim)
            if (LegitAura.hasActiveRotation()) {
                legitAuraActive = true;

                savedRenderYawOffset = entity.renderYawOffset;
                savedPrevRenderYawOffset = entity.prevRenderYawOffset;
                savedRotationYawHead = entity.rotationYawHead;
                savedPrevRotationYawHead = entity.prevRotationYawHead;
                savedRotationPitch = entity.rotationPitch;
                savedPrevRotationPitch = entity.prevRotationPitch;

                float targetYaw = LegitAura.getRenderYaw();
                float targetPitch = LegitAura.getRenderPitch();

                entity.renderYawOffset = targetYaw;
                entity.prevRenderYawOffset = targetYaw;
                entity.rotationYawHead = targetYaw;
                entity.prevRotationYawHead = targetYaw;
                if (Math.abs(targetPitch - entity.rotationPitch) > 5) {
                    entity.rotationPitch = targetPitch;
                    entity.prevRotationPitch = targetPitch;
                }
            }
            // Check LegitAura6
            else if (LegitAura6.hasActiveRotation()) {
                legitAuraActive = true;

                savedRenderYawOffset = entity.renderYawOffset;
                savedPrevRenderYawOffset = entity.prevRenderYawOffset;
                savedRotationYawHead = entity.rotationYawHead;
                savedPrevRotationYawHead = entity.prevRotationYawHead;
                savedRotationPitch = entity.rotationPitch;
                savedPrevRotationPitch = entity.prevRotationPitch;

                float targetYaw = LegitAura6.getRenderYaw();
                float targetPitch = LegitAura6.getRenderPitch();

                entity.renderYawOffset = targetYaw;
                entity.prevRenderYawOffset = targetYaw;
                entity.rotationYawHead = targetYaw;
                entity.prevRotationYawHead = targetYaw;
                if (Math.abs(targetPitch - entity.rotationPitch) > 5) {
                    entity.rotationPitch = targetPitch;
                    entity.prevRotationPitch = targetPitch;
                }
            }
            // Check LegitAura3
            else if (LegitAura3.hasActiveRotation()) {
                legitAuraActive = true;

                savedRenderYawOffset = entity.renderYawOffset;
                savedPrevRenderYawOffset = entity.prevRenderYawOffset;
                savedRotationYawHead = entity.rotationYawHead;
                savedPrevRotationYawHead = entity.prevRotationYawHead;
                savedRotationPitch = entity.rotationPitch;
                savedPrevRotationPitch = entity.prevRotationPitch;

                float targetYaw = LegitAura3.getRenderYaw();
                float targetPitch = LegitAura3.getRenderPitch();

                entity.renderYawOffset = targetYaw;
                entity.prevRenderYawOffset = targetYaw;
                entity.rotationYawHead = targetYaw;
                entity.prevRotationYawHead = targetYaw;
                if (Math.abs(targetPitch - entity.rotationPitch) > 5) {
                    entity.rotationPitch = targetPitch;
                    entity.prevRotationPitch = targetPitch;
                }
            }
        }

        // =====================================================
        // ANTIAIM - Client-side pitch for LOCAL PLAYER
        // Only applies pitch during render, doesn't affect camera
        // =====================================================
        boolean antiAimPitchActive = false;
        float savedAntiAimPitch = 0;
        float savedAntiAimPrevPitch = 0;

        // Only apply AntiAim pitch if LegitAura is NOT active (LegitAura takes priority for aiming)
        if (entity == mc.thePlayer && !legitAuraActive && AntiAim.shouldModifyPitch()) {
            antiAimPitchActive = true;
            savedAntiAimPitch = entity.rotationPitch;
            savedAntiAimPrevPitch = entity.prevRotationPitch;

            float targetPitch = AntiAim.getRenderPitch();
            entity.rotationPitch = targetPitch;
            entity.prevRotationPitch = targetPitch;
        }

        // Check for Chams module (use cached reference) - skip bots
        boolean skipNormalRender = false;
        if (mm != null && entity != mc.thePlayer && !isBot) {
            refreshModuleCache(mm);
            if (cachedChams != null && cachedChams.isEnabled()) {
                chamsModule = cachedChams;
                if (chamsModule.shouldApplyForEntity(entity)) {
                    if (chamsModule.isColored()) {
                        // Like Exhibition: Cancel normal render and do it ourselves
                        // This renders chams + layers (armor/items) correctly
                        renderColoredChamsComplete(entity, x, y, z, entityYaw, partialTicks, chamsModule);
                        skipNormalRender = true;
                    } else {
                        // Non-colored mode - use polygon offset for see-through effect
                        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
                        GL11.glPolygonOffset(1.0F, -1100000.0F);
                        usePolygonOffset = true;
                    }
                }
            }
        }

        if (!skipNormalRender) {
            // Ghost Invisibles - render invisible players as semi-transparent (skip bots)
            if (!isBot && entity.isInvisible() && entity != mc.thePlayer && cachedESP != null && cachedESP.isEnabled() && cachedESP.shouldGhostInvisibles()) {
                // Force render invisible player with reduced alpha
                renderGhostInvisible(entity, x, y, z, entityYaw, partialTicks);
            } else {
                try {
                    super.doRender(entity, x, y, z, entityYaw, partialTicks);
                } catch (IllegalArgumentException e) {
                    // Ignore - happens when player is "sleeping" but bed block is air
                    // Common in Murder Mystery corpses
                }
            }
        }


        // =====================================================
        // LEGITAURA - Restore original rotations after render
        // =====================================================
        if (legitAuraActive) {
            entity.renderYawOffset = savedRenderYawOffset;
            entity.prevRenderYawOffset = savedPrevRenderYawOffset;
            entity.rotationYawHead = savedRotationYawHead;
            entity.prevRotationYawHead = savedPrevRotationYawHead;
            entity.rotationPitch = savedRotationPitch;
            entity.prevRotationPitch = savedPrevRotationPitch;
        }

        // =====================================================
        // ANTIAIM - Restore original pitch after render
        // =====================================================
        if (antiAimPitchActive) {
            entity.rotationPitch = savedAntiAimPitch;
            entity.prevRotationPitch = savedAntiAimPrevPitch;
        }

        // Disable polygon offset after rendering
        if (usePolygonOffset) {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1.0F, 1100000.0F);
        }

        if (mm == null) return;

        // Refresh module cache (called once per frame, not per player)
        refreshModuleCache(mm);

        // Render custom nametags if Nametags module is enabled (skip bots)
        if (!isBot && cachedNametags != null && cachedNametags.isEnabled()) {
            renderCustomName(entity, x, y, z, cachedNametags);
        }

        // Render hitbox if Hitbox module is enabled (skip bots)
        if (!isBot && cachedHitbox != null && cachedHitbox.isEnabled()) {
            renderHitbox(entity, x, y, z, cachedHitbox);
        }

        // Render skeleton if Skeleton module is enabled (skip bots)
        if (!isBot && cachedSkeleton != null && cachedSkeleton.isEnabled()) {
            cachedSkeleton.renderSkeleton(entity, x, y, z, partialTicks);
        }

        // NOTE: Glow is now rendered via Render3DManager using BloomShader
        // No longer called per-entity from CustomRenderPlayer

        // Render ESP if enabled - same pattern as Skeleton (skip bots)
        if (!isBot && cachedESP != null && cachedESP.isEnabled()) {
            cachedESP.renderESP(entity, x, y, z);
        }

        // Render Blink position box (for local player)
        if (cachedBlink != null && cachedBlink.isEnabled()) {
            cachedBlink.renderBlink(entity, x, y, z);
        }

        // Render Backtrack position box (for target player)
        if (cachedBacktrack != null && cachedBacktrack.isEnabled()) {
            cachedBacktrack.renderBacktrack(entity, x, y, z);
        }

        // NOTE: World renders (BedESP, ItemESP, Trajectories, Tracers) are now handled by
        // Render3DManager via the EntityRenderHook proxy's Render3DEvent
        // This ensures correct GL matrices for 3D ESP rendering
    }

    /**
     * Render invisible player as semi-transparent ghost (like GM3 spectator view)
     */
    private void renderGhostInvisible(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // Temporarily make entity visible for rendering
        boolean wasInvisible = entity.isInvisible();

        // Calculate animation values
        float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
        float limbSwingAmount = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
        float ageInTicks = entity.ticksExisted + partialTicks;
        float headYaw = entity.prevRotationYawHead + (entity.rotationYawHead - entity.prevRotationYawHead) * partialTicks;
        float headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        float bodyYaw = entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;
        float netHeadYaw = headYaw - bodyYaw;
        float scale = 0.0625F;

        GlStateManager.pushMatrix();
        GlStateManager.disableCull();

        // Setup model
        net.minecraft.client.model.ModelBiped modelBiped = this.getMainModel();
        modelBiped.swingProgress = entity.getSwingProgress(partialTicks);
        modelBiped.isSneak = entity.isSneaking();
        modelBiped.isRiding = entity.isRiding();
        modelBiped.isChild = entity.isChild();

        ItemStack heldItem = entity.getHeldItem();
        modelBiped.aimedBow = heldItem != null && entity.getItemInUseCount() > 0
                && heldItem.getItem() instanceof net.minecraft.item.ItemBow;
        if (heldItem != null) {
            modelBiped.heldItemRight = 1;
            if (entity.getItemInUseCount() > 0) {
                net.minecraft.item.EnumAction action = heldItem.getItemUseAction();
                if (action == net.minecraft.item.EnumAction.BLOCK || action == net.minecraft.item.EnumAction.BOW ||
                    action == net.minecraft.item.EnumAction.EAT || action == net.minecraft.item.EnumAction.DRINK) {
                    modelBiped.heldItemRight = 3;
                }
            }
        } else {
            modelBiped.heldItemRight = 0;
        }

        modelBiped.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        modelBiped.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);

        // Matrix setup
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);

        float entityScale = 0.9375F;
        GlStateManager.scale(entityScale, entityScale, entityScale);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        GlStateManager.translate(0.0F, -1.5078125F, 0.0F);

        // Enable blending for transparency
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Bind player skin texture
        this.bindEntityTexture(entity);

        // Render with 30% alpha (ghost effect)
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.3F);

        // Render the model
        this.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        // Render layers (armor, etc.) also semi-transparent
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.3F);
        this.renderLayers(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);

        // Reset color and state
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();

        // Render name tag
        if (!this.renderOutlines) {
            this.renderName(entity, x, y, z);
        }
    }

    /**
     * Render custom nametag - Based on client-master implementation
     * Uses x, y, z coordinates provided directly by Minecraft's entity rendering system
     */
    private void renderCustomName(AbstractClientPlayer entity, double x, double y, double z, Nametags module) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = rm.getFontRenderer();

        // Skip self
        if (entity == mc.thePlayer) return;

        // Get team and apply team color formatting (like client-master)
        ScorePlayerTeam team = (ScorePlayerTeam) entity.getTeam();
        String name = entity.getName();
        String originalName = name; // Keep original for spoof lookup

        // Apply team color prefix if player is in a team
        if (team != null) {
            name = ScorePlayerTeam.formatPlayerName(team, name);
        }

        // Apply spoof from SpoofManager (handles developer accounts and custom spoofs)
        // This preserves team prefixes like [MVP]
        name = SpoofManager.applySpoof(name);

        // Apply AntiObfuscate (remove §k scrambled text)
        name = io.github.exodar.module.modules.misc.AntiObfuscate.stripObfuscated(name);

        // Add MurderMystery prefix if applicable
        String mmPrefix = io.github.exodar.module.modules.misc.MurderMystery.getNametagPrefix(originalName);
        if (mmPrefix != null) {
            name = mmPrefix + name;
        }

        // Add health if enabled
        if (module.isShowHealth()) {
            float health = 20.0f;
            float maxHealth = 20.0f;
            String healthSource = "FALLBACK";

            try {
                // PRIMARY: Try scoreboard belowname health first (most accurate on servers)
                int scoreboardHealth = Nametags.getScoreboardHealth(entity);
                if (scoreboardHealth > 0) {
                    // Detect if server uses HP format (0-20) by seeing values > 10
                    if (scoreboardHealth > 10) {
                        Nametags.setServerUsesHPFormat();
                    }

                    // Use consistent format detection from Nametags
                    if (Nametags.isServerUsingHPFormat()) {
                        // Server uses HP format (0-20), use value directly
                        health = scoreboardHealth;
                    } else {
                        // Server uses hearts format (0-10), convert to HP
                        health = scoreboardHealth * 2.0f;
                    }
                    maxHealth = 20.0f;
                    healthSource = "BELOWNAME(" + scoreboardHealth + ")";
                } else {
                    // FALLBACK: Use direct entity health if scoreboard didn't work
                    health = entity.getHealth();
                    maxHealth = entity.getMaxHealth();
                    if (maxHealth <= 0) maxHealth = 20.0f;
                    if (health < 0) health = 0;
                    healthSource = "ENTITY(" + (int)health + "/" + (int)maxHealth + ")";
                }
            } catch (Exception e) {
                // On any error, use entity health
                try {
                    health = entity.getHealth();
                    maxHealth = entity.getMaxHealth();
                    if (maxHealth <= 0) maxHealth = 20.0f;
                    healthSource = "ERROR-FALLBACK";
                } catch (Exception e2) {
                    health = 20.0f;
                    maxHealth = 20.0f;
                }
            }

            // Debug disabled - uncomment to enable
            // try {
            //     debugHealthSource(entity.getName(), healthSource);
            // } catch (Exception ignored) {}

            // Convert to hearts (divide by 2) and round up to fix 0.5 offset
            float hearts = (float) Math.ceil(health / 2.0f);
            double ratio = (maxHealth <= 0.0f) ? 0.0 : (health / maxHealth);
            String color = ratio < 0.3 ? "\u00a7c" : (ratio < 0.5 ? "\u00a76" : (ratio < 0.7 ? "\u00a7e" : "\u00a7a"));
            String heartStr = (hearts == (int) hearts) ? String.valueOf((int) hearts) : String.format("%.1f", hearts);
            name = name + " " + color + heartStr + " \u2764";

            // Add absorption separately in gold if enabled
            if (module.isAbsorptionSeparately()) {
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

        // Add distance if enabled
        if (module.isShowDistance()) {
            int dist = (int) mc.thePlayer.getDistanceToEntity(entity);
            String distColor = dist <= 8 ? "\u00a7c" : (dist <= 15 ? "\u00a76" : (dist <= 25 ? "\u00a7e" : "\u00a77"));
            name = distColor + dist + "m\u00a7r " + name;
        }

        // Get distance for autoscale
        float distance = mc.thePlayer.getDistanceToEntity(entity);

        // Get scale from module with optional autoscale
        float baseScale = module.getScale();
        float scale = baseScale;
        if (module.isAutoScale()) {
            // Scale proportionally to distance so all nametags appear same size on screen
            // Reference distance is 5 blocks - at 5 blocks, scale = baseScale
            // At 10 blocks: scale = 2x baseScale, at 2.5 blocks: scale = 0.5x baseScale
            float referenceDistance = 5.0F;
            float effectiveDistance = Math.max(1.0F, distance);
            scale = baseScale * (effectiveDistance / referenceDistance);
        }

        // Adjust height if sneaking (like client-master)
        float yOffset = entity.height + module.getYOffset();
        if (entity.isSneaking()) {
            yOffset -= 0.25F;
        }

        // No compensation needed - we anchor the nametag at its bottom edge
        // by using positive Y values in local space (which become negative after -scale flip)

        GlStateManager.pushMatrix();

        // Translate to entity position (like client-master - using coordinates from doRender)
        GlStateManager.translate((float) x, (float) y + yOffset, (float) z);

        // Face the camera (like client-master)
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);

        // Scale (like client-master)
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int stringWidth = fr.getStringWidth(name) / 2;

        // Collect armor items
        List<ItemStack> armorItems = new ArrayList<>();
        ItemStack heldItem = null;
        if (module.isShowArmor()) {
            heldItem = entity.getEquipmentInSlot(0);
            for (int i = 4; i >= 1; i--) {
                ItemStack stack = entity.getEquipmentInSlot(i);
                if (stack != null) armorItems.add(stack);
            }
        }

        int totalItems = armorItems.size() + (heldItem != null ? 1 : 0);
        // Items very close: 16px item - 2px overlap = 14px per item
        int itemSpacing = 14;
        int armorTotalWidth = totalItems * itemSpacing;

        float bgAlpha = module.getBgOpacity();

        int maxWidth = Math.max(stringWidth, armorTotalWidth / 2);

        // No invisible background needed anymore

        // Draw background for name ONLY
        // Y positions shifted so bottom is at Y=0 (nametag grows UPWARD after -scale flip)
        if (module.isShowBackground() && bgAlpha > 0.01f) {
            GlStateManager.disableTexture2D();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldRenderer.pos(-stringWidth - 1, -12, 0).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            worldRenderer.pos(-stringWidth - 1, 0, 0).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            worldRenderer.pos(stringWidth + 1, 0, 0).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            worldRenderer.pos(stringWidth + 1, -12, 0).color(0.0F, 0.0F, 0.0F, bgAlpha).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
        }

        // Draw the name with team color (shifted up so bottom aligns with Y=0)
        fr.drawString(name, -stringWidth, -10, -1, true);

        // Draw armor icons above name
        if (totalItems > 0) {
            int iconX = -armorTotalWidth / 2;
            int iconY = -28;

            if (heldItem != null) {
                renderItemStack(heldItem, iconX, iconY, fr, mc, module.isShowEnchants());
                iconX += itemSpacing;
            }

            for (ItemStack armorStack : armorItems) {
                renderItemStack(armorStack, iconX, iconY, fr, mc, module.isShowEnchants());
                iconX += itemSpacing;
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.popMatrix();
    }

    /**
     * Render item as a simple 2D textured quad - NO matrix operations inside
     * Uses same coordinate system as text rendering for stable positioning
     */
    private void renderItemStack(ItemStack stack, int xPos, int yPos, FontRenderer fr, Minecraft mc, boolean showEnchants) {
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
        if (showEnchants) {
            GlStateManager.scale(0.5, 0.5, 0.5);
            renderEnchantText(stack, xPos + 2, yPos + 16, fr);
            GlStateManager.scale(2.0, 2.0, 2.0);
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

    /**
     * Draw enchant with white letter and colored number
     * @return width of text drawn
     */
    private int drawEnchantWithColor(FontRenderer fr, String letter, int level, int x, int y) {
        // Draw letter in white
        int letterWidth = fr.drawStringWithShadow(letter, x, y, 0xFFFFFF);
        // Draw number in color
        fr.drawStringWithShadow(String.valueOf(level), letterWidth, y, getEnchantColor(level));
        return letterWidth;
    }

    private void renderEnchantText(ItemStack stack, int xPos, int yPos, FontRenderer fr) {
        // Position enchant text above the item
        int newYPos = (yPos - 16) * 2; // Adjusted for 0.5 scale
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

            if (eff > 0) { drawEnchantWithColor(fr, "E", eff, x, newYPos); newYPos += 8; }
            if (unbreak > 0) { drawEnchantWithColor(fr, "U", unbreak, x, newYPos); }
        } else {
            // For any other item (like sticks), show KB if present
            int kb = EnchantmentHelper.getEnchantmentLevel(19, stack);
            if (kb > 0) { drawEnchantWithColor(fr, "K", kb, x, newYPos); }
        }
    }

    /**
     * Render colored chams COMPLETE - Like Exhibition
     * This REPLACES the normal doRender, rendering:
     * 1. Chams (invisible + visible passes)
     * 2. Layers (armor, held items, etc.) on top
     */
    private void renderColoredChamsComplete(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, Chams chams) {
        Minecraft mc = Minecraft.getMinecraft();

        // Update rainbow animation
        chams.updateRainbow();

        // Calculate animation values exactly like RendererLivingEntity does
        float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
        float limbSwingAmount = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
        float ageInTicks = entity.ticksExisted + partialTicks;
        float headYaw = entity.prevRotationYawHead + (entity.rotationYawHead - entity.prevRotationYawHead) * partialTicks;
        float headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        float bodyYaw = entity.prevRenderYawOffset + (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;
        float netHeadYaw = headYaw - bodyYaw;
        float scale = 0.0625F;

        // === OUTER PUSH - for the entire render ===
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();

        // CRITICAL: Set model state BEFORE setRotationAngles (like vanilla RenderPlayer.doRender)
        net.minecraft.client.model.ModelBiped modelBiped = this.getMainModel();

        // Set swing progress for attack animation
        modelBiped.swingProgress = entity.getSwingProgress(partialTicks);

        // Set sneak animation (crouching)
        modelBiped.isSneak = entity.isSneaking();

        // Set riding animation (boat, horse, pig, minecart)
        modelBiped.isRiding = entity.isRiding();

        // Set child flag (for baby entities - not common for players but good to have)
        modelBiped.isChild = entity.isChild();

        // Set aimedBow for bow aiming animation
        ItemStack heldItem = entity.getHeldItem();
        modelBiped.aimedBow = heldItem != null && entity.getItemInUseCount() > 0
                && heldItem.getItem() instanceof net.minecraft.item.ItemBow;

        // Set heldItemRight for held item posture (0=nothing, 1=item, 3=blocking/eating)
        if (heldItem != null) {
            modelBiped.heldItemRight = 1;
            if (entity.getItemInUseCount() > 0) {
                net.minecraft.item.EnumAction action = heldItem.getItemUseAction();
                if (action == net.minecraft.item.EnumAction.BLOCK) {
                    modelBiped.heldItemRight = 3;
                } else if (action == net.minecraft.item.EnumAction.BOW) {
                    modelBiped.heldItemRight = 3;
                } else if (action == net.minecraft.item.EnumAction.EAT || action == net.minecraft.item.EnumAction.DRINK) {
                    // Eating/drinking animation - arm raised to mouth
                    modelBiped.heldItemRight = 3;
                }
            }
        } else {
            modelBiped.heldItemRight = 0;
        }

        // Setup model animations
        modelBiped.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        modelBiped.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);

        // === Matrix setup like RendererLivingEntity.doRender() ===
        // 1. Translate to entity position
        GlStateManager.translate((float) x, (float) y, (float) z);

        // 2. Rotate body
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);

        // 3. Apply player scale (0.9375F)
        float entityScale = 0.9375F;
        GlStateManager.scale(entityScale, entityScale, entityScale);

        // 4. Standard model transforms
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);

        // 5. Apply hurt/death rotation BEFORE final translate (like vanilla rotateCorpse)
        // This makes the body rotate around feet, not center
        if (entity.deathTime > 0) {
            // Death animation - body falls over
            float deathProgress = ((float) entity.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            deathProgress = net.minecraft.util.MathHelper.sqrt_float(deathProgress);
            if (deathProgress > 1.0F) deathProgress = 1.0F;
            GlStateManager.rotate(deathProgress * 90.0F, 0.0F, 0.0F, 1.0F);
        } else if (entity.hurtTime > 0) {
            // Hurt animation - tilt based on hurtTime (vanilla formula with -1)
            float f = (float)(entity.hurtTime - 1) - partialTicks;
            if (f < 0.0F) f = 0.0F;
            f = net.minecraft.util.MathHelper.sin(f / (float) entity.maxHurtTime * (float) Math.PI) * f;
            GlStateManager.rotate(f, 0.0F, 0.0F, 1.0F);
        }

        // 6. Final translate (after rotation so body rotates around feet)
        GlStateManager.translate(0.0F, -1.5078125F, 0.0F);

        // === INNER PUSH - for the chams model ===
        GL11.glPushMatrix();

        // Setup for colored rendering
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425); // GL_SMOOTH

        boolean isTextureMode = chams.isTextureMode();
        boolean wasLightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);

        if (isTextureMode) {
            // TEXTURE MODE: Keep texture enabled, bind skin, tint with color
            GlStateManager.enableTexture2D();
            this.bindEntityTexture(entity); // Bind player skin
            // Disable lighting for cleaner look but keep texture
            if (wasLightingEnabled) {
                GL11.glDisable(GL11.GL_LIGHTING);
            }
        } else {
            // FLAT MODE: No texture, solid color
            GlStateManager.disableTexture2D();
            if (wasLightingEnabled) {
                GL11.glDisable(GL11.GL_LIGHTING);
            }
        }

        // First pass: Render with invisible color (through walls) - depth test disabled
        GlStateManager.color(
            chams.getInvisibleRed(entity),
            chams.getInvisibleGreen(entity),
            chams.getInvisibleBlue(entity),
            chams.getInvisibleAlpha(entity)
        );
        this.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        // Second pass: Render with visible color - depth test enabled
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GlStateManager.color(
            chams.getVisibleRed(entity),
            chams.getVisibleGreen(entity),
            chams.getVisibleBlue(entity),
            chams.getVisibleAlpha(entity)
        );
        this.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);

        // Restore GL state for layers
        GlStateManager.shadeModel(7424); // GL_FLAT
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        // Restore lighting
        if (wasLightingEnabled) {
            GL11.glEnable(GL11.GL_LIGHTING);
        }

        GL11.glPopMatrix();
        // === END INNER - chams model done ===

        // === Now render layers (armor, held items, cape, etc.) ON TOP of chams ===
        // This is exactly what Exhibition does - layers render with normal textures
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderLayers(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);

        GlStateManager.disableRescaleNormal();
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        // === END OUTER ===

        // Render name tag (if not handled by Nametags module)
        if (!this.renderOutlines) {
            this.renderName(entity, x, y, z);
        }
    }

    /**
     * Render hitbox - using coordinates from doRender
     * Supports team colors when Teams module has Affect Visuals enabled
     */
    private void renderHitbox(AbstractClientPlayer entity, double x, double y, double z, Hitbox module) {
        GlStateManager.pushMatrix();

        // Disable textures and enable blending
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();

        // Get color from module (team-aware)
        float red = module.getRedForEntity(entity);
        float green = module.getGreenForEntity(entity);
        float blue = module.getBlueForEntity(entity);
        float alpha = module.getAlpha();
        float lineWidth = module.getLineWidth();

        GlStateManager.color(red, green, blue, alpha);
        GL11.glLineWidth(lineWidth);

        // Get the entity's bounding box
        AxisAlignedBB bb = entity.getEntityBoundingBox();

        // Create a new box offset by render position
        AxisAlignedBB renderBox = new AxisAlignedBB(
                bb.minX - entity.posX + x,
                bb.minY - entity.posY + y,
                bb.minZ - entity.posZ + z,
                bb.maxX - entity.posX + x,
                bb.maxY - entity.posY + y,
                bb.maxZ - entity.posZ + z
        );

        // Render the hitbox outline
        RenderGlobal.drawSelectionBoundingBox(renderBox);

        // Reset states
        GL11.glLineWidth(1.0F);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();

        GlStateManager.popMatrix();
    }

    /**
     * Debug health source to chat (throttled)
     */
    private void debugHealthSource(String playerName, String source) {
        long now = System.currentTimeMillis();
        Long lastTime = lastDebugTime.get(playerName);

        if (lastTime == null || now - lastTime > DEBUG_INTERVAL) {
            lastDebugTime.put(playerName, now);

            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                String msg = EnumChatFormatting.GOLD + "[Nametags HP] " +
                        EnumChatFormatting.WHITE + playerName +
                        EnumChatFormatting.GRAY + " -> " +
                        EnumChatFormatting.AQUA + source;
                mc.thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        }
    }
}
