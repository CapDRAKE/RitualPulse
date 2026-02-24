package fr.majestycraft.ritualpulse.service;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class LangManager {

    private final RitualPulsePlugin plugin;
    private YamlConfiguration lang;
    private String currentLang;

    public LangManager(RitualPulsePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.currentLang = plugin.getConfig().getString("lang", "fr").toLowerCase();
        String resName = currentLang.equals("en") ? "lang_en.yml" : "lang_fr.yml";

        try {
            File out = new File(plugin.getDataFolder(), resName);
            if (!out.exists()) {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                try (InputStream in = plugin.getResource(resName)) {
                    if (in != null) {
                        Files.copy(in, out.toPath());
                    }
                }
            }
            this.lang = YamlConfiguration.loadConfiguration(out);
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur chargement langue, fallback interne");
            this.lang = new YamlConfiguration();
        }
    }

    public String get(String path) {
        return color(lang.getString(path, path));
    }

    public String prefixed(String path) {
        return color(lang.getString("prefix", "&6[RitualPulse] &r") + lang.getString(path, path));
    }

    public String color(String in) {
        return ChatColor.translateAlternateColorCodes('&', in == null ? "" : in);
    }

    public String getCurrentLang() {
        return currentLang;
    }
}
