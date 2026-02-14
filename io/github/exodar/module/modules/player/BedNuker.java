/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

/**
 * BedNuker - Auto breaks beds in range
 * Based on Myau BedNuker (simplified)
 */
public class BedNuker extends Module {

    private ModeSetting mode;
    private SliderSetting range;
    private SliderSetting speed;
    private TickSetting autoTool;
    private TickSetting swing;

    private BlockPos targetBed = null;
    private int breakStage = 0;
    private float breakProgress = 0.0f;
    private int savedSlot = -1;
    private long lastSearchTime = 0;

    public BedNuker() {
        super("BedNuker", ModuleCategory.PLAYER);

        this.registerSetting(new DescriptionSetting("Auto breaks beds"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Legit", "Swap"}));
        this.registerSetting(range = new SliderSetting("Range", 4.5, 3.0, 6.0, 0.1));
        this.registerSetting(speed = new SliderSetting("Speed %", 0.0, 0.0, 30.0, 1.0));
        this.registerSetting(autoTool = new TickSetting("Auto Tool", true));
        this.registerSetting(swing = new TickSetting("Swing", true));
    }

    @Override
    public void onEnable() {
        resetBreaking();
    }

    @Override
    public void onDisable() {
        restoreSlot();
        resetBreaking();
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        if (!mc.thePlayer.capabilities.allowEdit) return;

        // Check if current target is still valid
        if (targetBed != null) {
            if (mc.theWorld.isAirBlock(targetBed) || !isInRange(targetBed)) {
                restoreSlot();
                resetBreaking();
            }
        }

        // Breaking logic
        if (targetBed != null) {
            IBlockState state = mc.theWorld.getBlockState(targetBed);
            int bestSlot = autoTool.isEnabled() ? findBestTool(state.getBlock()) : mc.thePlayer.inventory.currentItem;

            // Handle tool switching based on mode
            if (mode.getSelected().equals("Legit") && savedSlot == -1 && autoTool.isEnabled()) {
                savedSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = bestSlot;
            }

            switch (breakStage) {
                case 0: // Start breaking
                    if (!mc.thePlayer.isUsingItem()) {
                        if (swing.isEnabled()) mc.thePlayer.swingItem();
                        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                            targetBed,
                            getHitFacing(targetBed)
                        ));
                        breakStage = 1;
                    }
                    break;

                case 1: // Continue breaking
                    float delta = getBreakDelta(state, targetBed, bestSlot);
                    breakProgress += delta;

                    // Add break particles
                    mc.effectRenderer.addBlockHitEffects(targetBed, getHitFacing(targetBed));

                    // Speed boost (reduce required progress)
                    float requiredProgress = 1.0f - (0.3f * ((float) speed.getValue() / 100.0f));

                    if (breakProgress >= requiredProgress) {
                        // Swap mode: switch tool right before breaking
                        if (mode.getSelected().equals("Swap") && autoTool.isEnabled()) {
                            savedSlot = mc.thePlayer.inventory.currentItem;
                            mc.thePlayer.inventory.currentItem = bestSlot;
                        }

                        // Send stop packet
                        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            targetBed,
                            getHitFacing(targetBed)
                        ));
                        if (swing.isEnabled()) mc.thePlayer.swingItem();

                        // Play break sound/particles
                        IBlockState blockState = mc.theWorld.getBlockState(targetBed);
                        if (blockState.getBlock().getMaterial() != Material.air) {
                            mc.theWorld.playAuxSFX(2001, targetBed, Block.getStateId(blockState));
                            mc.theWorld.setBlockToAir(targetBed);
                        }

                        breakStage = 2;
                    }
                    break;

                case 2: // Finished
                    restoreSlot();
                    resetBreaking();
                    break;
            }

            // Show break progress
            if (targetBed != null && !mc.theWorld.isAirBlock(targetBed)) {
                float requiredProgress = 1.0f - (0.3f * ((float) speed.getValue() / 100.0f));
                int stage = (int) ((breakProgress / requiredProgress) * 10.0f) - 1;
                mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), targetBed, stage);
            }

            if (targetBed != null) return;
        }

        // Search for new bed (throttled)
        long now = System.currentTimeMillis();
        if (now - lastSearchTime > 500) {
            lastSearchTime = now;
            targetBed = findNearestBed();
            if (targetBed != null) {
                breakStage = 0;
                breakProgress = 0.0f;

                // Silent rotation - COMMENTED OUT as requested
                // double x = targetBed.getX() + 0.5 - mc.thePlayer.posX;
                // double y = targetBed.getY() + 0.5 - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
                // double z = targetBed.getZ() + 0.5 - mc.thePlayer.posZ;
                // float[] rotations = getRotationsTo(x, y, z);
                // sendRotationPacket(rotations[0], rotations[1]);
            }
        }
    }

    private void resetBreaking() {
        if (targetBed != null) {
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), targetBed, -1);
        }
        targetBed = null;
        breakStage = 0;
        breakProgress = 0.0f;
    }

    private void restoreSlot() {
        if (savedSlot != -1) {
            mc.thePlayer.inventory.currentItem = savedSlot;
            savedSlot = -1;
        }
    }

    private BlockPos findNearestBed() {
        double maxRange = range.getValue();
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;

        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        int searchRange = 6;
        int sX = MathHelper.floor_double(eyeX);
        int sY = MathHelper.floor_double(eyeY);
        int sZ = MathHelper.floor_double(eyeZ);

        for (int x = sX - searchRange; x <= sX + searchRange; x++) {
            for (int y = sY - searchRange; y <= sY + searchRange; y++) {
                for (int z = sZ - searchRange; z <= sZ + searchRange; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();

                    if (block instanceof BlockBed) {
                        double dist = pos.distanceSqToCenter(eyeX, eyeY, eyeZ);
                        if (dist < nearestDist && Math.sqrt(dist) <= maxRange) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }

        return nearest;
    }

    private boolean isInRange(BlockPos pos) {
        double eyeX = mc.thePlayer.posX;
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double eyeZ = mc.thePlayer.posZ;
        double dist = Math.sqrt(pos.distanceSqToCenter(eyeX, eyeY, eyeZ));
        return dist <= range.getValue();
    }

    private EnumFacing getHitFacing(BlockPos pos) {
        // Simple: return UP if above player, else closest horizontal face
        double eyeY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        if (pos.getY() + 0.5 > eyeY) {
            return EnumFacing.DOWN;
        }
        return EnumFacing.UP;
    }

    private int findBestTool(Block block) {
        int bestSlot = mc.thePlayer.inventory.currentItem;
        float bestSpeed = 1.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemPickaxe) {
                float speed = stack.getStrVsBlock(block);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private float getBreakDelta(IBlockState state, BlockPos pos, int slot) {
        Block block = state.getBlock();
        float hardness = block.getBlockHardness(mc.theWorld, pos);
        if (hardness < 0) return 0;

        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        float digSpeed = stack == null ? 1.0f : stack.getStrVsBlock(block);

        // Efficiency enchantment
        if (digSpeed > 1.0f && stack != null) {
            int efficiency = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
            if (efficiency > 0) {
                digSpeed += efficiency * efficiency + 1;
            }
        }

        // Haste effect
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            digSpeed *= 1.0f + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2f;
        }

        // Mining fatigue
        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            int amp = mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier();
            switch (amp) {
                case 0: digSpeed *= 0.3f; break;
                case 1: digSpeed *= 0.09f; break;
                case 2: digSpeed *= 0.0027f; break;
                default: digSpeed *= 0.00081f;
            }
        }

        // Not on ground penalty
        if (!mc.thePlayer.onGround) {
            digSpeed /= 5.0f;
        }

        // In water penalty
        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            digSpeed /= 5.0f;
        }

        boolean canHarvest = block.getMaterial().isToolNotRequired() ||
            (stack != null && stack.canHarvestBlock(block));
        float divisor = canHarvest ? 30.0f : 100.0f;

        return digSpeed / hardness / divisor;
    }

    // Silent rotation methods - COMMENTED OUT as requested
    /*
    private float[] getRotationsTo(double x, double y, double z) {
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(y, dist) * 180.0 / Math.PI);
        return new float[]{yaw, pitch};
    }

    private void sendRotationPacket(float yaw, float pitch) {
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround));
    }
    */

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }
}
