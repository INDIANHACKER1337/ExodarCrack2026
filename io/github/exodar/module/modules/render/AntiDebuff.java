/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.render;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.Iterator;

/**
 * AntiDebuff - Removes blindness and nausea effects (client-side)
 */
public class AntiDebuff extends Module {

    public AntiDebuff() {
        super("AntiDebuff", ModuleCategory.VISUALS);
    }

    @Override
    public void onUpdate() {
        if (!enabled) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Remove blindness and nausea effects
        Iterator<PotionEffect> iterator = mc.thePlayer.getActivePotionEffects().iterator();
        while (iterator.hasNext()) {
            PotionEffect effect = iterator.next();
            int potionId = effect.getPotionID();

            // Blindness (ID: 15)
            if (potionId == Potion.blindness.id) {
                mc.thePlayer.removePotionEffectClient(potionId);
            }
            // Nausea/Confusion (ID: 9)
            else if (potionId == Potion.confusion.id) {
                mc.thePlayer.removePotionEffectClient(potionId);
            }
        }
    }
}
