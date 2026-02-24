package fr.majestycraft.ritualpulse.listener;

import fr.majestycraft.ritualpulse.gui.RitualGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class RitualListener implements Listener {

    private final RitualGui gui;

    public RitualListener(RitualGui gui) {
        this.gui = gui;
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
}
