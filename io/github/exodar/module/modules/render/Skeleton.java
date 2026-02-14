/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.modules.misc.AntiBot;
import io.github.exodar.setting.TickSetting;
import io.github.exodar.setting.DescriptionSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Skeleton - Renders skeleton lines on players showing their pose
 * Called from CustomRenderPlayer.doRender() with correct coordinates
 */
public class Skeleton extends Module {

    private SliderSetting lineWidth;
    private ColorSetting color;
    private TickSetting showInvisibles;

    private static final Map<EntityPlayer, float[][]> modelRotations = new HashMap<>();

    public Skeleton() {
        super("Skeleton", ModuleCategory.VISUALS);

        this.registerSetting(new DescriptionSetting("Skeleton ESP"));
        this.registerSetting(lineWidth = new SliderSetting("Line Width", 1.5, 0.5, 5.0, 0.5));
        this.registerSetting(color = new ColorSetting("Color", 255, 255, 255));
        this.registerSetting(showInvisibles = new TickSetting("Show Invisibles", false));
    }

    @Override
    public void onDisable() {
        modelRotations.clear();
    }

    public boolean shouldShowInvisibles() {
        return showInvisibles.isEnabled();
    }

    /**
     * Called from CustomRenderPlayer.doRender() with correct x, y, z coordinates
     */
    public void renderSkeleton(EntityPlayer player, double x, double y, double z, float partialTicks) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (player == mc.thePlayer) return;
        if (AntiBot.isBotForVisuals(player)) return;
        if (player.isInvisible() && !showInvisibles.isEnabled()) return;

        // Update model rotations
        updateModelRotations(player, partialTicks);

        float[][] rotations = modelRotations.get(player);
        if (rotations == null) return;

        // Setup GL state - use direct GL11 calls for shader compatibility
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth((float) lineWidth.getValue());

        // Set color - cyan for friends, or configured color
        int c;
        if (io.github.exodar.module.modules.misc.Friends.isFriend(player.getName())) {
            c = 0x55FFFF; // Cyan for friends
        } else {
            c = color.getColor();
        }
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;
        GL11.glColor4f(r, g, b, 1.0f);

        // Translate to entity position (using coordinates from doRender)
        GL11.glTranslated(x, y, z);

        // Rotate to match body orientation
        float bodyYawOffset = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        GL11.glRotatef(-bodyYawOffset, 0, 1, 0);

        // Sneaking offset
        if (player.isSneaking()) {
            GL11.glTranslated(0, 0, -0.235);
        }

        float legHeight = player.isSneaking() ? 0.6F : 0.75F;

        // Check for slim arms
        float armWidth = 0;
        try {
            Render render = mc.getRenderManager().getEntityRenderObject(player);
            if (render instanceof RenderPlayer) {
                RenderPlayer rp = (RenderPlayer) render;
                ModelPlayer model = rp.getMainModel();
                java.lang.reflect.Field smallArmsField = ModelPlayer.class.getDeclaredField("smallArms");
                smallArmsField.setAccessible(true);
                if (smallArmsField.getBoolean(model)) {
                    armWidth = 0.05F;
                }
            }
        } catch (Exception ignored) {}

        // Right Leg
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.125f, legHeight, 0);
        if (rotations[3][0] != 0.0F) GL11.glRotatef(rotations[3][0] * 57.2958F, 1.0F, 0.0F, 0.0F);
        if (rotations[3][1] != 0.0F) GL11.glRotatef(rotations[3][1] * 57.2958F, 0.0F, 1.0F, 0.0F);
        if (rotations[3][2] != 0.0F) GL11.glRotatef(rotations[3][2] * 57.2958F, 0.0F, 0.0F, 1.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, -legHeight, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        // Left Leg
        GL11.glPushMatrix();
        GL11.glTranslatef(0.125f, legHeight, 0);
        if (rotations[4][0] != 0.0F) GL11.glRotatef(rotations[4][0] * 57.2958F, 1.0F, 0.0F, 0.0F);
        if (rotations[4][1] != 0.0F) GL11.glRotatef(rotations[4][1] * 57.2958F, 0.0F, 1.0F, 0.0F);
        if (rotations[4][2] != 0.0F) GL11.glRotatef(rotations[4][2] * 57.2958F, 0.0F, 0.0F, 1.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, -legHeight, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        // Upper body offset
        if (player.isSneaking()) {
            GL11.glTranslatef(0, 0, 0.25f);
        }

        GL11.glPushMatrix();
        if (player.isSneaking()) {
            GL11.glTranslatef(0, -0.05f, -0.01725f);
        }

        // Right Arm
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.375f + armWidth, legHeight + 0.55f, 0);
        if (rotations[1][0] != 0.0F) GL11.glRotatef(rotations[1][0] * 57.2958F, 1.0F, 0.0F, 0.0F);
        if (rotations[1][1] != 0.0F) GL11.glRotatef(rotations[1][1] * 57.2958F, 0.0F, 1.0F, 0.0F);
        if (rotations[1][2] != 0.0F) GL11.glRotatef(-rotations[1][2] * 57.2958F, 0.0F, 0.0F, 1.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, -0.5, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        // Left Arm
        GL11.glPushMatrix();
        GL11.glTranslatef(0.375f - armWidth, legHeight + 0.55f, 0);
        if (rotations[2][0] != 0.0F) GL11.glRotatef(rotations[2][0] * 57.2958F, 1.0F, 0.0F, 0.0F);
        if (rotations[2][1] != 0.0F) GL11.glRotatef(rotations[2][1] * 57.2958F, 0.0F, 1.0F, 0.0F);
        if (rotations[2][2] != 0.0F) GL11.glRotatef(-rotations[2][2] * 57.2958F, 0.0F, 0.0F, 1.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, -0.5, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        // Head
        float yawHead = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks;
        GL11.glRotatef(bodyYawOffset - yawHead, 0, 1, 0);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0f, legHeight + 0.55f, 0);
        if (rotations[0][0] != 0.0F) GL11.glRotatef(rotations[0][0] * 57.2958F, 1.0F, 0.0F, 0.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, 0.3, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPopMatrix();

        // Body structure
        if (player.isSneaking()) {
            GL11.glRotatef(25, 1, 0, 0);
            GL11.glTranslatef(0, -0.16175f, -0.48025f);
        }

        // Hip line
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0f, legHeight, 0);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(-0.125, 0, 0);
        GL11.glVertex3d(0.125, 0, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        // Spine
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0f, legHeight, 0);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(0, 0, 0);
        GL11.glVertex3d(0, 0.55, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        // Shoulder line
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0f, legHeight + 0.55f, 0);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(-0.375 + armWidth, 0, 0);
        GL11.glVertex3d(0.375 - armWidth, 0, 0);
        GL11.glEnd();
        GL11.glPopMatrix();

        // Restore GL state - use direct GL11 calls for shader compatibility
        GL11.glLineWidth(1.0f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    private void updateModelRotations(EntityPlayer player, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();

        try {
            Render render = mc.getRenderManager().getEntityRenderObject(player);
            if (render instanceof RenderPlayer) {
                RenderPlayer rp = (RenderPlayer) render;
                ModelPlayer model = rp.getMainModel();

                float limbSwing = player.limbSwing - player.limbSwingAmount * (1.0F - partialTicks);
                float limbSwingAmount = player.prevLimbSwingAmount + (player.limbSwingAmount - player.prevLimbSwingAmount) * partialTicks;
                float ageInTicks = player.ticksExisted + partialTicks;

                float yawHead = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks;
                float yawBody = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
                float netHeadYaw = yawHead - yawBody;
                float headPitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;

                model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F, player);

                float[][] rotations = modelRotations.getOrDefault(player, new float[5][3]);

                // Head
                rotations[0][0] = model.bipedHead.rotateAngleX;
                rotations[0][1] = model.bipedHead.rotateAngleY;
                rotations[0][2] = model.bipedHead.rotateAngleZ;

                // Right Arm
                rotations[1][0] = model.bipedRightArm.rotateAngleX;
                rotations[1][1] = model.bipedRightArm.rotateAngleY;
                rotations[1][2] = model.bipedRightArm.rotateAngleZ;

                // Left Arm
                rotations[2][0] = model.bipedLeftArm.rotateAngleX;
                rotations[2][1] = model.bipedLeftArm.rotateAngleY;
                rotations[2][2] = model.bipedLeftArm.rotateAngleZ;

                // Right Leg
                rotations[3][0] = model.bipedRightLeg.rotateAngleX;
                rotations[3][1] = model.bipedRightLeg.rotateAngleY;
                rotations[3][2] = model.bipedRightLeg.rotateAngleZ;

                // Left Leg
                rotations[4][0] = model.bipedLeftLeg.rotateAngleX;
                rotations[4][1] = model.bipedLeftLeg.rotateAngleY;
                rotations[4][2] = model.bipedLeftLeg.rotateAngleZ;

                modelRotations.put(player, rotations);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
