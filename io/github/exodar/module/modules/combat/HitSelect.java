/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C02PacketUseEntity;

/**
 * HitSelect - Chooses the best time to hit (Based on Raven XD)
 *
 * Modes:
 * - Single: 1 hit, then wait for conditions
 * - Burst: Multiple hits, then wait for conditions
 *
 * Preferences:
 * - Move Speed: Uses delay as main mechanic (keeps sprint)
 * - KB Reduction: Only hit when hurt + in air + moving
 * - Critical Hits: Only hit when falling
 *
 * Delay: Fallback - if conditions not met for X ms, allow attack anyway
 */
public class HitSelect extends Module {

    private static HitSelect instance;

    // Settings
    private ModeSetting mode;
    private ModeSetting preference;
    private SliderSetting burstHits;
    private SliderSetting delay;
    private SliderSetting chance;
    private io.github.exodar.setting.TickSetting disableDuringKB;

    // State
    private static volatile boolean currentShouldAttack = false;
    private static volatile long lastAttackTime = -1;
    private static volatile int burstCounter = 0;
    private static volatile boolean burstActive = false;

    public HitSelect() {
        super("HitSelect", ModuleCategory.COMBAT);
        instance = this;

        this.registerSetting(new DescriptionSetting("Best time to hit"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Single", "Burst"})
            .onChange(this::updateSettingVisibility));
        this.registerSetting(preference = new ModeSetting("Preference", new String[]{"Move Speed", "KB Reduction", "Critical Hits"}));
        this.registerSetting(burstHits = new SliderSetting("Burst Hits", 3.0, 2.0, 10.0, 1.0));
        this.registerSetting(delay = new SliderSetting("Delay", 420.0, 300.0, 500.0, 1.0));
        this.registerSetting(chance = new SliderSetting("Chance %", 80.0, 0.0, 100.0, 1.0));
        this.registerSetting(disableDuringKB = new io.github.exodar.setting.TickSetting("Disable During KB", false));

        updateSettingVisibility();
    }

    private void updateSettingVisibility() {
        boolean isBurst = mode.getSelected().equals("Burst");
        burstHits.setVisible(isBurst);
    }

    @Override
    public void onEnable() {
        currentShouldAttack = false;
        lastAttackTime = -1;
        burstCounter = 0;
        burstActive = false;
    }

    @Override
    public void onDisable() {
        currentShouldAttack = true;
        burstCounter = 0;
        burstActive = false;
    }

    @Override
    public void onUpdate() {
        updateSettingVisibility();

        if (!isInGame()) {
            currentShouldAttack = true;
            return;
        }

        EntityPlayerSP player = getPlayer();
        if (player == null) {
            currentShouldAttack = true;
            return;
        }

        // Disable during knockback - allow free attacks when taking KB
        if (disableDuringKB.isEnabled() && player.hurtTime > 0) {
            currentShouldAttack = true;
            return;
        }

        String selectedMode = mode.getSelected();

        // ============ BURST MODE ============
        if (selectedMode.equals("Burst")) {
            // If burst is active, allow attacks until burst count reached
            if (burstActive) {
                currentShouldAttack = true;
                return;
            }

            // Burst not active - check if conditions are met to start a new burst
            currentShouldAttack = false;

            // Chance check
            if (Math.random() * 100.0 > chance.getValue()) {
                startBurst();
                return;
            }

            // Check preference conditions
            if (checkPreferenceConditions(player)) {
                startBurst();
                return;
            }

            // Delay fallback
            long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
            if (lastAttackTime == -1 || timeSinceLastAttack >= delay.getValue()) {
                startBurst();
            }
            return;
        }

        // ============ SINGLE MODE ============
        currentShouldAttack = false;

        // Chance check - random chance to bypass all conditions
        if (Math.random() * 100.0 > chance.getValue()) {
            currentShouldAttack = true;
            return;
        }

        // Check preference conditions
        if (checkPreferenceConditions(player)) {
            currentShouldAttack = true;
            return;
        }

        // Delay fallback - if conditions not met, check if enough time passed
        long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
        if (lastAttackTime == -1 || timeSinceLastAttack >= delay.getValue()) {
            currentShouldAttack = true;
        }
    }

    private boolean checkPreferenceConditions(EntityPlayerSP player) {
        String pref = preference.getSelected();

        if (pref.equals("KB Reduction")) {
            // Only attack when: hurt + in air + moving
            return player.hurtTime > 0 && !player.onGround && isMoving(player);
        } else if (pref.equals("Critical Hits")) {
            // Only attack when falling
            return !player.onGround && player.motionY < 0;
        }
        // Move Speed: no specific condition, relies on delay
        return false;
    }

    private void startBurst() {
        burstActive = true;
        burstCounter = 0;
        currentShouldAttack = true;
    }

    private boolean isMoving(EntityPlayerSP player) {
        return player.moveForward != 0 || player.moveStrafing != 0;
    }

    /**
     * Called when an attack actually happens - track time and burst count
     */
    private static void onAttackPerformed() {
        lastAttackTime = System.currentTimeMillis();

        // Handle burst counting
        if (instance != null && instance.mode.getSelected().equals("Burst")) {
            burstCounter++;
            int maxBurst = (int) instance.burstHits.getValue();

            if (burstCounter >= maxBurst) {
                // Burst complete - wait for next conditions
                burstActive = false;
                burstCounter = 0;
            }
        }
    }

    /**
     * Check if we can swing - called by AutoClicker
     */
    public static boolean canSwing() {
        if (instance == null || !instance.enabled) return true;
        return currentShouldAttack;
    }

    /**
     * Check if we can attack - same as canSwing for external use
     */
    public static boolean canAttack() {
        return canSwing();
    }

    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled) return true;

        // Track attack time when attack packet is sent
        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;

            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                onAttackPerformed();
            }
        }

        return true;
    }

    public static HitSelect getInstance() {
        return instance;
    }
}
