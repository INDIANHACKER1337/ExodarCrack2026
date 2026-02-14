/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * BloomShader - Rise-style bloom effect using framebuffers and Gaussian blur
 * Identical implementation to Rise client
 */
public class BloomShader {

    private static BloomShader instance;
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ShaderProgram bloomProgram;
    private Framebuffer inputFramebuffer;
    private Framebuffer outputFramebuffer;
    private GaussianKernel gaussianKernel;

    private final List<Runnable> bloomQueue = new ArrayList<>();
    private boolean initialized = false;

    private BloomShader() {
        this.bloomProgram = new ShaderProgram("bloom.frag", "vertex.vsh");
        this.gaussianKernel = new GaussianKernel(0);
    }

    public static BloomShader getInstance() {
        if (instance == null) {
            instance = new BloomShader();
        }
        return instance;
    }

    /**
     * Check if bloom shader is available
     */
    public boolean isAvailable() {
        return bloomProgram.isValid();
    }

    /**
     * Initialize framebuffers (call once OpenGL context is ready)
     */
    public void init() {
        if (initialized) return;
        try {
            inputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            outputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            initialized = true;
        } catch (Exception e) {
            System.out.println("[Exodar BloomShader] Failed to init framebuffers: " + e.getMessage());
        }
    }

    /**
     * Add a render task to the bloom queue
     */
    public void addToQueue(Runnable renderTask) {
        bloomQueue.add(renderTask);
    }

    /**
     * Clear the bloom queue
     */
    public void clearQueue() {
        bloomQueue.clear();
    }

    /**
     * Update framebuffer sizes if display size changed
     */
    public void update() {
        if (!initialized) {
            init();
            return;
        }

        int width = mc.displayWidth;
        int height = mc.displayHeight;

        try {
            if (inputFramebuffer.framebufferWidth != width || inputFramebuffer.framebufferHeight != height) {
                inputFramebuffer.deleteFramebuffer();
                inputFramebuffer = new Framebuffer(width, height, true);
            } else {
                inputFramebuffer.framebufferClear();
            }

            if (outputFramebuffer.framebufferWidth != width || outputFramebuffer.framebufferHeight != height) {
                outputFramebuffer.deleteFramebuffer();
                outputFramebuffer = new Framebuffer(width, height, true);
            } else {
                outputFramebuffer.framebufferClear();
            }

            inputFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            outputFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        } catch (Exception e) {
            // Ignore framebuffer errors
        }
    }

    /**
     * Render all queued items with bloom effect
     */
    public void render() {
        if (!Display.isVisible() || !bloomProgram.isValid() || !initialized) {
            bloomQueue.clear();
            return;
        }

        if (bloomQueue.isEmpty()) {
            return;
        }

        // Clear any existing GL errors
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {}

        try {
            // Clear and render to input framebuffer
            inputFramebuffer.framebufferClear();
            inputFramebuffer.bindFramebuffer(true);

            for (Runnable task : bloomQueue) {
                task.run();
            }
            bloomQueue.clear();

            // Apply bloom effect
            final int radius = 16;
            final float compression = 2.5F;
            final int programId = bloomProgram.getProgramId();

            outputFramebuffer.framebufferClear();
            outputFramebuffer.bindFramebuffer(true);
            bloomProgram.start();

            // Setup kernel if needed
            if (gaussianKernel.getSize() != radius) {
                gaussianKernel = new GaussianKernel(radius);
                gaussianKernel.compute();

                FloatBuffer buffer = BufferUtils.createFloatBuffer(radius);
                buffer.put(gaussianKernel.getKernel());
                buffer.flip();

                ShaderUniforms.uniform1f(programId, "u_radius", radius);
                ShaderUniforms.uniformFB(programId, "u_kernel", buffer);
                ShaderUniforms.uniform1i(programId, "u_diffuse_sampler", 0);
                ShaderUniforms.uniform1i(programId, "u_other_sampler", 16);
            }

            ShaderUniforms.uniform2f(programId, "u_texel_size", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
            ShaderUniforms.uniform2f(programId, "u_direction", compression, 0.0F);

            // First pass - horizontal blur
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            inputFramebuffer.bindFramebufferTexture();
            drawQuad();

            // Second pass - vertical blur and composite to main framebuffer
            mc.getFramebuffer().bindFramebuffer(true);
            ShaderUniforms.uniform2f(programId, "u_direction", 0.0F, compression);

            // Use standard alpha blending for shadow compatibility
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            outputFramebuffer.bindFramebufferTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE16);
            inputFramebuffer.bindFramebufferTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            drawQuad();

            ShaderProgram.stop();

        } catch (Exception e) {
            System.out.println("[Exodar BloomShader] Render error: " + e.getMessage());
            bloomQueue.clear();
            ShaderProgram.stop();
        }

        // Always restore state
        mc.getFramebuffer().bindFramebuffer(true);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        // Clear any GL errors we generated
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
    }

    /**
     * Draw fullscreen quad for post-processing
     */
    private void drawQuad() {
        ScaledResolution sr = new ScaledResolution(mc);
        double width = sr.getScaledWidth_double();
        double height = sr.getScaledHeight_double();

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2d(0, height);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2d(width, height);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2d(width, 0);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2d(0, 0);
        GL11.glEnd();
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (inputFramebuffer != null) {
            inputFramebuffer.deleteFramebuffer();
        }
        if (outputFramebuffer != null) {
            outputFramebuffer.deleteFramebuffer();
        }
        initialized = false;
    }
}
