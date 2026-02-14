/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.DescriptionSetting;

/**
 * Fullbright - Makes everything fully lit
 * Works by setting gamma to maximum
 */
public class Fullbright extends Module {

    private float previousGamma = 1.0f;

    public Fullbright() {
        super("Fullbright", ModuleCategory.VISUALS);
        this.registerSetting(new DescriptionSetting("See in the dark"));
    }

    @Override
    public void onEnable() {
        if (mc.gameSettings != null) {
            previousGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 100.0f;
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = previousGamma;
        }
    }

    @Override
    public void onUpdate() {
        // Keep gamma at max in case something resets it
        if (enabled && mc.gameSettings != null) {
            if (mc.gameSettings.gammaSetting < 100.0f) {
                mc.gameSettings.gammaSetting = 100.0f;
            }
        }
    }
}
