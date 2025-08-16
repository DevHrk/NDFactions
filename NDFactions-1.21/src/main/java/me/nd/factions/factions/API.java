package me.nd.factions.factions;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.nd.factions.enums.Protecao;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.Utils;

public class API {

	public static NDPlayer getPlayer(String nome) {
		return DataManager.players.get(nome);
	}
	
	public static NDFaction getFactionByName(String nome) {
		return DataManager.factions.get(nome);
	}
	
	public static NDFaction getFactionByTerra(Terra t) {
		return t.getFaction();
	}
	
	public static NDFaction getFactionByTag(String tag) {
		return Utils.getFactionByTag(tag);
	}
	
	public static Protecao getProtecao(Player player, Location loc) {
		return Utils.getProtection(loc.getChunk(), player);
	}
	
	
}
