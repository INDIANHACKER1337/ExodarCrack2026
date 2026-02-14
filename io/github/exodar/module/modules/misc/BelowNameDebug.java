/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.event.Subscribe;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * BelowNameDebug - Detects belowname scoreboard health and prints changes to chat
 */
public class BelowNameDebug extends Module {

    // Track last known health for each player
    private Map<String, Integer> lastHealthMap = new HashMap<>();
    private long lastDebugPrint = 0;

    public BelowNameDebug() {
        super("BelowNameDebug", ModuleCategory.MISC);
        this.hidden = true; // Hidden from ClickGUI
        this.registerSetting(new DescriptionSetting("Prints belowname health to chat"));
    }

    @Override
    public void onEnable() {
        lastHealthMap.clear();
        lastDebugPrint = 0;
        sendChat(EnumChatFormatting.GREEN + "[BelowNameDebug] Enabled - monitoring belowname health");
    }

    @Override
    public void onDisable() {
        lastHealthMap.clear();
    }

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!enabled) return;
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return;

        // Get the belowName objective (display slot 2)
        ScoreObjective belowNameObjective = scoreboard.getObjectiveInDisplaySlot(2);

        // Every 3 seconds, print debug info about the scoreboard
        long now = System.currentTimeMillis();
        if (now - lastDebugPrint > 3000) {
            lastDebugPrint = now;
            printScoreboardDebug(scoreboard, belowNameObjective);
        }

        if (belowNameObjective == null) return;

        // Get ALL scores for this objective
        Collection<Score> allScores = scoreboard.getSortedScores(belowNameObjective);

        // Check all players in the world
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;

            String playerName = player.getName();

            // Try multiple ways to find the score
            int belowNameHealth = findPlayerScore(scoreboard, player, belowNameObjective, allScores);

            if (belowNameHealth > 0) {
                Integer lastHealth = lastHealthMap.get(playerName);

                if (lastHealth == null) {
                    // First time seeing this player
                    lastHealthMap.put(playerName, belowNameHealth);
                    sendChat(EnumChatFormatting.AQUA + "[BelowName] " +
                            EnumChatFormatting.WHITE + playerName +
                            EnumChatFormatting.GRAY + " detected with " +
                            EnumChatFormatting.RED + belowNameHealth +
                            getHealthSuffix(belowNameObjective));
                } else if (belowNameHealth != lastHealth) {
                    // Health changed
                    int diff = belowNameHealth - lastHealth;
                    String diffStr = diff > 0 ? "+" + diff : String.valueOf(diff);
                    EnumChatFormatting diffColor = diff > 0 ? EnumChatFormatting.GREEN : EnumChatFormatting.RED;

                    sendChat(EnumChatFormatting.AQUA + "[BelowName] " +
                            EnumChatFormatting.WHITE + playerName +
                            EnumChatFormatting.GRAY + ": " +
                            EnumChatFormatting.YELLOW + lastHealth +
                            EnumChatFormatting.GRAY + " -> " +
                            EnumChatFormatting.RED + belowNameHealth +
                            EnumChatFormatting.GRAY + " (" + diffColor + diffStr + EnumChatFormatting.GRAY + ")" +
                            getHealthSuffix(belowNameObjective));

                    lastHealthMap.put(playerName, belowNameHealth);
                }
            }
        }

        // Clean up players no longer in world
        lastHealthMap.keySet().removeIf(name -> {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player.getName().equals(name)) return false;
            }
            return true;
        });

        // Also render debug on screen
        renderDebugOnScreen(scoreboard, belowNameObjective);
    }

    /**
     * Print detailed scoreboard debug info to chat
     */
    private void printScoreboardDebug(Scoreboard scoreboard, ScoreObjective belowNameObjective) {
        sendChat(EnumChatFormatting.GOLD + "=== Scoreboard Debug ===");

        if (belowNameObjective == null) {
            sendChat(EnumChatFormatting.RED + "No belowName objective (slot 2) found!");

            // List all objectives
            Collection<ScoreObjective> allObjectives = scoreboard.getScoreObjectives();
            sendChat(EnumChatFormatting.YELLOW + "All objectives (" + allObjectives.size() + "):");
            for (ScoreObjective obj : allObjectives) {
                String displaySlot = "none";
                for (int i = 0; i < 3; i++) {
                    if (scoreboard.getObjectiveInDisplaySlot(i) == obj) {
                        displaySlot = String.valueOf(i);
                        break;
                    }
                }
                sendChat(EnumChatFormatting.GRAY + "  - " + obj.getName() + " (" + obj.getDisplayName() + ") slot=" + displaySlot);
            }
            return;
        }

        sendChat(EnumChatFormatting.GREEN + "BelowName objective: " + belowNameObjective.getName());
        sendChat(EnumChatFormatting.GREEN + "Display name: " + belowNameObjective.getDisplayName());

        // List ALL scores in this objective
        Collection<Score> scores = scoreboard.getSortedScores(belowNameObjective);
        sendChat(EnumChatFormatting.YELLOW + "Scores in objective (" + scores.size() + "):");

        int count = 0;
        for (Score score : scores) {
            if (count >= 10) {
                sendChat(EnumChatFormatting.GRAY + "  ... and " + (scores.size() - 10) + " more");
                break;
            }
            String scoreName = score.getPlayerName();
            int points = score.getScorePoints();
            // Show raw name with escapes for color codes
            String rawName = scoreName.replace("\u00a7", "&");
            sendChat(EnumChatFormatting.GRAY + "  [" + rawName + "] = " + points);
            count++;
        }

        // List players in world for comparison
        sendChat(EnumChatFormatting.YELLOW + "Players in world:");
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            String name = player.getName();
            String displayName = player.getDisplayName().getUnformattedText();
            String formattedName = player.getDisplayName().getFormattedText().replace("\u00a7", "&");

            // Also get team formatted name
            String teamName = "none";
            ScorePlayerTeam team = scoreboard.getPlayersTeam(name);
            if (team != null) {
                teamName = ScorePlayerTeam.formatPlayerName(team, name).replace("\u00a7", "&");
            }

            sendChat(EnumChatFormatting.GRAY + "  " + name + " | display=" + formattedName + " | team=" + teamName);
        }
    }

    /**
     * Try multiple ways to find a player's score
     */
    private int findPlayerScore(Scoreboard scoreboard, EntityPlayer player, ScoreObjective objective, Collection<Score> allScores) {
        String playerName = player.getName();

        // Method 1: Direct lookup by player name
        if (scoreboard.entityHasObjective(playerName, objective)) {
            Score score = scoreboard.getValueFromObjective(playerName, objective);
            if (score != null) {
                return score.getScorePoints();
            }
        }

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

        return -1;
    }

    /**
     * Strip Minecraft color codes from a string
     */
    private String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    /**
     * Render debug info on screen
     */
    private void renderDebugOnScreen(Scoreboard scoreboard, ScoreObjective belowNameObjective) {
        ScaledResolution sr = new ScaledResolution(mc);
        int y = 60;
        int x = 10;

        mc.fontRendererObj.drawStringWithShadow("=== BelowName Debug ===", x, y, 0xFFFF55);
        y += 12;

        if (belowNameObjective == null) {
            mc.fontRendererObj.drawStringWithShadow("No belowName objective active", x, y, 0xFF5555);
            return;
        }

        mc.fontRendererObj.drawStringWithShadow("Objective: " + belowNameObjective.getName(), x, y, 0x55FF55);
        y += 10;
        mc.fontRendererObj.drawStringWithShadow("Display: " + belowNameObjective.getDisplayName(), x, y, 0x55FF55);
        y += 12;

        // Show scores
        Collection<Score> scores = scoreboard.getSortedScores(belowNameObjective);
        mc.fontRendererObj.drawStringWithShadow("Scores (" + scores.size() + "):", x, y, 0xFFFF55);
        y += 10;

        int count = 0;
        for (Score score : scores) {
            if (count >= 8) break;
            String name = score.getPlayerName().replace("\u00a7", "&");
            if (name.length() > 20) name = name.substring(0, 20) + "...";
            mc.fontRendererObj.drawStringWithShadow(name + " = " + score.getScorePoints(), x + 5, y, 0xAAAAAA);
            y += 9;
            count++;
        }
    }

    /**
     * Get the suffix from the objective display name (e.g., " ❤")
     */
    private String getHealthSuffix(ScoreObjective objective) {
        String displayName = objective.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            return " " + displayName;
        }
        return "";
    }

    private void sendChat(String message) {
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
