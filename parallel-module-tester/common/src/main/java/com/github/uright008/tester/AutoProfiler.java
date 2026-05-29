package com.github.uright008.tester;

import com.github.uright008.pc.ServerHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class AutoProfiler {

    private static final Logger LOG = LoggerFactory.getLogger("mc-parallel:profiler");

    private AutoProfiler() {}

    public static void register() {
        if (!ProfileConfig.isAutoProfile()) return;
        ServerLifecycleEvents.SERVER_STARTED.register(AutoProfiler::onServerStarted);
        LOG.info("Auto-profile registered");
    }

    private static void onServerStarted(MinecraftServer server) {
        int warmup = ProfileConfig.warmupSeconds();
        int record = ProfileConfig.recordSeconds();
        int timeout = StressTestConfig.timeoutSeconds();
        LOG.info("Auto-profile: warmup {}s, record {}s, timeout {}s {}", warmup, record, timeout, timeout == 0 ? "(infinite)" : "");

        Thread profileThread = new Thread(() -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(warmup));
                LOG.info(ServerHelper.msptLog(server, "pre-record"));

                if (timeout > 0) {
                    ServerHelper.runCommand(server, "spark profiler start --timeout " + record + " --thread *");
                    Thread.sleep(TimeUnit.SECONDS.toMillis(record + 2));
                    LOG.info(ServerHelper.msptLog(server, "post-record"));
                    LOG.info("Auto-profile: shutting down");
                    server.halt(false);
                } else {
                    // timeout=0: log MSPT periodically, never stop spark, never shutdown
                    while (true) {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(record));
                        LOG.info(ServerHelper.msptLog(server, "periodic"));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.error("Auto-profile failed", e);
                if (timeout > 0) server.halt(false);
            }
        }, "profiler-auto-profile");
        profileThread.setDaemon(true);
        profileThread.start();
    }
}
