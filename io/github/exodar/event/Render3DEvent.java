/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

/**
 * Event fired during world rendering at the correct point for 3D ESP rendering.
 * Fired AFTER world/entities/particles are rendered, BEFORE hand is rendered.
 * At this point, the GL matrices are set up correctly for 3D world-space rendering.
 */
public class Render3DEvent extends Event {

    private final float partialTicks;

    public Render3DEvent(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
