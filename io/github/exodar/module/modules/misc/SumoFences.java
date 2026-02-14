/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.ModeSetting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * SumoFences - Places client-side fences around Hypixel Sumo arena
 *
 * Prevents falling off the edge by placing visual barriers.
 */
public class SumoFences extends Module {

    private final SliderSetting fenceHeight;
    private final ModeSetting blockType;

    private Timer timer;
    private IBlockState fenceState;

    // Hypixel Sumo arena fence positions
    private static final List<BlockPos> FENCE_POSITIONS = Arrays.asList(
            new BlockPos(9, 65, -2), new BlockPos(9, 65, -1), new BlockPos(9, 65, 0),
            new BlockPos(9, 65, 1), new BlockPos(9, 65, 2), new BlockPos(9, 65, 3),
            new BlockPos(8, 65, 3), new BlockPos(8, 65, 4), new BlockPos(8, 65, 5),
            new BlockPos(7, 65, 5), new BlockPos(7, 65, 6), new BlockPos(7, 65, 7),
            new BlockPos(6, 65, 7), new BlockPos(5, 65, 7), new BlockPos(5, 65, 8),
            new BlockPos(4, 65, 8), new BlockPos(3, 65, 8), new BlockPos(3, 65, 9),
            new BlockPos(2, 65, 9), new BlockPos(1, 65, 9), new BlockPos(0, 65, 9),
            new BlockPos(-1, 65, 9), new BlockPos(-2, 65, 9), new BlockPos(-3, 65, 9),
            new BlockPos(-3, 65, 8), new BlockPos(-4, 65, 8), new BlockPos(-5, 65, 8),
            new BlockPos(-5, 65, 7), new BlockPos(-6, 65, 7), new BlockPos(-7, 65, 7),
            new BlockPos(-7, 65, 6), new BlockPos(-7, 65, 5), new BlockPos(-8, 65, 5),
            new BlockPos(-8, 65, 4), new BlockPos(-8, 65, 3), new BlockPos(-9, 65, 3),
            new BlockPos(-9, 65, 2), new BlockPos(-9, 65, 1), new BlockPos(-9, 65, 0),
            new BlockPos(-9, 65, -1), new BlockPos(-9, 65, -2), new BlockPos(-9, 65, -3),
            new BlockPos(-8, 65, -3), new BlockPos(-8, 65, -4), new BlockPos(-8, 65, -5),
            new BlockPos(-7, 65, -5), new BlockPos(-7, 65, -6), new BlockPos(-7, 65, -7),
            new BlockPos(-6, 65, -7), new BlockPos(-5, 65, -7), new BlockPos(-5, 65, -8),
            new BlockPos(-4, 65, -8), new BlockPos(-3, 65, -8), new BlockPos(-3, 65, -9),
            new BlockPos(-2, 65, -9), new BlockPos(-1, 65, -9), new BlockPos(0, 65, -9),
            new BlockPos(1, 65, -9), new BlockPos(2, 65, -9), new BlockPos(3, 65, -9),
            new BlockPos(3, 65, -8), new BlockPos(4, 65, -8), new BlockPos(5, 65, -8),
            new BlockPos(5, 65, -7), new BlockPos(6, 65, -7), new BlockPos(7, 65, -7),
            new BlockPos(7, 65, -6), new BlockPos(7, 65, -5), new BlockPos(8, 65, -5),
            new BlockPos(8, 65, -4), new BlockPos(8, 65, -3), new BlockPos(9, 65, -3)
    );

    // Sumo map names
    private static final List<String> SUMO_MAPS = Arrays.asList("Sumo", "Space Mine", "White Crystal");

    public SumoFences() {
        super("SumoFences", ModuleCategory.MISC);
        this.registerSetting(new DescriptionSetting("Fences for Hypixel Sumo"));
        this.registerSetting(fenceHeight = new SliderSetting("Fence Height", 4.0, 1.0, 16.0, 1.0));
        this.registerSetting(blockType = new ModeSetting("Block Type", new String[]{"Fence", "Leaves", "Glass", "Barrier"}));

        fenceState = Blocks.oak_fence.getDefaultState();
    }

    @Override
    public void onEnable() {
        updateBlockType();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isInSumoGame()) {
                    placeFences();
                }
            }
        }, 0L, 500L);
    }

    @Override
    public void onDisable() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        removeFences();
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;

        // Block interaction with fence positions
        if (mc.objectMouseOver != null &&
                mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int x = mc.objectMouseOver.getBlockPos().getX();
            int z = mc.objectMouseOver.getBlockPos().getZ();

            for (BlockPos pos : FENCE_POSITIONS) {
                if (pos.getX() == x && pos.getZ() == z) {
                    // Cancel attack/use on fence positions
                    // This prevents accidental breaking
                    break;
                }
            }
        }
    }

    private void updateBlockType() {
        switch (blockType.getCurrentIndex()) {
            case 0:
                fenceState = Blocks.oak_fence.getDefaultState();
                break;
            case 1:
                fenceState = Blocks.leaves.getDefaultState();
                break;
            case 2:
                fenceState = Blocks.glass.getDefaultState();
                break;
            case 3:
                fenceState = Blocks.barrier.getDefaultState();
                break;
        }
    }

    private void placeFences() {
        if (mc.theWorld == null) return;

        updateBlockType();
        int height = (int) fenceHeight.getValue();
        int yOffset = getYOffset();

        for (BlockPos pos : FENCE_POSITIONS) {
            for (int i = 0; i < height; i++) {
                BlockPos fencePos = new BlockPos(pos.getX(), pos.getY() + i + yOffset, pos.getZ());
                if (mc.theWorld.getBlockState(fencePos).getBlock() == Blocks.air) {
                    mc.theWorld.setBlockState(fencePos, fenceState);
                }
            }
        }
    }

    private void removeFences() {
        if (mc.theWorld == null) return;

        int height = (int) fenceHeight.getValue();

        for (BlockPos pos : FENCE_POSITIONS) {
            for (int i = 0; i < height; i++) {
                BlockPos fencePos = new BlockPos(pos.getX(), pos.getY() + i, pos.getZ());
                if (mc.theWorld.getBlockState(fencePos).getBlock() == fenceState.getBlock()) {
                    mc.theWorld.setBlockState(fencePos, Blocks.air.getDefaultState());
                }
            }
        }
    }

    private boolean isInSumoGame() {
        if (!isInGame()) return false;
        if (!isHypixel()) return false;

        // Check scoreboard for Sumo indicators
        try {
            if (mc.thePlayer.getWorldScoreboard() == null) return false;

            List<String> scoreboard = getScoreboardLines();
            for (String line : scoreboard) {
                String clean = stripColorCodes(line);

                // Check for Sumo Duel mode
                if (clean.equals("Mode: Sumo Duel")) {
                    return true;
                }

                // Check for Sumo map names
                if (clean.startsWith("Map:")) {
                    String mapName = clean.substring(4).trim();
                    if (SUMO_MAPS.contains(mapName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }

        return false;
    }

    private int getYOffset() {
        // Some maps have different Y levels
        try {
            List<String> scoreboard = getScoreboardLines();
            for (String line : scoreboard) {
                if (stripColorCodes(line).contains("Fort Royale")) {
                    return 7;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    private boolean isHypixel() {
        if (mc.getCurrentServerData() == null) return false;
        String ip = mc.getCurrentServerData().serverIP.toLowerCase();
        return ip.contains("hypixel");
    }

    private List<String> getScoreboardLines() {
        List<String> lines = new java.util.ArrayList<>();
        try {
            if (mc.thePlayer.getWorldScoreboard() == null) return lines;
            net.minecraft.scoreboard.ScoreObjective sidebar = mc.thePlayer.getWorldScoreboard().getObjectiveInDisplaySlot(1);
            if (sidebar == null) return lines;

            java.util.Collection<net.minecraft.scoreboard.Score> scores = mc.thePlayer.getWorldScoreboard().getSortedScores(sidebar);
            for (net.minecraft.scoreboard.Score score : scores) {
                String playerName = score.getPlayerName();
                if (playerName != null && !playerName.startsWith("#")) {
                    net.minecraft.scoreboard.ScorePlayerTeam team = mc.thePlayer.getWorldScoreboard().getPlayersTeam(playerName);
                    String line = net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName(team, playerName);
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

    @Override
    public String getDisplaySuffix() {
        return blockType.getSelected();
    }
}
