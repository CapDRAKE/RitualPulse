package fr.majestycraft.ritualpulse.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public final class ConfigUtil {

    private ConfigUtil() {}

    public static Material materialOrDefault(String name, Material def) {
        if (name == null || name.isBlank()) return def;
        Material m = Material.matchMaterial(name.trim());
        return m != null ? m : def;
    }

    public static List<Integer> getIntListSafe(ConfigurationSection section, String path, List<Integer> def) {
        if (section == null || !section.contains(path)) return def;
        List<Integer> out = new ArrayList<>();
        for (Object obj : section.getList(path, List.of())) {
            if (obj instanceof Number n) out.add(n.intValue());
            else {
                try {
                    out.add(Integer.parseInt(String.valueOf(obj)));
                } catch (Exception ignored) {
                }
            }
        }
        return out.isEmpty() ? def : out;
    }
}
