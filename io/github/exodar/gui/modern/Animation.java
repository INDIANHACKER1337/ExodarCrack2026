/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.gui.modern;

/**
 * Animation utilities with easing functions
 * Makes the GUI feel smooth and modern
 */
public class Animation {

    private float value;
    private float target;
    private float speed;
    private EasingType easingType;
    private long lastUpdateTime;

    public Animation(float initialValue, float speed, EasingType easingType) {
        this.value = initialValue;
        this.target = initialValue;
        this.speed = speed;
        this.easingType = easingType;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public Animation(float initialValue, float speed) {
        this(initialValue, speed, EasingType.EASE_OUT_CUBIC);
    }

    public Animation(float initialValue) {
        this(initialValue, 10f, EasingType.EASE_OUT_CUBIC);
    }

    /**
     * Update the animation value
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;

        if (Math.abs(value - target) < 0.001f) {
            value = target;
            return;
        }

        float t = Math.min(1, deltaTime * speed);
        float easedT = applyEasing(t);

        value = value + (target - value) * easedT;
    }

    /**
     * Apply easing function
     */
    private float applyEasing(float t) {
        switch (easingType) {
            case LINEAR:
                return t;

            case EASE_IN_QUAD:
                return t * t;

            case EASE_OUT_QUAD:
                return 1 - (1 - t) * (1 - t);

            case EASE_IN_OUT_QUAD:
                return t < 0.5f ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;

            case EASE_IN_CUBIC:
                return t * t * t;

            case EASE_OUT_CUBIC:
                return 1 - (float)Math.pow(1 - t, 3);

            case EASE_IN_OUT_CUBIC:
                return t < 0.5f ? 4 * t * t * t : 1 - (float)Math.pow(-2 * t + 2, 3) / 2;

            case EASE_IN_QUART:
                return t * t * t * t;

            case EASE_OUT_QUART:
                return 1 - (float)Math.pow(1 - t, 4);

            case EASE_OUT_EXPO:
                return t == 1 ? 1 : 1 - (float)Math.pow(2, -10 * t);

            case EASE_OUT_BACK:
                float c1 = 1.70158f;
                float c3 = c1 + 1;
                return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);

            case EASE_OUT_ELASTIC:
                if (t == 0) return 0;
                if (t == 1) return 1;
                float c4 = (2 * (float)Math.PI) / 3;
                return (float)Math.pow(2, -10 * t) * (float)Math.sin((t * 10 - 0.75f) * c4) + 1;

            case EASE_OUT_BOUNCE:
                return bounceOut(t);

            default:
                return t;
        }
    }

    private float bounceOut(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;

        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5 / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        } else {
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    }

    // ============ GETTERS & SETTERS ============

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public float getTarget() {
        return target;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void setTargetInstant(float target) {
        this.target = target;
        this.value = target;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public EasingType getEasingType() {
        return easingType;
    }

    public void setEasingType(EasingType easingType) {
        this.easingType = easingType;
    }

    public boolean isFinished() {
        return Math.abs(value - target) < 0.001f;
    }

    // ============ STATIC HELPERS ============

    /**
     * Simple lerp without animation object
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    /**
     * Smooth lerp with delta time
     */
    public static float smoothLerp(float current, float target, float speed, float deltaTime) {
        float t = 1 - (float)Math.pow(1 - speed, deltaTime * 60);
        return lerp(current, target, t);
    }

    /**
     * Easing function types
     */
    public enum EasingType {
        LINEAR,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_QUAD,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_QUART,
        EASE_OUT_QUART,
        EASE_OUT_EXPO,
        EASE_OUT_BACK,
        EASE_OUT_ELASTIC,
        EASE_OUT_BOUNCE
    }
}
