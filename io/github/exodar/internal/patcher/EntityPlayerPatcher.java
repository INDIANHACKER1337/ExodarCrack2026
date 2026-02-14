/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Patches EntityPlayer.attackTargetEntityWithCurrentItem to fire AttackEvent
 * Injects at method HEAD - calls AttackEventBridge.onAttack(targetEntity)
 */
public class EntityPlayerPatcher {

    private static final String TARGET_CLASS = "net/minecraft/entity/player/EntityPlayer";
    private static final String TARGET_METHOD = "attackTargetEntityWithCurrentItem";
    private static final String TARGET_DESC = "(Lnet/minecraft/entity/Entity;)V";

    public static byte[] patch(byte[] classBytes) {
        try {
            System.out.println("[EntityPlayerPatcher] Starting patch...");
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.EXPAND_FRAMES);

            boolean found = false;
            for (MethodNode mn : cn.methods) {
                if (mn.name.equals(TARGET_METHOD) && mn.desc.equals(TARGET_DESC)) {
                    System.out.println("[EntityPlayerPatcher] Found method: " + mn.name);
                    injectAttackEvent(mn);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("[EntityPlayerPatcher] Method not found, skipping");
                return classBytes;
            }

            System.out.println("[EntityPlayerPatcher] Writing patched class...");
            SafeClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            System.out.println("[EntityPlayerPatcher] Patch complete!");
            return cw.toByteArray();
        } catch (Exception e) {
            System.out.println("[EntityPlayerPatcher] ERROR: " + e.getMessage());
            e.printStackTrace();
            return classBytes;
        }
    }

    /**
     * Inject at HEAD: AttackEventBridge.onAttack(targetEntity)
     * Simple version without try-catch to avoid frame issues
     */
    private static void injectAttackEvent(MethodNode mn) {
        InsnList inject = new InsnList();

        LabelNode skip = new LabelNode();

        // System.getProperties().get("exodar.onAttack")
        inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperties",
            "()Ljava/util/Properties;", false));
        inject.add(new LdcInsnNode("exodar.onAttack"));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Properties", "get",
            "(Ljava/lang/Object;)Ljava/lang/Object;", false));

        // if null, skip
        inject.add(new InsnNode(Opcodes.DUP));
        inject.add(new JumpInsnNode(Opcodes.IFNULL, skip));

        // Cast to Method and invoke with targetEntity (param 1)
        inject.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/reflect/Method"));
        inject.add(new InsnNode(Opcodes.ACONST_NULL));
        inject.add(new InsnNode(Opcodes.ICONST_1));
        inject.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        inject.add(new InsnNode(Opcodes.DUP));
        inject.add(new InsnNode(Opcodes.ICONST_0));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 1)); // targetEntity
        inject.add(new InsnNode(Opcodes.AASTORE));
        inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false));
        inject.add(new InsnNode(Opcodes.POP));
        LabelNode end = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.GOTO, end));

        // skip: pop the null and continue
        inject.add(skip);
        inject.add(new InsnNode(Opcodes.POP));
        inject.add(end);

        // Insert at method start
        AbstractInsnNode firstInsn = mn.instructions.getFirst();
        while (firstInsn instanceof LabelNode || firstInsn instanceof LineNumberNode || firstInsn instanceof FrameNode) {
            firstInsn = firstInsn.getNext();
        }
        mn.instructions.insertBefore(firstInsn, inject);

        System.out.println("[EntityPlayerPatcher] Injected AttackEvent at HEAD");
    }
}
