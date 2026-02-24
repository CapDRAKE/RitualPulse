package fr.majestycraft.ritualpulse.placeholder;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.model.PlayerRitualData;
import fr.majestycraft.ritualpulse.service.RitualService;
import fr.majestycraft.ritualpulse.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class RitualPlaceholderExpansion extends PlaceholderExpansion {

    private final RitualPulsePlugin plugin;
    private final RitualService service;

    public RitualPlaceholderExpansion(RitualPulsePlugin plugin, RitualService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public String getIdentifier() {
        return plugin.getConfig().getString("placeholders.identifier", "ritualpulse");
    }

    @Override
    public String getAuthor() {
        return "Bastien";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || player.getUniqueId() == null) return "";

        PlayerRitualData d = service.getData(player.getUniqueId());
        long now = System.currentTimeMillis();

        return switch (params.toLowerCase()) {
            case "streak" -> String.valueOf(d.getStreak());
            case "freeze_tokens" -> String.valueOf(d.getFreezeTokens());
            case "total_claims" -> String.valueOf(d.getTotalClaims());
            case "cycle_day" -> String.valueOf(service.getCycleDayFromStreak(Math.max(1, d.getStreak())));
            case "next_claim_in" -> TimeUtil.formatDuration(Math.max(0L, service.getNextClaimAt(d) - now));
            case "can_claim" -> String.valueOf(d.getLastClaimAt() <= 0 || now >= service.getNextClaimAt(d));
            default -> "";
        };
    }
}
