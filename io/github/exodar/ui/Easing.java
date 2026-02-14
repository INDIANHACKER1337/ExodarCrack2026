/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import java.util.function.Function;

/**
 * Easing functions for smooth animations
 * Based on Raven XD implementation
 * More easing functions: https://easings.net/
 */
public enum Easing {
    LINEAR(x -> x),
    EASE_IN_QUAD(x -> x * x),
    EASE_OUT_QUAD(x -> x * (2 - x)),
    EASE_IN_OUT_QUAD(x -> x < 0.5 ? 2 * x * x : -1 + (4 - 2 * x) * x),
    EASE_IN_CUBIC(x -> x * x * x),
    EASE_OUT_CUBIC(x -> {
        double t = x - 1;
        return t * t * t + 1;
    }),
    EASE_IN_OUT_CUBIC(x -> x < 0.5 ? 4 * x * x * x : (x - 1) * (2 * x - 2) * (2 * x - 2) + 1),
    EASE_OUT_QUART(x -> {
        double t = x - 1;
        return 1 - t * t * t * t;
    }),
    EASE_OUT_CIRC(x -> {
        double t = x - 1;
        return Math.sqrt(1 - t * t);
    }),
    EASE_OUT_EXPO(x -> x == 1 ? 1 : 1 - Math.pow(2, -10 * x)),
    EASE_OUT_ELASTIC(x -> {
        if (x == 0) return 0.0;
        if (x == 1) return 1.0;
        return Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * ((2 * Math.PI) / 3)) * 0.5 + 1;
    }),
    DECELERATE(x -> 1 - ((x - 1) * (x - 1)));

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    public Function<Double, Double> getFunction() {
        return function;
    }

    public double apply(double progress) {
        // Clamp progress to 0-1
        progress = Math.max(0, Math.min(1, progress));
        return function.apply(progress);
    }
}
