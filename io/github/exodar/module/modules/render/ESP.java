/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * ESP - 2D Box ESP with Box/Corner/Split styles
 * Called directly from CustomRenderPlayer.doRender() like Skeleton
 */
public class ESP extends Module {

    private TickSetting showBox;
    private ModeSetting boxStyle;
    private TickSetting showHealthBar;
    private TickSetting showInvisibles;
    private TickSetting ghostInvisibles;
    private ColorSetting color;

    // Cached buffers - reused for all projections
    private static final FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
    private static final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer projection = BufferUtils.createFloatBuffer(16);

    public ESP() {
        super("ESP", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("2D Box ESP"));
        this.registerSetting(showBox = new TickSetting("Show Box", true));
        this.registerSetting(boxStyle = new ModeSetting("Style", new String[]{"Box", "Corner", "Split"})
            .onChange(() -> System.out.println("[ESP] Style changed to: " + boxStyle.getSelected())));
        this.registerSetting(color = new ColorSetting("Color", 255, 50, 50));
        this.registerSetting(showHealthBar = new TickSetting("Health Bar", true));
        this.registerSetting(showInvisibles = new TickSetting("Show Invisibles", false));
        this.registerSetting(ghostInvisibles = new TickSetting("Ghost Invisibles", true));
    }

    public boolean shouldGhostInvisibles() {
        return ghostInvisibles.isEnabled();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player != null && player != mc.thePlayer) {
                    player.ignoreFrustumCheck = true;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player != null && player != mc.thePlayer) {
                    player.ignoreFrustumCheck = false;
                }
            }
        }
    }

    public boolean shouldShowInvisibles() {
        return showInvisibles.isEnabled();
    }

    /**
     * Get color for entity - MurderMystery roles, friends, team color, or default
     */
    public int getColorForEntity(Entity entity) {
        if (entity instanceof EntityPlayer) {
            String playerName = entity.getName();

            // Check MurderMystery first (takes priority in MM games)
            int mmColor = io.github.exodar.module.modules.misc.MurderMystery.getESPColor(playerName);
            if (mmColor != -1) {
                return mmColor;
            }

            // Check if friend (cyan color)
            if (io.github.exodar.module.modules.misc.Friends.isFriend(playerName)) {
                return 0xFF55FFFF; // Cyan for friends
            }

            // Check for team color (affects all players, not just teammates)
            Teams teams = Teams.getInstance();
            if (teams != null && teams.shouldAffectVisuals()) {
                // Use Teams.getTeamColor() which properly bypasses spoof
                int teamColor = teams.getTeamColor(entity);
                if (teamColor != -1) {
                    return 0xFF000000 | (teamColor & 0x00FFFFFF);
                }
            }
        }

        return color.getColor();
    }

    /**
     * Called from CustomRenderPlayer.doRender() - renders immediately like Skeleton
     */
    public void renderESP(EntityPlayer player, double x, double y, double z) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (player == mc.thePlayer) return;
        if (AntiBot.isBotForVisuals(player)) return;

        // Invisible player handling:
        // - If Ghost Invisibles is enabled: show them semi-transparent
        // - If Show Invisibles is enabled: show them normally
        // - If neither: don't show them
        if (player.isInvisible() && !showInvisibles.isEnabled() && !ghostInvisibles.isEnabled()) return;

        // Get current matrices
        viewport.clear();
        modelView.clear();
        projection.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        // Player dimensions
        float width = player.width / 2.0f;
        float height = player.height;

        // Project all 8 corners of bounding box
        double[][] corners = {
            {x - width, y, z - width},
            {x + width, y, z - width},
            {x - width, y, z + width},
            {x + width, y, z + width},
            {x - width, y + height, z - width},
            {x + width, y + height, z - width},
            {x - width, y + height, z + width},
            {x + width, y + height, z + width}
        };

        double minScreenX = Double.MAX_VALUE;
        double maxScreenX = -Double.MAX_VALUE;
        double minScreenY = Double.MAX_VALUE;
        double maxScreenY = -Double.MAX_VALUE;
        boolean anyVisible = false;

        for (double[] corner : corners) {
            double[] screen = projectToScreen(corner[0], corner[1], corner[2]);
            if (screen != null) {
                anyVisible = true;
                if (screen[0] < minScreenX) minScreenX = screen[0];
                if (screen[0] > maxScreenX) maxScreenX = screen[0];
                if (screen[1] < minScreenY) minScreenY = screen[1];
                if (screen[1] > maxScreenY) maxScreenY = screen[1];
            }
        }

        if (!anyVisible || minScreenX >= maxScreenX || minScreenY >= maxScreenY) return;

        // Get health - PRIMARY: belowname scoreboard, FALLBACK: direct entity health
        float health = -1;
        float maxHealth = 20.0f;

        try {
            // PRIMARY: Try scoreboard belowname health first (most accurate on servers)
            int scoreboardHealth = Nametags.getScoreboardHealth(player);
            if (scoreboardHealth > 0) {
                // Scoreboard health is usually in half-hearts (0-20) or hearts (0-10)
                // If value is <= 10, assume it's hearts and convert to half-hearts
                health = scoreboardHealth <= 10 ? scoreboardHealth * 2.0f : scoreboardHealth;
                if (health > maxHealth) {
                    maxHealth = health;
                }
            }

            // FALLBACK: Use direct entity health if scoreboard didn't work
            if (health < 0) {
                float directHealth = player.getHealth();
                float directMaxHealth = player.getMaxHealth();

                // Validate values
                if (directMaxHealth > 0 && directMaxHealth <= 100 && !Float.isNaN(directMaxHealth)) {
                    maxHealth = directMaxHealth;
                }
                if (directHealth >= 0 && !Float.isNaN(directHealth)) {
                    health = directHealth;
                } else {
                    health = maxHealth; // Default to full if invalid
                }

                // Add absorption amount
                float absorption = player.getAbsorptionAmount();
                if (absorption > 0 && !Float.isNaN(absorption)) {
                    health += absorption;
                    if (health > maxHealth) {
                        maxHealth = health;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to full health if anything fails
            health = 20.0f;
            maxHealth = 20.0f;
        }

        // Final safety checks
        if (Float.isNaN(health) || Float.isInfinite(health) || health < 0) health = 20.0f;
        if (Float.isNaN(maxHealth) || Float.isInfinite(maxHealth) || maxHealth <= 0) maxHealth = 20.0f;
        if (health > maxHealth) maxHealth = health;

        // Clamp health to valid range
        health = Math.max(0.0f, Math.min(health, maxHealth));

        // Scale factor
        ScaledResolution sr = new ScaledResolution(mc);
        double scaleFactor = sr.getScaleFactor();

        double sMinX = minScreenX / scaleFactor;
        double sMinY = minScreenY / scaleFactor;
        double sMaxX = maxScreenX / scaleFactor;
        double sMaxY = maxScreenY / scaleFactor;

        // Save all GL state
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        // Setup 2D rendering
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, sr.getScaledWidth(), sr.getScaledHeight(), 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        // Use raw GL11 only to avoid state sync issues
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        // Get color (team-aware)
        int c = getColorForEntity(player);
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;

        // Ghost effect for invisible players (semi-transparent like spectators)
        float alpha = 1.0f;
        if (player.isInvisible() && ghostInvisibles.isEnabled()) {
            alpha = 0.3f; // Semi-transparent like GM3
        }

        String style = boxStyle.getSelected();

        // Draw box if enabled
        if (showBox.isEnabled()) {
            // Black outline (thicker)
            GL11.glLineWidth(2.5f);
            GL11.glColor4f(0.0f, 0.0f, 0.0f, alpha);
            drawStyle(style, sMinX, sMinY, sMaxX, sMaxY);

            // Colored line (thinner)
            GL11.glLineWidth(1.0f);
            GL11.glColor4f(r, g, b, alpha);
            drawStyle(style, sMinX, sMinY, sMaxX, sMaxY);
        }

        // Health bar (to the left of the box)
        if (showHealthBar.isEnabled() && maxHealth > 0) {
            double boxWidth = sMaxX - sMinX;
            drawHealthBar(sMinX, sMinY, sMaxY, health, maxHealth, boxWidth);
        }

        // Restore matrices
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();

        // Restore all GL state
        GL11.glPopAttrib();
        GL11.glPopMatrix();

        // Force re-enable textures for GlStateManager sync
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private double[] projectToScreen(double x, double y, double z) {
        screenCoords.clear();

        boolean result = GLU.gluProject((float) x, (float) y, (float) z, modelView, projection, viewport, screenCoords);

        if (result) {
            float screenX = screenCoords.get(0);
            float screenY = viewport.get(3) - screenCoords.get(1);
            float screenZ = screenCoords.get(2);

            if (screenZ >= 0 && screenZ <= 1) {
                return new double[]{screenX, screenY};
            }
        }
        return null;
    }

    private void drawStyle(String style, double minX, double minY, double maxX, double maxY) {
        switch (style) {
            case "Box":
                drawBox2D(minX, minY, maxX, maxY);
                break;
            case "Corner":
                drawCorners2D(minX, minY, maxX, maxY);
                break;
            case "Split":
                drawSplit2D(minX, minY, maxX, maxY);
                break;
        }
    }

    private void drawHealthBar(double x, double minY, double maxY, float health, float maxHealth, double boxWidth) {
        // Bar dimensions - same style as box
        double barHeight = maxY - minY;
        if (barHeight <= 0 || maxHealth <= 0) return;

        // Bar width scales with box size (proportional to distance)
        double barWidth = Math.max(1.5, Math.min(4.0, boxWidth * 0.03));
        double barX = x - barWidth - 1.5;

        // Clamp health
        if (health < 0) health = 0;
        if (health > maxHealth) health = maxHealth;
        double healthRatio = health / maxHealth;

        // Health color: Green (100%) -> Yellow (50%) -> Red (0%)
        float r, g;
        if (healthRatio > 0.5) {
            r = (float) ((1.0 - healthRatio) * 2.0);
            g = 1.0f;
        } else {
            r = 1.0f;
            g = (float) (healthRatio * 2.0);
        }

        double fillHeight = barHeight * healthRatio;

        // Draw outline around health bar (thin black border)
        GL11.glLineWidth(1.0f);
        GL11.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(barX, minY);
        GL11.glVertex2d(barX + barWidth, minY);
        GL11.glVertex2d(barX + barWidth, maxY);
        GL11.glVertex2d(barX, maxY);
        GL11.glEnd();

        // 3. Draw health fill as a filled rectangle (from bottom up)
        if (fillHeight > 0) {
            double fillTop = maxY - fillHeight;

            // Use Tessellator for fill
            net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
            net.minecraft.client.renderer.WorldRenderer wr = tessellator.getWorldRenderer();

            wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
            wr.pos(barX + 0.5, fillTop, 0).color(r, g, 0.0f, 1.0f).endVertex();
            wr.pos(barX + 0.5, maxY - 0.5, 0).color(r, g, 0.0f, 1.0f).endVertex();
            wr.pos(barX + barWidth - 0.5, maxY - 0.5, 0).color(r, g, 0.0f, 1.0f).endVertex();
            wr.pos(barX + barWidth - 0.5, fillTop, 0).color(r, g, 0.0f, 1.0f).endVertex();
            tessellator.draw();
        }
    }

    private void drawBox2D(double minX, double minY, double maxX, double maxY) {
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX, minY);
        GL11.glVertex2d(maxX, minY);
        GL11.glVertex2d(maxX, maxY);
        GL11.glVertex2d(minX, maxY);
        GL11.glEnd();
    }

    private void drawCorners2D(double minX, double minY, double maxX, double maxY) {
        double w = maxX - minX;
        double h = maxY - minY;
        double len = Math.min(w, h) * 0.25;

        GL11.glBegin(GL11.GL_LINES);
        // Top-left corner
        GL11.glVertex2d(minX, minY); GL11.glVertex2d(minX + len, minY);
        GL11.glVertex2d(minX, minY); GL11.glVertex2d(minX, minY + len);
        // Top-right corner
        GL11.glVertex2d(maxX, minY); GL11.glVertex2d(maxX - len, minY);
        GL11.glVertex2d(maxX, minY); GL11.glVertex2d(maxX, minY + len);
        // Bottom-left corner
        GL11.glVertex2d(minX, maxY); GL11.glVertex2d(minX + len, maxY);
        GL11.glVertex2d(minX, maxY); GL11.glVertex2d(minX, maxY - len);
        // Bottom-right corner
        GL11.glVertex2d(maxX, maxY); GL11.glVertex2d(maxX - len, maxY);
        GL11.glVertex2d(maxX, maxY); GL11.glVertex2d(maxX, maxY - len);
        GL11.glEnd();
    }

    private void drawSplit2D(double minX, double minY, double maxX, double maxY) {
        double w = maxX - minX;
        double h = maxY - minY;
        double len = Math.min(w, h) * 0.25;
        double midY = minY + h / 2;

        GL11.glBegin(GL11.GL_LINES);
        // Top corners
        GL11.glVertex2d(minX, minY); GL11.glVertex2d(minX + len, minY);
        GL11.glVertex2d(maxX, minY); GL11.glVertex2d(maxX - len, minY);
        // Bottom corners
        GL11.glVertex2d(minX, maxY); GL11.glVertex2d(minX + len, maxY);
        GL11.glVertex2d(maxX, maxY); GL11.glVertex2d(maxX - len, maxY);
        // Side lines
        GL11.glVertex2d(minX, minY); GL11.glVertex2d(minX, maxY);
        GL11.glVertex2d(maxX, minY); GL11.glVertex2d(maxX, maxY);
        // Middle dashes
        GL11.glVertex2d(minX, midY); GL11.glVertex2d(minX + len, midY);
        GL11.glVertex2d(maxX, midY); GL11.glVertex2d(maxX - len, midY);
        GL11.glEnd();
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + boxStyle.getSelected();
    }
}
