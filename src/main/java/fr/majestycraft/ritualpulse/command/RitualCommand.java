package fr.majestycraft.ritualpulse.command;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.gui.RitualGui;
import fr.majestycraft.ritualpulse.model.PlayerRitualData;
import fr.majestycraft.ritualpulse.service.RitualService;
import fr.majestycraft.ritualpulse.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RitualCommand implements CommandExecutor, TabCompleter {

    private final RitualPulsePlugin plugin;
    private final RitualService service;
    private final RitualGui gui;

    public RitualCommand(RitualPulsePlugin plugin, RitualService service, RitualGui gui) {
        this.plugin = plugin;
        this.service = service;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLang().prefixed("player-only"));
                return true;
            }
            if (!player.hasPermission("ritualpulse.use")) {
                player.sendMessage(plugin.getLang().prefixed("no-permission"));
                return true;
            }
            gui.open(player);
            player.sendMessage(plugin.getLang().prefixed("opened"));
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLang().prefixed("player-only"));
                return true;
            }
            if (!player.hasPermission("ritualpulse.use")) {
                player.sendMessage(plugin.getLang().prefixed("no-permission"));
                return true;
            }

            RitualService.ClaimResult result = service.claim(player);
            if (handleCommonClaimStatuses(player, result)) return true;

            if (result.streakReset) player.sendMessage(plugin.getLang().prefixed("streak-reset"));
            if (result.usedFreeze) {
                player.sendMessage(plugin.getLang().prefixed("claimed-with-freeze").replace("%streak%", String.valueOf(result.newStreak)));
            }

            player.sendMessage(plugin.getLang().prefixed("claimed")
                    .replace("%streak%", String.valueOf(result.newStreak))
                    .replace("%day%", String.valueOf(result.cycleDay)));

            if (result.commandFailures > 0) {
                player.sendMessage(plugin.getLang().prefixed("claim-command-warn").replace("%count%", String.valueOf(result.commandFailures)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ritualpulse.admin")) {
                sender.sendMessage(plugin.getLang().prefixed("no-permission"));
                return true;
            }
            plugin.reloadEverything();
            sender.sendMessage(plugin.getLang().prefixed("reloaded"));
            sender.sendMessage(plugin.getLang().prefixed("storage-ready").replace("%storage%", service.getStorage().getName()));
            sendDoctorSummary(sender, 5);
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("ritualpulse.admin")) {
                sender.sendMessage(plugin.getLang().prefixed("no-permission"));
                return true;
            }

            if (args.length >= 2 && args[1].equalsIgnoreCase("doctor")) {
                plugin.runConfigDoctorAndLog("manual-command");
                sendDoctorSummary(sender, 10);
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(plugin.getLang().get("usage-admin"));
                return true;
            }

            String sub = args[1].toLowerCase();
            String targetName = args[2];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || target.getUniqueId() == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage(plugin.getLang().prefixed("unknown-player"));
                return true;
            }

            switch (sub) {
                case "reset" -> {
                    service.resetPlayer(target.getUniqueId());
                    sender.sendMessage(plugin.getLang().prefixed("admin-reset").replace("%target%", targetName));
                    return true;
                }
                case "givefreeze" -> {
                    if (args.length < 4) {
                        sender.sendMessage(plugin.getLang().get("usage-admin-givefreeze"));
                        return true;
                    }
                    Integer amount = parseInt(args[3], sender);
                    if (amount == null) return true;
                    service.addFreeze(target.getUniqueId(), amount);
                    sender.sendMessage(plugin.getLang().prefixed("admin-freeze")
                            .replace("%target%", targetName)
                            .replace("%amount%", String.valueOf(amount)));
                    return true;
                }
                case "setstreak" -> {
                    if (args.length < 4) {
                        sender.sendMessage(plugin.getLang().get("usage-admin-setstreak"));
                        return true;
                    }
                    Integer value = parseInt(args[3], sender);
                    if (value == null) return true;
                    service.setStreak(target.getUniqueId(), value);
                    sender.sendMessage(plugin.getLang().prefixed("admin-setstreak")
                            .replace("%target%", targetName)
                            .replace("%value%", String.valueOf(value)));
                    return true;
                }
                case "inspect" -> {
                    PlayerRitualData d = service.getData(target.getUniqueId());
                    long now = System.currentTimeMillis();
                    long next = Math.max(0L, service.getNextClaimAt(d) - now);
                    sender.sendMessage(plugin.getLang().get("inspect.header").replace("%target%", targetName));
                    sender.sendMessage(plugin.getLang().get("inspect.streak").replace("%streak%", String.valueOf(d.getStreak())));
                    sender.sendMessage(plugin.getLang().get("inspect.total").replace("%total%", String.valueOf(d.getTotalClaims())));
                    sender.sendMessage(plugin.getLang().get("inspect.freeze").replace("%freeze%", String.valueOf(d.getFreezeTokens())));
                    sender.sendMessage(plugin.getLang().get("inspect.last-claim")
                            .replace("%last_claim%", TimeUtil.formatTimestamp(d.getLastClaimAt(), plugin.getPluginZoneId())));
                    sender.sendMessage(plugin.getLang().get("inspect.next-claim")
                            .replace("%next_claim%", TimeUtil.formatDuration(next)));
                    return true;
                }
                default -> {
                    sender.sendMessage(plugin.getLang().prefixed("sub-unknown"));
                    return true;
                }
            }
        }

        sender.sendMessage(plugin.getLang().get("usage-main"));
        return true;
    }

    private boolean handleCommonClaimStatuses(Player player, RitualService.ClaimResult result) {
        if (result.status == RitualService.ClaimStatus.BUSY) {
            player.sendMessage(plugin.getLang().prefixed("claim-busy"));
            return true;
        }
        if (result.status == RitualService.ClaimStatus.COOLDOWN) {
            player.sendMessage(plugin.getLang().prefixed("on-cooldown").replace("%time%", TimeUtil.formatDuration(result.timeRemaining)));
            return true;
        }
        if (result.status == RitualService.ClaimStatus.STORAGE_ERROR) {
            player.sendMessage(plugin.getLang().prefixed("claim-storage-error"));
            return true;
        }
        return false;
    }

    private void sendDoctorSummary(CommandSender sender, int maxLines) {
        List<String> warnings = plugin.getLastConfigWarnings();
        sender.sendMessage(plugin.getLang().get("admin-doctor-header"));
        if (warnings.isEmpty()) {
            sender.sendMessage(plugin.getLang().get("admin-doctor-ok"));
            return;
        }
        sender.sendMessage(plugin.getLang().get("admin-doctor-count").replace("%count%", String.valueOf(warnings.size())));
        int max = Math.min(maxLines, warnings.size());
        for (int i = 0; i < max; i++) {
            sender.sendMessage(plugin.getLang().get("admin-doctor-line").replace("%line%", warnings.get(i)));
        }
        if (warnings.size() > max) {
            sender.sendMessage(plugin.getLang().get("admin-doctor-line").replace("%line%", "+" + (warnings.size() - max) + " autres warnings (voir console)"));
        }
    }

    private Integer parseInt(String raw, CommandSender sender) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLang().prefixed("invalid-number"));
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(List.of("claim", "reload", "admin"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) return filter(List.of("reset", "givefreeze", "setstreak", "inspect", "doctor"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("doctor")) return Collections.emptyList();
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return filter(names, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            if (args[1].equalsIgnoreCase("givefreeze")) return List.of("1", "2", "3");
            if (args[1].equalsIgnoreCase("setstreak")) return List.of("0", "7", "14", "30");
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> input, String start) {
        String s = start.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String str : input) if (str.toLowerCase().startsWith(s)) out.add(str);
        return out;
    }
}
