package studio.itsmy.itsmydata.commands.subcommands;

import org.bukkit.command.CommandSender;

public interface DataSubcommand {

    String name();

    String permission();

    String usage();

    String descriptionKey();

    boolean execute(CommandSender sender, String label, String[] args);
}
