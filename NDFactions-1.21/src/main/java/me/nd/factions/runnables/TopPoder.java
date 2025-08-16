package me.nd.factions.runnables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.scheduler.BukkitRunnable;

import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.DataManager.FactionRankingType;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.utils.Utils;

public class TopPoder extends BukkitRunnable{

	@Override
	public void run() {
	    Map<NDFaction, Integer> top = new HashMap<>();

	    for (NDFaction fac : DataManager.factions.values()) {
	        top.put(fac, fac.getPoder());
	    }

	    List<NDFaction> ranking = Utils.entriesSortedByValues(top)
	        .stream()
	        .limit(200)
	        .map(Entry::getKey)
	        .collect(Collectors.toList());

	    DataManager.setRanking(FactionRankingType.PODER, ranking);
	}


}
