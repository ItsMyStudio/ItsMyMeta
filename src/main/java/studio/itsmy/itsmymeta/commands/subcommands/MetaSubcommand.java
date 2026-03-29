package studio.itsmy.itsmymeta.commands.subcommands;

import org.bukkit.command.CommandSender;

public interface MetaSubcommand {

    String name();

    String permission();

    String usage();

    String descriptionKey();

    boolean execute(CommandSender sender, String label, String[] args);
}
