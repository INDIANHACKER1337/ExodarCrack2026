/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module;

import io.github.exodar.event.EventBus;
import io.github.exodar.setting.Setting;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class Module {
    public enum BindMode {
        TOGGLE,  // Press to toggle on/off
        HOLD     // Hold to keep enabled, release to disable
    }

    protected String name;
    protected volatile boolean enabled;  // volatile for thread-safe access
    protected Minecraft mc;
    protected List<Setting> settings;
    protected ModuleCategory category;
    protected int keyCode = 0; // 0 = no keybind (legacy)
    protected int toggleBind = 0; // 0 = no bind
    protected int holdBind = 0; // 0 = no bind
    protected BindMode currentBindMode = BindMode.TOGGLE;
    protected boolean hidden = false; // Hidden from ClickGUI

    public Module(String name, ModuleCategory category) {
        // System.out.println("[Module] Constructor called for: " + name);
        this.name = name;
        // System.out.println("[Module] Name set");
        this.category = category;
        // System.out.println("[Module] Category set");
        this.enabled = false;
        // System.out.println("[Module] Enabled set to false");
        try {
            // System.out.println("[Module] Getting Minecraft instance...");
            this.mc = Minecraft.getMinecraft();
            // System.out.println("[Module] Minecraft instance obtained: " + this.mc);
        } catch (Exception e) {
            // System.out.println("[Module] Warning: Could not get Minecraft instance for " + name + ": " + e.getMessage());
            e.printStackTrace();
            this.mc = null;
        }
        // System.out.println("[Module] Creating settings list...");
        this.settings = new ArrayList<>();
        // System.out.println("[Module] Settings list created");
        this.keyCode = 0;
        // System.out.println("[Module] Constructor completed for: " + name);
    }

    public void toggle() {
        this.enabled = !this.enabled;
        // System.out.println("[Module] " + this.name + " toggled to: " + (this.enabled ? "ON" : "OFF"));

        // Add notification (except for HUD module)
        if (!this.name.equals("HUD")) {
            io.github.exodar.ui.ModuleNotification.addNotification(this.name, this.enabled);
        }

        if (this.enabled) {
            // Register with EventBus for @Subscribe annotations
            EventBus.register(this);
            onEnable();
        } else {
            onDisable();
            // Unregister from EventBus
            EventBus.unregister(this);
        }
    }

    public void setEnabled(boolean enabled) {
        boolean changed = this.enabled != enabled;
        this.enabled = enabled;

        // Add notification only if state actually changed (except for HUD module)
        if (changed && !this.name.equals("HUD")) {
            io.github.exodar.ui.ModuleNotification.addNotification(this.name, this.enabled);
        }

        if (changed) {
            if (this.enabled) {
                // Register with EventBus for @Subscribe annotations
                EventBus.register(this);
                onEnable();
            } else {
                onDisable();
                // Unregister from EventBus
                EventBus.unregister(this);
            }
        }
    }

    public void onEnable() {
        // Override en subclases
    }

    public void onDisable() {
        // Override en subclases
    }

    public void onUpdate() {
        // Llamado cada tick cuando está activado
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void registerSetting(Setting setting) {
        this.settings.add(setting);
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public Setting getSettingByName(String name) {
        for (Setting setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public int getToggleBind() {
        return toggleBind;
    }

    public void setToggleBind(int toggleBind) {
        this.toggleBind = toggleBind;
    }

    public int getHoldBind() {
        return holdBind;
    }

    public void setHoldBind(int holdBind) {
        this.holdBind = holdBind;
    }

    public BindMode getCurrentBindMode() {
        return currentBindMode;
    }

    public void setCurrentBindMode(BindMode mode) {
        this.currentBindMode = mode;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Get display suffix for ArrayList (e.g., "x1.30" for FastBreak)
     * Override in modules to show extra info
     */
    public String getDisplaySuffix() {
        return "";
    }

    /**
     * Get custom display color for ArrayList
     * Override in modules to show dynamic color (e.g., charging effect)
     * @return -1 for default color, or RGB int color
     */
    public int getDisplayColor() {
        return -1; // -1 = use default ArrayList color
    }

    // ==================================================
    // =============== MODULE HELPERS ===================
    // ==================================================
    //
    // IMPORTANT:
    // - NEVER access mc.thePlayer directly
    // - Lunar / Genesis removes or remaps fields
    // - Always use these helpers
    //

    // ---------- PLAYER (EntityPlayerSP) ----------

    private static java.lang.reflect.Field cachedPlayerField = null;

    /**
     * Safe player getter (Lunar compatible)
     * @return EntityPlayerSP or null
     */
    protected net.minecraft.client.entity.EntityPlayerSP getPlayer() {
        try {
            if (cachedPlayerField == null) {
                for (java.lang.reflect.Field field : mc.getClass().getDeclaredFields()) {
                    if (net.minecraft.client.entity.EntityPlayerSP.class
                            .isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        cachedPlayerField = field;
                        break;
                    }
                }
            }
            if (cachedPlayerField != null) {
                return (net.minecraft.client.entity.EntityPlayerSP) cachedPlayerField.get(mc);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ---------- WORLD ----------

    private static java.lang.reflect.Field cachedWorldField = null;

    /**
     * Safe world getter
     */
    protected net.minecraft.client.multiplayer.WorldClient getWorld() {
        try {
            if (cachedWorldField == null) {
                for (java.lang.reflect.Field field : mc.getClass().getDeclaredFields()) {
                    if (net.minecraft.client.multiplayer.WorldClient.class
                            .isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        cachedWorldField = field;
                        break;
                    }
                }
            }
            if (cachedWorldField != null) {
                return (net.minecraft.client.multiplayer.WorldClient) cachedWorldField.get(mc);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ---------- VALIDATION ----------

    /**
     * Check if game is ready (safe for onUpdate / onRender)
     */
    protected boolean isInGame() {
        return mc != null && getPlayer() != null && getWorld() != null;
    }

    // ---------- NETWORKING (SendQueue) ----------

    private static java.lang.reflect.Field cachedSendQueueField = null;

    /**
     * Get sendQueue/netHandler (Lunar-safe)
     * DO NOT access sendQueue directly - causes NoSuchFieldError
     */
    protected Object getSendQueue() {
        try {
            net.minecraft.client.entity.EntityPlayerSP player = getPlayer();
            if (player == null) return null;

            if (cachedSendQueueField == null) {
                for (java.lang.reflect.Field f : player.getClass().getDeclaredFields()) {
                    if (f.getType().getName().contains("NetHandlerPlayClient")) {
                        f.setAccessible(true);
                        cachedSendQueueField = f;
                        break;
                    }
                }
            }

            return cachedSendQueueField != null
                    ? cachedSendQueueField.get(player)
                    : null;

        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Send packet directly (Lunar-proof)
     * Works on Lunar, Genesis, Vanilla, Forge
     */
    protected void sendPacketDirect(Object packet) {
        try {
            Object sendQueue = getSendQueue();
            if (sendQueue == null || packet == null) return;

            for (java.lang.reflect.Method m : sendQueue.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() == 1 &&
                    m.getParameterTypes()[0].getName().contains("Packet")) {
                    m.setAccessible(true);
                    m.invoke(sendQueue, packet);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }
    /**
     * Send packet WITH hooks - calls onSendPacket on all enabled modules
     */
    protected void sendPacket(Object packet) {
        if (packet == null) return;
        io.github.exodar.module.ModuleManager manager = io.github.exodar.Main.getModuleManager();
        if (manager != null) {
            for (Module module : manager.getModules()) {
                if (module.isEnabled()) {
                    try {
                        if (!module.onSendPacket(packet)) return;
                    } catch (Throwable ignored) {}
                }
            }
        }
        sendPacketDirect(packet);
    }

    public boolean onSendPacket(Object packet) { return true; }
    public boolean onReceivePacket(Object packet) { return true; }
}
