package me.nd.factions.objetos;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.nd.factions.Main;
import me.nd.factions.addons.SobAtaque;
import me.nd.factions.api.Config;
import me.nd.factions.api.Vault;
import me.nd.factions.enums.Cargo;
import me.nd.factions.enums.Protecao;
import me.nd.factions.enums.Relacao;
import me.nd.factions.factions.API;
import me.nd.factions.listeners.FactionGenerators;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.Methods;
import me.nd.factions.utils.Utils;

public class NDFaction {

    private List<String> aliados = new ArrayList<>();
    private List<String> inimigos = new ArrayList<>();
    private double banco;
    private Location base;
    private List<String> capitoes = new ArrayList<>();
    private List<String> membros = new ArrayList<>();
    private List<String> recrutas = new ArrayList<>();
    private String lider;
    private String nome;
    private List<NDFaction> pedidos = new ArrayList<>();
    private List<NDFaction> pedidos3 = new ArrayList<>();
    private String tag;
    private List<Terra> temporarios = new ArrayList<>();
    private final Map<EntityType, Integer> cacheSpawners = new HashMap<>();
    private List<Terra> terras = new ArrayList<>();
    private Map<Cargo, Map<String, Boolean>> permissoes = new HashMap<>();
    private Map<Relacao, Map<String, Boolean>> permissoesRelacoes = new HashMap<>();
    private Map<String, Map<String, Boolean>> permissoesMembros = new HashMap<>();
    private Map<EntityType, Integer> storedGenerators = new HashMap<>();
    private Map<EntityType, Integer> placedGenerators = new HashMap<>();
    private List<GeneratorLog> generatorLogs = new ArrayList<>();
    private Map<String, Location> homes;
    private double cachedTotalMoney = -1;
    private long lastMoneyCalcTime = 0;
    private static final long MONEY_CACHE_INTERVAL = 60000;
    private List<VaultLog> vaultLogs = new ArrayList<>();
    private List<RelationLog> relationLogs = new ArrayList<>();
    private Map<NDFaction, Long> activeTruces = new HashMap<>(); // Tréguas ativas com tempo de expiração
    private int successfulInvasions; // Tracks the number of successful invasions
    private Map<EntityType, Integer> destroyedSpawners; // Tracks spawners destroyed during invasio
    
    public NDFaction(String nome, Location base, double banco, List<Terra> terras, List<String> aliados,
                     String lider, List<String> capitoes, List<String> membros, List<String> recrutas, String tag,
                     List<String> inimigos) {
        this.nome = nome;
        this.base = base;
        this.banco = banco;
        this.terras = terras;
        this.aliados = aliados;
        this.lider = lider;
        this.capitoes = capitoes;
        this.membros = membros;
        this.recrutas = recrutas;
        this.tag = tag != null ? tag : nome;
        this.inimigos = inimigos;
        this.pedidos = new ArrayList<>();
        this.pedidos3 = new ArrayList<>();
        this.temporarios = new ArrayList<>();
        this.permissoes = new HashMap<>();
        this.permissoesRelacoes = new HashMap<>();
        this.permissoesMembros = new HashMap<>();
        this.homes = new HashMap<>();
        this.placedGenerators = new HashMap<>();
        this.storedGenerators = new HashMap<>();
        this.successfulInvasions = 0;
        this.destroyedSpawners = new HashMap<>();
        setupPermissoes();
    }
    
    private void setupPermissoes() {
        for (Cargo cargo : new Cargo[]{Cargo.Recruta, Cargo.Membro, Cargo.Capitão}) {
            Map<String, Boolean> cargoPerms = new HashMap<>();
            cargoPerms.put("abrir_bau", false);
            cargoPerms.put("abrir_porta", false);
            cargoPerms.put("apertar_botao", false);
            cargoPerms.put("teleportar", false);
            cargoPerms.put("colocar_bloco", false);
            cargoPerms.put("quebrar_bloco", false);
            permissoes.put(cargo, cargoPerms);
        }

        for (Relacao relacao : new Relacao[]{Relacao.Aliada, Relacao.Neutra, Relacao.Inimiga}) {
            Map<String, Boolean> relacaoPerms = new HashMap<>();
            relacaoPerms.put("abrir_bau", false);
            relacaoPerms.put("abrir_porta", false);
            relacaoPerms.put("apertar_botao", false);
            relacaoPerms.put("teleportar", false);
            relacaoPerms.put("colocar_bloco", false);
            relacaoPerms.put("quebrar_bloco", false);
            permissoesRelacoes.put(relacao, relacaoPerms);
        }

        for (String member : getAllMembers()) {
            Map<String, Boolean> memberPerms = new HashMap<>();
            permissoesMembros.put(member, memberPerms);
        }
    }
    
    public static class GeneratorLog {
        private final String playerName;
        private final String action;
        private final EntityType generatorType;
        private final int amount;
        private final long timestamp;

        public GeneratorLog(String playerName, String action, EntityType generatorType, int amount, long timestamp) {
            this.playerName = playerName;
            this.action = action;
            this.generatorType = generatorType;
            this.amount = amount;
            this.timestamp = timestamp;
        }

        public String getPlayerName() { return playerName; }
        public String getAction() { return action; }
        public EntityType getGeneratorType() { return generatorType; }
        public int getAmount() { return amount; }
        public long getTimestamp() { return timestamp; }
    }

    public static class RelationLog {
        private final String factionName;
        private final Relacao relation;
        private final String action;
        private final long timestamp;

        public RelationLog(String factionName, Relacao relation, String action, long timestamp) {
            this.factionName = factionName;
            this.relation = relation;
            this.action = action;
            this.timestamp = timestamp;
        }

        public String getFactionName() { return factionName; }
        public Relacao getRelation() { return relation; }
        public String getAction() { return action; }
        public long getTimestamp() { return timestamp; }
    }

    public static class VaultLog {
        private final String playerName;
        private final String action;
        private final long timestamp;

        public VaultLog(String playerName, String action, long timestamp) {
            this.playerName = playerName;
            this.action = action;
            this.timestamp = timestamp;
        }

        public String getPlayerName() { return playerName; }
        public String getAction() { return action; }
        public long getTimestamp() { return timestamp; }
    }

    public static class TaxResult {
        private final double moneyDeducted;
        private final Map<EntityType, Integer> spawnersDeducted;
        private final boolean success;

        public TaxResult(double moneyDeducted, Map<EntityType, Integer> spawnersDeducted, boolean success) {
            this.moneyDeducted = moneyDeducted;
            this.spawnersDeducted = spawnersDeducted;
            this.success = success;
        }

        public double getMoneyDeducted() { return moneyDeducted; }
        public Map<EntityType, Integer> getSpawnersDeducted() { return spawnersDeducted; }
        public boolean isSuccess() { return success; }
    }
    
    public int getSuccessfulInvasions() {
        return successfulInvasions;
    }

    public void setSuccessfulInvasions(int successfulInvasions) {
        this.successfulInvasions = successfulInvasions;
    }

    public Map<EntityType, Integer> getDestroyedSpawners() {
        return new HashMap<>(destroyedSpawners);
    }

    public void incrementSuccessfulInvasions() {
        this.successfulInvasions++;
        try {
            API.updateSuccessfulInvasions(this); // Use API instead of Methods
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Erro ao salvar successfulInvasions para " + nome + ": " + e.getMessage());
        }
    }

    public void addDestroyedSpawner(EntityType type, int amount) {
        if (type == null) {
            Bukkit.getLogger().warning("Tentativa de adicionar spawner destruído com tipo nulo para a facção " + nome);
            return;
        }
        if (amount <= 0) {
            Bukkit.getLogger().warning("Tentativa de adicionar spawner destruído com quantidade inválida (" + amount + ") para a facção " + nome);
            return;
        }
        destroyedSpawners.merge(type, amount, Integer::sum);
        cachedTotalMoney = -1; // Invalidate cache since spawner changes affect value
        try {
            API.updateDestroyedSpawners(this); // Use API instead of Methods
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Erro ao salvar destroyedSpawners para " + nome + ": " + e.getMessage());
        }
    }
    
    public void resetDestroyedSpawners() {
        destroyedSpawners.clear();
        cachedTotalMoney = -1;
        try {
            API.updateDestroyedSpawners(this);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Erro ao limpar destroyedSpawners para " + nome + ": " + e.getMessage());
        }
    }

    public void setDestroyedSpawners(Map<EntityType, Integer> destroyedSpawners) {
        if (destroyedSpawners == null) {
            this.destroyedSpawners = new HashMap<>();
        } else {
            this.destroyedSpawners = new HashMap<>(destroyedSpawners);
        }
        cachedTotalMoney = -1; // Invalidate cache since spawner changes affect value
        try {
            API.updateDestroyedSpawners(this); // Use API instead of Methods
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Erro ao salvar destroyedSpawners para " + nome + ": " + e.getMessage());
        }
    }
    
    public void clearHomes() {
        homes.clear();
    }
    
    public double getInvasionDamage() {
        if (cachedTotalMoney != -1 && (System.currentTimeMillis() - lastMoneyCalcTime) < MONEY_CACHE_INTERVAL) {
            return cachedTotalMoney;
        }
        double total = 0;
        for (Map.Entry<EntityType, Integer> entry : destroyedSpawners.entrySet()) {
            EntityType type = entry.getKey();
            int quantity = entry.getValue();
            Object configValue = Config.get("Top.Spawners." + type.toString());
            double price = 0;
            if (configValue != null) {
                try {
                    if (configValue instanceof Number) {
                        price = ((Number) configValue).doubleValue();
                    } else if (configValue instanceof String) {
                        price = Double.parseDouble((String) configValue);
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Falha ao parsear preço para Top.Spawners." + type + " na facção " + nome + ": " + configValue + " - " + e.getMessage());
                }
            } else {
                Bukkit.getLogger().warning("Preço não encontrado para Top.Spawners." + type + " na facção " + nome);
            }
            total += quantity * price;
        }
        cachedTotalMoney = total;
        lastMoneyCalcTime = System.currentTimeMillis();
        return total;
    }

    public List<RelationLog> getRelationLogs() {
        return new ArrayList<>(relationLogs);
    }

    public void addRelationLog(String factionName, Relacao relation, String action) {
        relationLogs.add(new RelationLog(factionName, relation, action, System.currentTimeMillis()));
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving relation log for " + nome + ": " + e.getMessage());
        }
    }

    public void clearRelationLogs() {
        relationLogs.clear();
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error clearing relation logs for " + nome + ": " + e.getMessage());
        }
    }

    public boolean isZonaProtegida(Location location, NDPlayer player) {
        if (location == null || player == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk(), player.getPlayer());
        return !(protection == Protecao.Protegida || protection == Protecao.Guerra);
    }

    public boolean isZonaProtegida(Location location) {
        if (location == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk());
        return !(protection == Protecao.Protegida || protection == Protecao.Guerra);
    }

    public boolean isInFactionTerritory(Player player) {
        if (player == null || player.getLocation() == null) {
            return false;
        }

        Location location = player.getLocation();
        Terra terra = new Terra(location.getWorld(), location.getChunk().getX(), location.getChunk().getZ());
        return ownsTerritory(terra);
    }

    public Boolean hasPermissaoMembro(String playerName, String permissao) {
        Map<String, Boolean> memberPerms = permissoesMembros.get(playerName);
        if (memberPerms != null && memberPerms.containsKey(permissao)) {
            return memberPerms.get(permissao);
        }
        return null; // Retorna null se não houver permissão individual
    }
    public void setPermissaoMembro(String playerName, String permissao, boolean valor) {
        Map<String, Boolean> memberPerms = permissoesMembros.computeIfAbsent(playerName, k -> new HashMap<>());
        memberPerms.put(permissao, valor);
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar permissão '" + permissao + "' para o membro " + playerName + " na facção " + nome + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removePermissaoMembro(String playerName, String permissao) {
        Map<String, Boolean> memberPerms = permissoesMembros.get(playerName);
        if (memberPerms != null) {
            memberPerms.remove(permissao);
            if (memberPerms.isEmpty()) {
                permissoesMembros.remove(playerName);
            }
            try {
                save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao salvar remoção da permissão '" + permissao + "' para o membro " + playerName + " na facção " + nome + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Map<String, Map<String, Boolean>> getPermissoesMembros() {
        return permissoesMembros;
    }
    

    public boolean hasPermissao(Cargo cargo, String permissao, String playerName) {
        // Verifica se há uma permissão individual para o jogador
        Boolean specificPerm = hasPermissaoMembro(playerName, permissao);
        if (specificPerm != null) {
            return specificPerm;
        }
        // Caso contrário, retorna a permissão do cargo
        return permissoes.getOrDefault(cargo, new HashMap<>()).getOrDefault(permissao, false);
    }


    public List<String> getAllMembers() {
        List<String> all = new ArrayList<>();
        if (lider != null) {
            all.add(lider);
        }
        all.addAll(capitoes);
        all.addAll(membros);
        all.addAll(recrutas);
        return all;
    }

    public boolean hasImmunity() {
        return DataManager.hasImmunity(this);
    }

    public boolean canActivateImmunity() {
        return DataManager.canActivateImmunity(this);
    }

    public void activateImmunity() {
        DataManager.activateImmunity(this);
    }

    public boolean canBreakSpawners() {
        return DataManager.canBreakSpawners(this);
    }

    public long getRemainingBreakDelay() {
        return DataManager.getRemainingBreakDelay(this);
    }

    public List<VaultLog> getVaultLogs() {
        return new ArrayList<>(vaultLogs);
    }

    public void setVaultLogs(List<VaultLog> logs) {
        this.vaultLogs = new ArrayList<>(logs);
    }

    public void addVaultLog(String playerName, String action) {
        vaultLogs.add(new VaultLog(playerName, action, System.currentTimeMillis()));
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar log de baú para " + nome + ": " + e.getMessage());
        }
    }

    public Map<String, Location> getHomes() {
        return homes;
    }

    public void setHomes(Map<String, Location> homes) {
        this.homes = homes != null ? homes : new HashMap<>();
    }

    public void addHome(String name, Location location) {
        homes.put(name, location);
    }

    public void removeHome(String name) {
        homes.remove(name);
    }

    public Location getHome(String name) {
        return homes.get(name);
    }

    public boolean hasHome(String name) {
        return homes.containsKey(name);
    }

    public List<String> getRoster() {
        return DataManager.loadRoster(nome);
    }

    public void addToRoster(String playerName) {
        DataManager.addToRoster(nome, playerName);
    }

    public void removeFromRoster(String playerName) {
        DataManager.removeFromRoster(nome, playerName);
    }

    public boolean isInRoster(String playerName) {
        return DataManager.isInRoster(nome, playerName);
    }

    public void setRoster(List<String> roster) {
        DataManager.saveRoster(nome, new ArrayList<>(roster));
    }

    public int getTotalStoredGenerators() {
        return storedGenerators.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getPlacedGenerator() {
        return placedGenerators.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getStoreSpawner() {
        return storedGenerators.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<EntityType, Location> getSpawnPoints() {
        return DataManager.loadSpawnPoints(nome);
    }

    public int getExtraMemberSlots() {
        return DataManager.getExtraMemberSlots(nome);
    }

    public boolean setExtraMemberSlots(int extraMemberSlots) {
        DataManager.setExtraMemberSlots(nome, extraMemberSlots);
        return extraMemberSlots == DataManager.getExtraMemberSlots(nome);
    }

    public boolean addExtraMemberSlot() {
        return DataManager.addExtraMemberSlot(nome);
    }

    public void setSpawnPoint(EntityType type, Location location) {
        if (type == null || location == null || location.getWorld() == null) {
            Bukkit.getLogger().warning("Invalid spawn point data for faction " + nome + ": type=" + type + ", location=" + location);
            return;
        }
        DataManager.setSpawnPoint(nome, type, location);
    }

    public void removeSpawnPoint(EntityType type) {
        if (type == null) {
            Bukkit.getLogger().warning("Attempted to remove null spawn point type in faction " + nome);
            return;
        }
        DataManager.removeSpawnPoint(nome, type);
    }

    public Location getSpawnPoint(EntityType type) {
        return DataManager.getSpawnPoint(nome, type);
    }

    public String getTotalPlacedGeneratorsPerGenerator() {
        if (placedGenerators.isEmpty()) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<EntityType, Integer> entry : placedGenerators.entrySet()) {
            result.append(entry.getValue()).append(", ");
        }
        if (result.length() > 0) {
            result.setLength(result.length() - 2);
        }

        return result.toString();
    }

    public String getTotalPlacedGenerator() {
        if (placedGenerators.isEmpty()) {
            return "0";
        }

        int total = placedGenerators.values().stream().mapToInt(Integer::intValue).sum();
        return String.valueOf(total);
    }

    public int getTotalGenerators() {
        int totalStored = getTotalStoredGenerators();
        int totalPlaced;
        try {
            totalPlaced = Integer.parseInt(getTotalPlacedGenerator());
        } catch (NumberFormatException e) {
            totalPlaced = placedGenerators.values().stream().mapToInt(Integer::intValue).sum();
            Bukkit.getLogger().warning("Erro ao converter getTotalPlacedGenerator para inteiro na facção " + tag + ": " + e.getMessage());
        }
        return totalStored + totalPlaced;
    }

    public List<String> getNameGenerators() {
        List<String> result = new ArrayList<>();
        if (placedGenerators.isEmpty()) {
            result.add("§cNenhum gerador colocado.");
            return result;
        }

        for (Map.Entry<EntityType, Integer> entry : placedGenerators.entrySet()) {
            EntityType type = entry.getKey();
            int quantity = entry.getValue();
            if (quantity > 0) {
                String mobName = FactionGenerators.GENERATOR_NAMES.getOrDefault(type, type.name().toLowerCase().replace("_", " "));
                result.add("  §f• §7" + mobName + ": §f" + quantity);
            }
        }

        return result;
    }

    public Map<EntityType, Integer> getPlacedGeneratorsByType() {
        return new HashMap<>(placedGenerators);
    }

    public double getTotalMoneyEmSpawners() {
        long currentTime = System.currentTimeMillis();
        if (cachedTotalMoney >= 0 && (currentTime - lastMoneyCalcTime) < MONEY_CACHE_INTERVAL) {
            return cachedTotalMoney;
        }

        double total = 0;
        Map<EntityType, Integer> allSpawners = new HashMap<>();
        for (Map.Entry<EntityType, Integer> entry : placedGenerators.entrySet()) {
            allSpawners.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (Map.Entry<EntityType, Integer> entry : storedGenerators.entrySet()) {
            allSpawners.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        for (Map.Entry<EntityType, Integer> entry : allSpawners.entrySet()) {
            EntityType type = entry.getKey();
            int quantity = entry.getValue();
            Object configValue = Config.get("Top.Spawners." + type.toString());
            double price = 0;

            if (configValue != null) {
                try {
                    if (configValue instanceof Number) {
                        price = ((Number) configValue).doubleValue();
                    } else if (configValue instanceof String) {
                        price = Double.parseDouble((String) configValue);
                    } else {
                        Bukkit.getLogger().warning("Invalid price format for Top.Spawners." + type + ": " + configValue);
                    }
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("Failed to parse price for Top.Spawners." + type + ": " + configValue + " - " + e.getMessage());
                }
            } else {
                Bukkit.getLogger().warning("Price not found for Top.Spawners." + type + " in faction " + nome);
            }

            total += quantity * price;
        }

        cachedTotalMoney = total;
        lastMoneyCalcTime = currentTime;
        return total;
    }

    public Map<Cargo, Map<String, Boolean>> getPermissoes() {
        return permissoes;
    }

    public Map<Relacao, Map<String, Boolean>> getPermissoesRelacoes() {
        return permissoesRelacoes;
    }

    public Map<EntityType, Integer> getStoredGenerators() {
        return storedGenerators;
    }

    public Map<EntityType, Integer> getPlacedGenerators() {
        return new HashMap<>(placedGenerators);
    }

    public List<GeneratorLog> getGeneratorLogs() {
        return generatorLogs;
    }

    public void setStoredGenerators(Map<EntityType, Integer> generators) {
        this.storedGenerators = generators;
    }

    public void setPlacedGenerators(Map<EntityType, Integer> generators) {
        this.placedGenerators = generators;
    }

    public void setGeneratorLogs(List<GeneratorLog> logs) {
        this.generatorLogs = logs;
    }

    public void addPlacedGenerator(EntityType type, int amount, String playerName) {
        placedGenerators.merge(type, amount, Integer::sum);
        cacheSpawners.merge(type, amount, Integer::sum);
        if (playerName != null) {
            generatorLogs.add(new GeneratorLog(playerName, "place", type, amount, System.currentTimeMillis()));
        }
        cachedTotalMoney = -1;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar gerador colocado para " + nome + ": " + e.getMessage());
        }
    }

    public void addStoredGenerator(EntityType type, int amount, String playerName) {
        storedGenerators.merge(type, amount, Integer::sum);
        if (playerName != null) {
            generatorLogs.add(new GeneratorLog(playerName, "store", type, amount, System.currentTimeMillis()));
        }
        cachedTotalMoney = -1;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar gerador armazenado para " + nome + ": " + e.getMessage());
        }
    }

    public boolean removeStoredGenerator(EntityType type, int amount, String playerName) {
        Integer current = storedGenerators.get(type);
        if (current == null || current < amount) {
            return false;
        }
        storedGenerators.put(type, current - amount);
        if (storedGenerators.get(type) <= 0) {
            storedGenerators.remove(type);
        }
        if (playerName != null) {
            generatorLogs.add(new GeneratorLog(playerName, "withdraw", type, amount, System.currentTimeMillis()));
        }
        cachedTotalMoney = -1;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao remover gerador armazenado para " + nome + ": " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean removePlacedGenerator(EntityType type, int amount, String playerName) {
        Integer current = placedGenerators.get(type);
        if (current == null || current < amount) {
            return false;
        }
        placedGenerators.put(type, current - amount);
        cacheSpawners.put(type, cacheSpawners.getOrDefault(type, 0) - amount);
        if (placedGenerators.get(type) <= 0) {
            placedGenerators.remove(type);
        }
        if (cacheSpawners.get(type) <= 0) {
            cacheSpawners.remove(type);
        }
        if (playerName != null) {
            generatorLogs.add(new GeneratorLog(playerName, "store", type, amount, System.currentTimeMillis()));
        }
        cachedTotalMoney = -1;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao remover gerador colocado para " + nome + ": " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean ownsTerritory(Terra terra) {
        if (terra == null || terra.getChunk() == null) {
            return false;
        }

        for (Terra t : terras) {
            if (t.equals(terra)) {
                return true;
            }
        }
        for (Terra t : temporarios) {
            if (t.equals(terra)) {
                return true;
            }
        }
        return false;
    }

 // Estrutura para armazenar informações de spawners em cache
    private final Map<Chunk, List<SpawnerInfo>> spawnerCache = new ConcurrentHashMap<>();

    // Classe auxiliar para armazenar informações de spawners
    private static class SpawnerInfo {
        Block block;
        EntityType type;

        SpawnerInfo(Block block, EntityType type) {
            this.block = block;
            this.type = type;
        }
    }

    // Inicializar ou atualizar o cache para um chunk
    private void cacheSpawners(Chunk chunk) {
        List<SpawnerInfo> spawners = new ArrayList<>();
        // Usar getBlockEntities() para obter diretamente os tile entities (spawners)
        for (BlockState state : chunk.getTileEntities()) {
        	if (state instanceof CreatureSpawner) {
        	    CreatureSpawner spawner = (CreatureSpawner) state;
        	    spawners.add(new SpawnerInfo(state.getBlock(), spawner.getSpawnedType()));
        	}
        }
        spawnerCache.put(chunk, spawners);
    }

    // Método otimizado para armazenar um único spawner
    public boolean storeSingleGeneratorFromTerritory(Terra terra, String playerName) {
        if (isSobAtaque() || hasActiveTruce() || !ownsTerritory(terra)) {
            return false;
        }

        Chunk chunk = terra.getChunk();
        
        // Atualizar cache se necessário
        if (!spawnerCache.containsKey(chunk)) {
            cacheSpawners(chunk);
        }

        List<SpawnerInfo> spawners = spawnerCache.get(chunk);
        if (spawners.isEmpty()) {
            return false;
        }

        // Pegar o primeiro spawner disponível
        SpawnerInfo spawnerInfo = spawners.remove(0); // Remove do cache
        addStoredGenerator(spawnerInfo.type, 1, playerName);
        removePlacedGenerator(spawnerInfo.type, 1, null);
        
        // Remover o spawner do mundo
        spawnerInfo.block.setType(Material.AIR);
        
        updateSpawners();
        
        // Atualizar cache se ainda houver spawners
        if (spawners.isEmpty()) {
            spawnerCache.remove(chunk);
        }
        
        return true;
    }

    // Método otimizado para armazenar todos os spawners
    public boolean storeGeneratorsFromTerritory(Terra terra, String playerName) {
        if (isSobAtaque() || hasActiveTruce() || !ownsTerritory(terra)) {
            return false;
        }

        Chunk chunk = terra.getChunk();
        
        // Atualizar cache se necessário
        if (!spawnerCache.containsKey(chunk)) {
            cacheSpawners(chunk);
        }

        List<SpawnerInfo> spawners = spawnerCache.get(chunk);
        if (spawners.isEmpty()) {
            return false;
        }

        Map<EntityType, Integer> collected = new HashMap<>();
        
        // Processar todos os spawners no cache
        for (SpawnerInfo spawnerInfo : spawners) {
            collected.merge(spawnerInfo.type, 1, Integer::sum);
            spawnerInfo.block.setType(Material.AIR);
        }

        // Atualizar inventário do jogador e registros
        for (Map.Entry<EntityType, Integer> entry : collected.entrySet()) {
            addStoredGenerator(entry.getKey(), entry.getValue(), playerName);
            removePlacedGenerator(entry.getKey(), entry.getValue(), null);
        }

        updateSpawners();
        
        // Limpar cache
        spawnerCache.remove(chunk);

        // Log apenas em modo debug ou se necessário
        if (!collected.isEmpty()) {
            Bukkit.getLogger().info("Armazenados " + collected + " em " + terra + " por " + playerName + ". placedGenerators: " + placedGenerators);
        }

        return !collected.isEmpty();
    }

    public boolean addMember(NDPlayer player) {
        if (player == null || player.hasFaction()) {
            return false;
        }

        int currentMembers = getAll().size();
        int maxMembers = getMaxMembers();

        if (currentMembers >= maxMembers) {
            return false;
        }

        recrutas.add(player.getNome());
        player.setFaction(this);

        Map<String, Boolean> memberPerms = new HashMap<>();
        permissoesMembros.put(player.getNome(), memberPerms);

        try {
            Methods.updateFaction(this);
            Methods.updatePlayer(player);
            DataManager.factions.put(this.nome, Methods.getFaction(this.nome));
            return true;
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao adicionar membro à facção " + nome + ": " + e.getMessage());
            return false;
        }
    }

    public void setPermissao(Cargo cargo, String permissao, boolean valor) {
        Map<String, Boolean> cargoPerms = permissoes.computeIfAbsent(cargo, k -> new HashMap<>());
        cargoPerms.put(permissao, valor);
        // Do not update permissoesMembros for members who have custom permissions
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar permissão '" + permissao + "' para " + cargo + " na facção " + nome + ": " + e.getMessage());
        }
    }

    public void setPermissaoRelacao(Relacao relacao, String permissao, boolean valor) {
        Map<String, Boolean> relacaoPerms = permissoesRelacoes.computeIfAbsent(relacao, k -> new HashMap<>());
        relacaoPerms.put(permissao, valor);
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar permissão de relação '" + permissao + "' para " + relacao + " na facção " + nome + ": " + e.getMessage());
        }
    }

    public boolean hasPermissao(Cargo cargo, String permissao) {
        return permissoes.getOrDefault(cargo, new HashMap<>()).getOrDefault(permissao, false);
    }

    public boolean hasPermissaoRelacao(Relacao relacao, String permissao) {
        return permissoesRelacoes.getOrDefault(relacao, new HashMap<>()).getOrDefault(permissao, false);
    }

    public void save() throws Exception {
        Methods.updateFaction(this);
    }

    public boolean promover(NDPlayer target) {
        if (target == null || !target.hasFaction() || !target.getFaction().equals(this)) {
            return false;
        }

        Cargo currentCargo = target.getCargo();
        Cargo newCargo;

        switch (currentCargo) {
            case Recruta:
                newCargo = Cargo.Membro;
                break;
            case Membro:
                newCargo = Cargo.Capitão;
                break;
            case Capitão:
            case Lider:
                return false;
            default:
                return false;
        }

        switch (currentCargo) {
            case Recruta:
                recrutas.remove(target.getNome());
                break;
            case Membro:
                membros.remove(target.getNome());
                break;
            case Capitão:
                capitoes.remove(target.getNome());
                break;
            default:
                break;
        }

        switch (newCargo) {
            case Membro:
                membros.add(target.getNome());
                break;
            case Capitão:
                capitoes.add(target.getNome());
                break;
            default:
                break;
        }

        try {
            Methods.updateFaction(this);
            Methods.updatePlayer(target);
            DataManager.factions.put(this.nome, Methods.getFaction(this.nome));
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar mudanças de promoção no banco de dados para a facção " + nome + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    public boolean rebaixar(NDPlayer target) {
        if (target == null || !target.hasFaction() || !target.getFaction().equals(this)) {
            Bukkit.getLogger().warning("Tentativa de rebaixar jogador inválido ou sem facção: " + (target != null ? target.getNome() : "null"));
            return false;
        }

        Cargo currentCargo = target.getCargo();
        Cargo newCargo;

        switch (currentCargo) {
            case Capitão:
                newCargo = Cargo.Membro;
                break;
            case Membro:
                newCargo = Cargo.Recruta;
                break;
            case Recruta:
            case Lider:
                return false;
            default:
                Bukkit.getLogger().warning("Cargo inválido para rebaixamento: " + currentCargo + " para jogador " + target.getNome());
                return false;
        }

        switch (currentCargo) {
            case Capitão:
                capitoes.remove(target.getNome());
                break;
            case Membro:
                membros.remove(target.getNome());
                break;
            default:
                break;
        }

        switch (newCargo) {
            case Membro:
                membros.add(target.getNome());
                break;
            case Recruta:
                recrutas.add(target.getNome());
                break;
            default:
                break;
        }

        try {
            Methods.updateFaction(this);
            Methods.updatePlayer(target);
            DataManager.factions.put(this.nome, Methods.getFaction(this.nome));
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar mudanças de rebaixamento no banco de dados para a facção " + nome + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    public void declarar(Relacao r, NDFaction f) {
        if (f == null) return;
        pedidos.remove(f);
        pedidos3.remove(f);
        String fName = f.getNome();
        aliados.remove(fName);
        inimigos.remove(fName);

        switch (r) {
            case Aliada:
                aliados.add(fName);
                addRelationLog(fName, Relacao.Aliada, "set");
                break;
            case Inimiga:
                inimigos.add(fName);
                addRelationLog(fName, Relacao.Inimiga, "set");
                break;
            case Neutra:
                addRelationLog(fName, Relacao.Neutra, "set");
                break;
        }
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving faction relation for " + nome + ": " + e.getMessage());
        }
    }

    public boolean isSobAtaque() {
        return SobAtaque.cooldown.contains(this);
    }

    public int getAbates() {
        int i = 0;
        for (NDPlayer p : getAll()) {
            if (p != null)
                i += p.getKills();
        }
        return i;
    }

    public List<NDFaction> getAliados() {
        List<NDFaction> all = new ArrayList<>();
        for (String s : aliados) {
            if (DataManager.factions.get(s) != null)
                all.add(DataManager.factions.get(s));
        }
        return all;
    }

    public List<NDPlayer> getAll() {
        List<NDPlayer> all = new ArrayList<>();
        if (getLider() != null) {
            all.add(getLider());
        }
        if (!getCapitoes().isEmpty())
            all.addAll(getCapitoes());
        if (!getMembros().isEmpty())
            all.addAll(getMembros());
        if (!getRecrutas().isEmpty())
            all.addAll(getRecrutas());
        return all;
    }

    public List<Player> getAllOnline() {
        List<Player> all = new ArrayList<>();
        for (NDPlayer p : getAll()) {
            if (p != null && Bukkit.getPlayer(p.getNome()) != null) {
                all.add(Bukkit.getPlayer(p.getNome()));
            }
        }
        return all;
    }

    public double getBanco() {
        return banco;
    }

    public Location getBase() {
        return base;
    }

    public List<NDPlayer> getCapitoes() {
        List<NDPlayer> all = new ArrayList<>();
        for (String s : capitoes) {
            if (DataManager.players.get(s) != null)
                all.add(DataManager.players.get(s));
        }
        return all;
    }

    public List<NDFaction> getInimigos() {
        List<NDFaction> all = new ArrayList<>();
        for (String s : inimigos) {
            if (DataManager.factions.get(s) != null)
                all.add(DataManager.factions.get(s));
        }
        return all;
    }

    public double getKdr() {
        double i = 0;
        for (NDPlayer p : getAll()) {
            if (p != null) {
                i += p.getKDR();
            }
        }
        return i;
    }

    public NDPlayer getLider() {
        return DataManager.players.get(lider);
    }

    public int getMaxMembers() {
        Object configValue = Config.get("Padrao.MaximoMembrosEmFac");
        int baseMaxMembers = (configValue instanceof Integer) ? (int) configValue : 10;
        return baseMaxMembers + getExtraMemberSlots();
    }

    public List<NDPlayer> getMembros() {
        List<NDPlayer> all = new ArrayList<>();
        for (String s : membros) {
            if (DataManager.players.get(s) != null)
                all.add(DataManager.players.get(s));
        }
        return all;
    }

    public double getMoneyEmSpawners() {
        double total = 0;
        for (Map.Entry<EntityType, Integer> entry : placedGenerators.entrySet()) {
            Object configValue = Config.get("Top.Spawners." + entry.getKey().toString());
            if (configValue != null) {
                try {
                    int valor = (int) configValue;
                    double subtotal = entry.getValue() * valor;
                    total += subtotal;
                } catch (ClassCastException e) {
                    Bukkit.getLogger().warning("Erro ao converter valor de Top.Spawners." + entry.getKey() + ": " + configValue + " - " + e.getMessage());
                }
            } else {
                Bukkit.getLogger().warning("Preço não encontrado para " + entry.getKey() + " em Top.Spawners");
            }
        }
        return total;
    }

    public double getMoneyTotal() {
        double i = 0;
        for (NDPlayer p : getAll()) {
            i += Vault.getPlayerBalance(p.getNome());
        }
        i += banco;
        return i;
    }

    public int getMortes() {
        int i = 0;
        for (NDPlayer p : getAll()) {
            if (p != null)
                i += p.getMortes();
        }
        return i;
    }

    public String getNome() {
        if (getTag() != null && !getTag().isEmpty()) {
            return nome.replaceFirst("(?i)" + Pattern.quote(getTag()) + "\\s+", "");
        }
        return nome;
    }

    public List<NDFaction> getPedidosRelacoesAliados() {
        return pedidos;
    }

    public List<NDFaction> getPedidosRelacoesNeutras() {
        return pedidos3;
    }
    
    public void setPoder(int novoPoder) {
        if (novoPoder < 0) {
            novoPoder = 0; // Evitar poder negativo
        }

        int poderMax = getPoderMax();
        if (novoPoder > poderMax) {
            novoPoder = poderMax; // Limitar ao poder máximo
        }

        int poderAtual = getPoder();
        if (poderAtual == 0 && novoPoder == 0) {
            return; // Evitar divisão por zero ou processamento desnecessário
        }

        List<NDPlayer> membros = getAll();
        if (membros.isEmpty()) {
            return; // Sem membros, nada a fazer
        }

        // Calcular a proporção do novo poder em relação ao poder atual
        double proporcao = poderAtual > 0 ? (double) novoPoder / poderAtual : 0;
        int poderDistribuido = 0;

        // Distribuir o poder proporcionalmente aos membros
        for (NDPlayer p : membros) {
            if (p != null) {
                int poderMembroAtual = p.getPoder();
                int novoPoderMembro = (int) Math.round(poderMembroAtual * proporcao);
                if (novoPoderMembro > p.getPodermax()) {
                    novoPoderMembro = p.getPodermax(); // Respeitar o limite máximo do jogador
                }
                p.setPoder(novoPoderMembro);
                poderDistribuido += novoPoderMembro;
            }
        }

        // Ajustar qualquer diferença devido a arredondamentos
        int diferenca = novoPoder - poderDistribuido;
        if (diferenca != 0) {
            for (NDPlayer p : membros) {
                if (p != null && diferenca != 0) {
                    int poderMembro = p.getPoder();
                    int poderMaxMembro = p.getPodermax();
                    int ajuste = Math.min(diferenca, poderMaxMembro - poderMembro);
                    if (ajuste < 0) {
                        ajuste = Math.max(diferenca, -poderMembro);
                    }
                    p.setPoder(poderMembro + ajuste);
                    diferenca -= ajuste;
                    if (diferenca == 0) {
                        break;
                    }
                }
            }
        }

        try {
            save(); // Salvar as alterações na facção
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar facção " + getNome() + " após setPoder: " + e.getMessage());
        }
    }

    public void addPoder(int quantidade) {
        int poderAtual = getPoder();
        int novoPoder = poderAtual + quantidade;
        setPoder(novoPoder); // Reutiliza setPoder para aplicar a lógica de distribuição
    }
    
    public void removePoder(int quantidade) {
        int poderAtual = getPoder();
        int novoPoder = poderAtual - quantidade;
        setPoder(novoPoder); // Reutiliza setPoder para aplicar a lógica de distribuição
    }
    
    public int getPoder() {
        int i = 0;
        for (NDPlayer p : getAll()) {
            if (p != null)
                i += p.getPoder();
        }
        return i;
    }

    public int getPoderMax() {
        int i = 0;
        for (NDPlayer p : getAll()) {
            if (p != null)
                i += p.getPodermax();
        }
        return i;
    }

    public List<NDPlayer> getRecrutas() {
        List<NDPlayer> all = new ArrayList<>();
        for (String s : recrutas) {
            if (DataManager.players.get(s) != null)
                all.add(DataManager.players.get(s));
        }
        return all;
    }

    public int getStoredGeneratorAmount(EntityType type) {
        return storedGenerators.getOrDefault(type, 0);
    }

    public Map<EntityType, Integer> getSpawners() {
        cacheSpawners.clear();

        for (Terra t : terras) {
            Chunk c = t.getChunk();
            World world = c.getWorld();

            if (!c.isLoaded()) {
                c.load();
            }

            int cx = c.getX() << 4, cz = c.getZ() << 4;
            for (int x = cx; x < cx + 16; x++) {
                for (int z = cz; z < cz + 16; z++) {
                    for (int y = 0; y < world.getMaxHeight(); y++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.MOB_SPAWNER) {
                            CreatureSpawner spawner = (CreatureSpawner) block.getState();
                            EntityType type = spawner.getSpawnedType();
                            if (type != null) {
                                cacheSpawners.merge(type, 1, Integer::sum);
                            } else {
                                Bukkit.getLogger().warning("Spawner sem tipo definido encontrado em " + x + "," + y + "," + z + " (mundo: " + world.getName() + ")");
                            }
                        }
                    }
                }
            }
        }

        return cacheSpawners;
    }

    public void syncPlacedGenerators() {
        Map<EntityType, Integer> worldSpawners = new HashMap<>();
        Set<Chunk> processedChunks = new HashSet<>();


        for (Terra t : terras) {
            Chunk c = t.getChunk();
            if (processedChunks.contains(c)) {
                continue;
            }
            processedChunks.add(c);

            // Usar cache se disponível, caso contrário, atualizar
            if (!spawnerCache.containsKey(c)) {
                cacheSpawners(c);
            }

            List<SpawnerInfo> spawners = spawnerCache.getOrDefault(c, new ArrayList<>());
            for (SpawnerInfo spawnerInfo : spawners) {
                if (spawnerInfo.type != null) {
                    worldSpawners.merge(spawnerInfo.type, 1, Integer::sum);
                } else {
                }
            }
        }

        // Atualizar placedGenerators e cacheSpawners
        placedGenerators.clear();
        placedGenerators.putAll(worldSpawners);
        cacheSpawners.clear();
        cacheSpawners.putAll(worldSpawners);


        try {
            save();
        } catch (Exception e) {
        }
    }

    public void updateSpawners() {
        cacheSpawners.clear();
        getSpawners();
    }

    public String getTag() {
        String strippedTag = ChatColor.stripColor(tag);
        if (strippedTag == null) {
            Bukkit.getLogger().warning("Faction " + nome + " has a null tag after stripping colors. Using nome as fallback.");
            return nome;
        }
        return strippedTag;
    }

    public List<Terra> getTemporarios() {
        return temporarios;
    }

    public List<Terra> getTerras() {
        return terras;
    }

    public boolean hasBase() {
        return base != null;
    }

    public boolean isAliada(NDFaction f) {
        if (f == null)
            return false;
        return aliados.contains(f.getNome());
    }

    public boolean isInimigo(NDFaction f) {
        if (f == null)
            return false;
        return inimigos.contains(f.getNome());
    }

    public boolean isNeutra(NDFaction f) {
        if (f == null)
            return true;
        return !aliados.contains(f.getNome()) && !inimigos.contains(f.getNome());
    }

    public void kick(NDPlayer p) {
        if (p == null) return;
        recrutas.remove(p.getNome());
        membros.remove(p.getNome());
        capitoes.remove(p.getNome());
        permissoesMembros.remove(p.getNome());
        if (lider != null && lider.equals(p.getNome())) {
            lider = null;
        }
        p.setFaction(null);
    }

    public void disband() {
        List<NDPlayer> allPlayers = new ArrayList<>(getAll());
        for (NDPlayer player : allPlayers) {
            if (player != null) {
                player.setFaction(null);
                if (player.getPlayer() != null && (getLider() == null || !player.getNome().equals(getLider().getNome()))) {
                    player.getPlayer().sendMessage(Config.get("Mensagens.FaccaoFoiDesfeita").toString()
                            .replace("&", "§")
                            .replace("<nome>", getNome())
                            .replace("<lider>", getLider() != null ? getLider().getNome() : "Desconhecido"));
                }
            }
        }
        aliados.clear();
        inimigos.clear();
        terras.clear();
        temporarios.clear();
        pedidos.clear();
        pedidos3.clear();
        cacheSpawners.clear();
        storedGenerators.clear();
        placedGenerators.clear();
        generatorLogs.clear();
        permissoes.clear();
        permissoesRelacoes.clear();
        permissoesMembros.clear();
        vaultLogs.clear();
        recrutas.clear();
        membros.clear();
        capitoes.clear();
        relationLogs.clear();
        activeTruces.clear();
        lider = null;
        base = null;
        banco = 0;
        homes.clear();
        DataManager.removeRoster(nome);
        DataManager.saveSpawnPoints(nome, new HashMap<>());
        DataManager.removeHomes(nome);
        DataManager.factions.remove(nome);
        DataManager.removeExtraMemberSlots(nome);
    }

    public void removerPedidos(NDFaction f) {
        pedidos.remove(f);
        pedidos3.remove(f);
    }

    public void setAliados(List<NDFaction> aliados) {
        List<String> all = new ArrayList<>();
        for (NDFaction f : aliados) {
            all.add(f.getNome());
        }
        this.aliados = all;
    }

    public void setBanco(double banco) {
        this.banco = banco;
    }

    public void setBase(Location base) {
        this.base = base;
    }

    public void setCapitoes(List<NDPlayer> capitoes) {
        List<String> all = new ArrayList<>();
        for (NDPlayer f : capitoes) {
            all.add(f.getNome());
        }
        this.capitoes = all;
    }

    public void setInimigos(List<NDFaction> aliados) {
        List<String> all = new ArrayList<>();
        for (NDFaction f : aliados) {
            all.add(f.getNome());
        }
        this.inimigos = all;
    }

    public void setLider(NDPlayer lider) {
        this.lider = lider.getNome();
        recrutas.remove(lider.getNome());
        membros.remove(lider.getNome());
        capitoes.remove(lider.getNome());
    }

    public void setMembros(List<NDPlayer> membros) {
        List<String> all = new ArrayList<>();
        for (NDPlayer f : membros) {
            all.add(f.getNome());
        }
        this.membros = all;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setPedidosRelacoesAliados(List<NDFaction> players) {
        pedidos = players;
    }

    public void setPedidosRelacoesNeutras(List<NDFaction> players) {
        pedidos3 = players;
    }

    public void setRecrutas(List<NDPlayer> recrutas) {
        List<String> all = new ArrayList<>();
        for (NDPlayer f : recrutas) {
            all.add(f.getNome());
        }
        this.recrutas = all;
    }

    public void setTemporarios(List<Terra> temporarios) {
        this.temporarios = temporarios;
    }

    public void setTerras(List<Terra> terras) {
        this.terras = terras;
    }

    public void broadcast(String message) {
        for (Player player : getAllOnline()) {
            player.sendMessage(message);
        }
    }

    public int getTotalPlacedGenerators() {
        return placedGenerators.values().stream().mapToInt(Integer::intValue).sum();
    }

    public List<NDPlayer> getMembers() {
        List<NDPlayer> all = new ArrayList<>();
        for (String s : membros) {
            if (DataManager.players.get(s) != null)
                all.add(DataManager.players.get(s));
        }
        return all;
    }

    public String getLiderName() {
        return lider;
    }

    public TaxResult applyTax(double percentage) {
        double totalValue = getTotalMoneyEmSpawners() + getBanco();
        if (totalValue <= 0) {
            return new TaxResult(0, new HashMap<>(), false);
        }

        double taxAmount = totalValue * (percentage / 100.0);
        double remainingTax = taxAmount;
        double moneyDeducted = 0;
        Map<EntityType, Integer> spawnersDeducted = new HashMap<>();

        if (remainingTax > 0 && banco > 0) {
            double deductFromBanco = Math.min(banco, remainingTax);
            banco -= deductFromBanco;
            moneyDeducted = deductFromBanco;
            remainingTax -= deductFromBanco;
            cachedTotalMoney = -1;
        }

        if (remainingTax > 0) {
            List<Map.Entry<EntityType, Integer>> spawnerEntries = new ArrayList<>();
            spawnerEntries.addAll(placedGenerators.entrySet());
            spawnerEntries.addAll(storedGenerators.entrySet());
            spawnerEntries.sort((e1, e2) -> {
                Integer v1 = (Integer) Config.get("Top.Spawners." + e1.getKey().toString());
                Integer v2 = (Integer) Config.get("Top.Spawners." + e2.getKey().toString());
                return v1.compareTo(v2);
            });

            for (Map.Entry<EntityType, Integer> entry : spawnerEntries) {
                if (remainingTax <= 0) break;
                EntityType type = entry.getKey();
                int quantity = entry.getValue();
                int spawnerValue = (Integer) Config.get("Top.Spawners." + type.toString());
                if (spawnerValue <= 0) continue;

                int spawnersToRemove = (int) Math.ceil(remainingTax / spawnerValue);
                spawnersToRemove = Math.min(spawnersToRemove, quantity);
                if (spawnersToRemove <= 0) continue;

                if (placedGenerators.containsKey(type)) {
                    int current = placedGenerators.get(type);
                    int toRemove = Math.min(current, spawnersToRemove);
                    if (removePlacedGenerator(type, toRemove, null)) {
                        spawnersDeducted.merge(type, toRemove, Integer::sum);
                        remainingTax -= toRemove * spawnerValue;
                    }
                } else if (storedGenerators.containsKey(type)) {
                    int current = storedGenerators.get(type);
                    int toRemove = Math.min(current, spawnersToRemove);
                    if (removeStoredGenerator(type, toRemove, null)) {
                        spawnersDeducted.merge(type, toRemove, Integer::sum);
                        remainingTax -= toRemove * spawnerValue;
                    }
                }
            }
        }

        if (banco < 0) {
            banco = 0;
        }

        try {
            save();
            updateSpawners();
            return new TaxResult(moneyDeducted, spawnersDeducted, true);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar facção após taxação: " + e.getMessage());
            return new TaxResult(moneyDeducted, spawnersDeducted, false);
        }
    }

    // Métodos para gerenciar tréguas
    public void activateTruce(NDFaction otherFaction, long duration) {
        activeTruces.put(otherFaction, System.currentTimeMillis() + duration);
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar trégua para " + nome + ": " + e.getMessage());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeTruces.remove(otherFaction) != null) {
                    broadcast("§aA trégua com §f[" + otherFaction.getTag() + "] §aexpirou!");
                    otherFaction.broadcast("§aA trégua com §f[" + getTag() + "] §aexpirou!");
                }
            }
        }.runTaskLater(Main.getPlugin(Main.class), duration / 50L);
    }

    public boolean hasTruceWith(NDFaction otherFaction) {
        Long expiration = activeTruces.get(otherFaction);
        if (expiration == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiration) {
            activeTruces.remove(otherFaction);
            try {
                save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao salvar remoção de trégua para " + nome + ": " + e.getMessage());
            }
            return false;
        }
        return true;
    }

    public boolean hasActiveTruce() {
        return activeTruces.values().stream().anyMatch(expiration -> System.currentTimeMillis() <= expiration);
    }

    public Map<NDFaction, Long> getActiveTruces() {
        return new HashMap<>(activeTruces);
    }

}
