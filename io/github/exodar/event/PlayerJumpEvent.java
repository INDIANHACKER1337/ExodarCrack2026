/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

/**
 * PlayerJumpEvent - Fired when player is about to jump
 * Port from LiquidBounce mixin system
 * Allows modules to modify jump motion (e.g., higher jumps for Spider)
 */
public class PlayerJumpEvent extends Event {

    private float motion;

    public PlayerJumpEvent(float motion) {
        this.motion = motion;
    }

    /**
     * Get the current jump motion value
     * Default vanilla jump is 0.42f
     */
    public float getMotion() {
        return motion;
    }

    /**
     * Set a custom jump motion value
     * @param motion The new jump motion (vanilla is 0.42f)
     */
    public void setMotion(float motion) {
        this.motion = motion;
    }
}
