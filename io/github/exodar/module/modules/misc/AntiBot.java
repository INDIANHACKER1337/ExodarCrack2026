/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * AntiBot - Filters players that are likely bots/NPCs
 * Affects Combat modules (AimAssist, Reach, etc.) and Visual modules (ESP, Nametags, etc.)
 */
public class AntiBot extends Module {

    // Checks
    private final TickSetting checkNPCName;
    private final TickSetting checkInvisible;
    private final TickSetting checkInAir;
    private final TickSetting checkStationary;
    private final TickSetting checkEntityAge;
    private final SliderSetting minEntityAge;

    // Apply to
    private final TickSetting affectCombat;
    private final TickSetting affectVisuals;

    // Singleton instance
    private static AntiBot instance;

    // Track player states - use WeakHashMap so dead entities get cleaned up
    private final Map<EntityPlayer, PlayerState> playerStates = new WeakHashMap<>();

    private static class PlayerState {
        boolean wasEverVisible = false;
        boolean wasEverOnGround = false;
        boolean hasEverMoved = false;
        double firstX, firstY, firstZ;
        long firstSeenTime;
        boolean initialized = false;

        PlayerState(EntityPlayer player) {
            this.firstX = player.posX;
            this.firstY = player.posY;
            this.firstZ = player.posZ;
            this.firstSeenTime = System.currentTimeMillis();
            this.initialized = true;
        }
    }

    public AntiBot() {
        super("AntiBot", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Filter bots/NPCs"));

        // Checks
        this.registerSetting(new DescriptionSetting("--- Checks ---"));
        this.registerSetting(checkNPCName = new TickSetting("[NPC] in Name", true));
        this.registerSetting(checkInvisible = new TickSetting("Always Invisible", true));
        this.registerSetting(checkInAir = new TickSetting("Always In Air", true));
        this.registerSetting(checkStationary = new TickSetting("Always Stationary", true));
        this.registerSetting(checkEntityAge = new TickSetting("Entity Age", true));
        this.registerSetting(minEntityAge = new SliderSetting("Min Age (ms)", 1000, 100, 5000, 100));

        // Apply to
        this.registerSetting(new DescriptionSetting("--- Apply To ---"));
        this.registerSetting(affectCombat = new TickSetting("Combat", true));
        this.registerSetting(affectVisuals = new TickSetting("Visuals", true));
    }

    public static AntiBot getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        playerStates.clear();
    }

    @Override
    public void onDisable() {
        playerStates.clear();
    }

    @Override
    public void onUpdate() {
        if (mc.theWorld == null) return;

        // Update player states
        for (Object obj : mc.theWorld.playerEntities) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) continue;

            PlayerState state = playerStates.get(player);
            if (state == null) {
                state = new PlayerState(player);
                playerStates.put(player, state);
            }

            // Track if player was ever visible
            if (!player.isInvisible()) {
                state.wasEverVisible = true;
            }

            // Track if player was ever on ground
            if (player.onGround) {
                state.wasEverOnGround = true;
            }

            // Track if player has ever moved
            double dx = Math.abs(player.posX - state.firstX);
            double dy = Math.abs(player.posY - state.firstY);
            double dz = Math.abs(player.posZ - state.firstZ);
            if (dx > 0.1 || dy > 0.1 || dz > 0.1) {
                state.hasEverMoved = true;
            }
        }
    }

    /**
     * Check if entity is likely a bot
     * Used by Combat and Visual modules
     */
    public boolean isBot(Entity entity) {
        if (!isEnabled()) return false;
        if (entity == null) return false;
        if (!(entity instanceof EntityPlayer)) return false;
        if (entity == mc.thePlayer) return false;

        EntityPlayer player = (EntityPlayer) entity;

        // Check: [NPC] in name (doesn't need tracking state)
        if (checkNPCName.isEnabled()) {
            String name = player.getName();
            if (name != null && name.contains("[NPC]")) {
                return true;
            }
        }

        PlayerState state = playerStates.get(player);

        // If we haven't tracked this player yet, don't filter
        if (state == null || !state.initialized) return false;

        // Check: Always invisible
        if (checkInvisible.isEnabled()) {
            if (!state.wasEverVisible && player.isInvisible()) {
                return true;
            }
        }

        // Check: Always in air
        if (checkInAir.isEnabled()) {
            if (!state.wasEverOnGround && !player.onGround) {
                return true;
            }
        }

        // Check: Always stationary
        if (checkStationary.isEnabled()) {
            if (!state.hasEverMoved) {
                return true;
            }
        }

        // Check: Entity age (spawned recently)
        if (checkEntityAge.isEnabled()) {
            long age = System.currentTimeMillis() - state.firstSeenTime;
            if (age < (long) minEntityAge.getValue()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if AntiBot should affect combat modules
     */
    public boolean shouldAffectCombat() {
        return isEnabled() && affectCombat.isEnabled();
    }

    /**
     * Check if AntiBot should affect visual modules
     */
    public boolean shouldAffectVisuals() {
        return isEnabled() && affectVisuals.isEnabled();
    }

    /**
     * Static helper for other modules to check if entity is a bot for combat
     */
    public static boolean isBotForCombat(Entity entity) {
        if (instance == null) return false;
        if (!instance.shouldAffectCombat()) return false;
        return instance.isBot(entity);
    }

    /**
     * Static helper for other modules to check if entity is a bot for visuals
     */
    public static boolean isBotForVisuals(Entity entity) {
        if (instance == null) return false;
        if (!instance.shouldAffectVisuals()) return false;
        return instance.isBot(entity);
    }

    @Override
    public String getDisplaySuffix() {
        return "";
    }
}
