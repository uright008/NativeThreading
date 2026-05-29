package com.github.uright008.vec.core;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public final class VectorialAgent {

    private static volatile Instrumentation instrumentation;

    private VectorialAgent() {}

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[vectorial] agent loaded via -javaagent");
        init(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[vectorial] agent attached dynamically");
        init(inst);
    }

    private static synchronized void init(Instrumentation inst) {
        if (instrumentation != null) return;
        instrumentation = inst;

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                byte[] result = VectorialTransformer.transform(className, classfileBuffer);
                return result != null ? result : classfileBuffer;
            }

            @Override
            public byte[] transform(Module module, ClassLoader loader, String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) {
                return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            }
        });
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
