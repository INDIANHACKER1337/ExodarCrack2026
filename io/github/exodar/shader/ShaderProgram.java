/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Wrapper class for OpenGL shader programs
 */
public class ShaderProgram {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private final int programId;
    private boolean valid;

    public ShaderProgram(String fragmentPath, String vertexPath) {
        this.programId = ShaderUtil.createShader(fragmentPath, vertexPath);
        this.valid = programId != -1;
    }

    /**
     * Start using this shader program
     */
    public void start() {
        if (valid) {
            GL20.glUseProgram(programId);
        }
    }

    /**
     * Stop using any shader program
     */
    public static void stop() {
        GL20.glUseProgram(0);
    }

    /**
     * Get the program ID for setting uniforms
     */
    public int getProgramId() {
        return programId;
    }

    /**
     * Check if shader is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Draw a quad at specified position with texture coordinates
     */
    public static void drawQuad(float x, float y, float width, float height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(x + width, y);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
    }

    /**
     * Draw a fullscreen quad
     */
    public static void drawQuad() {
        ScaledResolution sr = new ScaledResolution(mc);
        drawQuad(0, 0, (float) sr.getScaledWidth_double(), (float) sr.getScaledHeight_double());
    }
}
