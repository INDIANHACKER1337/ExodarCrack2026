/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.event;

import net.minecraft.entity.Entity;

public class AttackEvent extends Event {

    private final Entity entity;
    private boolean allowSprint = true;
    private double slowdownFactor = 1.0D;

    public AttackEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public boolean isAllowSprint() {
        return this.allowSprint;
    }

    public void setAllowSprint(boolean allowSprint) {
        this.allowSprint = allowSprint;
    }

    public double getSlowdownFactor() {
        return this.slowdownFactor;
    }

    public void setSlowdownFactor(double slowdownFactor) {
        this.slowdownFactor = slowdownFactor;
    }
}
