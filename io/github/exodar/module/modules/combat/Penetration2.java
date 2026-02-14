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
import io.github.exodar.setting.DescriptionSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.List;

/**
 * Penetration - Hit entities through blocks
 * Works alone (3 block range) or with Reach (extended range)
 */
public class Penetration2 extends Module {

    // Cache for Reach module
    private static Reach reachModule = null;

    public Penetration2() {
        super("Penetration", ModuleCategory.COMBAT);
        this.registerSetting(new DescriptionSetting("Hit through blocks"));
    }

    @Override
    public void onUpdate() {
        // Logic handled in extendReachThroughBlocks called from hook
    }

    /**
     * Called from Main.onGetMouseOver hook
     * Allows hitting entities through blocks
     */
    public void extendReachThroughBlocks() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Determine reach distance
        double reach = 3.0; // Default vanilla reach

        // Check if Reach module is enabled for extended distance
        if (reachModule == null) {
            ModuleManager mm = Main.getModuleManager();
            if (mm != null) {
                Module m = mm.getModuleByName("Reach");
                if (m instanceof Reach) {
                    reachModule = (Reach) m;
                }
            }
        }

        // If Reach is enabled, let Reach handle extended distance
        // We only need to handle the "through blocks" part
        if (reachModule != null && reachModule.isEnabled()) {
            // Reach is active - it will handle extended reach with our through-blocks flag
            return;
        }

        // Reach is NOT enabled - we handle 3.0 range through blocks ourselves
        // Check if already pointing at an entity
        if (mc.objectMouseOver != null &&
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY &&
            mc.objectMouseOver.entityHit != null) {
            // Already have a target, nothing to do
            return;
        }

        // Find entity at normal reach (3.0) ignoring blocks
        Object[] result = getEntityAtReachThroughBlocks(reach);
        if (result != null) {
            Entity entity = (Entity) result[0];
            Vec3 hitVec = (Vec3) result[1];

            // Set the target - this hits through blocks
            mc.objectMouseOver = new MovingObjectPosition(entity, hitVec);
            mc.pointedEntity = entity;
        }
    }

    /**
     * Find entity at given reach, ignoring blocks
     */
    private Object[] getEntityAtReachThroughBlocks(double reach) {
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

}
