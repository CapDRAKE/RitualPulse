package fr.majestycraft.ritualpulse.service;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.util.ConfigUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalTime;
import java.util.*;

public final class ConfigDoctor {

    private ConfigDoctor() {}

    public static List<String> validate(RitualPulsePlugin plugin) {
        List<String> warnings = new ArrayList<>();

        double cooldown = plugin.getConfig().getDouble("claim.cooldown-hours", 20.0);
        double deadline = plugin.getConfig().getDouble("claim.streak-deadline-hours", 36.0);
        if (cooldown <= 0) warnings.add("claim.cooldown-hours doit etre > 0");
        if (deadline <= 0) warnings.add("claim.streak-deadline-hours doit etre > 0");
        if (deadline < cooldown) warnings.add("streak-deadline-hours < cooldown-hours (ça resettra vite)");

        int guiSize = plugin.getConfig().getInt("gui.size", 27);
        if (guiSize < 9 || guiSize > 54 || guiSize % 9 != 0) {
            warnings.add("gui.size invalide (utilise 9..54 et multiple de 9)");
        }

        checkMaterial(plugin, warnings, "gui.fill-material", true);
        for (String key : List.of("claim-ready", "claim-cooldown", "stats", "timing", "close", "info")) {
            checkMaterial(plugin, warnings, "gui.icons." + key, false);
        }

        Set<Integer> usedSlots = new HashSet<>();
        for (String slotPath : List.of("gui.items.claim-slot", "gui.items.stats-slot", "gui.items.timing-slot", "gui.items.close-slot", "gui.items.info-slot")) {
            int slot = plugin.getConfig().getInt(slotPath, -1);
            if (slot < 0 || slot >= Math.max(9, guiSize)) {
                warnings.add(slotPath + " hors inventaire (0-" + (Math.max(9, guiSize) - 1) + ")");
            }
            if (!usedSlots.add(slot)) warnings.add("slot GUI en double: " + slotPath + "=" + slot);
        }

        ConfigurationSection itemSec = plugin.getConfig().getConfigurationSection("gui.items");
        List<Integer> preview = ConfigUtil.getIntListSafe(itemSec, "preview-slots", List.of());
        if (preview.isEmpty()) {
            warnings.add("gui.items.preview-slots vide");
        }
        for (Integer s : preview) {
            if (s == null || s < 0 || s >= Math.max(9, guiSize)) warnings.add("preview slot invalide: " + s);
        }

        int cycleSize = Math.max(1, plugin.getConfig().getInt("rewards.cycle-size", 7));
        if (cycleSize > 54) warnings.add("rewards.cycle-size tres grand (" + cycleSize + ")");
        for (int day = 1; day <= cycleSize; day++) {
            String base = "rewards.days." + day;
            if (!plugin.getConfig().contains(base)) {
                warnings.add("reward day manquant: " + base);
                continue;
            }
            checkMaterial(plugin, warnings, base + ".material", false);
            if (plugin.getConfig().getStringList(base + ".commands").isEmpty()) {
                warnings.add(base + ".commands vide");
            }
        }

        if (plugin.getConfig().getBoolean("lucky-hour.enabled", true)) {
            try {
                LocalTime.parse(plugin.getConfig().getString("lucky-hour.start", "18:00"));
                LocalTime.parse(plugin.getConfig().getString("lucky-hour.end", "20:00"));
            } catch (Exception e) {
                warnings.add("lucky-hour.start/end format invalide (HH:mm)");
            }
        }

        String storageType = plugin.getConfig().getString("storage.type", "YAML").toUpperCase(Locale.ROOT);
        if (!Set.of("YAML", "SQLITE", "MYSQL").contains(storageType)) {
            warnings.add("storage.type inconnu: " + storageType + " (fallback YAML possible)");
        }

        if ("MYSQL".equals(storageType)) {
            String host = plugin.getConfig().getString("storage.mysql.host", "");
            String db = plugin.getConfig().getString("storage.mysql.database", "");
            String user = plugin.getConfig().getString("storage.mysql.username", "");
            if (host.isBlank() || db.isBlank() || user.isBlank()) {
                warnings.add("mysql config incomplete (host/database/username)");
            }
        }

        return warnings;
    }

    private static void checkMaterial(RitualPulsePlugin plugin, List<String> warnings, String path, boolean optional) {
        String name = plugin.getConfig().getString(path, "");
        if ((name == null || name.isBlank())) {
            if (!optional) warnings.add(path + " vide");
            return;
        }
        Material m = Material.matchMaterial(name);
        if (m == null) warnings.add(path + " material invalide: " + name);
    }
}
