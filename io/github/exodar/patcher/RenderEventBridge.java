/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.patcher;

import io.github.exodar.event.EventBus;
import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Render3DEvent;

import java.lang.reflect.Method;

/**
 * RenderEventBridge - Translates JVMTI bytecode patch callbacks into EventBus events
 *
 * When JVMTI patches are injected into Minecraft methods (via EntityRendererPatcher),
 * they retrieve Method objects from System.getProperties() and invoke them.
 * This bridge registers those methods so the injected bytecode can call back
 * into Exodar to fire render events.
 *
 * This architecture ensures:
 * 1. Cross-classloader communication (Minecraft's classloader -> Exodar's classloader)
 * 2. Render events fire at the correct point in the render pipeline
 * 3. Shader compatibility (events fire INSIDE the render method, not after)
 */
public class RenderEventBridge {
    private static boolean initialized = false;
    private static ClassLoader exodarClassLoader;
    private static volatile boolean bytecodeActive = false;

    /**
     * Initialize the event bridge by registering fire* methods in System.getProperties()
     * Must be called during client initialization (onLoad)
     */
    public static void initialize() {
        if (initialized) {
            System.out.println("[Exodar-RenderEventBridge] Already initialized");
            return;
        }

        exodarClassLoader = RenderEventBridge.class.getClassLoader();
        registerEventMethods();
        initialized = true;

        System.out.println("[Exodar-RenderEventBridge] Initialized (classloader: " + exodarClassLoader + ")");
    }

    /**
     * Shutdown and cleanup - remove registered methods from System.getProperties()
     */
    public static void shutdown() {
        if (!initialized) return;

        // Remove all registered event methods
        System.getProperties().remove("exodar.render2d");
        System.getProperties().remove("exodar.render3d");

        initialized = false;
        System.out.println("[Exodar-RenderEventBridge] Shutdown complete");
    }

    /**
     * Register fire* methods in System.getProperties() for cross-classloader access
     * The keys match what EntityRendererPatcher injects:
     * - "exodar.render2d" -> fireRender2D method
     * - "exodar.render3d" -> fireRender3D method
     */
    private static void registerEventMethods() {
        try {
            // Register fireRender2D
            Method render2dMethod = RenderEventBridge.class.getDeclaredMethod("fireRender2D", Float.class);
            System.getProperties().put("exodar.render2d", render2dMethod);
            System.out.println("[Exodar-RenderEventBridge] Registered: exodar.render2d -> fireRender2D");

            // Register fireRender3D
            Method render3dMethod = RenderEventBridge.class.getDeclaredMethod("fireRender3D", Float.class);
            System.getProperties().put("exodar.render3d", render3dMethod);
            System.out.println("[Exodar-RenderEventBridge] Registered: exodar.render3d -> fireRender3D");

            System.out.println("[Exodar-RenderEventBridge] All event methods registered for cross-classloader access");
        } catch (Exception e) {
            System.err.println("[Exodar-RenderEventBridge] Failed to register event methods: " + e);
            e.printStackTrace();
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Mark that bytecode patching is active.
     * When bytecode patching is active, EntityRenderHook should NOT install the proxy.
     */
    public static void markBytecodeActive() {
        bytecodeActive = true;
        System.out.println("[Exodar-RenderEventBridge] Bytecode patching is now active - proxy will be skipped");
    }

    /**
     * Check if bytecode patching is active.
     * Used by EntityRenderHook to decide whether to install the proxy.
     */
    public static boolean isBytecodeActive() {
        return bytecodeActive;
    }

    /**
     * Get the classloader that has Exodar
     * Used by reflection-based bytecode injection
     */
    public static ClassLoader getExodarClassLoader() {
        return exodarClassLoader;
    }

    // ===== RENDERING EVENTS =====

    /**
     * Fire Render3DEvent
     * Called from EntityRendererPatcher injected code in renderWorldPass()
     * This is invoked via reflection from Minecraft's classloader
     *
     * IMPORTANT: We save and restore GL state to prevent shader/render corruption
     *
     * @param partialTicks The partial tick value (boxed Float from reflection)
     */
    public static void fireRender3D(Float partialTicks) {
        if (!initialized) return;
        try {
            float pt = (partialTicks != null) ? partialTicks : 0.0f;
            EventBus.post(new Render3DEvent(pt));
        } catch (Exception e) {
            System.out.println("[Exodar-RenderEventBridge] fireRender3D error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fire Render2DEvent
     * Called from EntityRendererPatcher injected code in updateCameraAndRender()
     * This is invoked via reflection from Minecraft's classloader
     *
     * IMPORTANT: We save and restore GL state to prevent shader/render corruption
     *
     * @param partialTicks The partial tick value (boxed Float from reflection)
     */
    public static void fireRender2D(Float partialTicks) {
        if (!initialized) return;
        try {
            float pt = (partialTicks != null) ? partialTicks : 0.0f;
            EventBus.post(new Render2DEvent(pt));
        } catch (Exception e) {
            System.out.println("[Exodar-RenderEventBridge] fireRender2D error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
