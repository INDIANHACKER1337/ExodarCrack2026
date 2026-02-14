/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

/**
 * SafeWalkEvent - Fired when checking if player should be safe walking
 * Port from Raven XD MixinEntity
 * Allows modules to prevent walking off edges without holding sneak
 */
public class SafeWalkEvent extends Event {

    private boolean safeWalk;

    public SafeWalkEvent(boolean safeWalk) {
        this.safeWalk = safeWalk;
    }

    public boolean isSafeWalk() {
        return safeWalk;
    }

    public void setSafeWalk(boolean safeWalk) {
        this.safeWalk = safeWalk;
    }
}
