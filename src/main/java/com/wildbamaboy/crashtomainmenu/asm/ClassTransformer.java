package com.wildbamaboy.crashtomainmenu.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Base transform code adapted from https://github.com/Vazkii/Quark/blob/master/src/main/java/vazkii/quark/base/asm/LoadingPlugin.java
 */
@SuppressWarnings("unused")
public class ClassTransformer implements IClassTransformer, Opcodes {

    private static final String ASM_HOOKS = "com/wildbamaboy/crashtomainmenu/asm/ASMHooks";

    private static final Map<String, Transformer> transformers = new HashMap<>();

    static {
        transformers.put("net.minecraft.client.Minecraft", ClassTransformer::transformMinecraft);
        log("Base transform code adapted from https://github.com/Vazkii/Quark/blob/master/src/main/java/vazkii/quark/base/asm/LoadingPlugin.java");
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformers.containsKey(transformedName)) {
            log("Transforming " + transformedName);
            return transformers.get(transformedName).apply(basicClass);
        }

        return basicClass;
    }

    private static byte[] transformMinecraft(byte[] basicClass) {
        MethodSignature sig = new MethodSignature("run", "func_99999_d", "()V");

        return transform(basicClass, forMethod(sig, combine(
                (AbstractInsnNode node) -> { // Filter
                    return node.getOpcode() == ALOAD;
                },
                (MethodNode method, AbstractInsnNode node) -> { // Action
                    InsnList newInstructions = new InsnList();
                    newInstructions.add(new MethodInsnNode(INVOKESTATIC, //Invoke the method.
                            "com/wildbamaboy/crashtomainmenu/asm/ASMHooks",
                            "onMinecraftRun",
                            "()V",
                            false));
                    method.instructions.insertBefore(node, newInstructions);
                    return true;
                })));
    }

    // BOILERPLATE BELOW ==========================================================================================================================================

    private static byte[] transform(byte[] basicClass, TransformerAction... methods) {
        ClassReader reader;
        try {
            reader = new ClassReader(basicClass);
        } catch (NullPointerException ex) {
            return basicClass;
        }

        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        boolean didAnything = false;

        for (TransformerAction pair : methods)
            didAnything |= pair.test(node);

        if (didAnything) {
            ClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            return writer.toByteArray();
        }

        return basicClass;
    }

    public static boolean findMethodAndTransform(ClassNode node, MethodSignature sig, MethodAction predicate) {
        for (MethodNode method : node.methods) {
            if (sig.matches(method)) {
                log("Located method, patching...");

                boolean finish = predicate.test(method);
                log("Patch result: " + finish);

                return finish;
            }
        }

        log("Failed to locate the method!");
        return false;
    }

    public static MethodAction combine(NodeFilter filter, NodeAction action) {
        return (MethodNode node) -> applyOnNode(node, filter, action);
    }

    public static boolean applyOnNode(MethodNode method, NodeFilter filter, NodeAction action) {
        Iterator<AbstractInsnNode> iterator = method.instructions.iterator();

        boolean didAny = false;
        while (iterator.hasNext()) {
            AbstractInsnNode anode = iterator.next();
            if (filter.test(anode)) {
                log("Located patch target node " + getNodeString(anode));
                didAny = true;
                if (action.test(method, anode))
                    break;
            }
        }

        return didAny;
    }

    private static void log(String str) {
        LogManager.getLogger("CrashToMainMenu").info(str);
    }

    private static String getNodeString(ClassNode node) {
        StringWriter sw = new StringWriter();
        PrintWriter printer = new PrintWriter(sw);

        TraceClassVisitor visitor = new TraceClassVisitor(printer);
        node.accept(visitor);

        return sw.toString();
    }


    private static String getNodeString(AbstractInsnNode node) {
        Printer printer = new Textifier();

        TraceMethodVisitor visitor = new TraceMethodVisitor(printer);
        node.accept(visitor);

        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();

        return sw.toString().replaceAll("\n", "").trim();
    }

    private static boolean hasOptifine(String msg) {
        try {
            if (Class.forName("optifine.OptiFineTweaker") != null) {
                log("Optifine Detected. Disabling Patch for " + msg);
                return true;
            }
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static class MethodSignature {
        private final String funcName, srgName, funcDesc;

        public MethodSignature(String funcName, String srgName, String funcDesc) {
            this.funcName = funcName;
            this.srgName = srgName;
            this.funcDesc = funcDesc;
        }

        @Override
        public String toString() {
            return "Names [" + funcName + ", " + srgName + "] Descriptor " + funcDesc;
        }

        public boolean matches(String methodName, String methodDesc) {
            return (methodName.equals(funcName) || methodName.equals(srgName))
                    && (methodDesc.equals(funcDesc));
        }

        public boolean matches(MethodNode method) {
            return matches(method.name, method.desc);
        }

        public boolean matches(MethodInsnNode method) {
            return matches(method.name, method.desc);
        }

        public String mappedName(String owner) {
            return FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, srgName, funcDesc);
        }

    }
    /**
     * Safe class writer.
     * The way COMPUTE_FRAMES works may require loading additional classes. This can cause ClassCircularityErrors.
     * The override for getCommonSuperClass will ensure that COMPUTE_FRAMES works properly by using the right ClassLoader.
     * <p>
     * Code from: https://github.com/JamiesWhiteShirt/clothesline/blob/master/src/core/java/com/jamieswhiteshirt/clothesline/core/SafeClassWriter.java
     */
    public static class SafeClassWriter extends ClassWriter {
        public SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            Class<?> c, d;
            ClassLoader classLoader = Launch.classLoader;
            try {
                c = Class.forName(type1.replace('/', '.'), false, classLoader);
                d = Class.forName(type2.replace('/', '.'), false, classLoader);
            } catch (Exception e) {
                throw new RuntimeException(e.toString());
            }
            if (c.isAssignableFrom(d)) {
                return type1;
            }
            if (d.isAssignableFrom(c)) {
                return type2;
            }
            if (c.isInterface() || d.isInterface()) {
                return "java/lang/Object";
            } else {
                do {
                    c = c.getSuperclass();
                } while (!c.isAssignableFrom(d));
                return c.getName().replace('.', '/');
            }
        }
    }

    // Basic interface aliases to not have to clutter up the code with generics over and over again
    private interface Transformer extends Function<byte[], byte[]> {
        // NO-OP
    }

    private interface MethodAction extends Predicate<MethodNode> {
        // NO-OP
    }

    private interface NodeFilter extends Predicate<AbstractInsnNode> {
        // NO-OP
    }

    private interface NodeAction extends BiPredicate<MethodNode, AbstractInsnNode> {
        // NO-OP
    }

    private interface TransformerAction extends Predicate<ClassNode> {
        // NO-OP
    }

    private interface NewMethodAction extends Predicate<MethodVisitor> {
        // NO-OP
    }

    private static TransformerAction forMethod(MethodSignature sig, MethodAction... actions) {
        return new MethodTransformerAction(sig, actions);
    }

    private static TransformerAction inject(MethodSignature sig, NewMethodAction... actions) {
        return new MethodInjectorAction(sig, actions);
    }

    private static class MethodTransformerAction implements TransformerAction {
        private final MethodSignature sig;
        private final MethodAction[] actions;

        public MethodTransformerAction(MethodSignature sig, MethodAction[] actions) {
            this.sig = sig;
            this.actions = actions;
        }

        @Override
        public boolean test(ClassNode classNode) {
            boolean didAnything = false;
            log("Applying Transformation to method (" + sig + ")");
            for (MethodAction action : actions)
                didAnything |= findMethodAndTransform(classNode, sig, action);
            return didAnything;
        }
    }

    private static class MethodInjectorAction implements TransformerAction {
        private final MethodSignature sig;
        private final NewMethodAction[] actions;

        public MethodInjectorAction(MethodSignature sig, NewMethodAction[] actions) {
            this.sig = sig;
            this.actions = actions;
        }

        @Override
        public boolean test(ClassNode classNode) {
            log("Injecting method (" + sig + ")");

            MethodVisitor method = classNode.visitMethod(ACC_PUBLIC, LoadingPlugin.runtimeDeobfEnabled ? sig.srgName : sig.funcName, sig.funcDesc, null, null);
            for (NewMethodAction action : actions) {
                boolean finish = action.test(method);
                log("Patch result: " + finish);
            }

            return true;
        }
    }
}