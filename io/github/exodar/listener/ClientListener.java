/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.listener;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import org.lwjgl.input.Keyboard;

import io.github.exodar.Main;
import io.github.exodar.event.KeyPressEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.gui.SkeetClickGui;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.ui.AccountIslandScreen;
import io.github.exodar.ui.AccountManagerScreen;
import io.github.exodar.ui.ArrayListConfig;

public class ClientListener {

    @Subscribe
    public void onKeyPress(KeyPressEvent event) {
        // Only process key press events (not release)
        if (!event.isPressed()) return;

        Minecraft mc = Minecraft.getMinecraft();
        ModuleManager moduleManager = Main.getModuleManager();
        int keyCode = event.getKeyCode();

        // Menu keybind - Toggle GUI (configurable, default INSERT)
        if (keyCode == ArrayListConfig.menuKeybind) {
            handleGuiToggle(mc, moduleManager);
            return;
        }

        // Handle module keybinds
        // Skip keyCode 0 and 86 (< > key on Spanish keyboards causes issues)
        if (keyCode == 0 || keyCode == 86) {
            return;
        }

        if (moduleManager != null) {
            for (Module module : moduleManager.getModules()) {
                // Legacy keyCode support - only if module has a keybind set (> 0)
                int moduleKey = module.getKeyCode();
                if (moduleKey > 0 && moduleKey == keyCode) {
                    module.toggle();
                }

                // TOGGLE bind (positive = keyboard, negative = mouse)
                int toggleBind = module.getToggleBind();
                if (toggleBind > 0 && toggleBind == keyCode) {
                    module.toggle();
                }
            }
        }
    }

    private void handleGuiToggle(Minecraft mc, ModuleManager moduleManager) {
        // Schedule on main thread to ensure proper mouse handling
        mc.addScheduledTask(() -> {
            try {
                // If in our ClickGUI, close it
                if (mc.currentScreen instanceof SkeetClickGui) {
                    SkeetClickGui.onClose();
                    mc.displayGuiScreen(null);
                }
                // If in AccountManagerScreen, close it and return to multiplayer menu
                else if (mc.currentScreen instanceof AccountManagerScreen) {
                    mc.displayGuiScreen(((AccountManagerScreen) mc.currentScreen).getParentScreen());
                }
                // If in AccountIslandScreen (legacy), close it
                else if (mc.currentScreen instanceof AccountIslandScreen) {
                    mc.displayGuiScreen(null);
                }
                // If in multiplayer menu, show Account Manager
                else if (mc.currentScreen instanceof GuiMultiplayer) {
                    mc.displayGuiScreen(new AccountManagerScreen(mc.currentScreen));
                }
                // If in game (no screen), open full ClickGUI
                else if (mc.currentScreen == null && moduleManager != null && SkeetClickGui.canOpen()) {
                    SkeetClickGui gui = new SkeetClickGui(moduleManager);
                    mc.displayGuiScreen(gui);
                }
            } catch (Exception ignored) {}
        });
    }
}
