/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.font;

/**
 * Manager for custom fonts
 * Provides easy access to loaded fonts
 */
public class FontManager {
    private static FontManager instance;

    // Custom fonts
    public CustomFont exodar;
    public CustomFont verdana;
    public CustomFont exodarSmall;
    public CustomFont exodarLarge;
    public CustomFont exodarHD;  // High quality for GUI icons
    public CustomFont esp;
    public CustomFont espSmall;
    public CustomFont espLarge;

    private FontManager() {
        try {
            // Load Exodar font (for icons A B C D E F)
            exodar = new CustomFont("/fonts/exodar.ttf", 20);
            exodarSmall = new CustomFont("/fonts/exodar.ttf", 16);
            exodarLarge = new CustomFont("/fonts/exodar.ttf", 32);
            exodarHD = new CustomFont("/fonts/exodar.ttf", 48);  // HD for GUI

            // Load Verdana font
            verdana = new CustomFont("/fonts/verdana.ttf", 18);

            // Load ESP font
            esp = new CustomFont("/fonts/esp.ttf", 20);
            espSmall = new CustomFont("/fonts/esp.ttf", 14);
            espLarge = new CustomFont("/fonts/esp.ttf", 28);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    /**
     * Get the Exodar icon for a given letter
     * @param letter Letter A-F
     * @return String containing the letter (will render as icon with Exodar font)
     */
    public String getExodarIcon(char letter) {
        letter = Character.toUpperCase(letter);
        if (letter >= 'A' && letter <= 'F') {
            return String.valueOf(letter);
        }
        return "";
    }

    /**
     * Draw an Exodar icon
     * @param letter Letter A-F
     * @param x X position
     * @param y Y position
     * @param color Color in ARGB format
     */
    public void drawExodarIcon(char letter, float x, float y, int color) {
        String icon = getExodarIcon(letter);
        if (!icon.isEmpty() && exodar != null) {
            exodar.drawString(icon, x, y, color);
        }
    }
}
