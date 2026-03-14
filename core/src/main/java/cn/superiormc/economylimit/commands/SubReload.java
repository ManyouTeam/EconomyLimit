package cn.superiormc.economylimit.commands;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SubReload extends AbstractCommand {

    public SubReload(EconomyLimitPlugin plugin) {
        super(plugin, "reload", "economylimit.admin.reload", false, 1);
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        reload(player);
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        reload(null);
    }

    private void reload(CommandSender sender) {
        try {
            plugin.reloadPluginState();
            language().sendStringText(sender, "plugin.reloaded");
        } catch (Exception exception) {
            language().sendStringText(sender, "error.reload-failed", "reason", exception.getMessage());
        }
    }
}
