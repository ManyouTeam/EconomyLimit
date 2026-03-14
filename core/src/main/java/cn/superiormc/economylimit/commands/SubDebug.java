package cn.superiormc.economylimit.commands;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.inject.VaultDepositBridge;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SubDebug extends AbstractCommand {

    public SubDebug(EconomyLimitPlugin plugin) {
        super(plugin, "debug", "economylimit.admin.debug", false, 1);
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        debug(player);
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        debug(null);
    }

    private void debug(CommandSender sender) {
        language().sendStringText(sender, "debug.title");
        language().sendStringText(sender, "debug.provider", "value",
                plugin.getEconomy() == null ? "null" : plugin.getEconomy().getClass().getName());
        language().sendStringText(sender, "debug.injection-status", "value",
                plugin.getVaultInjectionManager() == null ? "null" : plugin.getVaultInjectionManager().getLastInjectionStatus());
        language().sendStringText(sender, "debug.bridge-hits", "value", String.valueOf(VaultDepositBridge.getHitCount()));
        language().sendStringText(sender, "debug.last-hit", "value", VaultDepositBridge.getLastHitSummary());
        language().sendStringText(sender, "debug.last-error", "value", VaultDepositBridge.getLastError());
    }
}
