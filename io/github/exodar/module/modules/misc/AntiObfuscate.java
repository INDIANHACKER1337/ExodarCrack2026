/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;

/**
 * AntiObfuscate - Removes §k obfuscated text formatting
 *
 * When enabled, strips the §k formatting code that causes
 * text to appear as scrambled/random characters.
 */
public class AntiObfuscate extends Module {

    private static AntiObfuscate instance;

    public AntiObfuscate() {
        super("AntiObfuscate", ModuleCategory.MISC);
        instance = this;
        this.registerSetting(new DescriptionSetting("Removes scrambled text"));
    }

    /**
     * Strip obfuscated formatting from text
     * @param input The input string potentially containing §k
     * @return String with §k removed
     */
    public static String stripObfuscated(String input) {
        if (instance == null || !instance.isEnabled() || input == null) {
            return input;
        }
        return input.replaceAll("§k", "");
    }

    public static AntiObfuscate getInstance() {
        return instance;
    }
}
