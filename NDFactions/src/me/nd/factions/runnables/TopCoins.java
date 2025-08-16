package me.nd.factions.runnables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;

import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.DataManager.FactionRankingType;
import me.nd.factions.objetos.NDFaction;

public class TopCoins extends BukkitRunnable {

	@Override
	public void run() {
	    List<NDFaction> allFactions = new ArrayList<>(DataManager.factions.values());

	    // Ordena pelo valor total (dinheiro + spawners) em ordem decrescente
	    allFactions.sort((f1, f2) -> {
	        double v1 = f1.getMoneyEmSpawners() + f1.getMoneyTotal();
	        double v2 = f2.getMoneyEmSpawners() + f2.getMoneyTotal();
	        return Double.compare(v2, v1); // decrescente
	    });

	    // Limita ao top 200
	    List<NDFaction> top200 = allFactions.size() > 200 ? allFactions.subList(0, 200) : allFactions;

	    // Atualiza ranking no DataManager
	    DataManager.setRanking(FactionRankingType.VALOR, top200);
	}


}
