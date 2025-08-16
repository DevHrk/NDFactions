package me.nd.factions.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import me.nd.factions.api.Config;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.Methods;
import me.nd.factions.objetos.NDPlayer;

public class onJoin implements Listener{

	@EventHandler
	public void onJoin2(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		e.setJoinMessage("");

		String name = player.getName();

		if (!DataManager.players.containsKey(name)) {
			if (!Methods.contains("NDPlayers", "nome", name)) {
				DataManager.players.put(name, new NDPlayer(
					name, "", 0, 0,
					(int) Config.get("Padrao.PoderInicial"),
					(int) Config.get("Padrao.PoderMaxInicial"),
					System.currentTimeMillis()
				));
			}
		}
		else {
			DataManager.players.get(name).setLast();
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		e.setQuitMessage("");
		if (DataManager.players.containsKey(e.getPlayer().getName()) && DataManager.players.get(e.getPlayer().getName()).hasFaction()) {
			for (Player p : DataManager.players.get(e.getPlayer().getName()).getFaction().getAllOnline()) {
				p.getPlayer().sendMessage(Config.get("Mensagens.JogadorSaiu").toString().replace("&", "§").replace("<jogador>", e.getPlayer().getName()));
			}
		}
	}
	
}
