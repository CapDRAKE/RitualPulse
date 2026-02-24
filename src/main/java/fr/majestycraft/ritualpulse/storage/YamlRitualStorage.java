package fr.majestycraft.ritualpulse.storage;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.model.PlayerRitualData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class YamlRitualStorage implements RitualStorage {

    private final RitualPulsePlugin plugin;
    private File file;
    private YamlConfiguration yaml;

    public YamlRitualStorage(RitualPulsePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() throws Exception {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        String fileName = plugin.getConfig().getString("storage.yaml-file", "data.yml");
        this.file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public PlayerRitualData load(UUID uuid) {
        String base = "players." + uuid;
        PlayerRitualData d = new PlayerRitualData(uuid);
        d.setLastClaimAt(yaml.getLong(base + ".lastClaimAt", 0L));
        d.setStreak(yaml.getInt(base + ".streak", 0));
        d.setFreezeTokens(yaml.getInt(base + ".freezeTokens", plugin.getConfig().getInt("streak.freeze.start-tokens", 0)));
        d.setTotalClaims(yaml.getInt(base + ".totalClaims", 0));
        return d;
    }

    @Override
    public void save(PlayerRitualData d) {
        String base = "players." + d.getUuid();
        yaml.set(base + ".lastClaimAt", d.getLastClaimAt());
        yaml.set(base + ".streak", d.getStreak());
        yaml.set(base + ".freezeTokens", d.getFreezeTokens());
        yaml.set(base + ".totalClaims", d.getTotalClaims());
        flush();
    }

    @Override
    public void saveAll(Map<UUID, PlayerRitualData> cache) {
        for (PlayerRitualData d : cache.values()) {
            String base = "players." + d.getUuid();
            yaml.set(base + ".lastClaimAt", d.getLastClaimAt());
            yaml.set(base + ".streak", d.getStreak());
            yaml.set(base + ".freezeTokens", d.getFreezeTokens());
            yaml.set(base + ".totalClaims", d.getTotalClaims());
        }
        flush();
    }

    private void flush() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder le fichier YAML du plugin");
            e.printStackTrace();
            throw new RuntimeException("yaml save failed", e);
        }
    }

    @Override
    public void close() {
        flush();
    }

    @Override
    public String getName() {
        return "YAML";
    }
}
