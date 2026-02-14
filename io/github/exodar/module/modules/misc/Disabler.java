/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.server.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Disabler - Attempts to disable anticheat checks
 * Based on Gothaj 3.01b Alpha implementation
 *
 * Modes:
 * - Verus Experimental: Boat exploit with timer manipulation
 * - Vulcan: Digging packet spam to confuse anticheat
 * - Verus: Transaction packet queue (200 max)
 * - Verus New: Water bucket + transaction queue
 * - Via Version: Position packet fix for cross-version
 */
public class Disabler extends Module {

    // Mode constants
    private static final String MODE_VERUS_EXP = "Verus Experimental";
    private static final String MODE_VULCAN = "Vulcan";
    private static final String MODE_VERUS = "Verus";
    private static final String MODE_VERUS_NEW = "Verus New";
    private static final String MODE_VIA = "Via Version";
    private static final String MODE_ABILITIES = "Abilities";
    private static final String MODE_KEEPALIVE = "KeepAlive";
    private static final String MODE_TRANSACTION = "Transaction";
    private static final String MODE_VERUS_CUSTOM = "Verus Custom";
    private static final String MODE_VERUS2 = "Verus2";
    private static final String MODE_EXPERIMENTAL = "Experimental";
    private static final String MODE_GHOSTLY = "Ghostly";
    private static final String MODE_SOLOLEGENDS = "SoloLegends";

    private final ModeSetting mode;

    // Verus Custom / Verus2 settings (Rise)
    private final TickSetting verusMovement;
    private final TickSetting verusSprint;
    private final TickSetting verusDamage;

    // Experimental settings (Rise)
    private final SliderSetting experimentalTickDelay;

    // Verus Experimental settings
    private final SliderSetting verusTimer;
    private final TickSetting verusAutoWeapon;

    // Vulcan settings
    private final SliderSetting vulcanDigInterval;
    private final SliderSetting vulcanAbortInterval;

    // Verus/Verus New settings
    private final SliderSetting maxQueueSize;

    // Debug
    private final TickSetting debug;

    // Packet queues
    private final Deque<Packet<?>> packetQueue = new ConcurrentLinkedDeque<>();
    private final Deque<Packet<?>> transactionQueue = new ConcurrentLinkedDeque<>();
    private final Deque<Packet<?>> keepAliveQueue = new ConcurrentLinkedDeque<>();
    private final Deque<Packet<?>> experimentalQueue = new ConcurrentLinkedDeque<>();

    // State
    private boolean disabled = false;
    private int counter = 0;
    private static volatile boolean isSendingDirect = false;

    // Verus2 state
    private boolean teleported = false;
    private long lastFlushTime = 0;
    private int lastHurtTime = 0;

    // Reflection for onGround field
    private static Field onGroundField = null;
    private static Field yField = null;
    private static Field timerField = null;
    private static Field timerSpeedField = null;
    private static boolean reflectionInit = false;

    public Disabler() {
        super("Disabler", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Disable anticheat checks"));
        this.registerSetting(mode = new ModeSetting("Mode",
            new String[]{MODE_VERUS_EXP, MODE_VULCAN, MODE_VERUS, MODE_VERUS_NEW, MODE_VIA, MODE_ABILITIES,
                         MODE_KEEPALIVE, MODE_TRANSACTION, MODE_VERUS_CUSTOM, MODE_VERUS2, MODE_EXPERIMENTAL, MODE_GHOSTLY, MODE_SOLOLEGENDS}));

        // Verus Experimental
        this.registerSetting(verusTimer = new SliderSetting("Timer Speed", 0.3, 0.1, 1.0, 0.05));
        this.registerSetting(verusAutoWeapon = new TickSetting("Auto Weapon", true));

        // Vulcan
        this.registerSetting(vulcanDigInterval = new SliderSetting("Dig Interval", 5, 1, 20, 1));
        this.registerSetting(vulcanAbortInterval = new SliderSetting("Abort Interval", 7, 1, 20, 1));

        // Verus/Verus New
        this.registerSetting(maxQueueSize = new SliderSetting("Max Queue", 200, 50, 500, 10));

        // Verus Custom / Verus2 (Rise)
        this.registerSetting(verusMovement = new TickSetting("Movement", false));
        this.registerSetting(verusSprint = new TickSetting("Sprint", false));
        this.registerSetting(verusDamage = new TickSetting("Damage", false));

        // Experimental (Rise)
        this.registerSetting(experimentalTickDelay = new SliderSetting("Tick Delay", 5, 1, 20, 1));

        // Debug
        this.registerSetting(debug = new TickSetting("Debug", false));

        initReflection();
    }

    private void initReflection() {
        if (reflectionInit) return;
        reflectionInit = true;

        try {
            // Try different field names for onGround in C03PacketPlayer
            String[] names = {"onGround", "field_149474_g", "p"};
            for (String name : names) {
                try {
                    onGroundField = C03PacketPlayer.class.getDeclaredField(name);
                    onGroundField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            // Try different field names for y in C03PacketPlayer (for Ghostly)
            String[] yNames = {"y", "field_149477_b", "b"};
            for (String name : yNames) {
                try {
                    yField = C03PacketPlayer.class.getDeclaredField(name);
                    yField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception ignored) {}

        // Get timer field from Minecraft
        try {
            String[] timerNames = {"timer", "field_71428_T", "Q"};
            for (String name : timerNames) {
                try {
                    timerField = net.minecraft.client.Minecraft.class.getDeclaredField(name);
                    timerField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception ignored) {}

        // Get timerSpeed field from Timer
        try {
            Class<?> timerClass = Class.forName("net.minecraft.util.Timer");
            String[] speedNames = {"timerSpeed", "field_74278_d", "c"};
            for (String name : speedNames) {
                try {
                    timerSpeedField = timerClass.getDeclaredField(name);
                    timerSpeedField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onEnable() {
        packetQueue.clear();
        transactionQueue.clear();
        keepAliveQueue.clear();
        experimentalQueue.clear();
        disabled = false;
        counter = 0;
        isSendingDirect = false;
        teleported = false;
        lastFlushTime = System.currentTimeMillis();
        lastHurtTime = 0;

        String currentMode = mode.getSelected();
        if (MODE_VERUS_EXP.equals(currentMode)) {
            debug("Verus Experimental enabled - need boat nearby");
        } else if (MODE_VERUS.equals(currentMode)) {
            debug("Verus enabled - queuing transactions");
        } else if (MODE_VERUS_NEW.equals(currentMode)) {
            debug("Verus New enabled - water bucket + transactions");
        } else if (MODE_VULCAN.equals(currentMode)) {
            debug("Vulcan enabled - dig packet spam");
        } else if (MODE_VIA.equals(currentMode)) {
            debug("Via Version enabled - position fix");
        } else if (MODE_VERUS_CUSTOM.equals(currentMode)) {
            debug("Verus Custom enabled - sprint toggle");
        } else if (MODE_VERUS2.equals(currentMode)) {
            debug("Verus2 enabled - wait for damage");
        } else if (MODE_EXPERIMENTAL.equals(currentMode)) {
            debug("Experimental enabled - delayed transactions");
        } else if (MODE_GHOSTLY.equals(currentMode)) {
            debug("Ghostly enabled - Y rounding + onGround");
        } else if (MODE_SOLOLEGENDS.equals(currentMode)) {
            debug("SoloLegends enabled - Abilities + Transaction bypass");
        }
    }

    @Override
    public void onDisable() {
        // Flush remaining packets
        flushAllPackets();
        flushQueue(transactionQueue);
        flushQueue(keepAliveQueue);
        flushQueue(experimentalQueue);

        // Reset timer
        setTimerSpeed(1.0f);

        counter = 0;
        disabled = false;
        teleported = false;

        debug("Disabler disabled");
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Update setting visibility
        updateSettingVisibility();

        String currentMode = mode.getSelected();

        switch (currentMode) {
            case MODE_VERUS_EXP:
                handleVerusExperimentalTick();
                break;

            case MODE_VULCAN:
                handleVulcanTick();
                break;

            case MODE_VERUS:
            case MODE_VERUS_NEW:
                // Flush old packets if queue too large
                int maxQueue = (int) maxQueueSize.getValue();
                while (packetQueue.size() > maxQueue) {
                    sendPacketDirect(packetQueue.poll());
                    if (!disabled && MODE_VERUS.equals(currentMode)) {
                        disabled = true;
                        debug("Verus disabled - queue overflow");
                    }
                }
                break;

            case MODE_ABILITIES:
                handleAbilitiesTick();
                break;

            case MODE_VERUS_CUSTOM:
                handleVerusCustomTick();
                break;

            case MODE_VERUS2:
                handleVerus2Tick();
                break;

            case MODE_EXPERIMENTAL:
                handleExperimentalTick();
                break;

            case MODE_GHOSTLY:
                handleGhostlyTick();
                break;

            case MODE_SOLOLEGENDS:
                handleSoloLegendsTick();
                break;
        }
    }

    // ==================== Abilities (Rise) ====================

    private void handleAbilitiesTick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // Send flying abilities packet every 5 ticks
        if (player.ticksExisted % 5 == 0) {
            PlayerCapabilities capabilities = new PlayerCapabilities();
            capabilities.isFlying = true;
            sendPacketDirect(new C13PacketPlayerAbilities(capabilities));
            debug("Sent abilities packet (isFlying=true)");
        }
    }

    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled || isSendingDirect) return true;
        if (!(packet instanceof Packet)) return true;

        Packet<?> pkt = (Packet<?>) packet;
        String currentMode = mode.getSelected();

        switch (currentMode) {
            case MODE_VERUS_EXP:
                return handleVerusExperimentalSend(pkt);

            case MODE_VULCAN:
                return handleVulcanSend(pkt);

            case MODE_VERUS:
                return handleVerusSend(pkt);

            case MODE_VERUS_NEW:
                return handleVerusNewSend(pkt);

            case MODE_KEEPALIVE:
                return handleKeepAliveSend(pkt);

            case MODE_TRANSACTION:
                return handleTransactionSend(pkt);

            case MODE_VERUS_CUSTOM:
                return handleVerusCustomSend(pkt);

            case MODE_VERUS2:
                return handleVerus2Send(pkt);

            case MODE_EXPERIMENTAL:
                return handleExperimentalSend(pkt);

            case MODE_GHOSTLY:
                return handleGhostlySend(pkt);

            case MODE_SOLOLEGENDS:
                return handleSoloLegendsSend(pkt);
        }

        return true;
    }

    // ==================== KeepAlive (Rise) ====================

    private boolean handleKeepAliveSend(Packet<?> packet) {
        // Cancel all C00PacketKeepAlive
        if (packet instanceof C00PacketKeepAlive) {
            debug("Cancelled KeepAlive packet");
            return false;
        }
        return true;
    }

    // ==================== Transaction (Rise) ====================

    private boolean handleTransactionSend(Packet<?> packet) {
        // Cancel all C0FPacketConfirmTransaction
        if (packet instanceof C0FPacketConfirmTransaction) {
            debug("Cancelled Transaction packet");
            return false;
        }
        return true;
    }

    // ==================== Verus Custom (Rise) ====================

    private void handleVerusCustomTick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // Sprint toggle every 5/10 ticks (like Rise)
        if (verusSprint.isEnabled() && isPlayerMoving() && player.ticksExisted > 50) {
            if (player.ticksExisted % 5 == 0) {
                if (player.ticksExisted % 10 == 0) {
                    sendPacketDirect(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.START_SPRINTING));
                } else {
                    sendPacketDirect(new C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SPRINTING));
                }
            }
        }
    }

    private boolean handleVerusCustomSend(Packet<?> packet) {
        // Cancel sprint packets when sprint disabler enabled
        if (verusSprint.isEnabled() && packet instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction c0b = (C0BPacketEntityAction) packet;
            if (c0b.getAction() == C0BPacketEntityAction.Action.START_SPRINTING ||
                c0b.getAction() == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                return false;
            }
        }
        return true;
    }

    // ==================== Verus2 (Rise) ====================

    private void handleVerus2Tick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        long now = System.currentTimeMillis();

        // Movement disabler
        if (verusMovement.isEnabled()) {
            lastHurtTime++;

            // Send position packets every 100 ticks
            if (player.ticksExisted % 100 == 0) {
                sendPacketDirect(new C03PacketPlayer.C04PacketPlayerPosition(
                    player.posX, player.posY, player.posZ, player.onGround));
                sendPacketDirect(new C03PacketPlayer.C04PacketPlayerPosition(
                    player.posX, -0.015625, player.posZ, false));
                sendPacketDirect(new C03PacketPlayer.C04PacketPlayerPosition(
                    player.posX, player.posY, player.posZ, player.onGround));
                teleported = true;
            }

            // Flush transactions periodically (500ms)
            if (now - lastFlushTime >= 500 && !transactionQueue.isEmpty()) {
                sendPacketDirect(transactionQueue.poll());
                lastFlushTime = now;
            }
        }

        // Reset hurt time
        if (player.hurtTime > 0) {
            lastHurtTime = 0;
        }

        // Flush keepalives periodically (400ms)
        if (now - lastFlushTime >= 400 && !keepAliveQueue.isEmpty()) {
            sendPacketDirect(keepAliveQueue.poll());
            lastFlushTime = now;
        }

        // Sprint disabler
        if (verusSprint.isEnabled() && isPlayerMoving()) {
            sendPacketDirect(new C0BPacketEntityAction(player,
                player.ticksExisted % 2 == 0 ? C0BPacketEntityAction.Action.STOP_SPRINTING :
                    C0BPacketEntityAction.Action.START_SPRINTING));
        }
    }

    private boolean handleVerus2Send(Packet<?> packet) {
        // Cancel sprint packets
        if (verusSprint.isEnabled() && packet instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction c0b = (C0BPacketEntityAction) packet;
            if (c0b.getAction() == C0BPacketEntityAction.Action.START_SPRINTING ||
                c0b.getAction() == C0BPacketEntityAction.Action.STOP_SPRINTING) {
                return false;
            }
        }

        // Movement disabler - queue transactions and keepalives
        if (verusMovement.isEnabled()) {
            if (packet instanceof C0FPacketConfirmTransaction) {
                transactionQueue.add(packet);
                if (transactionQueue.size() > 300) {
                    sendPacketDirect(transactionQueue.poll());
                }
                return false;
            }

            if (packet instanceof C00PacketKeepAlive) {
                keepAliveQueue.add(packet);
                return false;
            }
        }

        return true;
    }

    // ==================== Experimental (Rise) ====================

    private void handleExperimentalTick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        int tickDelay = (int) experimentalTickDelay.getValue();

        // Flush queued transactions every N ticks
        if (player.ticksExisted % tickDelay == 0 && !experimentalQueue.isEmpty()) {
            while (!experimentalQueue.isEmpty()) {
                sendPacketDirect(experimentalQueue.poll());
            }
            debug("Flushed experimental queue");
        }
    }

    private boolean handleExperimentalSend(Packet<?> packet) {
        // Queue transaction packets
        if (packet instanceof C0FPacketConfirmTransaction) {
            experimentalQueue.add(packet);
            return false;
        }
        return true;
    }

    // ==================== Ghostly (Rise) ====================

    private void handleGhostlyTick() {
        // Ghostly doesn't need tick handling, it works on packet send
    }

    private boolean handleGhostlySend(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03 = (C03PacketPlayer) packet;

            // Send C0CPacketInput before position packet
            sendPacketDirect(new C0CPacketInput());

            // Round Y position (like Rise: Math.round(y / 0.015625) * 0.015625)
            setPacketY(c03, Math.round(getPacketY(c03) / 0.015625) * 0.015625);

            // Set onGround to true
            setPacketOnGround(c03, true);
        }
        return true;
    }

    // ==================== SoloLegends (Abilities + Transaction) ====================

    private void handleSoloLegendsTick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        // Send flying abilities packet every 5 ticks (like Abilities mode)
        if (player.ticksExisted % 5 == 0) {
            PlayerCapabilities capabilities = new PlayerCapabilities();
            capabilities.isFlying = true;
            capabilities.allowFlying = true;  // Also set allowFlying for extra confusion
            sendPacketDirect(new C13PacketPlayerAbilities(capabilities));
        }

        // Flush queued transactions slowly to avoid timeout (every 20 ticks)
        if (player.ticksExisted % 20 == 0 && !transactionQueue.isEmpty()) {
            // Send a few at a time to maintain connection
            for (int i = 0; i < 3 && !transactionQueue.isEmpty(); i++) {
                sendPacketDirect(transactionQueue.poll());
            }
        }

        // Prevent queue from getting too large (causes kick)
        while (transactionQueue.size() > 150) {
            sendPacketDirect(transactionQueue.poll());
        }
    }

    private boolean handleSoloLegendsSend(Packet<?> packet) {
        // Queue transaction packets (creates desync)
        if (packet instanceof C0FPacketConfirmTransaction) {
            transactionQueue.add(packet);
            return false; // Cancel original
        }
        return true;
    }

    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;
        if (!(packet instanceof Packet)) return true;

        Packet<?> pkt = (Packet<?>) packet;
        String currentMode = mode.getSelected();

        switch (currentMode) {
            case MODE_VERUS_EXP:
                return handleVerusExperimentalReceive(pkt);

            case MODE_VIA:
                return handleViaVersionReceive(pkt);

            case MODE_VERUS:
            case MODE_VERUS_NEW:
                // Reset on world change
                if (pkt instanceof S07PacketRespawn || pkt instanceof S01PacketJoinGame) {
                    packetQueue.clear();
                    disabled = false;
                    debug("World change - cleared queue");
                }
                break;
        }

        return true;
    }

    // ==================== Verus Experimental ====================

    private void handleVerusExperimentalTick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        counter++;

        if (player.ridingEntity != null) {
            // Find nearby boats
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityBoat)) continue;

                double dist = player.getDistanceToEntity(entity);
                if (dist >= 5.0) continue;

                // Slow down timer while exploiting
                setTimerSpeed((float) verusTimer.getValue());

                if (entity == player.ridingEntity) {
                    continue;
                }

                // Auto weapon switch
                if (verusAutoWeapon.isEnabled()) {
                    int bestSlot = findBestWeaponSlot();
                    if (bestSlot == -1) {
                        debug("Need a weapon in hotbar!");
                        return;
                    }

                    if (bestSlot != player.inventory.currentItem) {
                        player.inventory.currentItem = bestSlot;
                        return;
                    }

                    ItemStack held = player.getCurrentEquippedItem();
                    if (held == null || getItemDamage(held) < 4.0) {
                        debug("Need better weapon!");
                        return;
                    }
                }

                // Attack riding entity and nearby boat (like Gothaj)
                player.swingItem();
                mc.playerController.attackEntity(player, player.ridingEntity);
                player.swingItem();
                mc.playerController.attackEntity(player, entity);

                debug("Attacked boats!");
            }
        }
    }

    private boolean handleVerusExperimentalSend(Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            // Modify onGround to false when riding or disabled
            if (mc.thePlayer.isRiding() || disabled) {
                setPacketOnGround((C03PacketPlayer) packet, false);
            }
        }
        return true;
    }

    private boolean handleVerusExperimentalReceive(Packet<?> packet) {
        // Reset on world change
        if (packet instanceof S07PacketRespawn || packet instanceof S01PacketJoinGame) {
            packetQueue.clear();
            counter = 0;
            disabled = false;
            debug("World change - reset");
        }

        // Check if riding entity was destroyed
        if (packet instanceof S13PacketDestroyEntities) {
            S13PacketDestroyEntities destroyPacket = (S13PacketDestroyEntities) packet;
            if (mc.thePlayer != null && mc.thePlayer.ridingEntity != null) {
                int[] ids = destroyPacket.getEntityIDs();
                if (ids.length != 100) {
                    for (int entityId : ids) {
                        if (entityId == mc.thePlayer.ridingEntity.getEntityId()) {
                            setTimerSpeed(1.0f);
                            debug("Verus has been disabled (entity destroyed)");
                            disabled = true;
                        }
                    }
                } else {
                    // Mass destroy - check if our entity is in it
                    for (int entityId : ids) {
                        if (entityId == mc.thePlayer.ridingEntity.getEntityId()) {
                            debug("Verus disabler failed (mass destroy)");
                        }
                    }
                }
            }
        }

        // Reset counter on attach
        if (packet instanceof S1BPacketEntityAttach) {
            S1BPacketEntityAttach attachPacket = (S1BPacketEntityAttach) packet;
            if (mc.thePlayer != null &&
                attachPacket.getEntityId() == mc.thePlayer.getEntityId() &&
                attachPacket.getVehicleEntityId() > 0) {
                counter = 0;
                debug("Attached to vehicle - reset counter");
            }
        }

        return true;
    }

    // ==================== Vulcan ====================

    private void handleVulcanTick() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        int digInterval = (int) vulcanDigInterval.getValue();
        int abortInterval = (int) vulcanAbortInterval.getValue();

        // Send dig packets to confuse anticheat (like Gothaj onMotion)
        if (player.ticksExisted % digInterval == 0) {
            sendPacketDirect(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.START_DESTROY_BLOCK,
                new BlockPos(player),
                EnumFacing.UP
            ));
        }

        if (player.ticksExisted % abortInterval == 0) {
            sendPacketDirect(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,
                BlockPos.ORIGIN,
                EnumFacing.DOWN
            ));
        }

        // Flush queued packets on odd ticks (like Gothaj)
        if (player.ticksExisted % 2 != 0) {
            flushAllPackets();
        }
    }

    private boolean handleVulcanSend(Packet<?> packet) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return true;

        // Queue packets on even ticks (like Gothaj)
        if (player.ticksExisted % 2 == 0) {
            if (packet instanceof C08PacketPlayerBlockPlacement ||
                packet instanceof C03PacketPlayer ||
                packet instanceof C0APacketAnimation ||
                packet instanceof C0BPacketEntityAction ||
                packet instanceof C02PacketUseEntity) {
                packetQueue.add(packet);
                return false; // Cancel original
            }
        }

        return true;
    }

    // ==================== Verus ====================

    private boolean handleVerusSend(Packet<?> packet) {
        // Queue transaction packets (like Gothaj)
        if (packet instanceof C0FPacketConfirmTransaction) {
            packetQueue.add(packet);
            return false; // Cancel original
        }
        return true;
    }

    // ==================== Verus New ====================

    private boolean handleVerusNewSend(Packet<?> packet) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return true;

        // Send water bucket placement packets to confuse anticheat (like Gothaj)
        sendPacketDirect(new C08PacketPlayerBlockPlacement(
            new BlockPos(player.posX, player.posY, player.posZ),
            255,
            new ItemStack(Items.water_bucket),
            0, 0.5f, 0
        ));

        sendPacketDirect(new C08PacketPlayerBlockPlacement(
            new BlockPos(player.posX, player.posY - 0.5, player.posZ),
            255,
            new ItemStack(Items.water_bucket),
            0, 0.5f, 0
        ));

        sendPacketDirect(new C08PacketPlayerBlockPlacement(
            new BlockPos(player.posX, player.posY - 1.5, player.posZ),
            1,
            new ItemStack(Blocks.stone),
            0, 0.94f, 0
        ));

        // Queue transaction packets
        if (packet instanceof C0FPacketConfirmTransaction) {
            packetQueue.add(packet);
            return false; // Cancel original
        }

        return true;
    }

    // ==================== Via Version ====================

    private boolean handleViaVersionReceive(Packet<?> packet) {
        if (packet instanceof S08PacketPlayerPosLook) {
            S08PacketPlayerPosLook posPacket = (S08PacketPlayerPosLook) packet;
            EntityPlayerSP player = mc.thePlayer;
            if (player == null) return true;

            // Manually handle position packet (like Gothaj)
            player.setPositionAndRotation(
                posPacket.getX(),
                posPacket.getY(),
                posPacket.getZ(),
                posPacket.getYaw(),
                posPacket.getPitch()
            );

            // Send response
            sendPacketDirect(new C03PacketPlayer.C06PacketPlayerPosLook(
                posPacket.getX(),
                posPacket.getY(),
                posPacket.getZ(),
                posPacket.getYaw(),
                posPacket.getPitch(),
                player.onGround
            ));

            debug("Via Version: handled position packet");
            return false; // Cancel original processing
        }
        return true;
    }

    // ==================== Helpers ====================

    private void updateSettingVisibility() {
        String currentMode = mode.getSelected();

        verusTimer.setVisible(MODE_VERUS_EXP.equals(currentMode));
        verusAutoWeapon.setVisible(MODE_VERUS_EXP.equals(currentMode));
        vulcanDigInterval.setVisible(MODE_VULCAN.equals(currentMode));
        vulcanAbortInterval.setVisible(MODE_VULCAN.equals(currentMode));
        maxQueueSize.setVisible(MODE_VERUS.equals(currentMode) || MODE_VERUS_NEW.equals(currentMode));

        // Verus Custom / Verus2 settings
        boolean isVerusCustomOrVerus2 = MODE_VERUS_CUSTOM.equals(currentMode) || MODE_VERUS2.equals(currentMode);
        verusMovement.setVisible(MODE_VERUS2.equals(currentMode));
        verusSprint.setVisible(isVerusCustomOrVerus2);
        verusDamage.setVisible(MODE_VERUS2.equals(currentMode));

        // Experimental settings
        experimentalTickDelay.setVisible(MODE_EXPERIMENTAL.equals(currentMode));
    }

    private int findBestWeaponSlot() {
        int bestSlot = -1;
        double bestDamage = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null) {
                double damage = getItemDamage(stack);
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private double getItemDamage(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;

        if (stack.getItem() instanceof ItemSword) {
            if (stack.getItem() == Items.wooden_sword) return 4;
            if (stack.getItem() == Items.stone_sword) return 5;
            if (stack.getItem() == Items.iron_sword) return 6;
            if (stack.getItem() == Items.golden_sword) return 4;
            if (stack.getItem() == Items.diamond_sword) return 7;
        }

        return 0;
    }

    private void setPacketOnGround(C03PacketPlayer packet, boolean value) {
        if (onGroundField == null) return;
        try {
            onGroundField.setBoolean(packet, value);
        } catch (Exception ignored) {}
    }

    private double getPacketY(C03PacketPlayer packet) {
        if (yField == null) return 0;
        try {
            return yField.getDouble(packet);
        } catch (Exception ignored) {}
        return 0;
    }

    private void setPacketY(C03PacketPlayer packet, double value) {
        if (yField == null) return;
        try {
            yField.setDouble(packet, value);
        } catch (Exception ignored) {}
    }

    private boolean isPlayerMoving() {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    private void flushQueue(Deque<Packet<?>> queue) {
        while (!queue.isEmpty()) {
            sendPacketDirect(queue.poll());
        }
    }

    private void setTimerSpeed(float speed) {
        if (timerField == null || timerSpeedField == null) return;
        try {
            Object timer = timerField.get(mc);
            if (timer != null) {
                timerSpeedField.setFloat(timer, speed);
            }
        } catch (Exception ignored) {}
    }

    private void sendPacketDirect(Packet<?> packet) {
        if (packet == null) return;
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        isSendingDirect = true;
        try {
            mc.thePlayer.sendQueue.addToSendQueue(packet);
        } finally {
            isSendingDirect = false;
        }
    }

    private void flushAllPackets() {
        while (!packetQueue.isEmpty()) {
            sendPacketDirect(packetQueue.poll());
        }
    }

    private void debug(String message) {
        if (debug.isEnabled() && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[§cDisabler§7] §f" + message));
        }
    }

    public static boolean isSendingDirectPacket() {
        return isSendingDirect;
    }

    @Override
    public String getDisplaySuffix() {
        String currentMode = mode.getSelected();
        int queueSize = packetQueue.size();
        if (queueSize > 0) {
            return " §7" + currentMode + " §c[" + queueSize + "]";
        }
        return " §7" + currentMode;
    }
}
