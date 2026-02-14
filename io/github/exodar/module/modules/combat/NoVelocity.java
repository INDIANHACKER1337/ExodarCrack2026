/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.event.AttackEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.util.Clock;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

import java.util.Random;

/**
 * NoVelocity - 1:1 port from Sakura Velocity
 * Modes: Intave, Legit, Intave 13
 */
public class NoVelocity extends Module {

    // Settings (1:1 Sakura)
    private final ModeSetting mode;
    private final SliderSetting xzOnSprintHit;
    private final SliderSetting xzOnHit;
    private final SliderSetting jumpDelay;
    private final SliderSetting accuracy;
    private final TickSetting holdingS;
    private final TickSetting notWhileSpeed;
    private final TickSetting notWhileJumpBoost;

    // State (1:1 Sakura)
    private final Clock clockHold = new Clock();
    private final Clock clockJump = new Clock();

    public NoVelocity() {
        super("NoVelocity", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("1:1 Sakura Velocity"));

        // Mode (1:1 Sakura)
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Intave", "Legit", "Intave 13"})
            .onChange(this::updateVisibility));

        // Intave settings (1:1 Sakura)
        this.registerSetting(xzOnSprintHit = new SliderSetting("Horizontal Sprint Hit", 60.0, 0.0, 100.0, 1.0));
        this.registerSetting(xzOnHit = new SliderSetting("Horizontal Hit", 100.0, 0.0, 100.0, 1.0));

        // Legit settings (1:1 Sakura)
        this.registerSetting(jumpDelay = new SliderSetting("Jump Delay", 0.0, 0.0, 1000.0, 10.0));
        this.registerSetting(accuracy = new SliderSetting("Accuracy", 100.0, 0.0, 100.0, 1.0));

        // Conditions (1:1 Sakura)
        this.registerSetting(new DescriptionSetting("--- Conditions ---"));
        this.registerSetting(holdingS = new TickSetting("Disable while holding S", false));
        this.registerSetting(notWhileSpeed = new TickSetting("Not While Speed", false));
        this.registerSetting(notWhileJumpBoost = new TickSetting("Not While Jump Boost", false));

        updateVisibility();
    }

    private void updateVisibility() {
        String selected = mode.getSelected();
        boolean isIntave = selected.equals("Intave");
        boolean isLegit = selected.equals("Legit");

        xzOnSprintHit.setVisible(isIntave);
        xzOnHit.setVisible(isIntave);
        jumpDelay.setVisible(isLegit);
        accuracy.setVisible(isLegit);
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }

    /**
     * Check conditions (1:1 Sakura checks())
     */
    private boolean checks() {
        if (holdingS.isEnabled() && GameSettings.isKeyDown(mc.gameSettings.keyBindBack)) {
            return false;
        }
        return true;
    }

    /**
     * Check noAction (1:1 Sakura noAction())
     */
    private boolean noAction() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return true;

        if (notWhileSpeed.isEnabled() && player.isPotionActive(Potion.moveSpeed)) {
            return true;
        }

        if (notWhileJumpBoost.isEnabled() && player.isPotionActive(Potion.jump)) {
            return true;
        }

        return false;
    }

    /**
     * AttackEvent handler - Intave mode (1:1 Sakura)
     *
     * Called BEFORE vanilla attack code runs.
     * Sets slowdownFactor and allowSprint on the event so AttackEventBridge applies them.
     */
    @Subscribe
    public void onAttackEvent(AttackEvent event) {
        if (!enabled) return;
        if (!checks() || noAction()) return;

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Intave mode (1:1 Sakura logic)
        // Set event values that AttackEventBridge will use for compensation
        if (mode.is("Intave") && player.hurtTime > 0) {
            if (player.isSprinting()) {
                event.setSlowdownFactor(xzOnSprintHit.getValue() / 100.0D);
                event.setAllowSprint(true);
            } else {
                event.setSlowdownFactor(xzOnHit.getValue() / 100.0D);
                event.setAllowSprint(false);
            }
        }
    }

    /**
     * Packet handler - Legit mode (1:1 Sakura)
     */
    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;

        EntityPlayerSP player = getPlayer();
        if (player == null) return true;

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity velPacket = (S12PacketEntityVelocity) packet;

            // Check if this velocity is for us
            if (velPacket.getEntityID() != player.getEntityId()) {
                return true;
            }

            if (!checks() || noAction()) return true;

            // Legit mode (1:1 Sakura)
            if (mode.is("Legit")) {
                if (Math.random() * 100.0D < accuracy.getValue()) {
                    jump();
                } else if (clockJump.reach((long) jumpDelay.getValue())) {
                    jump();
                    clockJump.reset();
                }
            }
        }

        return true;
    }

    /**
     * Update handler - Intave 13 mode
     * Note: Intave mode is handled via AttackEvent + AttackEventBridge
     */
    @Override
    public void onUpdate() {
        if (!enabled || !isInGame()) return;

        updateVisibility();

        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        // Intave 13 mode (1:1 Sakura)
        if (mode.is("Intave 13") && mc.inGameHasFocus) {
            if (!checks() || noAction()) return;

            float velocity = (float) MathHelper.getRandomDoubleInRange(new Random(), 0.3045D, 0.3345D);

            if (player.isCollidedHorizontally &&
                !player.onGround &&
                !player.isCollidedVertically &&
                !isInWeb(player) &&
                !player.isInWater() &&
                !player.isInLava() &&
                player.hurtTime != 0) {

                setSpeed(velocity, player.rotationYaw);
            }
        }
    }

    /**
     * Jump method (1:1 Sakura)
     */
    private void jump() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);

        if (clockHold.reach(0L)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(),
                GameSettings.isKeyDown(mc.gameSettings.keyBindJump));
            clockHold.reset();
        }
    }

    /**
     * Check if player is in web
     */
    private boolean isInWeb(EntityPlayerSP player) {
        try {
            java.lang.reflect.Field field = net.minecraft.entity.Entity.class.getDeclaredField("isInWeb");
            field.setAccessible(true);
            return field.getBoolean(player);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * setSpeed (1:1 Sakura MoveUtil.setSpeed)
     */
    private void setSpeed(double speed, float yaw) {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        float direction = getDirection(yaw);
        player.motionX = -Math.sin(Math.toRadians(direction)) * speed;
        player.motionZ = Math.cos(Math.toRadians(direction)) * speed;
    }

    /**
     * getDirection (1:1 Sakura MoveUtil.getDirection)
     */
    private float getDirection(float rotationYaw) {
        boolean leftPressed = GameSettings.isKeyDown(mc.gameSettings.keyBindLeft);
        boolean rightPressed = GameSettings.isKeyDown(mc.gameSettings.keyBindRight);
        boolean backPressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack);
        boolean forwardPressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward);

        float left = leftPressed ?
            (backPressed ? 45 : (forwardPressed ? -45 : -90)) : 0;

        float right = rightPressed ?
            (backPressed ? -45 : (forwardPressed ? 45 : 90)) : 0;

        float back = backPressed ? 180 : 0;

        float yaw = left + right + back;
        return rotationYaw + yaw;
    }
}
