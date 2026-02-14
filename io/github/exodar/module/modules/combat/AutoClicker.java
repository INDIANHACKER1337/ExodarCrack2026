/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.combat;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoClicker extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random rand = new Random();

    // Settings
    private SliderSetting targetCPS;
    private SliderSetting minCPS;
    private SliderSetting maxCPS;
    private TickSetting useAdvancedCPS;
    private TickSetting exhaustation;  // When ON: current system with variation. When OFF: constant CPS like Raven
    private TickSetting weaponOnly;
    private TickSetting ignoreBlocks;
    private TickSetting inventoryClicker;

    // Thread control
    private Thread clickThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Click tracking (kept for potential future use)
    private int clicksSinceStart = 0;
    private long sessionStart = 0;
    private int consecutiveClicks = 0;

    // Cached fields
    private Field thePlayerField = null;
    private Field theWorldField = null;
    private Field currentScreenField = null;
    private Field gameSettingsField = null;
    private Field keyBindAttackField = null;

    public AutoClicker() {
        super("AutoClicker", ModuleCategory.COMBAT);

        this.registerSetting(new DescriptionSetting("Auto click when holding mouse"));
        this.registerSetting(useAdvancedCPS = new TickSetting("Advanced CPS", false));
        this.registerSetting(targetCPS = new SliderSetting("Target CPS", 12.0, 1.0, 30.0, 0.5));
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 7.0, 1.0, 30.0, 0.5));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 14.0, 1.0, 30.0, 0.5));
        this.registerSetting(exhaustation = new TickSetting("Exhaustation", true));  // ON = variation/spikes, OFF = constant CPS
        this.registerSetting(weaponOnly = new TickSetting("Weapon only", false));
        this.registerSetting(ignoreBlocks = new TickSetting("Ignore Blocks", false));
        this.registerSetting(inventoryClicker = new TickSetting("Inventory Clicker", false));

        // Initially hide Min/Max CPS (only show when Advanced is enabled)
        minCPS.setVisible(false);
        maxCPS.setVisible(false);

        initFields();
    }

    private void initFields() {
        try {
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String type = f.getType().getName();
                if (type.contains("EntityPlayer")) {
                    thePlayerField = f;
                } else if (type.contains("World")) {
                    theWorldField = f;
                } else if (type.contains("GuiScreen")) {
                    currentScreenField = f;
                } else if (type.contains("GameSettings")) {
                    gameSettingsField = f;
                }
            }

            if (gameSettingsField != null) {
                Object settings = gameSettingsField.get(mc);
                if (settings != null) {
                    for (Field f : settings.getClass().getDeclaredFields()) {
                        if (f.getType() == KeyBinding.class) {
                            f.setAccessible(true);
                            KeyBinding kb = (KeyBinding) f.get(settings);
                            if (kb != null && kb.getKeyDescription().contains("attack")) {
                                keyBindAttackField = f;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[AutoClicker] Error initializing fields: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        System.out.println("[AutoClicker] Module ENABLED");

        // Kill previous thread if still running
        if (clickThread != null && clickThread.isAlive()) {
            running.set(false);
            clickThread.interrupt();
            try { clickThread.join(100); } catch (Exception e) {}
        }

        running.set(true);
        clicksSinceStart = 0;
        consecutiveClicks = 0;
        sessionStart = System.currentTimeMillis();

        clickThread = new Thread(() -> {
            while (running.get()) {
                try {
                    if (!enabled || mc == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    EntityPlayer player = (EntityPlayer) thePlayerField.get(mc);
                    Object world = theWorldField.get(mc);
                    Object screen = currentScreenField.get(mc);

                    boolean isInInventory = screen != null;

                    // Skip ClickGUI and other non-inventory screens
                    // Inventory Clicker only works in survival/creative inventory, NOT in chests/furnaces/etc
                    boolean isClickGui = screen != null && screen.getClass().getName().contains("ClickGui");
                    String screenName = screen != null ? screen.getClass().getSimpleName() : "";
                    boolean isPlayerInventory = screenName.equals("GuiInventory") || screenName.equals("GuiContainerCreative");
                    boolean shouldClickInventory = isInInventory && inventoryClicker.isEnabled() && isPlayerInventory && !isClickGui;

                    // Skip if no player/world, or if screen is open and inventory clicker is disabled
                    if (player == null || world == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    if (isInInventory && !shouldClickInventory) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Weapon only check (only for combat, not inventory)
                    if (!shouldClickInventory && weaponOnly.isEnabled() && !isHoldingWeapon(player)) {
                        Thread.sleep(50);
                        continue;
                    }

                    boolean mouseDown = org.lwjgl.input.Mouse.isButtonDown(0);

                    if (!mouseDown) {
                        clicksSinceStart = 0;
                        consecutiveClicks = 0;
                        sessionStart = System.currentTimeMillis();
                        Thread.sleep(50);
                        continue;
                    }

                    // Block check (only for combat, not inventory)
                    if (!shouldClickInventory && !ignoreBlocks.isEnabled()) {
                        if (isLookingAtBlock()) {
                            Thread.sleep(50);
                            continue;
                        }
                    }

                    // Determine CPS based on mode
                    int effectiveMinCPS;
                    int effectiveMaxCPS;

                    if (useAdvancedCPS.isEnabled()) {
                        // Advanced mode: use min/max directly
                        effectiveMinCPS = (int) minCPS.getValue();
                        effectiveMaxCPS = (int) maxCPS.getValue();
                    } else {
                        // Simple mode: target +/-2
                        int target = (int) targetCPS.getValue();
                        effectiveMinCPS = Math.max(1, target - 2);
                        effectiveMaxCPS = Math.min(30, target + 2);
                    }

                    // Select random CPS in range
                    int targetCps = effectiveMinCPS + rand.nextInt(Math.max(1, effectiveMaxCPS - effectiveMinCPS + 1));
                    int baseDelay = 1000 / targetCps;

                    int finalDelay;

                    if (exhaustation.isEnabled()) {
                        // EXHAUSTATION MODE: Variation/spikes/outliers (original system)

                        // AGGRESSIVE VARIATION (+/-50%)
                        int variation = rand.nextInt(baseDelay) - (baseDelay / 2);
                        finalDelay = baseDelay + variation;

                        // OUTLIERS: 15% probability of extreme clicks
                        if (rand.nextInt(100) < 15) {
                            if (rand.nextBoolean()) {
                                // Very fast click
                                finalDelay = 25 + rand.nextInt(16); // 25-40ms
                            } else {
                                // Very slow click
                                finalDelay = 150 + rand.nextInt(101); // 150-250ms
                            }
                        }

                        // SPIKES: 8% probability of sudden changes
                        if (rand.nextInt(100) < 8) {
                            int spikeMult = 50 + rand.nextInt(151); // 50-200%
                            finalDelay = (finalDelay * spikeMult) / 100;
                        }

                        // MICRO-STUTTERS: 10% probability
                        if (rand.nextInt(100) < 10) {
                            finalDelay += 10 + rand.nextInt(26); // +10-35ms
                        }
                    } else {
                        // CONSTANT CPS MODE (Raven style): Simple 1000/cps delay
                        // Just small jitter +/-10ms for slight human variation
                        finalDelay = baseDelay + (rand.nextInt(21) - 10);
                    }

                    // Ensure minimum
                    finalDelay = Math.max(finalDelay, 33);

                    // Variable click duration (12-28ms)
                    int clickDuration = calculateClickDuration();

                    // Inventory clicking mode
                    if (shouldClickInventory) {
                        // Simulate mouse click for inventory
                        try {
                            // Use reflection to call mouseClicked on the GuiScreen
                            java.lang.reflect.Method mouseClicked = null;
                            Class<?> screenClass = screen.getClass();

                            // Find mouseClicked method (protected, need to search up hierarchy)
                            while (screenClass != null && mouseClicked == null) {
                                try {
                                    mouseClicked = screenClass.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
                                } catch (NoSuchMethodException e) {
                                    screenClass = screenClass.getSuperclass();
                                }
                            }

                            if (mouseClicked != null) {
                                mouseClicked.setAccessible(true);
                                int mouseX = org.lwjgl.input.Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
                                int mouseY = mc.currentScreen.height - org.lwjgl.input.Mouse.getY() * mc.currentScreen.height / mc.displayHeight - 1;
                                mouseClicked.invoke(screen, mouseX, mouseY, 0); // 0 = left click
                            }
                        } catch (Exception e) {
                            // Silent
                        }
                    } else {
                        // Normal combat clicking
                        // Wait if AutoBlock is currently blocking (but NOT during lagging - we can attack during lag)
                        while (AutoBlock.isCurrentlyBlockingStatic() || BlockHit.isBlocking()) {
                            Thread.sleep(5);
                        }

                        // HitSelect check - Pause mode blocks the swing
                        if (!HitSelect.canSwing()) {
                            Thread.sleep(10);
                            continue;
                        }

                        // Perform click using KeyBinding
                        Object settings = gameSettingsField.get(mc);
                        KeyBinding attackKey = (KeyBinding) keyBindAttackField.get(settings);
                        if (attackKey != null) {
                            int keyCode = attackKey.getKeyCode();

                            KeyBinding.setKeyBindState(keyCode, true);
                            KeyBinding.onTick(keyCode);

                            // Notify AutoBlock that we attacked - it will start blocking
                            AutoBlock.notifyAttack();

                            // Hold click for duration
                            Thread.sleep(clickDuration);

                            KeyBinding.setKeyBindState(keyCode, false);

                            // Wait while AutoBlock is blocking
                            while (AutoBlock.isCurrentlyBlockingStatic() || BlockHit.isBlocking()) {
                                Thread.sleep(5);
                            }
                        }
                    }

                    // Update counters
                    clicksSinceStart++;
                    consecutiveClicks++;

                    // Wait for next click
                    int actualDelay = finalDelay - clickDuration;
                    if (actualDelay > 0) {
                        Thread.sleep(actualDelay);
                    }

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.out.println("[AutoClicker] Error: " + e.getMessage());
                }
            }
        }, "MC-InputHandler");

        clickThread.setDaemon(true);
        clickThread.start();
        System.out.println("[AutoClicker] Thread started");
    }

    @Override
    public void onDisable() {
        System.out.println("[AutoClicker] Module DISABLED");
        running.set(false);

        if (clickThread != null) {
            clickThread.interrupt();
            try {
                clickThread.join(100);
            } catch (InterruptedException e) {
                System.out.println("[AutoClicker] Error stopping thread: " + e.getMessage());
            }
        }

        try {
            Object settings = gameSettingsField.get(mc);
            KeyBinding attackKey = (KeyBinding) keyBindAttackField.get(settings);
            if (attackKey != null) {
                KeyBinding.setKeyBindState(attackKey.getKeyCode(), false);
            }
        } catch (Exception e) {
            System.out.println("[AutoClicker] Error releasing key: " + e.getMessage());
        }
    }

    @Override
    public void onUpdate() {
        // Update visibility of settings based on Advanced CPS
        boolean advanced = useAdvancedCPS.isEnabled();

        // If Advanced is enabled: show Min/Max, hide Target
        // If Advanced is disabled: show Target, hide Min/Max
        targetCPS.setVisible(!advanced);
        minCPS.setVisible(advanced);
        maxCPS.setVisible(advanced);
    }

    /**
     * Click duration with human distribution (12-28ms)
     */
    private int calculateClickDuration() {
        double roll = rand.nextDouble();

        if (roll < 0.70) {
            // Tight center (70%) - 16-22ms
            return 16 + rand.nextInt(7);
        } else if (roll < 0.90) {
            // Shorter (20%) - 12-15ms
            return 12 + rand.nextInt(4);
        } else {
            // Longer (10%) - 23-28ms
            return 23 + rand.nextInt(6);
        }
    }

    private boolean isHoldingWeapon(EntityPlayer player) {
        try {
            net.minecraft.item.ItemStack held = player.getHeldItem();
            if (held == null) return false;
            String itemName = held.getUnlocalizedName().toLowerCase();
            if (itemName.contains("sword") || itemName.contains("axe")) return true;
            // Check for KB stick
            if (itemName.contains("stick")) {
                int kbLevel = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(
                    net.minecraft.enchantment.Enchantment.knockback.effectId, held);
                if (kbLevel > 0) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLookingAtBlock() {
        try {
            if (mc.objectMouseOver == null) return false;

            Field typeOfHitField = mc.objectMouseOver.getClass().getDeclaredField("typeOfHit");
            typeOfHitField.setAccessible(true);
            Object typeOfHit = typeOfHitField.get(mc.objectMouseOver);

            return typeOfHit != null && typeOfHit.toString().equals("BLOCK");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDisplaySuffix() {
        if (useAdvancedCPS.isEnabled()) {
            // Advanced mode: show "15-18"
            return " \u00a77" + String.format("%.0f-%.0f", minCPS.getValue(), maxCPS.getValue());
        } else {
            // Target mode: show "15"
            return " \u00a77" + String.format("%.0f", targetCPS.getValue());
        }
    }
}
