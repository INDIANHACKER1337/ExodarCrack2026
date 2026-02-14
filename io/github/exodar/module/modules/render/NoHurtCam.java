/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.ModuleCategory;
import io.github.exodar.module.Module;
import io.github.exodar.setting.DescriptionSetting;

public class NoHurtCam extends Module {

    public NoHurtCam() {
        super("NoHurtCam", ModuleCategory.VISUALS);
        this.registerSetting(new DescriptionSetting("Removes camera shake when hit"));
    }

    @Override
    public void onEnable() {
        System.out.println("[NoHurtCam] Enabled");
    }

    @Override
    public void onDisable() {
        System.out.println("[NoHurtCam] Disabled");
    }

    @Override
    public void onUpdate() {
        // Este módulo funciona via Mixin/Hook en EntityRenderer.hurtCameraEffect()
        // Por ahora solo necesitamos que esté enabled para que el hook lo detecte
    }
}
