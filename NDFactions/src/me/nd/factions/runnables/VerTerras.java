package me.nd.factions.runnables;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.nd.factions.mysql.DataManager;
import me.nd.factions.utils.Utils;

public class VerTerras extends BukkitRunnable{

	@Override
	public void run() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (DataManager.players.containsKey(p.getName())) {
				if (DataManager.players.get(p.getName()).isVerTerras()) {
					Utils.efeito(p);
				}
			}
		}
	}

	
	
}
