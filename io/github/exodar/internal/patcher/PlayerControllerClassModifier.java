/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import java.util.HashSet;
import java.util.Set;

/**
 * ClassModifier for PlayerControllerMP that patches attackEntity
 * to inject AttackEvent support (teamoandre approach)
 *
 * This is simpler than patching EntityPlayer because:
 * - attackEntity is the entry point for all attacks
 * - We just fire an event at method start
 * - No complex bytecode replacement needed
 */
public class PlayerControllerClassModifier extends ClassModifier {

    public PlayerControllerClassModifier() {
        super("net/minecraft/client/multiplayer/PlayerControllerMP");
    }

    @Override
    public byte[] patch(byte[] originalBytes) {
        return PlayerControllerPatcher.patch(originalBytes);
    }

    @Override
    public Set<String> getEventHandlerClassesNames() {
        Set<String> result = new HashSet<>();
        result.add("io.github.exodar.event.AttackEvent");
        result.add("io.github.exodar.event.EventBus");
        return result;
    }
}
