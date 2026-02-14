/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

/**
 * MoveInputEvent - Fired after movement input is processed
 * Port from teamoandre mixin system
 * Allows modules to modify movement input (moveForward, moveStrafing, sneak, jump)
 *
 * Supports both naming styles:
 * - Exodar style: getMoveForward(), getMoveStrafe()
 * - cc.unknown style: getForward(), getStrafe()
 */
public class MoveInputEvent extends Event {

    private float moveForward;
    private float moveStrafe;
    private boolean sneak;
    private boolean jump;

    public MoveInputEvent(float moveForward, float moveStrafe, boolean sneak, boolean jump) {
        this.moveForward = moveForward;
        this.moveStrafe = moveStrafe;
        this.sneak = sneak;
        this.jump = jump;
    }

    // === Exodar style methods ===

    public float getMoveForward() {
        return moveForward;
    }

    public void setMoveForward(float moveForward) {
        this.moveForward = moveForward;
    }

    public float getMoveStrafe() {
        return moveStrafe;
    }

    public void setMoveStrafe(float moveStrafe) {
        this.moveStrafe = moveStrafe;
    }

    // === cc.unknown style aliases ===

    public float getForward() {
        return moveForward;
    }

    public void setForward(float forward) {
        this.moveForward = forward;
    }

    public float getStrafe() {
        return moveStrafe;
    }

    public void setStrafe(float strafe) {
        this.moveStrafe = strafe;
    }

    // === Common methods (same in both styles) ===

    public boolean isSneak() {
        return sneak;
    }

    public void setSneak(boolean sneak) {
        this.sneak = sneak;
    }

    public boolean isJump() {
        return jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }
}
