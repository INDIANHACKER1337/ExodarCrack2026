/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.Main;
import io.github.exodar.event.Render3DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.friend.FriendManager;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.spoof.SpoofManager;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

/**
 * Tracers - Draw lines from your view to other players
 */
public class Tracers extends Module {

    private final ModeSetting colorMode;
    private final SliderSetting opacity;
    private final SliderSetting lineWidth;
    private final TickSetting showPlayers;
    private final TickSetting showFriends;
    private final TickSetting showInvisibles;
    private final TickSetting hideTeam;
    private final TickSetting throughWalls;

    public Tracers() {
        super("Tracers", ModuleCategory.VISUALS);
        this.registerSetting(new DescriptionSetting("Lines to players"));

        this.registerSetting(colorMode = new ModeSetting("Color", new String[]{"Distance", "Team", "Static"}));
        this.registerSetting(opacity = new SliderSetting("Opacity", 80.0, 10.0, 100.0, 5.0));
        this.registerSetting(lineWidth = new SliderSetting("Line Width", 1.5, 0.5, 5.0, 0.5));

        this.registerSetting(new DescriptionSetting("--- Filters ---"));
        this.registerSetting(showPlayers = new TickSetting("Players", true));
        this.registerSetting(showFriends = new TickSetting("Friends", true));
        this.registerSetting(showInvisibles = new TickSetting("Invisibles", false));
        this.registerSetting(hideTeam = new TickSetting("Hide Team", false));
        this.registerSetting(throughWalls = new TickSetting("Through Walls", true));
    }

    /**
     * Check if entity should have tracer drawn
     */
    private boolean shouldRender(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        // Skip self
        if (player == mc.thePlayer) return false;
        if (player == mc.getRenderViewEntity()) return false;

        // Skip dead players
        if (player.deathTime > 0) return false;

        // Skip bots (AntiBot compatibility)
        if (AntiBot.isBotForVisuals(player)) return false;

        // Skip too far players
        if (mc.thePlayer.getDistanceToEntity(player) > 256) return false;

        // Check invisibility
        if (player.isInvisible() && !showInvisibles.isEnabled()) return false;

        // Check team
        if (hideTeam.isEnabled()) {
            Teams teams = Teams.getInstance();
            if (teams != null && teams.isEnabled() && teams.isTeamMate(player)) return false;
        }

        // Check if friend
        boolean isFriend = FriendManager.getInstance().isFriend(player.getName());
        if (isFriend) {
            return showFriends.isEnabled();
        }

        return showPlayers.isEnabled();
    }

    // Cached color values to avoid object creation
    private final float[] colorCache = new float[4];

    /**
     * Get color for entity based on mode - writes to colorCache[r,g,b,a]
     */
    private void getEntityColor(EntityPlayer player, float alpha, Minecraft mc) {
        // Friends are always cyan
        if (FriendManager.getInstance().isFriend(player.getName())) {
            colorCache[0] = 0f; colorCache[1] = 1f; colorCache[2] = 1f; colorCache[3] = alpha;
            return;
        }

        String mode = colorMode.getSelected();

        if ("Distance".equals(mode)) {
            float distance = mc.thePlayer.getDistanceToEntity(player);
            float ratio = Math.min(distance / 64.0f, 1.0f);
            colorCache[0] = ratio; colorCache[1] = 1f - ratio; colorCache[2] = 0f; colorCache[3] = alpha;
        } else if ("Team".equals(mode)) {
            String displayName = null;
            String playerName = player.getName();

            // For spoofed players, get CURRENT team from scoreboard (real-time, not cached)
            if (SpoofManager.isSpoofed(playerName)) {
                // Try to get team color from scoreboard directly (most accurate)
                if (player.getTeam() != null) {
                    displayName = net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName(player.getTeam(), playerName);
                }
                // Fallback: try cached team info
                if (displayName == null || displayName.equals(playerName)) {
                    SpoofManager.TeamInfo teamInfo = SpoofManager.getTeamInfo(playerName);
                    if (teamInfo != null && !teamInfo.prefix.isEmpty()) {
                        displayName = teamInfo.prefix + playerName + teamInfo.suffix;
                    }
                }
            }

            // Default: use display name directly
            if (displayName == null || displayName.isEmpty()) {
                displayName = player.getDisplayName().getFormattedText();
            }

            int color = 0xFFFFFF;
            if (displayName.contains("§")) {
                for (int i = 0; i < displayName.length() - 1; i++) {
                    if (displayName.charAt(i) == '§') {
                        char code = displayName.charAt(i + 1);
                        if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                            color = getMinecraftColor(code);
                            break;
                        }
                    }
                }
            }
            colorCache[0] = ((color >> 16) & 0xFF) / 255f;
            colorCache[1] = ((color >> 8) & 0xFF) / 255f;
            colorCache[2] = (color & 0xFF) / 255f;
            colorCache[3] = alpha;
        } else {
            // Static - red
            colorCache[0] = 1f; colorCache[1] = 0.2f; colorCache[2] = 0.2f; colorCache[3] = alpha;
        }
    }

    /**
     * Convert Minecraft color code to RGB
     */
    private int getMinecraftColor(char code) {
        switch (code) {
            case '0': return 0x000000; // Black
            case '1': return 0x0000AA; // Dark Blue
            case '2': return 0x00AA00; // Dark Green
            case '3': return 0x00AAAA; // Dark Aqua
            case '4': return 0xAA0000; // Dark Red
            case '5': return 0xAA00AA; // Dark Purple
            case '6': return 0xFFAA00; // Gold
            case '7': return 0xAAAAAA; // Gray
            case '8': return 0x555555; // Dark Gray
            case '9': return 0x5555FF; // Blue
            case 'a': return 0x55FF55; // Green
            case 'b': return 0x55FFFF; // Aqua
            case 'c': return 0xFF5555; // Red
            case 'd': return 0xFF55FF; // Light Purple
            case 'e': return 0xFFFF55; // Yellow
            case 'f': return 0xFFFFFF; // White
            default: return 0xFFFFFF;
        }
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
        if (mc.currentScreen != null) return;

        RenderManager rm = mc.getRenderManager();
        if (rm == null) return;

        // Use RenderManager camera position
        double camX = rm.viewerPosX;
        double camY = rm.viewerPosY;
        double camZ = rm.viewerPosZ;

        // Calculate start position (relative to camera)
        double eyeX, eyeY, eyeZ;

        if (mc.gameSettings.thirdPersonView == 0) {
            // First person - start from slightly in front of camera (looking direction)
            float yaw = mc.thePlayer.rotationYaw;
            float pitch = mc.thePlayer.rotationPitch;

            double dirX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
            double dirY = -Math.sin(Math.toRadians(pitch));
            double dirZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));

            eyeX = dirX;
            eyeY = dirY + mc.thePlayer.getEyeHeight();
            eyeZ = dirZ;
        } else {
            // Third person - start from player eye (relative to camera = 0)
            eyeX = 0;
            eyeY = mc.thePlayer.getEyeHeight();
            eyeZ = 0;
        }

        float alpha = (float)(opacity.getValue() / 100.0);

        // Setup GL state
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glLineWidth((float) lineWidth.getValue());

        if (throughWalls.isEnabled()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        // Draw lines to each player (limit to 50 to prevent lag)
        int rendered = 0;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (rendered >= 50) break;
            if (!(entity instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) entity;

            if (!shouldRender(player)) continue;
            rendered++;

            // Interpolate target position (relative to camera)
            double targetX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - camX;
            double targetY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - camY;
            double targetZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - camZ;

            // Adjust for sneaking
            if (player.isSneaking()) {
                targetY -= 0.125;
            }

            // Target eye height
            targetY += player.getEyeHeight();

            // Get color (writes to colorCache)
            getEntityColor(player, alpha, mc);
            GL11.glColor4f(colorCache[0], colorCache[1], colorCache[2], colorCache[3]);

            // Draw line
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3d(eyeX, eyeY, eyeZ);
            GL11.glVertex3d(targetX, targetY, targetZ);
            GL11.glEnd();
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    // @Override
    // public String getDisplaySuffix() {
    //     return " §7" + colorMode.getSelected();
    // }
}
