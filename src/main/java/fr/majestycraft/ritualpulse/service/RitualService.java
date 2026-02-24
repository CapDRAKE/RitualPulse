package fr.majestycraft.ritualpulse.service;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.model.PlayerRitualData;
import fr.majestycraft.ritualpulse.storage.RitualStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RitualService {

    private final RitualPulsePlugin plugin;
    private final RitualStorage storage;
    private final Map<UUID, PlayerRitualData> cache = new ConcurrentHashMap<>();
    private final Set<UUID> claimBusy = ConcurrentHashMap.newKeySet();

    private long cooldownMs;
    private long streakDeadlineMs;

    public RitualService(RitualPulsePlugin plugin, RitualStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        reloadConfigDependentValues();
    }

    public void reloadConfigDependentValues() {
        double cooldownHours = plugin.getConfig().getDouble("claim.cooldown-hours", 20.0);
        double deadlineHours = plugin.getConfig().getDouble("claim.streak-deadline-hours", 36.0);

        this.cooldownMs = Math.max(1L, (long) (cooldownHours * 3600000L));
        this.streakDeadlineMs = Math.max(cooldownMs, (long) (deadlineHours * 3600000L));
    }

    public RitualStorage getStorage() {
        return storage;
    }

    public Map<UUID, PlayerRitualData> getCache() {
        return cache;
    }

    public PlayerRitualData getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, storage::load);
    }

    public void save(PlayerRitualData data) {
        storage.save(data);
    }

    public void saveAll() {
        storage.saveAll(cache);
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public long getStreakDeadlineMs() {
        return streakDeadlineMs;
    }

    public long getNextClaimAt(PlayerRitualData data) {
        if (data.getLastClaimAt() <= 0) return 0L;
        return data.getLastClaimAt() + cooldownMs;
    }

    public long getStreakDeadlineAt(PlayerRitualData data) {
        if (data.getLastClaimAt() <= 0) return 0L;
        return data.getLastClaimAt() + streakDeadlineMs;
    }

    public int getCycleSize() {
        return Math.max(1, plugin.getConfig().getInt("rewards.cycle-size", 7));
    }

    public int getCycleDayFromStreak(int streak) {
        if (streak <= 0) return 1;
        int cycle = getCycleSize();
        return ((streak - 1) % cycle) + 1;
    }

    public Snapshot buildSnapshot(Player player) {
        PlayerRitualData d = getData(player.getUniqueId());
        long now = System.currentTimeMillis();

        Snapshot s = new Snapshot();
        s.data = d;
        s.now = now;
        s.nextClaimAt = getNextClaimAt(d);
        s.streakDeadlineAt = getStreakDeadlineAt(d);
        s.canClaim = d.getLastClaimAt() <= 0 || now >= s.nextClaimAt || player.hasPermission("ritualpulse.bypasscooldown");
        s.wouldBreakIfClaimNow = d.getLastClaimAt() > 0 && now > s.streakDeadlineAt;
        s.previewStreak = predictNextStreak(d, now);
        s.previewDay = getCycleDayFromStreak(s.previewStreak);
        return s;
    }

    private int predictNextStreak(PlayerRitualData d, long now) {
        if (d.getLastClaimAt() <= 0) return 1;
        if (now <= d.getLastClaimAt() + streakDeadlineMs) return d.getStreak() + 1;

        boolean freezeEnabled = plugin.getConfig().getBoolean("streak.freeze.enabled", true);
        if (freezeEnabled && d.getFreezeTokens() > 0) return d.getStreak() + 1;
        return 1;
    }

    public ClaimResult claim(Player player) {
        UUID uuid = player.getUniqueId();
        if (!claimBusy.add(uuid)) {
            return new ClaimResult(ClaimStatus.BUSY);
        }

        try {
            return doClaim(player);
        } finally {
            claimBusy.remove(uuid);
        }
    }

    private ClaimResult doClaim(Player player) {
        PlayerRitualData d = getData(player.getUniqueId());
        long now = System.currentTimeMillis();

        if (!player.hasPermission("ritualpulse.bypasscooldown")) {
            long next = getNextClaimAt(d);
            if (d.getLastClaimAt() > 0 && now < next) {
                ClaimResult r = new ClaimResult(ClaimStatus.COOLDOWN);
                r.nextClaimAt = next;
                r.timeRemaining = next - now;
                return r;
            }
        }

        boolean usedFreeze = false;
        boolean streakReset = false;
        boolean comebackTriggered = false;
        int previousStreak = d.getStreak();

        if (d.getLastClaimAt() <= 0) {
            d.setStreak(1);
        } else {
            boolean missedDeadline = now > (d.getLastClaimAt() + streakDeadlineMs);
            if (missedDeadline) {
                boolean freezeEnabled = plugin.getConfig().getBoolean("streak.freeze.enabled", true);
                if (freezeEnabled && d.getFreezeTokens() > 0) {
                    d.setFreezeTokens(d.getFreezeTokens() - 1);
                    d.setStreak(d.getStreak() + 1);
                    usedFreeze = true;
                } else {
                    streakReset = true;
                    d.setStreak(1);
                    boolean comebackEnabled = plugin.getConfig().getBoolean("streak.comeback.enabled", true);
                    int minPrev = plugin.getConfig().getInt("streak.comeback.min-previous-streak", 3);
                    comebackTriggered = comebackEnabled && previousStreak >= minPrev;
                }
            } else {
                d.setStreak(d.getStreak() + 1);
            }
        }

        d.setLastClaimAt(now);
        d.setTotalClaims(d.getTotalClaims() + 1);

        int cycleDay = getCycleDayFromStreak(d.getStreak());

        int everyXClaims = plugin.getConfig().getInt("streak.freeze.grant-every-10-claims", 10);
        int maxTokens = plugin.getConfig().getInt("streak.freeze.max-tokens", 3);
        if (everyXClaims > 0 && d.getTotalClaims() > 0 && (d.getTotalClaims() % everyXClaims == 0)) {
            d.setFreezeTokens(Math.min(maxTokens, d.getFreezeTokens() + 1));
        }

        // mode premium-safe: on persiste avant d'executer les commandes pour eviter la duplication en cas de crash
        boolean persistBeforeDispatch = plugin.getConfig().getBoolean("command-execution.persist-before-dispatch", true);
        if (persistBeforeDispatch) {
            try {
                save(d);
            } catch (Throwable t) {
                plugin.getLogger().severe("claim save failed avant dispatch pour " + player.getName());
                t.printStackTrace();
                return new ClaimResult(ClaimStatus.STORAGE_ERROR);
            }
        }

        int commandFailures = 0;
        commandFailures += executeRewardDayCommands(player, d, cycleDay);
        commandFailures += executeMilestoneCommands(player, d, cycleDay);
        commandFailures += executeLuckyHourCommands(player, d, cycleDay);
        commandFailures += executeVipBonuses(player, d, cycleDay);
        if (comebackTriggered) {
            commandFailures += executeCommands(plugin.getConfig().getStringList("streak.comeback.commands"), player, d, cycleDay, "streak.comeback.commands");
        }

        if (!persistBeforeDispatch) {
            try {
                save(d);
            } catch (Throwable t) {
                plugin.getLogger().severe("claim save failed apres dispatch pour " + player.getName());
                t.printStackTrace();
                return new ClaimResult(ClaimStatus.STORAGE_ERROR);
            }
        }

        ClaimResult r = new ClaimResult(ClaimStatus.SUCCESS);
        r.usedFreeze = usedFreeze;
        r.streakReset = streakReset;
        r.comebackTriggered = comebackTriggered;
        r.newStreak = d.getStreak();
        r.cycleDay = cycleDay;
        r.nextClaimAt = getNextClaimAt(d);
        r.streakDeadlineAt = getStreakDeadlineAt(d);
        r.commandFailures = commandFailures;
        return r;
    }

    private int executeRewardDayCommands(Player player, PlayerRitualData d, int cycleDay) {
        return executeCommands(plugin.getConfig().getStringList("rewards.days." + cycleDay + ".commands"), player, d, cycleDay,
                "rewards.days." + cycleDay + ".commands");
    }

    private int executeMilestoneCommands(Player player, PlayerRitualData d, int cycleDay) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("milestones");
        if (section == null) return 0;
        String key = String.valueOf(d.getStreak());
        if (!section.contains(key)) return 0;
        return executeCommands(plugin.getConfig().getStringList("milestones." + key + ".commands"), player, d, cycleDay,
                "milestones." + key + ".commands");
    }

    private int executeLuckyHourCommands(Player player, PlayerRitualData d, int cycleDay) {
        if (!plugin.getConfig().getBoolean("lucky-hour.enabled", true)) return 0;
        try {
            LocalTime now = LocalTime.now(plugin.getPluginZoneId());
            LocalTime start = LocalTime.parse(plugin.getConfig().getString("lucky-hour.start", "18:00"));
            LocalTime end = LocalTime.parse(plugin.getConfig().getString("lucky-hour.end", "20:00"));

            boolean inWindow;
            if (start.equals(end)) {
                inWindow = true;
            } else if (start.isBefore(end)) {
                inWindow = !now.isBefore(start) && now.isBefore(end);
            } else {
                inWindow = !now.isBefore(start) || now.isBefore(end);
            }

            if (inWindow) {
                return executeCommands(plugin.getConfig().getStringList("lucky-hour.commands"), player, d, cycleDay, "lucky-hour.commands");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Config lucky-hour invalide (HH:mm)");
            plugin.debug("lucky-hour invalide (HH:mm)");
        }
        return 0;
    }

    private int executeVipBonuses(Player player, PlayerRitualData d, int cycleDay) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("vip-bonuses");
        if (section == null) return 0;

        int failCount = 0;
        for (String key : section.getKeys(false)) {
            String base = "vip-bonuses." + key;
            String permission = plugin.getConfig().getString(base + ".permission", "");
            if (permission.isEmpty() || !player.hasPermission(permission)) continue;
            failCount += executeCommands(plugin.getConfig().getStringList(base + ".commands"), player, d, cycleDay, base + ".commands");
        }
        return failCount;
    }

    private int executeCommands(List<String> commands, Player player, PlayerRitualData d, int cycleDay, String sourcePath) {
        if (commands == null) return 0;

        int failures = 0;
        boolean warnOnFail = plugin.getConfig().getBoolean("command-execution.warn-on-fail", true);
        boolean stopOnFirstFail = plugin.getConfig().getBoolean("command-execution.stop-on-first-fail", false);

        for (int i = 0; i < commands.size(); i++) {
            String raw = commands.get(i);
            if (raw == null || raw.isBlank()) continue;

            String cmd = replaceVars(raw, player, d, cycleDay);
            if (cmd.startsWith("/")) cmd = cmd.substring(1);

            try {
                plugin.debug("exec cmd: " + cmd);
                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                if (!ok) {
                    failures++;
                    if (warnOnFail) {
                        plugin.getLogger().warning("commande reward non exécutée (retour=false) path=" + sourcePath + " index=" + i + " cmd=" + cmd);
                    }
                    if (stopOnFirstFail) break;
                }
            } catch (Throwable t) {
                failures++;
                plugin.getLogger().warning("erreur commande reward path=" + sourcePath + " index=" + i + " cmd=" + cmd);
                t.printStackTrace();
                if (stopOnFirstFail) break;
            }
        }
        return failures;
    }

    public String replaceVars(String input, Player player, PlayerRitualData d, int cycleDay) {
        String out = input
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%streak%", String.valueOf(d.getStreak()))
                .replace("%day%", String.valueOf(cycleDay))
                .replace("%total_claims%", String.valueOf(d.getTotalClaims()))
                .replace("%freeze_tokens%", String.valueOf(d.getFreezeTokens()));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                out = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, out);
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    public void resetPlayer(UUID uuid) {
        PlayerRitualData d = getData(uuid);
        d.setLastClaimAt(0L);
        d.setStreak(0);
        d.setFreezeTokens(plugin.getConfig().getInt("streak.freeze.start-tokens", 0));
        d.setTotalClaims(0);
        save(d);
    }

    public void addFreeze(UUID uuid, int amount) {
        PlayerRitualData d = getData(uuid);
        int maxTokens = plugin.getConfig().getInt("streak.freeze.max-tokens", 3);
        d.setFreezeTokens(Math.min(maxTokens, d.getFreezeTokens() + Math.max(0, amount)));
        save(d);
    }

    public void setStreak(UUID uuid, int value) {
        PlayerRitualData d = getData(uuid);
        d.setStreak(Math.max(0, value));
        save(d);
    }

    public String getRewardDayDisplayName(int day) {
        return plugin.getLang().color(plugin.getConfig().getString("rewards.days." + day + ".display", "&fJour " + day));
    }

    public Material getRewardDayMaterial(int day) {
        String matName = plugin.getConfig().getString("rewards.days." + day + ".material", "CHEST");
        Material m = Material.matchMaterial(matName);
        return m != null ? m : Material.CHEST;
    }

    public List<String> getRewardDayLore(int day) {
        List<String> lore = plugin.getConfig().getStringList("rewards.days." + day + ".lore");
        List<String> out = new ArrayList<>();
        for (String s : lore) out.add(plugin.getLang().color(s));
        return out;
    }

    public enum ClaimStatus {
        SUCCESS, COOLDOWN, BUSY, STORAGE_ERROR
    }

    public static class ClaimResult {
        public final ClaimStatus status;
        public boolean usedFreeze;
        public boolean streakReset;
        public boolean comebackTriggered;
        public int newStreak;
        public int cycleDay;
        public long nextClaimAt;
        public long streakDeadlineAt;
        public long timeRemaining;
        public int commandFailures;

        public ClaimResult(ClaimStatus status) {
            this.status = status;
        }
    }

    public static class Snapshot {
        public PlayerRitualData data;
        public long now;
        public long nextClaimAt;
        public long streakDeadlineAt;
        public boolean canClaim;
        public boolean wouldBreakIfClaimNow;
        public int previewStreak;
        public int previewDay;
    }
}
