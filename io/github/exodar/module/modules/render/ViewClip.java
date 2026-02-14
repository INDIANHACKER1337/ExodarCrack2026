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
 * ViewClip - Prevents camera from clipping into walls in third person view
 * Works via hook in EntityRenderer.orientCamera
 */
public class ViewClip extends Module {

    private static ViewClip instance;

    public ViewClip() {
        super("ViewClip", ModuleCategory.VISUALS);
        instance = this;
        this.registerSetting(new DescriptionSetting("No camera clip"));
    }

    public static ViewClip getInstance() {
        return instance;
    }

    public static boolean shouldPreventClip() {
        return instance != null && instance.isEnabled();
    }
}
