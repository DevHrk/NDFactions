package me.nd.factions.addons;

import me.nd.factions.Main;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.mysql.DataManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class BlockCommands implements Listener {

    private final static List<String> blockedCommands;
    private final static String blockMessage;
    static FileConfiguration config = Main.get().getConfig();
    static {
        
        blockedCommands = config.getStringList("BloquearComandos");
        blockMessage = config.getString("Mensagens.ComandoBloqueado", "&cVocê não pode usar este comando em território de facção!");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase().split(" ")[0].substring(1); // Get command without arguments (e.g., "home" from "/home")

        // Check if command is in blocked list
        if (!isCommandBlocked(command)) {
            return; // Command not blocked, allow execution
        }

        // Check bypass permission
        if (player.hasPermission("factions.bypasscommands")) {
            return; // Player has bypass permission, allow command
        }

        // Get player's location and corresponding Terra
        Location location = player.getLocation();
        Terra terra = new Terra(location.getWorld(), location.getChunk().getX(), location.getChunk().getZ());

        // Get the player's faction
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        NDFaction playerFaction = (ndPlayer != null) ? ndPlayer.getFaction() : null;

        // Check if player is in any faction's territory
        for (NDFaction faction : DataManager.factions.values()) {
            if (faction.ownsTerritory(terra)) {
                // Block commands for players without a faction
                if (playerFaction == null) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', blockMessage.replace("<relacao>", "uma facção")));
                    return;
                }
                // Block commands in enemy territory
                if (playerFaction.isInimigo(faction)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', blockMessage.replace("<relacao>", "uma facção inimiga")));
                    return;
                }
                // Allow commands in own, allied, or neutral territory
                return;
            }
        }
    }

    private boolean isCommandBlocked(String command) {
        // Check if command or its aliases are in the blocked list
        for (String blocked : blockedCommands) {
            if (blocked.equalsIgnoreCase(command)) {
                return true;
            }
            // Handle aliases (e.g., "f" for "faction")
            String[] parts = blocked.split(":");
            if (parts.length > 1 && parts[0].equalsIgnoreCase(command)) {
                return true;
            }
        }
        return false;
    }
}
