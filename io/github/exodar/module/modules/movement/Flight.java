/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.movement;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.Timer;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/**
 * Flight - Fly in survival mode
 *
 * Modes:
 * - Creative: Uses capabilities
 * - Vanilla: Direct motion control
 * - Verus: Damage-based fly for Verus anticheat
 * - VerusGlide: Glide fly for Verus anticheat
 */
public class Flight extends Module {

    // Reflection fields
    private static Field timerField = null;
    private static Field timerSpeedField = null;
    private static Field onGroundField = null;
    private static boolean reflectionInitialized = false;

    // Mode constants
    private static final String MODE_CREATIVE = "Creative";
    private static final String MODE_VANILLA = "Vanilla";
    private static final String MODE_SOLO_LEGEND = "SoloLegend";
    private static final String MODE_VERUS = "Verus";
    private static final String MODE_VERUS_GLIDE = "VerusGlide";
    private static final String MODE_VERUS_FAST = "VerusFast";
    private static final String MODE_DAMAGE = "Damage";
    private static final String MODE_VULCAN = "Vulcan2";
    private static final String MODE_VULCAN_AABB = "VulcanAABB";

    // Settings
    private final ModeSetting mode;
    private final SliderSetting speed;
    private final SliderSetting verticalSpeed;

    // Verus settings
    private final TickSetting verusDamage;
    private final SliderSetting verusBoostSpeed;
    private final TickSetting verusTimerSlow;

    // VerusFast settings
    private final SliderSetting verusFastGroundSpeed;
    private final SliderSetting verusFastAirSpeed;
    private final SliderSetting verusFastJumpInterval;

    // Damage settings
    private final SliderSetting damageSpeed;
    private final SliderSetting damageHeight;

    // Vulcan settings
    private final SliderSetting vulcanSpeed;
    private final SliderSetting vulcanTimerSpeed;

    // State
    private int boostTicks = 0;
    private boolean wasOnGround = false;

    // SoloLegend mode state
    private int vanillaState = 0; // 0=searching feather, 1=using feather, 2=waiting boost, 3=flying
    private int originalSlot = -1;
    private int featherSlot = -1;
    private int waitTicks = 0;
    private double lastMotionY = 0;
    private boolean cooldownDetected = false;

    // VerusFast state
    private int verusFastTicks = 0;

    // Vulcan state (from Rise 6.2.4)
    private int vulcanTicks = 0;
    private boolean vulcanShouldTimer = false;

    public Flight() {
        super("Flight", ModuleCategory.MOVEMENT);

        this.registerSetting(new DescriptionSetting("Fly in survival"));
        this.registerSetting(mode = new ModeSetting("Mode", new String[]{
            MODE_CREATIVE, MODE_VANILLA, MODE_SOLO_LEGEND, MODE_VERUS, MODE_VERUS_GLIDE, MODE_VERUS_FAST, MODE_DAMAGE,
            MODE_VULCAN, MODE_VULCAN_AABB
        }));
        this.registerSetting(speed = new SliderSetting("Speed", 1.0, 0.1, 5.0, 0.1));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical Speed", 1.0, 0.1, 5.0, 0.1));

        // Verus specific settings
        this.registerSetting(new DescriptionSetting("-- Verus Mode --"));
        this.registerSetting(verusDamage = new TickSetting("Damage", true));
        this.registerSetting(verusBoostSpeed = new SliderSetting("Boost Speed", 0.5, 0.1, 2.0, 0.05));
        this.registerSetting(verusTimerSlow = new TickSetting("Timer Slow", true));

        // VerusFast settings (from Rise)
        this.registerSetting(new DescriptionSetting("-- VerusFast --"));
        this.registerSetting(verusFastGroundSpeed = new SliderSetting("Ground Speed", 1.01, 0.5, 2.0, 0.01));
        this.registerSetting(verusFastAirSpeed = new SliderSetting("Air Speed", 0.41, 0.2, 1.0, 0.01));
        this.registerSetting(verusFastJumpInterval = new SliderSetting("Jump Interval", 14, 5, 30, 1));

        // Damage settings (from Rise)
        this.registerSetting(new DescriptionSetting("-- Damage --"));
        this.registerSetting(damageSpeed = new SliderSetting("Fly Speed", 1.0, 0.1, 9.5, 0.1));
        this.registerSetting(damageHeight = new SliderSetting("Damage Height", 3.42, 2.0, 5.0, 0.1));

        // Vulcan settings
        this.registerSetting(new DescriptionSetting("-- Vulcan --"));
        this.registerSetting(vulcanSpeed = new SliderSetting("Vulcan Speed", 1.0, 0.5, 5.0, 0.1));
        this.registerSetting(vulcanTimerSpeed = new SliderSetting("Timer Speed", 2.0, 1.5, 5.0, 0.5));

        // Set initial visibility
        updateSettingsVisibility();
        mode.onChange(this::updateSettingsVisibility);

        initReflection();
    }

    private void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            // Find timer field in Minecraft
            for (Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == Timer.class) {
                    f.setAccessible(true);
                    timerField = f;
                    break;
                }
            }

            // Find timerSpeed field in Timer
            if (timerField != null) {
                for (Field f : Timer.class.getDeclaredFields()) {
                    if (f.getType() == float.class) {
                        String name = f.getName().toLowerCase();
                        if (name.contains("speed") || name.equals("field_74278_d")) {
                            f.setAccessible(true);
                            timerSpeedField = f;
                            break;
                        }
                    }
                }
                // Fallback - first float field
                if (timerSpeedField == null) {
                    for (Field f : Timer.class.getDeclaredFields()) {
                        if (f.getType() == float.class) {
                            f.setAccessible(true);
                            timerSpeedField = f;
                            break;
                        }
                    }
                }
            }

            // Find onGround field in C03PacketPlayer
            for (Field f : C03PacketPlayer.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    String name = f.getName().toLowerCase();
                    if (name.contains("ground") || name.equals("field_149474_g")) {
                        f.setAccessible(true);
                        onGroundField = f;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    private void setTimerSpeed(float speed) {
        try {
            if (timerField != null && timerSpeedField != null) {
                Object timer = timerField.get(mc);
                if (timer != null) {
                    timerSpeedField.set(timer, speed);
                }
            }
        } catch (Exception e) {
            // Silent
        }
    }

    private void setPacketOnGround(C03PacketPlayer packet, boolean onGround) {
        try {
            if (onGroundField != null) {
                onGroundField.set(packet, onGround);
            }
        } catch (Exception e) {
            // Silent
        }
    }

    private void updateSettingsVisibility() {
        String selected = mode.getSelected();
        boolean isVerus = selected.equals(MODE_VERUS);
        boolean isVerusFast = selected.equals(MODE_VERUS_FAST);
        boolean isDamage = selected.equals(MODE_DAMAGE);
        boolean showSpeedSettings = selected.equals(MODE_VANILLA) ||
                                    selected.equals(MODE_CREATIVE) ||
                                    selected.equals(MODE_SOLO_LEGEND);

        speed.setVisible(showSpeedSettings);
        verticalSpeed.setVisible(showSpeedSettings);

        // Verus settings
        verusDamage.setVisible(isVerus);
        verusBoostSpeed.setVisible(isVerus);
        verusTimerSlow.setVisible(isVerus);

        // VerusFast settings
        verusFastGroundSpeed.setVisible(isVerusFast);
        verusFastAirSpeed.setVisible(isVerusFast);
        verusFastJumpInterval.setVisible(isVerusFast);

        // Damage settings
        damageSpeed.setVisible(isDamage);
        damageHeight.setVisible(isDamage);

        // Vulcan settings
        boolean isVulcan = selected.equals(MODE_VULCAN) || selected.equals(MODE_VULCAN_AABB);
        vulcanSpeed.setVisible(selected.equals(MODE_VULCAN));
        vulcanTimerSpeed.setVisible(selected.equals(MODE_VULCAN_AABB));
    }

    @Override
    public void onEnable() {
        EntityPlayerSP player = getPlayer();
        if (player == null) return;

        boostTicks = 0;
        wasOnGround = player.onGround;

        // Reset SoloLegend state
        vanillaState = 0;
        originalSlot = -1;
        featherSlot = -1;
        waitTicks = 0;
        lastMotionY = 0;
        cooldownDetected = false;

        String selectedMode = mode.getSelected();

        if (selectedMode.equals(MODE_VERUS)) {
            handleVerusEnable(player);
        } else if (selectedMode.equals(MODE_DAMAGE)) {
            handleDamageEnable(player);
        } else if (selectedMode.equals(MODE_VERUS_FAST)) {
            verusFastTicks = 0;
        } else if (selectedMode.equals(MODE_VULCAN)) {
            handleVulcanEnable(player);
        } else if (selectedMode.equals(MODE_VULCAN_AABB)) {
            vulcanShouldTimer = false;
        }
    }

    /**
     * Vulcan enable - sends initial teleport packet
     */
    private void handleVulcanEnable(EntityPlayerSP player) {
        vulcanTicks = 0;
        try {
            if (player.sendQueue != null) {
                // Send initial teleport 2 blocks below
                player.sendQueue.addToSendQueue(
                    new C03PacketPlayer.C06PacketPlayerPosLook(
                        player.posX, player.posY - 2, player.posZ,
                        player.rotationYaw, player.rotationPitch, false
                    )
                );
            }
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Damage enable - sends damage packets
     */
    private void handleDamageEnable(EntityPlayerSP player) {
        try {
            double damageHeightValue = damageHeight.getValue();

            // Check if there's room above
            if (getWorld().getCollidingBoundingBoxes(player,
                    player.getEntityBoundingBox().offset(0.0, damageHeightValue + 0.001, 0.0)).isEmpty()) {

                // Send position packet to teleport up (takes fall damage)
                if (player.sendQueue != null) {
                    C03PacketPlayer.C04PacketPlayerPosition posPacket =
                        new C03PacketPlayer.C04PacketPlayerPosition(
                            player.posX, player.posY + damageHeightValue + 0.001, player.posZ, false);
                    player.sendQueue.addToSendQueue(posPacket);

                    // Send position back
                    C03PacketPlayer.C06PacketPlayerPosLook posLook =
                        new C03PacketPlayer.C06PacketPlayerPosLook(
                            player.posX, player.posY, player.posZ,
                            player.rotationYaw, player.rotationPitch, true);
                    player.sendQueue.addToSendQueue(posLook);
                }
            }

            // Boost player up slightly
            player.setPosition(player.posX, player.posY + 0.1, player.posZ);
        } catch (Exception e) {
            // Silent
        }
    }

    @Override
    public void onDisable() {
        EntityPlayerSP player = getPlayer();
        if (player != null) {
            // Reset creative flight capabilities
            player.capabilities.isFlying = false;
            if (!player.capabilities.isCreativeMode) {
                player.capabilities.allowFlying = false;
            }

            // Reset fly speed to default
            player.capabilities.setFlySpeed(0.05f);

            // Send abilities to server to confirm we stopped flying
            player.sendPlayerAbilities();

            // Reset motion and timer
            if (player.motionY > 0) {
                player.motionY = 0;
            }
            setTimerSpeed(1.0f);

            // Restore original slot if we changed it
            if (originalSlot != -1 && player.inventory.currentItem != originalSlot) {
                player.inventory.currentItem = originalSlot;
                if (player.sendQueue != null) {
                    player.sendQueue.addToSendQueue(new C09PacketHeldItemChange(originalSlot));
                }
            }
        }

        // Reset SoloLegend state
        vanillaState = 0;
        originalSlot = -1;
        featherSlot = -1;
        cooldownDetected = false;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7" + mode.getSelected();
    }

    @Override
    public void onUpdate() {
        EntityPlayerSP player = getPlayer();
        if (player == null || getWorld() == null) {
            setEnabled(false);
            return;
        }

        String selectedMode = mode.getSelected();

        switch (selectedMode) {
            case MODE_CREATIVE:
                handleCreativeMode(player);
                break;
            case MODE_VANILLA:
                handleSimpleVanillaMode(player);
                break;
            case MODE_SOLO_LEGEND:
                handleSoloLegendMode(player);
                break;
            case MODE_VERUS:
                handleVerusMode(player);
                break;
            case MODE_VERUS_GLIDE:
                handleVerusGlideMode(player);
                break;
            case MODE_VERUS_FAST:
                handleVerusFastMode(player);
                break;
            case MODE_DAMAGE:
                handleDamageMode(player);
                break;
            case MODE_VULCAN:
                handleVulcanFly(player);
                break;
            case MODE_VULCAN_AABB:
                handleVulcanAABBFly(player);
                break;
        }
    }

    /**
     * Called when outgoing packet is sent
     * @return true to allow, false to cancel
     */
    @Override
    public boolean onSendPacket(Object packet) {
        if (!enabled) return true;

        // Verus mode: spoof onGround in movement packets
        if (mode.getSelected().equals(MODE_VERUS)) {
            if (packet instanceof C03PacketPlayer) {
                C03PacketPlayer c03 = (C03PacketPlayer) packet;
                setPacketOnGround(c03, true);
            }
        }

        return true;
    }

    /**
     * Called when incoming packet is received
     * Used to detect cooldown messages for SoloLegend mode
     */
    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled) return true;

        // Only for SoloLegend mode
        if (!mode.getSelected().equals(MODE_SOLO_LEGEND)) return true;

        // Detect chat messages for cooldown
        if (packet instanceof S02PacketChat) {
            try {
                S02PacketChat chatPacket = (S02PacketChat) packet;
                IChatComponent chatComponent = chatPacket.getChatComponent();
                if (chatComponent != null) {
                    String message = chatComponent.getUnformattedText().toLowerCase();

                    // Common cooldown messages - check for keywords
                    // Adjust these based on the actual server messages
                    if (message.contains("cooldown") || message.contains("espera") ||
                        message.contains("wait") || message.contains("segundos") ||
                        message.contains("seconds") || message.contains("enfriamiento")) {

                        // Cooldown detected while waiting for boost
                        if (vanillaState == 2) {
                            cooldownDetected = true;
                            sendChat("§c[Flight] Cooldown detected! Disabling...");
                            setEnabled(false);
                        }
                    }
                }
            } catch (Exception e) {
                // Silent
            }
        }

        return true;
    }

    private void handleCreativeMode(EntityPlayerSP player) {
        double hSpeed = speed.getValue();
        double vSpeed = verticalSpeed.getValue();

        player.capabilities.isFlying = true;
        player.capabilities.allowFlying = true;
        player.capabilities.setFlySpeed((float) (0.05 * hSpeed));

        // Hover by default
        player.motionY = 0.0;

        // Vertical movement
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
            player.motionY = 0.5 * vSpeed;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            player.motionY = -0.5 * vSpeed;
        }
    }

    /**
     * Vanilla fly mode - Uses capabilities like C++ version
     * Sets allowFlying and isFlying, then lets Minecraft handle the flight
     */
    private void handleSimpleVanillaMode(EntityPlayerSP player) {
        double hSpeed = speed.getValue();

        // Enable flight capabilities (like C++ version)
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;

        // Set fly speed (0.05 = normal Minecraft fly speed)
        float actualSpeed = (float) (0.05f * hSpeed);
        player.capabilities.setFlySpeed(actualSpeed);

        // Send abilities to server so it knows we're flying
        player.sendPlayerAbilities();

        // Minecraft handles the actual flight movement automatically
        // We just need to keep capabilities updated
    }

    /**
     * SoloLegend mode - feather-based flight
     */
    private void handleSoloLegendMode(EntityPlayerSP player) {
        // State machine for feather-based flight
        switch (vanillaState) {
            case 0: // Searching for feather
                featherSlot = findFeatherInHotbar(player);
                if (featherSlot == -1) {
                    // No feather found, disable
                    sendChat("§c[Flight] No feather found in hotbar!");
                    setEnabled(false);
                    return;
                }
                // Save original slot and switch to feather
                originalSlot = player.inventory.currentItem;
                if (originalSlot != featherSlot) {
                    player.inventory.currentItem = featherSlot;
                    if (player.sendQueue != null) {
                        player.sendQueue.addToSendQueue(new C09PacketHeldItemChange(featherSlot));
                    }
                }
                vanillaState = 1;
                waitTicks = 2; // Wait a bit before using
                break;

            case 1: // Using feather (right click)
                if (waitTicks > 0) {
                    waitTicks--;
                    return;
                }
                // Use the feather (right click)
                useCurrentItem(player);

                // Immediately return to original slot
                if (originalSlot != -1 && originalSlot != featherSlot) {
                    player.inventory.currentItem = originalSlot;
                    if (player.sendQueue != null) {
                        player.sendQueue.addToSendQueue(new C09PacketHeldItemChange(originalSlot));
                    }
                }

                vanillaState = 2;
                waitTicks = 5; // Wait for boost detection
                lastMotionY = player.motionY;
                break;

            case 2: // Waiting for boost
                waitTicks--;

                // Detect significant boost (much stronger than knockback)
                // Normal jump = 0.42, knockback = 0.3-0.5, feather boost should be 1.0+
                if (player.motionY > 1.0) {
                    // Significant boost detected! Start flying
                    vanillaState = 3;
                    sendChat("§a[Flight] Boost detected! Flying...");
                    return;
                }

                lastMotionY = player.motionY;

                // Timeout - feather might be on cooldown
                if (waitTicks <= -40) { // Wait up to ~2 seconds
                    sendChat("§c[Flight] Feather cooldown or failed to boost!");
                    setEnabled(false);
                    return;
                }
                break;

            case 3: // Flying!
                // Check if touched ground - disable instantly
                if (player.onGround) {
                    setEnabled(false);
                    return;
                }

                double hSpeed = speed.getValue();
                double vSpeed = verticalSpeed.getValue();

                // Hover by default
                player.motionY = 0;
                player.fallDistance = 0;

                // Vertical movement
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                    player.motionY = vSpeed;
                }
                if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                    player.motionY = -vSpeed;
                }

                // Horizontal movement
                strafe(player, (float) hSpeed);
                break;
        }
    }

    /**
     * Find feather in hotbar (slots 0-8)
     * @return slot index or -1 if not found
     */
    private int findFeatherInHotbar(EntityPlayerSP player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.feather) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Use current held item (right click)
     */
    private void useCurrentItem(EntityPlayerSP player) {
        try {
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && player.sendQueue != null) {
                // Send use item packet
                C08PacketPlayerBlockPlacement packet = new C08PacketPlayerBlockPlacement(heldItem);
                player.sendQueue.addToSendQueue(packet);
            }
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Send chat message to player
     */
    private void sendChat(String message) {
        try {
            EntityPlayerSP player = getPlayer();
            if (player != null) {
                player.addChatMessage(new net.minecraft.util.ChatComponentText(message));
            }
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Verus fly mode - damage-based
     */
    private void handleVerusEnable(EntityPlayerSP player) {
        try {
            // Check if there's room above
            if (getWorld().getCollidingBoundingBoxes(player,
                    player.getEntityBoundingBox().offset(0.0, 3.0001, 0.0)).isEmpty()) {

                // Send damage packet (teleport up then back)
                if (verusDamage.isEnabled() && player.sendQueue != null) {
                    C03PacketPlayer.C04PacketPlayerPosition posPacket =
                        new C03PacketPlayer.C04PacketPlayerPosition(
                            player.posX, player.posY + 3.0001, player.posZ, false);
                    player.sendQueue.addToSendQueue(posPacket);
                }

                // Send position look packets
                if (player.sendQueue != null) {
                    C03PacketPlayer.C06PacketPlayerPosLook posLook1 =
                        new C03PacketPlayer.C06PacketPlayerPosLook(
                            player.posX, player.posY, player.posZ,
                            player.rotationYaw, player.rotationPitch, false);
                    player.sendQueue.addToSendQueue(posLook1);

                    C03PacketPlayer.C06PacketPlayerPosLook posLook2 =
                        new C03PacketPlayer.C06PacketPlayerPosLook(
                            player.posX, player.posY, player.posZ,
                            player.rotationYaw, player.rotationPitch, true);
                    player.sendQueue.addToSendQueue(posLook2);
                }
            }

            // Boost player up slightly
            player.setPosition(player.posX, player.posY + 0.1, player.posZ);
        } catch (Exception e) {
            // Silent
        }
    }

    private void handleVerusMode(EntityPlayerSP player) {
        // Stop all motion
        player.motionX = 0;
        player.motionY = 0;
        player.motionZ = 0;

        // Check for damage to start boost
        if (boostTicks == 0 && player.hurtTime > 0) {
            boostTicks = 20; // Boost for 20 ticks
        }

        if (boostTicks > 0) {
            boostTicks--;
        }

        // Timer slow for bypass
        if (verusTimerSlow.isEnabled()) {
            if (player.ticksExisted % 3 == 0) {
                setTimerSpeed(0.15f);
            } else {
                setTimerSpeed(0.08f);
            }
        }

        // Apply strafe movement
        float boostSpeed = (float) verusBoostSpeed.getValue();
        strafe(player, boostSpeed);
    }

    /**
     * VerusGlide - Glide fly for Verus anticheat
     */
    private void handleVerusGlideMode(EntityPlayerSP player) {
        // Skip if in liquid or on ladder
        if (player.isInWater() || player.isInLava() || player.isOnLadder()) {
            return;
        }

        // Only glide when falling and has fallen enough
        if (!player.onGround && player.fallDistance > 1) {
            // Glide - set motionY to slow fall
            player.motionY = -0.09800000190734863;

            // Apply horizontal strafe
            boolean diagonal = player.movementInput.moveForward != 0 && player.movementInput.moveStrafe != 0;
            if (diagonal) {
                strafe(player, 0.334f);
            } else {
                strafe(player, 0.3345f);
            }
        }
    }

    /**
     * VerusFast - Fast fly mode from Rise's VerusFlight
     * Uses AABB modification technique (simplified version)
     */
    private void handleVerusFastMode(EntityPlayerSP player) {
        verusFastTicks++;

        // When pressing jump, go up every 2 ticks
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
            if (player.ticksExisted % 2 == 0) {
                player.motionY = 0.42F;
            }
        }

        // Get speed potion amplifier
        double speedAmp = 0;
        if (player.isPotionActive(net.minecraft.potion.Potion.moveSpeed)) {
            speedAmp = player.getActivePotionEffect(net.minecraft.potion.Potion.moveSpeed).getAmplifier() + 1;
        }

        int jumpInterval = (int) verusFastJumpInterval.getValue();

        // Every jumpInterval ticks when on ground, apply boost
        if (player.onGround && verusFastTicks % jumpInterval == 0) {
            player.motionY = 0.42F;
            strafe(player, 0.69f);
            // Adjust motionY to stay at same level
            player.motionY = -(player.posY - Math.floor(player.posY));
        } else {
            // Normal movement
            if (player.onGround) {
                double groundSpeed = verusFastGroundSpeed.getValue() + (speedAmp * 0.15);
                strafe(player, (float) groundSpeed);
            } else {
                double airSpeed = verusFastAirSpeed.getValue() + (speedAmp * 0.05);
                strafe(player, (float) airSpeed);
            }
        }

        // Force sprint
        player.setSprinting(true);
    }

    /**
     * Damage - Damage-based fly from Rise
     * Damages player on enable for velocity then flies
     */
    private void handleDamageMode(EntityPlayerSP player) {
        float flySpeed = (float) damageSpeed.getValue();

        // Vertical movement
        player.motionY = 0.0D
                + (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) ? flySpeed : 0.0D)
                - (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) ? flySpeed : 0.0D);

        // Horizontal movement
        strafe(player, flySpeed);

        // Prevent fall damage
        player.fallDistance = 0;
    }

    /**
     * Vulcan fly - Rise 6.2.4 style
     * Sends initial teleport, uses near-zero motionY
     */
    private void handleVulcanFly(EntityPlayerSP player) {
        float flySpeed = (float) vulcanSpeed.getValue();

        // Almost zero motionY to avoid fall detection
        player.motionY = -1E-10D
            + (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) ? flySpeed : 0.0D)
            - (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) ? flySpeed : 0.0D);

        // Simple fly - just strafe and control motion
        strafe(player, flySpeed);

        vulcanTicks++;
        // Auto-disable after 80 ticks (4 seconds)
        if (vulcanTicks >= 80) {
            player.motionX = 0;
            player.motionY = 0;
            player.motionZ = 0;
            setEnabled(false);
        }

        player.fallDistance = 0;
    }

    /**
     * VulcanAABB fly - Raven XD style
     * Cancels teleport packets
     */
    private void handleVulcanAABBFly(EntityPlayerSP player) {
        // Apply timer if teleport was cancelled
        if (vulcanShouldTimer) {
            double randomOffset = Math.random() / 1000;
            setTimerSpeed((float) (vulcanTimerSpeed.getValue() - randomOffset));
        }

        // Hover
        player.motionY = 0;
        player.fallDistance = 0;

        // Vertical
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
            player.motionY = 0.5;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            player.motionY = -0.5;
        }

        // Horizontal
        if (isMovingFlight(player)) {
            strafe(player, 0.5f);
        } else {
            player.motionX = 0;
            player.motionZ = 0;
        }
    }

    private boolean isMovingFlight(EntityPlayerSP player) {
        return player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0;
    }

    /**
     * Strafe - Apply movement in player's direction
     */
    private void strafe(EntityPlayerSP player, float speed) {
        float forward = player.movementInput.moveForward;
        float strafe = player.movementInput.moveStrafe;

        if (forward == 0 && strafe == 0) {
            player.motionX = 0;
            player.motionZ = 0;
            return;
        }

        // Normalize diagonal movement
        if (forward != 0 && strafe != 0) {
            forward *= 0.7071067811865476f;
            strafe *= 0.7071067811865476f;
        }

        float yaw = player.rotationYaw;
        double yawRadians = Math.toRadians(yaw);

        player.motionX = (forward * -Math.sin(yawRadians) + strafe * Math.cos(yawRadians)) * speed;
        player.motionZ = (forward * Math.cos(yawRadians) + strafe * Math.sin(yawRadians)) * speed;
    }
}
