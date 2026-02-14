/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Animations - Custom blocking animations (Rise-style)
 * Based on Rise 6.2.4 implementation
 */
public class Animations extends Module {

    private static Animations instance;

    private final ModeSetting blockAnimation;
    private final TickSetting onlySword;
    private final TickSetting customOffset;
    private final SliderSetting offsetX;
    private final SliderSetting offsetY;
    private final SliderSetting offsetZ;
    private final SliderSetting scale;

    public Animations() {
        super("Animations", ModuleCategory.VISUALS);
        instance = this;

        this.registerSetting(new DescriptionSetting("Custom blocking animations"));
        this.registerSetting(blockAnimation = new ModeSetting("Mode", new String[]{
            "1.7", "Smooth", "Exhibition", "Swong", "Spin", "Stab", "Tap", "Swing"
        }));
        this.registerSetting(onlySword = new TickSetting("Only Sword", true));
        this.registerSetting(customOffset = new TickSetting("Custom Offset", false));
        this.registerSetting(offsetX = new SliderSetting("X", 0.0, -1.0, 1.0, 0.05));
        this.registerSetting(offsetY = new SliderSetting("Y", 0.0, -1.0, 1.0, 0.05));
        this.registerSetting(offsetZ = new SliderSetting("Z", 0.0, -1.0, 1.0, 0.05));
        this.registerSetting(scale = new SliderSetting("Scale", 1.0, 0.5, 1.5, 0.1));
    }

    public static Animations getInstance() {
        return instance;
    }

    public String getSelectedAnimation() {
        return blockAnimation.getSelected();
    }

    public boolean isOnlySword() {
        return onlySword.isEnabled();
    }

    public static boolean shouldApplyAnimation() {
        return instance != null && instance.enabled;
    }

    /**
     * Apply blocking animation transform
     * Called when player is blocking with sword
     */
    public static void applyBlockingAnimation(float equipProgress, float swingProgress) {
        if (instance == null || !instance.enabled) return;

        String mode = instance.blockAnimation.getSelected();
        float convertedProgress = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        double scaleVal = instance.scale.getValue();

        // Apply custom offset if enabled
        if (instance.customOffset.isEnabled()) {
            GlStateManager.translate(
                instance.offsetX.getValue(),
                instance.offsetY.getValue(),
                instance.offsetZ.getValue()
            );
        }

        switch (mode) {
            case "1.7":
                // Classic 1.7 blocking - shows swing while blocking
                transformFirstPersonItem(equipProgress, swingProgress);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                doBlockTransformations();
                break;

            case "Smooth":
                // Sigma-style smooth animation
                transformFirstPersonItem(equipProgress, 0.0F);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                float y = -convertedProgress * 2.0F;
                GlStateManager.translate(0.0F, y / 10.0F + 0.1F, 0.0F);
                GlStateManager.rotate(y * 10.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(250, 0.2F, 1.0F, -0.6F);
                GlStateManager.rotate(-10.0F, 1.0F, 0.5F, 1.0F);
                GlStateManager.rotate(-y * 20.0F, 1.0F, 0.5F, 1.0F);
                break;

            case "Exhibition":
                // Exhibition-style animation
                GlStateManager.translate(0.0f, -0.05f, 0.0f);
                transformFirstPersonItem(equipProgress / 2.0F, 0.0F);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                GlStateManager.translate(0.0F, 0.3F, 0.0F);
                GlStateManager.rotate(-convertedProgress * 31.0F, 1.0F, 0.0F, 2.0F);
                GlStateManager.rotate(-convertedProgress * 33.0F, 1.5F, (convertedProgress / 1.1F), 0.0F);
                doBlockTransformations();
                break;

            case "Swong":
                // Swong-style animation
                GlStateManager.translate(0.0f, 0.1f, -0.05f);
                transformFirstPersonItem(equipProgress / 2.0F, swingProgress);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                GlStateManager.rotate(convertedProgress * 30.0F, -convertedProgress, -0.0F, 9.0F);
                GlStateManager.rotate(convertedProgress * 40.0F, 1.0F, -convertedProgress, -0.0F);
                doBlockTransformations();
                break;

            case "Spin":
                // Spinning sword animation
                transformFirstPersonItem(equipProgress, 0.0F);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                GlStateManager.translate(0, 0.2F, -1);
                GlStateManager.rotate(-59, -1, 0, 3);
                GlStateManager.rotate(-(System.currentTimeMillis() / 2 % 360), 1, 0, 0.0F);
                GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                break;

            case "Stab":
                // Stabbing animation
                float spin = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
                GlStateManager.translate(0.6f, 0.3f, -0.6f + -spin * 0.7);
                GlStateManager.rotate(6090, 0.0f, 0.0f, 0.1f);
                GlStateManager.rotate(6085, 0.0f, 0.1f, 0.0f);
                GlStateManager.rotate(6110, 0.1f, 0.0f, 0.0f);
                transformFirstPersonItem(0.0F, 0.0f);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                doBlockTransformations();
                break;

            case "Tap":
                // Tap animation
                GL11.glTranslatef(0, 0.3f, 0);
                float smooth = (swingProgress * 0.8f - (swingProgress * swingProgress) * 0.8f);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
                GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(smooth * -90.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(0.37F, 0.37F, 0.37F);
                doBlockTransformations();
                break;

            case "Swing":
                // Simple swing animation
                transformFirstPersonItem(equipProgress, swingProgress);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                doBlockTransformations();
                GlStateManager.translate(-0.3F, -0.1F, -0.0F);
                break;

            default:
                // Vanilla blocking
                transformFirstPersonItem(equipProgress, 0.0F);
                GlStateManager.scale(scaleVal, scaleVal, scaleVal);
                doBlockTransformations();
                break;
        }
    }

    /**
     * Standard first person item transform (from vanilla ItemRenderer)
     */
    public static void transformFirstPersonItem(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    /**
     * Block transformation (from vanilla ItemRenderer)
     */
    public static void doBlockTransformations() {
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public String getDisplaySuffix() {
        return " \u00a77" + blockAnimation.getSelected();
    }
}
