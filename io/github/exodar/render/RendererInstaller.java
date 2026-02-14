/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

/**
 * RendererInstaller - Installs CustomRenderPlayer to replace vanilla player rendering
 * Based on GhostClient's RendererInstaller
 */
public class RendererInstaller {

    private static boolean installed = false;

    public static void installCustomRenderers() {
        if (installed) {
            System.out.println("[RendererInstaller] Already installed, skipping");
            return;
        }

        try {
            System.out.println("[RendererInstaller] Installing custom player renderers...");

            Minecraft mc = Minecraft.getMinecraft();
            RenderManager rm = mc.getRenderManager();

            if (rm == null) {
                System.out.println("[RendererInstaller] ERROR: Could not get RenderManager");
                return;
            }

            Field playerRendererOriginal = RenderManager.class.getDeclaredField("playerRenderer");
            playerRendererOriginal.setAccessible(true);

            // Get the original playerRenderer before replacing it
            RenderPlayer original = (RenderPlayer) playerRendererOriginal.get(rm);

            // Get skinMap field
            Field skinMapField = RenderManager.class.getDeclaredField("skinMap");
            skinMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, RenderPlayer> skinMap = (Map<String, RenderPlayer>) skinMapField.get(rm);

            if (skinMap == null) {
                System.out.println("[RendererInstaller] ERROR: skinMap is null");
                return;
            }

            // Get the original renderers for each skin type from skinMap
            RenderPlayer originalDefault = skinMap.get("default");
            RenderPlayer originalSlim = skinMap.get("slim");

            // Fallback to main playerRenderer if skinMap entries are missing
            if (originalDefault == null) originalDefault = original;
            if (originalSlim == null) originalSlim = original;

            System.out.println("[RendererInstaller] Original default smallArms: " + getSmallArms(originalDefault));
            System.out.println("[RendererInstaller] Original slim smallArms: " + getSmallArms(originalSlim));

            // Create custom renderers with the correct original for each type
            CustomRenderPlayer customDefault = new CustomRenderPlayer(rm, false, originalDefault);
            CustomRenderPlayer customSlim = new CustomRenderPlayer(rm, true, originalSlim);

            // Replace in skinMap
            skinMap.put("default", customDefault);
            skinMap.put("slim", customSlim);
            System.out.println("[RendererInstaller] Replaced skinMap renderers (default + slim)");

            // Try to replace playerRenderer field - use the matching type
            try {
                Field playerRendererField = RenderManager.class.getDeclaredField("playerRenderer");
                playerRendererField.setAccessible(true);
                // Use the same type as the original playerRenderer
                boolean originalWasSlim = getSmallArms(original);
                playerRendererField.set(rm, originalWasSlim ? customSlim : customDefault);
                System.out.println("[RendererInstaller] Replaced playerRenderer field (slim=" + originalWasSlim + ")");
            } catch (NoSuchFieldException ignored) {
                System.out.println("[RendererInstaller] playerRenderer field not found (ok)");
            }

            // Get and update entityRenderMap
            Field entityRenderMapField = RenderManager.class.getDeclaredField("entityRenderMap");
            entityRenderMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<? extends Entity>, Object> entityRenderMap =
                (Map<Class<? extends Entity>, Object>) entityRenderMapField.get(rm);

            if (entityRenderMap != null) {
                int replaced = 0;

                // Replace all RenderPlayer instances
                for (Map.Entry<Class<? extends Entity>, Object> entry : new ArrayList<>(entityRenderMap.entrySet())) {
                    Object rendererObj = entry.getValue();
                    if (rendererObj instanceof RenderPlayer && !(rendererObj instanceof CustomRenderPlayer)) {
                        RenderPlayer old = (RenderPlayer) rendererObj;
                        boolean smallArms = getSmallArms(old);
                        // Constructor now handles copying layers from original
                        CustomRenderPlayer replacement = new CustomRenderPlayer(rm, smallArms, old);
                        entityRenderMap.put(entry.getKey(), replacement);
                        replaced++;
                    }
                }

                // Remove EntityPlayer and AbstractClientPlayer to force re-lookup
                entityRenderMap.remove(EntityPlayer.class);
                entityRenderMap.remove(AbstractClientPlayer.class);

                System.out.println("[RendererInstaller] Replaced " + replaced + " entity renderers");
            }

            installed = true;
            System.out.println("[RendererInstaller] Custom renderers installed successfully!");

        } catch (Throwable t) {
            System.out.println("[RendererInstaller] ERROR: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public static void reloadAllPlayerRenderers() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null) return;

            for (EntityPlayer p : mc.theWorld.playerEntities) {
                int cx = (int) p.posX >> 4, cz = (int) p.posZ >> 4;
                mc.theWorld.markBlockRangeForRenderUpdate(
                    cx << 4, 0, cz << 4,
                    (cx << 4) + 15, 255, (cz << 4) + 15
                );
            }
            System.out.println("[RendererInstaller] Reloaded player renderers");
        } catch (Throwable ignored) {}
    }

    /**
     * Get smallArms value from RenderPlayer
     */
    private static boolean getSmallArms(RenderPlayer render) {
        try {
            Field f = RenderPlayer.class.getDeclaredField("smallArms");
            f.setAccessible(true);
            return (boolean) f.get(render);
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isInstalled() {
        return installed;
    }
}
