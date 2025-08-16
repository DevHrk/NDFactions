package me.nd.factions.runnables;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.nd.factions.mysql.DataManager;

public class Poder extends BukkitRunnable{

	@Override
	public void run() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (DataManager.players.containsKey(p.getName())) {
				if (DataManager.players.get(p.getName()).getPoder() < DataManager.players.get(p.getName()).getPodermax()) {
					DataManager.players.get(p.getName()).setPoder(DataManager.players.get(p.getName()).getPoder()+1);
				}
			}
		}		
	}

}
