package me.nd.factions.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

public class Vault {

	public static Economy Economy;

	public Vault() {
		setupEconomy();
	}

	public static double getPlayerBalance(String p) {
		return Economy.getBalance(p);
	}

	public static double getPlayerBalance(Player p) {
		return Economy.getBalance(p);
	}

	public static void take(Player p, int value) {
		Economy.withdrawPlayer(p, value);
	}

	private boolean setupEconomy() {
		if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		Economy = rsp.getProvider();
		return Economy != null;
	}
}
