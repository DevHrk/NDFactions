package me.nd.factions.mysql;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.nd.factions.Main;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDFaction.VaultLog;
import me.nd.factions.objetos.NDPlayer;

public class DataManager {

    // Campos existentes
    public static final Map<String, NDPlayer> players = new ConcurrentHashMap<>();
    public static final Map<String, NDFaction> factions = new ConcurrentHashMap<>();

    public enum FactionRankingType {
        VALOR, COINS, KDR, SPAWNERS, PODER
    }

    public static final Map<FactionRankingType, List<NDFaction>> factionRankings = new EnumMap<>(FactionRankingType.class);

    static {
        for (FactionRankingType type : FactionRankingType.values()) {
            factionRankings.put(type, new ArrayList<>());
        }
    }

    public static void setRanking(FactionRankingType type, List<NDFaction> factions) {
        factionRankings.put(type, factions);
    }

    public static List<NDFaction> getRanking(FactionRankingType type) {
        return factionRankings.get(type);
    }
    
    private static final Map<String, Integer> extraMemberSlotsMap = new HashMap<>();
    public static File extraMemberSlotsFile;
    public static FileConfiguration extraMemberSlotsConfig;

    // Campos para spawn_points.yml
    public static File spawnPointsFile;
    public static FileConfiguration spawnPointsConfig;

    // Novos campos para rosters.yml
    public static File rostersFile;
    public static FileConfiguration rostersConfig;

    // New fields for homes.yml
    public static File homesFile;
    public static FileConfiguration homesConfig;

    // New fields for vaults.yml
    public static File vaultsFile;
    public static FileConfiguration vaultsConfig;

    // Novo campo para faction_data.yml
    public static File factionDataFile;
    public static FileConfiguration factionDataConfig;

    public static void initFactionDataFile() {
        factionDataFile = new File(Main.get().getDataFolder(), "faction_data.yml");
        if (!factionDataFile.exists()) {
            try {
                factionDataFile.createNewFile();
                Main.get().getLogger().info("Criado novo faction_data.yml");
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, "Erro ao criar faction_data.yml: " + e.getMessage(), e);
            }
        }
        factionDataConfig = YamlConfiguration.loadConfiguration(factionDataFile);
        Main.get().getLogger().info("factionDataConfig inicializado: " + (factionDataConfig != null));
    }

    public static void loadFactionData(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return;
        }
        String factionName = faction.getNome();
        String path = "factions." + factionName;
        if (factionDataConfig.contains(path)) {
            Main.get().getLogger().info("Carregado dados para " + factionName);
        } else {
            Main.get().getLogger().info("Nenhum dado encontrado para " + factionName + " em faction_data.yml");
        }
    }

    public static void saveFactionData(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return;
        }
        String factionName = faction.getNome();
        try {
            factionDataConfig.save(factionDataFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar faction_data.yml para " + factionName + ": " + e.getMessage(), e);
        }
    }

    public static void removeFactionData(String factionName) {
        if (factionName == null || factionName.isEmpty()) {
            return;
        }
        String path = "factions." + factionName;
        factionDataConfig.set(path, null);
        try {
            factionDataConfig.save(factionDataFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao remover dados de " + factionName + " em faction_data.yml: " + e.getMessage(), e);
        }
    }

    public static boolean canActivateImmunity(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return false;
        }
        String path = "factions." + faction.getNome();
        long currentTime = System.currentTimeMillis();
        long twelveHoursInMillis = 12 * 60 * 60 * 1000;
        long lastImmunityEnd = factionDataConfig.getLong(path + ".lastImmunityEnd", 0);
        return !hasImmunity(faction) && (lastImmunityEnd == 0 || currentTime >= lastImmunityEnd + twelveHoursInMillis);
    }
    
    public static long getRemainingImmunityTime(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return 0;
        }
        String path = "factions." + faction.getNome();
        long immunityEndTime = factionDataConfig.getLong(path + ".immunityEndTime", 0);
        long currentTime = System.currentTimeMillis();
        return Math.max(0, (immunityEndTime - currentTime) / 1000); // Convert to seconds
    }

    public static long getImmunityCooldown(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return 0;
        }
        String path = "factions." + faction.getNome();
        long currentTime = System.currentTimeMillis();
        long lastImmunityEnd = factionDataConfig.getLong(path + ".lastImmunityEnd", 0);
        long twelveHoursInMillis = 12 * 60 * 60 * 1000;
        return Math.max(0, ((lastImmunityEnd + twelveHoursInMillis) - currentTime) / 1000); // Convert to seconds
    }

    public static void activateImmunity(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return;
        }
        if (canActivateImmunity(faction)) {
            String path = "factions." + faction.getNome();
            long currentTime = System.currentTimeMillis();
            long thirtyMinutesInMillis = 30 * 60 * 1000;
            factionDataConfig.set(path + ".isImmune", true);
            factionDataConfig.set(path + ".immunityEndTime", currentTime + thirtyMinutesInMillis);
            factionDataConfig.set(path + ".spawnerBreakDelayStart", currentTime);
            try {
                factionDataConfig.save(factionDataFile);
                Main.get().getLogger().info("Imunidade ativada para facção " + faction.getNome());
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar imunidade para " + faction.getNome() + ": " + e.getMessage(), e);
            }
        }
    }

    public static boolean canBreakSpawners(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return true;
        }
        String path = "factions." + faction.getNome();
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = 60 * 60 * 1000;
        long spawnerBreakDelayStart = factionDataConfig.getLong(path + ".spawnerBreakDelayStart", 0);
        return spawnerBreakDelayStart == 0 || currentTime >= spawnerBreakDelayStart + oneHourInMillis;
    }

    public static long getRemainingBreakDelay(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return 0;
        }
        String path = "factions." + faction.getNome();
        long oneHourInMillis = 60 * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        long spawnerBreakDelayStart = factionDataConfig.getLong(path + ".spawnerBreakDelayStart", 0);
        return Math.max(0, (spawnerBreakDelayStart + oneHourInMillis) - currentTime);
    }

    public static boolean hasImmunity(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty() || factionDataConfig == null) {
            return false;
        }
        String path = "factions." + faction.getNome();
        boolean isImmune = factionDataConfig.getBoolean(path + ".isImmune", false);
        long immunityEndTime = factionDataConfig.getLong(path + ".immunityEndTime", 0);
        long currentTime = System.currentTimeMillis();
        if (isImmune && currentTime > immunityEndTime) {
            factionDataConfig.set(path + ".isImmune", false);
            factionDataConfig.set(path + ".immunityEndTime", 0);
            factionDataConfig.set(path + ".lastImmunityEnd", currentTime);
            try {
                factionDataConfig.save(factionDataFile);
                Main.get().getLogger().info("Imunidade expirada para facção " + faction.getNome());
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar expiração de imunidade para " + faction.getNome() + ": " + e.getMessage(), e);
            }
            return false;
        }
        return isImmune;
    }
    
    // Initialize vaults.yml
    public static void initVaultsFile() {
        vaultsFile = new File(Main.get().getDataFolder(), "vaults.yml");
        if (!vaultsFile.exists()) {
            try {
                vaultsFile.createNewFile();
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, "Erro ao criar vaults.yml: " + e.getMessage(), e);
            }
        }
        vaultsConfig = YamlConfiguration.loadConfiguration(vaultsFile);
    }

    // Load vault for a faction from vaults.yml
    public static Inventory loadVault(String factionName) {
        String path = "factions." + factionName + ".vault";
        Inventory vault = Bukkit.createInventory(null, 54, "Baú da Facção - " + factionName);

        if (vaultsConfig.contains(path)) {
            List<?> items = vaultsConfig.getList(path);
            if (items != null) {
                int slot = 0;
                for (Object obj : items) {
                    if (obj instanceof ItemStack && slot < vault.getSize()) {
                        vault.setItem(slot++, (ItemStack) obj);
                    }
                }
            }
        }
        return vault;
    }

    // Save vault for a faction to vaults.yml
    public static void saveVault(String factionName, Inventory vault) {
        String path = "factions." + factionName + ".vault";
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : vault.getContents()) {
            if (item != null) {
                items.add(item);
            }
        }
        vaultsConfig.set(path, items);
        try {
            vaultsConfig.save(vaultsFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar vaults.yml para " + factionName + ": " + e.getMessage(), e);
        }
    }

    // Load vault logs for a faction
    public static List<VaultLog> loadVaultLogs(String factionName) {
        String path = "factions." + factionName + ".logs";
        List<VaultLog> logs = new ArrayList<>();
        if (vaultsConfig.contains(path)) {
            List<?> rawLogs = vaultsConfig.getList(path);
            if (rawLogs != null) {
                for (Object obj : rawLogs) {
                    if (obj instanceof Map) {
                        Map<?, ?> logData = (Map<?, ?>) obj;
                        String playerName = (String) logData.get("player");
                        String action = (String) logData.get("action");
                        long timestamp = ((Number) logData.get("timestamp")).longValue();
                        logs.add(new VaultLog(playerName, action, timestamp));
                    }
                }
            }
        }
        return logs;
    }

    // Save vault logs for a faction
    public static void saveVaultLogs(String factionName, List<VaultLog> logs) {
        String path = "factions." + factionName + ".logs";
        List<Map<String, Object>> serializedLogs = new ArrayList<>();
        for (VaultLog log : logs) {
            Map<String, Object> logData = new HashMap<>();
            logData.put("player", log.getPlayerName());
            logData.put("action", log.getAction());
            logData.put("timestamp", log.getTimestamp());
            serializedLogs.add(logData);
        }
        vaultsConfig.set(path, serializedLogs);
        try {
            vaultsConfig.save(vaultsFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar logs de baú para " + factionName + ": " + e.getMessage(), e);
        }
    }

    // Remove vault and logs for a faction
    public static void removeVault(String factionName) {
        String path = "factions." + factionName;
        vaultsConfig.set(path, null);
        try {
            vaultsConfig.save(vaultsFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao remover vault e logs de " + factionName + " em vaults.yml: " + e.getMessage(), e);
        }
    }
    
    // Initialize homes.yml
    public static void initHomesFile() {
        homesFile = new File(Main.get().getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, "Erro ao criar homes.yml: " + e.getMessage(), e);
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    // Load homes for a faction from homes.yml
    public static Map<String, Location> loadHomes(String factionName) {
        Map<String, Location> homes = new HashMap<>();
        String path = "factions." + factionName + ".homes";

        if (homesConfig.contains(path)) {
            for (String homeName : homesConfig.getConfigurationSection(path).getKeys(false)) {
                Location location = (Location) homesConfig.get(path + "." + homeName);
                if (location != null && location.getWorld() != null) {
                    homes.put(homeName, location);
                } else {
                    Main.get().getLogger().warning("Localização inválida para home '" + homeName + "' na facção " + factionName);
                }
            }
        }
        return homes;
    }

    // Save homes for a faction to homes.yml
    public static void saveHomes(String factionName, Map<String, Location> homes) {
        String path = "factions." + factionName + ".homes";

        // Clear existing data
        homesConfig.set(path, null);

        // Save new data
        for (Map.Entry<String, Location> entry : homes.entrySet()) {
            homesConfig.set(path + "." + entry.getKey(), entry.getValue());
        }

        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar homes.yml para " + factionName + ": " + e.getMessage(), e);
        }
    }

    // Set a home for a faction
    public static void setHome(String factionName, String homeName, Location location) {
        Map<String, Location> homes = loadHomes(factionName);
        homes.put(homeName, location);
        saveHomes(factionName, homes);
    }

    // Remove a home from a faction
    public static void removeHome(String factionName, String homeName) {
        Map<String, Location> homes = loadHomes(factionName);
        if (homes.remove(homeName) != null) {
            saveHomes(factionName, homes);
        }
    }
    
    // New method to remove all homes for a faction
    public static void removeHomes(String factionName) {
        String path = "factions." + factionName + ".homes";
        homesConfig.set(path, null);
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao remover homes de " + factionName + " em homes.yml: " + e.getMessage(), e);
        }
    }

    // Get a specific home
    public static Location getHome(String factionName, String homeName) {
        return loadHomes(factionName).get(homeName);
    }
    
    // Inicializar o arquivo rosters.yml
    public static void initRostersFile() {
        rostersFile = new File(Main.get().getDataFolder(), "rosters.yml");
        if (!rostersFile.exists()) {
            try {
                rostersFile.createNewFile();
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, "Erro ao criar rosters.yml: " + e.getMessage(), e);
            }
        }
        rostersConfig = YamlConfiguration.loadConfiguration(rostersFile);
    }

    // Carregar o roster de uma facção do arquivo YAML
    public static List<String> loadRoster(String factionName) {
        String path = "factions." + factionName + ".roster";
        List<String> roster = rostersConfig.getStringList(path);
        return new ArrayList<>(roster); // Retorna uma cópia para evitar modificações externas
    }

    // Salvar o roster de uma facção no arquivo YAML
    public static void saveRoster(String factionName, List<String> roster) {
        String path = "factions." + factionName + ".roster";
        rostersConfig.set(path, roster);
        try {
            rostersConfig.save(rostersFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar rosters.yml para " + factionName + ": " + e.getMessage(), e);
        }
    }

    // Adicionar um jogador ao roster
    public static void addToRoster(String factionName, String playerName) {
        List<String> roster = loadRoster(factionName);
        if (!roster.contains(playerName)) {
            roster.add(playerName);
            saveRoster(factionName, roster);
        }
    }

    // Remover um jogador do roster
    public static void removeFromRoster(String factionName, String playerName) {
        List<String> roster = loadRoster(factionName);
        if (roster.remove(playerName)) {
            saveRoster(factionName, roster);
        }
    }

    // Verificar se um jogador está no roster
    public static boolean isInRoster(String factionName, String playerName) {
        return loadRoster(factionName).contains(playerName);
    }

    // Remover o roster de uma facção (usado ao dissolver)
    public static void removeRoster(String factionName) {
        String path = "factions." + factionName;
        rostersConfig.set(path, null);
        try {
            rostersConfig.save(rostersFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao remover roster de " + factionName + " em rosters.yml: " + e.getMessage(), e);
        }
    }

    // Inicializar o arquivo spawn_points.yml
    public static void initSpawnPointsFile() {
        spawnPointsFile = new File(Main.get().getDataFolder(), "spawn_points.yml");
        if (!spawnPointsFile.exists()) {
            try {
                spawnPointsFile.createNewFile();
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, "Erro ao criar spawn_points.yml: " + e.getMessage(), e);
            }
        }
        spawnPointsConfig = YamlConfiguration.loadConfiguration(spawnPointsFile);
    }

    // Carregar spawn points de uma facção do arquivo YAML
    public static Map<EntityType, Location> loadSpawnPoints(String factionName) {
        Map<EntityType, Location> spawnPoints = new HashMap<>();
        String path = "factions." + factionName + ".spawn_points";
        
        if (spawnPointsConfig.contains(path)) {
            for (String key : spawnPointsConfig.getConfigurationSection(path).getKeys(false)) {
                try {
                    EntityType entityType = EntityType.valueOf(key);
                    Location location = (Location) spawnPointsConfig.get(path + "." + key);
                    if (location != null && location.getWorld() != null) {
                        spawnPoints.put(entityType, location);
                    } else {
                        Main.get().getLogger().warning("Localização inválida para spawn point de " + key + " na facção " + factionName);
                    }
                } catch (IllegalArgumentException e) {
                    Main.get().getLogger().warning("EntityType inválido: " + key + " para facção " + factionName);
                }
            }
        }
        return spawnPoints;
    }

    // Salvar spawn points de uma facção no arquivo YAML
    public static void saveSpawnPoints(String factionName, Map<EntityType, Location> spawnPoints) {
        String path = "factions." + factionName + ".spawn_points";
        
        // Limpar os dados existentes
        spawnPointsConfig.set(path, null);
        
        // Salvar novos dados
        for (Map.Entry<EntityType, Location> entry : spawnPoints.entrySet()) {
            spawnPointsConfig.set(path + "." + entry.getKey().name(), entry.getValue());
        }
        
        try {
            spawnPointsConfig.save(spawnPointsFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar spawn_points.yml para " + factionName + ": " + e.getMessage(), e);
        }
    }

    // Definir um spawn point para uma facção
    public static void setSpawnPoint(String factionName, EntityType type, Location location) {
        Map<EntityType, Location> spawnPoints = loadSpawnPoints(factionName);
        spawnPoints.put(type, location);
        saveSpawnPoints(factionName, spawnPoints);
    }

    // Remover um spawn point de uma facção
    public static void removeSpawnPoint(String factionName, EntityType type) {
        Map<EntityType, Location> spawnPoints = loadSpawnPoints(factionName);
        if (spawnPoints.remove(type) != null) {
            saveSpawnPoints(factionName, spawnPoints);
        }
    }

    // Obter um spawn point específico
    public static Location getSpawnPoint(String factionName, EntityType type) {
        return loadSpawnPoints(factionName).get(type);
    }

    // Métodos existentes para extraMemberSlots
    public static void loadExtraMemberSlots() {
        if (!extraMemberSlotsFile.exists()) {
            saveExtraMemberSlots();
        }

        for (String factionName : extraMemberSlotsConfig.getKeys(false)) {
            int slots = extraMemberSlotsConfig.getInt(factionName, 0);
            extraMemberSlotsMap.put(factionName, slots);
        }
    }

    public static void saveExtraMemberSlots() {
        for (Map.Entry<String, Integer> entry : extraMemberSlotsMap.entrySet()) {
            extraMemberSlotsConfig.set(entry.getKey(), entry.getValue());
        }
        try {
            extraMemberSlotsConfig.save(extraMemberSlotsFile);
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Erro ao salvar extra_member_slots.yml: " + e.getMessage(), e);
        }
    }

    public static int getExtraMemberSlots(String factionName) {
        return extraMemberSlotsMap.getOrDefault(factionName, 0);
    }

    public static void setExtraMemberSlots(String factionName, int slots) {
        if (slots < 0 || slots > 5) {
            Main.get().getLogger().warning("Tentativa de definir extraMemberSlots inválido (" + slots + ") para " + factionName);
            return;
        }
        extraMemberSlotsMap.put(factionName, slots);
        saveExtraMemberSlots();
    }

    public static boolean addExtraMemberSlot(String factionName) {
        int currentSlots = getExtraMemberSlots(factionName);
        if (currentSlots >= 5) {
            return false;
        }
        setExtraMemberSlots(factionName, currentSlots + 1);
        return true;
    }

    public static void removeExtraMemberSlots(String factionName) {
        extraMemberSlotsMap.remove(factionName);
        saveExtraMemberSlots();
    }
}