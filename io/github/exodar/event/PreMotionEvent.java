/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

/**
 * PreMotionEvent - Fired before player movement packet is sent
 * Allows modules to modify the rotation that will be sent to the server
 * Used for Silent Aim
 */
public class PreMotionEvent extends Event {

    private float yaw;
    private float pitch;
    private double x;
    private double y;
    private double z;
    private boolean onGround;
    private boolean modified;

    public PreMotionEvent(float yaw, float pitch, double x, double y, double z, boolean onGround) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;
        this.modified = false;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        this.modified = true;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        this.modified = true;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        this.modified = true;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        this.modified = true;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
        this.modified = true;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }
}
