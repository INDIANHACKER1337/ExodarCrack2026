/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.shader;

import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;

/**
 * Shader for drawing rounded rectangles
 * Used by Exodar Theme for Rise-style ArrayList
 */
public class RoundedQuadShader {

    private static RoundedQuadShader instance;
    private final ShaderProgram program;

    private RoundedQuadShader() {
        this.program = new ShaderProgram("roundedquad.frag", "vertex.vsh");
    }

    /**
     * Get singleton instance
     */
    public static RoundedQuadShader getInstance() {
        if (instance == null) {
            instance = new RoundedQuadShader();
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
     * Draw a rounded rectangle
     *
     * @param x      X position
     * @param y      Y position
     * @param width  Width
     * @param height Height
     * @param radius Corner radius
     * @param color  Color (with alpha)
     */
    public void draw(float x, float y, float width, float height, float radius, Color color) {
        if (!program.isValid()) return;

        int programId = program.getProgramId();
        program.start();

        ShaderUniforms.uniform2f(programId, "u_size", width, height);
        ShaderUniforms.uniform1f(programId, "u_radius", radius);
        ShaderUniforms.uniformColor(programId, "u_color", color);

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        ShaderProgram.drawQuad(x, y, width, height);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        ShaderProgram.stop();
    }

    /**
     * Draw a rounded rectangle with int color
     */
    public void draw(float x, float y, float width, float height, float radius, int argbColor) {
        int a = (argbColor >> 24) & 0xFF;
        int r = (argbColor >> 16) & 0xFF;
        int g = (argbColor >> 8) & 0xFF;
        int b = argbColor & 0xFF;
        draw(x, y, width, height, radius, new Color(r, g, b, a));
    }

    /**
     * Draw a rounded rectangle with separate color and alpha
     */
    public void draw(float x, float y, float width, float height, float radius, int rgbColor, int alpha) {
        int r = (rgbColor >> 16) & 0xFF;
        int g = (rgbColor >> 8) & 0xFF;
        int b = rgbColor & 0xFF;
        draw(x, y, width, height, radius, new Color(r, g, b, alpha));
    }
}
