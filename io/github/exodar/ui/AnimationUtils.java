/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

/**
 * Delta-based smooth animation utility
 * Based on Raven XD implementation
 */
public class AnimationUtils {
    private double value;
    private long lastMS;

    public AnimationUtils(double value) {
        this.value = value;
        this.lastMS = System.currentTimeMillis();
    }

    /**
     * Calculate compensation for smooth animation
     */
    public static double calculateCompensation(double target, double current, double speed, long delta) {
        double diff = current - target;
        double add = (delta * (speed / 50.0));

        if (diff > speed) {
            if (current - add > target) {
                current -= add;
            } else {
                current = target;
            }
        } else if (diff < -speed) {
            if (current + add < target) {
                current += add;
            } else {
                current = target;
            }
        } else {
            current = target;
        }

        return current;
    }

    /**
     * Smoothly animate towards target value
     * @param target Target value to animate to
     * @param speed Animation speed (1-28, higher = faster)
     */
    public void setAnimation(double target, double speed) {
        long currentMS = System.currentTimeMillis();
        long delta = currentMS - this.lastMS;
        this.lastMS = currentMS;

        // Clamp delta to prevent huge jumps
        if (delta > 100) delta = 100;
        if (delta < 1) delta = 1;

        // Clamp speed
        if (speed > 28) speed = 28;
        if (speed < 1) speed = 1;

        double deltaValue = 0.0;
        if (speed != 0.0) {
            deltaValue = Math.abs(target - this.value) * 0.35 / (10.0 / speed);
        }

        this.value = calculateCompensation(target, this.value, deltaValue, delta);
    }

    /**
     * Get current animated value
     */
    public double getValue() {
        return value;
    }

    /**
     * Force set value without animation
     */
    public void setValue(double value) {
        this.value = value;
    }
}
