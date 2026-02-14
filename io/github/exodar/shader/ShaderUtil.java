/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class for loading and compiling OpenGL shaders
 */
public class ShaderUtil {

    /**
     * Create a shader program from fragment and vertex shader files
     */
    public static int createShader(String fragmentResource, String vertexResource) {
        System.out.println("[Exodar] Loading shaders: " + fragmentResource + ", " + vertexResource);

        String fragmentSource = getShaderResource(fragmentResource);
        String vertexSource = getShaderResource(vertexResource);

        if (fragmentSource == null || vertexSource == null) {
            System.out.println("[Exodar] Error loading shader resources - source is null");
            return -1;
        }

        System.out.println("[Exodar] Shader sources loaded, compiling...");

        int fragmentId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        int vertexId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);

        GL20.glShaderSource(fragmentId, fragmentSource);
        GL20.glShaderSource(vertexId, vertexSource);
        GL20.glCompileShader(fragmentId);
        GL20.glCompileShader(vertexId);

        if (!checkCompileStatus(fragmentId, fragmentResource)) {
            GL20.glDeleteShader(fragmentId);
            GL20.glDeleteShader(vertexId);
            return -1;
        }
        if (!checkCompileStatus(vertexId, vertexResource)) {
            GL20.glDeleteShader(fragmentId);
            GL20.glDeleteShader(vertexId);
            return -1;
        }

        int programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, fragmentId);
        GL20.glAttachShader(programId, vertexId);
        GL20.glLinkProgram(programId);

        // Check link status
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            String log = GL20.glGetProgramInfoLog(programId, 8192);
            System.out.println("[Exodar] Error linking shader program:");
            System.out.println(log);
            GL20.glDeleteShader(fragmentId);
            GL20.glDeleteShader(vertexId);
            GL20.glDeleteProgram(programId);
            return -1;
        }

        GL20.glValidateProgram(programId);

        // Clean up individual shaders after linking
        GL20.glDeleteShader(fragmentId);
        GL20.glDeleteShader(vertexId);

        System.out.println("[Exodar] Shader program created successfully: " + programId);

        return programId;
    }

    /**
     * Check if shader compiled successfully
     */
    private static boolean checkCompileStatus(int shaderId, String name) {
        boolean compiled = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE;
        if (!compiled) {
            String log = GL20.glGetShaderInfoLog(shaderId, 8192);
            System.out.println("[Exodar] Error compiling shader '" + name + "':");
            System.out.println(log);
            return false;
        }
        System.out.println("[Exodar] Shader compiled: " + name);
        return true;
    }

    /**
     * Load shader source from ClassLoader resources (works without pack.mcmeta)
     */
    public static String getShaderResource(String resource) {
        // Try multiple paths to find the shader
        String[] paths = {
            "/assets/exodar/shader/" + resource,
            "assets/exodar/shader/" + resource,
            "/shader/" + resource,
            "shader/" + resource
        };

        for (String path : paths) {
            try {
                InputStream inputStream = ShaderUtil.class.getResourceAsStream(path);
                if (inputStream == null) {
                    inputStream = ShaderUtil.class.getClassLoader().getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
                }

                if (inputStream != null) {
                    System.out.println("[Exodar] Found shader at: " + path);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder source = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        source.append(line).append("\n");
                    }

                    reader.close();
                    inputStream.close();
                    return source.toString();
                }
            } catch (Exception e) {
                // Try next path
            }
        }

        System.out.println("[Exodar] Could not find shader: " + resource);
        System.out.println("[Exodar] Tried paths: " + String.join(", ", paths));
        return null;
    }
}
