/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.event.Render3DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.ColorSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Trajectories - Shows the predicted path of thrown projectiles
 * Colors indicate what the projectile will hit:
 * - Purple: Default path
 * - Red: Will hit an enemy
 * - Green: Will hit a wall (horizontal surface)
 * - Cyan: Will hit the ground (vertical surface)
 */
public class Trajectories extends Module {

    // Colors
    private final ColorSetting defaultColor;
    private final ColorSetting enemyColor;
    private final ColorSetting wallColor;
    private final ColorSetting groundColor;

    // Settings
    private final SliderSetting lineWidth;
    private final SliderSetting maxTicks;
    private final TickSetting showLandingBox;
    private final TickSetting onlyWhenHolding;

    // Hit types
    private static final int HIT_NONE = 0;
    private static final int HIT_ENTITY = 1;
    private static final int HIT_WALL = 2;
    private static final int HIT_GROUND = 3;

    public Trajectories() {
        super("Trajectories", ModuleCategory.VISUALS);
        this.registerSetting(new DescriptionSetting("Show projectile paths"));

        this.registerSetting(new DescriptionSetting("--- Colors ---"));
        this.registerSetting(defaultColor = new ColorSetting("Default", 170, 0, 255));   // Purple
        this.registerSetting(enemyColor = new ColorSetting("Enemy Hit", 255, 50, 50));   // Red
        this.registerSetting(wallColor = new ColorSetting("Wall Hit", 50, 255, 50));     // Green
        this.registerSetting(groundColor = new ColorSetting("Ground Hit", 85, 255, 255)); // Cyan

        this.registerSetting(new DescriptionSetting("--- Settings ---"));
        this.registerSetting(lineWidth = new SliderSetting("Line Width", 2.0, 1.0, 5.0, 0.5));
        this.registerSetting(maxTicks = new SliderSetting("Max Ticks", 100.0, 30.0, 200.0, 10.0));
        this.registerSetting(showLandingBox = new TickSetting("Show Landing", true));
        this.registerSetting(onlyWhenHolding = new TickSetting("Only When Holding", true));
    }

    /**
     * Get projectile properties for an item
     * Returns [velocity, gravity, size] or null if not a projectile
     */
    private float[] getProjectileProperties(Item item, EntityPlayer player) {
        if (item == Items.bow) {
            // Bow - velocity depends on charge time
            int useCount = player.getItemInUseCount();
            int maxUse = 72000;
            int chargeTime = maxUse - useCount;

            float velocity = (float) chargeTime / 20.0f;
            velocity = (velocity * velocity + velocity * 2.0f) / 3.0f;
            if (velocity > 1.0f) velocity = 1.0f;
            velocity *= 3.0f;

            return new float[]{velocity, 0.05f, 0.5f}; // Arrow
        }
        if (item == Items.ender_pearl) {
            return new float[]{1.5f, 0.03f, 0.25f};
        }
        if (item == Items.snowball || item == Items.egg) {
            return new float[]{1.5f, 0.03f, 0.25f};
        }
        if (item == Items.experience_bottle) {
            return new float[]{0.7f, 0.07f, 0.25f};
        }
        if (item == Items.potionitem) {
            return new float[]{0.5f, 0.05f, 0.25f};
        }
        if (item == Items.fishing_rod) {
            return new float[]{1.5f, 0.04f, 0.25f};
        }
        return null;
    }

    /**
     * Check if the player is holding a projectile item
     */
    private ItemStack getHeldProjectile(EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        if (held == null) return null;

        Item item = held.getItem();

        // Check if it's a throwable
        if (item == Items.ender_pearl || item == Items.snowball ||
            item == Items.egg || item == Items.experience_bottle) {
            return held;
        }

        // Check splash potions
        if (item == Items.potionitem) {
            // Check if it's a splash potion (damage value)
            int damage = held.getMetadata();
            if ((damage & 16384) != 0) { // Splash bit
                return held;
            }
        }

        // Check bow (only if using)
        if (item instanceof ItemBow && player.isUsingItem()) {
            return held;
        }

        // Fishing rod
        if (item == Items.fishing_rod) {
            return held;
        }

        return null;
    }

    /**
     * Main 3D render method - called from CustomRenderPlayer.doRender()
     */
    @Subscribe
    public void render3D(Render3DEvent event) {
        float partialTicks = event.getPartialTicks();
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        // Don't render when GUI is open
        if (mc.currentScreen != null) return;

        EntityPlayer player = mc.thePlayer;

        // Check if holding a projectile
        ItemStack heldStack = getHeldProjectile(player);
        if (onlyWhenHolding.isEnabled() && heldStack == null) return;
        if (heldStack == null) return;

        Item item = heldStack.getItem();
        float[] props = getProjectileProperties(item, player);
        if (props == null) return;

        float velocity = props[0];
        float gravity = props[1];
        float size = props[2];

        // Skip bow if not charged enough
        if (item == Items.bow && velocity < 0.1f) return;

        RenderManager rm = mc.getRenderManager();
        if (rm == null) return;

        // Use RenderManager camera position
        double camX = rm.viewerPosX;
        double camY = rm.viewerPosY;
        double camZ = rm.viewerPosZ;

        // Calculate initial position and direction
        float yaw = player.rotationYaw;
        float pitch = player.rotationPitch;

        // Base position at eye level
        double baseX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double baseY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.getEyeHeight() - 0.1;
        double baseZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        // In first person, offset the start position forward so the line appears near the crosshair
        double startX = baseX;
        double startY = baseY;
        double startZ = baseZ;

        if (mc.gameSettings.thirdPersonView == 0) {
            // First person - offset forward by 0.5 blocks in look direction
            // This makes the trajectory start near where the hand/crosshair appears
            double forwardOffset = 0.5;
            double dirX = -MathHelper.sin(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI);
            double dirY = -MathHelper.sin(pitch / 180.0f * (float) Math.PI);
            double dirZ = MathHelper.cos(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI);

            startX += dirX * forwardOffset;
            startY += dirY * forwardOffset;
            startZ += dirZ * forwardOffset;
        }

        // Calculate motion vector
        double motionX = -MathHelper.sin(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI) * velocity;
        double motionY = -MathHelper.sin(pitch / 180.0f * (float) Math.PI) * velocity;
        double motionZ = MathHelper.cos(yaw / 180.0f * (float) Math.PI) * MathHelper.cos(pitch / 180.0f * (float) Math.PI) * velocity;

        // Collect path points
        List<Vec3> pathPoints = new ArrayList<>();
        pathPoints.add(new Vec3(startX, startY, startZ));

        double x = startX;
        double y = startY;
        double z = startZ;
        double vx = motionX;
        double vy = motionY;
        double vz = motionZ;

        int hitType = HIT_NONE;
        Vec3 hitPos = null;
        Entity hitEntity = null;

        int maxIterations = (int) maxTicks.getValue();

        for (int i = 0; i < maxIterations; i++) {
            Vec3 currentPos = new Vec3(x, y, z);
            Vec3 nextPos = new Vec3(x + vx, y + vy, z + vz);

            // Check block collision
            MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(currentPos, nextPos);

            if (blockHit != null && blockHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                // Determine if it's a wall or ground hit
                BlockPos hitBlockPos = blockHit.getBlockPos();
                int side = blockHit.sideHit.getIndex();

                // Side 0 = bottom, 1 = top, 2-5 = sides
                if (side == 0 || side == 1) {
                    hitType = HIT_GROUND; // Hit top or bottom of block (horizontal surface)
                } else {
                    hitType = HIT_WALL; // Hit side of block (vertical surface)
                }

                hitPos = blockHit.hitVec;
                pathPoints.add(hitPos);
                break;
            }

            // Check entity collision
            AxisAlignedBB projectileBB = new AxisAlignedBB(
                x - size, y - size, z - size,
                x + size, y + size, z + size
            ).addCoord(vx, vy, vz).expand(1.0, 1.0, 1.0);

            Entity closestEntity = null;
            double closestDistance = Double.MAX_VALUE;
            Vec3 closestHitVec = null;

            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity == player) continue;
                if (!(entity instanceof EntityLivingBase)) continue;
                if (entity.isDead) continue;

                AxisAlignedBB entityBB = entity.getEntityBoundingBox().expand(0.3, 0.3, 0.3);

                MovingObjectPosition entityHit = entityBB.calculateIntercept(currentPos, nextPos);
                if (entityHit != null) {
                    double dist = currentPos.distanceTo(entityHit.hitVec);
                    if (dist < closestDistance) {
                        closestDistance = dist;
                        closestEntity = entity;
                        closestHitVec = entityHit.hitVec;
                    }
                }
            }

            if (closestEntity != null) {
                hitType = HIT_ENTITY;
                hitPos = closestHitVec;
                hitEntity = closestEntity;
                pathPoints.add(hitPos);
                break;
            }

            // Apply physics
            x += vx;
            y += vy;
            z += vz;

            // Check if in water - slow down
            BlockPos currentBlockPos = new BlockPos(x, y, z);
            Block block = mc.theWorld.getBlockState(currentBlockPos).getBlock();
            if (block.getMaterial() == Material.water) {
                vx *= 0.8;
                vy *= 0.8;
                vz *= 0.8;
            } else {
                vx *= 0.99;
                vy *= 0.99;
                vz *= 0.99;
                vy -= gravity;
            }

            pathPoints.add(new Vec3(x, y, z));

            // Stop if too far down
            if (y < -64) break;
        }

        // Get color based on hit type
        int color;
        switch (hitType) {
            case HIT_ENTITY:
                color = enemyColor.getColor();
                break;
            case HIT_WALL:
                color = wallColor.getColor();
                break;
            case HIT_GROUND:
                color = groundColor.getColor();
                break;
            default:
                color = defaultColor.getColor();
                break;
        }

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // Render trajectory line
        renderTrajectoryLine(pathPoints, camX, camY, camZ, r, g, b);

        // Render landing indicator
        if (showLandingBox.isEnabled() && hitPos != null) {
            renderLandingBox(hitPos, camX, camY, camZ, r, g, b, hitType, hitEntity);
        }
    }

    private void renderTrajectoryLine(List<Vec3> points, double camX, double camY, double camZ, float r, float g, float b) {
        if (points.size() < 2) return;

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        GL11.glLineWidth((float) lineWidth.getValue());
        GL11.glColor4f(r, g, b, 1.0f);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (Vec3 point : points) {
            GL11.glVertex3d(point.xCoord - camX, point.yCoord - camY, point.zCoord - camZ);
        }
        GL11.glEnd();

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void renderLandingBox(Vec3 hitPos, double camX, double camY, double camZ, float r, float g, float b, int hitType, Entity hitEntity) {
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        double x = hitPos.xCoord - camX;
        double y = hitPos.yCoord - camY;
        double z = hitPos.zCoord - camZ;

        // Different box size based on hit type
        double boxSize = hitType == HIT_ENTITY ? 0.4 : 0.2;

        AxisAlignedBB box = new AxisAlignedBB(
            x - boxSize, y - boxSize, z - boxSize,
            x + boxSize, y + boxSize, z + boxSize
        );

        // Draw outline
        GL11.glLineWidth(2.0f);
        GL11.glColor4f(r, g, b, 1.0f);
        RenderGlobal.drawSelectionBoundingBox(box);

        // Draw filled with transparency
        GL11.glColor4f(r, g, b, 0.3f);
        drawFilledBox(box);

        // Draw crosshair at hit point
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(r, g, b, 1.0f);

        double crossSize = hitType == HIT_ENTITY ? 0.5 : 0.3;

        GL11.glBegin(GL11.GL_LINES);
        // X axis
        GL11.glVertex3d(x - crossSize, y, z);
        GL11.glVertex3d(x + crossSize, y, z);
        // Y axis
        GL11.glVertex3d(x, y - crossSize, z);
        GL11.glVertex3d(x, y + crossSize, z);
        // Z axis
        GL11.glVertex3d(x, y, z - crossSize);
        GL11.glVertex3d(x, y, z + crossSize);
        GL11.glEnd();

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void drawFilledBox(AxisAlignedBB box) {
        GL11.glBegin(GL11.GL_QUADS);

        // Bottom
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);

        // Top
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);

        // North
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);

        // South
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);

        // West
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);

        // East
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);

        GL11.glEnd();
    }
}
