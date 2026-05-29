package com.github.uright008.pc.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.LEVEL_GAMEMASTERS;

/**
 * Fabric-specific command registration.
 * Registered via CommandRegistrationCallback in ParallelCoreFabric.
 */
public final class ParallelCommandFabric {

    private ParallelCommandFabric() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root =
                LiteralArgumentBuilder.<CommandSourceStack>literal("parallel")
                        .requires(Commands.hasPermission(LEVEL_GAMEMASTERS))
                        .executes(ParallelCommandFabric::showOverview);

        for (ParallelSubCommand sub : ParallelCommand.subCommands().values()) {
            LiteralArgumentBuilder<CommandSourceStack> node = LiteralArgumentBuilder.literal(sub.getName());
            sub.build(node);
            root.then(node);
        }

        dispatcher.register(root);

        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("pc")
                        .requires(Commands.hasPermission(LEVEL_GAMEMASTERS))
                        .executes(ParallelCommandFabric::showOverview)
        );
    }

    private static int showOverview(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder("§6--- Parallel Systems ---\n");
        if (ParallelCommand.subCommands().isEmpty()) {
            sb.append("§7  (no subsystems registered)\n");
        } else {
            for (ParallelSubCommand sub : ParallelCommand.subCommands().values()) {
                sb.append(sub.getStatusLine()).append("\n");
            }
        }
        sb.append("§7Usage: /parallel <subsystem>  |  /pc");
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }
}
