/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui.modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/**
 * Blur effect for the modern ClickGUI
 * Uses a simple box blur approximation when shaders aren't available
 */
public class BlurShader {

    private static boolean initialized = false;
    private static int blurProgram = 0;
    private static int blurDirectionLoc = 0;
    private static int blurRadiusLoc = 0;
    private static int textureSizeLoc = 0;

    private static Framebuffer blurFramebuffer1 = null;
    private static Framebuffer blurFramebuffer2 = null;

    // Vertex shader
    private static final String VERTEX_SHADER =
        "#version 120\n" +
        "void main() {\n" +
        "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
        "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
        "}\n";

    // Fragment shader - Gaussian blur
    private static final String FRAGMENT_SHADER =
        "#version 120\n" +
        "uniform sampler2D texture;\n" +
        "uniform vec2 direction;\n" +
        "uniform float radius;\n" +
        "uniform vec2 textureSize;\n" +
        "\n" +
        "void main() {\n" +
        "    vec2 texCoord = gl_TexCoord[0].st;\n" +
        "    vec4 color = vec4(0.0);\n" +
        "    float total = 0.0;\n" +
        "    \n" +
        "    for (float i = -radius; i <= radius; i++) {\n" +
        "        float weight = exp(-(i * i) / (2.0 * radius * radius));\n" +
        "        vec2 offset = direction * i / textureSize;\n" +
        "        color += texture2D(texture, texCoord + offset) * weight;\n" +
        "        total += weight;\n" +
        "    }\n" +
        "    \n" +
        "    gl_FragColor = color / total;\n" +
        "}\n";

    /**
     * Initialize the blur shader
     */
    public static void init() {
        if (initialized) return;

        try {
            if (!OpenGlHelper.shadersSupported) {
                System.out.println("[BlurShader] Shaders not supported");
                return;
            }

            // Create vertex shader
            int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vertexShader, VERTEX_SHADER);
            GL20.glCompileShader(vertexShader);

            if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                System.out.println("[BlurShader] Vertex shader error: " + GL20.glGetShaderInfoLog(vertexShader, 1024));
                return;
            }

            // Create fragment shader
            int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(fragmentShader, FRAGMENT_SHADER);
            GL20.glCompileShader(fragmentShader);

            if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
                System.out.println("[BlurShader] Fragment shader error: " + GL20.glGetShaderInfoLog(fragmentShader, 1024));
                return;
            }

            // Create program
            blurProgram = GL20.glCreateProgram();
            GL20.glAttachShader(blurProgram, vertexShader);
            GL20.glAttachShader(blurProgram, fragmentShader);
            GL20.glLinkProgram(blurProgram);

            if (GL20.glGetProgrami(blurProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                System.out.println("[BlurShader] Program link error: " + GL20.glGetProgramInfoLog(blurProgram, 1024));
                return;
            }

            // Get uniform locations
            blurDirectionLoc = GL20.glGetUniformLocation(blurProgram, "direction");
            blurRadiusLoc = GL20.glGetUniformLocation(blurProgram, "radius");
            textureSizeLoc = GL20.glGetUniformLocation(blurProgram, "textureSize");

            // Cleanup shaders (attached to program now)
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);

            initialized = true;
            System.out.println("[BlurShader] Initialized successfully");

        } catch (Exception e) {
            System.out.println("[BlurShader] Init error: " + e.getMessage());
        }
    }

    /**
     * Apply blur effect to a region of the screen
     */
    public static void blur(int x, int y, int width, int height, float radius) {
        if (!initialized || blurProgram == 0) {
            // Fallback: just draw a semi-transparent overlay
            drawFallbackBlur(x, y, width, height);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        try {
            // Setup framebuffers if needed
            if (blurFramebuffer1 == null || blurFramebuffer1.framebufferWidth != mc.displayWidth) {
                if (blurFramebuffer1 != null) blurFramebuffer1.deleteFramebuffer();
                if (blurFramebuffer2 != null) blurFramebuffer2.deleteFramebuffer();
                blurFramebuffer1 = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
                blurFramebuffer2 = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            }

            // Copy screen to framebuffer 1
            blurFramebuffer1.framebufferClear();
            blurFramebuffer1.bindFramebuffer(false);
            GlStateManager.bindTexture(mc.getFramebuffer().framebufferTexture);

            drawTexturedQuad(0, 0, mc.displayWidth, mc.displayHeight, 0, 1, 1, 0);

            // Horizontal blur pass
            blurFramebuffer2.framebufferClear();
            blurFramebuffer2.bindFramebuffer(false);

            GL20.glUseProgram(blurProgram);
            GL20.glUniform2f(blurDirectionLoc, 1.0f, 0.0f);
            GL20.glUniform1f(blurRadiusLoc, radius);
            GL20.glUniform2f(textureSizeLoc, mc.displayWidth, mc.displayHeight);

            GlStateManager.bindTexture(blurFramebuffer1.framebufferTexture);
            drawTexturedQuad(0, 0, mc.displayWidth, mc.displayHeight, 0, 1, 1, 0);

            // Vertical blur pass
            blurFramebuffer1.framebufferClear();
            blurFramebuffer1.bindFramebuffer(false);

            GL20.glUniform2f(blurDirectionLoc, 0.0f, 1.0f);

            GlStateManager.bindTexture(blurFramebuffer2.framebufferTexture);
            drawTexturedQuad(0, 0, mc.displayWidth, mc.displayHeight, 0, 1, 1, 0);

            GL20.glUseProgram(0);

            // Draw blurred result to screen
            mc.getFramebuffer().bindFramebuffer(false);

            ScaledResolution sr = new ScaledResolution(mc);
            float scaleX = (float)mc.displayWidth / sr.getScaledWidth();
            float scaleY = (float)mc.displayHeight / sr.getScaledHeight();

            // Enable scissor for the blur region
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int)(x * scaleX), mc.displayHeight - (int)((y + height) * scaleY),
                          (int)(width * scaleX), (int)(height * scaleY));

            GlStateManager.bindTexture(blurFramebuffer1.framebufferTexture);
            drawTexturedQuad(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0, 1, 1, 0);

            GL11.glDisable(GL11.GL_SCISSOR_TEST);

        } catch (Exception e) {
            // Fallback on error
            mc.getFramebuffer().bindFramebuffer(false);
            drawFallbackBlur(x, y, width, height);
        }
    }

    /**
     * Fallback blur - just a semi-transparent overlay
     */
    private static void drawFallbackBlur(int x, int y, int width, int height) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        // Draw multiple semi-transparent layers for a fake blur effect
        for (int i = 0; i < 3; i++) {
            int alpha = 60 - i * 15;
            GlStateManager.color(0.04f, 0.04f, 0.06f, alpha / 255f);

            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer wr = tessellator.getWorldRenderer();
            wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            wr.pos(x - i, y - i, 0).endVertex();
            wr.pos(x - i, y + height + i, 0).endVertex();
            wr.pos(x + width + i, y + height + i, 0).endVertex();
            wr.pos(x + width + i, y - i, 0).endVertex();
            tessellator.draw();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Draw a textured quad
     */
    private static void drawTexturedQuad(float x, float y, float width, float height,
                                         float u1, float v1, float u2, float v2) {
        GlStateManager.enableTexture2D();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + height, 0).tex(u1, v1).endVertex();
        wr.pos(x + width, y + height, 0).tex(u2, v1).endVertex();
        wr.pos(x + width, y, 0).tex(u2, v2).endVertex();
        wr.pos(x, y, 0).tex(u1, v2).endVertex();
        tessellator.draw();
    }

    /**
     * Cleanup resources
     */
    public static void cleanup() {
        if (blurProgram != 0) {
            GL20.glDeleteProgram(blurProgram);
            blurProgram = 0;
        }
        if (blurFramebuffer1 != null) {
            blurFramebuffer1.deleteFramebuffer();
            blurFramebuffer1 = null;
        }
        if (blurFramebuffer2 != null) {
            blurFramebuffer2.deleteFramebuffer();
            blurFramebuffer2 = null;
        }
        initialized = false;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
