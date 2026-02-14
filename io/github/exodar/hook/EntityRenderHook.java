/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.hook;

import io.github.exodar.event.EventBus;
import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Render3DEvent;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.glu.Project;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hook that replaces EntityRenderer with a proxy to fire Render3DEvent
 * at the correct point in the render pipeline.
 *
 * OPTIMIZED: Calls super.renderWorldPass() via reflection (1 call) instead of
 * reimplementing the entire method with ~15 reflection calls.
 */
public class EntityRenderHook extends Thread {

    private final Minecraft mc;
    private static EntityRenderHook instance;
    private static final AtomicBoolean hooked = new AtomicBoolean(false);

    // Listeners for Render3DEvent
    private static final List<Render3DListener> listeners = new ArrayList<>();

    public interface Render3DListener {
        void onRender3D(Render3DEvent event);
    }

    public EntityRenderHook(Minecraft minecraft) {
        super("Exodar-EntityRenderHook");
        this.mc = minecraft;
        instance = this;
        setDaemon(true);
    }

    public static void addListener(Render3DListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(Render3DListener listener) {
        listeners.remove(listener);
    }

    public static void clearListeners() {
        listeners.clear();
    }

    public static boolean isHooked() {
        return hooked.get();
    }

    static void fireRender3DEvent(float partialTicks) {
        if (listeners.isEmpty()) return;

        Render3DEvent event = new Render3DEvent(partialTicks);
        for (Render3DListener listener : listeners) {
            try {
                listener.onRender3D(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        try {
            // Wait for the game to initialize
            Thread.sleep(2000);

            // NOTE: We always install the proxy now because the bytecode hook for renderWorldPass
            // was removed (it fired AFTER hand rendering, causing ESP positions to be wrong).
            // The proxy fires Render3DEvent BEFORE hand rendering, which is correct.
            if (false) { // Disabled - always install proxy
                System.out.println("[Exodar] Bytecode patching is active - skipping EntityRenderHook proxy installation");
                hooked.set(true); // Mark as hooked to prevent retry
                return;
            }

            while (!isInterrupted() && !hooked.get()) {
                Thread.sleep(500);

                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.entityRenderer != null) {
                    mc.addScheduledTask(() -> {
                        try {
                            if (!hooked.get()) {
                                hookEntityRenderer(mc);
                                hooked.set(true);
                                System.out.println("[Exodar] EntityRenderHook installed successfully!");
                            }
                        } catch (Exception e) {
                            System.out.println("[Exodar] Failed to install EntityRenderHook: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    Thread.sleep(1000);
                    if (hooked.get()) break;
                }
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookEntityRenderer(Minecraft mc) throws Exception {
        EntityRenderer original = mc.entityRenderer;

        // Get the renderWorld method
        Method renderWorldMethod = null;
        for (Method method : EntityRenderer.class.getDeclaredMethods()) {
            if (method.getName().equals("renderWorld") &&
                    method.getParameterCount() == 2) {
                // renderWorld(float partialTicks, long finishTimeNano)
                Class<?>[] params = method.getParameterTypes();
                if (params[0] == float.class && params[1] == long.class) {
                    renderWorldMethod = method;
                    break;
                }
            }
        }

        if (renderWorldMethod == null) {
            System.err.println("Could not find renderWorld method in EntityRenderer");
            return;
        }

        // Create a proxy that intercepts renderWorld calls
        final Method originalMethod = renderWorldMethod;
        originalMethod.setAccessible(true);

        // Create proxy
        EntityRendererProxy proxy = new EntityRendererProxy(mc, original, originalMethod);

        // Replace entityRenderer field
        Field rendererField = null;
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (f.getType() == EntityRenderer.class) {
                rendererField = f;
                break;
            }
        }

        if (rendererField == null) {
            throw new RuntimeException("Could not find entityRenderer field");
        }

        rendererField.setAccessible(true);
        rendererField.set(mc, proxy);
    }

    public static void cleanup() {
        hooked.set(false);
        clearListeners();
        if (instance != null) {
            instance.interrupt();
            instance = null;
        }
    }

    /**
     * Proxy class that intercepts EntityRenderer.renderWorldPass
     * OPTIMIZED: Calls super method via reflection instead of reimplementing
     */
    public static class EntityRendererProxy extends EntityRenderer {
        private final EntityRenderer original;
        private final Method renderWorldMethod;
        private final Method renderWorldPassMethod;
        private final Minecraft mc;
        private long prevFrameTime = Minecraft.getSystemTime();
        private long renderEndNanoTime;

        private Field smoothCamYawField;
        private Field smoothCamPitchField;
        private Field smoothCamPartialTicksField;
        private Field smoothCamFilterXField;
        private Field smoothCamFilterYField;

        // Private method reflections
        private Method updateLightmapMethod;
        private Method getMouseOverMethod;
        private Method isDrawBlockOutlineMethod;
        private Method updateFogColorMethod;
        private Method setupCameraTransformMethod;
        private Method setupFogMethod;
        private Method getFOVModifierMethod;
        private Method renderCloudsCheckMethod;
        private Method enableLightmapMethod;
        private Method disableLightmapMethod;
        private Method renderRainSnowMethod;
        private Method renderHandMethod;
        private Method renderWorldDirectionsMethod;

        // Private field reflections
        private Field frameCountField;
        private Field debugViewField;
        private Field renderHandField;
        private Field farPlaneDistanceField;
        private Field theShaderGroupField;
        private Field useShaderField;

        public EntityRendererProxy(Minecraft minecraft, EntityRenderer original, Method renderWorldMethod) {
            super(minecraft, minecraft.getResourceManager());
            this.original = original;
            this.mc = minecraft;
            this.renderWorldMethod = renderWorldMethod;
            this.renderWorldPassMethod = findRenderWorldPassMethod();
            initializeFields();
            initializeMethodReflections();
        }

        private Method findRenderWorldPassMethod() {
            try {
                for (Method method : EntityRenderer.class.getDeclaredMethods()) {
                    if (method.getName().equals("renderWorldPass")) {
                        Class<?>[] params = method.getParameterTypes();
                        if (params.length == 3 && params[0] == int.class && params[1] == float.class && params[2] == long.class) {
                            method.setAccessible(true);
                            return method;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void initializeFields() {
            try {
                smoothCamYawField = EntityRenderer.class.getDeclaredField("smoothCamYaw");
                smoothCamYawField.setAccessible(true);

                smoothCamPitchField = EntityRenderer.class.getDeclaredField("smoothCamPitch");
                smoothCamPitchField.setAccessible(true);

                smoothCamPartialTicksField = EntityRenderer.class.getDeclaredField("smoothCamPartialTicks");
                smoothCamPartialTicksField.setAccessible(true);

                smoothCamFilterXField = EntityRenderer.class.getDeclaredField("smoothCamFilterX");
                smoothCamFilterXField.setAccessible(true);

                smoothCamFilterYField = EntityRenderer.class.getDeclaredField("smoothCamFilterY");
                smoothCamFilterYField.setAccessible(true);

                frameCountField = EntityRenderer.class.getDeclaredField("frameCount");
                frameCountField.setAccessible(true);

                debugViewField = EntityRenderer.class.getDeclaredField("debugView");
                debugViewField.setAccessible(true);

                renderHandField = EntityRenderer.class.getDeclaredField("renderHand");
                renderHandField.setAccessible(true);

                farPlaneDistanceField = EntityRenderer.class.getDeclaredField("farPlaneDistance");
                farPlaneDistanceField.setAccessible(true);

                theShaderGroupField = EntityRenderer.class.getDeclaredField("theShaderGroup");
                theShaderGroupField.setAccessible(true);

                useShaderField = EntityRenderer.class.getDeclaredField("useShader");
                useShaderField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        private void initializeMethodReflections() {
            try {
                updateLightmapMethod = EntityRenderer.class.getDeclaredMethod("updateLightmap", float.class);
                updateLightmapMethod.setAccessible(true);

                getMouseOverMethod = EntityRenderer.class.getDeclaredMethod("getMouseOver", float.class);
                getMouseOverMethod.setAccessible(true);

                isDrawBlockOutlineMethod = EntityRenderer.class.getDeclaredMethod("isDrawBlockOutline");
                isDrawBlockOutlineMethod.setAccessible(true);

                updateFogColorMethod = EntityRenderer.class.getDeclaredMethod("updateFogColor", float.class);
                updateFogColorMethod.setAccessible(true);

                setupCameraTransformMethod = EntityRenderer.class.getDeclaredMethod("setupCameraTransform", float.class, int.class);
                setupCameraTransformMethod.setAccessible(true);

                setupFogMethod = EntityRenderer.class.getDeclaredMethod("setupFog", int.class, float.class);
                setupFogMethod.setAccessible(true);

                getFOVModifierMethod = EntityRenderer.class.getDeclaredMethod("getFOVModifier", float.class, boolean.class);
                getFOVModifierMethod.setAccessible(true);

                renderCloudsCheckMethod = EntityRenderer.class.getDeclaredMethod("renderCloudsCheck", RenderGlobal.class, float.class, int.class);
                renderCloudsCheckMethod.setAccessible(true);

                enableLightmapMethod = EntityRenderer.class.getDeclaredMethod("enableLightmap");
                enableLightmapMethod.setAccessible(true);

                disableLightmapMethod = EntityRenderer.class.getDeclaredMethod("disableLightmap");
                disableLightmapMethod.setAccessible(true);

                renderRainSnowMethod = EntityRenderer.class.getDeclaredMethod("renderRainSnow", float.class);
                renderRainSnowMethod.setAccessible(true);

                renderHandMethod = EntityRenderer.class.getDeclaredMethod("renderHand", float.class, int.class);
                renderHandMethod.setAccessible(true);

                renderWorldDirectionsMethod = EntityRenderer.class.getDeclaredMethod("renderWorldDirections", float.class);
                renderWorldDirectionsMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        private float getSmoothCamYaw() {
            try {
                return smoothCamYawField.getFloat(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return 0.0F;
            }
        }

        private void setSmoothCamYaw(float value) {
            try {
                smoothCamYawField.setFloat(this, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private float getSmoothCamPitch() {
            try {
                return smoothCamPitchField.getFloat(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return 0.0F;
            }
        }

        private void setSmoothCamPitch(float value) {
            try {
                smoothCamPitchField.setFloat(this, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private float getSmoothCamPartialTicks() {
            try {
                return smoothCamPartialTicksField.getFloat(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return 0.0F;
            }
        }

        private void setSmoothCamPartialTicks(float value) {
            try {
                smoothCamPartialTicksField.setFloat(this, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private float getSmoothCamFilterX() {
            try {
                return smoothCamFilterXField.getFloat(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return 0.0F;
            }
        }

        private float getSmoothCamFilterY() {
            try {
                return smoothCamFilterYField.getFloat(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return 0.0F;
            }
        }

        private ShaderGroup getTheShaderGroup() {
            try {
                return (ShaderGroup) theShaderGroupField.get(this);
            } catch (Exception e) {
                return null;
            }
        }

        private boolean getUseShader() {
            try {
                return useShaderField.getBoolean(this);
            } catch (Exception e) {
                return false;
            }
        }

        // Wrapper methods for private methods
        private void updateLightmap(float partialTicks) {
            try {
                updateLightmapMethod.invoke(this, partialTicks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private boolean isDrawBlockOutline() {
            try {
                return (boolean) isDrawBlockOutlineMethod.invoke(this);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private void updateFogColor(float partialTicks) {
            try {
                updateFogColorMethod.invoke(this, partialTicks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void setupCameraTransform(float partialTicks, int pass) {
            try {
                setupCameraTransformMethod.invoke(this, partialTicks, pass);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void setupFog(int mode, float partialTicks) {
            try {
                setupFogMethod.invoke(this, mode, partialTicks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private float getFOVModifier(float partialTicks, boolean useFOVSetting) {
            try {
                return (float) getFOVModifierMethod.invoke(this, partialTicks, useFOVSetting);
            } catch (Exception e) {
                e.printStackTrace();
                return 70.0F;
            }
        }

        private void renderCloudsCheck(RenderGlobal renderGlobal, float partialTicks, int pass) {
            try {
                renderCloudsCheckMethod.invoke(this, renderGlobal, partialTicks, pass);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void renderHand(float partialTicks, int pass) {
            try {
                renderHandMethod.invoke(this, partialTicks, pass);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void renderWorldDirections(float partialTicks) {
            try {
                renderWorldDirectionsMethod.invoke(this, partialTicks);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Wrapper methods for private fields
        private int getFrameCount() {
            try {
                return frameCountField.getInt(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return 0;
            }
        }

        private void setFrameCount(int value) {
            try {
                frameCountField.setInt(this, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private boolean isDebugView() {
            try {
                return debugViewField.getBoolean(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
        }

        private boolean getRenderHand() {
            try {
                return renderHandField.getBoolean(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return true;
            }
        }

        private float getFarPlaneDistance() {
            try {
                return farPlaneDistanceField.getFloat(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return 256.0F;
            }
        }

        // Cache for Optifine detection
        private static Boolean optifinePresent = null;

        /**
         * Check if Optifine is present (not just if shaders are active).
         * When Optifine is present, we ALWAYS delegate to original to avoid breaking shaders.
         */
        private static boolean isOptifinePresent() {
            if (optifinePresent == null) {
                try {
                    Class.forName("net.optifine.shaders.Shaders");
                    optifinePresent = true;
                    System.out.println("[Exodar] Optifine detected - using delegation mode for shader compatibility");
                } catch (ClassNotFoundException e) {
                    try {
                        Class.forName("shadersmod.client.Shaders");
                        optifinePresent = true;
                        System.out.println("[Exodar] Optifine (old) detected - using delegation mode for shader compatibility");
                    } catch (ClassNotFoundException e2) {
                        try {
                            // Also check for OptiFine main class
                            Class.forName("optifine.OptiFineClassTransformer");
                            optifinePresent = true;
                            System.out.println("[Exodar] Optifine detected - using delegation mode for shader compatibility");
                        } catch (ClassNotFoundException e3) {
                            optifinePresent = false;
                            System.out.println("[Exodar] Optifine not detected - using reimplemented mode for Lunar compatibility");
                        }
                    }
                }
            }
            return optifinePresent;
        }

        @Override
        public void renderWorld(float partialTicks, long finishTimeNano) {
            // ALWAYS use reimplemented version like friend's client (GhostClient)
            // This fires Render3DEvent BEFORE hand rendering at the correct position
            // Optifine shaders should still work because we're just overriding the method,
            // not changing the rendering pipeline itself
            renderWorldReimplemented(partialTicks, finishTimeNano);

            // NOTE: Render3DEvent is fired inside renderWorldPass at line 774 (BEFORE hand)
            // Do NOT fire it here again - that would be after hand and cause wrong positions
        }

        /**
         * Reimplemented renderWorld for Lunar Client motion blur compatibility.
         * All calls happen on 'this' (proxy) so Lunar can intercept them.
         */
        private void renderWorldReimplemented(float partialTicks, long finishTimeNano) {
            this.updateLightmap(partialTicks);
            if (this.mc.getRenderViewEntity() == null) {
                this.mc.setRenderViewEntity(this.mc.thePlayer);
            }

            this.getMouseOver(partialTicks);
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.5F);
            this.mc.mcProfiler.startSection("center");
            if (this.mc.gameSettings.anaglyph) {
                anaglyphField = 0;
                GlStateManager.colorMask(false, true, true, false);
                this.renderWorldPass(0, partialTicks, finishTimeNano);
                anaglyphField = 1;
                GlStateManager.colorMask(true, false, false, false);
                this.renderWorldPass(1, partialTicks, finishTimeNano);
                GlStateManager.colorMask(true, true, true, false);
            } else {
                this.renderWorldPass(2, partialTicks, finishTimeNano);
            }

            this.mc.mcProfiler.endSection();
        }

        private void renderWorldPass(int pass, float partialTicks, long finishTimeNano) {
            RenderGlobal renderglobal = this.mc.renderGlobal;
            EffectRenderer effectrenderer = this.mc.effectRenderer;
            boolean flag = this.isDrawBlockOutline();
            GlStateManager.enableCull();
            this.mc.mcProfiler.endStartSection("clear");
            GlStateManager.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
            this.updateFogColor(partialTicks);
            GlStateManager.clear(16640);
            this.mc.mcProfiler.endStartSection("camera");
            this.setupCameraTransform(partialTicks, pass);
            ActiveRenderInfo.updateRenderInfo(this.mc.thePlayer, this.mc.gameSettings.thirdPersonView == 2);
            this.mc.mcProfiler.endStartSection("frustum");
            ClippingHelperImpl.getInstance();
            this.mc.mcProfiler.endStartSection("culling");
            ICamera icamera = new Frustum();
            Entity entity = this.mc.getRenderViewEntity();
            double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
            double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
            double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;
            icamera.setPosition(d0, d1, d2);
            if (this.mc.gameSettings.renderDistanceChunks >= 4) {
                this.setupFog(-1, partialTicks);
                this.mc.mcProfiler.endStartSection("sky");
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.getFarPlaneDistance() * 2.0F);
                GlStateManager.matrixMode(5888);
                renderglobal.renderSky(partialTicks, pass);
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float)this.mc.displayWidth / (float)this.mc.displayHeight, 0.05F, this.getFarPlaneDistance() * MathHelper.SQRT_2);
                GlStateManager.matrixMode(5888);
            }

            this.setupFog(0, partialTicks);
            GlStateManager.shadeModel(7425);
            if (entity.posY + (double)entity.getEyeHeight() < (double)128.0F) {
                this.renderCloudsCheck(renderglobal, partialTicks, pass);
            }

            this.mc.mcProfiler.endStartSection("prepareterrain");
            this.setupFog(0, partialTicks);
            this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            RenderHelper.disableStandardItemLighting();
            this.mc.mcProfiler.endStartSection("terrain_setup");
            int frameCount = this.getFrameCount();
            renderglobal.setupTerrain(entity, (double)partialTicks, icamera, frameCount, this.mc.thePlayer.isSpectator());
            this.setFrameCount(frameCount + 1);
            if (pass == 0 || pass == 2) {
                this.mc.mcProfiler.endStartSection("updatechunks");
                this.mc.renderGlobal.updateChunks(finishTimeNano);
            }

            this.mc.mcProfiler.endStartSection("terrain");
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            GlStateManager.disableAlpha();
            renderglobal.renderBlockLayer(EnumWorldBlockLayer.SOLID, (double)partialTicks, pass, entity);
            GlStateManager.enableAlpha();
            renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT_MIPPED, (double)partialTicks, pass, entity);
            this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
            renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT, (double)partialTicks, pass, entity);
            this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
            GlStateManager.shadeModel(7424);
            GlStateManager.alphaFunc(516, 0.1F);
            if (!this.isDebugView()) {
                GlStateManager.matrixMode(5888);
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                RenderHelper.enableStandardItemLighting();
                this.mc.mcProfiler.endStartSection("entities");
                renderglobal.renderEntities(entity, icamera, partialTicks);
                RenderHelper.disableStandardItemLighting();
                this.disableLightmap();
                GlStateManager.matrixMode(5888);
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                if (this.mc.objectMouseOver != null && entity.isInsideOfMaterial(Material.water) && flag) {
                    EntityPlayer entityplayer = (EntityPlayer)entity;
                    GlStateManager.disableAlpha();
                    this.mc.mcProfiler.endStartSection("outline");
                    renderglobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, partialTicks);
                    GlStateManager.enableAlpha();
                }
            }

            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            if (flag && this.mc.objectMouseOver != null && !entity.isInsideOfMaterial(Material.water)) {
                EntityPlayer entityplayer1 = (EntityPlayer)entity;
                GlStateManager.disableAlpha();
                this.mc.mcProfiler.endStartSection("outline");
                renderglobal.drawSelectionBox(entityplayer1, this.mc.objectMouseOver, 0, partialTicks);
                GlStateManager.enableAlpha();
            }

            this.mc.mcProfiler.endStartSection("destroyProgress");
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
            this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
            renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getWorldRenderer(), entity, partialTicks);
            this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
            GlStateManager.disableBlend();
            if (!this.isDebugView()) {
                this.enableLightmap();
                this.mc.mcProfiler.endStartSection("litParticles");
                effectrenderer.renderLitParticles(entity, partialTicks);
                RenderHelper.disableStandardItemLighting();
                this.setupFog(0, partialTicks);
                this.mc.mcProfiler.endStartSection("particles");
                effectrenderer.renderParticles(entity, partialTicks);
                this.disableLightmap();
            }

            GlStateManager.depthMask(false);
            GlStateManager.enableCull();
            this.mc.mcProfiler.endStartSection("weather");
            this.renderRainSnow(partialTicks);
            GlStateManager.depthMask(true);
            renderglobal.renderWorldBorder(entity, partialTicks);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.alphaFunc(516, 0.1F);
            this.setupFog(0, partialTicks);
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GlStateManager.shadeModel(7425);
            this.mc.mcProfiler.endStartSection("translucent");
            renderglobal.renderBlockLayer(EnumWorldBlockLayer.TRANSLUCENT, partialTicks, pass, entity);
            GlStateManager.shadeModel(7424);
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.disableFog();
            if (entity.posY + (double)entity.getEyeHeight() >= (double)128.0F) {
                this.mc.mcProfiler.endStartSection("aboveClouds");
                this.renderCloudsCheck(renderglobal, partialTicks, pass);
            }

            EventBus.post(new Render3DEvent(partialTicks));

            this.mc.mcProfiler.endStartSection("hand");
            if (this.getRenderHand()) {
                GlStateManager.clear(256);
                this.renderHand(partialTicks, pass);
                this.renderWorldDirections(partialTicks);
            }

        }

        /**
         * Override getMouseOver to call Reach/Penetration hooks after the original runs.
         * This allows extending reach and hitting through blocks.
         */
        @Override
        public void getMouseOver(float partialTicks) {
            // Call original first
            super.getMouseOver(partialTicks);

            // Then call our hooks for Reach and Penetration
            try {
                io.github.exodar.Main.onGetMouseOverProxy(this, partialTicks);
            } catch (Exception e) {
                // Silently ignore errors
            }
        }

        /**
         * Override updateCameraAndRender to fire Render2DEvent at the right time.
         * Calls super to let Lunar Client's FreeCam work properly.
         */
        @Override
        public void updateCameraAndRender(float partialTicks, long nanoTime) {
            // Let parent handle everything (including FreeCam from Lunar)
            super.updateCameraAndRender(partialTicks, nanoTime);

            // Fire Render2DEvent after the GUI has been rendered
            if (!this.mc.skipRenderWorld && this.mc.theWorld != null) {
                if (!this.mc.gameSettings.hideGUI || this.mc.currentScreen != null) {
                    // Setup GL state for 2D rendering
                    GlStateManager.enableTexture2D();
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

                    GlStateManager.pushMatrix();
                    EventBus.post(new Render2DEvent(partialTicks));
                    GlStateManager.popMatrix();

                    GlStateManager.disableBlend();
                }
            }
        }
    }
}

