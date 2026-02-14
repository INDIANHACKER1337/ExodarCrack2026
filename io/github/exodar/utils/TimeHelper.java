/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.utils;

/**
 * TimeHelper - Port from Augustus
 * Simple timing utility for measuring elapsed time
 */
public class TimeHelper {
    private long lastTime;

    public TimeHelper() {
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * Reset the timer to current time
     */
    public void reset() {
        this.lastTime = System.currentTimeMillis();
    }

    /**
     * Check if specified milliseconds have passed
     * @param milliseconds Time to check
     * @return true if time has passed
     */
    public boolean hasReached(long milliseconds) {
        return System.currentTimeMillis() - this.lastTime >= milliseconds;
    }

    /**
     * Get elapsed time since last reset
     * @return Elapsed time in milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastTime;
    }

    /**
     * Get last recorded time
     * @return Last time in milliseconds
     */
    public long getLastTime() {
        return this.lastTime;
    }

    /**
     * Set last time manually
     * @param time Time in milliseconds
     */
    public void setLastTime(long time) {
        this.lastTime = time;
    }
}
