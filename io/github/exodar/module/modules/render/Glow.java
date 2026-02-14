/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.event.Render3DEvent;
import io.github.exodar.friend.FriendManager;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.shader.BloomShader;
import io.github.exodar.util.GlowUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import java.awt.Color;

/**
 * Glow - Rise-style glow effect using framebuffer bloom shader.
 * Renders players with a solid color to a framebuffer and applies
 * Gaussian blur for a real glow/bloom effect.
 */
public class Glow extends Module {

    private ModeSetting colorMode;
    private ColorSetting color;
    private ColorSetting friendColor;
    private TickSetting showInvisibles;
    private TickSetting showSelf;

    // Color presets
    private static final int FRIEND_COLOR = 0xFF55FF55;    // Green
    private static final int ENEMY_COLOR = 0xFFFF5555;     // Red
    private static final int TEAM_COLOR = 0xFF55FFFF;      // Cyan

    // Rainbow animation
    private float rainbowHue = 0;

    public Glow() {
        super("Glow", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Rise-style glow effect"));
        this.registerSetting(colorMode = new ModeSetting("Color", new String[]{"Custom", "Friend", "Team", "Rainbow"}));
        this.registerSetting(color = new ColorSetting("Glow Color", 255, 100, 100, 255));
        this.registerSetting(friendColor = new ColorSetting("Friend Color", 100, 255, 100, 255));
        this.registerSetting(showInvisibles = new TickSetting("Show Invisibles", true));
        this.registerSetting(showSelf = new TickSetting("Show Self", false));

        // Show/hide color settings based on mode
        colorMode.onChange(() -> {
            boolean isCustom = colorMode.getSelected().equals("Custom");
            boolean isFriend = colorMode.getSelected().equals("Friend");
            color.setVisible(isCustom);
            friendColor.setVisible(isFriend);
        });

        // Initial visibility
        boolean isCustom = colorMode.getSelected().equals("Custom");
        boolean isFriend = colorMode.getSelected().equals("Friend");
        color.setVisible(isCustom);
        friendColor.setVisible(isFriend);
    }

    /**
     * Called from Render3DManager to render glow effect
     */
    public void render3D(Render3DEvent event) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        BloomShader bloomShader = BloomShader.getInstance();
        if (!bloomShader.isAvailable()) {
            return;
        }

        // Update rainbow
        rainbowHue += 0.5f;
        if (rainbowHue >= 255) rainbowHue = 0;

        // Initialize/update bloom shader
        bloomShader.update();

        final float partialTicks = event.getPartialTicks();
        final RenderManager renderManager = mc.getRenderManager();

        // Add glow render task to bloom queue
        bloomShader.addToQueue(() -> {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityPlayer)) continue;
                EntityPlayer player = (EntityPlayer) entity;

                // Skip self unless enabled
                if (player == mc.thePlayer && !showSelf.isEnabled()) continue;

                // Skip bots
                if (AntiBot.isBotForVisuals(entity)) continue;

                // Skip invisible unless enabled
                if (player.isInvisible() && !showInvisibles.isEnabled()) continue;

                // Check if in view
                if (!isInViewFrustum(player)) continue;

                // Get render for this entity
                Render<EntityPlayer> render = renderManager.getEntityRenderObject(player);
                if (render == null) continue;

                // Get color based on mode
                Color glowColor = getColorForEntity(player);

                // Calculate interpolated position
                double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
                double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
                double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
                float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;

                // Render with glow color
                boolean wasInvisible = player.isInvisible();
                player.setInvisible(false);

                GlowUtil.setShaderBrightness(glowColor);
                try {
                    render.doRender(player,
                        x - renderManager.viewerPosX,
                        y - renderManager.viewerPosY,
                        z - renderManager.viewerPosZ,
                        yaw, partialTicks);
                } catch (Exception e) {
                    // Ignore render errors
                }
                GlowUtil.unsetShaderBrightness();

                player.setInvisible(wasInvisible);
            }

            RenderHelper.disableStandardItemLighting();
            mc.entityRenderer.disableLightmap();
        });

        // Apply the bloom effect
        bloomShader.render();
    }

    /**
     * Get the glow color for a specific entity based on current mode
     */
    private Color getColorForEntity(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();

        // Check if player is hurt - use red
        if (player.hurtTime > 0) {
            return Color.RED;
        }

        String mode = colorMode.getSelected();

        switch (mode) {
            case "Friend":
                if (player == mc.thePlayer) {
                    return new Color(friendColor.getColor());
                }
                if (FriendManager.getInstance().isFriend(player.getName())) {
                    return new Color(friendColor.getColor());
                }
                return new Color(ENEMY_COLOR);

            case "Team":
                if (player == mc.thePlayer) {
                    return new Color(TEAM_COLOR);
                }
                Teams teamsModule = Teams.getInstance();
                if (teamsModule != null && teamsModule.isEnabled() && teamsModule.isTeamMate(player)) {
                    return new Color(TEAM_COLOR);
                }
                return new Color(ENEMY_COLOR);

            case "Rainbow":
                float hue = rainbowHue + player.getEntityId() * 10;
                hue = hue % 255;
                int rgb = MathHelper.hsvToRGB(hue / 255.0f, 0.8f, 1.0f);
                return new Color(rgb);

            case "Custom":
            default:
                return new Color(color.getColor());
        }
    }

    /**
     * Simple frustum check - player is in view
     */
    private boolean isInViewFrustum(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderViewEntity() == null) return false;

        // Simple distance check - entities within render distance
        double distance = mc.thePlayer.getDistanceSqToEntity(entity);
        int renderDist = mc.gameSettings.renderDistanceChunks * 16;
        return distance < renderDist * renderDist;
    }

    @Override
    public String getDisplaySuffix() {
        return colorMode.getSelected();
    }
}
