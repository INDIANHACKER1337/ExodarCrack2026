/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.hook;

import io.github.exodar.event.EventBus;
import io.github.exodar.event.KeyPressEvent;
import io.github.exodar.ui.AccountManagerScreen;
import io.github.exodar.ui.ArrayListConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiMultiplayer;
import org.lwjgl.input.Keyboard;

public class KeyboardHook extends Thread {

    // Track key states
    private final boolean[] lastKeyStates = new boolean[256];
    private final long[] lastPressTimes = new long[256];

    // Minimum time between presses (ms)
    private static final int DEBOUNCE_MS = 250;

    public KeyboardHook() {
        super("GhostClient-KeyboardHook");
        setDaemon(true);
        System.out.println("[KeyboardHook] Started");
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                Thread.sleep(20); // poll every 20ms

                Minecraft mc = Minecraft.getMinecraft();
                if (mc == null) continue;

                // Check for INSERT key in multiplayer menu (special case - works without world/player)
                boolean inMultiplayerMenu = mc.currentScreen instanceof GuiMultiplayer;
                boolean inAccountManager = mc.currentScreen instanceof AccountManagerScreen;

                if (inMultiplayerMenu || inAccountManager) {
                    // Only check INSERT key when in multiplayer/account manager
                    int menuKey = ArrayListConfig.menuKeybind;
                    boolean currentState = Keyboard.isKeyDown(menuKey);
                    boolean previousState = lastKeyStates[menuKey];
                    long currentTime = System.currentTimeMillis();

                    if (currentState && !previousState) {
                        if (currentTime - lastPressTimes[menuKey] >= DEBOUNCE_MS) {
                            lastPressTimes[menuKey] = currentTime;
                            EventBus.post(new KeyPressEvent(menuKey, true));
                        }
                    }
                    if (!currentState && previousState) {
                        lastPressTimes[menuKey] = 0;
                    }
                    lastKeyStates[menuKey] = currentState;
                    continue;
                }

                // Normal processing requires world and player
                if (mc.theWorld == null || mc.thePlayer == null) continue;

                try {
                    for (int keyCode = 0; keyCode < 256; keyCode++) {
                        boolean currentState = Keyboard.isKeyDown(keyCode);
                        boolean previousState = lastKeyStates[keyCode];
                        long currentTime = System.currentTimeMillis();

                        // Key PRESSED event
                        if (currentState && !previousState) {
                            // Skip if GUI is open (except menu keybind to close GUI)
                            boolean isMenuKey = (keyCode == ArrayListConfig.menuKeybind);
                            if (mc.currentScreen != null && !isMenuKey) {
                                lastKeyStates[keyCode] = currentState;
                                continue;
                            }

                            // Only trigger if cooldown passed
                            if (currentTime - lastPressTimes[keyCode] >= DEBOUNCE_MS) {
                                lastPressTimes[keyCode] = currentTime;
                                EventBus.post(new KeyPressEvent(keyCode, true));
                            }
                        }

                        // Key RELEASED event
                        if (!currentState && previousState) {
                            // Reset last press time to allow next press immediately
                            lastPressTimes[keyCode] = 0;
                        }

                        lastKeyStates[keyCode] = currentState;
                    }
                } catch (Exception e) {
                    System.err.println("[KeyboardHook] Error processing keyboard input");
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
