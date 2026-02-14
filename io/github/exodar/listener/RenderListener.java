/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.listener;

import io.github.exodar.Main;
import io.github.exodar.account.AccountManager;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ScaledResolution;

import io.github.exodar.api.BuildInfo;
import io.github.exodar.config.UserConfig;
import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.ui.ArrayListConfig;
import io.github.exodar.ui.ClientSkin;
import io.github.exodar.ui.ModuleAnimation;
import io.github.exodar.ui.ModuleNotification;
import io.github.exodar.ui.VerdanaFont;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import io.github.exodar.shader.RoundedQuadShader;
import io.github.exodar.shader.GlowShader;
// import io.github.exodar.shader.BloomShader; // DISABLED - performance optimization

/**
 * RenderListener - Optimized for performance
 */
public class RenderListener {

    private final Map<String, ModuleAnimation> moduleAnimations = new HashMap<>();
    private static final long ANIMATION_DURATION = 600; // milliseconds (much smoother transition)

    private long startTime = 0;

    // Cached data
    private static final List<Module> cachedEnabledModules = new ArrayList<>();
    private static int lastModuleStateHash = 0;
    private static int screenWidth = 854;
    private static int screenHeight = 480;
    private static long lastScreenUpdate = 0;

    // Performance: Cache sorted modules list
    private static List<Module> cachedSortedModules = null;
    private static long lastSortTime = 0;
    private static final long SORT_INTERVAL = 500; // Only sort every 500ms
    private static final Map<String, Integer> cachedTextWidths = new HashMap<>();
    private static long lastWidthCacheTime = 0;

    @Subscribe
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;


        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }

        ModuleManager moduleManager = Main.getModuleManager();
        float partialTicks = event.getPartialTicks();

        // Update screen size periodically
        long now = System.currentTimeMillis();
        if (now - lastScreenUpdate > 100) {
            lastScreenUpdate = now;
            try {
                ScaledResolution sr = new ScaledResolution(mc);
                screenWidth = sr.getScaledWidth();
                screenHeight = sr.getScaledHeight();
            } catch (Exception ignored) {}
        }

        // Draw watermark
        if (ArrayListConfig.watermarkEnabled) {
            String watermarkName = ArrayListConfig.watermarkText;
            if (watermarkName != null && !watermarkName.isEmpty()) {
                String displayName = ClientSkin.getWatermarkText(watermarkName);

                int firstLetterColor;
                int restColor;
                if (ClientSkin.isEasterEggActive()) {
                    firstLetterColor = ClientSkin.getWatermarkFirstColor();
                    restColor = ClientSkin.getWatermarkRestColor();
                } else {
                    firstLetterColor = ArrayListConfig.getWatermarkFirstLetterColor();
                    restColor = 0xFFFFFF;
                }

                String firstLetter = displayName.substring(0, 1);
                String rest = displayName.length() > 1 ? displayName.substring(1) : "";

                fr.drawStringWithShadow(firstLetter, 5, 5, firstLetterColor);
                int offset = fr.getStringWidth(firstLetter);
                fr.drawStringWithShadow(rest, 5 + offset, 5, restColor);
            }
        }

        // ArrayList
        if (moduleManager != null && ArrayListConfig.arrayListEnabled) {
            int padding = 2;  // Closer to edge
            // Start lower on left side to avoid watermark
            int yOffset = ClientSkin.isArrayListOnLeft() ? 20 : 2;
            // Increase line height for Exodar theme to fix z-fighting
            int lineHeight = ArrayListConfig.exodarThemeEnabled ? 11 : 10;

            long currentTime = System.currentTimeMillis();

            // Performance: Only sort modules periodically, not every frame
            if (cachedSortedModules == null || currentTime - lastSortTime > SORT_INTERVAL) {
                lastSortTime = currentTime;
                cachedSortedModules = new ArrayList<>(moduleManager.getModules());

                // Clear width cache periodically
                if (currentTime - lastWidthCacheTime > 2000) {
                    cachedTextWidths.clear();
                    lastWidthCacheTime = currentTime;
                }

                // Sort using cached widths
                cachedSortedModules.sort((m1, m2) -> {
                    int w1 = getCachedWidth(fr, m1);
                    int w2 = getCachedWidth(fr, m2);
                    return Integer.compare(w2, w1);
                });
            }
            List<Module> modules = cachedSortedModules;

            UserConfig userConfig = UserConfig.getInstance();

            for (Module module : modules) {
                // Skip HUD module - never show in ArrayList
                if ("HUD".equalsIgnoreCase(module.getName())) {
                    continue;
                }

                // Skip hidden modules
                if (userConfig.isModuleHidden(module)) {
                    continue;
                }

                String name = module.getName();
                ModuleAnimation anim = moduleAnimations.get(name);

                if (anim == null) {
                    // First time seeing this module
                    anim = new ModuleAnimation();
                    if (module.isEnabled()) {
                        anim.targetState = AnimationState.VISIBLE;
                        anim.currentState = AnimationState.VISIBLE;
                        anim.progress = 1.0f;
                    } else {
                        anim.targetState = AnimationState.HIDDEN;
                        anim.currentState = AnimationState.HIDDEN;
                        anim.progress = 0.0f;
                    }
                    moduleAnimations.put(name, anim);
                } else {
                    // Update target state based on enabled status
                    AnimationState newTarget = module.isEnabled() ? AnimationState.VISIBLE : AnimationState.HIDDEN;

                    if (anim.targetState != newTarget) {
                        anim.targetState = newTarget;
                        anim.animationStartTime = currentTime;
                        anim.currentState = (newTarget == AnimationState.VISIBLE) ? AnimationState.SLIDING_IN : AnimationState.SLIDING_OUT;
                    }
                }
            }

            List<ModuleRenderData> modulesToRender = new ArrayList<>();

            for (Module module : modules) {
                // Skip hidden modules
                if (userConfig.isModuleHidden(module)) {
                    continue;
                }

                String suffix = module.getDisplaySuffix();
                String suffixPlain = suffix != null ? suffix.replaceAll("\u00a7.", "") : "";
                String moduleName = module.getName() + suffixPlain;
                ModuleAnimation anim = moduleAnimations.get(module.getName());
                if (anim == null) continue; // Skip if no animation state

                // Update animation progress
                if (anim.currentState == AnimationState.SLIDING_IN) {
                    long elapsed = currentTime - anim.animationStartTime;
                    anim.progress = Math.min(1.0f, elapsed / (float) ANIMATION_DURATION);

                    if (anim.progress >= 1.0f) {
                        anim.currentState = AnimationState.VISIBLE;
                        anim.progress = 1.0f;
                    }
                } else if (anim.currentState == AnimationState.SLIDING_OUT) {
                    long elapsed = currentTime - anim.animationStartTime;
                    anim.progress = Math.max(0.0f, 1.0f - (elapsed / (float) ANIMATION_DURATION));

                    if (anim.progress <= 0.0f) {
                        anim.currentState = AnimationState.HIDDEN;
                        anim.progress = 0.0f;
                    }
                }

                // Only render if not completely hidden
                if (anim.progress > 0.0f) {
                    // Apply smoothstep easing for smoother glow transition
                    float smoothProgress = anim.progress * anim.progress * (3.0f - 2.0f * anim.progress);
                    modulesToRender.add(new ModuleRenderData(module.getName(), suffixPlain, smoothProgress, module.getCategory()));
                }
            }

            // Check if ArrayList should be on left (Huzuni, Wurst Easter eggs)
            boolean leftSide = ClientSkin.isArrayListOnLeft();

            // Exodar theme offset (3px inward)
            int exodarOffsetX = ArrayListConfig.exodarThemeEnabled ? 3 : 0;
            int exodarOffsetY = ArrayListConfig.exodarThemeEnabled ? 2 : 0;

            // Pre-calculate all render positions for layered rendering
            List<ModuleRenderInfo> renderInfos = new ArrayList<>();
            int tempYOffset = yOffset + exodarOffsetY;

            for (ModuleRenderData data : modulesToRender) {
                String fullText = data.name + data.suffix;
                int textWidth = mc.fontRendererObj.getStringWidth(fullText);
                float slideOffset = (1.0f - data.progress) * 50;

                int xPos;
                if (leftSide) {
                    xPos = (int) (padding + exodarOffsetX - slideOffset);
                } else {
                    xPos = (int) (screenWidth - textWidth - padding - exodarOffsetX + slideOffset);
                }

                int alpha = (int) (data.progress * 255);
                int color;
                if (ClientSkin.isEasterEggActive()) {
                    color = ClientSkin.getModuleColor(data.category, tempYOffset, screenHeight);
                } else {
                    color = ArrayListConfig.getColorForY(tempYOffset, screenHeight);
                }

                int fullWidth = mc.fontRendererObj.getStringWidth(data.name + data.suffix);

                ModuleRenderInfo info = new ModuleRenderInfo();
                info.name = data.name;
                info.suffix = data.suffix;
                info.xPos = xPos;
                info.yPos = tempYOffset;
                info.alpha = alpha;
                info.color = color;
                info.fullWidth = fullWidth;
                renderInfos.add(info);

                tempYOffset += lineHeight;
            }

            // === EXODAR THEME - Rise-style ArrayList ===
            if (ArrayListConfig.exodarThemeEnabled && !renderInfos.isEmpty()) {
                int moduleHeight = 10;

                // LAYER 1: Glow effect (simple fallback - BloomShader DISABLED for performance)
                if (ArrayListConfig.exodarGlow) {
                    // Fallback: Simple glow without shader
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

                    for (ModuleRenderInfo info : renderInfos) {
                        float x = info.xPos - 2;
                        float y = info.yPos - 1;
                        float w = info.fullWidth + 6;
                        float h = moduleHeight + 1;

                        int c = info.color & 0xFFFFFF;
                        float r = ((c >> 16) & 0xFF) / 255.0f;
                        float g = ((c >> 8) & 0xFF) / 255.0f;
                        float b = (c & 0xFF) / 255.0f;

                        // Multi-layer glow fallback
                        drawGlowRect(x - 6, y - 6, w + 12, h + 12, r, g, b, 0.04f);
                        drawGlowRect(x - 4, y - 4, w + 8, h + 8, r, g, b, 0.08f);
                        drawGlowRect(x - 2, y - 2, w + 4, h + 4, r, g, b, 0.12f);
                        drawGlowRect(x, y, w, h, r, g, b, 0.2f);
                    }

                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.enableTexture2D();
                }

                // LAYER 2: Background
                if (ArrayListConfig.exodarBackground) {
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

                    float bgAlphaBase = ArrayListConfig.exodarBackgroundAlpha / 255.0f;

                    GL11.glBegin(GL11.GL_QUADS);
                    for (ModuleRenderInfo info : renderInfos) {
                        float x = info.xPos - 2;
                        float y = info.yPos - 1;
                        float w = info.fullWidth + 4;
                        float h = moduleHeight + 1;

                        // Multiply background alpha by animation alpha for smooth fade
                        float animAlpha = info.alpha / 255.0f;
                        float finalBgAlpha = bgAlphaBase * animAlpha;

                        GL11.glColor4f(0, 0, 0, finalBgAlpha);
                        GL11.glVertex2f(x, y + h);
                        GL11.glVertex2f(x + w, y + h);
                        GL11.glVertex2f(x + w, y);
                        GL11.glVertex2f(x, y);
                    }
                    GL11.glEnd();

                    GlStateManager.enableTexture2D();
                }

                // LAYER 3: Sidebar (Rise-style, right edge)
                if (ArrayListConfig.exodarSidebar) {
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

                    GL11.glBegin(GL11.GL_QUADS);
                    for (ModuleRenderInfo info : renderInfos) {
                        float x = info.xPos + info.fullWidth + 3;
                        float y = info.yPos - 1;
                        float h = moduleHeight + 1;

                        int c = info.color & 0xFFFFFF;
                        float r = ((c >> 16) & 0xFF) / 255.0f;
                        float g = ((c >> 8) & 0xFF) / 255.0f;
                        float b = (c & 0xFF) / 255.0f;

                        // Use animation alpha for smooth fade
                        float animAlpha = info.alpha / 255.0f;

                        GL11.glColor4f(r, g, b, animAlpha);
                        GL11.glVertex2f(x, y + h);
                        GL11.glVertex2f(x + 2, y + h);
                        GL11.glVertex2f(x + 2, y);
                        GL11.glVertex2f(x, y);
                    }
                    GL11.glEnd();

                    GlStateManager.enableTexture2D();
                }

                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            }

            // LAYER 4: Text (always render)
            // Disable depth test to fix z-fighting
            GlStateManager.disableDepth();
            for (ModuleRenderInfo info : renderInfos) {
                int colorWithAlpha = (info.alpha << 24) | (info.color & 0xFFFFFF);
                int suffixColor;
                if (ClientSkin.isEasterEggActive()) {
                    suffixColor = (info.alpha << 24) | ClientSkin.getSuffixColor();
                } else {
                    suffixColor = (info.alpha << 24) | 0xAAAAAA;
                }

                mc.fontRendererObj.drawStringWithShadow(info.name, info.xPos, info.yPos, colorWithAlpha);

                if (!info.suffix.isEmpty()) {
                    int nameWidth = mc.fontRendererObj.getStringWidth(info.name);
                    mc.fontRendererObj.drawStringWithShadow(info.suffix, info.xPos + nameWidth, info.yPos, suffixColor);
                }
            }
            GlStateManager.enableDepth();
        }

            /*

            // Calculate state hash (single loop)
            int currentStateHash = 0;
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                currentStateHash = currentStateHash * 31 + (m.isEnabled() ? 1 : 0);
                if (!m.isEnabled() && !"HUD".equals(m.getName())) {
                    ModuleAnimation.setDisabled(m);
                }
            }

            // Rebuild cache when state changes
            if (currentStateHash != lastModuleStateHash) {
                cachedEnabledModules.clear();
                for (int i = 0; i < modules.size(); i++) {
                    Module m = modules.get(i);
                    if (m.isEnabled() && !"HUD".equals(m.getName())) {
                        cachedEnabledModules.add(m);
                    }
                }
                lastModuleStateHash = currentStateHash;
            }

            // Always sort (suffixes can change dynamically)
            cachedEnabledModules.sort((m1, m2) -> {
                int w1 = fr.getStringWidth(m1.getName() + getSuffixPlain(m1));
                int w2 = fr.getStringWidth(m2.getName() + getSuffixPlain(m2));
                return Integer.compare(w2, w1);
            });

            // Render enabled modules
            boolean leftSide = ClientSkin.isArrayListOnLeft();
            int yOffset = leftSide ? 25 : 2;

            for (int i = 0; i < cachedEnabledModules.size(); i++) {
                Module module = cachedEnabledModules.get(i);
                String name = module.getName();
                String suffix = module.getDisplaySuffix();
                String suffixPlain = suffix != null ? suffix.replaceAll("\u00a7.", "") : "";

                ModuleAnimation.setEnabled(module, yOffset);
                ModuleAnimation.AnimationState anim = ModuleAnimation.getState(module);

                int totalWidth = fr.getStringWidth(name + suffixPlain);
                float animX = leftSide ? (2 - anim.getX(partialTicks)) : (screenWidth - totalWidth - 2 + anim.getX(partialTicks));
                float animY = anim.getY(partialTicks);

                int color = ClientSkin.isEasterEggActive()
                    ? ClientSkin.getModuleColor(module.getCategory(), (int) animY, screenHeight)
                    : ArrayListConfig.getColorForY((int) animY, screenHeight);

                fr.drawStringWithShadow(name, animX, animY, color);
                if (suffix != null && !suffix.isEmpty()) {
                    int suffixColor = ClientSkin.isEasterEggActive() ? ClientSkin.getSuffixColor() : 0x7F7F7F;
                    fr.drawStringWithShadow(suffix, animX + fr.getStringWidth(name), animY, suffixColor);
                }
                yOffset += 10;
            }

            // Render exiting modules
            List<String> exitingModules = ModuleAnimation.updateExitingModules();
            for (int i = 0; i < exitingModules.size(); i++) {
                String exitingName = exitingModules.get(i);
                ModuleAnimation.AnimationState anim = ModuleAnimation.getStateByName(exitingName);
                if (anim == null) continue;

                Module exitingModule = moduleManager.getModuleByName(exitingName);
                String suffix = exitingModule != null ? exitingModule.getDisplaySuffix() : "";
                String suffixPlain = suffix != null ? suffix.replaceAll("\u00a7.", "") : "";

                int totalWidth = fr.getStringWidth(exitingName + suffixPlain);
                float animX = leftSide ? (2 - anim.getX(partialTicks)) : (screenWidth - totalWidth - 2 + anim.getX(partialTicks));
                float animY = anim.getY(partialTicks);

                int color = (ClientSkin.isEasterEggActive() && exitingModule != null)
                    ? ClientSkin.getModuleColor(exitingModule.getCategory(), (int) animY, screenHeight)
                    : ArrayListConfig.getColorForY((int) animY, screenHeight);

                fr.drawStringWithShadow(exitingName, animX, animY, color);
                if (suffix != null && !suffix.isEmpty()) {
                    int suffixColor = ClientSkin.isEasterEggActive() ? ClientSkin.getSuffixColor() : 0x7F7F7F;
                    fr.drawStringWithShadow(suffix, animX + fr.getStringWidth(exitingName), animY, suffixColor);
                }
            }
        }*/

        // Render notifications
        if (ArrayListConfig.notificationsEnabled) {
            ModuleNotification.render(mc, screenWidth, screenHeight);
        }

        // Render build watermark
        if (ArrayListConfig.buildWatermarkEnabled) {
            try {
                String buildText = BuildInfo.getInstance().getFormattedString();
                int buildWidth = fr.getStringWidth(buildText);
                fr.drawStringWithShadow(buildText, screenWidth - buildWidth - 5, screenHeight - 12, ArrayListConfig.buildWatermarkColor);
            } catch (Exception ignored) {}
        }

        // Render current account display at bottom-left (only in main menu and multiplayer menu)
        if (ArrayListConfig.arrayListEnabled) {
            try {
                boolean inMainMenu = mc.currentScreen instanceof GuiMainMenu;
                boolean inMultiplayer = mc.currentScreen instanceof GuiMultiplayer;

                if (inMainMenu || inMultiplayer) {
                    String currentUsername = mc.getSession() != null ? mc.getSession().getUsername() : "Unknown";
                    String accountText = "Account: §b" + currentUsername;
                    fr.drawStringWithShadow(accountText, 5, screenHeight - 12, 0xAAAAAA);
                }
            } catch (Exception ignored) {}
        }

        // Render decorative line at screen edge (animated RGB, follows ArrayList position)
        if (ArrayListConfig.decorativeLineEnabled) {
            int lineWidth = ArrayListConfig.decorativeLineWidth;
            // Line position follows ArrayList - if ArrayList is on left, line is on left edge; otherwise right edge
            boolean onLeft = ClientSkin.isArrayListOnLeft();

            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            int segments = 20;
            float segmentHeight = (float) screenHeight / segments;

            GL11.glBegin(GL11.GL_QUADS);
            for (int i = 0; i < segments; i++) {
                float y1 = i * segmentHeight;
                float y2 = (i + 1) * segmentHeight;

                // Get color from same function as ArrayList (synchronized)
                int color1 = ArrayListConfig.getColorForY((int) y1, screenHeight);
                int color2 = ArrayListConfig.getColorForY((int) y2, screenHeight);

                float r1 = ((color1 >> 16) & 0xFF) / 255.0f;
                float g1 = ((color1 >> 8) & 0xFF) / 255.0f;
                float b1 = (color1 & 0xFF) / 255.0f;

                float r2 = ((color2 >> 16) & 0xFF) / 255.0f;
                float g2 = ((color2 >> 8) & 0xFF) / 255.0f;
                float b2 = (color2 & 0xFF) / 255.0f;

                float x1, x2;
                if (onLeft) {
                    x1 = 0;
                    x2 = lineWidth;
                } else {
                    x1 = screenWidth - lineWidth;
                    x2 = screenWidth;
                }

                GL11.glColor4f(r1, g1, b1, 1.0f);
                GL11.glVertex2f(x1, y1);
                GL11.glVertex2f(x2, y1);
                GL11.glColor4f(r2, g2, b2, 1.0f);
                GL11.glVertex2f(x2, y2);
                GL11.glVertex2f(x1, y2);
            }
            GL11.glEnd();

            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static String getSuffixPlain(Module m) {
        String s = m.getDisplaySuffix();
        return s != null ? s.replaceAll("\u00a7.", "") : "";
    }

    // Performance: Cache text widths to avoid recalculating every frame
    private int getCachedWidth(FontRenderer fr, Module m) {
        String key = m.getName();
        Integer cached = cachedTextWidths.get(key);
        if (cached != null) return cached;

        int width = fr.getStringWidth(m.getName() + getSuffixPlain(m));
        cachedTextWidths.put(key, width);
        return width;
    }

    private enum AnimationState {
        HIDDEN,
        SLIDING_IN,
        VISIBLE,
        SLIDING_OUT
    }

    private static class ModuleAnimation {
        AnimationState currentState = AnimationState.HIDDEN;
        AnimationState targetState = AnimationState.HIDDEN;
        float progress = 0.0f;
        long animationStartTime = 0;
    }

    private static class ModuleRenderData {
        String name;
        String suffix;
        float progress;
        ModuleCategory category;

        ModuleRenderData(String name, String suffix, float progress, ModuleCategory category) {
            this.name = name;
            this.suffix = suffix;
            this.category = category;
            this.progress = progress;
        }
    }

    private static class ModuleRenderInfo {
        String name;
        String suffix;
        int xPos;
        int yPos;
        int alpha;
        int color;
        int fullWidth;
    }

    /**
     * Draw a glow rectangle (for bloom effect)
     */
    private void drawGlowRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
    }

    private int getSmoothRainbowColor() {
        long elapsed = System.currentTimeMillis() - startTime;
        float hue = (elapsed % 3000) / 3000.0f; // 3 second cycle

        // Convert HSB to RGB
        int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);

        // Remove alpha channel and keep only RGB
        return rgb & 0xFFFFFF;
    }

    /**
     * Draw a colored rectangle (for Exodar Theme)
     */
    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int temp = left;
            left = right;
            right = temp;
        }
        if (top < bottom) {
            int temp = top;
            top = bottom;
            bottom = temp;
        }

        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }
}
