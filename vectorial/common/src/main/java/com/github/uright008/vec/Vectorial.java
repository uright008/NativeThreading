package com.github.uright008.vec;

import java.io.File;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.minecraft.world.entity.Entity;
import com.github.uright008.vec.core.VectorialAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Vectorial {

    public static final String MOD_ID = "vectorial";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private Vectorial() {}

    public static void init() {
        LOGGER.info("Vectorial init — attempting agent attachment");
        try {
            attachAgent();
            retransformEntity();
        } catch (Exception e) {
            LOGGER.warn("Vectorial agent attachment failed — SoA disabled", e);
        }
    }

    private static void attachAgent() throws Exception {
        File jarFile = new File(Vectorial.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());

        if (!jarFile.isFile() || !jarFile.getName().endsWith(".jar")) {
            LOGGER.warn("Not running from a JAR — SoA disabled");
            return;
        }

        LOGGER.info("Attaching agent from: {}", jarFile.getAbsolutePath());
        ByteBuddyAgent.attach(jarFile, String.valueOf(ProcessHandle.current().pid()));
    }

    private static void retransformEntity() {
        Instrumentation inst = VectorialAgent.getInstrumentation();
        if (inst != null && inst.isRetransformClassesSupported()) {
            try {
                inst.retransformClasses(Entity.class);
                LOGGER.info("Vectorial: retransformed Entity class");
            } catch (Exception e) {
                LOGGER.warn("Retransform Entity failed", e);
            }
        }
    }
}
