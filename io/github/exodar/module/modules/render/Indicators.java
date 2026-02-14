/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.module.modules.misc.Teams;
import io.github.exodar.spoof.SpoofManager;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;

import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Indicators - Shows directional indicators for incoming projectiles
 * Also alerts when players have dangerous items
 */
public class Indicators extends Module {

    // Projectile settings
    private SliderSetting scale;
    private SliderSetting offset;
    private TickSetting directionCheck;
    private TickSetting fireballs;
    private TickSetting pearls;
    private TickSetting arrows;
    private TickSetting bowAiming;

    // Player item alerts
    private TickSetting alertPearls;
    private TickSetting alertFireballs;
    private TickSetting alertGapples;
    private TickSetting alertArmor;
    private TickSetting alertDiamondSword;
    private TickSetting alertPotions;
    private TickSetting alertKBStick;
    private TickSetting alertTNT;

    // Enhanced notifications
    private TickSetting enhanceNotifications;
    private TickSetting soundNotifications;
    private TickSetting soundBowAiming;
    private TickSetting soundWeapons;
    private TickSetting soundPotions;
    private TickSetting soundPearls;
    private TickSetting soundFireballs;
    private TickSetting soundTNT;

    // Alert tracking for inventory items (alert once)
    private final Set<String> alertedPearl = new HashSet<>();
    private final Set<String> alertedFireball = new HashSet<>();
    private final Set<String> alertedGapple = new HashSet<>();
    private final Set<String> alertedArmor = new HashSet<>();
    private final Set<String> alertedDiamondSword = new HashSet<>();
    private final Set<String> alertedInvisibility = new HashSet<>();
    private final Set<String> alertedSpeed = new HashSet<>();
    private final Set<String> alertedJump = new HashSet<>();
    private final Set<String> alertedKBStick = new HashSet<>();

    // Alert cooldowns for held items (alert constantly with cooldown)
    private final java.util.Map<String, Long> heldItemAlertCooldowns = new java.util.HashMap<>();
    private static final long HELD_ITEM_ALERT_COOLDOWN = 3000; // 3 seconds between alerts for same player+item

    // Bow aiming tracking for sound alerts
    private final java.util.Map<String, Long> bowAimingSoundCooldowns = new java.util.HashMap<>();
    private static final long BOW_AIMING_SOUND_COOLDOWN = 2000; // 2 seconds between bow aiming sounds for same player

    // Track thrown pearls and drinking potions
    private final Set<Integer> alertedThrownPearls = new HashSet<>();
    private final java.util.Map<String, Long> drinkingPotionCooldowns = new java.util.HashMap<>();
    private static final long DRINKING_POTION_COOLDOWN = 5000; // 5 seconds cooldown for drinking alerts

    // Reflection for EntityArrow.inGround (private field)
    private static Field inGroundField = null;
    static {
        try {
            for (Field f : EntityArrow.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    String name = f.getName().toLowerCase();
                    if (name.contains("ground") || name.equals("d") || name.equals("inground")) {
                        f.setAccessible(true);
                        inGroundField = f;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private boolean isArrowInGround(EntityArrow arrow) {
        if (inGroundField == null) return false;
        try {
            return inGroundField.getBoolean(arrow);
        } catch (Exception e) {
            return false;
        }
    }

    public Indicators() {
        super("Indicators", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Projectile indicators"));
        this.registerSetting(scale = new SliderSetting("Scale", 1.0, 0.5, 1.5, 0.1));
        this.registerSetting(offset = new SliderSetting("Offset", 50.0, 0.0, 150.0, 5.0));
        this.registerSetting(directionCheck = new TickSetting("Direction Check", true));
        this.registerSetting(fireballs = new TickSetting("Fireballs", true));
        this.registerSetting(pearls = new TickSetting("Pearls", true));
        this.registerSetting(arrows = new TickSetting("Arrows", false));
        this.registerSetting(bowAiming = new TickSetting("Bow Aiming", true));

        this.registerSetting(new DescriptionSetting("--- Item Alerts ---"));
        this.registerSetting(alertPearls = new TickSetting("Alert Pearls", true));
        this.registerSetting(alertFireballs = new TickSetting("Alert Fireballs", true));
        this.registerSetting(alertGapples = new TickSetting("Alert Gapples", true));
        this.registerSetting(alertArmor = new TickSetting("Alert Armor", true));
        this.registerSetting(alertDiamondSword = new TickSetting("Alert Diamond Sword", true));
        this.registerSetting(alertPotions = new TickSetting("Alert Potions", true));
        this.registerSetting(alertKBStick = new TickSetting("Alert KB Stick", true));
        this.registerSetting(alertTNT = new TickSetting("Alert TNT", true));

        this.registerSetting(new DescriptionSetting("--- Notification Options ---"));
        this.registerSetting(enhanceNotifications = new TickSetting("Enhance Notifications", false));
        this.registerSetting(soundNotifications = new TickSetting("Sound Notifications", false));
        this.registerSetting(soundBowAiming = new TickSetting("Sound: Bow Aiming", true));
        this.registerSetting(soundWeapons = new TickSetting("Sound: KB Stick/Sword", true));
        this.registerSetting(soundPotions = new TickSetting("Sound: Invisibility", true));
        this.registerSetting(soundPearls = new TickSetting("Sound: Ender Pearl", true));
        this.registerSetting(soundFireballs = new TickSetting("Sound: Fireball", true));
        this.registerSetting(soundTNT = new TickSetting("Sound: TNT", true));
    }

    @Override
    public void onEnable() {
        alertedPearl.clear();
        alertedFireball.clear();
        alertedGapple.clear();
        alertedArmor.clear();
        alertedDiamondSword.clear();
        alertedInvisibility.clear();
        alertedSpeed.clear();
        alertedJump.clear();
        alertedKBStick.clear();
    }

    @Override
    public void onUpdate() {
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();

        // Check player held items (alert constantly with cooldown)
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (player.isDead) continue;
            if (AntiBot.isBotForVisuals(player)) continue;
            // Skip corpses when MurderMystery is enabled
            if (player.isPlayerSleeping() && io.github.exodar.module.modules.misc.MurderMystery.isInMurderMysteryGame()) continue;
            // Skip teammates
            Teams teams = Teams.getInstance();
            if (teams != null && teams.isEnabled() && teams.isTeamMate(player)) continue;

            String name = player.getName();
            String displayName = getSpoofedDisplayName(player);
            int distance = (int) mc.thePlayer.getDistanceToEntity(player);

            // Check HELD item (in hand)
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null) {
                Item item = heldItem.getItem();
                String itemKey = name + "_held_";

                // Ender Pearl in hand (alert every time with cooldown, like fireball)
                if (alertPearls.isEnabled() && item == Items.ender_pearl) {
                    String key = itemKey + "pearl";
                    Long lastAlert = heldItemAlertCooldowns.get(key);
                    if (lastAlert == null || now - lastAlert >= HELD_ITEM_ALERT_COOLDOWN) {
                        alertWithDistance(displayName, "\u00a77has \u00a75Ender Pearl", distance, "pearl");
                        alertedPearl.add(name);
                        heldItemAlertCooldowns.put(key, now);
                    }
                }

                // Fireball in hand
                if (alertFireballs.isEnabled() && item == Items.fire_charge) {
                    String key = itemKey + "fireball";
                    Long lastAlert = heldItemAlertCooldowns.get(key);
                    if (lastAlert == null || now - lastAlert >= HELD_ITEM_ALERT_COOLDOWN) {
                        alertWithDistance(displayName, "\u00a77has \u00a7cFireball", distance, "fireball");
                        alertedFireball.add(name);
                        heldItemAlertCooldowns.put(key, now);
                    }
                }

                // Knockback Stick in hand
                if (alertKBStick.isEnabled() && item == Items.stick) {
                    int kbLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, heldItem);
                    if (kbLevel > 0) {
                        String key = itemKey + "kbstick";
                        Long lastAlert = heldItemAlertCooldowns.get(key);
                        if (lastAlert == null || now - lastAlert >= HELD_ITEM_ALERT_COOLDOWN) {
                            if (!alertedKBStick.contains(name)) {
                                alertWithDistance(displayName, "\u00a77has \u00a76Knockback Stick", distance, "weapon");
                                alertedKBStick.add(name);
                            }
                            heldItemAlertCooldowns.put(key, now);
                        }
                    }
                }

                // TNT in hand
                if (alertTNT.isEnabled() && item == net.minecraft.item.Item.getItemFromBlock(Blocks.tnt)) {
                    String key = itemKey + "tnt";
                    Long lastAlert = heldItemAlertCooldowns.get(key);
                    if (lastAlert == null || now - lastAlert >= HELD_ITEM_ALERT_COOLDOWN) {
                        alertWithDistance(displayName, "\u00a77has \u00a7cTNT", distance, "tnt");
                        heldItemAlertCooldowns.put(key, now);
                    }
                }

                // Potion in hand (splash/throwable)
                if (alertPotions.isEnabled() && item instanceof ItemPotion) {
                    ItemPotion potion = (ItemPotion) item;
                    if (ItemPotion.isSplash(heldItem.getMetadata())) {
                        String key = itemKey + "potion";
                        Long lastAlert = heldItemAlertCooldowns.get(key);
                        if (lastAlert == null || now - lastAlert >= HELD_ITEM_ALERT_COOLDOWN) {
                            // Get potion name/type
                            java.util.List<PotionEffect> effects = potion.getEffects(heldItem);
                            String potionName = "Splash Potion";
                            String potionColor = "\u00a7d"; // Default pink
                            if (effects != null && !effects.isEmpty()) {
                                int potionId = effects.get(0).getPotionID();
                                if (potionId == Potion.harm.id) {
                                    potionName = "Harming";
                                    potionColor = "\u00a74";
                                } else if (potionId == Potion.poison.id) {
                                    potionName = "Poison";
                                    potionColor = "\u00a72";
                                } else if (potionId == Potion.moveSlowdown.id) {
                                    potionName = "Slowness";
                                    potionColor = "\u00a78";
                                } else if (potionId == Potion.weakness.id) {
                                    potionName = "Weakness";
                                    potionColor = "\u00a77";
                                }
                            }
                            alertWithDistance(displayName, "\u00a77has " + potionColor + potionName, distance);
                            heldItemAlertCooldowns.put(key, now);
                        }
                    }
                }
            }
        }

        // Check for thrown ender pearls
        if (alertPearls.isEnabled()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityEnderPearl)) continue;

                int entityId = entity.getEntityId();
                if (alertedThrownPearls.contains(entityId)) continue;

                EntityEnderPearl pearl = (EntityEnderPearl) entity;
                Entity thrower = pearl.getThrower();
                if (thrower == null || thrower == mc.thePlayer) continue;
                if (!(thrower instanceof EntityPlayer)) continue;

                EntityPlayer throwerPlayer = (EntityPlayer) thrower;
                if (AntiBot.isBotForVisuals(throwerPlayer)) continue;

                // Skip teammates
                Teams teams = Teams.getInstance();
                if (teams != null && teams.isEnabled() && teams.isTeamMate(throwerPlayer)) continue;

                String displayName = getSpoofedDisplayName(throwerPlayer);
                int distance = (int) mc.thePlayer.getDistanceToEntity(throwerPlayer);
                alertWithDistance(displayName, "\u00a77throws \u00a75Ender Pearl", distance, "pearl");
                alertedThrownPearls.add(entityId);
            }

            // Clean up old pearl IDs (remove pearls that no longer exist)
            alertedThrownPearls.removeIf(id -> mc.theWorld.getEntityByID(id) == null);
        }

        // Check for players with invisibility effect (drank potion)
        if (alertPotions.isEnabled()) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) continue;
                if (player.isDead) continue;
                if (AntiBot.isBotForVisuals(player)) continue;

                // Skip teammates
                Teams teams = Teams.getInstance();
                if (teams != null && teams.isEnabled() && teams.isTeamMate(player)) continue;

                // Check if player has invisibility effect active
                if (player.isPotionActive(Potion.invisibility)) {
                    String playerName = player.getName();
                    Long lastAlert = drinkingPotionCooldowns.get(playerName);
                    if (lastAlert == null || now - lastAlert >= DRINKING_POTION_COOLDOWN) {
                        String displayName = getSpoofedDisplayName(player);
                        int distance = (int) mc.thePlayer.getDistanceToEntity(player);
                        alertWithDistance(displayName, "\u00a77drinks \u00a7fInvisibility \u00a77potion", distance, "potion");
                        drinkingPotionCooldowns.put(playerName, now);
                    }
                }
            }
        }

        // Check player inventories for items (one-time alerts)
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (player.isDead) continue;
            if (AntiBot.isBotForVisuals(player)) continue;
            // Skip corpses when MurderMystery is enabled
            if (player.isPlayerSleeping() && io.github.exodar.module.modules.misc.MurderMystery.isInMurderMysteryGame()) continue;
            // Skip teammates
            Teams teams = Teams.getInstance();
            if (teams != null && teams.isEnabled() && teams.isTeamMate(player)) continue;

            String name = player.getName();
            String displayName = getSpoofedDisplayName(player);

            // Check inventory
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (stack == null) continue;

                Item item = stack.getItem();

                // Pearl check
                if (alertPearls.isEnabled() && !alertedPearl.contains(name) && item == Items.ender_pearl) {
                    alert(displayName, "\u00a77has \u00a75Ender Pearl", "pearl");
                    alertedPearl.add(name);
                }

                // Fireball check
                if (alertFireballs.isEnabled() && !alertedFireball.contains(name) && item == Items.fire_charge) {
                    alert(displayName, "\u00a77has \u00a7cFireball");
                    alertedFireball.add(name);
                }

                // Golden apple check
                if (alertGapples.isEnabled() && !alertedGapple.contains(name) && item == Items.golden_apple) {
                    alert(displayName, "\u00a77has \u00a76Golden Apple");
                    alertedGapple.add(name);
                }

                // Armor check (diamond or iron)
                if (alertArmor.isEnabled() && !alertedArmor.contains(name) && item instanceof ItemArmor) {
                    ItemArmor armor = (ItemArmor) item;
                    String armorMaterial = armor.getArmorMaterial().getName();
                    if (armorMaterial.contains("diamond")) {
                        alert(displayName, "\u00a77has \u00a7bDiamond Armor");
                        alertedArmor.add(name);
                    } else if (armorMaterial.contains("iron")) {
                        alert(displayName, "\u00a77has \u00a77Iron Armor");
                        alertedArmor.add(name);
                    }
                }

                // Diamond sword check
                if (alertDiamondSword.isEnabled() && !alertedDiamondSword.contains(name) && item == Items.diamond_sword) {
                    alert(displayName, "\u00a77has \u00a7bDiamond Sword", "weapon");
                    alertedDiamondSword.add(name);
                }

                // Knockback Stick check
                if (alertKBStick.isEnabled() && !alertedKBStick.contains(name) && item == Items.stick) {
                    int kbLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack);
                    if (kbLevel > 0) {
                        alert(displayName, "\u00a77has \u00a76Knockback Stick", "weapon");
                        alertedKBStick.add(name);
                    }
                }

                // Potion checks
                if (alertPotions.isEnabled() && item instanceof ItemPotion) {
                    ItemPotion potion = (ItemPotion) item;
                    java.util.List<PotionEffect> effects = potion.getEffects(stack);
                    if (effects != null) {
                        for (PotionEffect effect : effects) {
                            int potionId = effect.getPotionID();

                            // Invisibility (white color)
                            if (potionId == Potion.invisibility.id && !alertedInvisibility.contains(name)) {
                                alert(displayName, "\u00a77has \u00a7fInvisibility Potion", "potion");
                                alertedInvisibility.add(name);
                            }
                            // Speed
                            else if (potionId == Potion.moveSpeed.id && !alertedSpeed.contains(name)) {
                                alert(displayName, "\u00a77has \u00a7bSpeed Potion");
                                alertedSpeed.add(name);
                            }
                            // Jump boost
                            else if (potionId == Potion.jump.id && !alertedJump.contains(name)) {
                                alert(displayName, "\u00a77has \u00a7aJump Potion");
                                alertedJump.add(name);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get display name with spoof applied
     * Uses SpoofManager to apply spoofed names
     */
    private String getSpoofedDisplayName(EntityPlayer player) {
        String displayName = player.getDisplayName().getFormattedText();
        String playerName = player.getName();

        // Apply spoof if player is spoofed
        if (SpoofManager.isSpoofed(playerName)) {
            return SpoofManager.applySpoof(displayName);
        }
        return displayName;
    }

    private void alert(String displayName, String message) {
        alert(displayName, message, null);
    }

    private void alert(String displayName, String message, String soundCategory) {
        if (mc == null || mc.thePlayer == null) return;

        String fullMessage = displayName + " " + message;

        // Chat message
        mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
            "\u00a78[\u00a7dE\u00a78]\u00a7r " + fullMessage
        ));

        // Enhanced notification (Exodar alert popup)
        if (enhanceNotifications.isEnabled()) {
            // Strip color codes for notification
            String stripped = fullMessage.replaceAll("\u00a7.", "");
            io.github.exodar.ui.ModuleNotification.alert(stripped);
        }

        // Sound notification
        if (soundNotifications.isEnabled() && soundCategory != null) {
            playAlertSound(soundCategory);
        }
    }

    private void alertWithDistance(String displayName, String message, int distance) {
        alertWithDistance(displayName, message, distance, null);
    }

    private void alertWithDistance(String displayName, String message, int distance, String soundCategory) {
        if (mc == null || mc.thePlayer == null) return;

        String fullMessage = displayName + " " + message + " \u00a77(" + distance + "m)";

        // Chat message
        mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
            "\u00a78[\u00a7dE\u00a78]\u00a7r " + fullMessage
        ));

        // Enhanced notification (Exodar alert popup)
        if (enhanceNotifications.isEnabled()) {
            String stripped = fullMessage.replaceAll("\u00a7.", "");
            io.github.exodar.ui.ModuleNotification.alert(stripped);
        }

        // Sound notification
        if (soundNotifications.isEnabled() && soundCategory != null) {
            playAlertSound(soundCategory);
        }
    }

    private void playAlertSound(String category) {
        if (mc == null || mc.thePlayer == null) return;

        switch (category) {
            case "bow":
                if (soundBowAiming.isEnabled()) {
                    // Arrow sound for bow aiming (higher pitch)
                    mc.thePlayer.playSound("random.bow", 0.8f, 1.8f);
                }
                break;
            case "weapon":
                if (soundWeapons.isEnabled()) {
                    // Anvil sound for dangerous weapons
                    mc.thePlayer.playSound("random.anvil_land", 0.5f, 1.5f);
                }
                break;
            case "potion":
                if (soundPotions.isEnabled()) {
                    // Potion splash sound for invisibility
                    mc.thePlayer.playSound("game.potion.smash", 0.6f, 1.0f);
                }
                break;
            case "pearl":
                if (soundPearls.isEnabled()) {
                    // Enderman teleport sound for ender pearl
                    mc.thePlayer.playSound("mob.endermen.portal", 0.6f, 1.2f);
                }
                break;
            case "fireball":
                if (soundFireballs.isEnabled()) {
                    // Fire/blaze sound for fireball
                    mc.thePlayer.playSound("mob.ghast.fireball", 0.6f, 1.2f);
                }
                break;
            case "tnt":
                if (soundTNT.isEnabled()) {
                    // TNT fuse sound
                    mc.thePlayer.playSound("game.tnt.primed", 0.8f, 1.0f);
                }
                break;
        }
    }

    /**
     * Check if entity should be rendered as indicator
     */
    private boolean shouldRender(Entity entity) {
        if (mc == null || mc.thePlayer == null) return false;

        // Check entity type first
        boolean isValidType = false;
        if (fireballs.isEnabled() && entity instanceof EntityFireball) isValidType = true;
        else if (pearls.isEnabled() && entity instanceof EntityEnderPearl) isValidType = true;
        else if (arrows.isEnabled() && entity instanceof EntityArrow) {
            EntityArrow arrow = (EntityArrow) entity;
            // Don't show arrows that are in the ground
            if (isArrowInGround(arrow)) return false;
            isValidType = true;
        }

        if (!isValidType) return false;

        // Direction check - only show projectiles coming towards player
        if (directionCheck.isEnabled()) {
            // Use entity's motion vector instead of position delta
            // This is more stable and doesn't flicker
            double dx = entity.motionX;
            double dy = entity.motionY;
            double dz = entity.motionZ;

            // If motion is zero, fall back to position delta
            if (dx == 0 && dy == 0 && dz == 0) {
                dx = entity.posX - entity.lastTickPosX;
                dy = entity.posY - entity.lastTickPosY;
                dz = entity.posZ - entity.lastTickPosZ;
            }

            // Vector from entity to player
            double toPlayerX = mc.thePlayer.posX - entity.posX;
            double toPlayerY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - entity.posY - entity.height / 2.0;
            double toPlayerZ = mc.thePlayer.posZ - entity.posZ;

            // Dot product: positive means moving towards player
            double dot = dx * toPlayerX + dy * toPlayerY + dz * toPlayerZ;

            // Use small threshold to prevent flickering at edge cases
            if (dot <= 0.001) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get item to display for entity type
     */
    private Item getIndicatorItem(Entity entity) {
        if (entity instanceof EntityFireball) {
            return Items.fire_charge;
        }
        if (entity instanceof EntityEnderPearl) {
            return Items.ender_pearl;
        }
        if (entity instanceof EntityArrow) {
            return Items.arrow;
        }
        return Items.stick;
    }

    /**
     * Get color for entity type
     */
    private int getIndicatorColor(Entity entity) {
        if (entity instanceof EntityFireball) {
            return 0xFFFF6400; // Orange
        }
        if (entity instanceof EntityEnderPearl) {
            return 0xFF8000FF; // Purple
        }
        if (entity instanceof EntityArrow) {
            return 0xFF969696; // Gray
        }
        return 0xFFFFFFFF;
    }

    /**
     * Linear interpolation
     */
    private double lerp(double prev, double curr, float partialTicks) {
        return prev + (curr - prev) * partialTicks;
    }

    /**
     * Draw simple triangle arrow
     */
    private void drawTriangle(float x, float y, float rotation, float size, int color) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(rotation, 0, 0, 1);

        // Save GL state
        boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasTexture2DEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Filled triangle pointing up (tip at top)
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(0, -size);       // Top tip
        GL11.glVertex2f(-size * 0.6f, size * 0.5f);  // Bottom left
        GL11.glVertex2f(size * 0.6f, size * 0.5f);   // Bottom right
        GL11.glEnd();

        // Outline
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(0, 0, 0, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(0, -size);
        GL11.glVertex2f(-size * 0.6f, size * 0.5f);
        GL11.glVertex2f(size * 0.6f, size * 0.5f);
        GL11.glEnd();

        // Restore GL state
        GL11.glLineWidth(1.0f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        if (wasTexture2DEnabled) GL11.glEnable(GL11.GL_TEXTURE_2D);
        else GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (!wasBlendEnabled) GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    /**
     * Check if a player is aiming at the local player with a bow
     */
    private boolean isAimingAtPlayer(EntityPlayer player) {
        if (mc.thePlayer == null) return false;

        // Get player's look vector
        Vec3 lookVec = player.getLookVec();

        // Get vector from player to local player
        double dx = mc.thePlayer.posX - player.posX;
        double dy = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - (player.posY + player.getEyeHeight());
        double dz = mc.thePlayer.posZ - player.posZ;

        // Normalize
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.5) return true; // Very close

        dx /= dist;
        dy /= dist;
        dz /= dist;

        // Dot product - how aligned is their aim with direction to player
        double dot = lookVec.xCoord * dx + lookVec.yCoord * dy + lookVec.zCoord * dz;

        // Consider aiming if within ~20 degrees (cos(20) ≈ 0.94)
        return dot > 0.94;
    }

    /**
     * Get bow draw progress (0.0 - 1.0)
     */
    private float getBowDrawProgress(EntityPlayer player) {
        if (!player.isUsingItem()) return 0;

        ItemStack item = player.getItemInUse();
        if (item == null || !(item.getItem() instanceof ItemBow)) return 0;

        int useTime = player.getItemInUseCount();
        int maxUseTime = item.getMaxItemUseDuration();
        int drawTime = maxUseTime - useTime;

        // Bow takes 20 ticks to fully draw
        return Math.min(1.0f, drawTime / 20.0f);
    }

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!enabled) return;
        float partialTicks = event.getPartialTicks();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float scaleVal = (float) scale.getValue();
        float offsetVal = 10.0f + (float) offset.getValue();

        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        // Player position
        double playerX = lerp(mc.thePlayer.prevPosX, mc.thePlayer.posX, partialTicks);
        double playerZ = lerp(mc.thePlayer.prevPosZ, mc.thePlayer.posZ, partialTicks);

        // Check for players aiming at us with bow
        if (bowAiming.isEnabled()) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) continue;
                if (player.isDead) continue;
                if (AntiBot.isBotForVisuals(player)) continue;
                // Skip corpses when MurderMystery is enabled
                if (player.isPlayerSleeping() && io.github.exodar.module.modules.misc.MurderMystery.isInMurderMysteryGame()) continue;
                // Skip teammates
                Teams teams = Teams.getInstance();
                if (teams != null && teams.isEnabled() && teams.isTeamMate(player)) continue;

                float drawProgress = getBowDrawProgress(player);
                if (drawProgress > 0.1f && isAimingAtPlayer(player)) {
                    // Alert for bow aiming (sound + chat)
                    String playerName = player.getName();
                    long now = System.currentTimeMillis();
                    Long lastSound = bowAimingSoundCooldowns.get(playerName);
                    if (lastSound == null || now - lastSound >= BOW_AIMING_SOUND_COOLDOWN) {
                        // Chat alert with distance
                        int distance = (int) mc.thePlayer.getDistanceToEntity(player);
                        String displayName = getSpoofedDisplayName(player);
                        alertWithDistance(displayName, "\u00a77aims \u00a76Bow", distance, "bow");
                        bowAimingSoundCooldowns.put(playerName, now);
                    }

                    // Player is drawing bow and aiming at us
                    double entityX = lerp(player.prevPosX, player.posX, partialTicks);
                    double entityZ = lerp(player.prevPosZ, player.posZ, partialTicks);

                    double dx = entityX - playerX;
                    double dz = entityZ - playerZ;
                    float angleToEntity = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float cameraYaw = mc.thePlayer.rotationYaw;
                    if (mc.gameSettings.thirdPersonView == 2) cameraYaw += 180.0f;
                    float relativeAngle = angleToEntity - cameraYaw;
                    float rad = (float) Math.toRadians(relativeAngle);

                    float indicatorX = centerX + (float) Math.sin(rad) * offsetVal * scaleVal;
                    float indicatorY = centerY - (float) Math.cos(rad) * offsetVal * scaleVal;

                    // Color based on draw progress (yellow -> red as bow draws)
                    int red = 255;
                    int green = (int) (255 * (1.0f - drawProgress));
                    int color = 0xFF000000 | (red << 16) | (green << 8);

                    GlStateManager.pushMatrix();
                    GlStateManager.disableDepth();

                    // Draw arrow indicator
                    drawTriangle(indicatorX, indicatorY, relativeAngle, 8.0f * scaleVal, color);

                    // Draw bow icon
                    float iconX = centerX + (float) Math.sin(rad) * (offsetVal - 15) * scaleVal - 8;
                    float iconY = centerY - (float) Math.cos(rad) * (offsetVal - 15) * scaleVal - 8;
                    mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(Items.bow), (int) iconX, (int) iconY);

                    // Draw draw progress bar
                    float barWidth = 16 * drawProgress;
                    float barX = iconX;
                    float barY = iconY + 18;
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glColor4f(red / 255f, green / 255f, 0, 0.8f);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(barX, barY);
                    GL11.glVertex2f(barX + barWidth, barY);
                    GL11.glVertex2f(barX + barWidth, barY + 2);
                    GL11.glVertex2f(barX, barY + 2);
                    GL11.glEnd();
                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Reset color
                    GL11.glEnable(GL11.GL_TEXTURE_2D);

                    // Draw distance
                    int dist = (int) mc.thePlayer.getDistanceToEntity(player);
                    String distStr = dist + "m";
                    float textX = centerX + (float) Math.sin(rad) * (offsetVal + 12) * scaleVal - mc.fontRendererObj.getStringWidth(distStr) / 2.0f;
                    float textY = centerY - (float) Math.cos(rad) * (offsetVal + 12) * scaleVal - 4;
                    mc.fontRendererObj.drawStringWithShadow(distStr, textX, textY, color);

                    GlStateManager.enableDepth();
                    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                    GlStateManager.popMatrix();
                }
            }
        }

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!shouldRender(entity)) continue;

            // Entity position
            double entityX = lerp(entity.prevPosX, entity.posX, partialTicks);
            double entityZ = lerp(entity.prevPosZ, entity.posZ, partialTicks);

            // Angle from player to entity
            double dx = entityX - playerX;
            double dz = entityZ - playerZ;
            float angleToEntity = (float) Math.toDegrees(Math.atan2(-dx, dz));

            // Player's camera yaw
            float cameraYaw = mc.thePlayer.rotationYaw;
            if (mc.gameSettings.thirdPersonView == 2) {
                cameraYaw += 180.0f;
            }

            // Relative angle (where entity is relative to where player is looking)
            float relativeAngle = angleToEntity - cameraYaw;

            // Convert to radians for positioning
            float rad = (float) Math.toRadians(relativeAngle);
            float indicatorX = centerX + (float) Math.sin(rad) * offsetVal * scaleVal;
            float indicatorY = centerY - (float) Math.cos(rad) * offsetVal * scaleVal;

            // Arrow rotation (points toward entity direction from center)
            float arrowRotation = relativeAngle;

            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();

            // Draw arrow
            drawTriangle(indicatorX, indicatorY, arrowRotation, 8.0f * scaleVal, getIndicatorColor(entity));

            // Draw item icon (offset from arrow)
            float iconX = centerX + (float) Math.sin(rad) * (offsetVal - 15) * scaleVal - 8;
            float iconY = centerY - (float) Math.cos(rad) * (offsetVal - 15) * scaleVal - 8;
            mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(getIndicatorItem(entity)), (int) iconX, (int) iconY);

            // Draw distance
            int dist = (int) mc.thePlayer.getDistanceToEntity(entity);
            String distStr = dist + "m";
            float textX = centerX + (float) Math.sin(rad) * (offsetVal + 12) * scaleVal - mc.fontRendererObj.getStringWidth(distStr) / 2.0f;
            float textY = centerY - (float) Math.cos(rad) * (offsetVal + 12) * scaleVal - 4;
            mc.fontRendererObj.drawStringWithShadow(distStr, textX, textY, 0xAAFFFFFF);

            GlStateManager.enableDepth();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
