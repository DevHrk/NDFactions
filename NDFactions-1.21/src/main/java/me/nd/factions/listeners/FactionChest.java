package me.nd.factions.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;

public class FactionChest implements Listener {
	
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getView().getTitle().startsWith("Baú da Facção - ")) {
            String factionName = event.getView().getTitle().replace("Baú da Facção - ", "");
            NDFaction faction = DataManager.factions.get(factionName);
            if (faction != null && event.getPlayer() instanceof Player) {
                Player player = (Player) event.getPlayer();
                faction.addVaultLog(player.getName(), "abriu");
                DataManager.saveVaultLogs(factionName, faction.getVaultLogs());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith("Baú da Facção - ")) {
            String factionName = event.getView().getTitle().replace("Baú da Facção - ", "");
            DataManager.saveVault(factionName, event.getInventory());
        }
    }

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
	    if (event.getInventory() == null || event.getView().getTitle() == null) {
	        return;
	    }
	    if (event.getView().getTitle().startsWith("Status de Ataque - [")) {
	        event.setCancelled(true);
	    }
	}

}
