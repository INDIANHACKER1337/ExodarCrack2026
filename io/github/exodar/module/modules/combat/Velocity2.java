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
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.potion.Potion;
import org.lwjgl.input.Keyboard;

import java.util.Random;

/**
 * Velocity2 - Sakura Velocity clone
 * Modes: Intave, Legit, Intave 13
 */
public class Velocity2 extends Module {

    private final ModeSetting mode;

    // Intave settings
    private final SliderSetting xzOnSprintHit;
    private final SliderSetting xzOnHit;

    // Legit settings
    private final SliderSetting jumpDelay;
    private final SliderSetting accuracy;

    // Conditions
    private final TickSetting holdingS;
    private final TickSetting notWhileSpeed;
    private final TickSetting notWhileJumpBoost;

    // State
    private long lastJumpTime = 0;
    private long jumpHoldStart = 0;
    private boolean jumpHeld = false;

    public Velocity2() {
        super("Velocity2", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Sakura Velocity clone"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Intave", "Legit", "Intave 13"}));

        // Intave settings
        this.registerSetting(new DescriptionSetting("--- Intave ---"));
        this.registerSetting(xzOnSprintHit = new SliderSetting("Horizontal Sprint Hit", 60.0, 0.0, 100.0, 1.0));
        this.registerSetting(xzOnHit = new SliderSetting("Horizontal Hit", 100.0, 0.0, 100.0, 1.0));

        // Legit settings
        this.registerSetting(new DescriptionSetting("--- Legit ---"));
        this.registerSetting(jumpDelay = new SliderSetting("Jump Delay (ms)", 0.0, 0.0, 1000.0, 10.0));
        this.registerSetting(accuracy = new SliderSetting("Accuracy %", 100.0, 0.0, 100.0, 1.0));

        // Conditions
        this.registerSetting(new DescriptionSetting("--- Conditions ---"));
        this.registerSetting(holdingS = new TickSetting("Disable while holding S", false));
        this.registerSetting(notWhileSpeed = new TickSetting("Not While Speed", false));
        this.registerSetting(notWhileJumpBoost = new TickSetting("Not While Jump Boost", false));
    }

    @Override
    public void onEnable() {
        lastJumpTime = 0;
        jumpHoldStart = 0;
        jumpHeld = false;
    }

    @Override
    public void onUpdate() {
        if (!isInGame()) return;
        if (mc.thePlayer == null) return;

        updateSettingsVisibility();

        if (!checks() || noAction()) return;

        String selected = mode.getSelected();

        // Handle jump key release for Legit mode
        if (jumpHeld && System.currentTimeMillis() - jumpHoldStart > 50) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(),
                Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
            jumpHeld = false;
        }

        // Intave 13 mode - speed boost when collided horizontally while taking KB
        if (selected.equals("Intave 13") && mc.inGameHasFocus) {
            if (mc.thePlayer.isCollidedHorizontally &&
                !mc.thePlayer.onGround &&
                !mc.thePlayer.isCollidedVertically &&
                !mc.thePlayer.isInWater() &&
                !mc.thePlayer.isInLava() &&
                mc.thePlayer.hurtTime != 0) {

                Random random = new Random();
                float velocity = (float) (0.3045 + random.nextDouble() * 0.03);
                setSpeed(velocity, mc.thePlayer.rotationYaw);
            }
        }
    }

    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;
        if (mc.thePlayer == null) return true;

        if (!checks() || noAction()) return true;

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity velPacket = (S12PacketEntityVelocity) packet;

            if (velPacket.getEntityID() != mc.thePlayer.getEntityId()) {
                return true;
            }

            String selected = mode.getSelected();

            // Legit mode - jump on velocity
            if (selected.equals("Legit")) {
                long now = System.currentTimeMillis();

                if (Math.random() * 100 < accuracy.getValue()) {
                    if (now - lastJumpTime >= jumpDelay.getValue()) {
                        jump();
                        lastJumpTime = now;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Called when player attacks (for Intave mode)
     * This would need to be hooked from Main.java for full functionality
     */
    public void onAttack() {
        if (!enabled || mc.thePlayer == null) return;
        if (!checks() || noAction()) return;

        String selected = mode.getSelected();

        if (selected.equals("Intave") && mc.thePlayer.hurtTime > 0) {
            double factor;
            if (mc.thePlayer.isSprinting()) {
                factor = xzOnSprintHit.getValue() / 100.0;
            } else {
                factor = xzOnHit.getValue() / 100.0;
            }

            // Reduce horizontal motion
            mc.thePlayer.motionX *= factor;
            mc.thePlayer.motionZ *= factor;
        }
    }

    private void jump() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
        jumpHoldStart = System.currentTimeMillis();
        jumpHeld = true;
    }

    private void setSpeed(float speed, float yaw) {
        double rad = Math.toRadians(yaw);
        mc.thePlayer.motionX = -Math.sin(rad) * speed;
        mc.thePlayer.motionZ = Math.cos(rad) * speed;
    }

    private boolean checks() {
        if (holdingS.isEnabled() && GameSettings.isKeyDown(mc.gameSettings.keyBindBack)) {
            return false;
        }
        return true;
    }

    private boolean noAction() {
        if (mc.thePlayer == null) return true;

        if (notWhileSpeed.isEnabled() && mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return true;
        }

        if (notWhileJumpBoost.isEnabled() && mc.thePlayer.isPotionActive(Potion.jump)) {
            return true;
        }

        return false;
    }

    private void updateSettingsVisibility() {
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
}
