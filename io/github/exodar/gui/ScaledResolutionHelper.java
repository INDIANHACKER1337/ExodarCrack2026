/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.lang.reflect.Constructor;

/**
 * Helper to create ScaledResolution instances using reflection
 * Fixes compile-time vs runtime constructor mismatch in Lunar Client
 */
public class ScaledResolutionHelper {
    private static Constructor<?> scaledResolutionConstructor;
    private static boolean initialized = false;

    static {
        try {
            // Try to find the single-parameter constructor (runtime)
            try {
                scaledResolutionConstructor = ScaledResolution.class.getConstructor(Minecraft.class);
            } catch (NoSuchMethodException e) {
                // Try 3-parameter constructor (compile-time fallback)
                try {
                    scaledResolutionConstructor = ScaledResolution.class.getConstructor(Minecraft.class, int.class, int.class);
                } catch (NoSuchMethodException e2) {
                    // No compatible constructor found
                }
            }
            initialized = true;
        } catch (Exception e) {
            // Silent
        }
    }

    /**
     * Create a ScaledResolution instance using the correct constructor for the runtime environment
     */
    public static ScaledResolution create(Minecraft mc) {
        if (!initialized || scaledResolutionConstructor == null) {
            return null;
        }

        try {
            int paramCount = scaledResolutionConstructor.getParameterCount();

            if (paramCount == 1) {
                return (ScaledResolution) scaledResolutionConstructor.newInstance(mc);
            } else if (paramCount == 3) {
                return (ScaledResolution) scaledResolutionConstructor.newInstance(mc, mc.displayWidth, mc.displayHeight);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
