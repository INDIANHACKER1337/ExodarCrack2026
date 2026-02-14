/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.utils;

import net.minecraft.client.gui.FontRenderer;
import java.lang.reflect.Method;

/**
 * Helper class to handle FontRenderer method calls that have different signatures
 * between compile-time (vanilla MC) and runtime (Lunar Client)
 */
public class FontRendererHelper {
    private static Method drawStringWithShadowMethod = null;
    private static boolean methodInitialized = false;

    /**
     * Calls FontRenderer.drawStringWithShadow using the correct signature for Lunar Client
     * Lunar Client expects (String, float, float, int) but compile-time has (String, int, int, int)
     */
    public static int drawStringWithShadow(FontRenderer fr, String text, int x, int y, int color) {
        if (fr == null) return 0;

        // Try to find and cache the method on first call
        if (!methodInitialized) {
            methodInitialized = true;
            try {
                // Try to find the method with float parameters (Lunar Client)
                drawStringWithShadowMethod = FontRenderer.class.getMethod(
                    "drawStringWithShadow", String.class, float.class, float.class, int.class);
            } catch (NoSuchMethodException e) {
                // Method with float params not found, will fall back to direct call
                drawStringWithShadowMethod = null;
            }
        }

        // If we found the float-based method, use reflection
        if (drawStringWithShadowMethod != null) {
            try {
                return (int) drawStringWithShadowMethod.invoke(fr, text, (float)x, (float)y, color);
            } catch (Exception e) {
                System.out.println("[FontRendererHelper] Error calling drawStringWithShadow via reflection: " + e.getMessage());
                // Fall through to direct call
            }
        }

        // Fallback: try direct call (will work if signatures match)
        try {
            return fr.drawStringWithShadow(text, x, y, color);
        } catch (NoSuchMethodError e) {
            System.out.println("[FontRendererHelper] NoSuchMethodError - method signature mismatch");
            return 0;
        }
    }

    /**
     * Overload that accepts float coordinates directly for smooth rendering
     * Uses float precision when Lunar Client method is available
     */
    public static int drawStringWithShadow(FontRenderer fr, String text, float x, float y, int color) {
        if (fr == null) return 0;

        // Try to find and cache the method on first call
        if (!methodInitialized) {
            methodInitialized = true;
            try {
                // Try to find the method with float parameters (Lunar Client)
                drawStringWithShadowMethod = FontRenderer.class.getMethod(
                    "drawStringWithShadow", String.class, float.class, float.class, int.class);
            } catch (NoSuchMethodException e) {
                // Method with float params not found, will fall back to direct call
                drawStringWithShadowMethod = null;
            }
        }

        // If we found the float-based method, use reflection with actual float values
        if (drawStringWithShadowMethod != null) {
            try {
                return (int) drawStringWithShadowMethod.invoke(fr, text, x, y, color);
            } catch (Exception e) {
                // Fall through to direct call
            }
        }

        // Fallback: cast to int (loses precision but works)
        try {
            return fr.drawStringWithShadow(text, (int) x, (int) y, color);
        } catch (NoSuchMethodError e) {
            return 0;
        }
    }
}
