package me.nd.factions.mysql;

import me.nd.factions.objetos.NDFaction;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TaxLog {
    private final String id;
    private final NDFaction faction;
    private final double percentage;
    private final double moneyDeducted;
    private final Map<EntityType, Integer> spawnersDeducted;
    private final long timestamp;
    private static final Map<String, AtomicInteger> factionTaxCounters = new HashMap<>();

    public TaxLog(NDFaction faction, double percentage, double moneyDeducted, Map<EntityType, Integer> spawnersDeducted) {
        this.faction = faction;
        this.percentage = percentage;
        this.moneyDeducted = moneyDeducted;
        this.spawnersDeducted = new HashMap<>(spawnersDeducted);
        this.timestamp = System.currentTimeMillis();
        // Gera ID da facção para devolver.
        String factionName = faction.getNome().replaceAll("[^a-zA-Z0-9]", "_"); // Pega as letras de a-z e numero 0 - 9
        synchronized (factionTaxCounters) {
            AtomicInteger counter = factionTaxCounters.computeIfAbsent(factionName, k -> new AtomicInteger(0));
            this.id = factionName + "_" + counter.incrementAndGet();
        }
    }

    public String getId() {
        return id;
    }

    public NDFaction getFaction() {
        return faction;
    }

    public double getPercentage() {
        return percentage;
    }

    public double getMoneyDeducted() {
        return moneyDeducted;
    }

    public Map<EntityType, Integer> getSpawnersDeducted() {
        return new HashMap<>(spawnersDeducted);
    }

    public long getTimestamp() {
        return timestamp;
    }
}