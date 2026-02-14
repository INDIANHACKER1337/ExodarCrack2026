/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;

import org.lwjgl.input.Mouse;

import java.awt.Robot;
import java.awt.AWTException;
import java.awt.event.InputEvent;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CPSSpoof - Spoofs Lunar Client's Keystrokes/CPS mod
 *
 * Inyecta clicks falsos usando Robot para que el CPS mod los detecte.
 */
public class CPSSpoof extends Module {

    private final SliderSetting extraCPS;
    private final TickSetting onlyWhileHolding;

    private Robot robot = null;
    private Thread spoofThread = null;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Random rand = new Random();

    public CPSSpoof() {
        super("CPSSpoof", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Spoofs CPS display"));
        this.registerSetting(extraCPS = new SliderSetting("Extra CPS", 5.0, 1.0, 20.0, 1.0));
        this.registerSetting(onlyWhileHolding = new TickSetting("Only while holding", true));
    }

    @Override
    public void onEnable() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.out.println("[CPSSpoof] Failed to create Robot: " + e.getMessage());
            setEnabled(false);
            return;
        }

        running.set(true);

        spoofThread = new Thread(() -> {
            System.out.println("[CPSSpoof] Spoof thread started");

            while (running.get()) {
                try {
                    // Check if we should spoof
                    boolean shouldSpoof = true;

                    if (onlyWhileHolding.isEnabled()) {
                        shouldSpoof = Mouse.isButtonDown(0);
                    }

                    if (!shouldSpoof) {
                        Thread.sleep(50);
                        continue;
                    }

                    // Calculate delay for extra CPS
                    double cps = extraCPS.getValue();
                    int delay = (int) (1000.0 / cps);

                    // Add some randomization
                    delay += rand.nextInt(20) - 10;
                    delay = Math.max(delay, 30);

                    // Inject a fake click (very short press/release)
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    Thread.sleep(5);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

                    Thread.sleep(delay);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.out.println("[CPSSpoof] Error: " + e.getMessage());
                }
            }

            System.out.println("[CPSSpoof] Spoof thread stopped");
        }, "CPSSpoof-Thread");

        spoofThread.setDaemon(true);
        spoofThread.start();
    }

    @Override
    public void onDisable() {
        running.set(false);

        if (spoofThread != null) {
            spoofThread.interrupt();
            try {
                spoofThread.join(200);
            } catch (InterruptedException ignored) {}
            spoofThread = null;
        }

        robot = null;
    }

    @Override
    public String getDisplaySuffix() {
        return " §7+" + (int) extraCPS.getValue();
    }
}
