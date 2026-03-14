package cn.superiormc.economylimit.commands;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class SubStatus extends AbstractCommand {

    public SubStatus(EconomyLimitPlugin plugin) {
        super(plugin, "status", null, false, 1, 2);
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        if (args.length == 1) {
            sendStatus(player, player);
            return;
        }
        if (!player.hasPermission("economylimit.admin.status")) {
            language().sendStringText(player, "error.miss-permission");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        sendStatus(player, target);
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        if (args.length < 2) {
            language().sendStringText((CommandSender) null, "error.args");
            return;
        }
        sendStatus(null, Bukkit.getOfflinePlayer(args[1]));
    }

    @Override
    public List<String> getTabResult(String[] args, Player player) {
        if (args.length == 2 && player.hasPermission("economylimit.admin.status")) {
            List<String> results = new ArrayList<>();
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                results.add(onlinePlayer.getName());
            }
            return results;
        }
        return List.of();
    }

    private void sendStatus(CommandSender sender, OfflinePlayer target) {
        language().sendStringText(sender, "status.title", "player", target.getName() == null ? target.getUniqueId().toString() : target.getName());
        for (String line : plugin.getEarningLimitService().buildStatusLines(sender, target)) {
            language().sendStringText(sender, "command.raw", "text", line);
        }
    }
}
