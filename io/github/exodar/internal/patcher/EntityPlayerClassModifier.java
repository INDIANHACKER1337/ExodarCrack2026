/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import java.util.HashSet;
import java.util.Set;

/**
 * Special ClassModifier for EntityPlayer that patches attackTargetEntityWithCurrentItem
 * to inject AttackEvent support (1:1 Sakura mixin behavior)
 *
 * Changes:
 * 1. Creates AttackEvent at method start
 * 2. Replaces hardcoded 0.6 slowdown with event.getSlowdownFactor()
 * 3. Replaces setSprinting(false) with event.isAllowSprint()
 */
public class EntityPlayerClassModifier extends ClassModifier {

    public EntityPlayerClassModifier() {
        super("net/minecraft/entity/player/EntityPlayer");
    }

    @Override
    public byte[] patch(byte[] originalBytes) {
        return EntityPlayerPatcher.patch(originalBytes);
    }

    @Override
    public Set<String> getEventHandlerClassesNames() {
        Set<String> result = new HashSet<>();
        // Add the event classes that need to be excluded from Lunar classloader
        result.add("io.github.exodar.event.AttackEvent");
        result.add("io.github.exodar.event.EventBus");
        result.add("io.github.exodar.patcher.AttackEventBridge");
        return result;
    }
}
