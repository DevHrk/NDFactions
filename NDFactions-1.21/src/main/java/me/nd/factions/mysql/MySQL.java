package me.nd.factions.mysql;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import me.nd.factions.Main;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class MySQL {
    private static final JavaPlugin PLUGIN = Main.getPlugin(Main.class);
    private static final Logger LOGGER = Bukkit.getLogger();
    private static Connection connection;
    protected static boolean usingSQLite;
    private static File sqliteFile;

    public static void open() {
        usingSQLite = PLUGIN.getConfig().getBoolean("Mysql.SQLLite");
        try {
            if (usingSQLite) {
                setupSQLite();
            } else {
                setupMySQL();
            }
            createTables();
        } catch (SQLException e) {
            LOGGER.severe("Failed to connect to database: " + e.getMessage());
            PLUGIN.getPluginLoader().disablePlugin(PLUGIN);
        }
    }

    private static void setupSQLite() throws SQLException {
        File dataFolder = PLUGIN.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        sqliteFile = new File(dataFolder, "ndfactions.db");
        String url = "jdbc:sqlite:" + sqliteFile.getPath();
        LOGGER.info("Connecting to SQLite: " + url);

        connection = DriverManager.getConnection(url);
    }

    private static void setupMySQL() throws SQLException {
        String host = PLUGIN.getConfig().getString("Mysql.IP");
        String port = PLUGIN.getConfig().getString("Mysql.Porta");
        String database = PLUGIN.getConfig().getString("Mysql.DB");
        String user = PLUGIN.getConfig().getString("Mysql.Usuario");
        String pass = PLUGIN.getConfig().getString("Mysql.Senha");

        String url = String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true", host, port, database);
        LOGGER.info("Connecting to MySQL: " + url);

        connection = DriverManager.getConnection(url, user, pass);
    }

    public static boolean isOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void reconnectIfClosed() {
        if (!isOpen()) {
            LOGGER.info("Database connection closed. Attempting to reconnect...");
            open();
        }
    }

    public static void close() {
        if (isOpen()) {
            LOGGER.info("Saving all factions before closing connection...");
            Methods.saveAllFactions();
            try {
                connection.close();
                LOGGER.info("Database connection closed successfully.");
            } catch (SQLException e) {
                LOGGER.severe("Error closing database connection: " + e.getMessage());
            }
        }
    }

    private static void createTables() {
        if (!isOpen()) {
            return;
        }

        String playersTable = 
            "CREATE TABLE IF NOT EXISTS NDPlayers (" +
            "nome VARCHAR(16) NOT NULL," +
            "faction VARCHAR(32)," +
            "kills INTEGER," +
            "mortes INTEGER," +
            "poder INTEGER," +
            "podermaximo INTEGER," +
            "online VARCHAR(256) NOT NULL," +
            "PRIMARY KEY (nome)" +
            ");";

        String factionsTable = 
            "CREATE TABLE IF NOT EXISTS NDFactions (" +
            "nome VARCHAR(16) NOT NULL," +
            "base VARCHAR(256)," +
            "banco DOUBLE," +
            "terras VARCHAR(8192) NOT NULL," +
            "aliados VARCHAR(1024) NOT NULL," +
            "capitoes VARCHAR(1024) NOT NULL," +
            "membros VARCHAR(1024) NOT NULL," +
            "recrutas VARCHAR(1024) NOT NULL," +
            "lider VARCHAR(1024) NOT NULL," +
            "tag VARCHAR(16) NOT NULL," +
            "inimigos VARCHAR(1024) NOT NULL," +
            "permissoes VARCHAR(8192) NOT NULL," +
            "permissoes_relacoes VARCHAR(8192) NOT NULL," +
            "permissoes_membros VARCHAR(8192) NOT NULL," +
            "generators VARCHAR(8192) NOT NULL," +
            "PRIMARY KEY (nome)" +
            ");";

        try (PreparedStatement stmt1 = connection.prepareStatement(playersTable);
             PreparedStatement stmt2 = connection.prepareStatement(factionsTable)) {
            stmt1.executeUpdate();
            stmt2.executeUpdate();

            // Check and add missing columns
            ensureColumnsExist();
        } catch (SQLException e) {
            LOGGER.severe("Error creating tables: " + e.getMessage());
        }
    }

    private static void ensureColumnsExist() throws SQLException {
        String checkColumn = usingSQLite ? "PRAGMA table_info(NDFactions)" : "SHOW COLUMNS FROM NDFactions";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkColumn);
             ResultSet rs = checkStmt.executeQuery()) {
            boolean hasExtraMemberSlots = false;
            boolean hasPermissoesMembros = false;

            while (rs.next()) {
                String columnName = usingSQLite ? rs.getString("name") : rs.getString("Field");
                if ("extra_member_slots".equals(columnName)) {
                    hasExtraMemberSlots = true;
                }
                if ("permissoes_membros".equals(columnName)) {
                    hasPermissoesMembros = true;
                }
            }

            // Add missing columns if necessary
            if (!hasExtraMemberSlots) {
                String alterTable = "ALTER TABLE NDFactions ADD COLUMN extra_member_slots INTEGER NOT NULL DEFAULT 0";
                try (PreparedStatement alterStmt = connection.prepareStatement(alterTable)) {
                    alterStmt.executeUpdate();
                    LOGGER.info("Added extra_member_slots column to NDFactions table.");
                }
            }

            if (!hasPermissoesMembros) {
                String alterTable = "ALTER TABLE NDFactions ADD COLUMN permissoes_membros VARCHAR(8192) NOT NULL DEFAULT ''";
                try (PreparedStatement alterStmt = connection.prepareStatement(alterTable)) {
                    alterStmt.executeUpdate();
                    LOGGER.info("Added permissoes_membros column to NDFactions table.");
                }
            }
        }
    }

    public static Connection getConnection() {
        reconnectIfClosed();
        return connection;
    }
}