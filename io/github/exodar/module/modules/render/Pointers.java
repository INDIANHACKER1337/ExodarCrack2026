/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.Main;
import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Pointers - Displays arrows around crosshair pointing to other players
 * Shows direction to players even outside field of view
 */
public class Pointers extends Module {

    // Range settings
    private SliderSetting range;
    private SliderSetting ignoreWithinFov;

    // Style settings
    private ModeSetting style;
    private ModeSetting colorMode;

    // Distance colors
    private ColorSetting nearColor;
    private ColorSetting farColor;
    private SliderSetting transitionNear;
    private SliderSetting transitionFar;

    // Manual colors
    private ColorSetting enemyColor;
    private ColorSetting friendColor;

    // Appearance
    private SliderSetting scale;
    private SliderSetting radius;
    private TickSetting distanceBasedRadius;

    // Filter
    private TickSetting showInvisibles;
    private TickSetting hideTeam;

    public Pointers() {
        super("Pointers", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Arrows pointing to players"));

        // Range
        this.registerSetting(range = new SliderSetting("Range", 100.0, 10.0, 200.0, 5.0));
        this.registerSetting(ignoreWithinFov = new SliderSetting("Ignore FOV", 0.0, 0.0, 180.0, 5.0));

        // Style
        this.registerSetting(style = new ModeSetting("Style", new String[]{"2D", "3D"}));
        this.registerSetting(colorMode = new ModeSetting("Colors", new String[]{"Distance", "Name Tag", "Manual"}));

        // Distance colors
        this.registerSetting(nearColor = new ColorSetting("Near Color", 255, 50, 50));
        this.registerSetting(farColor = new ColorSetting("Far Color", 50, 255, 50));
        this.registerSetting(transitionNear = new SliderSetting("Near Distance", 10.0, 1.0, 50.0, 1.0));
        this.registerSetting(transitionFar = new SliderSetting("Far Distance", 50.0, 10.0, 150.0, 5.0));

        // Manual colors
        this.registerSetting(enemyColor = new ColorSetting("Enemy Color", 255, 100, 100));
        this.registerSetting(friendColor = new ColorSetting("Friend Color", 100, 255, 100));

        // Appearance
        this.registerSetting(scale = new SliderSetting("Scale", 1.0, 0.5, 3.0, 0.1));
        this.registerSetting(radius = new SliderSetting("Radius", 50.0, 20.0, 150.0, 5.0));
        this.registerSetting(distanceBasedRadius = new TickSetting("Distance Radius", true));

        // Filter
        this.registerSetting(showInvisibles = new TickSetting("Show Invisibles", false));
        this.registerSetting(hideTeam = new TickSetting("Hide Team", false));
    }

    /**
     * Render pointers - called from HUD rendering (2D overlay)
     */
    @Subscribe
    public void renderPointers(Render2DEvent event) {
        float partialTicks = event.getPartialTicks();
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        // Get player's look direction
        float playerYaw = mc.thePlayer.rotationYaw;
        float playerPitch = mc.thePlayer.rotationPitch;

        // Calculate FOV threshold for ignoring
        float fovThreshold = (float) ignoreWithinFov.getValue();
        float maxRange = (float) range.getValue();

        // Save GL state
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // Setup 2D rendering
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        // Iterate through players
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (player.isDead) continue;
            if (AntiBot.isBotForVisuals(player)) continue;
            if (player.isInvisible() && !showInvisibles.isEnabled()) continue;

            // Team filter
            if (hideTeam.isEnabled()) {
                Teams teams = Teams.getInstance();
                if (teams != null && teams.isEnabled() && teams.isTeamMate(player)) continue;
            }

            // Check distance
            float distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance > maxRange) continue;

            // Calculate angle to player
            double dx = player.posX - mc.thePlayer.posX;
            double dz = player.posZ - mc.thePlayer.posZ;
            double dy = (player.posY + player.getEyeHeight()) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());

            // Calculate yaw angle to player (in degrees)
            float angleToPlayer = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;

            // Calculate relative angle (difference from where player is looking)
            float relativeYaw = MathHelper.wrapAngleTo180_float(angleToPlayer - playerYaw);

            // Calculate pitch angle to player
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            float pitchToPlayer = (float) -(Math.atan2(dy, horizontalDist) * 180.0 / Math.PI);
            float relativePitch = pitchToPlayer - playerPitch;

            // Check if player is within FOV to ignore
            float absoluteAngle = (float) Math.sqrt(relativeYaw * relativeYaw + relativePitch * relativePitch);
            if (absoluteAngle < fovThreshold) continue;

            // Get pointer color
            int color = getPointerColor(player, distance);

            // Calculate radius for this pointer
            float pointerRadius = (float) radius.getValue();
            if (distanceBasedRadius.isEnabled()) {
                // Scale radius based on distance (farther = larger radius)
                float nearDist = (float) transitionNear.getValue();
                float farDist = (float) transitionFar.getValue();
                float distRatio = MathHelper.clamp_float((distance - nearDist) / (farDist - nearDist), 0.0F, 1.0F);
                pointerRadius += distRatio * 30; // Add up to 30 pixels for far players
            }

            // Calculate pointer position on screen
            // relativeYaw: positive = player is to the RIGHT, negative = player is to the LEFT
            // We want the pointer to be positioned where the player is relative to crosshair
            float pointerAngle = (float) Math.toRadians(relativeYaw - 90); // -90 to align with screen coords (up = -Y)
            float pointerX = centerX + (float) Math.cos(pointerAngle) * pointerRadius;
            float pointerY = centerY + (float) Math.sin(pointerAngle) * pointerRadius;

            // Render the pointer - rotation should make arrow point OUTWARD from center (towards player)
            float pointerScale = (float) scale.getValue();
            float arrowRotation = relativeYaw; // Arrow points in direction of player

            if (style.getSelected().equals("3D")) {
                // 3D style - render at an angle based on pitch
                float pitchFactor = MathHelper.clamp_float(relativePitch / 90.0F, -1.0F, 1.0F);
                renderPointer3D(pointerX, pointerY, arrowRotation, pitchFactor, pointerScale, color);
            } else {
                // 2D style - flat on screen
                renderPointer2D(pointerX, pointerY, arrowRotation, pointerScale, color);
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /**
     * Get color for pointer based on color mode
     */
    private int getPointerColor(EntityPlayer player, float distance) {
        String mode = colorMode.getSelected();

        switch (mode) {
            case "Distance":
                // Interpolate between near and far colors based on distance
                float nearDist = (float) transitionNear.getValue();
                float farDist = (float) transitionFar.getValue();
                float ratio = MathHelper.clamp_float((distance - nearDist) / (farDist - nearDist), 0.0F, 1.0F);

                int nearC = nearColor.getColor();
                int farC = farColor.getColor();

                int nr = (nearC >> 16) & 0xFF;
                int ng = (nearC >> 8) & 0xFF;
                int nb = nearC & 0xFF;
                int fr = (farC >> 16) & 0xFF;
                int fg = (farC >> 8) & 0xFF;
                int fb = farC & 0xFF;

                int r = (int) (nr + (fr - nr) * ratio);
                int g = (int) (ng + (fg - ng) * ratio);
                int b = (int) (nb + (fb - nb) * ratio);

                return 0xFF000000 | (r << 16) | (g << 8) | b;

            case "Name Tag":
                // Use team color or default
                Teams teams = Teams.getInstance();
                if (teams != null && teams.shouldAffectVisuals()) {
                    int teamColor = teams.getTeamColor(player);
                    if (teamColor != -1) {
                        return 0xFF000000 | teamColor;
                    }
                }
                // Fallback to white
                return 0xFFFFFFFF;

            case "Manual":
                // Friend or enemy
                if (Friends.isFriend(player.getName())) {
                    return friendColor.getColor();
                }
                return enemyColor.getColor();

            default:
                return 0xFFFFFFFF;
        }
    }

    /**
     * Render 2D pointer (triangle arrow)
     * Triangle points UP by default (-Y direction), then rotates to point toward player
     */
    private void renderPointer2D(float x, float y, float rotation, float scale, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(rotation, 0, 0, 1); // Arrow points OUTWARD from center (toward player)
        GL11.glScalef(scale, scale, 1);

        // Draw filled triangle pointing outward (tip pointing away from center)
        GL11.glColor4f(r, g, b, 0.8F);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(0, -10);  // Tip (pointing outward)
        GL11.glVertex2f(-6, 4);   // Bottom left
        GL11.glVertex2f(6, 4);    // Bottom right
        GL11.glEnd();

        // Draw outline
        GL11.glLineWidth(1.5F);
        GL11.glColor4f(0, 0, 0, 1.0F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(0, -10);
        GL11.glVertex2f(-6, 4);
        GL11.glVertex2f(6, 4);
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    /**
     * Render 3D pointer (triangle with depth/angle)
     */
    private void renderPointer3D(float x, float y, float rotation, float pitchFactor, float scale, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(rotation, 0, 0, 1); // Arrow points OUTWARD from center (toward player)

        // Apply 3D tilt based on pitch
        float tiltAngle = pitchFactor * 45; // Max 45 degree tilt
        GL11.glRotatef(tiltAngle, 1, 0, 0);

        GL11.glScalef(scale, scale, 1);

        // Draw filled triangle with slight 3D effect
        float depthOffset = pitchFactor * 3;

        GL11.glColor4f(r, g, b, 0.8F);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(0, -10 + depthOffset);  // Tip (pointing outward)
        GL11.glVertex2f(-6, 4 - depthOffset);   // Bottom left
        GL11.glVertex2f(6, 4 - depthOffset);    // Bottom right
        GL11.glEnd();

        // Draw outline
        GL11.glLineWidth(1.5F);
        GL11.glColor4f(0, 0, 0, 1.0F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(0, -10 + depthOffset);
        GL11.glVertex2f(-6, 4 - depthOffset);
        GL11.glVertex2f(6, 4 - depthOffset);
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + style.getSelected();
    }
}
