/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.combat.autoblock.*;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Friends;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import org.lwjgl.input.Mouse;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AutoBlock - Automatic blocking after hitting
 *
 * Modes:
 * - Legit: Simple hit -> block -> idle
 * - Lag: HypixelBlink style - queues packets while attacking
 */
public class AutoBlock extends Module {

    private static AutoBlock instance;

    // Mode selector
    private final ModeSetting mode;

    // Common Settings
    private final SliderSetting range;
    private final SliderSetting holdDuration;
    private final TickSetting blockAnimation;
    private final TickSetting requiredRightClick;
    private final TickSetting waitForDamage;
    private final TickSetting enemyLookingAtYou;

    // Lag Mode Settings
    private final SliderSetting lagDuration;
    private final TickSetting lagDebug;

    // Mode instances
    private final LegitMode legitMode;
    private final LagMode lagMode;

    // State
    private volatile boolean hasReceivedDamage = false;
    private volatile int lastHurtTime = 0;

    // Thread
    private Thread blockThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AutoBlock() {
        super("AutoBlock", ModuleCategory.COMBAT);
        instance = this;

        this.registerSetting(new DescriptionSetting("Block after hitting"));

        // Mode selector - Legit and Lag (HypixelBlink)
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Legit", "Lag"}));

        // Settings
        this.registerSetting(range = new SliderSetting("Range", 4.0, 0.0, 6.0, 0.1));
        this.registerSetting(holdDuration = new SliderSetting("Hold Duration ms", 250, 50, 500, 10));
        this.registerSetting(blockAnimation = new TickSetting("Block Animation", true));
        this.registerSetting(requiredRightClick = new TickSetting("Required Right Click", false));
        this.registerSetting(waitForDamage = new TickSetting("Wait For Damage", false));
        this.registerSetting(enemyLookingAtYou = new TickSetting("Enemy Looking At You", false));

        // Lag Mode Settings (visible only in Lag mode)
        this.registerSetting(lagDuration = new SliderSetting("Lag Duration ms", 76, 30, 200, 5));
        this.registerSetting(lagDebug = new TickSetting("Lag Debug", false));

        // Initialize modes
        legitMode = new LegitMode(this);
        lagMode = new LagMode(this);
    }

    /**
     * Get the current active mode based on setting
     */
    private BaseMode getCurrentMode() {
        if ("Lag".equals(mode.getSelected())) {
            return lagMode;
        }
        return legitMode;
    }

    @Override
    public void onEnable() {
        getCurrentMode().onEnable();

        if (blockThread != null && blockThread.isAlive()) {
            running.set(false);
            blockThread.interrupt();
            try { blockThread.join(100); } catch (Exception e) {}
        }

        running.set(true);
        hasReceivedDamage = false;
        lastHurtTime = 0;

        blockThread = new Thread(this::mainLoop, "AutoBlock-Thread");
        blockThread.setDaemon(true);
        blockThread.start();
    }

    @Override
    public void onDisable() {
        running.set(false);

        if (blockThread != null) {
            blockThread.interrupt();
            try { blockThread.join(100); } catch (Exception e) {}
        }

        getCurrentMode().onDisable();
    }

    private void mainLoop() {
        while (running.get()) {
            try {
                if (!enabled || mc == null) {
                    Thread.sleep(5);
                    continue;
                }

                EntityPlayerSP player = mc.thePlayer;
                if (player == null || player.isDead || mc.theWorld == null || mc.currentScreen != null) {
                    getCurrentMode().onDisable();
                    Thread.sleep(5);
                    continue;
                }

                // Must hold sword
                if (!isHoldingSword(player)) {
                    getCurrentMode().onDisable();
                    Thread.sleep(5);
                    continue;
                }

                // If player is using an item that's not blocking (eating/drinking), disable
                if (player.isUsingItem()) {
                    ItemStack usingItem = player.getItemInUse();
                    if (usingItem != null) {
                        net.minecraft.item.EnumAction action = usingItem.getItemUseAction();
                        if (action != net.minecraft.item.EnumAction.BLOCK) {
                            getCurrentMode().onDisable();
                            Thread.sleep(5);
                            continue;
                        }
                    }
                }

                // Required Right Click check - skip tick if not holding RMB (unless already blocking)
                if (requiredRightClick.isEnabled() && !Mouse.isButtonDown(1) && !getCurrentMode().isBlocking()) {
                    Thread.sleep(5);
                    continue;
                }

                // Wait for damage check
                if (waitForDamage.isEnabled()) {
                    if (player.hurtTime > 0 && lastHurtTime == 0) {
                        hasReceivedDamage = true;
                    }
                    lastHurtTime = player.hurtTime;

                    if (!getCurrentMode().isBlocking() && player.hurtTime == 0) {
                        hasReceivedDamage = false;
                    }

                    if (!hasReceivedDamage && !getCurrentMode().isBlocking()) {
                        Thread.sleep(5);
                        continue;
                    }
                }

                // Check if target in range
                EntityLivingBase target = findTarget(player);
                long now = System.currentTimeMillis();

                // Run mode
                getCurrentMode().onTick(player, target, now);

                Thread.sleep(1);

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                try { Thread.sleep(10); } catch (Exception ex) {}
            }
        }
    }

    /**
     * Intercept outgoing packets
     */
    public boolean shouldSendPacket(Packet<?> packet) {
        if (!enabled) return true;

        // Never delay critical packets
        if (packet instanceof C00PacketKeepAlive ||
            packet instanceof C01PacketChatMessage ||
            packet instanceof C0FPacketConfirmTransaction) {
            return true;
        }

        // Detect attack
        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity useEntity = (C02PacketUseEntity) packet;
            if (useEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                // Get the target entity from the packet
                Entity attackTarget = useEntity.getEntityFromWorld(mc.theWorld);

                // Don't trigger autoblock when attacking teammates
                if (isTeammateEntity(attackTarget)) {
                    return true;
                }

                // Don't trigger autoblock when attacking friends
                if (attackTarget instanceof EntityPlayer) {
                    if (Friends.isFriend(attackTarget.getName())) {
                        return true;
                    }
                }

                // Respect Required Right Click setting
                if (requiredRightClick.isEnabled() && !Mouse.isButtonDown(1)) {
                    return true;
                }
                // Wait for Damage check
                if (waitForDamage.isEnabled() && !hasReceivedDamage) {
                    return true;
                }
                // Enemy Looking At You check
                if (enemyLookingAtYou.isEnabled()) {
                    Entity target = mc.objectMouseOver != null ? mc.objectMouseOver.entityHit : null;
                    if (target instanceof EntityLivingBase) {
                        if (!isEntityLookingAtPlayer((EntityLivingBase) target, mc.thePlayer)) {
                            return true;
                        }
                    }
                }
                return getCurrentMode().onAttack(packet);
            }
        }

        return getCurrentMode().shouldSendPacket(packet);
    }

    /**
     * Called by AutoClicker when it performs an attack
     */
    public static void notifyAttack() {
        if (instance != null && instance.enabled) {
            // Get target from crosshair
            Entity target = instance.mc.objectMouseOver != null ? instance.mc.objectMouseOver.entityHit : null;

            // Don't trigger autoblock when attacking teammates
            if (instance.isTeammateEntity(target)) {
                return;
            }

            // Don't trigger autoblock when attacking friends
            if (target instanceof EntityPlayer) {
                if (Friends.isFriend(target.getName())) {
                    return;
                }
            }

            // Respect Required Right Click setting
            if (instance.requiredRightClick.isEnabled() && !Mouse.isButtonDown(1)) {
                return;
            }
            // Must be holding sword
            if (!instance.isHoldingSword(instance.mc.thePlayer)) {
                return;
            }
            // Wait for Damage check
            if (instance.waitForDamage.isEnabled() && !instance.hasReceivedDamage) {
                return;
            }
            // Enemy Looking At You check
            if (instance.enemyLookingAtYou.isEnabled()) {
                if (target instanceof EntityLivingBase) {
                    if (!instance.isEntityLookingAtPlayer((EntityLivingBase) target, instance.mc.thePlayer)) {
                        return;
                    }
                }
            }
            instance.getCurrentMode().onNotifyAttack();
        }
    }

    private EntityLivingBase findTarget(EntityPlayerSP player) {
        if (player == null || mc.theWorld == null) return null;

        EntityLivingBase closest = null;
        double closestDist = range.getValue();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == player || !entity.isEntityAlive()) continue;
            if (!(entity instanceof EntityLivingBase)) continue;

            if (entity instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) entity;
                if (Friends.isFriend(p.getName())) continue;
                if (AntiBot.isBotForCombat(p)) continue;
                if (isTeammate(player, p)) continue;
            }

            if (enemyLookingAtYou.isEnabled() && !isEntityLookingAtPlayer((EntityLivingBase) entity, player)) {
                continue;
            }

            double dist = player.getDistanceToEntity(entity);
            if (dist <= closestDist && dist > 0.5) {
                closestDist = dist;
                closest = (EntityLivingBase) entity;
            }
        }

        return closest;
    }

    private boolean isEntityLookingAtPlayer(EntityLivingBase entity, EntityPlayerSP player) {
        if (entity == null || player == null) return false;

        double dx = player.posX - entity.posX;
        double dz = player.posZ - entity.posZ;

        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) return true;

        dx /= dist;
        dz /= dist;

        float yaw = entity.rotationYawHead;
        double lookX = -Math.sin(Math.toRadians(yaw));
        double lookZ = Math.cos(Math.toRadians(yaw));

        double dot = dx * lookX + dz * lookZ;

        return dot > 0;
    }

    private boolean isTeammate(EntityPlayerSP player, EntityPlayer other) {
        if (player == null || other == null) return false;

        try {
            Teams teams = Teams.getInstance();
            if (teams != null && teams.shouldAffectCombat()) {
                return teams.isTeamMate(other);
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Check if an entity is a teammate (for packet interception)
     */
    private boolean isTeammateEntity(Entity entity) {
        if (entity == null) return false;
        if (!(entity instanceof EntityPlayer)) return false;

        try {
            Teams teams = Teams.getInstance();
            if (teams != null && teams.shouldAffectCombat()) {
                return teams.isTeamMate(entity);
            }
        } catch (Exception e) {}
        return false;
    }

    private boolean isHoldingSword(EntityPlayerSP player) {
        if (player == null) return false;
        ItemStack held = player.getHeldItem();
        return held != null && held.getItem() instanceof ItemSword;
    }

    @Override
    public void onUpdate() {
        // Update setting visibility based on mode
        boolean isLagMode = "Lag".equals(mode.getSelected());
        lagDuration.setVisible(isLagMode);
        lagDebug.setVisible(isLagMode);
    }

    // ==================== Getters ====================

    public double getHoldDuration() {
        return holdDuration.getValue();
    }

    public double getLagDuration() {
        return lagDuration.getValue();
    }

    public boolean isBlockAnimationEnabled() {
        return blockAnimation.isEnabled();
    }

    public boolean isLagDebugEnabled() {
        return lagDebug.isEnabled();
    }

    @Override
    public String getDisplaySuffix() {
        String modeName = mode.getSelected();
        int queueSize = getCurrentMode().getQueueSize();
        if (queueSize > 0) {
            return " §7" + modeName + " §c[" + queueSize + "]";
        }
        return " §7" + modeName;
    }

    // ==================== Static Methods ====================

    public static AutoBlock getInstance() {
        return instance;
    }

    public static boolean isCurrentlyBlockingStatic() {
        return instance != null && instance.enabled && instance.getCurrentMode().isBlocking();
    }

    public static boolean isLaggingStatic() {
        return instance != null && instance.enabled && instance.getCurrentMode().isLagging();
    }

    public static boolean isSendingBufferedPacket() {
        return LagMode.isSendingBufferedPacket();
    }

    /**
     * Hook called by Main.onSendPacket for packet interception
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (packet instanceof Packet) {
            return shouldSendPacket((Packet<?>) packet);
        }
        return true;
    }
}
