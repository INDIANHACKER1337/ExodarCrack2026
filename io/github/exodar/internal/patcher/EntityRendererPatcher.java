/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.internal.patcher;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Patches EntityRenderer to inject render event callbacks
 * Target: net.minecraft.client.renderer.EntityRenderer
 *
 * COPIED FROM GHOSTCLIENT - Uses EXPAND_FRAMES + COMPUTE_FRAMES + SafeClassWriter
 */
public class EntityRendererPatcher {

    private static final String TARGET_CLASS = "net/minecraft/client/renderer/EntityRenderer";

    public static boolean shouldPatch(String className) {
        return TARGET_CLASS.equals(className);
    }

    public static byte[] patch(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("renderWorldPass") && mn.desc.equals("(IFJ)V")) {
                patchRenderWorldPass(mn);
            } else if (mn.name.equals("updateCameraAndRender") && mn.desc.equals("(FJ)V")) {
                patchUpdateCameraAndRender(mn);
            } else if (mn.name.equals("getMouseOver") && mn.desc.equals("(F)V")) {
                patchGetMouseOver(mn, cn.name);
            }
        }

        SafeClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /**
     * Patch renderWorldPass - inject render3D event BEFORE endStartSection("hand")
     */
    private static void patchRenderWorldPass(MethodNode mn) {
        System.out.println("[Patcher] Patching renderWorldPass with Tree API");

        // Find: LDC "hand" followed by INVOKEVIRTUAL endStartSection
        // Inject BEFORE the ALOAD 0 that starts this sequence
        AbstractInsnNode targetInsn = null;

        for (int i = 0; i < mn.instructions.size() - 4; i++) {
            AbstractInsnNode insn = mn.instructions.get(i);

            // Look for LDC "hand"
            if (insn.getOpcode() == Opcodes.LDC && insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if ("hand".equals(ldc.cst)) {
                    // Found LDC "hand", now find the ALOAD 0 that starts this sequence
                    // Go backwards to find ALOAD 0
                    AbstractInsnNode prev = insn.getPrevious();
                    while (prev != null) {
                        // Skip labels, line numbers, frames
                        if (prev instanceof LabelNode || prev instanceof LineNumberNode || prev instanceof FrameNode) {
                            prev = prev.getPrevious();
                            continue;
                        }
                        // Check for GETFIELD (part of the pattern)
                        if (prev.getOpcode() == Opcodes.GETFIELD) {
                            prev = prev.getPrevious();
                            continue;
                        }
                        // Found ALOAD 0 - this is where we inject
                        if (prev.getOpcode() == Opcodes.ALOAD && prev instanceof VarInsnNode) {
                            VarInsnNode varInsn = (VarInsnNode) prev;
                            if (varInsn.var == 0) {
                                targetInsn = prev;
                                System.out.println("[Patcher] Found injection point before 'hand' section");
                                break;
                            }
                        }
                        break; // Not the pattern we're looking for
                    }
                    break;
                }
            }
        }

        if (targetInsn != null) {
            // Build injection instructions
            LabelNode[] labels = new LabelNode[4];
            InsnList inject = buildInjectionInsns("exodar.render3d", 2, labels);
            mn.instructions.insertBefore(targetInsn, inject);
            // Add try-catch to exception table
            mn.tryCatchBlocks.add(new TryCatchBlockNode(labels[0], labels[1], labels[2], "java/lang/Throwable"));
            System.out.println("[Patcher] Injected render3D event before 'hand' section");
        } else {
            System.out.println("[Patcher] WARNING: Could not find 'hand' section, injecting before RETURN");
            // Fallback: inject before RETURN
            for (int i = mn.instructions.size() - 1; i >= 0; i--) {
                AbstractInsnNode insn = mn.instructions.get(i);
                if (insn.getOpcode() == Opcodes.RETURN) {
                    LabelNode[] labels = new LabelNode[4];
                    InsnList inject = buildInjectionInsns("exodar.render3d", 2, labels);
                    mn.instructions.insertBefore(insn, inject);
                    mn.tryCatchBlocks.add(new TryCatchBlockNode(labels[0], labels[1], labels[2], "java/lang/Throwable"));
                    break;
                }
            }
        }
    }

    /**
     * Patch updateCameraAndRender - inject render2D event before RETURN
     */
    private static void patchUpdateCameraAndRender(MethodNode mn) {
        System.out.println("[Patcher] Patching updateCameraAndRender");

        for (int i = mn.instructions.size() - 1; i >= 0; i--) {
            AbstractInsnNode insn = mn.instructions.get(i);
            if (insn.getOpcode() == Opcodes.RETURN) {
                LabelNode[] labels = new LabelNode[4];
                InsnList inject = buildInjectionInsns("exodar.render2d", 1, labels);
                mn.instructions.insertBefore(insn, inject);
                mn.tryCatchBlocks.add(new TryCatchBlockNode(labels[0], labels[1], labels[2], "java/lang/Throwable"));
                System.out.println("[Patcher] Injected render2D event before RETURN");
                break;
            }
        }
    }

    /**
     * Patch getMouseOver - inject hook at end for Reach/Penetration
     * This allows modules to extend reach and hit through blocks
     */
    private static void patchGetMouseOver(MethodNode mn, String className) {
        System.out.println("[Patcher] Patching getMouseOver for Reach/Penetration hook");

        // Find RETURN instruction and inject before it
        for (int i = mn.instructions.size() - 1; i >= 0; i--) {
            AbstractInsnNode insn = mn.instructions.get(i);
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList inject = new InsnList();

                LabelNode tryStart = new LabelNode();
                LabelNode tryEnd = new LabelNode();
                LabelNode catchStart = new LabelNode();
                LabelNode catchEnd = new LabelNode();

                inject.add(tryStart);

                // Call Main.onGetMouseOver(thrower, this, partialTicks)
                // Get Thrower from System.getProperties()
                inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperties",
                    "()Ljava/util/Properties;", false));
                inject.add(new LdcInsnNode("exodar.thrower"));
                inject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Properties", "get",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", false));

                // Check if null
                inject.add(new InsnNode(Opcodes.DUP));
                LabelNode notNull = new LabelNode();
                inject.add(new JumpInsnNode(Opcodes.IFNONNULL, notNull));
                inject.add(new InsnNode(Opcodes.POP));
                inject.add(new JumpInsnNode(Opcodes.GOTO, catchEnd));

                inject.add(notNull);
                // Cast to Thrower
                inject.add(new TypeInsnNode(Opcodes.CHECKCAST, "io/github/exodar/internal/Thrower"));

                // Load 'this' (EntityRenderer)
                inject.add(new VarInsnNode(Opcodes.ALOAD, 0));

                // Load partialTicks (parameter 1)
                inject.add(new VarInsnNode(Opcodes.FLOAD, 1));

                // Call Main.onGetMouseOver(Thrower, EntityRenderer, float)
                inject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/exodar/Main", "onGetMouseOver",
                    "(Lio/github/exodar/internal/Thrower;Lnet/minecraft/client/renderer/EntityRenderer;F)V", false));

                inject.add(tryEnd);
                inject.add(new JumpInsnNode(Opcodes.GOTO, catchEnd));

                inject.add(catchStart);
                inject.add(new InsnNode(Opcodes.POP)); // Discard exception

                inject.add(catchEnd);

                mn.instructions.insertBefore(insn, inject);
                mn.tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchStart, "java/lang/Throwable"));

                System.out.println("[Patcher] Injected getMouseOver hook for Reach/Penetration");
                break;
            }
        }
    }

    /**
     * Build the injection instructions using Tree API
     * @param labels output array: [tryStart, tryEnd, catchStart, catchEnd]
     */
    private static InsnList buildInjectionInsns(String propertyKey, int partialTicksVarIndex, LabelNode[] labels) {
        InsnList list = new InsnList();

        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode catchStart = new LabelNode();
        LabelNode catchEnd = new LabelNode();

        labels[0] = tryStart;
        labels[1] = tryEnd;
        labels[2] = catchStart;
        labels[3] = catchEnd;

        list.add(tryStart);

        // Get the Method from System.getProperties().get(propertyKey)
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "getProperties",
            "()Ljava/util/Properties;", false));
        list.add(new LdcInsnNode(propertyKey));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Properties", "get",
            "(Ljava/lang/Object;)Ljava/lang/Object;", false));

        // Check if null
        list.add(new InsnNode(Opcodes.DUP));
        LabelNode notNull = new LabelNode();
        list.add(new JumpInsnNode(Opcodes.IFNONNULL, notNull));
        list.add(new InsnNode(Opcodes.POP));
        list.add(new JumpInsnNode(Opcodes.GOTO, catchEnd));

        list.add(notNull);
        // Cast to Method
        list.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/reflect/Method"));

        // Invoke: method.invoke(null, Float.valueOf(partialTicks))
        list.add(new InsnNode(Opcodes.ACONST_NULL));
        list.add(new InsnNode(Opcodes.ICONST_1));
        list.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new InsnNode(Opcodes.ICONST_0));
        list.add(new VarInsnNode(Opcodes.FLOAD, partialTicksVarIndex));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf",
            "(F)Ljava/lang/Float;", false));
        list.add(new InsnNode(Opcodes.AASTORE));
        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false));
        list.add(new InsnNode(Opcodes.POP)); // Discard return value

        list.add(tryEnd);
        list.add(new JumpInsnNode(Opcodes.GOTO, catchEnd));

        list.add(catchStart);
        list.add(new InsnNode(Opcodes.POP)); // Discard exception silently

        list.add(catchEnd);

        // Add try-catch block to method's exception table (will be handled by MethodNode)
        // We need to add this separately in the patch method

        return list;
    }
}
