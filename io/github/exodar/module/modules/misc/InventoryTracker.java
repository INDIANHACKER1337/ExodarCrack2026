/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.TickSetting;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.ChatComponentText;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InventoryTracker - Tracks equipment of other players
 * Used by .invsee command to view what items players have held/worn
 */
public class InventoryTracker extends Module {

    private static InventoryTracker instance;

    private TickSetting trackArmor;
    private TickSetting trackHeldItems;
    private TickSetting saveOfflinePlayers;
    private TickSetting debug;

    // Track equipment per player UUID
    // Key: UUID, Value: TrackedInventory
    private static final Map<UUID, TrackedInventory> trackedPlayers = new ConcurrentHashMap<>();

    // Keep reference to players that left render distance (for viewing later)
    private static final Map<UUID, String> playerNames = new ConcurrentHashMap<>();

    public InventoryTracker() {
        super("InventoryTracker", ModuleCategory.MISC);
        instance = this;

        this.registerSetting(new DescriptionSetting("Track player equipment"));
        this.registerSetting(trackArmor = new TickSetting("Track Armor", true));
        this.registerSetting(trackHeldItems = new TickSetting("Track Held Items", true));
        this.registerSetting(saveOfflinePlayers = new TickSetting("Save Offline", true));
        this.registerSetting(debug = new TickSetting("Debug", false));
    }

    public static InventoryTracker getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        debug("InventoryTracker enabled - tracking player equipment");
    }

    @Override
    public void onDisable() {
        if (!saveOfflinePlayers.isEnabled()) {
            trackedPlayers.clear();
            playerNames.clear();
        }
        debug("InventoryTracker disabled");
    }

    /**
     * Called for ALL received packets (even when module disabled for core tracking)
     * This is a static method that Main.java can call directly
     */
    public static void onEquipmentPacket(Object packet) {
        if (instance == null || !instance.isEnabled()) return;

        if (packet instanceof S04PacketEntityEquipment) {
            instance.handleEquipmentPacket((S04PacketEntityEquipment) packet);
        }
    }

    /**
     * Handle equipment change packet
     */
    private void handleEquipmentPacket(S04PacketEntityEquipment packet) {
        if (mc.theWorld == null) return;

        int entityId = packet.getEntityID();
        int slot = packet.getEquipmentSlot();
        ItemStack itemStack = packet.getItemStack();

        // Find the entity
        Entity entity = mc.theWorld.getEntityByID(entityId);
        if (entity == null || !(entity instanceof EntityPlayer)) return;
        if (entity == mc.thePlayer) return; // Don't track self

        EntityPlayer player = (EntityPlayer) entity;
        UUID uuid = player.getUniqueID();

        // Get or create tracked inventory
        TrackedInventory tracked = trackedPlayers.computeIfAbsent(uuid, k -> new TrackedInventory());
        playerNames.put(uuid, player.getName());

        // Update the slot
        // Slot 0 = held item, 1-4 = armor (boots, legs, chest, helmet)
        if (slot == 0 && trackHeldItems.isEnabled()) {
            tracked.updateHeldItem(itemStack);
            debug("§7" + player.getName() + " §fheld: " + getItemName(itemStack));
        } else if (slot >= 1 && slot <= 4 && trackArmor.isEnabled()) {
            tracked.updateArmor(slot - 1, itemStack);
            debug("§7" + player.getName() + " §farmor[" + (slot-1) + "]: " + getItemName(itemStack));
        }
    }

    @Override
    public boolean onReceivePacket(Object packet) {
        // Skip during world changes
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            return true;
        }

        // Handle equipment packets
        if (packet instanceof S04PacketEntityEquipment) {
            handleEquipmentPacket((S04PacketEntityEquipment) packet);
        }
        return true; // Don't cancel
    }

    /**
     * Get tracked inventory for a player by name
     */
    public static TrackedInventory getTrackedInventory(String playerName) {
        // Find UUID by name
        for (Map.Entry<UUID, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(playerName)) {
                return trackedPlayers.get(entry.getKey());
            }
        }
        return null;
    }

    /**
     * Get tracked inventory for a player by UUID
     */
    public static TrackedInventory getTrackedInventory(UUID uuid) {
        return trackedPlayers.get(uuid);
    }

    /**
     * Get player name from UUID
     */
    public static String getPlayerName(UUID uuid) {
        return playerNames.get(uuid);
    }

    /**
     * Get UUID from player name
     */
    public static UUID getPlayerUUID(String name) {
        for (Map.Entry<UUID, String> entry : playerNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Check if we have data for a player
     */
    public static boolean hasData(String playerName) {
        return getTrackedInventory(playerName) != null;
    }

    /**
     * Get all tracked player names
     */
    public static Map<UUID, String> getTrackedPlayerNames() {
        return new HashMap<>(playerNames);
    }

    /**
     * Clear all tracked data
     */
    public static void clearAll() {
        trackedPlayers.clear();
        playerNames.clear();
    }

    private String getItemName(ItemStack stack) {
        if (stack == null) return "Empty";
        return stack.getDisplayName();
    }

    private void debug(String message) {
        if (debug != null && debug.isEnabled() && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[§eInvTracker§7] §f" + message));
        }
    }

    @Override
    public String getDisplaySuffix() {
        return " §7[" + trackedPlayers.size() + "]";
    }

    /**
     * Tracked inventory data for a player
     */
    public static class TrackedInventory {
        // Armor slots: 0=boots, 1=legs, 2=chest, 3=helmet
        private final ItemStack[] armor = new ItemStack[4];

        // Held items history (most recent first)
        private final ItemStack[] heldHistory = new ItemStack[36];
        private int heldHistoryIndex = 0;

        // Current held item
        private ItemStack currentHeld = null;

        // Timestamps
        private final long[] armorTimestamps = new long[4];
        private long heldTimestamp = 0;

        public void updateArmor(int slot, ItemStack stack) {
            if (slot >= 0 && slot < 4) {
                armor[slot] = stack != null ? stack.copy() : null;
                armorTimestamps[slot] = System.currentTimeMillis();
            }
        }

        public void updateHeldItem(ItemStack stack) {
            // Add to history if different from current
            if (stack != null && !ItemStack.areItemStacksEqual(stack, currentHeld)) {
                // Shift history
                if (heldHistoryIndex < heldHistory.length - 1) {
                    heldHistory[heldHistoryIndex++] = stack.copy();
                } else {
                    // Shift array
                    System.arraycopy(heldHistory, 1, heldHistory, 0, heldHistory.length - 1);
                    heldHistory[heldHistory.length - 1] = stack.copy();
                }
            }
            currentHeld = stack != null ? stack.copy() : null;
            heldTimestamp = System.currentTimeMillis();
        }

        public ItemStack getArmor(int slot) {
            if (slot >= 0 && slot < 4) return armor[slot];
            return null;
        }

        public ItemStack getHelmet() { return armor[3]; }
        public ItemStack getChestplate() { return armor[2]; }
        public ItemStack getLeggings() { return armor[1]; }
        public ItemStack getBoots() { return armor[0]; }

        public ItemStack getCurrentHeld() { return currentHeld; }

        public ItemStack[] getHeldHistory() { return heldHistory; }
        public int getHeldHistoryCount() { return heldHistoryIndex; }

        public long getArmorTimestamp(int slot) {
            if (slot >= 0 && slot < 4) return armorTimestamps[slot];
            return 0;
        }

        public long getHeldTimestamp() { return heldTimestamp; }

        public boolean hasAnyData() {
            if (currentHeld != null) return true;
            for (ItemStack stack : armor) {
                if (stack != null) return true;
            }
            return false;
        }
    }
}
