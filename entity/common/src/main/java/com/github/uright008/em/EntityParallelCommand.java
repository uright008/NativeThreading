package com.github.uright008.em;

import com.github.uright008.pc.command.ParallelSubCommand;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class EntityParallelCommand implements ParallelSubCommand {
    @Override
    public String getName() { return "entity"; }

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder
            .then(literal("on").executes(ctx -> { EntityParallelConfig.setEnabled(true); return feed(ctx.getSource(), "Entity parallel ticks ON"); }))
            .then(literal("off").executes(ctx -> { EntityParallelConfig.setEnabled(false); return feed(ctx.getSource(), "Entity parallel ticks OFF"); }))
            .then(literal("reload").executes(ctx -> { EntityParallelConfig.reloadConfig(); return feed(ctx.getSource(), "Entity config reloaded"); }))
            .then(literal("timeout").then(argument("seconds", IntegerArgumentType.integer(1, 60))
                    .executes(ctx -> {
                        int s = IntegerArgumentType.getInteger(ctx, "seconds");
                        EntityParallelConfig.setTickTimeoutSeconds(s);
                        return feed(ctx.getSource(), "tickTimeoutSeconds = " + s);
                    })));
    }

    @Override
    public String getStatusLine() {
        return "  Entity: " + (EntityParallelConfig.isEnabled() ? "ON" : "OFF")
                + "  timeout=" + EntityParallelConfig.tickTimeoutSeconds() + "s";
    }

    private static int feed(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }
}
