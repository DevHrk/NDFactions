package me.nd.factions.mysql;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import me.nd.factions.enums.Cargo;
import me.nd.factions.enums.Relacao;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.Utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class Methods extends MySQL {
    private static final Logger LOGGER = Bukkit.getLogger();

    public static boolean contains(String table, String column, String value) {
        String query = "SELECT 1 FROM `" + table + "` WHERE `" + column + "` = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.severe("Error checking existence in " + table + ": " + e.getMessage());
            return false;
        }
    }

    public static void deleteTable(String table) {
        String query = usingSQLite ? "DELETE FROM " + table : "TRUNCATE TABLE `" + table + "`";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.severe("Error deleting table " + table + ": " + e.getMessage());
        }
    }

    public static List<NDPlayer> getAllPlayers() {
        List<NDPlayer> players = new ArrayList<>();
        String query = "SELECT * FROM `NDPlayers`";
        try (PreparedStatement stmt = getConnection().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                try {
                    players.add(new NDPlayer(
                        rs.getString("nome"),
                        rs.getString("faction"),
                        rs.getInt("kills"),
                        rs.getInt("mortes"),
                        rs.getInt("poder"),
                        rs.getInt("podermaximo"),
                        rs.getLong("online")
                    ));
                } catch (Exception e) {
                    LOGGER.warning("Error loading player " + rs.getString("nome") + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error fetching players: " + e.getMessage());
        }
        return players;
    }

    public static List<NDFaction> getAllFactions() {
        List<NDFaction> factions = new ArrayList<>();
        String query = "SELECT * FROM `NDFactions`";
        try (PreparedStatement stmt = getConnection().prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                factions.add(parseFaction(rs));
            }
        } catch (SQLException e) {
            LOGGER.severe("Error fetching factions: " + e.getMessage());
        }
        return factions;
    }

    public static NDFaction getFaction(String name) throws SQLException {
        String query = "SELECT * FROM `NDFactions` WHERE `nome` = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return parseFaction(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error fetching faction " + name + ": " + e.getMessage());
            throw e;
        }
        return null;
    }

    public static NDPlayer getPlayer(String name) throws SQLException {
        String query = "SELECT * FROM `NDPlayers` WHERE `nome` = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new NDPlayer(
                        rs.getString("nome"),
                        rs.getString("faction"),
                        rs.getInt("kills"),
                        rs.getInt("mortes"),
                        rs.getInt("poder"),
                        rs.getInt("podermaximo"),
                        rs.getLong("online")
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error fetching player " + name + ": " + e.getMessage());
            throw e;
        }
        return null;
    }

    public static void updateFaction(NDFaction faction) throws SQLException {
        String query = "REPLACE INTO `NDFactions`(`nome`, `base`, `banco`, `terras`, `aliados`, `capitoes`, `membros`, `recrutas`, `lider`, `tag`, `inimigos`, `permissoes`, `permissoes_relacoes`, `permissoes_membros`, `generators`, `successful_invasions`, `destroyed_spawners`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            setFactionStatement(stmt, faction);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.severe("Error updating faction " + faction.getNome() + ": " + e.getMessage());
            throw e;
        }
    }

    public static void updatePlayer(NDPlayer player) {
        String query = "REPLACE INTO `NDPlayers`(`nome`, `faction`, `kills`, `mortes`, `poder`, `podermaximo`, `online`) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, player.getNome());
            stmt.setString(2, player.getFaction() != null ? player.getFaction().getNome() : "");
            stmt.setInt(3, player.getKills());
            stmt.setInt(4, player.getMortes());
            stmt.setInt(5, player.getPoder());
            stmt.setInt(6, player.getPodermax());
            stmt.setString(7, String.valueOf(player.getLastMilis()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.severe("Error updating player " + player.getNome() + ": " + e.getMessage());
        }
    }

    public static void saveAllFactions() {
        if (!isOpen()) {
            LOGGER.warning("Database connection is not active. Cannot save factions.");
            return;
        }
        for (NDFaction faction : DataManager.factions.values()) {
            try {
                updateFaction(faction);
            } catch (SQLException e) {
                LOGGER.severe("Error saving faction " + faction.getNome() + ": " + e.getMessage());
            }
        }
    }

    private static NDFaction parseFaction(ResultSet rs) throws SQLException {
        String name = rs.getString("nome");
        Location base = Utils.getDeserializedLocation(rs.getString("base"));
        double bank = rs.getDouble("banco");
        List<Terra> lands = parseTerras(rs.getString("terras"));
        List<String> allies = parseList(rs.getString("aliados"));
        List<String> enemies = parseList(rs.getString("inimigos"));
        List<String> captains = parseList(rs.getString("capitoes"));
        List<String> members = parseList(rs.getString("membros"));
        List<String> recruits = parseList(rs.getString("recrutas"));
        String leader = rs.getString("lider");
        String tag = rs.getString("tag");
        String permissionsRaw = rs.getString("permissoes");
        String relationPermissionsRaw = rs.getString("permissoes_relacoes");
        String memberPermissionsRaw = rs.getString("permissoes_membros");
        String generatorsRaw = rs.getString("generators");
        int successfulInvasions = rs.getInt("successful_invasions");
        String destroyedSpawnersRaw = rs.getString("destroyed_spawners");

        NDFaction faction = new NDFaction(name, base, bank, lands, allies, leader, captains, members, recruits, tag, enemies);
        if (permissionsRaw != null && !permissionsRaw.isEmpty()) {
            faction.getPermissoes().clear();
            faction.getPermissoes().putAll(parsePermissions(permissionsRaw));
        }
        if (relationPermissionsRaw != null && !relationPermissionsRaw.isEmpty()) {
            faction.getPermissoesRelacoes().clear();
            faction.getPermissoesRelacoes().putAll(parseRelationPermissions(relationPermissionsRaw));
        }
        if (memberPermissionsRaw != null && !memberPermissionsRaw.isEmpty()) {
            faction.getPermissoesMembros().clear();
            faction.getPermissoesMembros().putAll(parseMemberPermissions(memberPermissionsRaw));
        }
        if (generatorsRaw != null && !generatorsRaw.isEmpty()) {
            Map<String, Object> generators = parseGenerators(generatorsRaw);
            faction.setStoredGenerators((Map<EntityType, Integer>) generators.getOrDefault("stored", new HashMap<>()));
            faction.setPlacedGenerators((Map<EntityType, Integer>) generators.getOrDefault("placed", new HashMap<>()));
            faction.setGeneratorLogs((List<NDFaction.GeneratorLog>) generators.getOrDefault("logs", new ArrayList<>()));
        }
        faction.setSuccessfulInvasions(successfulInvasions);
        if (destroyedSpawnersRaw != null && !destroyedSpawnersRaw.isEmpty()) {
            faction.setDestroyedSpawners(parseDestroyedSpawners(destroyedSpawnersRaw));
        }
        return faction;
    }

    private static void setFactionStatement(PreparedStatement stmt, NDFaction faction) throws SQLException {
        stmt.setString(1, faction.getNome());
        stmt.setString(2, Utils.getSerializedLocation(faction.getBase()));
        stmt.setDouble(3, faction.getBanco());
        stmt.setString(4, serializeTerras(faction.getTerras()));
        stmt.setString(5, serializeStrings(faction.getAliados(), NDFaction::getNome));
        stmt.setString(6, serializeStrings(faction.getCapitoes(), NDPlayer::getNome));
        stmt.setString(7, serializeStrings(faction.getMembros(), NDPlayer::getNome));
        stmt.setString(8, serializeStrings(faction.getRecrutas(), NDPlayer::getNome));
        stmt.setString(9, faction.getLider() != null ? faction.getLider().getNome() : "");
        stmt.setString(10, faction.getTag());
        stmt.setString(11, serializeStrings(faction.getInimigos(), NDFaction::getNome));
        stmt.setString(12, serializePermissions(faction.getPermissoes()));
        stmt.setString(13, serializeRelationPermissions(faction.getPermissoesRelacoes()));
        stmt.setString(14, serializeMemberPermissions(faction.getPermissoesMembros()));
        stmt.setString(15, serializeGenerators(faction.getStoredGenerators(), faction.getPlacedGenerators(), faction.getGeneratorLogs()));
        stmt.setInt(16, faction.getSuccessfulInvasions());
        stmt.setString(17, serializeDestroyedSpawners(faction.getDestroyedSpawners()));
    }

    private static List<String> parseList(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.length() < 3) return list;
        for (String item : raw.split("(?=\\{)|(?<=\\})")) {
            String cleaned = item.replace("{", "").replace("}", "").trim();
            if (!cleaned.isEmpty()) list.add(cleaned);
        }
        return list;
    }

    private static List<Terra> parseTerras(String raw) {
        List<Terra> lands = new ArrayList<>();
        if (raw == null || raw.length() < 3) return lands;
        for (String item : raw.split("(?=\\{)|(?<=\\})")) {
            try {
                String cleaned = item.replace("{", "").replace("}", "").trim();
                if (cleaned.isEmpty()) continue;
                String[] parts = cleaned.split("_");
                if (parts.length != 2) continue;
                World world = Bukkit.getWorld(parts[0]);
                if (world == null) continue;
                String[] coords = parts[1].split(":");
                if (coords.length != 2) continue;
                int x = Integer.parseInt(coords[0]);
                int z = Integer.parseInt(coords[1]);
                lands.add(new Terra(world, x, z));
            } catch (Exception e) {
                LOGGER.warning("Error parsing land: " + item + " - " + e.getMessage());
            }
        }
        return lands;
    }

    private static Map<String, Object> parseGenerators(String raw) {
        Map<String, Object> generators = new HashMap<>();
        if (raw == null || raw.length() < 3) return generators;

        for (String section : raw.split("(?=\\{)|(?<=\\})")) {
            String cleaned = section.replace("{", "").replace("}", "").trim();
            if (cleaned.isEmpty()) continue;

            String[] parts = cleaned.split(":", 2);
            if (parts.length != 2) continue;

            String type = parts[0];
            if (type.equals("stored") || type.equals("placed")) {
                Map<EntityType, Integer> genMap = new HashMap<>();
                if (!parts[1].isEmpty()) {
                    for (String gen : parts[1].split(",")) {
                        String[] genParts = gen.split("=");
                        if (genParts.length == 2) {
                            try {
                                EntityType entityType = EntityType.valueOf(genParts[0]);
                                int amount = Integer.parseInt(genParts[1]);
                                genMap.put(entityType, amount);
                            } catch (IllegalArgumentException e) {
                                LOGGER.warning("Error parsing generator: " + gen + " - " + e.getMessage());
                            }
                        }
                    }
                }
                generators.put(type, genMap);
            } else if (type.equals("logs")) {
                List<NDFaction.GeneratorLog> logs = new ArrayList<>();
                if (!parts[1].isEmpty()) {
                    for (String log : parts[1].split(";")) {
                        String[] logParts = log.split(",");
                        if (logParts.length == 5) {
                            try {
                                String playerName = logParts[0];
                                String action = logParts[1];
                                EntityType entityType = EntityType.valueOf(logParts[2]);
                                int amount = Integer.parseInt(logParts[3]);
                                long timestamp = Long.parseLong(logParts[4]);
                                logs.add(new NDFaction.GeneratorLog(playerName, action, entityType, amount, timestamp));
                            } catch (IllegalArgumentException e) {
                                LOGGER.warning("Error parsing generator log: " + log + " - " + e.getMessage());
                            }
                        }
                    }
                }
                generators.put("logs", logs);
            }
        }
        return generators;
    }

    public static String serializeGenerators(Map<EntityType, Integer> stored, Map<EntityType, Integer> placed, List<NDFaction.GeneratorLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{stored:");
        
        for (Map.Entry<EntityType, Integer> entry : stored.entrySet()) {
            sb.append(entry.getKey().toString()).append("=").append(entry.getValue()).append(",");
        }
        
        if (!stored.isEmpty()) sb.setLength(sb.length() - 1);
        
        sb.append("}{placed:");
        
        for (Map.Entry<EntityType, Integer> entry : placed.entrySet()) {
            sb.append(entry.getKey().toString()).append("=").append(entry.getValue()).append(",");
        }
        
        if (!placed.isEmpty()) sb.setLength(sb.length() - 1);
        
        sb.append("}{logs:");
        
        for (NDFaction.GeneratorLog log : logs) {
            sb.append(log.getPlayerName()).append(",")
              .append(log.getAction()).append(",")
              .append(log.getGeneratorType().toString()).append(",")
              .append(log.getAmount()).append(",")
              .append(log.getTimestamp()).append(";");
        }
        
        if (!logs.isEmpty()) sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    public static String serializePermissions(Map<Cargo, Map<String, Boolean>> permissions) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Cargo, Map<String, Boolean>> entry : permissions.entrySet()) {
            sb.append("{").append(entry.getKey().name()).append(":");
            for (Map.Entry<String, Boolean> perm : entry.getValue().entrySet()) {
                sb.append(perm.getKey()).append("=").append(perm.getValue()).append(",");
            }
            if (!entry.getValue().isEmpty()) sb.setLength(sb.length() - 1);
            sb.append("}");
        }
        return sb.toString();
    }

    private static Map<Cargo, Map<String, Boolean>> parsePermissions(String raw) {
        Map<Cargo, Map<String, Boolean>> permissions = new HashMap<>();
        if (raw == null || raw.length() < 3) return permissions;

        for (String section : raw.split("(?=\\{)|(?<=\\})")) {
            String cleaned = section.replace("{", "").replace("}", "").trim();
            if (cleaned.isEmpty()) continue;

            String[] parts = cleaned.split(":", 2);
            if (parts.length != 2) continue;

            Cargo cargo;
            try {
                cargo = Cargo.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Invalid cargo: " + parts[0]);
                continue;
            }

            Map<String, Boolean> perms = new HashMap<>();
            if (!parts[1].isEmpty()) {
                for (String perm : parts[1].split(",")) {
                    String[] permParts = perm.split("=");
                    if (permParts.length == 2) {
                        try {
                            perms.put(permParts[0], Boolean.parseBoolean(permParts[1]));
                        } catch (Exception e) {
                            LOGGER.warning("Error parsing permission: " + perm + " - " + e.getMessage());
                        }
                    }
                }
            }
            permissions.put(cargo, perms);
        }
        return permissions;
    }

    public static String serializeRelationPermissions(Map<Relacao, Map<String, Boolean>> relationPermissions) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Relacao, Map<String, Boolean>> entry : relationPermissions.entrySet()) {
            sb.append("{").append(entry.getKey().name()).append(":");
            for (Map.Entry<String, Boolean> perm : entry.getValue().entrySet()) {
                sb.append(perm.getKey()).append("=").append(perm.getValue()).append(",");
            }
            if (!entry.getValue().isEmpty()) sb.setLength(sb.length() - 1);
            sb.append("}");
        }
        return sb.toString();
    }

    private static Map<Relacao, Map<String, Boolean>> parseRelationPermissions(String raw) {
        Map<Relacao, Map<String, Boolean>> relationPermissions = new HashMap<>();
        if (raw == null || raw.length() < 3) return relationPermissions;

        for (String section : raw.split("(?=\\{)|(?<=\\})")) {
            String cleaned = section.replace("{", "").replace("}", "").trim();
            if (cleaned.isEmpty()) continue;

            String[] parts = cleaned.split(":", 2);
            if (parts.length != 2) continue;

            Relacao relation;
            try {
                relation = Relacao.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                LOGGER.warning("Invalid relation: " + parts[0]);
                continue;
            }

            Map<String, Boolean> perms = new HashMap<>();
            if (!parts[1].isEmpty()) {
                for (String perm : parts[1].split(",")) {
                    String[] permParts = perm.split("=");
                    if (permParts.length == 2) {
                        try {
                            perms.put(permParts[0], Boolean.parseBoolean(permParts[1]));
                        } catch (Exception e) {
                            LOGGER.warning("Error parsing relation permission: " + perm + " - " + e.getMessage());
                        }
                    }
                }
            }
            relationPermissions.put(relation, perms);
        }
        return relationPermissions;
    }

    public static String serializeMemberPermissions(Map<String, Map<String, Boolean>> memberPermissions) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Boolean>> entry : memberPermissions.entrySet()) {
            sb.append("{").append(entry.getKey()).append(":");
            for (Map.Entry<String, Boolean> perm : entry.getValue().entrySet()) {
                sb.append(perm.getKey()).append("=").append(perm.getValue()).append(",");
            }
            if (!entry.getValue().isEmpty()) sb.setLength(sb.length() - 1);
            sb.append("}");
        }
        return sb.toString();
    }

    private static Map<String, Map<String, Boolean>> parseMemberPermissions(String raw) {
        Map<String, Map<String, Boolean>> memberPermissions = new HashMap<>();
        if (raw == null || raw.length() < 3) return memberPermissions;

        for (String section : raw.split("(?=\\{)|(?<=\\})")) {
            String cleaned = section.replace("{", "").replace("}", "").trim();
            if (cleaned.isEmpty()) continue;

            String[] parts = cleaned.split(":", 2);
            if (parts.length != 2) continue;

            String playerName = parts[0];
            Map<String, Boolean> perms = new HashMap<>();
            if (!parts[1].isEmpty()) {
                for (String perm : parts[1].split(",")) {
                    String[] permParts = perm.split("=");
                    if (permParts.length == 2) {
                        try {
                            perms.put(permParts[0], Boolean.parseBoolean(permParts[1]));
                        } catch (Exception e) {
                            LOGGER.warning("Error parsing member permission: " + perm + " - " + e.getMessage());
                        }
                    }
                }
            }
            memberPermissions.put(playerName, perms);
        }
        return memberPermissions;
    }
    public static void updateSuccessfulInvasions(NDFaction faction) throws SQLException {
        String query = "UPDATE `NDFactions` SET `successful_invasions` = ? WHERE `nome` = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setInt(1, faction.getSuccessfulInvasions());
            stmt.setString(2, faction.getNome());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.severe("Error updating successful_invasions for faction " + faction.getNome() + ": " + e.getMessage());
            throw e;
        }
    }

    public static void updateDestroyedSpawners(NDFaction faction) throws SQLException {
        String query = "UPDATE `NDFactions` SET `destroyed_spawners` = ? WHERE `nome` = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setString(1, serializeDestroyedSpawners(faction.getDestroyedSpawners()));
            stmt.setString(2, faction.getNome());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.severe("Error updating destroyed_spawners for faction " + faction.getNome() + ": " + e.getMessage());
            throw e;
        }
    }

    public static String serializeDestroyedSpawners(Map<EntityType, Integer> destroyedSpawners) {
        if (destroyedSpawners == null || destroyedSpawners.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<EntityType, Integer> entry : destroyedSpawners.entrySet()) {
            sb.append(entry.getKey().toString()).append("=").append(entry.getValue()).append(",");
        }
        if (!destroyedSpawners.isEmpty()) sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    private static Map<EntityType, Integer> parseDestroyedSpawners(String raw) {
        Map<EntityType, Integer> destroyedSpawners = new HashMap<>();
        if (raw == null || raw.length() < 3) return destroyedSpawners;

        String cleaned = raw.replace("{", "").replace("}", "").trim();
        if (cleaned.isEmpty()) return destroyedSpawners;

        for (String spawner : cleaned.split(",")) {
            String[] parts = spawner.split("=");
            if (parts.length == 2) {
                try {
                    EntityType entityType = EntityType.valueOf(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    destroyedSpawners.put(entityType, amount);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Error parsing destroyed spawner: " + spawner + " - " + e.getMessage());
                }
            }
        }
        return destroyedSpawners;
    }
    public static String serializeTerras(List<Terra> lands) {
        StringBuilder sb = new StringBuilder();
        for (Terra land : lands) {
            sb.append("{").append(land.getWorld().getName()).append("_")
              .append(land.getX()).append(":").append(land.getZ()).append("}");
        }
        return sb.toString();
    }

    public static <T> String serializeStrings(Collection<T> list, java.util.function.Function<T, String> func) {
        StringBuilder sb = new StringBuilder();
        for (T item : list) {
            if (item != null) sb.append("{").append(func.apply(item)).append("}");
        }
        return sb.toString();
    }
}