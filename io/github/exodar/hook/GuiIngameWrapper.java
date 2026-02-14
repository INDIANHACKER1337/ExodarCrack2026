/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.hook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;

public class GuiIngameWrapper extends GuiIngame {

    private GuiIngame original;

    public GuiIngameWrapper(GuiIngame original) {
        super(Minecraft.getMinecraft());
        this.original = original;
    }

    @Override
    public void renderGameOverlay(float partialTicks) {
        // Just call super - Render2DEvent is fired from HudRenderThread at 60fps
        super.renderGameOverlay(partialTicks);
    }

}