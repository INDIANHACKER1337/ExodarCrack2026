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
 * NoVanillaNametags - Cancels vanilla nametag rendering
 * Works by being checked in the render hook
 */
public class NoVanillaNametags extends Module {

    public NoVanillaNametags() {
        super("NoVanillaNametags", ModuleCategory.MISC);
        this.registerSetting(new DescriptionSetting("Hides vanilla nametags"));
        this.registerSetting(new DescriptionSetting("Use with custom Nametags"));
    }

    @Override
    public String getDisplaySuffix() {
        return null;
    }
}
