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
    private static final String FACTION_CHEST_PREFIX = "Baú da Facção - ";
    private static final String ATTACK_STATUS_PREFIX = "Status de Ataque - [";

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        String title = event.getInventory().getTitle();
        if (!title.startsWith(FACTION_CHEST_PREFIX)) return;

        String factionName = title.substring(FACTION_CHEST_PREFIX.length());
        NDFaction faction = DataManager.factions.get(factionName);
        if (faction == null) return;

        Player player = (Player) event.getPlayer();
        faction.addVaultLog(player.getName(), "abriu");
        DataManager.saveVaultLogs(factionName, faction.getVaultLogs());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getInventory().getTitle();
        if (!title.startsWith(FACTION_CHEST_PREFIX)) return;

        String factionName = title.substring(FACTION_CHEST_PREFIX.length());
        DataManager.saveVault(factionName, event.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title == null || !title.startsWith(ATTACK_STATUS_PREFIX)) return;

        event.setCancelled(true);
    }
}
