/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import java.util.HashSet;
import java.util.Set;

/**
 * Special ClassModifier for EntityRenderer that wraps the original ClassModifier
 * and adds render3D bytecode patching via EntityRendererPatcher.
 *
 * This patches EntityRenderer to inject render events BEFORE the "hand" section,
 * which is required for correct ESP positions and shader compatibility.
 *
 * Also applies the normal @EventHandler patches (getMouseOver, updateCameraAndRender)
 * from the original ClassModifier.
 */
public class EntityRendererClassModifier extends ClassModifier {

    private final ClassModifier originalClassModifier;

    public EntityRendererClassModifier(ClassModifier originalClassModifier) {
        super("net/minecraft/client/renderer/EntityRenderer");
        this.originalClassModifier = originalClassModifier;
    }

    @Override
    public byte[] patch(byte[] originalBytes) {
        System.out.println("[EntityRendererClassModifier] Patching EntityRenderer (ONLY EntityRendererPatcher)");


        return EntityRendererPatcher.patch(originalBytes);
    }

    @Override
    public Set<String> getEventHandlerClassesNames() {
        Set<String> result = new HashSet<>();

        // Add the bridge class for render events
        result.add("io.github.exodar.patcher.RenderEventBridge");

        // Add event handler classes from the original ClassModifier
        if (originalClassModifier != null) {
            result.addAll(originalClassModifier.getEventHandlerClassesNames());
        }

        return result;
    }
}
