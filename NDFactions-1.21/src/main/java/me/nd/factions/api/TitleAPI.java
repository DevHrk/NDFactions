package me.nd.factions.api;

import org.bukkit.entity.Player;

public class TitleAPI {

    public static void sendTitle(Player player, int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        if (title == null) title = "";
        if (subtitle == null) subtitle = "";
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
}