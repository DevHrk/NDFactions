package me.nd.factions.runnables;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.scheduler.BukkitRunnable;

import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.DataManager.FactionRankingType;
import me.nd.factions.objetos.NDFaction;

public class Invasoes extends BukkitRunnable {

    @Override
    public void run() {
        List<NDFaction> allFactions = new ArrayList<>(DataManager.factions.values());

        // Ordena pelo número de invasões bem-sucedidas em ordem decrescente
        allFactions.sort((f1, f2) -> {
            int invasions1 = f1.getSuccessfulInvasions();
            int invasions2 = f2.getSuccessfulInvasions();
            // Compara invasões; em caso de empate, usa o prejuízo causado como desempate
            if (invasions1 != invasions2) {
                return Integer.compare(invasions2, invasions1); // Decrescente
            } else {
                double damage1 = f1.getInvasionDamage();
                double damage2 = f2.getInvasionDamage();
                return Double.compare(damage2, damage1); // Desempate por prejuízo, decrescente
            }
        });

        // Limita ao top 200
        List<NDFaction> top200 = allFactions.size() > 200 ? allFactions.subList(0, 200) : allFactions;

        // Atualiza ranking no DataManager para INVASOES
        DataManager.setRanking(FactionRankingType.INVASOES, top200);
    }
}