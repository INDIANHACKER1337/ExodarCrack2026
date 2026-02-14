/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassModifier for FontRenderer that patches text rendering to support AntiObfuscate
 * Strips §k formatting codes when AntiObfuscate module is enabled
 */
public class FontRendererClassModifier extends ClassModifier {

    public FontRendererClassModifier() {
        super("net/minecraft/client/gui/FontRenderer");
    }

    @Override
    public byte[] patch(byte[] originalBytes) {
        System.out.println("[FontRendererClassModifier] Patching FontRenderer for AntiObfuscate");
        return FontRendererPatcher.patch(originalBytes);
    }

    @Override
    public Set<String> getEventHandlerClassesNames() {
        Set<String> result = new HashSet<>();
        // Add the AntiObfuscate class since we call it from patched code
        result.add("io.github.exodar.module.modules.misc.AntiObfuscate");
        return result;
    }
}
