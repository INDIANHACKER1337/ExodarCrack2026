/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Patches PlayerControllerMP to inject AttackEvent support
 * Based on teamoandre MixinPlayerControllerMP approach
 *
 * Target: net.minecraft.client.multiplayer.PlayerControllerMP
 * Method: attackEntity(EntityPlayer, Entity)V
 *
 * Much simpler than patching EntityPlayer - just fires event at method start
 */
public class PlayerControllerPatcher {

    private static final String TARGET_CLASS = "net/minecraft/client/multiplayer/PlayerControllerMP";
    private static final String TARGET_METHOD = "attackEntity";
    private static final String TARGET_DESC = "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)V";

    public static boolean shouldPatch(String className) {
        return TARGET_CLASS.equals(className);
    }

    public static byte[] patch(byte[] classBytes) {
        try {
            System.out.println("[PlayerControllerPatcher] Starting patch...");
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.EXPAND_FRAMES);

            boolean found = false;
            for (MethodNode mn : cn.methods) {
                if (mn.name.equals(TARGET_METHOD) && mn.desc.equals(TARGET_DESC)) {
                    System.out.println("[PlayerControllerPatcher] Found target method: " + mn.name);
                    injectAttackEvent(mn);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("[PlayerControllerPatcher] Target method not found, skipping patch");
                return classBytes;
            }

            System.out.println("[PlayerControllerPatcher] Writing patched class...");
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            System.out.println("[PlayerControllerPatcher] Patch complete!");
            return cw.toByteArray();
        } catch (Exception e) {
            System.out.println("[PlayerControllerPatcher] ERROR: " + e.getMessage());
            e.printStackTrace();
            return classBytes;
        }
    }

    /**
     * Inject at start of attackEntity method:
     *
     * Method method = (Method) System.getProperties().get("exodar.onAttack");
     * if (method != null) {
     *     method.invoke(null, new Object[] { targetEntity });
     * }
     */
    private static void injectAttackEvent(MethodNode mn) {
        InsnList inject = new InsnList();

        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode catchHandler = new LabelNode();
        LabelNode afterTry = new LabelNode();

        inject.add(tryStart);

        // Get the Method from System.getProperties().get("exodar.onAttack")
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperties",
            "()Ljava/util/Properties;", false));
        inject.add(new LdcInsnNode("exodar.onAttack"));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Properties", "get",
            "(Ljava/lang/Object;)Ljava/lang/Object;", false));

        // Check if null
        inject.add(new InsnNode(Opcodes.DUP));
        LabelNode notNull = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IFNONNULL, notNull));
        inject.add(new InsnNode(Opcodes.POP));
        inject.add(new JumpInsnNode(Opcodes.GOTO, afterTry));

        inject.add(notNull);
        // Cast to Method and invoke with targetEntity (parameter 2)
        inject.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/reflect/Method"));
        inject.add(new InsnNode(Opcodes.ACONST_NULL)); // null for static invoke
        inject.add(new InsnNode(Opcodes.ICONST_1));
        inject.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        inject.add(new InsnNode(Opcodes.DUP));
        inject.add(new InsnNode(Opcodes.ICONST_0));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 2)); // targetEntity is parameter 2
        inject.add(new InsnNode(Opcodes.AASTORE));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false));
        inject.add(new InsnNode(Opcodes.POP)); // discard return value

        inject.add(tryEnd);
        inject.add(new JumpInsnNode(Opcodes.GOTO, afterTry));

        inject.add(catchHandler);
        inject.add(new InsnNode(Opcodes.POP)); // discard exception

        inject.add(afterTry);

        // Insert at method start (after labels/line numbers)
        AbstractInsnNode firstInsn = mn.instructions.getFirst();
        while (firstInsn instanceof LabelNode || firstInsn instanceof LineNumberNode || firstInsn instanceof FrameNode) {
            firstInsn = firstInsn.getNext();
        }
        mn.instructions.insertBefore(firstInsn, inject);

        // Register try-catch
        mn.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchHandler, "java/lang/Throwable"));

        System.out.println("[PlayerControllerPatcher] Injected AttackEvent at method HEAD");
    }
}
