package cn.superiormc.economylimit.commands;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MainCommand implements CommandExecutor {

    private final EconomyLimitPlugin plugin;

    public MainCommand(EconomyLimitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CommandManager commandManager = plugin.getCommandManager();
        if (args.length == 0) {
            if (sender instanceof Player player) {
                commandManager.getSubCommandsMap().get("status").executeCommandInGame(new String[]{"status"}, player);
            } else {
                for (String line : plugin.getLanguageManager().getStringListText(sender, "command.help-console")) {
                    plugin.getLanguageManager().sendStringText(sender, "command.raw", "text", line);
                }
            }
            return true;
        }

        AbstractCommand subCommand = commandManager.getSubCommandsMap().get(args[0].toLowerCase());
        if (subCommand == null) {
            plugin.getLanguageManager().sendStringText(sender, "error.args");
            return true;
        }

        if (subCommand.getOnlyInGame() && !(sender instanceof Player)) {
            plugin.getLanguageManager().sendStringText(sender, "error.in-game");
            return true;
        }

        if (subCommand.getRequiredPermission() != null && !subCommand.getRequiredPermission().isEmpty()
                && !sender.hasPermission(subCommand.getRequiredPermission())) {
            plugin.getLanguageManager().sendStringText(sender, "error.miss-permission");
            return true;
        }

        if (!subCommand.getLengthCorrect(args.length, sender)) {
            plugin.getLanguageManager().sendStringText(sender, "error.args");
            return true;
        }

        if (sender instanceof Player player) {
            subCommand.executeCommandInGame(args, player);
        } else {
            subCommand.executeCommandInConsole(args);
        }
        return true;
    }
}
