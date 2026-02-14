/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.util;

/**
 * Clock - Time tracking utility
 * 1:1 port from Sakura
 */
public class Clock {

    private long currentMs;

    public Clock() {
        reset();
    }

    public Clock(long ms) {
        this.currentMs = ms;
    }

    public boolean reach(long milliseconds, boolean reset) {
        if (elapsed() > milliseconds) {
            if (reset) {
                reset();
            }
            return true;
        }
        return false;
    }

    public boolean reach(long milliseconds) {
        return (elapsed() > milliseconds);
    }

    public long elapsed() {
        return System.currentTimeMillis() - this.currentMs;
    }

    public void reset() {
        this.currentMs = System.currentTimeMillis();
    }

    public long getTime() {
        return this.currentMs;
    }

    public void setTime(long currentMs) {
        this.currentMs = currentMs;
    }
}
