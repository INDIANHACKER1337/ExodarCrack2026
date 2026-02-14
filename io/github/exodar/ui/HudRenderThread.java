/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import io.github.exodar.config.UserConfig;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Background thread for HUD calculations (ArrayList & Notifications)
 * Performs all computation-heavy tasks off the main thread:
 * - Module list building and sorting
 * - Position calculations
 * - Animation updates
 * - Color calculations
 *
 * The main thread only reads the pre-computed data and does GL draw calls.
 * This results in smoother animations and less lag.
 */
public class HudRenderThread implements Runnable {
    private static HudRenderThread instance;
    private static Thread thread;
    private static volatile boolean running = false;

    // Target update rate (60fps for smooth animations)
    private static final long TARGET_FRAME_TIME = 16; // ~60fps

    // Reference to module manager
    private ModuleManager moduleManager;

    // Screen dimensions (updated by main thread)
    private volatile int screenWidth = 1920;
    private volatile int screenHeight = 1080;

    // Thread-safe render data containers
    private final AtomicReference<ArrayListRenderData> arrayListData = new AtomicReference<>(new ArrayListRenderData());
    private final AtomicReference<NotificationRenderData> notificationData = new AtomicReference<>(new NotificationRenderData());

    // Cache for module state hash to detect changes
    private int lastModuleStateHash = 0;
    private List<Module> cachedEnabledModules = new ArrayList<>();

    // String width cache (populated by main thread, read by this thread)
    private static final Map<String, Integer> stringWidthCache = new ConcurrentHashMap<>();

    /**
     * Cache a string width (called from main thread)
     */
    public static void cacheStringWidth(String text, int width) {
        stringWidthCache.put(text, width);
    }

    /**
     * Get cached string width (returns estimate if not cached)
     */
    public static int getCachedStringWidth(String text) {
        Integer cached = stringWidthCache.get(text);
        if (cached != null) {
            return cached;
        }
        // Estimate: average 6 pixels per character
        return text.length() * 6;
    }

    /**
     * Check if string width is cached
     */
    public static boolean isStringWidthCached(String text) {
        return stringWidthCache.containsKey(text);
    }

    /**
     * Data class for ArrayList rendering
     * Contains all pre-computed values needed for rendering
     */
    public static class ArrayListRenderData {
        public final List<ModuleRenderInfo> modules = new CopyOnWriteArrayList<>();
        public volatile boolean ready = false;
    }

    /**
     * Info for a single module in the ArrayList
     */
    public static class ModuleRenderInfo {
        public String name;
        public String suffix;
        public int x;
        public int y;
        public int color;
        public int suffixX;

        public ModuleRenderInfo(String name, String suffix, int x, int y, int color, int suffixX) {
            this.name = name;
            this.suffix = suffix;
            this.x = x;
            this.y = y;
            this.color = color;
            this.suffixX = suffixX;
        }
    }

    /**
     * Data class for Notification rendering
     */
    public static class NotificationRenderData {
        public final List<NotificationRenderInfo> notifications = new CopyOnWriteArrayList<>();
        public volatile boolean ready = false;
    }

    /**
     * Info for a single notification
     */
    public static class NotificationRenderInfo {
        public String moduleName;
        public boolean enabled;
        public int x;
        public int y;
        public float alpha;
        public int bgColor;
        public int borderColor;
        public int symbolColor;
        public int textColor;
        public String symbol;

        public NotificationRenderInfo() {}
    }

    private HudRenderThread() {}

    /**
     * Start the render thread
     */
    public static void start(ModuleManager moduleManager) {
        if (running) return;

        instance = new HudRenderThread();
        instance.moduleManager = moduleManager;

        running = true;
        thread = new Thread(instance, "Exodar-HudRenderThread");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stop the render thread
     */
    public static void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        instance = null;
    }

    /**
     * Check if thread is running
     */
    public static boolean isRunning() {
        return running && thread != null && thread.isAlive();
    }

    /**
     * Update screen dimensions (called from main thread)
     */
    public static void setScreenSize(int width, int height) {
        if (instance != null) {
            instance.screenWidth = width;
            instance.screenHeight = height;
        }
    }

    /**
     * Get the latest ArrayList render data (called from main thread)
     */
    public static ArrayListRenderData getArrayListData() {
        if (instance != null) {
            return instance.arrayListData.get();
        }
        return new ArrayListRenderData();
    }

    /**
     * Get the latest Notification render data (called from main thread)
     */
    public static NotificationRenderData getNotificationData() {
        if (instance != null) {
            return instance.notificationData.get();
        }
        return new NotificationRenderData();
    }

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();

            try {
                // Update ArrayList data
                if (moduleManager != null && ArrayListConfig.arrayListEnabled) {
                    updateArrayListData();
                }

                // Update Notification data
                if (ArrayListConfig.notificationsEnabled) {
                    updateNotificationData();
                }
            } catch (Exception e) {
                // Silent fail - don't crash the thread
            }

            // Sleep to maintain target framerate
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = TARGET_FRAME_TIME - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /**
     * Update ArrayList render data
     */
    private void updateArrayListData() {
        try {
            // Create new render data
            ArrayListRenderData newData = new ArrayListRenderData();

            // Calculate hash of current module states (includes hidden state)
            UserConfig userConfig = UserConfig.getInstance();
            int currentStateHash = userConfig.getHiddenCount(); // Include hidden count in hash
            for (Module module : moduleManager.getModules()) {
                currentStateHash = currentStateHash * 31 + (module.isEnabled() ? 1 : 0);
                currentStateHash = currentStateHash * 17 + (userConfig.isModuleHidden(module) ? 1 : 0);
            }

            // Rebuild enabled modules list if changed
            if (currentStateHash != lastModuleStateHash) {
                cachedEnabledModules.clear();
                for (Module module : moduleManager.getModules()) {
                    // Skip HUD, disabled modules, and hidden modules
                    if (module.isEnabled() && !module.getName().equals("HUD") && !userConfig.isModuleHidden(module)) {
                        cachedEnabledModules.add(module);
                    }
                }

                // Sort by display text width (longest first) using cached widths
                cachedEnabledModules.sort((m1, m2) -> {
                    String s1 = m1.getDisplaySuffix();
                    String s2 = m2.getDisplaySuffix();
                    String text1 = m1.getName() + (s1 != null ? s1.replaceAll("§.", "") : "");
                    String text2 = m2.getName() + (s2 != null ? s2.replaceAll("§.", "") : "");
                    return Integer.compare(getCachedStringWidth(text2), getCachedStringWidth(text1));
                });

                lastModuleStateHash = currentStateHash;
            }

            int yOffset = 2;

            // Update animations for disabled or hidden modules
            for (Module module : moduleManager.getModules()) {
                if (module.getName().equals("HUD")) continue;
                // Trigger slide-out for disabled OR hidden modules
                if (!module.isEnabled() || userConfig.isModuleHidden(module)) {
                    ModuleAnimation.setDisabled(module);
                }
            }

            // Get exiting modules
            List<String> exitingModules = ModuleAnimation.updateExitingModules();

            // Process enabled modules
            for (Module module : cachedEnabledModules) {
                String moduleName = module.getName();
                String suffix = module.getDisplaySuffix();
                if (suffix == null) suffix = "";

                // Update Y position
                ModuleAnimation.setEnabled(module, yOffset);
                ModuleAnimation.AnimationState anim = ModuleAnimation.getState(module);

                // Use cached string widths for accurate positioning
                String fullText = moduleName + suffix.replaceAll("§.", "");
                int totalWidth = getCachedStringWidth(fullText);

                // Calculate positions
                int baseX = screenWidth - totalWidth - 2;
                int animatedX = (int) (baseX + anim.getX());
                int y = (int) anim.getY();

                // Get color - check for custom module color first
                int customColor = module.getDisplayColor();
                int color = (customColor != -1) ? customColor : ArrayListConfig.getColorForY(y, screenHeight);

                // Calculate suffix X position using cached width
                int nameWidth = getCachedStringWidth(moduleName);
                int suffixX = animatedX + nameWidth;

                newData.modules.add(new ModuleRenderInfo(moduleName, suffix, animatedX, y, color, suffixX));

                yOffset += 10;
            }

            // Process exiting modules
            for (String exitingName : exitingModules) {
                ModuleAnimation.AnimationState anim = ModuleAnimation.getStateByName(exitingName);
                if (anim == null) continue;

                Module exitingModule = moduleManager.getModuleByName(exitingName);
                String suffix = "";
                if (exitingModule != null) {
                    suffix = exitingModule.getDisplaySuffix();
                    if (suffix == null) suffix = "";
                }

                String fullText = exitingName + suffix.replaceAll("§.", "");
                int totalWidth = getCachedStringWidth(fullText);

                int baseX = screenWidth - totalWidth - 2;
                int animatedX = (int) (baseX + anim.getX());
                int y = (int) anim.getY();
                int color = ArrayListConfig.getColorForY(y, screenHeight);
                int nameWidth = getCachedStringWidth(exitingName);
                int suffixX = animatedX + nameWidth;

                newData.modules.add(new ModuleRenderInfo(exitingName, suffix, animatedX, y, color, suffixX));
            }

            newData.ready = true;
            arrayListData.set(newData);

        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Update Notification render data
     */
    private void updateNotificationData() {
        try {
            NotificationRenderData newData = new NotificationRenderData();

            // Get current notifications from ModuleNotification
            // Note: The actual notification list is managed by ModuleNotification
            // We just compute the render positions here

            // For now, notifications still compute on main thread
            // TODO: Move notification computation here as well

            newData.ready = true;
            notificationData.set(newData);

        } catch (Exception e) {
            // Silent fail
        }
    }

}
