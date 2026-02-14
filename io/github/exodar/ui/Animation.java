/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

/**
 * Time-based animation with easing functions
 * Based on Raven XD implementation
 * Supports partialTicks for smooth frame interpolation
 */
public class Animation {
    private Easing easing;
    private long duration;
    private long startTime;

    private double startValue;
    private double destinationValue;
    private double value;
    private double previousValue; // For partialTicks interpolation
    private boolean finished;

    // Frame timing for partialTicks interpolation
    private static final long FRAME_TIME = 50; // 50ms = 20 ticks per second (Minecraft tick rate)
    private long lastUpdateTime;

    public Animation(Easing easing, long duration) {
        this.easing = easing;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.lastUpdateTime = this.startTime;
        this.startValue = 0;
        this.destinationValue = 0;
        this.value = 0;
        this.previousValue = 0;
        this.finished = false;
    }

    /**
     * Updates the animation by using the easing function and time
     * @param destinationValue the value that the animation is going to reach
     */
    public void run(double destinationValue) {
        long millis = System.currentTimeMillis();

        // Store previous value for interpolation
        this.previousValue = this.value;
        this.lastUpdateTime = millis;

        // If destination changed, reset animation from current position
        if (this.destinationValue != destinationValue) {
            this.destinationValue = destinationValue;
            this.reset();
        } else {
            this.finished = millis - this.duration > this.startTime;
            if (this.finished) {
                this.value = destinationValue;
                this.previousValue = destinationValue;
                return;
            }
        }

        // Apply easing function
        double progress = getProgress();
        double result = this.easing.apply(progress);

        if (this.startValue > destinationValue) {
            this.value = this.startValue - (this.startValue - destinationValue) * result;
        } else {
            this.value = this.startValue + (destinationValue - this.startValue) * result;
        }
    }

    /**
     * Returns the progress of the animation (0 to 1)
     */
    public double getProgress() {
        long elapsed = System.currentTimeMillis() - this.startTime;
        return Math.min(1.0, (double) elapsed / (double) this.duration);
    }

    /**
     * Get interpolated value using partialTicks for smooth frame rendering
     * @param partialTicks The partial tick value (0.0 to 1.0 between ticks)
     * @return Interpolated value for smooth rendering
     */
    public double getInterpolatedValue(float partialTicks) {
        // Interpolate between previous and current value based on partialTicks
        return previousValue + (value - previousValue) * partialTicks;
    }

    /**
     * Resets the animation to the current value as start
     */
    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.startValue = this.value;
        this.finished = false;
    }

    /**
     * Force set the value without animation
     */
    public void setValue(double value) {
        this.value = value;
        this.startValue = value;
        this.destinationValue = value;
        this.finished = true;
    }

    public double getValue() {
        return value;
    }

    public double getDestinationValue() {
        return destinationValue;
    }

    public boolean isFinished() {
        return finished;
    }

    public Easing getEasing() {
        return easing;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
