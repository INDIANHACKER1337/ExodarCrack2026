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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C03PacketPlayer;

/**
 * Criticals - Deal critical hits every attack
 * Based on Gothaj CriticalsModule
 */
public class Criticals extends Module {

    private ModeSetting mode;

    private boolean attacked = false;
    private int ticks = 0;

    public Criticals() {
        super("Criticals", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Critical hits every attack"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Packet", "Ground", "NCP", "Verus", "Vulcan", "MiniJump"}));
    }

    @Override
    public void onEnable() {
        attacked = false;
        ticks = 0;
    }

    /**
     * Called when player attacks an entity
     */
    public void onAttack(EntityLivingBase target) {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null) return;
        if (!mc.thePlayer.onGround) return; // Only crit from ground

        String selectedMode = mode.getSelected();

        // Packet mode - send fake position packets immediately
        if (selectedMode.equals("Packet")) {
            if (target == null || target.hurtTime > 2) return;

            // Classic crit packets
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 0.0625,
                mc.thePlayer.posZ,
                false
            ));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                false
            ));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 1.1E-5,
                mc.thePlayer.posZ,
                false
            ));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                false
            ));
        } else if (selectedMode.equals("MiniJump")) {
            // Mini jump - small Y offset
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 0.1,
                mc.thePlayer.posZ,
                false
            ));
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY + 0.01,
                mc.thePlayer.posZ,
                false
            ));
        } else {
            // Other modes use tick-based Y offsets in onUpdate
            attacked = true;
            ticks = 0;
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null) return;

        String selectedMode = mode.getSelected();

        // Tick-based modes
        if (attacked && mc.thePlayer.onGround) {
            ticks++;

            switch (selectedMode) {
                case "Ground":
                    switch (ticks) {
                        case 1:
                            sendPositionPacket(0.0005, false);
                            break;
                        case 2:
                            sendPositionPacket(0.0001, false);
                            attacked = false;
                            ticks = 0;
                            break;
                    }
                    break;

                case "NCP":
                    switch (ticks) {
                        case 1:
                            sendPositionPacket(0.001, true);
                            break;
                        case 2:
                            sendPositionPacket(0, false);
                            attacked = false;
                            ticks = 0;
                            break;
                    }
                    break;

                case "Verus":
                    switch (ticks) {
                        case 1:
                            sendPositionPacket(0.001, true);
                            break;
                        case 2:
                            sendPositionPacket(0, false);
                            attacked = false;
                            ticks = 0;
                            break;
                    }
                    break;

                case "Vulcan":
                    switch (ticks) {
                        case 1:
                            sendPositionPacket(0.164, false);
                            break;
                        case 2:
                            sendPositionPacket(0.083, false);
                            break;
                        case 3:
                            sendPositionPacket(0.003, false);
                            attacked = false;
                            ticks = 0;
                            break;
                    }
                    break;

                default:
                    attacked = false;
                    ticks = 0;
                    break;
            }
        } else if (!mc.thePlayer.onGround) {
            attacked = false;
            ticks = 0;
        }
    }

    private void sendPositionPacket(double yOffset, boolean onGround) {
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
            mc.thePlayer.posX,
            mc.thePlayer.posY + yOffset,
            mc.thePlayer.posZ,
            onGround
        ));
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
