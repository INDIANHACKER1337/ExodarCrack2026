/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.patcher;

import io.github.exodar.event.AttackEvent;
import io.github.exodar.event.EventBus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

import java.lang.reflect.Method;

/**
 * AttackEventBridge - Fires AttackEvent when player attacks
 *
 * teamoandre approach:
 * - PlayerControllerMP.attackEntity is patched to call onAttack at method HEAD
 * - This fires AttackEvent so modules can react to attacks
 * - Simpler than patching EntityPlayer bytecode
 */
public class AttackEventBridge {
    private static boolean initialized = false;

    private static final String ATTACK_METHOD_KEY = "exodar.onAttack";

    /**
     * Initialize the attack event bridge
     */
    public static void initialize() {
        if (initialized) return;

        System.out.println("[AttackEventBridge] Initializing...");

        try {
            // Register the onAttack method for the patcher to call
            Method attackMethod = AttackEventBridge.class.getDeclaredMethod("onAttack", Object.class);
            System.getProperties().put(ATTACK_METHOD_KEY, attackMethod);

            initialized = true;
            System.out.println("[AttackEventBridge] Initialized successfully");
        } catch (Exception e) {
            System.out.println("[AttackEventBridge] ERROR initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shutdown and cleanup
     */
    public static void shutdown() {
        if (!initialized) return;

        System.getProperties().remove(ATTACK_METHOD_KEY);
        initialized = false;
    }

    /**
     * Called at the start of EntityPlayer.attackTargetEntityWithCurrentItem
     * Fires AttackEvent so modules can modify slowdownFactor and allowSprint
     * Then applies the slowdown using *= with event properties
     *
     * @param targetEntity The entity being attacked (as Object for reflection compatibility)
     */
    public static void onAttack(Object targetEntity) {
        if (!initialized) return;

        try {
            if (targetEntity instanceof Entity) {
                Entity entity = (Entity) targetEntity;
                EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
                if (player == null) return;

                // Create and fire the event - handlers can modify slowdownFactor/allowSprint
                AttackEvent event = new AttackEvent(entity);
                EventBus.post(event);

                // Apply slowdown using *= with event properties (like Intave/Sakura)
                double factor = event.getSlowdownFactor();
                if (factor != 1.0D) {
                    player.motionX *= factor;
                    player.motionZ *= factor;
                }

                // Handle sprint based on event
                if (!event.isAllowSprint()) {
                    player.setSprinting(false);
                }

                System.out.println("[AttackEventBridge] AttackEvent fired - factor=" + factor + ", allowSprint=" + event.isAllowSprint());
            }
        } catch (Exception e) {
            System.out.println("[AttackEventBridge] Error in onAttack: " + e.getMessage());
        }
    }
}
