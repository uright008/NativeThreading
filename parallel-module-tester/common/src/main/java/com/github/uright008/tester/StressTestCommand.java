package com.github.uright008.tester;

import com.github.uright008.pc.command.ParallelSubCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class StressTestCommand implements ParallelSubCommand {
    @Override
    public String getName() { return "stresstest"; }

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder
            .then(literal("mode").then(argument("mode", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        sb.suggest("tnt").suggest("hopper").suggest("entity")
                                .suggest("explosion").suggest("benchmark").suggest("off");
                        return sb.buildFuture();
                    })
                    .executes(ctx -> {
                        String mode = StringArgumentType.getString(ctx, "mode");
                        StressTestConfig.setMode(mode.equals("off") ? "" : mode);
                        return feed(ctx.getSource(), "mode = " + (mode.equals("off") ? "OFF" : mode));
                    })))
            .then(literal("timeout").then(argument("seconds", IntegerArgumentType.integer(0, 3600))
                    .executes(ctx -> {
                        int s = IntegerArgumentType.getInteger(ctx, "seconds");
                        StressTestConfig.setTimeout(s);
                        return feed(ctx.getSource(), "timeout = " + s + "s" + (s == 0 ? " (infinite, no spark)" : ""));
                    })))
            .then(literal("chunks").then(argument("r", IntegerArgumentType.integer(1, 16))
                    .executes(ctx -> {
                        int r = IntegerArgumentType.getInteger(ctx, "r");
                        StressTestConfig.setChunkRadius(r);
                        return feed(ctx.getSource(), "chunkRadius = " + r + " (" + (r*2)*(r*2) + " chunks)");
                    })))
            .then(literal("tntSize").then(argument("n", IntegerArgumentType.integer(1, 5))
                    .executes(ctx -> {
                        int n = IntegerArgumentType.getInteger(ctx, "n");
                        StressTestConfig.setTntSize(n);
                        return feed(ctx.getSource(), "tntSize = " + n + " (" + (n*3) + " total layers)");
                    })))
            .then(literal("spawnRate").then(argument("n", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> {
                        int n = IntegerArgumentType.getInteger(ctx, "n");
                        StressTestConfig.setEntitySpawnRate(n);
                        return feed(ctx.getSource(), "entitySpawnRate = " + n + "/tick (" + (n*20) + "/s)");
                    })))
            .then(literal("start").then(argument("mode", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        sb.suggest("tnt").suggest("hopper").suggest("entity")
                                .suggest("explosion").suggest("benchmark");
                        return sb.buildFuture();
                    })
                    .executes(ctx -> {
                        String mode = StringArgumentType.getString(ctx, "mode");
                        StressTestConfig.setMode(mode);
                        return feed(ctx.getSource(), "started: " + mode);
                    })
                    .then(argument("timeout", IntegerArgumentType.integer(0, 3600))
                            .executes(ctx -> {
                                String mode = StringArgumentType.getString(ctx, "mode");
                                int s = IntegerArgumentType.getInteger(ctx, "timeout");
                                StressTestConfig.setMode(mode);
                                StressTestConfig.setTimeout(s);
                                return feed(ctx.getSource(), "started: " + mode + " timeout=" + s + "s");
                            }))))
            .then(literal("stop").executes(ctx -> {
                StressTestConfig.setMode("");
                return feed(ctx.getSource(), "stopped");
            }));
    }

    @Override
    public String getStatusLine() {
        String mode = StressTestConfig.getMode();
        return "  StressTest: " + (mode.isEmpty() ? "OFF" : mode.toUpperCase())
                + "  timeout=" + StressTestConfig.timeoutSeconds() + "s"
                + "  chunks=" + StressTestConfig.chunkRadius();
    }

    private static int feed(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }
}
