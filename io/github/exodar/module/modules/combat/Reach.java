/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.Main;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Reach - Extends player attack reach
 * Based on Raven XD implementation
 * Works with Penetration module for hitting through blocks
 */
public class Reach extends Module {

    private final SliderSetting minReach;
    private final SliderSetting maxReach;
    private final SliderSetting chance;
    private final TickSetting weaponOnly;
    private final TickSetting movingOnly;
    private final TickSetting sprintOnly;

    private final Random random = new Random();

    // Cache for Penetration module
    private static Penetration2 penetrationModule = null;

    public Reach() {
        super("Reach", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Extend attack reach"));
        this.registerSetting(minReach = new SliderSetting("Min Reach", 3.1, 3.0, 6.0, 0.05));
        this.registerSetting(maxReach = new SliderSetting("Max Reach", 3.3, 3.0, 6.0, 0.05));
        this.registerSetting(chance = new SliderSetting("Chance", 100, 0, 100, 1));
        this.registerSetting(weaponOnly = new TickSetting("Weapon Only", false));
        this.registerSetting(movingOnly = new TickSetting("Moving Only", false));
        this.registerSetting(sprintOnly = new TickSetting("Sprint Only", false));
    }

    /**
     * Check if Penetration module is enabled (allows hitting through blocks)
     */
    private boolean canHitThroughBlocks() {
        if (penetrationModule == null) {
            ModuleManager mm = Main.getModuleManager();
            if (mm != null) {
                Module m = mm.getModuleByName("Penetration");
                if (m instanceof Penetration2) {
                    penetrationModule = (Penetration2) m;
                }
            }
        }
        return penetrationModule != null && penetrationModule.isEnabled();
    }

    @Override
    public void onUpdate() {
        // Reach now works via hook in Main.onGetMouseOver()
        // This method is kept empty for compatibility
    }

    /**
     * Called from Main.onGetMouseOver hook after Minecraft's getMouseOver completes
     * This allows us to extend the reach if no entity was found at normal distance
     */
    public void extendReach() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Check conditions
        if (weaponOnly.isEnabled() && !isHoldingWeapon()) return;
        if (movingOnly.isEnabled() && !isMoving()) return;
        if (sprintOnly.isEnabled() && !mc.thePlayer.isSprinting()) return;

        // If already pointing at an entity within normal range (3.0), don't extend
        if (mc.objectMouseOver != null &&
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
            mc.objectMouseOver.entityHit != null) {
            double dist = mc.thePlayer.getDistanceToEntity(mc.objectMouseOver.entityHit);
            if (dist <= 3.0) return; // Normal reach already hit something
        }

        // Check chance
        if (chance.getValue() < 100 && random.nextDouble() * 100 >= chance.getValue()) return;

        // Get random reach within range
        double reach = minReach.getValue() + random.nextDouble() * (maxReach.getValue() - minReach.getValue());

        // Find entity at extended reach
        Object[] result = getEntityAtReach(reach);
        if (result != null) {
            Entity entity = (Entity) result[0];
            Vec3 hitVec = (Vec3) result[1];

            // Check if we can hit through blocks (requires Penetration module)
            if (!canHitThroughBlocks()) {
                MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(
                    mc.thePlayer.getPositionEyes(1.0F),
                    mc.thePlayer.getPositionEyes(1.0F).addVector(
                        mc.thePlayer.getLookVec().xCoord * reach,
                        mc.thePlayer.getLookVec().yCoord * reach,
                        mc.thePlayer.getLookVec().zCoord * reach
                    ),
                    false, false, false
                );
                if (blockHit != null && blockHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                    double blockDist = mc.thePlayer.getPositionEyes(1.0F).distanceTo(blockHit.hitVec);
                    double entityDist = mc.thePlayer.getPositionEyes(1.0F).distanceTo(hitVec);
                    if (blockDist < entityDist) return;
                }
            }

            // Set the target - this extends the reach
            mc.objectMouseOver = new MovingObjectPosition(entity, hitVec);
            mc.pointedEntity = entity;
        }
    }

    private Object[] getEntityAtReach(double reach) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity renderViewEntity = mc.getRenderViewEntity();
        if (renderViewEntity == null) return null;

        Vec3 eyePos = renderViewEntity.getPositionEyes(1.0F);
        Vec3 lookVec = renderViewEntity.getLook(1.0F);
        Vec3 reachVec = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);

        Entity foundEntity = null;
        Vec3 hitVec = null;
        double closestDist = reach;

        List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
            renderViewEntity,
            renderViewEntity.getEntityBoundingBox()
                .addCoord(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach)
                .expand(1.0, 1.0, 1.0)
        );

        for (Entity entity : entities) {
            if (!entity.canBeCollidedWith()) continue;

            // Skip friends
            if (entity instanceof net.minecraft.entity.player.EntityPlayer) {
                if (io.github.exodar.module.modules.misc.Friends.isFriend(entity.getName())) continue;
                if (AntiBot.isBotForCombat(entity)) continue;
            }

            float borderSize = entity.getCollisionBorderSize();
            AxisAlignedBB entityBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
            MovingObjectPosition intercept = entityBox.calculateIntercept(eyePos, reachVec);

            if (entityBox.isVecInside(eyePos)) {
                if (closestDist >= 0.0) {
                    foundEntity = entity;
                    hitVec = intercept == null ? eyePos : intercept.hitVec;
                    closestDist = 0.0;
                }
            } else if (intercept != null) {
                double dist = eyePos.distanceTo(intercept.hitVec);
                if (dist < closestDist) {
                    foundEntity = entity;
                    hitVec = intercept.hitVec;
                    closestDist = dist;
                }
            }
        }

        // Only return living entities or item frames
        if (foundEntity != null && closestDist < reach) {
            if (!(foundEntity instanceof EntityLivingBase) && !(foundEntity instanceof EntityItemFrame)) {
                return null;
            }
            return new Object[]{foundEntity, hitVec};
        }

        return null;
    }

    private boolean isHoldingWeapon() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return false;
        return mc.thePlayer.getHeldItem().getItem() instanceof ItemSword ||
               mc.thePlayer.getHeldItem().getItem() instanceof ItemAxe;
    }

    private boolean isMoving() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + String.format("%.1f", maxReach.getValue());
    }
}
