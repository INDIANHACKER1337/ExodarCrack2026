/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HackerDetector - Detects suspicious player behavior (cheats)
 * NoSlow, Speed, AutoBlock detection
 * Improved detection to reduce false positives
 */
public class HackerDetector extends Module {

    // Detection toggles
    private final TickSetting detectNoSlow;
    private final TickSetting detectSpeed;
    private final TickSetting detectAutoBlock;
    private final TickSetting playSound;

    // Tracking data
    private final Map<String, PlayerData> playerData = new HashMap<>();
    private final Set<String> alertedNoSlow = new HashSet<>();
    private final Set<String> alertedSpeed = new HashSet<>();
    private final Set<String> alertedAutoBlock = new HashSet<>();

    public HackerDetector() {
        super("HackerDetector", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Detects cheaters"));
        this.registerSetting(detectNoSlow = new TickSetting("Detect NoSlow", true));
        this.registerSetting(detectSpeed = new TickSetting("Detect Speed", true));
        this.registerSetting(detectAutoBlock = new TickSetting("Detect AutoBlock", true));
        this.registerSetting(playSound = new TickSetting("Play Sound", true));
    }

    @Override
    public void onEnable() {
        playerData.clear();
        alertedNoSlow.clear();
        alertedSpeed.clear();
        alertedAutoBlock.clear();
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (player.isDead) continue;

            String name = player.getName();
            PlayerData data = playerData.computeIfAbsent(name, k -> new PlayerData());

            // Check for NoSlow
            if (detectNoSlow.isEnabled()) {
                checkNoSlow(player, name, data, now);
            }

            // Check for Speed hack
            if (detectSpeed.isEnabled()) {
                checkSpeed(player, name, data, now);
            }

            // Check for AutoBlock
            if (detectAutoBlock.isEnabled()) {
                checkAutoBlock(player, name, data, now);
            }

            // Update tracking data
            data.lastPosX = player.posX;
            data.lastPosZ = player.posZ;
            data.lastPosY = player.posY;
            data.wasBlocking = player.isBlocking();
            data.wasSwinging = player.isSwingInProgress;
            data.lastHurtTime = player.hurtTime;
            data.lastUpdate = now;
        }

        // Cleanup old player data
        playerData.entrySet().removeIf(e -> now - e.getValue().lastUpdate > 60000);
    }

    private void checkNoSlow(EntityPlayer player, String name, PlayerData data, long now) {
        // Check if player is using item (eating, blocking, bow, etc)
        if (!player.isUsingItem()) {
            data.usingItemTicks = 0;
            data.noSlowViolations = 0;
            return;
        }

        // Ignore if player just got hit (knockback causes speed)
        if (player.hurtTime > 0) {
            data.usingItemTicks = 0;
            return;
        }

        // Ignore if player is falling/jumping (Y velocity)
        double dy = Math.abs(player.posY - data.lastPosY);
        if (dy > 0.1 || !player.onGround) {
            data.usingItemTicks = 0;
            return;
        }

        data.usingItemTicks++;

        // Need at least 10 ticks of consistent using item to check (more lenient)
        if (data.usingItemTicks < 10) return;

        // Calculate horizontal speed
        double dx = player.posX - data.lastPosX;
        double dz = player.posZ - data.lastPosZ;
        double speed = Math.sqrt(dx * dx + dz * dz);

        // Normal speed while using item is ~0.043 (80% slowdown from 0.215)
        // Using 0.18 threshold (higher to reduce false positives)
        // Also check for speed potion
        boolean hasSpeed = player.isPotionActive(Potion.moveSpeed);
        double threshold = hasSpeed ? 0.25 : 0.18;

        if (speed > threshold) {
            data.noSlowViolations++;
            // Need 5 consecutive violations to flag
            if (data.noSlowViolations >= 5 && !alertedNoSlow.contains(name)) {
                alertFlag(player.getDisplayName().getFormattedText(), "NoSlow");
                alertedNoSlow.add(name);
            }
        } else {
            // Decay violations slowly
            data.noSlowViolations = Math.max(0, data.noSlowViolations - 1);
        }
    }

    private void checkSpeed(EntityPlayer player, String name, PlayerData data, long now) {
        // Skip if player has speed potion
        if (player.isPotionActive(Potion.moveSpeed)) return;

        // Skip if player just got hit
        if (player.hurtTime > 0) return;

        // Calculate horizontal speed
        double dx = player.posX - data.lastPosX;
        double dz = player.posZ - data.lastPosZ;
        double speed = Math.sqrt(dx * dx + dz * dz);

        // Normal sprint speed is ~0.28, anything above 0.45 is suspicious
        if (speed > 0.45 && player.onGround) {
            data.speedViolations++;
            if (data.speedViolations >= 8 && !alertedSpeed.contains(name)) {
                alertFlag(player.getDisplayName().getFormattedText(), "Speed");
                alertedSpeed.add(name);
            }
        } else {
            data.speedViolations = Math.max(0, data.speedViolations - 1);
        }
    }

    private void checkAutoBlock(EntityPlayer player, String name, PlayerData data, long now) {
        // AutoBlock detection: player is blocking AND swinging at the EXACT same time
        // This is impossible without cheats - you can't swing while holding block
        boolean isBlocking = player.isBlocking();
        boolean isSwinging = player.isSwingInProgress;

        // Key insight: a legit player can block AFTER a swing finishes,
        // but cannot be in blocking state WHILE swing animation is active
        // Swing animation lasts about 6 ticks

        // Track swing state changes
        if (isSwinging && !data.wasSwinging) {
            // Just started swinging
            data.swingStartTick = data.ticksTracked;
        }

        // Only flag if blocking DURING active swing (first 4 ticks of swing)
        // Not when transitioning between states
        boolean inActiveSwing = isSwinging && (data.ticksTracked - data.swingStartTick) < 4;

        if (isBlocking && inActiveSwing) {
            // Blocking during active swing - highly suspicious
            data.autoBlockViolations++;
        } else {
            // Decay violations
            data.autoBlockViolations = Math.max(0, data.autoBlockViolations - 1);
        }

        data.ticksTracked++;

        // Need 6 consecutive violations to flag (stricter)
        if (data.autoBlockViolations >= 6 && !alertedAutoBlock.contains(name)) {
            alertFlag(player.getDisplayName().getFormattedText(), "AutoBlock");
            alertedAutoBlock.add(name);
        }
    }

    private void alertFlag(String displayName, String flagName) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                "\u00a78[\u00a7dE\u00a78]\u00a7r " + displayName + " \u00a77flags \u00a7b" + flagName
            ));

            // Play alert sound
            if (playSound.isEnabled()) {
                mc.thePlayer.playSound("note.pling", 1.0f, 1.5f);
            }
        }
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }

    private static class PlayerData {
        double lastPosX;
        double lastPosZ;
        double lastPosY;
        long lastUpdate;
        int usingItemTicks;
        int noSlowViolations;
        int speedViolations;
        int autoBlockViolations;
        boolean wasBlocking;
        boolean wasSwinging;
        int lastHurtTime;
        int ticksTracked;
        int swingStartTick;
    }
}
