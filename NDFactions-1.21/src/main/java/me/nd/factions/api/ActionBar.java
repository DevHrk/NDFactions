package me.nd.factions.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class ActionBar {

    public static void sendActionBarMessage(Player player, String message) {
        player.sendActionBar(Component.text(message));
    }
}
