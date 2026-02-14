/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.shader;

import org.lwjgl.opengl.GL20;

import java.awt.Color;
import java.nio.FloatBuffer;

/**
 * Utility class for setting shader uniform variables
 */
public class ShaderUniforms {

    /**
     * Set a float uniform
     */
    public static void uniform1f(int programId, String name, float value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform1f(location, value);
        }
    }

    /**
     * Set a vec2 uniform
     */
    public static void uniform2f(int programId, String name, float x, float y) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform2f(location, x, y);
        }
    }

    /**
     * Set a vec3 uniform
     */
    public static void uniform3f(int programId, String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    /**
     * Set a vec4 uniform
     */
    public static void uniform4f(int programId, String name, float x, float y, float z, float w) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform4f(location, x, y, z, w);
        }
    }

    /**
     * Set a vec4 uniform from a Color
     */
    public static void uniformColor(int programId, String name, Color color) {
        uniform4f(programId, name,
            color.getRed() / 255.0f,
            color.getGreen() / 255.0f,
            color.getBlue() / 255.0f,
            color.getAlpha() / 255.0f
        );
    }

    /**
     * Set a vec4 uniform from an ARGB int
     */
    public static void uniformColor(int programId, String name, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        uniform4f(programId, name, r, g, b, a);
    }

    /**
     * Set an int uniform
     */
    public static void uniform1i(int programId, String name, int value) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform1i(location, value);
        }
    }

    /**
     * Set a float array uniform (for kernel weights)
     */
    public static void uniformFB(int programId, String name, FloatBuffer buffer) {
        int location = GL20.glGetUniformLocation(programId, name);
        if (location != -1) {
            GL20.glUniform1(location, buffer);
        }
    }
}
