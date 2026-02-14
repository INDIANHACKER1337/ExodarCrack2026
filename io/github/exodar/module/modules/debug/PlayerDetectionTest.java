/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.debug;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.utils.FontRendererHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug module to test player detection and display on screen
 */
public class PlayerDetectionTest extends Module {
    private int tickCounter = 0;
    private boolean detailsShown = false;
    private List<PlayerInfo> detectedPlayers = new ArrayList<>();
    private String detectionMethod = "None";
    private int totalEntities = 0;

    private static class PlayerInfo {
        String name;
        double distance;
        double blocks;

        PlayerInfo(String name, double distance, double blocks) {
            this.name = name;
            this.distance = distance;
            this.blocks = blocks;
        }
    }

    public PlayerDetectionTest() {
        super("PlayerDetectionTest", ModuleCategory.RENDER);
        this.hidden = true; // Hidden from ClickGUI
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        tickCounter++;

        // Actualizar cada 10 ticks (0.5 segundos)
        if (tickCounter % 10 == 0) {
            updatePlayerDetection();
        }
    }

    /**
     * Llamar esto desde Main.java en el hook de renderGameOverlay
     */
    @Subscribe
    public void renderDebugInfo(Render2DEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;

        if (!enabled || fr == null) return;
        int startY = 50;

        int y = startY;
        int lineHeight = 10;

        // Título
        FontRendererHelper.drawStringWithShadow(fr, "§e§l[PLAYER DETECTION DEBUG]", 5, y, 0xFFFFFF);
        y += lineHeight + 2;

        // Método de detección
        FontRendererHelper.drawStringWithShadow(fr, "§7Method: §f" + detectionMethod, 5, y, 0xFFFFFF);
        y += lineHeight;

        // Total de entidades
        FontRendererHelper.drawStringWithShadow(fr, "§7Total Entities: §f" + totalEntities, 5, y, 0xFFFFFF);
        y += lineHeight;

        // Jugadores detectados
        FontRendererHelper.drawStringWithShadow(fr, "§7Players Found: §a" + detectedPlayers.size(), 5, y, 0xFFFFFF);
        y += lineHeight + 2;

        // Lista de jugadores con distancia y bloques
        if (detectedPlayers.isEmpty()) {
            FontRendererHelper.drawStringWithShadow(fr, "§c  No players detected!", 5, y, 0xFFFFFF);
        } else {
            for (PlayerInfo player : detectedPlayers) {
                String text = String.format("§f  %s §7- §e%.1f blocks",
                    player.name, player.blocks);
                FontRendererHelper.drawStringWithShadow(fr, text, 5, y, 0xFFFFFF);
                y += lineHeight;
            }
        }
    }

    private void updatePlayerDetection() {
        detectedPlayers.clear();
        detectionMethod = "None";
        totalEntities = 0;

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.theWorld == null) {
                detectionMethod = "ERROR: MC/Player/World null";
                return;
            }

            EntityPlayer currentPlayer = mc.thePlayer;

            // Mostrar detalles de World una sola vez en consola
            if (!detailsShown) {
                detailsShown = true;
                System.out.println("[PlayerTest] ================================");
                System.out.println("[PlayerTest] World class: " + mc.theWorld.getClass().getName());
                System.out.println("[PlayerTest] All List fields in World:");

                Class<?> worldClass = mc.theWorld.getClass();
                while (worldClass != null) {
                    for (Field f : worldClass.getDeclaredFields()) {
                        if (f.getType().getName().contains("List")) {
                            System.out.println("[PlayerTest]   " + f.getName() + " : " + f.getType().getName());
                        }
                    }
                    worldClass = worldClass.getSuperclass();
                    if (worldClass != null && !worldClass.getName().contains("World")) {
                        break;
                    }
                }
                System.out.println("[PlayerTest] ================================");
            }

            // Probar diferentes métodos
            boolean found = false;

            // Método 1: playerEntities
            if (!found) {
                found = tryPlayerEntities(mc, currentPlayer);
            }

            // Método 2: loadedEntityList
            if (!found) {
                found = tryLoadedEntityList(mc, currentPlayer);
            }

            // Método 3: Todos los campos List
            if (!found) {
                found = tryAllLists(mc, currentPlayer);
            }

            if (!found) {
                detectionMethod = "FAILED - No list found";
            }

        } catch (Exception e) {
            detectionMethod = "ERROR: " + e.getMessage();
            System.out.println("[PlayerTest] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean tryPlayerEntities(Minecraft mc, EntityPlayer currentPlayer) {
        try {
            Class<?> worldClass = mc.theWorld.getClass();
            while (worldClass != null) {
                for (Field f : worldClass.getDeclaredFields()) {
                    if (f.getName().equals("playerEntities")) {
                        f.setAccessible(true);
                        Object value = f.get(mc.theWorld);
                        if (value instanceof List) {
                            List<?> list = (List<?>) value;
                            extractPlayers(list, currentPlayer, "playerEntities");
                            return true;
                        }
                    }
                }
                worldClass = worldClass.getSuperclass();
                if (worldClass != null && !worldClass.getName().contains("World")) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[PlayerTest] M1 Error: " + e.getMessage());
        }
        return false;
    }

    private boolean tryLoadedEntityList(Minecraft mc, EntityPlayer currentPlayer) {
        try {
            Class<?> worldClass = mc.theWorld.getClass();
            while (worldClass != null) {
                for (Field f : worldClass.getDeclaredFields()) {
                    if (f.getName().contains("loadedEntity") || f.getName().equals("loadedEntityList")) {
                        f.setAccessible(true);
                        Object value = f.get(mc.theWorld);
                        if (value instanceof List) {
                            List<?> list = (List<?>) value;
                            extractPlayers(list, currentPlayer, f.getName());
                            return true;
                        }
                    }
                }
                worldClass = worldClass.getSuperclass();
                if (worldClass != null && !worldClass.getName().contains("World")) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[PlayerTest] M2 Error: " + e.getMessage());
        }
        return false;
    }

    private boolean tryAllLists(Minecraft mc, EntityPlayer currentPlayer) {
        try {
            Class<?> worldClass = mc.theWorld.getClass();
            while (worldClass != null) {
                for (Field f : worldClass.getDeclaredFields()) {
                    if (f.getType().getName().contains("List")) {
                        try {
                            f.setAccessible(true);
                            Object value = f.get(mc.theWorld);
                            if (value instanceof List) {
                                List<?> list = (List<?>) value;
                                if (extractPlayers(list, currentPlayer, f.getName())) {
                                    return true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
                worldClass = worldClass.getSuperclass();
                if (worldClass != null && !worldClass.getName().contains("World")) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[PlayerTest] M3 Error: " + e.getMessage());
        }
        return false;
    }

    private boolean extractPlayers(List<?> list, EntityPlayer currentPlayer, String fieldName) {
        totalEntities = list.size();
        int playerCount = 0;

        for (Object obj : list) {
            if (obj instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) obj;
                if (p == currentPlayer) continue; // Skip self

                double distance = currentPlayer.getDistanceToEntity(p);
                double blocks = distance; // En Minecraft, distance ya está en bloques
                String name = p.getName();

                detectedPlayers.add(new PlayerInfo(name, distance, blocks));
                playerCount++;
            }
        }

        if (playerCount > 0) {
            detectionMethod = fieldName + " (found " + playerCount + ")";
            return true;
        }

        return false;
    }
}
