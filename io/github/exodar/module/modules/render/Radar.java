/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Radar - Minimap showing player positions relative to you
 * Based on keystrokesmod Radar
 */
public class Radar extends Module {

    private SliderSetting size;
    private SliderSetting range;
    private SliderSetting posX;
    private SliderSetting posY;
    private TickSetting showTracerLines;
    private TickSetting showInvisibles;
    private ColorSetting bgColor;
    private ColorSetting borderColor;
    private ColorSetting playerColor;
    private ColorSetting friendColor;

    private int scale = 2;

    public Radar() {
        super("Radar", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Minimap with players"));

        // Size and position
        this.registerSetting(size = new SliderSetting("Size", 100.0, 50.0, 200.0, 5.0));
        this.registerSetting(range = new SliderSetting("Range", 60.0, 20.0, 150.0, 5.0));
        this.registerSetting(posX = new SliderSetting("Position X", 5.0, 0.0, 400.0, 1.0));
        this.registerSetting(posY = new SliderSetting("Position Y", 70.0, 0.0, 400.0, 1.0));

        // Options
        this.registerSetting(showTracerLines = new TickSetting("Tracer Lines", false));
        this.registerSetting(showInvisibles = new TickSetting("Show Invisibles", false));

        // Colors
        this.registerSetting(bgColor = new ColorSetting("Background", 0, 0, 0, 125));
        this.registerSetting(borderColor = new ColorSetting("Border", 255, 255, 255));
        this.registerSetting(playerColor = new ColorSetting("Player", 255, 50, 50));
        this.registerSetting(friendColor = new ColorSetting("Friend", 50, 255, 50));
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        try {
            ScaledResolution sr = new ScaledResolution(mc);
            this.scale = sr.getScaleFactor();
        } catch (Exception e) {
            this.scale = 2;
        }
    }

    /**
     * Render the radar - called from Main.onRenderGameOverlay
     */
    @Subscribe
    public void renderPointers(Render2DEvent event) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null || mc.gameSettings.showDebugInfo) return;

        int radarSize = (int) size.getValue();
        int x1 = (int) posX.getValue();
        int y1 = (int) posY.getValue();
        int x2 = x1 + radarSize;
        int y2 = y1 + radarSize;
        int centerX = x1 + radarSize / 2;
        int centerY = y1 + radarSize / 2;

        // Draw background
        int bgCol = bgColor.getColor();
        Gui.drawRect(x1, y1, x2, y2, bgCol);

        // Draw border
        int bordCol = borderColor.getColor();
        Gui.drawRect(x1 - 1, y1 - 1, x2 + 1, y1, bordCol); // Top
        Gui.drawRect(x1 - 1, y2, x2 + 1, y2 + 1, bordCol); // Bottom
        Gui.drawRect(x1 - 1, y1, x1, y2, bordCol); // Left
        Gui.drawRect(x2, y1, x2 + 1, y2, bordCol); // Right

        // Draw player indicator (triangle pointing up, representing forward)
        drawTriangle(centerX, centerY - 5, 5, 3, 0xFFFFFFFF);

        double maxRange = range.getValue();

        // Draw players (no scissor - just clamp positions)
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (player.isDead || player.deathTime > 0) continue;
            if (AntiBot.isBotForVisuals(player)) continue;
            if (player.isInvisible() && !showInvisibles.isEnabled()) continue;

            double distSq = player.getDistanceSqToEntity(mc.thePlayer);
            if (distSq > maxRange * maxRange) continue;

            // Calculate angle relative to player's rotation
            double dx = player.posX - mc.thePlayer.posX;
            double dz = player.posZ - mc.thePlayer.posZ;
            // atan2(dz, dx) gives angle from +X axis, we need angle from +Z (north)
            // Player yaw: 0 = south, 90 = west, 180 = north, 270 = east
            double angleToPlayer = Math.atan2(-dx, dz) * 180.0 / Math.PI;
            double relativeAngle = angleToPlayer - mc.thePlayer.rotationYaw;

            // Scale distance to fit radar
            double dist = Math.sqrt(distSq);
            double scaledDist = Math.min(dist / maxRange, 1.0) * (radarSize / 2.0 - 5);

            // Calculate position on radar (relative to center)
            double radAngle = Math.toRadians(relativeAngle);
            double offsetX = scaledDist * Math.sin(radAngle);
            double offsetY = -scaledDist * Math.cos(radAngle); // -cos because screen Y is inverted

            double dotX = centerX + offsetX;
            double dotY = centerY + offsetY;

            // Clamp to radar bounds
            dotX = Math.max(x1 + 3, Math.min(x2 - 3, dotX));
            dotY = Math.max(y1 + 3, Math.min(y2 - 3, dotY));

            // Draw tracer line if enabled
            if (showTracerLines.isEnabled()) {
                GL11.glPushMatrix();
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glBlendFunc(770, 771);
                GL11.glLineWidth(1.0f);
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex2d(centerX, centerY);
                GL11.glVertex2d(dotX, dotY);
                GL11.glEnd();
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glPopMatrix();
            }

            // Get player color
            int dotColor = getPlayerColor(player);

            // Draw player dot
            drawDot((int) dotX, (int) dotY, 3, dotColor);
        }
    }

    /**
     * Get color for player (friend, team, or enemy)
     */
    private int getPlayerColor(EntityPlayer player) {
        // Friend check
        if (Friends.isFriend(player.getName())) {
            return friendColor.getColor();
        }

        // Team color
        Teams teams = Teams.getInstance();
        if (teams != null && teams.shouldAffectVisuals()) {
            int teamColor = teams.getTeamColor(player);
            if (teamColor != -1) {
                return 0xFF000000 | teamColor;
            }
        }

        return playerColor.getColor();
    }

    /**
     * Draw a filled circle (dot)
     */
    private void drawDot(int x, int y, int radius, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < 360; i += 30) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + Math.cos(rad) * radius, y + Math.sin(rad) * radius);
        }
        GL11.glEnd();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    /**
     * Draw a triangle (player indicator)
     */
    private void drawTriangle(int x, int y, int width, int height, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(770, 771);
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2d(x, y);                    // Top
        GL11.glVertex2d(x - width, y + height * 2); // Bottom left
        GL11.glVertex2d(x + width, y + height * 2); // Bottom right
        GL11.glEnd();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }
}
