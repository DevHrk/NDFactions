package me.nd.factions.listeners;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.nd.factions.comandos.Comandos;
import me.nd.factions.objetos.NDFaction;

public class FactionRoster implements Listener {

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
	    if (!(event.getWhoClicked() instanceof Player)) return;
	    Player player = (Player) event.getWhoClicked();
	    Inventory inventory = event.getInventory();
	    String title = inventory.getTitle();

	    if (!title.startsWith("Facções com Roster - Página ")) return;

	    event.setCancelled(true); 

	    ItemStack clickedItem = event.getCurrentItem();
	    if (clickedItem == null || !clickedItem.hasItemMeta()) return;

	    String itemName = clickedItem.getItemMeta().getDisplayName();
	    int currentPage = Integer.parseInt(title.replace("Facções com Roster - Página ", ""));

	    List<NDFaction> rosterFactions = Comandos.getRosterFactions(player.getName());

	    if (event.getSlot() == 45 && itemName.equals("§aPágina Anterior")) {
	        Comandos.openRosterMenu(player, rosterFactions, currentPage - 1);
	    } else if (event.getSlot() == 53 && itemName.equals("§aPróxima Página")) {
	        Comandos.openRosterMenu(player, rosterFactions, currentPage + 1);
	    } else if (event.getSlot() == 49 && itemName.equals("§cFechar")) {
	        player.closeInventory();
	    } else if (clickedItem.getType() == Material.BANNER) {
	        // Lógica para entrar na facção ao clicar no banner
	        String factionTag = clickedItem.getItemMeta().getDisplayName().replaceAll("§a\\[([A-Z0-9]{3})\\].*", "$1");
	        player.performCommand("f entrar " + factionTag);
	        player.closeInventory();
	    }
	}
}
