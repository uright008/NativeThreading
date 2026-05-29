package com.github.uright008.tester;

import com.github.uright008.pc.command.ParallelCommand;
import com.github.uright008.pc.command.ParallelSubCommand;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ProfilerCommand implements ParallelSubCommand {

    @Override public String getName() { return "profiler"; }

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder
                .executes(this::showStatus)
                .then(Commands.literal("start")
                        .executes(ctx -> start(ctx, 30))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "seconds")))))
                .then(Commands.literal("stop").executes(this::stop));
    }

    @Override
    public String getStatusLine() {
        return "\u00a77  Profiler:  " + (ProfilerMonitor.isRecording() ? "\u00a7aREC" : "\u00a77off");
    }

    private int showStatus(CommandContext<CommandSourceStack> ctx) {
        String msg = "\u00a7e/parallel profiler\n"
                + "\u00a77  Status: " + (ProfilerMonitor.isRecording() ? "\u00a7aRECORDING" : "\u00a77idle") + "\n"
                + "\u00a77Usage: /parallel profiler [start [seconds] | stop]";
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private int start(CommandContext<CommandSourceStack> ctx, int seconds) {
        ProfilerMonitor.autoRecord(seconds);
        ctx.getSource().sendSuccess(
                () -> Component.literal("\u00a7aProfiler recording started (" + seconds + "s) \u2192 config/spark/"), true);
        return 1;
    }

    private int stop(CommandContext<CommandSourceStack> ctx) {
        ProfilerMonitor.stop();
        ctx.getSource().sendSuccess(
                () -> Component.literal("\u00a7aProfiler recording saved to config/spark/"), true);
        return 1;
    }
}
