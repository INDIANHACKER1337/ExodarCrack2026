/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import io.github.exodar.module.Module;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Time-based slide animations for ArrayList modules
 * Uses Easing functions for smooth, consistent animations
 * Tracks both entering and exiting modules for proper slide animations
 */
public class ModuleAnimation {
    private static final Map<String, AnimationState> animations = new HashMap<>();

    // Animation duration in milliseconds (longer = smoother)
    private static final long SLIDE_DURATION = 350; // 350ms for slide
    private static final long Y_DURATION = 250; // 250ms for vertical movement

    // Track modules that are animating out
    private static final List<String> animatingOut = new ArrayList<>();

    public static class AnimationState {
        // Use Animation objects with easing
        public final Animation animationX;
        public final Animation animationY;
        public boolean currentlyEnabled = false;
        public boolean previouslyEnabled = false;
        public String moduleName;
        public int lastTargetY = 0;

        public AnimationState(String name) {
            this.moduleName = name;
            // EASE_OUT_QUART gives smooth deceleration without being too snappy
            this.animationX = new Animation(Easing.EASE_OUT_QUART, SLIDE_DURATION);
            this.animationY = new Animation(Easing.EASE_OUT_CUBIC, Y_DURATION);
            // Start off-screen
            this.animationX.setValue(200);
            this.animationY.setValue(0);
        }

        public float getX() {
            return (float) animationX.getValue();
        }

        public float getY() {
            return (float) animationY.getValue();
        }

        /**
         * Get X position interpolated with partialTicks for smooth rendering
         */
        public float getX(float partialTicks) {
            return (float) animationX.getInterpolatedValue(partialTicks);
        }

        /**
         * Get Y position interpolated with partialTicks for smooth rendering
         */
        public float getY(float partialTicks) {
            return (float) animationY.getInterpolatedValue(partialTicks);
        }

        public boolean isVisible() {
            return animationX.getValue() < 195;
        }

        public boolean isAnimatingOut() {
            return !currentlyEnabled && animationX.getValue() < 195;
        }
    }

    /**
     * Get or create animation state for module
     */
    public static AnimationState getState(Module module) {
        String key = module.getName();
        AnimationState state = animations.get(key);
        if (state == null) {
            state = new AnimationState(key);
            animations.put(key, state);
        }
        return state;
    }

    /**
     * Get animation state by name (for exiting modules)
     */
    public static AnimationState getStateByName(String name) {
        return animations.get(name);
    }

    /**
     * Mark module as enabled and start slide-in animation
     */
    public static void setEnabled(Module module, int targetY) {
        AnimationState state = getState(module);

        // If just enabled (wasn't enabled before), reset X for slide-in
        if (!state.currentlyEnabled) {
            state.animationX.setValue(200); // Start off-screen
            // Remove from animating out list if it was there
            animatingOut.remove(module.getName());
        }

        state.previouslyEnabled = state.currentlyEnabled;
        state.currentlyEnabled = true;
        state.lastTargetY = targetY;

        // Run animations towards on-screen position
        state.animationX.run(0); // Target is on-screen (X offset = 0)
        state.animationY.run(targetY);
    }

    /**
     * Mark module as disabled and start slide-out animation
     */
    public static void setDisabled(Module module) {
        AnimationState state = getState(module);

        // If was enabled, start slide-out
        if (state.currentlyEnabled) {
            // Add to animating out list
            if (!animatingOut.contains(module.getName())) {
                animatingOut.add(module.getName());
            }
        }

        state.previouslyEnabled = state.currentlyEnabled;
        state.currentlyEnabled = false;

        // Run animation towards off-screen
        state.animationX.run(200); // Slide out to the right
    }

    /**
     * Update animations for a module (call every frame)
     */
    public static void updateAnimation(Module module, int targetY, boolean enabled) {
        if (enabled) {
            setEnabled(module, targetY);
        } else {
            setDisabled(module);
        }
    }

    /**
     * Update all exiting module animations and get list of modules still animating out
     * Returns list of module names that are still visible while animating out
     */
    public static List<String> updateExitingModules() {
        List<String> stillVisible = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (String name : animatingOut) {
            AnimationState state = animations.get(name);
            if (state != null) {
                // Continue the slide-out animation
                state.animationX.run(200);

                // Check if still visible
                if (state.isVisible()) {
                    stillVisible.add(name);
                } else {
                    // Animation complete, can remove
                    toRemove.add(name);
                }
            } else {
                toRemove.add(name);
            }
        }

        // Remove completed animations
        animatingOut.removeAll(toRemove);

        return stillVisible;
    }

    /**
     * Get all modules currently animating out
     */
    public static List<String> getAnimatingOutModules() {
        return new ArrayList<>(animatingOut);
    }

    /**
     * Check if module animation is visible (not fully off-screen)
     */
    public static boolean isVisible(Module module) {
        AnimationState state = getState(module);
        return state.isVisible();
    }

    /**
     * Clear all animations
     */
    public static void clearAll() {
        animations.clear();
        animatingOut.clear();
    }

    /**
     * Force reset animation for a specific module (for replay)
     */
    public static void resetModule(Module module) {
        String key = module.getName();
        AnimationState state = animations.get(key);
        if (state != null) {
            state.animationX.setValue(200);
            state.currentlyEnabled = false;
            state.previouslyEnabled = false;
        }
        animatingOut.remove(key);
    }
}
