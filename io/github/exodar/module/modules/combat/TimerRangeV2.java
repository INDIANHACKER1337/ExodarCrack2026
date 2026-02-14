/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.utils.TimeHelper;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.setting.SliderSetting;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import java.util.ArrayList;

/**
 * TimerRangeV2 - Port from Augustus b2.6
 *
 * Tracks packet timing balance to detect lag advantage
 * - Balance increases when sending packets fast
 * - Balance decreases when sending packets slow
 * - Position corrections penalize balance
 */
public class TimerRangeV2 extends Module {
    // Settings
    private final TickSetting enableTracking;
    private final SliderSetting balancePenalty;
    private final TickSetting showDebug;
    private final SliderSetting debugInterval;

    // Port from Augustus - exact same fields
    private final TimeHelper timeHelper;
    private final TimeHelper timeHelper2;
    private final ArrayList<Integer> diffs;
    public long balanceCounter;
    private long lastTime;
    private WorldClient lastWorld;

    private int updateCounter = 0;

    public TimerRangeV2() {
        super("TimerRangeV2", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Augustus balance tracker"));

        // Settings
        this.registerSetting(enableTracking = new TickSetting("Enable tracking", true));
        this.registerSetting(balancePenalty = new SliderSetting("Position penalty", 100, 0, 500, 10));
        this.registerSetting(showDebug = new TickSetting("Show debug", false));
        this.registerSetting(debugInterval = new SliderSetting("Debug interval", 100, 20, 200, 20));

        // Initialize exactly like Augustus
        this.timeHelper = new TimeHelper();
        this.timeHelper2 = new TimeHelper();
        this.diffs = new ArrayList<Integer>();
        this.balanceCounter = 0L;
        this.lastWorld = null;
        this.lastTime = System.currentTimeMillis();
    }

    @Override
    public void onEnable() {
        // Reset state on enable
        this.balanceCounter = 0L;
        this.diffs.clear();
        this.lastTime = System.currentTimeMillis();
        this.lastWorld = null;
        this.updateCounter = 0;
        System.out.println("[TimerRangeV2] Enabled - Balance tracking active");
    }

    @Override
    public void onDisable() {
        if (showDebug.isEnabled()) {
            System.out.println("[TimerRangeV2] Disabled");
            System.out.println("[TimerRangeV2] Final Balance: " + balanceCounter);
            System.out.println("[TimerRangeV2] Diffs collected: " + diffs.size());
            if (!diffs.isEmpty()) {
                System.out.println("[TimerRangeV2] Avg diff: " + getAverageDiff() + "ms");
            }
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled || !enableTracking.isEnabled()) return;
        updateCounter++;

        // Track timing every update
        processPacketTiming();

        // Debug output
        if (showDebug.isEnabled() && updateCounter % (int)debugInterval.getValue() == 0) {
            System.out.println("[TimerRangeV2] Balance: " + balanceCounter +
                             " | Avg: " + getAverageDiff() + "ms");
        }
    }

    /**
     * Augustus @EventTarget onEventSendPacket
     * NOW USING EXODAR'S PACKET HOOK SYSTEM
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled || !enableTracking.isEnabled()) return true;

        // Only track movement packets (C03PacketPlayer)
        if (!(packet instanceof C03PacketPlayer)) return true;

        WorldClient currentWorld = getWorld();

        // Check world change (Augustus line 47-50)
        if (this.lastWorld != null && this.lastWorld != currentWorld) {
            this.balanceCounter = 0L;
            this.diffs.clear();
            if (showDebug.isEnabled()) {
                System.out.println("[TimerRangeV2] World changed - Reset balance");
            }
        }

        // Decrement balance if positive (Augustus line 51-53)
        if (this.balanceCounter > 0L) {
            --this.balanceCounter;
        }

        // Calculate time diff (Augustus line 54-56)
        final long diff = System.currentTimeMillis() - this.lastTime;
        this.diffs.add((int)diff);

        // CORE AUGUSTUS LOGIC (line 56):
        // balanceCounter += (diff - 50L) * -3L;
        //
        // If diff < 50ms (packets fast): (diff-50) negative → *-3 = positive → balance UP
        // If diff > 50ms (packets slow): (diff-50) positive → *-3 = negative → balance DOWN
        this.balanceCounter += (diff - 50L) * -3L;

        this.lastTime = System.currentTimeMillis();

        // Augustus empty if block (line 58)
        if (this.balanceCounter > 150L) {
            // Original Augustus: empty block
        }

        this.lastWorld = currentWorld;

        return true; // Don't cancel packet
    }

    /**
     * Augustus @EventTarget onEventReadPacket
     * NOW USING EXODAR'S PACKET HOOK SYSTEM
     */
    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled || !enableTracking.isEnabled()) return true;

        // Check if packet is S08PacketPlayerPosLook (position correction from server)
        if (packet instanceof S08PacketPlayerPosLook) {
            // Augustus subtracts from balance when server corrects position
            this.balanceCounter -= (long)balancePenalty.getValue();
            if (showDebug.isEnabled()) {
                System.out.println("[TimerRangeV2] Position correction! Balance: " + balanceCounter);
            }
        }

        return true; // Don't cancel packet
    }

    /**
     * Process packet timing - called from onUpdate
     */
    private void processPacketTiming() {
        final long diff = System.currentTimeMillis() - this.lastTime;

        // Only track if significant time passed (avoid spam)
        if (diff < 10) return;

        // Track timing even without actual packets
        this.diffs.add((int)diff);
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * Calculate average packet diff for analysis
     */
    private long getAverageDiff() {
        if (diffs.isEmpty()) return 0;
        long sum = 0;
        for (int diff : diffs) {
            sum += diff;
        }
        return sum / diffs.size();
    }

    @Override
    public String getDisplaySuffix() {
        // Show balance in suffix with color coding
        String color = "§7";
        if (balanceCounter > 100) color = "§a"; // Green = good balance
        else if (balanceCounter < -100) color = "§c"; // Red = bad balance

        return " " + color + balanceCounter;
    }
}
