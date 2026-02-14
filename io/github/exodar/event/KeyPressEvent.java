/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;


public class KeyPressEvent extends Event {
    private final int keyCode;
    private final boolean pressed;

    public KeyPressEvent(int keyCode, boolean pressed) {
        this.keyCode = keyCode;
        this.pressed = pressed;
    }

    public int getKeyCode() { return keyCode; }
    public boolean isPressed() { return pressed; }
    public boolean isReleased() { return !pressed; }
}