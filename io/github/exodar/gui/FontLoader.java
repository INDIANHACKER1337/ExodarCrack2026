/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FontLoader {
    private static final Map<String, Font> loadedFonts = new HashMap<>();
    private static Font exodarFont = null;
    private static Font verdanaFont = null;

    public static Font getExodarFont(float size) {
        if (exodarFont == null) {
            try {
                // Load from resources (embedded in JAR)
                InputStream fontStream = FontLoader.class.getResourceAsStream("/fonts/exodar.ttf");

                if (fontStream != null) {
                    exodarFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                    fontStream.close();
                    System.out.println("[FontLoader] Loaded exodar.ttf from resources");
                } else {
                    System.out.println("[FontLoader] exodar.ttf not found in resources, using default");
                    exodarFont = new Font("Arial", Font.BOLD, 16);
                }
            } catch (FontFormatException | IOException e) {
                System.out.println("[FontLoader] Error loading exodar.ttf: " + e.getMessage());
                exodarFont = new Font("Arial", Font.BOLD, 16);
            }
        }
        return exodarFont.deriveFont(size);
    }

    public static Font getVerdanaFont(float size) {
        if (verdanaFont == null) {
            try {
                // Load from resources (embedded in JAR)
                InputStream fontStream = FontLoader.class.getResourceAsStream("/fonts/verdana.ttf");

                if (fontStream != null) {
                    verdanaFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                    fontStream.close();
                    System.out.println("[FontLoader] Loaded verdana.ttf from resources");
                } else {
                    System.out.println("[FontLoader] verdana.ttf not found in resources, using system Verdana");
                    verdanaFont = new Font("Verdana", Font.PLAIN, 12);
                }
            } catch (FontFormatException | IOException e) {
                System.out.println("[FontLoader] Error loading verdana.ttf: " + e.getMessage());
                verdanaFont = new Font("Verdana", Font.PLAIN, 12);
            }
        }
        return verdanaFont.deriveFont(size);
    }
}
