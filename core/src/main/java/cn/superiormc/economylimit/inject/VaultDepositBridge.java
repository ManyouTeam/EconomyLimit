package cn.superiormc.economylimit.inject;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.service.DepositContext;
import cn.superiormc.economylimit.service.EarningLimitService;
import cn.superiormc.economylimit.utils.TextUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class VaultDepositBridge {

    private static final ThreadLocal<DepositContext> ACTIVE_CONTEXT = new ThreadLocal<>();
    private static volatile long hitCount = 0L;
    private static volatile String lastHitSummary = "none";
    private static volatile String lastError = "none";

    private VaultDepositBridge() {
    }

    public static Object beforeDeposit(Object[] args) {
        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (plugin == null) {
            return null;
        }

        OfflinePlayer player = resolvePlayer(args);
        Double amount = resolveAmount(args);
        if (player == null || amount == null || amount <= 0D) {
            return null;
        }

        hitCount++;
        lastHitSummary = "player=" + (player.getName() == null ? player.getUniqueId() : player.getName()) + ", amount=" + amount;

        EarningLimitService service = plugin.getEarningLimitService();
        DepositContext context = service.prepareDeposit(player, amount);
        if (context == null) {
            return null;
        }

        if (context.shouldSkipOriginal()) {
            ACTIVE_CONTEXT.remove();
            service.completeSkippedDeposit(context);
            EconomyResponse response = new EconomyResponse(0D, 0D, EconomyResponse.ResponseType.SUCCESS,
                    "Stored in EconomyLimit virtual bank.");
            return new Object[]{Boolean.TRUE, response, 0D};
        }

        ACTIVE_CONTEXT.set(context);
        return new Object[]{Boolean.FALSE, null, context.directAmount()};
    }

    public static void afterCurrent(Object response, Throwable throwable) {
        DepositContext depositContext = ACTIVE_CONTEXT.get();
        ACTIVE_CONTEXT.remove();
        if (depositContext == null) {
            return;
        }
        if (throwable != null) {
            return;
        }
        if (response instanceof EconomyResponse economyResponse) {
            EconomyLimitPlugin.getInstance().getEarningLimitService().completeDeposit(depositContext, economyResponse);
        }
    }

    public static void recordError(Throwable throwable) {
        if (throwable == null) {
            return;
        }
        lastError = throwable.getClass().getName() + ": " + throwable.getMessage();
        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (plugin != null) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cEconomyLimit injection bridge error: " + lastError);
        }
    }

    public static long getHitCount() {
        return hitCount;
    }

    public static String getLastHitSummary() {
        return lastHitSummary;
    }

    public static String getLastError() {
        return lastError;
    }

    private static OfflinePlayer resolvePlayer(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof OfflinePlayer offlinePlayer) {
                return offlinePlayer;
            }
            if (arg instanceof String playerName) {
                return Bukkit.getOfflinePlayer(playerName);
            }
        }
        return null;
    }

    private static Double resolveAmount(Object[] args) {
        if (args.length == 0) {
            return null;
        }
        Object amount = args[args.length - 1];
        if (amount instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
