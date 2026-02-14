/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.render;

import io.github.exodar.event.Render3DEvent;
import io.github.exodar.hook.EntityRenderHook;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.module.modules.render.BedESP;
import io.github.exodar.module.modules.render.ChestESP;
// import io.github.exodar.module.modules.render.Glow; // DISABLED - performance optimization
import io.github.exodar.module.modules.render.ItemESP;
import io.github.exodar.module.modules.render.Nametags;
import io.github.exodar.module.modules.render.Tracers;
import io.github.exodar.module.modules.render.Trajectories;
import io.github.exodar.module.modules.render.Xray;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

/**
 * Manages 3D rendering for ESP modules using the Render3DEvent.
 * This ensures ESPs are rendered at the correct point in the pipeline
 * (after world/entities, before hand) with correct GL matrices.
 *
 * OPTIMIZED: Caches module references to avoid string lookups every frame.
 */
public class Render3DManager implements EntityRenderHook.Render3DListener {

    private static Render3DManager instance;
    private static ModuleManager moduleManager;

    // Cached module references - looked up once, not every frame
    private BedESP bedEsp;
    private ItemESP itemEsp;
    private Tracers tracers;
    private Trajectories trajectories;
    private Xray xray;
    private ChestESP chestEsp;
    private Nametags nametags;
    // private Glow glow; // DISABLED - performance optimization
    private boolean modulesCached = false;

    private Render3DManager() {
    }

    public static void init(ModuleManager manager) {
        moduleManager = manager;
        instance = new Render3DManager();
        EntityRenderHook.addListener(instance);
    }

    public static void cleanup() {
        if (instance != null) {
            EntityRenderHook.removeListener(instance);
            instance = null;
        }
        moduleManager = null;
    }

    /**
     * Cache module references once instead of looking them up every frame
     */
    private void cacheModules() {
        if (modulesCached || moduleManager == null) return;

        try {
            Module m;
            m = moduleManager.getModuleByName("BedESP");
            if (m instanceof BedESP) bedEsp = (BedESP) m;

            m = moduleManager.getModuleByName("ItemESP");
            if (m instanceof ItemESP) itemEsp = (ItemESP) m;

            m = moduleManager.getModuleByName("Tracers");
            if (m instanceof Tracers) tracers = (Tracers) m;

            m = moduleManager.getModuleByName("Trajectories");
            if (m instanceof Trajectories) trajectories = (Trajectories) m;

            m = moduleManager.getModuleByName("Xray");
            if (m instanceof Xray) xray = (Xray) m;

            m = moduleManager.getModuleByName("ChestESP");
            if (m instanceof ChestESP) chestEsp = (ChestESP) m;

            m = moduleManager.getModuleByName("Nametags");
            if (m instanceof Nametags) nametags = (Nametags) m;

            // DISABLED - Glow (performance optimization)
            // m = moduleManager.getModuleByName("Glow");
            // if (m instanceof Glow) glow = (Glow) m;

            modulesCached = true;
        } catch (Exception e) {
            // Will retry next frame
        }
    }

    /**
     * Quick check if any render3D module is enabled
     */
    private boolean anyModuleEnabled() {
        return (bedEsp != null && bedEsp.isEnabled()) ||
               (itemEsp != null && itemEsp.isEnabled()) ||
               (tracers != null && tracers.isEnabled()) ||
               (trajectories != null && trajectories.isEnabled()) ||
               (xray != null && xray.isEnabled()) ||
               (chestEsp != null && chestEsp.isEnabled()) ||
               (nametags != null && nametags.isEnabled());
               // (glow != null && glow.isEnabled()); // DISABLED - performance optimization
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (moduleManager == null) return;

        // Cache modules once
        if (!modulesCached) {
            cacheModules();
        }

        // Early exit if no modules are enabled
        if (!anyModuleEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        float partialTicks = event.getPartialTicks();

        // DISABLED - Glow (performance optimization)
        // if (glow != null && glow.isEnabled()) {
        //     try { glow.render3D(event); } catch (Exception ignored) {}
        // }

        // Render only enabled modules
        if (bedEsp != null && bedEsp.isEnabled()) {
            try { bedEsp.render3D(event); } catch (Exception ignored) {}
        }

        if (tracers != null && tracers.isEnabled()) {
            try { tracers.render3D(event); } catch (Exception ignored) {}
        }

        if (itemEsp != null && itemEsp.isEnabled()) {
            try { itemEsp.render3D(event); } catch (Exception ignored) {}
        }

        if (trajectories != null && trajectories.isEnabled()) {
            try { trajectories.render3D(event); } catch (Exception ignored) {}
        }

        if (xray != null && xray.isEnabled()) {
            try { xray.render3D(event); } catch (Exception ignored) {}
        }

        if (chestEsp != null && chestEsp.isEnabled()) {
            try { chestEsp.render3D(event); } catch (Exception ignored) {}
        }

        // NOTE: Nametags are rendered by CustomRenderPlayer per-player, not here
        // Don't add nametags.renderNametags() here or they will duplicate

        // Restore GL state after all renders
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
