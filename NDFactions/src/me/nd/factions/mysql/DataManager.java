package me.nd.factions.mysql;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final String FACTIONS_PATH = "factions.";
    
	public static final Map<String, NDPlayer> players = new HashMap<>();
	public static final Map<String, NDFaction> factions = new HashMap<>();
    
    public enum FactionRankingType {
        VALOR, COINS, KDR, SPAWNERS, PODER, INVASOES
    }
    
    public static final Map<FactionRankingType, List<NDFaction>> factionRankings = new EnumMap<>(FactionRankingType.class);
    
    static {
        for (FactionRankingType type : FactionRankingType.values()) {
            factionRankings.put(type, new ArrayList<>());
        }
    }
    
    private static final Map<String, Integer> extraMemberSlotsMap = new ConcurrentHashMap<>();
    public static File extraMemberSlotsFile;
    public static FileConfiguration extraMemberSlotsConfig;
    public static File spawnPointsFile;
    public static FileConfiguration spawnPointsConfig;
    public static File rostersFile;
    public static FileConfiguration rostersConfig;
    public static File homesFile;
    public static FileConfiguration homesConfig;
    public static File vaultsFile;
    public static FileConfiguration vaultsConfig;
    public static File factionDataFile;
    public static FileConfiguration factionDataConfig;
    public static File offersFile;
    public static FileConfiguration offersConfig;
    public static File attacksFile;
    public static FileConfiguration attacksConfig;

    private static void asyncSave(FileConfiguration config, File file, String errorMessage) {
        if (executor.isShutdown()) {
            try {
                config.save(file);
                Main.get().getLogger().info("Synchronously saved " + file.getName() + " due to executor shutdown");
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, errorMessage, e);
            }
            return;
        }
        executor.submit(() -> {
            try {
                config.save(file);
            } catch (IOException e) {
                Main.get().getLogger().log(Level.SEVERE, errorMessage, e);
            }
        });
    }
    
    public static void setRanking(FactionRankingType type, List<NDFaction> factions) {
        factionRankings.put(type, new ArrayList<>(factions));
    }
    
    public static List<NDFaction> getRanking(FactionRankingType type) {
        return new ArrayList<>(factionRankings.getOrDefault(type, new ArrayList<>()));
    }
    
    public static void initAttacksFile() {
        if (attacksFile == null) {
            attacksFile = new File(Main.get().getDataFolder(), "attacks.yml");
            if (!attacksFile.exists()) {
                try {
                    attacksFile.createNewFile();
                    Main.get().getLogger().info("Created new attacks.yml");
                } catch (IOException e) {
                    Main.get().getLogger().log(Level.SEVERE, "Error creating attacks.yml: " + e.getMessage(), e);
                }
            }
            attacksConfig = YamlConfiguration.loadConfiguration(attacksFile);
        }
    }
    
    public static void saveAttack(String defenderName, String attackerName) {
        if (defenderName == null || attackerName == null) return;
        
        initAttacksFile();
        attacksConfig.set(FACTIONS_PATH + defenderName + ".attacker", attackerName);
        asyncSave(attacksConfig, attacksFile, "Error saving attack for " + defenderName + " in attacks.yml: ");
    }
    
    public static String loadAttacker(String defenderName) {
        if (defenderName == null) return null;
        
        initAttacksFile();
        return attacksConfig.getString(FACTIONS_PATH + defenderName + ".attacker");
    }
    
    public static void removeAttack(String defenderName) {
        if (defenderName == null) return;
        
        initAttacksFile();
        attacksConfig.set(FACTIONS_PATH + defenderName, null);
        asyncSave(attacksConfig, attacksFile, "Error removing attack for " + defenderName + " in attacks.yml: ");
    }
    
    public static void initOffersFile() {
        if (offersFile == null) {
            offersFile = new File(Main.get().getDataFolder(), "offers.yml");
            try {
                if (!offersFile.exists()) {
                    offersFile.createNewFile();
                    Main.get().getLogger().info("Created new offers.yml");
                }
                offersConfig = YamlConfiguration.loadConfiguration(offersFile);
            } catch (Exception e) {
                Main.get().getLogger().log(Level.SEVERE, "Error loading offers.yml: " + e.getMessage(), e);
                File corruptedFile = new File(Main.get().getDataFolder(), "offers_corrupted_" + System.currentTimeMillis() + ".yml");
                if (offersFile.renameTo(corruptedFile)) {
                    Main.get().getLogger().info("Corrupted offers.yml renamed to " + corruptedFile.getName());
                }
                try {
                    offersFile.createNewFile();
                    offersConfig = YamlConfiguration.loadConfiguration(offersFile);
                } catch (IOException ex) {
                    Main.get().getLogger().log(Level.SEVERE, "Error creating new offers.yml: " + ex.getMessage(), ex);
                }
            }
        }
    }
    
    public static List<String> getAllFactionNames() {
        initOffersFile();
        if (offersConfig.contains("factions")) {
            return new ArrayList<>(offersConfig.getConfigurationSection("factions").getKeys(false));
        }
        return new ArrayList<>();
    }
    
    public static void saveOffer(String factionName, EntityType mob, int amount, double cost, long expirationTime) {
        if (factionName == null || mob == null) return;
        
        initOffersFile();
        String path = FACTIONS_PATH + factionName + ".offers";
        List<Map<String, Object>> offers = loadOffers(factionName);
        boolean updated = false;
        
        for (Map<String, Object> existingOffer : offers) {
            if (mob.equals(existingOffer.get("mob"))) {
                int newAmount = (int) existingOffer.get("amount") + amount;
                double newCost = (double) existingOffer.get("cost") + cost;
                long existingExpiration = (long) existingOffer.get("expirationTime");
                existingOffer.put("amount", newAmount);
                existingOffer.put("cost", newCost);
                existingOffer.put("expirationTime", Math.max(existingExpiration, expirationTime));
                updated = true;
                break;
            }
        }
        
        if (!updated) {
            Map<String, Object> newOffer = new HashMap<>();
            newOffer.put("mob", mob);
            newOffer.put("amount", amount);
            newOffer.put("cost", cost);
            newOffer.put("expirationTime", expirationTime);
            offers.add(newOffer);
        }
        
        List<Map<String, Object>> offersToSave = new ArrayList<>();
        for (Map<String, Object> offer : offers) {
            Map<String, Object> toSave = new HashMap<>();
            toSave.put("mob", ((EntityType) offer.get("mob")).name());
            toSave.put("amount", offer.get("amount"));
            toSave.put("cost", offer.get("cost"));
            toSave.put("expirationTime", offer.get("expirationTime"));
            offersToSave.add(toSave);
        }
        
        offersConfig.set(path, offersToSave);
        asyncSave(offersConfig, offersFile, "Error saving offer for " + factionName + " in offers.yml: ");
    }
    
    public static List<Map<String, Object>> loadOffers(String factionName) {
        if (factionName == null) return new ArrayList<>();
        
        initOffersFile();
        String path = FACTIONS_PATH + factionName + ".offers";
        List<Map<String, Object>> offers = new ArrayList<>();
        
        if (offersConfig.contains(path)) {
            for (Map<?, ?> rawOffer : offersConfig.getMapList(path)) {
                try {
                    Map<String, Object> offer = new HashMap<>();
                    String mobName = ((String) rawOffer.get("mob")).trim().toUpperCase();
                    EntityType mob = EntityType.valueOf(mobName);
                    offer.put("mob", mob);
                    
                    Object amountObj = rawOffer.get("amount");
                    Object costObj = rawOffer.get("cost");
                    Object expirationObj = rawOffer.get("expirationTime");
                    
                    if (amountObj instanceof Number && costObj instanceof Number && expirationObj instanceof Number) {
                        offer.put("amount", ((Number) amountObj).intValue());
                        offer.put("cost", ((Number) costObj).doubleValue());
                        offer.put("expirationTime", ((Number) expirationObj).longValue());
                        offers.add(offer);
                    }
                } catch (Exception e) {
                    Main.get().getLogger().warning("Error loading offer for " + factionName + ": " + e.getMessage());
                }
            }
        }
        return offers;
    }
    
    public static void removeOffer(String factionName, int index) {
        if (factionName == null) return;
        
        initOffersFile();
        String path = FACTIONS_PATH + factionName + ".offers";
        List<Map<String, Object>> offers = loadOffers(factionName);
        
        if (index >= 0 && index < offers.size()) {
            offers.remove(index);
            offersConfig.set(path, offers.isEmpty() ? null : offers);
            asyncSave(offersConfig, offersFile, "Error removing offer for " + factionName + " in offers.yml: ");
        }
    }
    
    public static boolean isOfferValid(String factionName, int index) {
        if (factionName == null) return false;
        
        List<Map<String, Object>> offers = loadOffers(factionName);
        if (index < 0 || index >= offers.size()) return false;
        
        return System.currentTimeMillis() < (long) offers.get(index).get("expirationTime");
    }
    
    public static void initFactionDataFile() {
        if (factionDataFile == null) {
            factionDataFile = new File(Main.get().getDataFolder(), "faction_data.yml");
            if (!factionDataFile.exists()) {
                try {
                    factionDataFile.createNewFile();
                    Main.get().getLogger().info("Created new faction_data.yml");
                } catch (IOException e) {
                    Main.get().getLogger().log(Level.SEVERE, "Error creating faction_data.yml: " + e.getMessage(), e);
                }
            }
            factionDataConfig = YamlConfiguration.loadConfiguration(factionDataFile);
        }
    }
    
    public static void loadFactionData(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return;
        
        initFactionDataFile();
        String path = FACTIONS_PATH + faction.getNome();
        Main.get().getLogger().info(factionDataConfig.contains(path) 
            ? "Loaded data for " + faction.getNome()
            : "No data found for " + faction.getNome() + " in faction_data.yml");
    }
    
    public static void saveFactionData(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return;
        
        initFactionDataFile();
        asyncSave(factionDataConfig, factionDataFile, "Error saving faction_data.yml for " + faction.getNome() + ": ");
    }
    
    public static void removeFactionData(String factionName) {
        if (factionName == null || factionName.isEmpty()) return;
        
        initFactionDataFile();
        factionDataConfig.set(FACTIONS_PATH + factionName, null);
        asyncSave(factionDataConfig, factionDataFile, "Error removing data for " + factionName + " in faction_data.yml: ");
    }
    
    public static boolean canActivateImmunity(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return false;
        
        initFactionDataFile();
        String path = FACTIONS_PATH + faction.getNome();
        long currentTime = System.currentTimeMillis();
        long twelveHoursInMillis = 12 * 60 * 60 * 1000;
        long lastImmunityEnd = factionDataConfig.getLong(path + ".lastImmunityEnd", 0);
        
        return !hasImmunity(faction) && (lastImmunityEnd == 0 || currentTime >= lastImmunityEnd + twelveHoursInMillis);
    }
    
    public static long getRemainingImmunityTime(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return 0;
        
        initFactionDataFile();
        String path = FACTIONS_PATH + faction.getNome();
        long immunityEndTime = factionDataConfig.getLong(path + ".immunityEndTime", 0);
        return Math.max(0, (immunityEndTime - System.currentTimeMillis()) / 1000);
    }
    
    public static long getImmunityCooldown(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return 0;
        
        initFactionDataFile();
        String path = FACTIONS_PATH + faction.getNome();
        long currentTime = System.currentTimeMillis();
        long twelveHoursInMillis = 12 * 60 * 60 * 1000;
        long lastImmunityEnd = factionDataConfig.getLong(path + ".lastImmunityEnd", 0);
        
        return Math.max(0, ((lastImmunityEnd + twelveHoursInMillis) - currentTime) / 1000);
    }
    
    public static void activateImmunity(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return;
        
        if (canActivateImmunity(faction)) {
            initFactionDataFile();
            String path = FACTIONS_PATH + faction.getNome();
            long currentTime = System.currentTimeMillis();
            long thirtyMinutesInMillis = 30 * 60 * 1000;
            
            factionDataConfig.set(path + ".isImmune", true);
            factionDataConfig.set(path + ".immunityEndTime", currentTime + thirtyMinutesInMillis);
            factionDataConfig.set(path + ".spawnerBreakDelayStart", currentTime);
            
            asyncSave(factionDataConfig, factionDataFile, "Error saving immunity for " + faction.getNome() + ": ");
            Main.get().getLogger().info("Immunity activated for faction " + faction.getNome());
        }
    }
    
    public static boolean canBreakSpawners(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return true;
        
        initFactionDataFile();
        String path = FACTIONS_PATH + faction.getNome();
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = 60 * 60 * 1000;
        long spawnerBreakDelayStart = factionDataConfig.getLong(path + ".spawnerBreakDelayStart", 0);
        
        return spawnerBreakDelayStart == 0 || currentTime >= spawnerBreakDelayStart + oneHourInMillis;
    }
    
    public static long getRemainingBreakDelay(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return 0;
        
        initFactionDataFile();
        String path = FACTIONS_PATH + faction.getNome();
        long oneHourInMillis = 60 * 60 * 1000;
        long currentTime = System.currentTimeMillis();
        long spawnerBreakDelayStart = factionDataConfig.getLong(path + ".spawnerBreakDelayStart", 0);
        
        return Math.max(0, (spawnerBreakDelayStart + oneHourInMillis) - currentTime);
    }
    
    public static boolean hasImmunity(NDFaction faction) {
        if (faction == null || faction.getNome() == null || faction.getNome().isEmpty()) return false;
        
        initFactionDataFile();
        String path = FACTIONS_PATH + faction.getNome();
        boolean isImmune = factionDataConfig.getBoolean(path + ".isImmune", false);
        long immunityEndTime = factionDataConfig.getLong(path + ".immunityEndTime", 0);
        long currentTime = System.currentTimeMillis();
        
        if (isImmune && currentTime > immunityEndTime) {
            factionDataConfig.set(path + ".isImmune", false);
            factionDataConfig.set(path + ".immunityEndTime", 0);
            factionDataConfig.set(path + ".lastImmunityEnd", currentTime);
            asyncSave(factionDataConfig, factionDataFile, "Error saving immunity expiration for " + faction.getNome() + ": ");
            Main.get().getLogger().info("Immunity expired for faction " + faction.getNome());
            return false;
        }
        return isImmune;
    }
    
    public static void initVaultsFile() {
        if (vaultsFile == null) {
            vaultsFile = new File(Main.get().getDataFolder(), "vaults.yml");
            if (!vaultsFile.exists()) {
                try {
                    vaultsFile.createNewFile();
                } catch (IOException e) {
                    Main.get().getLogger().log(Level.SEVERE, "Error creating vaults.yml: " + e.getMessage(), e);
                }
            }
            vaultsConfig = YamlConfiguration.loadConfiguration(vaultsFile);
        }
    }
    
    public static Inventory loadVault(String factionName) {
        if (factionName == null) return Bukkit.createInventory(null, 54, "Baú da Facção - " + factionName);
        
        initVaultsFile();
        String path = FACTIONS_PATH + factionName + ".vault";
        Inventory vault = Bukkit.createInventory(null, 54, "Baú da Facção - " + factionName);
        
        if (vaultsConfig.contains(path)) {
            List<?> items = vaultsConfig.getList(path);
            if (items != null) {
                for (int i = 0; i < items.size() && i < vault.getSize(); i++) {
                    if (items.get(i) instanceof ItemStack) {
                        vault.setItem(i, (ItemStack) items.get(i));
                    }
                }
            }
        }
        return vault;
    }
    
    public static void saveVault(String factionName, Inventory vault) {
        if (factionName == null || vault == null) return;
        
        initVaultsFile();
        String path = FACTIONS_PATH + factionName + ".vault";
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : vault.getContents()) {
            if (item != null) items.add(item);
        }
        
        vaultsConfig.set(path, items);
        asyncSave(vaultsConfig, vaultsFile, "Error saving vaults.yml for " + factionName + ": ");
    }
    
    public static void saveVaultAsync(String factionName, Inventory vault) {
        saveVault(factionName, vault);
    }
    
    public static List<VaultLog> loadVaultLogs(String factionName) {
        if (factionName == null) return new ArrayList<>();
        
        initVaultsFile();
        String path = FACTIONS_PATH + factionName + ".logs";
        List<VaultLog> logs = new ArrayList<>();
        
        if (vaultsConfig.contains(path)) {
            List<?> rawLogs = vaultsConfig.getList(path);
            if (rawLogs != null) {
                for (Object obj : rawLogs) {
                    if (obj instanceof Map) {
                        Map<?, ?> logData = (Map<?, ?>) obj;
                        try {
                            String playerName = (String) logData.get("player");
                            String action = (String) logData.get("action");
                            long timestamp = ((Number) logData.get("timestamp")).longValue();
                            logs.add(new VaultLog(playerName, action, timestamp));
                        } catch (Exception e) {
                            Main.get().getLogger().warning("Error loading vault log for " + factionName + ": " + e.getMessage());
                        }
                    }
                }
            }
        }
        return logs;
    }
    
    public static void saveVaultLogs(String factionName, List<VaultLog> logs) {
        if (factionName == null || logs == null) return;
        
        initVaultsFile();
        String path = FACTIONS_PATH + factionName + ".logs";
        List<Map<String, Object>> serializedLogs = new ArrayList<>();
        
        for (VaultLog log : logs) {
            Map<String, Object> logData = new HashMap<>();
            logData.put("player", log.getPlayerName());
            logData.put("action", log.getAction());
            logData.put("timestamp", log.getTimestamp());
            serializedLogs.add(logData);
        }
        
        vaultsConfig.set(path, serializedLogs);
        asyncSave(vaultsConfig, vaultsFile, "Error saving vault logs for " + factionName + ": ");
    }
    
    public static void saveVaultLogsAsync(String factionName, List<VaultLog> logs) {
        saveVaultLogs(factionName, logs);
    }
    
    public static void removeVault(String factionName) {
        if (factionName == null) return;
        
        initVaultsFile();
        vaultsConfig.set(FACTIONS_PATH + factionName, null);
        asyncSave(vaultsConfig, vaultsFile, "Error removing vault for " + factionName + " in vaults.yml: ");
    }
    
    public static void initHomesFile() {
        if (homesFile == null) {
            homesFile = new File(Main.get().getDataFolder(), "homes.yml");
            if (!homesFile.exists()) {
                try {
                    homesFile.createNewFile();
                } catch (IOException e) {
                    Main.get().getLogger().log(Level.SEVERE, "Error creating homes.yml: " + e.getMessage(), e);
                }
            }
            homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        }
    }
    public static void shutdown() {
        // Save all files synchronously
        try {
            if (homesConfig != null && homesFile != null) {
                homesConfig.save(homesFile);
            }
            if (factionDataConfig != null && factionDataFile != null) {
                factionDataConfig.save(factionDataFile);
            }
            if (vaultsConfig != null && vaultsFile != null) {
                vaultsConfig.save(vaultsFile);
            }
            if (rostersConfig != null && rostersFile != null) {
                rostersConfig.save(rostersFile);
            }
            if (spawnPointsConfig != null && spawnPointsFile != null) {
                spawnPointsConfig.save(spawnPointsFile);
            }
            if (offersConfig != null && offersFile != null) {
                offersConfig.save(offersFile);
            }
            if (attacksConfig != null && attacksFile != null) {
                attacksConfig.save(attacksFile);
            }
            if (extraMemberSlotsConfig != null && extraMemberSlotsFile != null) {
                extraMemberSlotsConfig.save(extraMemberSlotsFile);
            }
        } catch (IOException e) {
            Main.get().getLogger().log(Level.SEVERE, "Error saving data on shutdown: ", e);
        }

        // Shut down executor and wait for tasks
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                Main.get().getLogger().warning("Some async tasks did not complete within 10 seconds during shutdown.");
                executor.shutdownNow(); // Forcefully terminate remaining tasks
            } else {
                Main.get().getLogger().info("All async tasks completed during shutdown.");
            }
        } catch (InterruptedException e) {
            Main.get().getLogger().log(Level.SEVERE, "Interrupted while waiting for async tasks to complete: ", e);
            executor.shutdownNow();
        }
    }
    public static Map<String, Location> loadHomes(String factionName) {
        if (factionName == null) return new HashMap<>();
        
        initHomesFile();
        Map<String, Location> homes = new HashMap<>();
        String path = FACTIONS_PATH + factionName + ".homes";
        
        if (homesConfig.contains(path)) {
            for (String homeName : homesConfig.getConfigurationSection(path).getKeys(false)) {
                Location location = (Location) homesConfig.get(path + "." + homeName);
                if (location != null) {
                    // Ensure the world is loaded
                    if (location.getWorld() == null) {
                        String worldName = location.getWorld().getName(); // Get world name from serialized Location
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            location.setWorld(world); // Reassign the world
                        } else {
                            Main.get().getLogger().warning("World '" + worldName + "' not found for home '" + homeName + "' in faction " + factionName);
                            continue;
                        }
                    }
                    homes.put(homeName, location);
                } else {
                    Main.get().getLogger().warning("Invalid location data for home '" + homeName + "' in faction " + factionName);
                }
            }
        }
        
        return homes;
    }
    
    public static void saveHomes(String factionName, Map<String, Location> homes) {
        if (factionName == null || homes == null) return;
        
        initHomesFile();
        String path = FACTIONS_PATH + factionName + ".homes";
        homesConfig.set(path, null);
        
        for (Map.Entry<String, Location> entry : homes.entrySet()) {
            homesConfig.set(path + "." + entry.getKey(), entry.getValue());
        }
        
        asyncSave(homesConfig, homesFile, "Error saving homes.yml for " + factionName + ": ");
    }
    
    public static void setHome(String factionName, String homeName, Location location) {
        if (factionName == null || homeName == null || location == null || location.getWorld() == null) {
            return;
        }
        Map<String, Location> homes = loadHomes(factionName);
        homes.put(homeName, location);
        saveHomes(factionName, homes);
    }
    
    public static void removeHome(String factionName, String homeName) {
        if (factionName == null || homeName == null) return;
        
        Map<String, Location> homes = loadHomes(factionName);
        if (homes.remove(homeName) != null) {
            saveHomes(factionName, homes);
        }
    }
    
    public static void removeHomes(String factionName) {
        if (factionName == null) return;
        
        initHomesFile();
        homesConfig.set(FACTIONS_PATH + factionName + ".homes", null);
        asyncSave(homesConfig, homesFile, "Error removing homes for " + factionName + " in homes.yml: ");
    }
    
    public static Location getHome(String factionName, String homeName) {
        if (factionName == null || homeName == null) return null;
        return loadHomes(factionName).get(homeName);
    }
    
    public static void initRostersFile() {
        if (rostersFile == null) {
            rostersFile = new File(Main.get().getDataFolder(), "rosters.yml");
            if (!rostersFile.exists()) {
                try {
                    rostersFile.createNewFile();
                } catch (IOException e) {
                    Main.get().getLogger().log(Level.SEVERE, "Error creating rosters.yml: " + e.getMessage(), e);
                }
            }
            rostersConfig = YamlConfiguration.loadConfiguration(rostersFile);
        }
    }
    
    public static List<String> loadRoster(String factionName) {
        if (factionName == null) return new ArrayList<>();
        
        initRostersFile();
        return new ArrayList<>(rostersConfig.getStringList(FACTIONS_PATH + factionName + ".roster"));
    }
    
    public static void saveRoster(String factionName, List<String> roster) {
        if (factionName == null || roster == null) return;
        
        initRostersFile();
        rostersConfig.set(FACTIONS_PATH + factionName + ".roster", new ArrayList<>(roster));
        asyncSave(rostersConfig, rostersFile, "Error saving rosters.yml for " + factionName + ": ");
    }
    
    public static void addToRoster(String factionName, String playerName) {
        if (factionName == null || playerName == null) return;
        
        List<String> roster = loadRoster(factionName);
        if (!roster.contains(playerName)) {
            roster.add(playerName);
            saveRoster(factionName, roster);
        }
    }
    
    public static void removeFromRoster(String factionName, String playerName) {
        if (factionName == null || playerName == null) return;
        
        List<String> roster = loadRoster(factionName);
        if (roster.remove(playerName)) {
            saveRoster(factionName, roster);
        }
    }
    
    public static boolean isInRoster(String factionName, String playerName) {
        if (factionName == null || playerName == null) return false;
        return loadRoster(factionName).contains(playerName);
    }
    
    public static void removeRoster(String factionName) {
        if (factionName == null) return;
        
        initRostersFile();
        rostersConfig.set(FACTIONS_PATH + factionName, null);
        asyncSave(rostersConfig, rostersFile, "Error removing roster for " + factionName + " in rosters.yml: ");
    }
    
    public static void initSpawnPointsFile() {
        if (spawnPointsFile == null) {
            spawnPointsFile = new File(Main.get().getDataFolder(), "spawn_points.yml");
            if (!spawnPointsFile.exists()) {
                try {
                    spawnPointsFile.createNewFile();
                } catch (IOException e) {
                    Main.get().getLogger().log(Level.SEVERE, "Error creating spawn_points.yml: " + e.getMessage(), e);
                }
            }
            spawnPointsConfig = YamlConfiguration.loadConfiguration(spawnPointsFile);
        }
    }
    
    public static Map<EntityType, Location> loadSpawnPoints(String factionName) {
        if (factionName == null) return new HashMap<>();
        
        initSpawnPointsFile();
        Map<EntityType, Location> spawnPoints = new HashMap<>();
        String path = FACTIONS_PATH + factionName + ".spawn_points";
        
        if (spawnPointsConfig.contains(path)) {
            for (String key : spawnPointsConfig.getConfigurationSection(path).getKeys(false)) {
                try {
                    EntityType entityType = EntityType.valueOf(key);
                    Location location = (Location) spawnPointsConfig.get(path + "." + key);
                    if (location != null && location.getWorld() != null) {
                        spawnPoints.put(entityType, location);
                    } else {
                        Main.get().getLogger().warning("Invalid location for spawn point " + key + " in faction " + factionName);
                    }
                } catch (IllegalArgumentException e) {
                    Main.get().getLogger().warning("Invalid EntityType: " + key + " for faction " + factionName);
                }
            }
        }
        return spawnPoints;
    }
    
    public static void saveSpawnPoints(String factionName, Map<EntityType, Location> spawnPoints) {
        if (factionName == null || spawnPoints == null) return;
        
        initSpawnPointsFile();
        String path = FACTIONS_PATH + factionName + ".spawn_points";
        spawnPointsConfig.set(path, null);
        
        for (Map.Entry<EntityType, Location> entry : spawnPoints.entrySet()) {
            spawnPointsConfig.set(path + "." + entry.getKey().name(), entry.getValue());
        }
        
        asyncSave(spawnPointsConfig, spawnPointsFile, "Error saving spawn_points.yml for " + factionName + ": ");
    }
    
    public static void setSpawnPoint(String factionName, EntityType type, Location location) {
        if (factionName == null || type == null || location == null) return;
        
        Map<EntityType, Location> spawnPoints = loadSpawnPoints(factionName);
        spawnPoints.put(type, location);
        saveSpawnPoints(factionName, spawnPoints);
    }
    
    public static void removeSpawnPoint(String factionName, EntityType type) {
        if (factionName == null || type == null) return;
        
        Map<EntityType, Location> spawnPoints = loadSpawnPoints(factionName);
        if (spawnPoints.remove(type) != null) {
            saveSpawnPoints(factionName, spawnPoints);
        }
    }
    
    public static Location getSpawnPoint(String factionName, EntityType type) {
        if (factionName == null || type == null) return null;
        return loadSpawnPoints(factionName).get(type);
    }
    
    public static void loadExtraMemberSlots() {
        if (!extraMemberSlotsFile.exists()) {
            saveExtraMemberSlots();
        }
        
        for (String factionName : extraMemberSlotsConfig.getKeys(false)) {
            extraMemberSlotsMap.put(factionName, extraMemberSlotsConfig.getInt(factionName, 0));
        }
    }
    
    public static void saveExtraMemberSlots() {
        for (Map.Entry<String, Integer> entry : extraMemberSlotsMap.entrySet()) {
            extraMemberSlotsConfig.set(entry.getKey(), entry.getValue());
        }
        asyncSave(extraMemberSlotsConfig, extraMemberSlotsFile, "Error saving extra_member_slots.yml: ");
    }
    
    public static int getExtraMemberSlots(String factionName) {
        return extraMemberSlotsMap.getOrDefault(factionName, 0);
    }
    
    public static void setExtraMemberSlots(String factionName, int slots) {
        if (factionName == null || slots < 0 || slots > 5) {
            if (factionName != null) {
                Main.get().getLogger().warning("Invalid extraMemberSlots (" + slots + ") for " + factionName);
            }
            return;
        }
        
        extraMemberSlotsMap.put(factionName, slots);
        saveExtraMemberSlots();
    }
    
    public static boolean addExtraMemberSlot(String factionName) {
        if (factionName == null) return false;
        
        int currentSlots = getExtraMemberSlots(factionName);
        if (currentSlots >= 5) return false;
        
        setExtraMemberSlots(factionName, currentSlots + 1);
        return true;
    }
    
    public static void removeExtraMemberSlots(String factionName) {
        if (factionName == null) return;
        
        extraMemberSlotsMap.remove(factionName);
        saveExtraMemberSlots();
    }
}