package fr.majestycraft.ritualpulse.gui;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.service.RitualService;
import fr.majestycraft.ritualpulse.util.ConfigUtil;
import fr.majestycraft.ritualpulse.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RitualGui {

    private final RitualPulsePlugin plugin;
    private final RitualService service;

    public RitualGui(RitualPulsePlugin plugin, RitualService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void open(Player player) {
        int size = Math.max(9, plugin.getConfig().getInt("gui.size", 27));
        if (size % 9 != 0) size = 27;

        String title = plugin.getLang().color(plugin.getConfig().getString("gui.title", "&6&lRitualPulse"));
        MenuHolder holder = new MenuHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        fillBackground(inv);

        RitualService.Snapshot snap = service.buildSnapshot(player);
        ConfigurationSection itemSec = plugin.getConfig().getConfigurationSection("gui.items");

        int claimSlot = plugin.getConfig().getInt("gui.items.claim-slot", 13);
        int statsSlot = plugin.getConfig().getInt("gui.items.stats-slot", 11);
        int timingSlot = plugin.getConfig().getInt("gui.items.timing-slot", 15);
        int closeSlot = plugin.getConfig().getInt("gui.items.close-slot", 26);
        int infoSlot = plugin.getConfig().getInt("gui.items.info-slot", 4);
        List<Integer> previewSlots = ConfigUtil.getIntListSafe(itemSec, "preview-slots", List.of(19, 20, 21, 22, 23, 24, 25));

        setSafe(inv, claimSlot, buildClaimButton(player, snap));
        setSafe(inv, statsSlot, buildStatsItem(snap));
        setSafe(inv, timingSlot, buildTimingItem(snap));
        setSafe(inv, infoSlot, buildInfoItem());
        setSafe(inv, closeSlot, createItem(iconMat("close", Material.BARRIER), "&cFermer", List.of("&7Clique pour fermer")));

        int cycleSize = service.getCycleSize();
        int currentCycleDay = snap.previewDay;
        for (int i = 0; i < previewSlots.size(); i++) {
            int slot = previewSlots.get(i);
            int day = i + 1;
            if (day > cycleSize) {
                setSafe(inv, slot, createItem(Material.GRAY_STAINED_GLASS_PANE, "&8Slot vide", null));
                continue;
            }

            List<String> lore = new ArrayList<>(service.getRewardDayLore(day));
            lore.add("");
            lore.add(day == currentCycleDay ? "&e➡ Prochain day du cycle" : "&7Day du cycle");
            setSafe(inv, slot, createItem(service.getRewardDayMaterial(day), service.getRewardDayDisplayName(day), lore));
        }

        player.openInventory(inv);
    }

    private void setSafe(Inventory inv, int slot, ItemStack item) {
        if (slot < 0 || slot >= inv.getSize()) return;
        inv.setItem(slot, item);
    }

    private Material iconMat(String key, Material def) {
        return ConfigUtil.materialOrDefault(plugin.getConfig().getString("gui.icons." + key), def);
    }

    private void fillBackground(Inventory inv) {
        Material fillMat = ConfigUtil.materialOrDefault(plugin.getConfig().getString("gui.fill-material"), Material.BLACK_STAINED_GLASS_PANE);
        String fillName = plugin.getConfig().getString("gui.items.filler-name", " ");
        ItemStack pane = createItem(fillMat, fillName, null);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private ItemStack buildClaimButton(Player player, RitualService.Snapshot snap) {
        if (snap.canClaim) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Clique pour récupérer ta récompense.");
            lore.add("&7Streak après claim: &e" + snap.previewStreak);
            lore.add("&7Jour du cycle: &e" + snap.previewDay);
            if (snap.wouldBreakIfClaimNow) {
                if (snap.data.getFreezeTokens() > 0 && plugin.getConfig().getBoolean("streak.freeze.enabled", true)) {
                    lore.add("&6⚠ Claim tardif: un joker sera utilisé");
                } else {
                    lore.add("&c⚠ Claim tardif: la streak sera reset");
                }
            }
            return createItem(iconMat("claim-ready", Material.CHEST), "&a&lRécupérer la récompense", lore);
        }

        long remain = Math.max(0L, snap.nextClaimAt - snap.now);
        List<String> lore = new ArrayList<>();
        lore.add("&7Pas encore prêt.");
        lore.add("&7Prochain claim: &e" + TimeUtil.formatDuration(remain));
        return createItem(iconMat("claim-cooldown", Material.CLOCK), "&c&lEn cooldown", lore);
    }

    private ItemStack buildStatsItem(RitualService.Snapshot snap) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Streak actuelle: &e" + snap.data.getStreak());
        lore.add("&7Jokers: &b" + snap.data.getFreezeTokens());
        lore.add("&7Total claims: &a" + snap.data.getTotalClaims());
        lore.add("&7Prochain jour cycle: &e" + snap.previewDay);
        return createItem(iconMat("stats", Material.BOOK), "&6&lStatistiques", lore);
    }

    private ItemStack buildTimingItem(RitualService.Snapshot snap) {
        List<String> lore = new ArrayList<>();
        if (snap.data.getLastClaimAt() <= 0) {
            lore.add("&aPremier claim disponible maintenant.");
        } else {
            long nextRemain = Math.max(0L, snap.nextClaimAt - snap.now);
            long deadlineRemain = Math.max(0L, snap.streakDeadlineAt - snap.now);
            lore.add("&7Avant prochain claim: &e" + TimeUtil.formatDuration(nextRemain));
            lore.add("&7Deadline streak: &c" + TimeUtil.formatDuration(deadlineRemain));
            lore.add(snap.now > snap.streakDeadlineAt ? "&cTu as dépassé la deadline de streak." : "&aStreak encore sauvable.");
        }
        return createItem(iconMat("timing", Material.COMPASS), "&b&lTiming", lore);
    }

    private ItemStack buildInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Cooldown: &e" + plugin.getConfig().getDouble("claim.cooldown-hours", 20.0) + "h");
        lore.add("&7Deadline streak: &e" + plugin.getConfig().getDouble("claim.streak-deadline-hours", 36.0) + "h");
        lore.add("&7Storage: &f" + service.getStorage().getName());
        lore.add("&7Lang: &f" + plugin.getLang().getCurrentLang());
        return createItem(iconMat("info", Material.NETHER_STAR), "&d&lInfos plugin", lore);
    }

    public void handleClick(Player player, Inventory inventory, int rawSlot) {
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) return;

        int claimSlot = plugin.getConfig().getInt("gui.items.claim-slot", 13);
        int closeSlot = plugin.getConfig().getInt("gui.items.close-slot", 26);

        if (rawSlot == claimSlot) {
            RitualService.ClaimResult result = service.claim(player);

            if (result.status == RitualService.ClaimStatus.BUSY) {
                player.sendMessage(plugin.getLang().prefixed("claim-busy"));
                return;
            }
            if (result.status == RitualService.ClaimStatus.COOLDOWN) {
                player.sendMessage(plugin.getLang().prefixed("on-cooldown").replace("%time%", TimeUtil.formatDuration(result.timeRemaining)));
                open(player);
                return;
            }
            if (result.status == RitualService.ClaimStatus.STORAGE_ERROR) {
                player.sendMessage(plugin.getLang().prefixed("claim-storage-error"));
                return;
            }

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
            open(player);
            return;
        }

        if (rawSlot == closeSlot) {
            player.closeInventory();
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(plugin.getLang().color(name));
        if (lore != null) {
            List<String> fixed = new ArrayList<>();
            for (String s : lore) fixed.add(plugin.getLang().color(s));
            meta.setLore(fixed);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static class MenuHolder implements InventoryHolder {
        private final UUID owner;
        private Inventory inventory;

        public MenuHolder(UUID owner) {
            this.owner = owner;
        }

        public UUID getOwner() {
            return owner;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
