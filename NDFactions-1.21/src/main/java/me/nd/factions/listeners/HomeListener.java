package me.nd.factions.listeners;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.nd.factions.api.Config;
import me.nd.factions.enums.Cargo;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.ItemBuilder;

public class HomeListener implements Listener {
	
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        NDPlayer dplayer = DataManager.players.get(player.getName());
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        if (title.startsWith("Homes - [")) {
            if (!dplayer.hasFaction()) {
                player.closeInventory();
                player.sendMessage(Config.get("Mensagens.SemFac").toString().replace("&", "§"));
                return;
            }

            NDFaction faction = dplayer.getFaction();

            if (clickedItem.getType() == Material.ARROW && clickedItem.getItemMeta().getDisplayName().equals("§cFechar")) {
                player.closeInventory();
                return;
            }

            if (clickedItem.getType() == Material.ENDER_PEARL) {
                String homeName = clickedItem.getItemMeta().getDisplayName().replace("§aHome: ", "");
                if (event.isLeftClick()) {
                    // Teleport to home
                    Location home = faction.getHome(homeName);
                    if (home == null) {
                        player.sendMessage("§cA home '" + homeName + "' não existe mais.");
                        player.closeInventory();
                        return;
                    }
                    Terra terra = new Terra(home.getWorld(), home.getChunk().getX(), home.getChunk().getZ());
                    if (!faction.ownsTerritory(terra) && !faction.getTemporarios().contains(terra)) {
                        player.sendMessage("§cA home '" + homeName + "' está em um território que não pertence mais à sua facção.");
                        player.closeInventory();
                        return;
                    }
                    player.sendMessage(Config.get("Mensagens.TeleportadoParaBase").toString()
                            .replace("&", "§")
                            .replace("base", "home '" + homeName + "'"));
                    player.teleport(home);
                    player.closeInventory();
                } else if (event.isRightClick() && hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
                    // Open deletion confirmation
                    Inventory inv = Bukkit.createInventory(null, 27, "Deletar Home: " + homeName);
                    inv.setItem(11, new ItemBuilder(Material.GREEN_WOOL, 1, 5)
                            .setName("§aConfirmar")
                            .setLore("§7Confirmar a exclusão da home '" + homeName + "'")
                            .toItemStack());
                    inv.setItem(15, new ItemBuilder(Material.RED_WOOL, 1, 14)
                            .setName("§cCancelar")
                            .setLore("§7Cancelar a exclusão")
                            .toItemStack());
                    player.openInventory(inv);
                }
            }
            event.setCancelled(true);
        }

        // Handle Home Deletion Confirmation
        if (title.startsWith("Deletar Home: ")) {
            if (!dplayer.hasFaction() || !hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
                player.closeInventory();
                player.sendMessage(Config.get("Mensagens.SemPermissao").toString().replace("&", "§"));
                return;
            }

            NDFaction faction = dplayer.getFaction();
            String homeName = title.replace("Deletar Home: ", "");

                if (clickedItem.getType() == Material.GREEN_WOOL) { // Green wool (Confirm)
                    if (!faction.hasHome(homeName)) {
                        player.sendMessage("§cA home '" + homeName + "' não existe.");
                        player.closeInventory();
                        return;
                    }
                    faction.removeHome(homeName);
                    DataManager.removeHome(faction.getNome(), homeName);
                    player.sendMessage("§aHome '" + homeName + "' deletada com sucesso.");
                    player.closeInventory();
                } else if (clickedItem.getType() == Material.RED_WOOL) { // Red wool (Cancel)
                    player.closeInventory();
                }

            event.setCancelled(true);
        }
    }

    private boolean hasRequiredCargo(NDPlayer player, Cargo... requiredCargos) {
        if (Arrays.asList(requiredCargos).contains(player.getCargo())) return true;
        player.getPlayer().sendMessage(Config.get("Mensagens.SemCargo").toString()
                .replace("&", "§")
                .replace("<cargo>", player.getCargo().toString().toLowerCase()));
        return false;
    }
}
