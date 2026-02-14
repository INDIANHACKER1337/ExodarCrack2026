/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.hook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;

import java.lang.reflect.Field;

public class GuiRenderHook extends Thread {


    public GuiRenderHook() {
        super("GhostClient-GuiRenderHook");
        setDaemon(true);
    }

    @Override
    public void run() {
        boolean hooked = false;
        try {
            while (!isInterrupted()) {
                Thread.sleep(100);
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.ingameGUI != null && !hooked) {
                    mc.addScheduledTask(() -> {
                        try {
                            Field gameOverlayField = Minecraft.class.getDeclaredField("ingameGUI");
                            gameOverlayField.setAccessible(true);
                            GuiIngame original = (GuiIngame) gameOverlayField.get(mc);

                            if (!(original instanceof GuiIngameWrapper)) {
                                GuiIngameWrapper wrapped = new GuiIngameWrapper(original);
                                gameOverlayField.set(mc, wrapped);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    hooked = true;
                }
            }
        } catch (InterruptedException ignored) { }
        catch (Exception e) { e.printStackTrace(); }
    }
}