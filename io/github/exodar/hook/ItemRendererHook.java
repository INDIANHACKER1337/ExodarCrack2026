/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.hook;

import io.github.exodar.module.modules.render.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hook that replaces ItemRenderer with a proxy to support custom blocking animations
 * Based on Rise 6.2.4 approach - only modifies blocking animations
 */
public class ItemRendererHook extends Thread {

    private final Minecraft mc;
    private static ItemRendererHook instance;
    private static final AtomicBoolean hooked = new AtomicBoolean(false);

    public ItemRendererHook(Minecraft minecraft) {
        super("Exodar-ItemRendererHook");
        this.mc = minecraft;
        instance = this;
        setDaemon(true);
    }

    public static boolean isHooked() {
        return hooked.get();
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted() && !hooked.get()) {
                Thread.sleep(1000);
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null && mc.theWorld != null) {
                    mc.addScheduledTask(() -> {
                        try {
                            if (!hooked.get()) {
                                hookItemRenderer(mc);
                                hooked.set(true);
                                System.out.println("[Exodar] ItemRendererHook installed successfully!");
                            }
                        } catch (Exception e) {
                            System.out.println("[Exodar] Failed to install ItemRendererHook: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    Thread.sleep(2000);
                    if (hooked.get()) break;
                }
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookItemRenderer(Minecraft mc) throws Exception {
        Field itemRendererField = null;
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (f.getType() == ItemRenderer.class) {
                itemRendererField = f;
                break;
            }
        }

        if (itemRendererField == null) {
            throw new RuntimeException("Could not find itemRenderer field");
        }

        itemRendererField.setAccessible(true);
        ItemRenderer original = (ItemRenderer) itemRendererField.get(mc);

        ItemRendererProxy proxy = new ItemRendererProxy(mc, original);
        itemRendererField.set(mc, proxy);
    }

    public static void cleanup() {
        hooked.set(false);
        if (instance != null) {
            instance.interrupt();
            instance = null;
        }
    }

    /**
     * Proxy class that intercepts item rendering for custom blocking animations
     */
    public static class ItemRendererProxy extends ItemRenderer {
        private final ItemRenderer original;
        private final Minecraft mc;

        private Field equippedProgressField;
        private Field prevEquippedProgressField;
        private Field itemToRenderField;

        public ItemRendererProxy(Minecraft minecraft, ItemRenderer original) {
            super(minecraft);
            this.mc = minecraft;
            this.original = original;

            // Find reflection fields for equipped progress
            try {
                Field[] fields = ItemRenderer.class.getDeclaredFields();
                for (Field f : fields) {
                    f.setAccessible(true);
                    String name = f.getName().toLowerCase();

                    if (f.getType() == float.class) {
                        // Look for equippedProgress fields
                        if (name.contains("equipped") || name.contains("progress")) {
                            if (equippedProgressField == null) {
                                equippedProgressField = f;
                            } else if (prevEquippedProgressField == null) {
                                prevEquippedProgressField = f;
                            }
                        }
                    }
                    if (f.getType() == ItemStack.class) {
                        itemToRenderField = f;
                    }
                }

                // If we didn't find by name, find by order (first two floats are usually prev and current)
                if (equippedProgressField == null) {
                    int floatCount = 0;
                    for (Field f : fields) {
                        if (f.getType() == float.class) {
                            f.setAccessible(true);
                            floatCount++;
                            if (floatCount == 1) prevEquippedProgressField = f;
                            if (floatCount == 2) equippedProgressField = f;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[Exodar] Failed to setup ItemRenderer reflection: " + e.getMessage());
            }
        }

        @Override
        public void renderItemInFirstPerson(float partialTicks) {
            EntityPlayerSP player = mc.thePlayer;

            // Check if we should apply custom animation
            if (player != null && Animations.shouldApplyAnimation()) {
                ItemStack heldItem = player.getHeldItem();

                if (heldItem != null) {
                    Animations anim = Animations.getInstance();

                    // Check if only sword mode
                    boolean isSword = heldItem.getItem() instanceof ItemSword;
                    if (anim != null && anim.isOnlySword() && !isSword) {
                        // Not a sword, use original rendering
                        original.renderItemInFirstPerson(partialTicks);
                        return;
                    }

                    // Check if blocking
                    boolean isBlocking = player.isUsingItem() &&
                        heldItem.getItemUseAction() == EnumAction.BLOCK;

                    if (isBlocking) {
                        // Apply custom blocking animation
                        renderCustomBlockingAnimation(partialTicks, player, heldItem);
                        return;
                    }
                }
            }

            // Default rendering for non-blocking or when module disabled
            original.renderItemInFirstPerson(partialTicks);
        }

        private void renderCustomBlockingAnimation(float partialTicks, EntityPlayerSP player, ItemStack itemStack) {
            float swingProgress = player.getSwingProgress(partialTicks);

            // Get equip progress
            float equipProgress = 1.0F;
            try {
                if (equippedProgressField != null && prevEquippedProgressField != null) {
                    float curr = equippedProgressField.getFloat(original);
                    float prev = prevEquippedProgressField.getFloat(original);
                    equipProgress = prev + (curr - prev) * partialTicks;
                }
            } catch (Exception ignored) {
                equipProgress = 1.0F;
            }

            // Setup lighting (same as vanilla)
            float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
            float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;

            GlStateManager.pushMatrix();
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.popMatrix();

            // Set lightmap
            int light = mc.theWorld.getCombinedLight(
                new net.minecraft.util.BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ), 0);
            net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
                net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit,
                (float)(light & 65535),
                (float)(light >> 16));

            // Arm rotation interpolation
            float armPitch = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * partialTicks;
            float armYaw = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTicks;
            GlStateManager.rotate((player.rotationPitch - armPitch) * 0.1F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate((player.rotationYaw - armYaw) * 0.1F, 0.0F, 1.0F, 0.0F);

            GlStateManager.enableRescaleNormal();
            GlStateManager.pushMatrix();

            // Apply custom blocking animation transforms
            Animations.applyBlockingAnimation(equipProgress, swingProgress);

            // Render the item
            this.renderItem(player, itemStack, ItemCameraTransforms.TransformType.FIRST_PERSON);

            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
        }

        // Delegate other methods to original
        @Override
        public void renderOverlays(float partialTicks) {
            original.renderOverlays(partialTicks);
        }

        @Override
        public void updateEquippedItem() {
            original.updateEquippedItem();
        }

        @Override
        public void resetEquippedProgress() {
            original.resetEquippedProgress();
        }

        @Override
        public void resetEquippedProgress2() {
            original.resetEquippedProgress2();
        }
    }
}
