package cn.superiormc.economylimit.paper;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.paper.utils.PaperTextUtil;
import cn.superiormc.economylimit.utils.SpecialMethodUtil;
import cn.superiormc.economylimit.utils.TextUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class PaperMethodUtil implements SpecialMethodUtil {

    @Override
    public String methodID() {
        return "paper";
    }

    @Override
    public void sendChat(Player player, String text) {
        if (player == null) {
            Bukkit.getConsoleSender().sendMessage(PaperTextUtil.toLegacy(text));
            return;
        }
        player.sendMessage(PaperTextUtil.modernParse(text));
    }

    @Override
    public void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        player.showTitle(Title.title(
                PaperTextUtil.modernParse(title),
                PaperTextUtil.modernParse(subTitle),
                Title.Times.times(
                        Duration.ofMillis(Math.max(0L, fadeIn) * 50L),
                        Duration.ofMillis(Math.max(0L, stay) * 50L),
                        Duration.ofMillis(Math.max(0L, fadeOut) * 50L)
                )
        ));
    }

    @Override
    public void sendActionBar(Player player, String text) {
        if (player == null) {
            return;
        }
        player.sendActionBar(PaperTextUtil.modernParse(text));
    }

    @Override
    public void sendBossBar(Player player, String title, float progress, String color, String style) {
        if (player == null) {
            return;
        }

        BossBar.Color barColor;
        try {
            barColor = color == null ? BossBar.Color.WHITE : BossBar.Color.valueOf(color.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            barColor = BossBar.Color.WHITE;
        }

        String resolvedStyle = style;
        if (resolvedStyle != null && resolvedStyle.equalsIgnoreCase("SOLID")) {
            resolvedStyle = "PROGRESS";
        }

        BossBar.Overlay overlay;
        try {
            overlay = resolvedStyle == null ? BossBar.Overlay.PROGRESS : BossBar.Overlay.valueOf(resolvedStyle.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        BossBar bossBar = BossBar.bossBar(
                title == null ? Component.empty() : PaperTextUtil.modernParse(title),
                Math.max(0F, Math.min(1F, progress)),
                barColor,
                overlay
        );
        player.showBossBar(bossBar);
        Bukkit.getScheduler().runTaskLater(EconomyLimitPlugin.getInstance(), () -> player.hideBossBar(bossBar), 60L);
    }

    @Override
    public String legacyParse(String text) {
        EconomyLimitPlugin plugin = EconomyLimitPlugin.getInstance();
        if (plugin != null && !plugin.getConfig().getBoolean("config-files.force-parse-mini-message", true)) {
            return TextUtil.colorize(text);
        }
        return PaperTextUtil.toLegacy(text == null ? "" : text);
    }
}
