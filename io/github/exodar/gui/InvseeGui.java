/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui;

import io.github.exodar.module.modules.misc.InventoryTracker;
import io.github.exodar.module.modules.misc.InventoryTracker.TrackedInventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.UUID;

/**
 * InvseeGui - View tracked inventory of another player
 * Shows their equipment (armor + held item) that we've seen them use
 */
public class InvseeGui extends GuiScreen {

    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");

    private final String playerName;
    private final UUID playerUUID;
    private final TrackedInventory trackedInventory;

    // GUI dimensions
    private int guiLeft;
    private int guiTop;
    private final int xSize = 176;
    private final int ySize = 166;

    // For player model rendering
    private float mouseX;
    private float mouseY;

    public InvseeGui(String playerName) {
        this.playerName = playerName;
        this.playerUUID = InventoryTracker.getPlayerUUID(playerName);
        this.trackedInventory = InventoryTracker.getTrackedInventory(playerName);
    }

    public InvseeGui(UUID uuid) {
        this.playerUUID = uuid;
        this.playerName = InventoryTracker.getPlayerName(uuid);
        this.trackedInventory = InventoryTracker.getTrackedInventory(uuid);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw darkened background
        this.drawDefaultBackground();

        this.mouseX = (float) mouseX;
        this.mouseY = (float) mouseY;

        // Draw inventory background
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        this.drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        // Draw title
        String title = "§l" + playerName + "'s Equipment";
        if (trackedInventory == null || !trackedInventory.hasAnyData()) {
            title = "§c" + playerName + " §7(No data)";
        }
        drawCenteredString(fontRendererObj, title, guiLeft + xSize / 2, guiTop - 12, 0xFFFFFF);

        // Draw player entity (if in world)
        EntityPlayer targetPlayer = findPlayerInWorld();
        if (targetPlayer != null) {
            int playerX = guiLeft + 51;
            int playerY = guiTop + 75;
            int scale = 30;
            GuiInventory.drawEntityOnScreen(playerX, playerY, scale,
                    (float)(guiLeft + 51) - mouseX,
                    (float)(guiTop + 75 - 50) - mouseY,
                    targetPlayer);
        } else {
            // Draw placeholder text if player not in world
            drawCenteredString(fontRendererObj, "§7[Not in range]", guiLeft + 51, guiTop + 50, 0x888888);
        }

        // Draw tracked equipment
        if (trackedInventory != null) {
            drawTrackedEquipment();
        }

        // Draw help text
        drawString(fontRendererObj, "§7Press ESC or E to close", guiLeft + 5, guiTop + ySize + 5, 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Draw the tracked armor and held items
     */
    private void drawTrackedEquipment() {
        // Armor slots (vanilla inventory positions)
        // Helmet: slot 3
        // Chest: slot 2
        // Legs: slot 1
        // Boots: slot 0

        int armorX = guiLeft + 8;
        int[] armorY = {guiTop + 8, guiTop + 26, guiTop + 44, guiTop + 62}; // helmet, chest, legs, boots

        // Draw helmet (armor[3])
        drawItemSlot(trackedInventory.getHelmet(), armorX, armorY[0],
                trackedInventory.getArmorTimestamp(3));

        // Draw chestplate (armor[2])
        drawItemSlot(trackedInventory.getChestplate(), armorX, armorY[1],
                trackedInventory.getArmorTimestamp(2));

        // Draw leggings (armor[1])
        drawItemSlot(trackedInventory.getLeggings(), armorX, armorY[2],
                trackedInventory.getArmorTimestamp(1));

        // Draw boots (armor[0])
        drawItemSlot(trackedInventory.getBoots(), armorX, armorY[3],
                trackedInventory.getArmorTimestamp(0));

        // Draw current held item
        int heldX = guiLeft + 77;
        int heldY = guiTop + 62;
        drawItemSlot(trackedInventory.getCurrentHeld(), heldX, heldY,
                trackedInventory.getHeldTimestamp());

        // Draw label for held item
        drawString(fontRendererObj, "§eHeld", heldX, heldY - 10, 0xFFFF55);

        // Draw held item history (below main area)
        drawHeldHistory();
    }

    /**
     * Draw item in a slot with timestamp info
     */
    private void drawItemSlot(ItemStack stack, int x, int y, long timestamp) {
        // Draw slot background
        drawRect(x - 1, y - 1, x + 17, y + 17, 0x80000000);

        if (stack != null) {
            // Draw item
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 100);
            this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
            this.itemRender.renderItemOverlayIntoGUI(fontRendererObj, stack, x, y, null);
            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();

            // Check if mouse is over this slot for tooltip
            if (isMouseOver(x, y, 16, 16)) {
                drawItemTooltip(stack, timestamp);
            }
        }
    }

    /**
     * Draw held item history
     */
    private void drawHeldHistory() {
        ItemStack[] history = trackedInventory.getHeldHistory();
        int count = Math.min(trackedInventory.getHeldHistoryCount(), 9);

        if (count == 0) return;

        int startX = guiLeft + 8;
        int startY = guiTop + 84;

        drawString(fontRendererObj, "§7History:", startX, startY - 10, 0xAAAAAA);

        for (int i = 0; i < count; i++) {
            ItemStack stack = history[i];
            if (stack != null) {
                int slotX = startX + (i * 18);
                int slotY = startY;

                // Draw mini slot
                drawRect(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0x40000000);

                RenderHelper.enableGUIStandardItemLighting();
                this.itemRender.renderItemAndEffectIntoGUI(stack, slotX, slotY);
                this.itemRender.renderItemOverlayIntoGUI(fontRendererObj, stack, slotX, slotY, null);
                RenderHelper.disableStandardItemLighting();

                // Tooltip on hover
                if (isMouseOver(slotX, slotY, 16, 16)) {
                    drawItemTooltip(stack, 0);
                }
            }
        }
    }

    /**
     * Draw item tooltip with timestamp
     */
    private void drawItemTooltip(ItemStack stack, long timestamp) {
        if (stack == null) return;

        java.util.List<String> tooltip = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);

        // Add timestamp info
        if (timestamp > 0) {
            long elapsed = System.currentTimeMillis() - timestamp;
            String timeStr = formatTime(elapsed);
            tooltip.add("");
            tooltip.add("§7Last seen: §f" + timeStr + " ago");
        }

        // Draw tooltip at mouse position
        int tooltipX = (int) mouseX + 12;
        int tooltipY = (int) mouseY - 12;

        drawHoveringText(tooltip, tooltipX, tooltipY);
    }

    /**
     * Format milliseconds to human readable time
     */
    private String formatTime(long ms) {
        long seconds = ms / 1000;
        if (seconds < 60) return seconds + "s";

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) return minutes + "m " + seconds + "s";

        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }

    /**
     * Check if mouse is over a rectangle
     */
    private boolean isMouseOver(int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * Find the target player in the world (for rendering their model)
     */
    private EntityPlayer findPlayerInWorld() {
        if (mc.theWorld == null) return null;

        // Try by UUID first
        if (playerUUID != null) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player.getUniqueID().equals(playerUUID)) {
                    return player;
                }
            }
        }

        // Try by name
        if (playerName != null) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player.getName().equalsIgnoreCase(playerName)) {
                    return player;
                }
            }
        }

        return null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Close on ESC or E (inventory key)
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
