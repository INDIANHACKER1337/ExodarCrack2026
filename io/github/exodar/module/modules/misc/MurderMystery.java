/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.event.Subscribe;
import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Render3DEvent;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TextSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.*;

/**
 * MurderMystery - Detects murderers and detectives in Murder Mystery games
 *
 * Features:
 * - Detects knife by name ("Knife", "Cuchillo", etc.)
 * - Fallback: detects swords, axes, and other murder weapons
 * - Detects game via scoreboard (Murder Mystery, MURDER, MYSTERY)
 * - Integrates with Nametags module for prefixes
 * - Highlights murderers in red and detectives/bow holders in green
 */
public class MurderMystery extends Module {

    private static MurderMystery instance;

    private final TickSetting alertMurderers;
    private final TickSetting searchDetectives;
    private final TickSetting renderBox;
    private final TickSetting removeCorpses;
    // Warning settings
    private final TickSetting warningText;
    private final SliderSetting warningDistance;
    private final TickSetting heartbeatSound;
    // Announcer settings (at bottom)
    private final TickSetting announceMurderer;
    private final TextSetting announceMessage;

    // Known murderer knife names (case insensitive)
    private static final String[] KNIFE_NAMES = {
        "knife", "cuchillo", "dagger", "blade", "murderer",
        "killer", "faca", "messer", "couteau", "navaja"
    };

    // Player role tracking
    private final Set<String> murderers = new HashSet<>();
    private final Set<String> detectives = new HashSet<>();

    // Announced tracking (to avoid spam)
    private final Set<String> announcedMurderers = new HashSet<>();
    private final Set<String> announcedDetectives = new HashSet<>();

    // World change detection
    private int lastWorldId = 0;

    // Detect if server uses §c for player names (then we use §4 for MURDER)
    private boolean serverUsesRedNames = false;

    // Warning system tracking
    private String closestMurdererName = null;
    private double closestMurdererDistance = Double.MAX_VALUE;
    private long lastHeartbeatTime = 0;

    public MurderMystery() {
        super("MurderMystery", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Detects murderers/detectives"));
        this.registerSetting(alertMurderers = new TickSetting("Alert", true));
        this.registerSetting(searchDetectives = new TickSetting("Search Detectives", true));
        this.registerSetting(renderBox = new TickSetting("Render Box", false));
        this.registerSetting(removeCorpses = new TickSetting("Remove Corpses", true));
        // Warning system
        this.registerSetting(warningText = new TickSetting("Warning Text", true));
        this.registerSetting(warningDistance = new SliderSetting("Warning Distance", 30, 5, 50, 1));
        this.registerSetting(heartbeatSound = new TickSetting("Heartbeat Sound", true));
        // Announcer at bottom
        this.registerSetting(announceMurderer = new TickSetting("Announcer", false));
        this.registerSetting(announceMessage = new TextSetting("Announce Msg", "%s is a murderer!"));
    }

    @Override
    public void onEnable() {
        clearAll();
    }

    @Override
    public void onDisable() {
        clearAll();
    }

    private void clearAll() {
        murderers.clear();
        detectives.clear();
        announcedMurderers.clear();
        announcedDetectives.clear();
        serverUsesRedNames = false;
        closestMurdererName = null;
        closestMurdererDistance = Double.MAX_VALUE;
        lastHeartbeatTime = 0;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;

        // Detect world change and clear data
        int currentWorldId = mc.theWorld.hashCode();
        if (currentWorldId != lastWorldId) {
            lastWorldId = currentWorldId;
            clearAll();
        }

        // If not in Murder Mystery, pause detection but DON'T clear lists
        // This fixes flaky scoreboard detection clearing murderers and breaking nametags
        // Lists only get cleared on world change (above) or module disable
        if (!isInMurderMystery()) {
            return;
        }

        // Detect if server uses §c (light red) for player names
        // If so, we'll use §4 (dark red) for MURDER tag to distinguish
        if (!serverUsesRedNames) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                ScorePlayerTeam team = (ScorePlayerTeam) player.getTeam();
                if (team != null) {
                    String formatted = ScorePlayerTeam.formatPlayerName(team, player.getName());
                    // Check for §c (light red) in the formatted name
                    if (formatted.contains("\u00A7c") || formatted.contains("§c")) {
                        serverUsesRedNames = true;
                        break;
                    }
                }
            }
        }

        // Scan players for weapons
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (player.isInvisible()) continue;

            String playerName = player.getName();

            // Check held item
            ItemStack held = player.getHeldItem();
            if (held == null) continue;

            Item item = held.getItem();
            String displayName = held.hasDisplayName() ? held.getDisplayName().toLowerCase() : "";

            // === MURDERER DETECTION ===
            // Priority 1: Knife by name
            if (isKnifeByName(displayName)) {
                addMurderer(playerName);
                continue;
            }

            // Priority 2: Sword (fallback - most common murder weapon)
            if (item instanceof ItemSword) {
                addMurderer(playerName);
                continue;
            }

            // Priority 3: Other murder weapons (axes, hoes, pickaxes, fishing rods)
            if (isMurdererWeapon(item)) {
                addMurderer(playerName);
                continue;
            }

            // === DETECTIVE DETECTION ===
            // Detect by bow OR arrows
            if (searchDetectives.isEnabled()) {
                if (item instanceof ItemBow || item == net.minecraft.init.Items.arrow) {
                    addDetective(playerName);
                }
            }
        }

        // Remove only truly disconnected players (both murderers and detectives kept persistently)
        murderers.removeIf(this::shouldRemoveTrackedPlayer);
        detectives.removeIf(this::shouldRemoveTrackedPlayer);

        // === WARNING SYSTEM: Track closest murderer ===
        closestMurdererName = null;
        closestMurdererDistance = Double.MAX_VALUE;

        for (String murdererName : murderers) {
            EntityPlayer murderer = getPlayerByName(murdererName);
            if (murderer == null || murderer.isInvisible()) continue;

            double distance = mc.thePlayer.getDistanceToEntity(murderer);
            if (distance < closestMurdererDistance) {
                closestMurdererDistance = distance;
                closestMurdererName = murdererName;
            }
        }

        // Play heartbeat sound if murderer is within warning distance
        if (heartbeatSound.isEnabled() && closestMurdererName != null && closestMurdererDistance <= warningDistance.getValue()) {
            long now = System.currentTimeMillis();
            // Calculate heartbeat interval based on distance (closer = faster)
            // At max distance: 1200ms interval, at 0 distance: 400ms interval
            double distanceRatio = closestMurdererDistance / warningDistance.getValue();
            long heartbeatInterval = (long) (400 + (800 * distanceRatio)); // 400ms to 1200ms

            if (now - lastHeartbeatTime >= heartbeatInterval) {
                lastHeartbeatTime = now;
                // Volume based on distance (closer = louder)
                float volume = (float) (1.0 - (distanceRatio * 0.6)); // 0.4 to 1.0
                // Use bass drum noteblock for heartbeat - low pitch for deep beat
                mc.thePlayer.playSound("note.bd", volume, 0.6f);
            }
        }
    }

    /**
     * Check if a tracked player (murderer/detective) should be removed
     * CONSERVATIVE - only removes if DEFINITELY disconnected
     * Keeps tracking even if player dies/spectator to maintain nametag
     */
    private boolean shouldRemoveTrackedPlayer(String name) {
        // Check tab list status - most reliable indicator
        int tabStatus = getTabListStatus(name);

        // Only remove if NOT in tab list at all (truly disconnected)
        // Don't remove spectators - they might have died but we still want to show their role
        if (tabStatus == 0) {
            // Double check - also verify entity doesn't exist
            EntityPlayer p = getPlayerByName(name);
            if (p == null) {
                return true; // Truly gone - not in tab list AND no entity
            }
            // Entity exists but not in tab list - keep tracking (lag/sync issue)
            return false;
        }

        // Player is in tab list (normal or spectator) - keep tracking
        return false;
    }

    /**
     * Check if a player should be removed from tracking (legacy method for corpses)
     * Returns true if player is: disconnected, dead, invisible, spectator, corpse, or renamed
     */
    private boolean shouldRemovePlayer(String name) {
        // If Remove Corpses is disabled, only remove disconnected players
        if (!removeCorpses.isEnabled()) {
            return getPlayerByName(name) == null;
        }

        // Check tab list status first (most reliable)
        int tabStatus = getTabListStatus(name);

        // Not in tab list = dead/disconnected
        if (tabStatus == 0) return true;

        // Spectator in tab list = dead
        if (tabStatus == 1) return true;

        // Check if name looks scrambled/random (Hypixel death replacement)
        if (name.matches("^[a-z0-9]{8,}$")) return true;

        // Now check entity-based detection
        EntityPlayer p = getPlayerByName(name);

        // Player entity not found (but was in tab list - might be corpse)
        if (p == null) return false; // Keep tracking, might just be out of render distance

        // Player is dead
        if (p.isDead || p.getHealth() <= 0) return true;

        // Player is now invisible (spectator mode in MM)
        if (p.isInvisible()) return true;

        // Check if player is a corpse (lying on ground with small bounding box)
        if (isCorpse(p)) return true;

        return false;
    }

    /**
     * Check if a player entity is a corpse (lying on the ground)
     * Detection methods:
     * 1. isPlayerSleeping() - most reliable, Murder Mystery uses "sleeping" for corpses
     * 2. Bounding box height - corpses are shorter than standing players
     */
    private boolean isCorpse(EntityPlayer player) {
        try {
            // Most reliable: player is "sleeping" (lying down) without actual bed
            // Murder Mystery uses this for corpses
            if (player.isPlayerSleeping()) {
                return true;
            }

            // Fallback: Get bounding box height
            AxisAlignedBB bb = player.getEntityBoundingBox();
            if (bb == null) return false;

            double height = bb.maxY - bb.minY;

            // Normal standing player is ~1.8 blocks tall
            // Sneaking player is ~1.65 blocks tall
            // Lying down/corpse is ~0.2-0.5 blocks tall
            // Use 1.0 as threshold to be safe
            if (height < 1.0) {
                return true;
            }

            // Alternative: check entity height directly
            // Corpses often have reduced height
            if (player.height < 1.0f) {
                return true;
            }

        } catch (Exception e) {
            // Silent fail
        }
        return false;
    }

    /**
     * Check player status in tab list
     * Returns: 0 = not found, 1 = spectator, 2 = normal player
     */
    private int getTabListStatus(String playerName) {
        try {
            if (mc.getNetHandler() == null) return 0;

            for (net.minecraft.client.network.NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                    net.minecraft.world.WorldSettings.GameType gameType = info.getGameType();
                    if (gameType == net.minecraft.world.WorldSettings.GameType.SPECTATOR) {
                        return 1; // Spectator
                    }
                    return 2; // Normal player
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        return 0; // Not in tab list
    }

    /**
     * Check if player is in spectator mode via tab list
     */
    private boolean isSpectatorInTabList(String playerName) {
        return getTabListStatus(playerName) == 1;
    }

    /**
     * Check if player is NOT in tab list at all (disconnected or dead)
     */
    private boolean isNotInTabList(String playerName) {
        return getTabListStatus(playerName) == 0;
    }

    private boolean isKnifeByName(String displayName) {
        if (displayName == null || displayName.isEmpty()) return false;

        // Strip color codes
        String stripped = displayName.replaceAll("\u00A7.", "").toLowerCase();

        for (String knifeName : KNIFE_NAMES) {
            if (stripped.contains(knifeName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMurdererWeapon(Item item) {
        return item instanceof ItemAxe ||
               item instanceof ItemHoe ||
               item instanceof ItemPickaxe ||
               item instanceof ItemFishingRod ||
               item instanceof ItemEnderPearl;
    }

    private void addMurderer(String playerName) {
        if (murderers.contains(playerName)) return;

        murderers.add(playerName);

        // Alert once
        if (!announcedMurderers.contains(playerName)) {
            announcedMurderers.add(playerName);

            if (alertMurderers.isEnabled()) {
                mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
                sendMessage("\u00A78[\u00A7cExodar\u00A78] \u00A7e" + playerName + " \u00A7fis a \u00A7c\u00A7lMURDERER\u00A7f!");
            }

            if (announceMurderer.isEnabled() && !announceMessage.getValue().isEmpty()) {
                String msg = announceMessage.getValue().replace("%s", playerName);
                mc.thePlayer.sendChatMessage(msg);
            }
        }
    }

    private void addDetective(String playerName) {
        if (detectives.contains(playerName)) return;
        if (murderers.contains(playerName)) return; // Don't mark murderers as detectives

        detectives.add(playerName);

        // Alert once
        if (!announcedDetectives.contains(playerName)) {
            announcedDetectives.add(playerName);

            if (alertMurderers.isEnabled()) {
                sendMessage("\u00A78[\u00A7cExodar\u00A78] \u00A7e" + playerName + " \u00A7fhas a \u00A7a\u00A7lBOW\u00A7f (Detective?)");
            }
        }
    }

    private EntityPlayer getPlayerByName(String name) {
        if (mc.theWorld == null) return null;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (!enabled) return;  // Check if module is enabled first!
        if (!isInGame()) return;
        if (!isInMurderMystery()) return;
        if (!renderBox.isEnabled()) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (player.isInvisible()) continue;

            String playerName = player.getName();
            int color;

            if (murderers.contains(playerName)) {
                color = 0xFFFF3333; // Red for murderers
            } else if (detectives.contains(playerName)) {
                color = 0xFF00AAFF; // Sky blue for detectives
            } else {
                color = 0xFFDDDDDD; // Light gray for innocents
            }

            drawBoxAroundEntity(player, color, event.getPartialTicks());
        }
    }

    /**
     * Render warning/murder text when murderer is detected
     * - Outside warning range: Shows "MURDER" (calm)
     * - Inside warning range: Shows "WARNING!" (pulsing, with heartbeat)
     */
    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!enabled) return;
        if (!isInGame()) return;
        if (!isInMurderMystery()) return;
        if (!warningText.isEnabled()) return;
        if (closestMurdererName == null) return;

        // Save GL state
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        int y = screenHeight / 3;
        boolean isClose = closestMurdererDistance <= warningDistance.getValue();

        if (isClose) {
            // === DANGER MODE: Within warning range ===
            // Pulsing effect
            long time = System.currentTimeMillis();
            double pulse = (Math.sin(time / 200.0) + 1) / 2; // 0 to 1
            float scale = (float) (1.5 + pulse * 0.2); // 1.5 to 1.7

            String warning = "WARNING!";
            String subText = "The murderer is near you!";
            String murdererText = closestMurdererName + " (" + (int) closestMurdererDistance + "m)";

            // Draw WARNING! with pulsing scale
            GlStateManager.pushMatrix();
            int warningWidth = mc.fontRendererObj.getStringWidth(warning);
            float warningX = (screenWidth - warningWidth * scale) / 2;
            GlStateManager.translate(warningX, y, 0);
            GlStateManager.scale(scale, scale, 1.0f);
            mc.fontRendererObj.drawStringWithShadow(warning, 0, 0, 0xFFFF3333);
            GlStateManager.popMatrix();

            // Draw sub text
            int subWidth = mc.fontRendererObj.getStringWidth(subText);
            mc.fontRendererObj.drawStringWithShadow(subText, (screenWidth - subWidth) / 2, y + 20, 0xFFFF5555);

            // Draw murderer name
            int murdererWidth = mc.fontRendererObj.getStringWidth(murdererText);
            mc.fontRendererObj.drawStringWithShadow(murdererText, (screenWidth - murdererWidth) / 2, y + 32, 0xFFFFFF55);
        } else {
            // === ALERT MODE: Murderer detected but far away ===
            String murder = "MURDER";
            String murdererText = closestMurdererName;
            String distanceText = (int) closestMurdererDistance + "m";

            // Draw MURDER (static, no pulse)
            GlStateManager.pushMatrix();
            float scale = 1.3f;
            int murderWidth = mc.fontRendererObj.getStringWidth(murder);
            float murderX = (screenWidth - murderWidth * scale) / 2;
            GlStateManager.translate(murderX, y, 0);
            GlStateManager.scale(scale, scale, 1.0f);
            mc.fontRendererObj.drawStringWithShadow(murder, 0, 0, 0xFFAA0000);
            GlStateManager.popMatrix();

            // Draw murderer name
            int nameWidth = mc.fontRendererObj.getStringWidth(murdererText);
            mc.fontRendererObj.drawStringWithShadow(murdererText, (screenWidth - nameWidth) / 2, y + 16, 0xFFFFAA00);

            // Draw distance
            int distWidth = mc.fontRendererObj.getStringWidth(distanceText);
            mc.fontRendererObj.drawStringWithShadow(distanceText, (screenWidth - distWidth) / 2, y + 28, 0xFF888888);
        }

        // Restore GL state
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    /**
     * Check if in Murder Mystery game
     * Detects by scoreboard title containing "MURDER" or "MYSTERY"
     */
    private boolean isInMurderMystery() {
        try {
            if (mc.thePlayer == null || mc.thePlayer.getWorldScoreboard() == null) return false;

            ScoreObjective sidebar = mc.thePlayer.getWorldScoreboard().getObjectiveInDisplaySlot(1);
            if (sidebar == null) return false;

            String displayName = stripColorCodes(sidebar.getDisplayName()).toUpperCase();

            // Check for Murder Mystery variations
            if (displayName.contains("MURDER") || displayName.contains("MYSTERY")) {
                // Additional check: look for "Role:" in scoreboard lines to confirm in-game
                List<String> lines = getScoreboardLines();
                for (String line : lines) {
                    String stripped = stripColorCodes(line).toUpperCase();
                    if (stripped.contains("ROLE:") || stripped.contains("ROL:")) {
                        return true;
                    }
                }
                // If no role line, still return true if title matches (lobby detection)
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }

        return false;
    }

    private List<String> getScoreboardLines() {
        List<String> lines = new ArrayList<>();
        try {
            if (mc.thePlayer.getWorldScoreboard() == null) return lines;
            ScoreObjective sidebar = mc.thePlayer.getWorldScoreboard().getObjectiveInDisplaySlot(1);
            if (sidebar == null) return lines;

            Collection<Score> scores = mc.thePlayer.getWorldScoreboard().getSortedScores(sidebar);
            for (Score score : scores) {
                String playerName = score.getPlayerName();
                if (playerName != null && !playerName.startsWith("#")) {
                    ScorePlayerTeam team = mc.thePlayer.getWorldScoreboard().getPlayersTeam(playerName);
                    String line = ScorePlayerTeam.formatPlayerName(team, playerName);
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return lines;
    }

    private String stripColorCodes(String text) {
        return text.replaceAll("\u00A7.", "").trim();
    }

    private void sendMessage(String msg) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(msg));
        }
    }

    private void drawBoxAroundEntity(EntityPlayer entity, int color, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        x -= mc.getRenderManager().viewerPosX;
        y -= mc.getRenderManager().viewerPosY;
        z -= mc.getRenderManager().viewerPosZ;

        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        double width = entity.width / 2.0;
        double height = entity.height;

        AxisAlignedBB box = new AxisAlignedBB(
                x - width, y, z - width,
                x + width, y + height, z + width
        );

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();

        GL11.glLineWidth(2.0F);
        GlStateManager.color(r, g, b, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        // Draw box outline
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        tessellator.draw();

        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        tessellator.draw();

        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        worldRenderer.pos(box.minX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.minZ).endVertex();
        worldRenderer.pos(box.maxX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.maxX, box.maxY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.minY, box.maxZ).endVertex();
        worldRenderer.pos(box.minX, box.maxY, box.maxZ).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // ==================== PUBLIC API FOR NAMETAGS ====================

    /**
     * Check if a player is a detected murderer
     */
    public static boolean isMurderer(String playerName) {
        if (instance == null || !instance.enabled) return false;
        return instance.murderers.contains(playerName);
    }

    /**
     * Check if a player is a detected detective
     */
    public static boolean isDetective(String playerName) {
        if (instance == null || !instance.enabled) return false;
        return instance.detectives.contains(playerName);
    }

    /**
     * Check if currently in a Murder Mystery game
     */
    public static boolean isInMurderMysteryGame() {
        if (instance == null || !instance.enabled) return false;
        return instance.isInMurderMystery();
    }

    /**
     * Get the prefix for a player's nametag
     * @return prefix string or null if none
     */
    public static String getNametagPrefix(String playerName) {
        if (instance == null || !instance.enabled) return null;

        // Don't re-check isInMurderMystery() - if they're in the list, show the tag
        // This fixes the tag not appearing when scoreboard detection is flaky
        if (instance.murderers.contains(playerName)) {
            // Use §4 (dark red) if server uses §c for player names, otherwise use §c
            if (instance.serverUsesRedNames) {
                return "\u00A74\u00A7lMURDER \u00A7r";  // Dark red
            } else {
                return "\u00A7c\u00A7lMURDER \u00A7r";  // Light red
            }
        }
        if (instance.detectives.contains(playerName)) {
            return "\u00A79\u00A7lDETECTIVE \u00A7r";
        }
        return null;
    }

    /**
     * Get the color for a player for ESP/visuals
     * @return color int or -1 if no override (use default)
     */
    public static int getESPColor(String playerName) {
        if (instance == null || !instance.enabled) return -1;

        // Don't re-check isInMurderMystery() - if they're in the list, use the color
        if (instance.murderers.contains(playerName)) {
            // Use dark red if server uses red names, otherwise light red
            if (instance.serverUsesRedNames) {
                return 0xFFAA0000;  // Dark red (§4)
            } else {
                return 0xFFFF5555;  // Light red (§c)
            }
        }
        if (instance.detectives.contains(playerName)) {
            // Sky blue for detectives (different from friends cyan 0xFF55FFFF)
            return 0xFF00AAFF;
        }

        // Only return innocent color if we're actually in Murder Mystery
        if (!instance.isInMurderMystery()) return -1;

        // Light gray (almost white) for innocents
        return 0xFFDDDDDD;
    }

    /**
     * Get the role of a player
     * @return "MURDERER", "DETECTIVE", "INNOCENT", or null if not in MM game
     */
    public static String getRole(String playerName) {
        if (instance == null || !instance.enabled) return null;
        if (!instance.isInMurderMystery()) return null;

        if (instance.murderers.contains(playerName)) {
            return "MURDERER";
        }
        if (instance.detectives.contains(playerName)) {
            return "DETECTIVE";
        }
        return "INNOCENT";
    }

    public static MurderMystery getInstance() {
        return instance;
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
