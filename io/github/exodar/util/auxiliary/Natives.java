/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.util.auxiliary;

import org.lwjgl.input.Keyboard;

/**
 * Native keyboard input using JNI - identical to cc.unknown.loader.Natives
 * Sends keyboard input directly to the OS, bypassing Minecraft's input system
 *
 * Native methods are registered by the DLL via RegisterNatives
 */
public class Natives {

    // Native methods implemented in C++ (src/natives/natives.cpp)
    private static native void nativeSetKeyBoard(int lwjglKeyCode, boolean pressed);
    private static native boolean nativeIsKeyDown(int lwjglKeyCode);
    private static native void nativeSetMouse(int button, boolean pressed);
    private static native boolean nativeIsMouseDown(int button);

    // Mouse button constants (matches LWJGL Mouse)
    public static final int MOUSE_LEFT = 0;
    public static final int MOUSE_RIGHT = 1;
    public static final int MOUSE_MIDDLE = 2;

    // Track if natives are available
    private static boolean nativesAvailable = true;

    /**
     * Set keyboard key state - sends input directly to OS
     * Identical to cc.unknown Natives.SetKeyBoard
     *
     * @param lwjglKeyCode LWJGL key code (e.g., Keyboard.KEY_LSHIFT = 42)
     * @param pressed true to press, false to release
     */
    public static void SetKeyBoard(int lwjglKeyCode, boolean pressed) {
        if (nativesAvailable) {
            try {
                nativeSetKeyBoard(lwjglKeyCode, pressed);
            } catch (UnsatisfiedLinkError e) {
                nativesAvailable = false;
                System.err.println("[Natives] Native methods not available, keyboard input disabled");
            }
        }
    }

    /**
     * Check if a key is currently pressed at OS level
     * Identical to cc.unknown Natives.IsKeyDown
     *
     * @param lwjglKeyCode LWJGL key code
     * @return true if key is pressed
     */
    public static boolean IsKeyDown(int lwjglKeyCode) {
        if (nativesAvailable) {
            try {
                return nativeIsKeyDown(lwjglKeyCode);
            } catch (UnsatisfiedLinkError e) {
                nativesAvailable = false;
            }
        }
        // Fallback to LWJGL
        return Keyboard.isKeyDown(lwjglKeyCode);
    }

    /**
     * Set mouse button state - sends input directly to OS
     * Clicks will appear in keystrokes/CPS mods
     *
     * @param button 0 = left, 1 = right, 2 = middle
     * @param pressed true to press, false to release
     */
    public static void SetMouse(int button, boolean pressed) {
        if (nativesAvailable) {
            try {
                nativeSetMouse(button, pressed);
            } catch (UnsatisfiedLinkError e) {
                nativesAvailable = false;
            }
        }
    }

    /**
     * Perform a mouse click (press + release)
     * Click will appear in keystrokes/CPS mods
     *
     * @param button 0 = left, 1 = right, 2 = middle
     */
    public static void Click(int button) {
        SetMouse(button, true);
        SetMouse(button, false);
    }

    /**
     * Perform a left click
     */
    public static void LeftClick() {
        Click(MOUSE_LEFT);
    }

    /**
     * Perform a right click
     */
    public static void RightClick() {
        Click(MOUSE_RIGHT);
    }

    /**
     * Check if a mouse button is currently pressed at OS level
     *
     * @param button 0 = left, 1 = right, 2 = middle
     * @return true if button is pressed
     */
    public static boolean IsMouseDown(int button) {
        if (nativesAvailable) {
            try {
                return nativeIsMouseDown(button);
            } catch (UnsatisfiedLinkError e) {
                nativesAvailable = false;
            }
        }
        // Fallback to LWJGL
        return org.lwjgl.input.Mouse.isButtonDown(button);
    }

    /**
     * Get the Windows Virtual Key code for sneak (Left Shift)
     * The original uses hardcoded 32, but we use proper LWJGL key code
     */
    public static final int KEY_SNEAK = Keyboard.KEY_LSHIFT; // 42 in LWJGL
}
