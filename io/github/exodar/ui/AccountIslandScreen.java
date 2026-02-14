/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/**
 * Standalone screen for AccountIsland
 * Used when opening from multiplayer menu
 */
public class AccountIslandScreen extends GuiScreen {

    private final GuiScreen parentScreen;

    public AccountIslandScreen(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Draw semi-transparent background
        this.drawDefaultBackground();

        // Draw the account island
        AccountIsland.render(mc, this.width, this.height);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // Close on ESC or menu keybind
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == ArrayListConfig.menuKeybind) {
            // If not adding account, close the screen
            if (!AccountIsland.isAddingAccount()) {
                mc.displayGuiScreen(parentScreen);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // AccountIsland handles its own clicks in render()
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
