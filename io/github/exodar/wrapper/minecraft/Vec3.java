/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.wrapper.minecraft;

/**
 * Wrapper for net.minecraft.util.Vec3
 * Used to maintain type semantics when the original class is not directly accessible
 */
public class Vec3 {
    public final Object handle;

    public Vec3(Object handle) {
        this.handle = handle;
    }

    /**
     * Create Vec3 from coordinates using reflection
     */
    public static Vec3 create(double x, double y, double z) {
        try {
            Class<?> vec3Class = Class.forName("net.minecraft.util.Vec3");
            Object mcVec3 = vec3Class.getConstructor(double.class, double.class, double.class)
                    .newInstance(x, y, z);
            return new Vec3(mcVec3);
        } catch (Exception e) {
            return null;
        }
    }
}
