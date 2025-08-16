package me.nd.factions;

import java.io.File;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nd.factions.addons.MembroPlus;
import me.nd.factions.addons.FExpansion;
import me.nd.factions.chats.*;
import me.nd.factions.comandos.*;
import me.nd.factions.listeners.listener.Listeners;
import me.nd.factions.mysql.*;
import me.nd.factions.objetos.*;
import me.nd.factions.runnables.*;
import me.nd.factions.scoreboard.Board;
import me.nd.factions.scoreboard.Score;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        MySQL.open();

        loadData();
        
        Score.register();
        Listeners.setupListeners();
        registerCommands();
        registerRunnables();

        Board.start();
        getLogger().info("[NDFactions] iniciado com sucesso.");
        
        DataManager.extraMemberSlotsFile = new File(this.getDataFolder(), "extra_member_slots.yml");
        DataManager.extraMemberSlotsConfig = YamlConfiguration.loadConfiguration(DataManager.extraMemberSlotsFile);
        DataManager.loadExtraMemberSlots();
        
        DataManager.initSpawnPointsFile();
        
        DataManager.initHomesFile();
        
        DataManager.initVaultsFile();
        
        DataManager.initFactionDataFile();
        
        DataManager.initRostersFile();
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPI.registerExpansion(new FExpansion());
        }
    }

    @Override
    public void onDisable() {
        if (!MySQL.isOpen()) return;

        long start = System.currentTimeMillis();
        Methods.deleteTable("NDPlayers");
        Methods.deleteTable("NDFactions");

        int playersSaved = 0;
        for (NDPlayer p : DataManager.players.values()) {
            try {
                p.save();
                playersSaved++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int factionsSaved = 0;
        for (NDFaction f : DataManager.factions.values()) {
            try {
                f.save();
                DataManager.saveFactionData(f); // Save immunity data to YAML
                factionsSaved++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long duration = (System.currentTimeMillis() - start) / 1000;
        getLogger().info("Salvos " + playersSaved + " jogadores em " + duration + "s");
        getLogger().info("Salvas " + factionsSaved + " facções em " + duration + "s");
        MySQL.close();
    }
    
    private void loadData() {
        long start = System.currentTimeMillis();

        int loadedPlayers = 0;
        for (NDPlayer p : Methods.getAllPlayers()) {
            try {
                DataManager.players.put(p.getNome(), p);
                loadedPlayers++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        int loadedFactions = 0;
        for (NDFaction f : Methods.getAllFactions()) {
            try {
                DataManager.factions.put(f.getNome(), f);
                loadedFactions++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long duration = (System.currentTimeMillis() - start) / 1000;
        getLogger().info("Carregados " + loadedPlayers + " jogadores em " + duration + "s");
        getLogger().info("Carregadas " + loadedFactions + " facções em " + duration + "s");
    }

    private void registerCommands() {
        getCommand("f").setExecutor(new Comandos());
        getCommand(".").setExecutor(new Chat());
        getCommand("a").setExecutor(new Chat());
        getCommand("cpower").setExecutor(new me.nd.factions.addons.Poder());
        getCommand("cmembroplus").setExecutor(new MembroPlus());
        getCommand("s").setExecutor(new ChatStaff());
        getCommand("grace").setExecutor(new AdminCommands());
        getCommand("fs").setExecutor(new AdminCommands());
        getCommand("togglesobataque").setExecutor(new AdminCommands());
        getCommand("sgive").setExecutor(new AdminCommands());
    }

    private void registerRunnables() {
        new VerTerras().runTaskTimer(this, 40L, 20L);
        new Poder().runTaskTimer(this, 40L, 20L * 60 * Main.get().getConfig().getInt("Geral.TempoParaRecuperarPoder"));
        new me.nd.factions.runnables.McMMO().runTaskTimerAsynchronously(this, 40L, 20L * 60 * 5);

        final long baseDelay = 20L * 60 * 5;
        new TopCoins().runTaskTimer(this, 200L, baseDelay);
        new TopCoins2().runTaskTimer(this, 200L, baseDelay + 20);
        new TopKDR().runTaskTimer(this, 200L, baseDelay + 40);
        new TopPoder().runTaskTimer(this, 200L, baseDelay + 60);
        new TopGeradores().runTaskTimer(this, 200L, baseDelay + 80);
    }
    
    public static Main get() {
        return JavaPlugin.getPlugin(Main.class);
    }
}