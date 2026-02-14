/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockHopper;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

/**
 * Phase - Pass through blocks
 * Based on Exhibition + Raven XD Phase modules (1:1 ports)
 *
 * Modes (Exhibition):
 * - Normal: Standard phase exploit
 * - FullBlock: Phase with Double.MAX_VALUE packets
 * - Silent: Phase with large Y packets
 * - Skip: Phase with Y offset array
 * - Spider: Climb up when inside block
 *
 * Modes (Raven XD):
 * - Vanilla: Simple collision bypass
 * - GrimAC: World spawn phase with high motion
 */
public class Phase extends Module {

    // Mode selection
    private final ModeSetting mode;

    // Settings
    private final SliderSetting distance;
    private final TickSetting autoToggle;
    private final SliderSetting grimMotion;

    // State
    private Vec3 enablePos;
    private int delay = 0;
    private boolean phasing = false;

    public Phase() {
        super("Phase", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Pass through blocks"));

        // Mode selection - Exhibition + Raven modes
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{
            "Normal", "FullBlock", "Silent", "Skip", "Spider", "Vanilla", "GrimAC"
        }));

        // Distance for movement
        this.registerSetting(distance = new SliderSetting("Distance", 0.5, 0.1, 2.0, 0.1));

        // Auto toggle off when moved 5 blocks
        this.registerSetting(autoToggle = new TickSetting("Auto Toggle", false));

        // GrimAC motion
        this.registerSetting(grimMotion = new SliderSetting("Grim Motion", 3.9, 1.0, 10.0, 0.1));

        updateSettingsVisibility();
    }

    private void updateSettingsVisibility() {
        String selected = mode.getSelected();
        grimMotion.setVisible(selected.equals("GrimAC"));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            enablePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        }
        delay = 0;
        phasing = false;

        // GrimAC mode - set high motion on enable (simulates world change)
        if (mode.getSelected().equals("GrimAC")) {
            if (mc.thePlayer != null) {
                mc.thePlayer.motionY = grimMotion.getValue();
            }
        }
    }

    @Override
    public void onDisable() {
        enablePos = null;
        delay = 0;
        phasing = false;

        if (mc.thePlayer != null) {
            mc.thePlayer.noClip = false;
        }
    }

    @Override
    public void onUpdate() {
        if (!enabled || !isInGame()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        updateSettingsVisibility();

        String selected = mode.getSelected();

        // Auto toggle check
        if (autoToggle.isEnabled() && enablePos != null && !isInsideBlock()) {
            if (mc.thePlayer.getDistance(enablePos.xCoord, enablePos.yCoord, enablePos.zCoord) > 5) {
                setEnabled(false);
                return;
            }
        }

        // Handle modes
        switch (selected) {
            case "Normal":
                handleNormal();
                break;
            case "FullBlock":
                handleFullBlock();
                break;
            case "Silent":
                handleSilent();
                break;
            case "Skip":
                handleSkip();
                break;
            case "Spider":
                handleSpider();
                break;
            case "Vanilla":
                handleVanilla();
                break;
            case "GrimAC":
                handleGrimAC();
                break;
        }
    }

    /**
     * Normal mode (Exhibition)
     * When collided horizontally, sends position packets with offset
     */
    private void handleNormal() {
        // Enable noClip when sneaking or inside block
        if (mc.thePlayer.isSneaking() || isInsideBlock()) {
            mc.thePlayer.noClip = true;
        }

        if (mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isOnLadder() && !isInsideBlock()) {
            double multiplier = 0.3;
            double mx = Math.cos(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double mz = Math.sin(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double x = mc.thePlayer.movementInput.moveForward * multiplier * mx + mc.thePlayer.movementInput.moveStrafe * multiplier * mz;
            double z = mc.thePlayer.movementInput.moveForward * multiplier * mz - mc.thePlayer.movementInput.moveStrafe * multiplier * mx;

            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX + x,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + z,
                false
            ));

            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY - (isOnLiquid() ? 9000.0 : 0.09),
                mc.thePlayer.posZ,
                false
            ));

            mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z);
        }
    }

    /**
     * FullBlock mode (Exhibition)
     * Similar to Normal but sends 10 packets with Double.MAX_VALUE
     */
    private void handleFullBlock() {
        if (mc.thePlayer.isSneaking() || isInsideBlock()) {
            mc.thePlayer.noClip = true;
        }

        if (mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isOnLadder() && !isInsideBlock()) {
            double multiplier = 0.31;
            double mx = Math.cos(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double mz = Math.sin(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double x = mc.thePlayer.movementInput.moveForward * multiplier * mx + mc.thePlayer.movementInput.moveStrafe * multiplier * mz;
            double z = mc.thePlayer.movementInput.moveForward * multiplier * mz - mc.thePlayer.movementInput.moveStrafe * multiplier * mx;

            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX + x,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + z,
                false
            ));

            for (int i = 1; i < 11; ++i) {
                sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    Double.MAX_VALUE * i,
                    mc.thePlayer.posZ,
                    false
                ));
            }

            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY - (isOnLiquid() ? 9000.0 : 0.1),
                mc.thePlayer.posZ,
                false
            ));

            mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z);
        }
    }

    /**
     * Silent mode (Exhibition)
     * Sends packets with large Y value
     */
    private void handleSilent() {
        if (mc.thePlayer.isSneaking() || isInsideBlock()) {
            mc.thePlayer.noClip = true;
        }

        if (mc.thePlayer.isCollidedHorizontally && !mc.thePlayer.isOnLadder() && !isInsideBlock()) {
            double multiplier = 0.3;
            double mx = Math.cos(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double mz = Math.sin(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double x = mc.thePlayer.movementInput.moveForward * multiplier * mx + mc.thePlayer.movementInput.moveStrafe * multiplier * mz;
            double z = mc.thePlayer.movementInput.moveForward * multiplier * mz - mc.thePlayer.movementInput.moveStrafe * multiplier * mx;

            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX + x,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + z,
                false
            ));

            for (int i = 1; i < 10; ++i) {
                sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    8.988465674311579E307,
                    mc.thePlayer.posZ,
                    false
                ));
            }

            mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z);
        }
    }

    /**
     * Skip mode (Exhibition)
     * Sends packets with specific Y offset array
     */
    private void handleSkip() {
        if (mc.thePlayer.isSneaking() || isInsideBlock()) {
            mc.thePlayer.noClip = true;
        }

        if (mc.thePlayer.isCollidedHorizontally) {
            mc.thePlayer.motionX *= 0.5;
            mc.thePlayer.motionZ *= 0.5;

            double multiplier = 0.3;
            double mx = Math.cos(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double mz = Math.sin(Math.toRadians(mc.thePlayer.rotationYaw + 90.0f));
            double x = mc.thePlayer.movementInput.moveForward * multiplier * mx + mc.thePlayer.movementInput.moveStrafe * multiplier * mz;
            double z = mc.thePlayer.movementInput.moveForward * multiplier * mz - mc.thePlayer.movementInput.moveStrafe * multiplier * mx;

            double[] OPOP = {
                -0.02500000037252903, -0.028571428997176036, -0.033333333830038704,
                -0.04000000059604645, -0.05000000074505806, -0.06666666766007741,
                -0.10000000149011612, 0.0, -0.20000000298023224, -0.04000000059604645,
                -0.033333333830038704, -0.028571428997176036, -0.02500000037252903
            };

            for (int j = 0; j < OPOP.length; ++j) {
                sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + OPOP[j],
                    mc.thePlayer.posZ,
                    false
                ));
                sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                    mc.thePlayer.posX + x * j,
                    mc.thePlayer.getEntityBoundingBox().minY,
                    mc.thePlayer.posZ + z * j,
                    false
                ));
            }

            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.posY,
                mc.thePlayer.posZ,
                true
            ));

            mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z);

            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX,
                mc.thePlayer.getEntityBoundingBox().minY,
                mc.thePlayer.posZ,
                false
            ));
        }
    }

    /**
     * Spider mode (Exhibition)
     * When inside block, climb up slowly
     */
    private void handleSpider() {
        if (mc.thePlayer.isSneaking() || isInsideBlock()) {
            mc.thePlayer.noClip = true;
        }

        if (isInsideBlock()) {
            mc.thePlayer.posY += 0.1;
            mc.thePlayer.motionY = 0.065;
            mc.thePlayer.fallDistance = 0;
        }
    }

    /**
     * Vanilla mode (Raven XD)
     * Simple collision bypass with position packet
     */
    private void handleVanilla() {
        phasing = false;

        double rotation = Math.toRadians(mc.thePlayer.rotationYaw);
        double x = Math.sin(rotation);
        double z = Math.cos(rotation);

        if (mc.thePlayer.isCollidedHorizontally) {
            mc.thePlayer.setPosition(
                mc.thePlayer.posX - x * 0.005,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + z * 0.005
            );
            phasing = true;
            mc.thePlayer.noClip = true;
        } else if (isInsideBlock()) {
            sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(
                mc.thePlayer.posX - x * 1.5,
                mc.thePlayer.posY,
                mc.thePlayer.posZ + z * 1.5,
                false
            ));

            mc.thePlayer.motionX *= 0.3;
            mc.thePlayer.motionZ *= 0.3;

            phasing = true;
            mc.thePlayer.noClip = true;
        }
    }

    /**
     * GrimAC mode (Raven XD)
     * Sets high motionY on enable, removes collisions for first 10 ticks
     * Best used when teleporting to a new world/game
     */
    private void handleGrimAC() {
        // Remove block collisions for first 10 ticks
        if (mc.thePlayer.ticksExisted < 10) {
            mc.thePlayer.noClip = true;
        } else {
            // After 10 ticks, disable module
            setEnabled(false);
        }
    }

    /**
     * Send packet helper
     */
    private void sendPacket(C03PacketPlayer packet) {
        try {
            if (mc.thePlayer != null && mc.thePlayer.sendQueue != null) {
                mc.thePlayer.sendQueue.addToSendQueue(packet);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Check if player is inside a solid block (Exhibition method)
     */
    private boolean isInsideBlock() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        if (bb == null) return false;

        for (int x = MathHelper.floor_double(bb.minX); x < MathHelper.floor_double(bb.maxX) + 1; ++x) {
            for (int y = MathHelper.floor_double(bb.minY); y < MathHelper.floor_double(bb.maxY) + 1; ++y) {
                for (int z = MathHelper.floor_double(bb.minZ); z < MathHelper.floor_double(bb.maxZ) + 1; ++z) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();

                    if (block != null && !(block instanceof BlockAir)) {
                        AxisAlignedBB blockBB = block.getCollisionBoundingBox(mc.theWorld, pos, mc.theWorld.getBlockState(pos));

                        if (block instanceof BlockHopper) {
                            blockBB = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
                        }

                        if (blockBB != null && bb.intersectsWith(blockBB)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if player is on liquid
     */
    private boolean isOnLiquid() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        if (bb == null) return false;

        AxisAlignedBB below = new AxisAlignedBB(bb.minX, bb.minY - 0.01, bb.minZ, bb.maxX, bb.minY, bb.maxZ);

        for (int x = MathHelper.floor_double(below.minX); x < MathHelper.floor_double(below.maxX) + 1; ++x) {
            for (int z = MathHelper.floor_double(below.minZ); z < MathHelper.floor_double(below.maxZ) + 1; ++z) {
                BlockPos pos = new BlockPos(x, (int) below.minY, z);
                Block block = mc.theWorld.getBlockState(pos).getBlock();
                if (block.getMaterial().isLiquid()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
