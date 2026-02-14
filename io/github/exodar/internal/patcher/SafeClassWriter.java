/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * Custom ClassWriter that handles missing types gracefully.
 * Used during JVMTI bytecode patching when not all referenced types are available.
 * This is especially important for Optifine compatibility where Optifine classes
 * might not be in the same classloader.
 */
public class SafeClassWriter extends ClassWriter {
    public SafeClassWriter(int flags) {
        super(flags);
    }

    public SafeClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (TypeNotPresentException e) {
            // If a type is not present, just return Object as the common superclass
            // This is safe because we're only injecting simple method calls
            return "java/lang/Object";
        } catch (Exception e) {
            // Catch any other exception (ClassNotFoundException wrapped, etc)
            return "java/lang/Object";
        }
    }
}
