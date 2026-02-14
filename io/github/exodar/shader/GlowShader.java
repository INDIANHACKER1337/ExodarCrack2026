/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.shader;

import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;

/**
 * Shader for drawing rectangles with glow effect
 * Used by Exodar Theme for Rise-style glow
 */
public class GlowShader {

    private static GlowShader instance;
    private final ShaderProgram program;

    private GlowShader() {
        this.program = new ShaderProgram("glow.frag", "vertex.vsh");
    }

    /**
     * Get singleton instance
     */
    public static GlowShader getInstance() {
        if (instance == null) {
            instance = new GlowShader();
        }
        return instance;
    }

    /**
     * Check if shader is available
     */
    public boolean isAvailable() {
        return program.isValid();
    }

    /**
     * Draw a rectangle with glow effect
     *
     * @param x        X position
     * @param y        Y position
     * @param width    Width
     * @param height   Height
     * @param radius   Corner radius
     * @param glowSize Size of the glow effect
     * @param color    Color (with alpha)
     */
    public void draw(float x, float y, float width, float height, float radius, float glowSize, Color color) {
        if (!program.isValid()) return;

        int programId = program.getProgramId();
        program.start();

        ShaderUniforms.uniform2f(programId, "u_size", width, height);
        ShaderUniforms.uniform1f(programId, "u_radius", radius);
        ShaderUniforms.uniform1f(programId, "u_glow_size", glowSize);
        ShaderUniforms.uniformColor(programId, "u_color", color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Draw slightly larger quad to accommodate glow
        float padding = glowSize;
        ShaderProgram.drawQuad(x - padding, y - padding, width + padding * 2, height + padding * 2);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        ShaderProgram.stop();
    }

    /**
     * Draw with int color
     */
    public void draw(float x, float y, float width, float height, float radius, float glowSize, int rgbColor, int alpha) {
        int r = (rgbColor >> 16) & 0xFF;
        int g = (rgbColor >> 8) & 0xFF;
        int b = rgbColor & 0xFF;
        draw(x, y, width, height, radius, glowSize, new Color(r, g, b, alpha));
    }
}
