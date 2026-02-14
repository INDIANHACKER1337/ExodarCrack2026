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
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;

import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AutoBlockSlinky - Predictive damage-based auto block with lag/blink
 *
 * Based on Slinky's Auto Block:
 * - Predicts incoming damage based on enemy distance and hurt time
 * - Blocks sword to reduce damage before it happens
 * - Uses lag (blink) to keep sword blocked from server perspective
 *
 * Flow:
 * 1. Detect enemy in range with potential to hit you
 * 2. Start blocking based on "Maximum hurt time" (how early before damage)
 * 3. When unblocking, use lag to delay the unblock packet
 * 4. Server thinks you're still blocked, client lets you attack
 * 5. Reblock immediately after lag ends
 */
public class AutoBlockSlinky extends Module {

    // Settings
    private final SliderSetting range;
    private final SliderSetting maxHurtTime;
    private final SliderSetting maxHoldDuration;
    private final TickSetting forceBlockAnimation;

    // Lag settings
    private final SliderSetting lagChance;
    private final SliderSetting lagMaxDuration;
    private final TickSetting preventDelayAttacks;
    private final TickSetting blockAgainImmediately;

    // Conditions
    private final TickSetting requireLeftClick;
    private final TickSetting requireRightClick;
    private final TickSetting requireDamaged;

    // State
    private boolean isBlocking = false;
    private boolean isLagging = false;
    private long blockStartTime = 0;
    private long lagStartTime = 0;
    private EntityLivingBase currentTarget = null;

    // Packet queue for lag
    private final Queue<Packet<?>> laggedPackets = new ConcurrentLinkedQueue<>();
    private boolean isSendingDirect = false;

    // Reflection for fake block animation
    private static Field itemInUseField = null;
    private static Field itemInUseCountField = null;
    private static boolean reflectionInitialized = false;

    public AutoBlockSlinky() {
        super("AutoBlockSlinky", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Slinky-style predictive block"));

        // Main settings
        this.registerSetting(range = new SliderSetting("Range", 4.0, 2.0, 6.0, 0.1));
        this.registerSetting(maxHurtTime = new SliderSetting("Max Hurt Time ms", 200, 50, 500, 10));
        this.registerSetting(maxHoldDuration = new SliderSetting("Max Hold ms", 150, 50, 500, 10));
        this.registerSetting(forceBlockAnimation = new TickSetting("Force Block Anim", true));

        // Lag settings
        this.registerSetting(new DescriptionSetting("-- Lag Settings --"));
        this.registerSetting(lagChance = new SliderSetting("Lag Chance %", 100, 0, 100, 5));
        this.registerSetting(lagMaxDuration = new SliderSetting("Lag Max Duration", 200, 50, 500, 10));
        this.registerSetting(preventDelayAttacks = new TickSetting("Prevent Delay Attacks", true));
        this.registerSetting(blockAgainImmediately = new TickSetting("Block Again Immediately", true));

        // Conditions
        this.registerSetting(new DescriptionSetting("-- Conditions --"));
        this.registerSetting(requireLeftClick = new TickSetting("Require LMB", true));
        this.registerSetting(requireRightClick = new TickSetting("Require RMB", false));
        this.registerSetting(requireDamaged = new TickSetting("Only When Damaged", false));
    }

    @Override
    public void onEnable() {
        isBlocking = false;
        isLagging = false;
        blockStartTime = 0;
        lagStartTime = 0;
        currentTarget = null;
        laggedPackets.clear();
        isSendingDirect = false;
        initReflection();
    }

    @Override
    public void onDisable() {
        // Release lagged packets from thread
        releasePacketsFromThread();

        // Unblock
        if (isBlocking) {
            setBlockState(false);
            isBlocking = false;
        }

        // Clear fake animation
        EntityPlayerSP player = getPlayer();
        if (player != null) {
            setFakeBlockAnimation(player, false);
        }

        currentTarget = null;
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || player.isDead) return;
        if (mc.currentScreen != null) return;

        // Check if holding sword
        if (!isHoldingSword(player)) {
            if (isBlocking) {
                setBlockState(false);
                isBlocking = false;
            }
            setFakeBlockAnimation(player, false);
            return;
        }

        long now = System.currentTimeMillis();

        // Check conditions
        if (!checkConditions(player)) {
            if (isBlocking && !isLagging) {
                setBlockState(false);
                isBlocking = false;
            }
            setFakeBlockAnimation(player, false);
            return;
        }

        // Find closest threatening enemy
        currentTarget = findThreateningTarget(player);

        // Handle lag release
        if (isLagging) {
            if (now - lagStartTime >= lagMaxDuration.getValue()) {
                // Lag duration over - release packets from separate thread
                releasePacketsFromThread();
                isLagging = false;

                // Reblock immediately if enabled
                if (blockAgainImmediately.isEnabled() && currentTarget != null) {
                    setBlockState(true);
                    isBlocking = true;
                    blockStartTime = now;
                }
            }
        }

        // Force block animation (cosmetic - client side only)
        if (forceBlockAnimation.isEnabled() && currentTarget != null) {
            setFakeBlockAnimation(player, true);
        } else {
            setFakeBlockAnimation(player, false);
        }

        // Predictive blocking logic
        if (currentTarget != null) {
            // Check if we should be blocking based on hurt time prediction
            // maxHurtTime setting = how many ms before potential damage we start blocking
            int hurtTimeMs = getHurtTimeMs(currentTarget);
            boolean shouldBlock = hurtTimeMs <= (int) maxHurtTime.getValue();

            if (shouldBlock && !isBlocking && !isLagging) {
                // Start blocking
                setBlockState(true);
                isBlocking = true;
                blockStartTime = now;
            }

            // Check max hold duration
            if (isBlocking && now - blockStartTime >= maxHoldDuration.getValue()) {
                // Time to unblock - start lag to keep blocked from server perspective
                if (shouldStartLag()) {
                    isLagging = true;
                    lagStartTime = now;
                }

                // Unblock client-side (server still thinks we're blocked due to lag)
                setBlockState(false);
                isBlocking = false;
            }
        } else {
            // No target - stop blocking
            if (isBlocking && !isLagging) {
                setBlockState(false);
                isBlocking = false;
            }
        }

        // Handle attack while lagging (Prevent Delay Attacks)
        if (isLagging && preventDelayAttacks.isEnabled()) {
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                // Player wants to attack - immediately release lag so attack isn't delayed
                releasePacketsFromThread();
                isLagging = false;
            }
        }
    }

    /**
     * Calculate hurt time in milliseconds
     * hurtTime starts at 10 (500ms) when damaged and decrements each tick
     * Returns how many ms until the enemy can be hit again
     */
    private int getHurtTimeMs(EntityLivingBase target) {
        // Each tick = 50ms, hurtTime ticks down from 10
        // When hurtTime is 0, target can be hit
        return target.hurtTime * 50;
    }

    private boolean shouldStartLag() {
        if (lagChance.getValue() >= 100) return true;
        return Math.random() * 100 < lagChance.getValue();
    }

    private boolean checkConditions(EntityPlayerSP player) {
        // Left click condition
        if (requireLeftClick.isEnabled() && !org.lwjgl.input.Mouse.isButtonDown(0)) {
            return false;
        }

        // Right click condition
        if (requireRightClick.isEnabled() && !org.lwjgl.input.Mouse.isButtonDown(1)) {
            return false;
        }

        // Damaged condition - only block if recently took damage
        if (requireDamaged.isEnabled() && player.hurtTime == 0) {
            return false;
        }

        return true;
    }

    private EntityLivingBase findThreateningTarget(EntityPlayerSP player) {
        EntityLivingBase closest = null;
        double closestDist = range.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityPlayer)) continue;
            if (entity == player) continue;
            if (!entity.isEntityAlive()) continue;

            EntityPlayer ep = (EntityPlayer) entity;

            if (Friends.isFriend(ep.getName())) continue;
            if (isTeamMate(player, ep)) continue;

            double dist = player.getDistanceToEntity(ep);
            if (dist > closestDist) continue;

            // Check if target is facing us (threatening)
            if (isTargetFacingMe(player, ep)) {
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = ep;
                }
            }
        }

        return closest;
    }

    /**
     * Check if target is facing us (within 90 degrees)
     */
    private boolean isTargetFacingMe(EntityPlayerSP me, EntityPlayer target) {
        double dx = me.posX - target.posX;
        double dz = me.posZ - target.posZ;
        double yawToMe = Math.toDegrees(Math.atan2(dz, dx)) - 90;

        double yawDiff = wrapAngle(target.rotationYaw - yawToMe);
        return Math.abs(yawDiff) <= 90; // Within 90 degrees = facing us
    }

    private double wrapAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private boolean isTeamMate(EntityPlayerSP player, Entity entity) {
        try {
            ModuleManager manager = Main.getModuleManager();
            Module teams = manager != null ? manager.getModuleByName("Teams") : null;
            if (teams != null && teams.isEnabled() && teams instanceof Teams) {
                return ((Teams) teams).isTeamMate(entity);
            }
            if (entity instanceof EntityLivingBase) {
                return player.isOnSameTeam((EntityLivingBase) entity);
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Intercept packets during lag phase
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled || isSendingDirect) return true;
        if (!isLagging) return true;
        if (packet == null) return true;

        // Queue the packet
        laggedPackets.add((Packet<?>) packet);
        return false;
    }

    /**
     * Release all lagged packets from a separate thread
     * Restores timer to normal after release
     */
    private void releasePacketsFromThread() {
        if (laggedPackets.isEmpty()) return;

        new Thread(() -> {
            try {
                isSendingDirect = true;
                EntityPlayerSP player = getPlayer();
                if (player != null && player.sendQueue != null) {
                    while (!laggedPackets.isEmpty()) {
                        Packet<?> packet = laggedPackets.poll();
                        if (packet != null) {
                            player.sendQueue.addToSendQueue(packet);
                        }
                    }
                }
            } catch (Exception e) {
                // Silent
            } finally {
                isSendingDirect = false;
                laggedPackets.clear();
            }
        }).start();
    }

    private boolean isHoldingSword(EntityPlayerSP player) {
        try {
            ItemStack held = player.getHeldItem();
            if (held == null) return false;
            return held.getItem() instanceof ItemSword;
        } catch (Exception e) {
            return false;
        }
    }

    private void setBlockState(boolean blocking) {
        try {
            if (mc.gameSettings != null && mc.gameSettings.keyBindUseItem != null) {
                int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
                KeyBinding.setKeyBindState(keyCode, blocking);
                if (blocking) {
                    KeyBinding.onTick(keyCode);
                }
            }
        } catch (Exception ignored) {}
    }

    private void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            for (Field f : EntityPlayer.class.getDeclaredFields()) {
                if (f.getType() == ItemStack.class) {
                    f.setAccessible(true);
                    if (itemInUseField == null) {
                        itemInUseField = f;
                    }
                }
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    String name = f.getName().toLowerCase();
                    if (name.contains("use") || name.contains("count")) {
                        itemInUseCountField = f;
                    }
                }
            }
        } catch (Exception e) {}
    }

    private void setFakeBlockAnimation(EntityPlayerSP player, boolean blocking) {
        if (!reflectionInitialized) {
            initReflection();
        }

        try {
            if (blocking) {
                ItemStack held = player.getHeldItem();
                if (held != null && held.getItem() instanceof ItemSword) {
                    if (itemInUseField != null) {
                        itemInUseField.set(player, held);
                    }
                    if (itemInUseCountField != null) {
                        itemInUseCountField.set(player, 72000);
                    }
                }
            } else {
                if (itemInUseField != null) {
                    itemInUseField.set(player, null);
                }
                if (itemInUseCountField != null) {
                    itemInUseCountField.set(player, 0);
                }
            }
        } catch (Exception e) {}
    }

    @Override
    public String getDisplaySuffix() {
        if (isLagging) {
            return " §cLAG";
        }
        if (isBlocking) {
            return " §aBLOCK";
        }
        return "";
    }
}
