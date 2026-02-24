package fr.majestycraft.ritualpulse.listener;

import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.gui.RitualGui;
import fr.majestycraft.ritualpulse.service.RitualService;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryType;

public class RitualListener implements Listener {

	private final RitualPulsePlugin plugin;
	private final RitualGui gui;
	private final RitualService service;

	public RitualListener(RitualPulsePlugin plugin, RitualGui gui, RitualService service) {
	    this.plugin = plugin;
	    this.gui = gui;
	    this.service = service;
	}

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory() == null) return;
        if (!(event.getInventory().getHolder() instanceof RitualGui.MenuHolder)) return;

        event.setCancelled(true); // on lock la GUI sinon ca devient vite le bazar

        if (event.getRawSlot() >= event.getInventory().getSize()) return;
        gui.handleClick(player, event.getInventory(), event.getRawSlot());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory() == null) return;
        if (!(event.getInventory().getHolder() instanceof RitualGui.MenuHolder)) return;
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("join-open.enabled", false)) return;

        Player player = event.getPlayer();

        if (!player.hasPermission("ritualpulse.use")) return;

        int delayTicks = Math.max(0, plugin.getConfig().getInt("join-open.delay-ticks", 20));
        boolean onlyWhenCanClaim = plugin.getConfig().getBoolean("join-open.only-when-can-claim", true);
        boolean dontOpenIfOtherGuiOpen = plugin.getConfig().getBoolean("join-open.dont-open-if-other-gui-open", true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // evite d'ecraser une autre gui ouverte au login (auth/menu etc)
            if (dontOpenIfOtherGuiOpen) {
                if (player.getOpenInventory() != null
                        && player.getOpenInventory().getTopInventory() != null
                        && player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
                    return;
                }
            }

            if (onlyWhenCanClaim) {
                RitualService.Snapshot snap = service.buildSnapshot(player);
                if (!snap.canClaim) return;
            }

            gui.open(player);
        }, delayTicks);
    }
}
