/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Patches FontRenderer to strip §k obfuscated text when AntiObfuscate is enabled
 * Target: net.minecraft.client.gui.FontRenderer
 *
 * Modifies the renderString method to call AntiObfuscate.stripObfuscated() on the text
 * This affects ALL text rendering: tab list, chat, nametags, GUIs, etc.
 */
public class FontRendererPatcher {

    private static final String TARGET_CLASS = "net/minecraft/client/gui/FontRenderer";

    public static boolean shouldPatch(String className) {
        return TARGET_CLASS.equals(className);
    }

    public static byte[] patch(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        boolean patched = false;

        for (MethodNode mn : cn.methods) {
            // Patch renderString method - this is where all string rendering starts
            // Method signature: private int renderString(String text, float x, float y, int color, boolean dropShadow)
            if (mn.name.equals("renderString") && mn.desc.equals("(Ljava/lang/String;FFIZ)I")) {
                patchRenderString(mn);
                patched = true;
            }
        }

        if (patched) {
            System.out.println("[FontRendererPatcher] Successfully patched FontRenderer");
        } else {
            System.out.println("[FontRendererPatcher] WARNING: Could not find renderString method");
        }

        SafeClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /**
     * Patch renderString to call AntiObfuscate.stripObfuscated() on the text parameter
     * Injects at the start of the method:
     *   text = AntiObfuscate.stripObfuscated(text);
     */
    private static void patchRenderString(MethodNode mn) {
        System.out.println("[FontRendererPatcher] Patching renderString method");

        InsnList inject = new InsnList();

        // text (parameter 1) = AntiObfuscate.stripObfuscated(text)
        // ALOAD 1 - load text parameter
        inject.add(new VarInsnNode(Opcodes.ALOAD, 1));

        // INVOKESTATIC AntiObfuscate.stripObfuscated(String)String
        inject.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "io/github/exodar/module/modules/misc/AntiObfuscate",
            "stripObfuscated",
            "(Ljava/lang/String;)Ljava/lang/String;",
            false
        ));

        // ASTORE 1 - store result back to text parameter
        inject.add(new VarInsnNode(Opcodes.ASTORE, 1));

        // Insert at the beginning of the method (after any existing labels)
        AbstractInsnNode firstInsn = mn.instructions.getFirst();

        // Skip labels and line numbers at the start
        while (firstInsn != null && (firstInsn instanceof LabelNode || firstInsn instanceof LineNumberNode || firstInsn instanceof FrameNode)) {
            firstInsn = firstInsn.getNext();
        }

        if (firstInsn != null) {
            mn.instructions.insertBefore(firstInsn, inject);
        } else {
            mn.instructions.insert(inject);
        }

        System.out.println("[FontRendererPatcher] Injected AntiObfuscate call at start of renderString");
    }
}
