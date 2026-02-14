/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event.auxiliary;

import io.github.exodar.event.Event;

/**
 * Identical to cc.unknown.event.impl.game.MoveInputEvent
 */
public class MoveInputEventAux extends Event {
    private float forward;
    private float strafe;
    private boolean jump;
    private boolean sneak;

    public MoveInputEventAux(float forward, float strafe, boolean jump, boolean sneak) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }

    public float getForward() { return this.forward; }
    public float getStrafe() { return this.strafe; }
    public boolean isJump() { return this.jump; }
    public boolean isSneak() { return this.sneak; }

    public void setForward(float forward) { this.forward = forward; }
    public void setStrafe(float strafe) { this.strafe = strafe; }
    public void setJump(boolean jump) { this.jump = jump; }
    public void setSneak(boolean sneak) { this.sneak = sneak; }
}
