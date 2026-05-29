package com.github.uright008.pc.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for mods that want to add subcommands under /parallel.
 *
 * Implement this interface and call
 * {@link ParallelCommand#registerSubCommand(ParallelSubCommand)} during
 * your mod's initialization to add your subcommand.
 *
 * <pre>
 * // Example: adding /parallel explosion
 * ParallelCommand.registerSubCommand(new ParallelSubCommand() {
 *     public String getName() { return "explosion"; }
 *     public void build(LiteralArgumentBuilder&lt;CommandSourceStack&gt; builder) {
 *         builder.executes(ctx -&gt; showStatus(ctx))
 *                .then(Commands.argument("enabled", BoolArgumentType.bool())
 *                         .executes(ctx -&gt; setEnabled(ctx)));
 *     }
 *     public String getStatusLine() { return "  Explosion: " + (enabled ? "ON" : "OFF"); }
 * });
 * </pre>
 */
public interface ParallelSubCommand {

    /**
     * The literal name of the subcommand, e.g. "explosion".
     * This becomes: /parallel <getName()>
     */
    String getName();

    /**
     * Build the argument tree for this subcommand.
     * The builder is a literal node with the name already set.
     * Add your arguments and execution callbacks here.
     */
    void build(LiteralArgumentBuilder<CommandSourceStack> builder);

    /**
     * A one-line status string shown in /parallel overview.
     * e.g. "§7  Explosion: §aON"
     */
    String getStatusLine();

    default void executePaper(Consumer<String> output, String[] args) {}

    default List<String> tabCompletePaper(String[] args) {
        return null;
    }
}
