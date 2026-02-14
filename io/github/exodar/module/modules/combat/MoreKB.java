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
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

/**
 * MoreKB - Increases knockback dealt to enemies by sprint resetting
 * Based on Myau MoreKB
 */
public class MoreKB extends Module {

    private ModeSetting mode;
    private TickSetting intelligent;
    private TickSetting onlyGround;

    private EntityLivingBase target;

    public MoreKB() {
        super("MoreKB", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("More knockback on hit"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Legit", "LegitFast", "LessPacket", "Packet", "DoublePacket"}));
        this.registerSetting(intelligent = new TickSetting("Intelligent", false));
        this.registerSetting(onlyGround = new TickSetting("Only Ground", true));
    }

    @Override
    public void onEnable() {
        target = null;
    }

    /**
     * Called when player attacks an entity
     */
    public void onAttack(Entity targetEntity) {
        if (!enabled) return;

        if (targetEntity instanceof EntityLivingBase) {
            this.target = (EntityLivingBase) targetEntity;
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        String selectedMode = mode.getSelected();

        // LegitFast mode - reset sprint ticks when we have a target
        if (selectedMode.equals("LegitFast")) {
            if (target != null && isMoving()) {
                if (!onlyGround.isEnabled() || mc.thePlayer.onGround) {
                    mc.thePlayer.sprintingTicksLeft = 0;
                }
                target = null;
            }
            return;
        }

        // Other modes - check entity we're looking at
        EntityLivingBase entity = null;
        if (mc.objectMouseOver != null &&
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
            mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
        }

        if (entity == null) return;

        // Intelligent check - only apply if enemy is looking at us
        if (intelligent.isEnabled()) {
            double x = mc.thePlayer.posX - entity.posX;
            double z = mc.thePlayer.posZ - entity.posZ;
            float calcYaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI - 90.0);
            float diffY = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - entity.rotationYawHead));
            if (diffY > 120.0F) {
                return;
            }
        }

        // Check if entity was just hit (hurtTime == 10 = just got hit)
        if (entity.hurtTime == 10) {
            switch (selectedMode) {
                case "Legit":
                    // Simple sprint reset
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                        mc.thePlayer.setSprinting(true);
                    }
                    break;

                case "LessPacket":
                    // Set sprint false, send START packet
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;

                case "Packet":
                    // Send STOP then START packets
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;

                case "DoublePacket":
                    // Send STOP/START/STOP/START packets
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
            }
        }
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
