/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import java.awt.Color;

/**
 * Configuración del ArrayList
 */
public class ArrayListConfig {
    public static ArrayListColorMode colorMode = ArrayListColorMode.RAINBOW;
    public static Color customColor = Color.RED; // Para custom pulse mode
    public static boolean slideAnimation = true;
    public static float animationSpeed = 10.0f; // pixels per frame

    // Enable/Disable toggles
    public static boolean arrayListEnabled = true;
    public static boolean notificationsEnabled = true;
    public static boolean watermarkEnabled = true;

    // Watermark config
    public static String watermarkText = "Exodar"; // Configurable client name
    public static String clientName = "Exodar"; // User-editable client name
    public static int watermarkFirstLetterColor = 0x00FFFF; // Cyan/aqua (§b)
    public static boolean watermarkRainbow = true; // Rainbow first letter

    // Build Watermark config (bottom-right corner)
    public static boolean buildWatermarkEnabled = true;
    public static int buildWatermarkColor = 0x888888; // Gray color for build info

    // Menu settings
    public static boolean invertMenuClicks = false; // If true, right click toggles modules instead of left click
    public static int menuKeybind = 210; // Default INSERT key (Keyboard.KEY_INSERT = 210)

    // Exodar Theme (Rise-style ArrayList)
    public static boolean exodarThemeEnabled = false;
    public static boolean exodarBackground = true;     // Semi-transparent background
    public static boolean exodarSidebar = true;        // Colored sidebar on right
    public static boolean exodarGlow = true;           // Glow effect
    public static int exodarBackgroundAlpha = 100;     // Background transparency (0-255)

    // Custom Font option
    public static boolean exodarCustomFont = false;    // Use Verdana custom font instead of Minecraft font (disabled by default - can cause issues)
    public static int exodarFontSize = 18;             // Custom font size

    // Glow settings
    public static int exodarGlowSize = 8;              // Glow radius (pixels)
    public static int exodarGlowPasses = 6;            // Number of glow layers (more = smoother)
    public static float exodarGlowIntensity = 0.8f;    // Glow brightness (0-1)
    public static boolean exodarGlowShadow = false;    // Use black shadow instead of colored glow

    // Decorative line at screen edge (animated RGB colors)
    public static boolean decorativeLineEnabled = true;  // Enabled by default
    public static int decorativeLineWidth = 2;           // Line width in pixels

    /**
     * Get color based on Y position (synchronized rainbow for all modules at same height)
     * This creates a vertical gradient: top = blue, bottom = red
     */
    public static int getColorForY(int yPos, int screenHeight) {
        long time = System.currentTimeMillis();

        // Calculate offset based on Y position (0.0 at top, 1.0 at bottom)
        // Divide by larger number for smoother gradient across screen
        float offset = (float) yPos / 300.0f;

        switch (colorMode) {
            case RAINBOW:
                return getRainbowColor(time, offset, 1.0f);

            case RAINBOW_PASTEL:
                return getRainbowColor(time, offset, 0.5f);

            case TRANSGENDER:
                return getTransgenderColor(time, offset);

            case COLOR_FADE:
                return getColorFade(time, offset);

            case PURPLE:
                return getPurpleColor(time, offset);

            case TOXIC:
                return getToxicColor(time, offset);

            case PINK:
                return getPinkColor(time, offset);

            case WHITE:
            default:
                return 0xFFFFFF;
        }
    }

    /**
     * Rainbow color - transición MUY suave
     */
    private static int getRainbowColor(long time, float offset, float saturation) {
        // Rotación lenta (4 segundos por ciclo completo)
        float hue = ((time % 4000) / 4000.0f + offset) % 1.0f;
        return Color.HSBtoRGB(hue, saturation, 1.0f);
    }

    /**
     * Transgender pride colors (animated with smooth fade)
     */
    private static int getTransgenderColor(long time, float offset) {
        // Transgender flag: Light Blue, Pink, White, Pink, Light Blue
        Color[] colors = {
            new Color(86, 208, 254),   // Light Blue (más saturado)
            new Color(254, 159, 178),  // Pink (más saturado)
            Color.WHITE,
            new Color(254, 159, 178),  // Pink (más saturado)
            new Color(86, 208, 254)    // Light Blue (más saturado)
        };

        // Transición más rápida (2 segundos en vez de 5)
        float position = ((time % 2000) / 2000.0f + offset) % 1.0f;
        float scaledPos = position * (colors.length - 1);
        int index = (int) scaledPos;
        int nextIndex = (index + 1) % colors.length;
        index = Math.min(index, colors.length - 1);
        nextIndex = Math.min(nextIndex, colors.length - 1);

        // Smooth interpolation between colors
        float blend = scaledPos - index;
        Color c1 = colors[index];
        Color c2 = colors[nextIndex];

        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * blend);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * blend);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * blend);

        return new Color(r, g, b).getRGB();
    }

    /**
     * Color Fade - fade entre colores específicos
     */
    private static int getColorFade(long time, float offset) {
        // Fade entre blue y morado
        Color[] colors = {
            new Color(0, 217, 255),   // Light Blue
            new Color(144, 0, 255),   // Purple
            new Color(255, 0, 204)    // Pink
        };

        float position = ((time % 3000) / 3000.0f + offset) % 1.0f;
        int index = (int) (position * colors.length);
        int nextIndex = (index + 1) % colors.length;
        index = Math.min(index, colors.length - 1);

        // Interpolate between colors
        float blend = (position * colors.length) % 1.0f;
        Color c1 = colors[index];
        Color c2 = colors[nextIndex];

        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * blend);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * blend);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * blend);

        return new Color(r, g, b).getRGB();
    }

    /**
     * Purple con pulse
     */
    private static int getPurpleColor(long time, float offset) {
        // Light purple gradient (like menu accent colors)
        Color lightPurple = new Color(180, 130, 255);   // Light lavender purple
        Color mediumPurple = new Color(155, 100, 235);  // Medium purple (menu color)
        Color softPurple = new Color(200, 160, 255);    // Soft lavender

        Color[] colors = { lightPurple, mediumPurple, softPurple, mediumPurple };

        // Smooth animated gradient (4 second cycle)
        float position = ((time % 4000) / 4000.0f + offset) % 1.0f;
        float scaledPos = position * (colors.length - 1);
        int index = (int) scaledPos;
        int nextIndex = (index + 1) % colors.length;
        index = Math.min(index, colors.length - 1);

        float blend = scaledPos - index;
        Color c1 = colors[index];
        Color c2 = colors[nextIndex];

        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * blend);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * blend);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * blend);
        return new Color(r, g, b).getRGB();
    }

    /**
     * Toxic - Dark green to light green gradient
     */
    private static int getToxicColor(long time, float offset) {
        // Dark green to light green gradient
        Color darkGreen = new Color(0, 100, 0);      // Dark green
        Color lightGreen = new Color(57, 255, 20);   // Neon/lime green

        // Animated gradient (3 second cycle)
        float position = ((time % 3000) / 3000.0f + offset) % 1.0f;
        // Oscillate between dark and light
        float blend = (float) (Math.sin(position * Math.PI * 2) * 0.5 + 0.5);

        int r = (int) (darkGreen.getRed() + (lightGreen.getRed() - darkGreen.getRed()) * blend);
        int g = (int) (darkGreen.getGreen() + (lightGreen.getGreen() - darkGreen.getGreen()) * blend);
        int b = (int) (darkGreen.getBlue() + (lightGreen.getBlue() - darkGreen.getBlue()) * blend);

        return new Color(r, g, b).getRGB();
    }

    /**
     * Pink - Pastel pink to cherry pink gradient (smooth)
     */
    private static int getPinkColor(long time, float offset) {
        // Pastel pink to cherry pink (soft gradient)
        Color pastelPink = new Color(255, 209, 220);  // Soft pastel pink
        Color rosePink = new Color(255, 174, 201);    // Rose pink (middle)
        Color cherryPink = new Color(255, 145, 175);  // Cherry/raspberry pink
        Color softRose = new Color(255, 182, 193);    // Soft rose

        Color[] colors = { pastelPink, rosePink, cherryPink, rosePink, softRose, pastelPink };

        // Very smooth animated gradient (5 second cycle for smoother transition)
        float position = ((time % 5000) / 5000.0f + offset * 0.3f) % 1.0f;
        float scaledPos = position * (colors.length - 1);
        int index = (int) scaledPos;
        int nextIndex = (index + 1) % colors.length;
        index = Math.min(index, colors.length - 1);

        // Smooth easing for blend
        float blend = scaledPos - index;
        blend = blend * blend * (3 - 2 * blend); // Smoothstep for softer transitions

        Color c1 = colors[index];
        Color c2 = colors[nextIndex];

        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * blend);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * blend);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * blend);

        return new Color(r, g, b).getRGB();
    }

    /**
     * Get watermark first letter color - respeta el color mode del ArrayList
     */
    public static int getWatermarkFirstLetterColor() {
        if (watermarkRainbow) {
            long time = System.currentTimeMillis();
            // Use the same color mode as ArrayList
            return getColorForY(5, 240); // Use top position for consistent color
        }
        return watermarkFirstLetterColor;
    }
}
