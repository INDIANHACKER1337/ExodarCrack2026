/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.util.auxiliary;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Identical to cc.unknown.utils.player.PlayerUtil
 */
public final class PlayerUtilAux {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private PlayerUtilAux() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static boolean isBlock() {
        ItemStack stack = getStack();
        return stack != null && stack.getItem() instanceof ItemBlock;
    }

    public static ItemStack getStack() {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return null;
        return mc.thePlayer.inventory.getCurrentItem();
    }
}
