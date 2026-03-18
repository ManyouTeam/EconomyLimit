package cn.superiormc.economylimit.spigot;

import cn.superiormc.economylimit.utils.SpecialMethodUtil;
import cn.superiormc.economylimit.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public final class SpigotMethodUtil implements SpecialMethodUtil {

    @Override
    public String methodID() {
        return "spigot";
    }

    @Override
    public void sendChat(Player player, String text) {
        String parsed = TextUtil.colorize(text);
        if (player == null) {
            Bukkit.getConsoleSender().sendMessage(parsed);
            return;
        }
        player.sendMessage(parsed);
    }

    @Override
    public void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            return;
        }
        player.sendTitle(TextUtil.parse(title), TextUtil.parse(subTitle), fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionBar(Player player, String text) {
        if (player == null) {
            return;
        }
        player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(TextUtil.parse(text))
        );
    }

    @Override
    public void sendBossBar(Player player, String title, float progress, String color, String style) {
        if (player == null) {
            return;
        }

        BarColor barColor;
        try {
            barColor = color == null ? BarColor.WHITE : BarColor.valueOf(color.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            barColor = BarColor.WHITE;
        }

        BarStyle barStyle;
        try {
            barStyle = style == null ? BarStyle.SOLID : BarStyle.valueOf(style.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            barStyle = BarStyle.SOLID;
        }

        BossBar bossBar = Bukkit.createBossBar(TextUtil.parse(title), barColor, barStyle);
        bossBar.setProgress(Math.max(0D, Math.min(1D, progress)));
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        Bukkit.getScheduler().runTaskLater(cn.superiormc.economylimit.EconomyLimitPlugin.getInstance(), bossBar::removeAll, 60L);
    }

    @Override
    public String legacyParse(String text) {
        return TextUtil.colorize(text);
    }
}
