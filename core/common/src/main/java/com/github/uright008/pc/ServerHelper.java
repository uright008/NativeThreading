package com.github.uright008.pc;

import com.mojang.brigadier.StringReader;
import net.minecraft.server.MinecraftServer;

/**
 * Shared server utilities used across stress tests and auto-profiling.
 */
public final class ServerHelper {

    private ServerHelper() {}

    public static void runCommand(MinecraftServer server, String cmd) {
        var source = server.createCommandSourceStack().withSuppressedOutput();
        var parse = server.getCommands().getDispatcher()
                .parse(new StringReader(cmd), source);
        server.getCommands().performCommand(parse, cmd);
    }

    public static String msptLog(MinecraftServer server, String tag) {
        long avgNs = server.getAverageTickTimeNanos();
        int tickCount = server.getTickCount();
        double avgMs = avgNs / 1_000_000.0;
        double tps = avgMs > 0 ? 1000.0 / avgMs : 20.0;
        return String.format("MSPT[%s]: %d ticks, avg %.1f ms/tick, %.1f TPS",
                tag, tickCount, avgMs, tps);
    }

    public static void forceload(MinecraftServer server, int chunkRadius) {
        runCommand(server, "forceload remove all");
        runCommand(server, "forceload add -" + chunkRadius + " -" + chunkRadius
                + " " + (chunkRadius - 1) + " " + (chunkRadius - 1));
    }
}
