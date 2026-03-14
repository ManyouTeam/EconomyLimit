package cn.superiormc.economylimit.managers;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.config.PluginSettings;
import cn.superiormc.economylimit.utils.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LanguageManager {

    private static final Pattern LANG_PATTERN = Pattern.compile("\\{lang:([^}]+)}");

    private final EconomyLimitPlugin plugin;
    private final PluginSettings settings;
    private final Map<String, YamlConfiguration> languageFiles = new HashMap<>();
    private YamlConfiguration fallbackLanguage;

    public LanguageManager(EconomyLimitPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        initLanguages();
    }

    public void sendStringText(CommandSender sender, String... args) {
        if (args.length == 0) {
            return;
        }
        TextUtil.sendMessage(sender, getStringText(sender, args[0], trimFirst(args)));
    }

    public String getStringText(CommandSender sender, String path, String... args) {
        String text = getMessage(sender instanceof Player player ? player : null, path);
        if (text == null) {
            text = "Language key not found: " + path;
        }
        for (int i = 0; i + 1 < args.length; i += 2) {
            text = text.replace("{" + args[i] + "}", args[i + 1] == null ? "" : args[i + 1]);
        }
        return resolveText(sender, text.replace("{plugin_folder}", String.valueOf(plugin.getDataFolder())));
    }

    public List<String> getStringListText(CommandSender sender, String path) {
        YamlConfiguration configuration = resolveLanguage(sender instanceof Player player ? player : null);
        List<String> lines = configuration.getStringList(path);
        if (!lines.isEmpty()) {
            return lines;
        }
        return fallbackLanguage == null ? List.of() : fallbackLanguage.getStringList(path);
    }

    public String resolveText(CommandSender sender, String text) {
        return resolveText(sender instanceof Player player ? player : null, text, 0);
    }

    private void initLanguages() {
        File languageFolder = new File(plugin.getDataFolder(), "languages");
        if (!languageFolder.exists() && !languageFolder.mkdirs()) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cCould not create languages folder.");
        }

        saveLanguageResource("en_US.yml");
        saveLanguageResource("zh_CN.yml");
        loadFallbackLanguage();

        File[] files = languageFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            languageFiles.put(name, YamlConfiguration.loadConfiguration(file));
        }
    }

    private void saveLanguageResource(String fileName) {
        File target = new File(plugin.getDataFolder(), "languages/" + fileName);
        if (target.exists()) {
            return;
        }
        plugin.saveResource("languages/" + fileName, false);
    }

    private void loadFallbackLanguage() {
        try (InputStream inputStream = plugin.getResource("languages/en_US.yml")) {
            if (inputStream == null) {
                fallbackLanguage = new YamlConfiguration();
                return;
            }
            File tempFile = File.createTempFile("economylimit-language", ".yml");
            Files.copy(inputStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            fallbackLanguage = YamlConfiguration.loadConfiguration(tempFile);
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        } catch (IOException exception) {
            fallbackLanguage = new YamlConfiguration();
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFailed to load fallback language: " + exception.getMessage());
        }
    }

    private String getMessage(Player player, String path) {
        YamlConfiguration configuration = resolveLanguage(player);
        String text = configuration.getString(path);
        if (text != null) {
            return text;
        }
        return fallbackLanguage == null ? null : fallbackLanguage.getString(path);
    }

    private YamlConfiguration resolveLanguage(Player player) {
        String languageKey = settings.defaultLanguage().toLowerCase(Locale.ROOT);
        if (player != null && settings.perPlayerLanguage()) {
            try {
                languageKey = player.getLocale().replace('-', '_').toLowerCase(Locale.ROOT);
            } catch (Throwable ignored) {
                languageKey = settings.defaultLanguage().toLowerCase(Locale.ROOT);
            }
        }
        return languageFiles.getOrDefault(
                languageKey,
                languageFiles.getOrDefault(settings.defaultLanguage().toLowerCase(Locale.ROOT), fallbackLanguage)
        );
    }

    private String resolveText(Player player, String text, int depth) {
        if (text == null || text.isEmpty() || depth >= 5) {
            return text == null ? "" : text;
        }

        Matcher matcher = LANG_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String key = matcher.group(1).trim();
            String replacement = getMessage(player, key);
            if (replacement == null) {
                replacement = "Language key not found: " + key;
            } else {
                replacement = resolveText(player, replacement, depth + 1);
            }
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(builder);
        return found ? builder.toString() : text;
    }

    private String[] trimFirst(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }
        String[] trimmed = new String[args.length - 1];
        System.arraycopy(args, 1, trimmed, 0, trimmed.length);
        return trimmed;
    }
}
