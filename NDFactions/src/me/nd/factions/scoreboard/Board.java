package me.nd.factions.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.events.experience.McMMOPlayerLevelUpEvent;

import me.clip.placeholderapi.PlaceholderAPI;
import me.nd.factions.Main;
import me.nd.factions.addons.SobAtaque;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Board implements Listener {

    private static final Map<Player, Integer> playerPower = new ConcurrentHashMap<>();
    private static final Map<Player, Scoreboard> playerBoards = new ConcurrentHashMap<>();
    private static final Map<Player, ScoreboardCache> scoreboardCache = new ConcurrentHashMap<>();
    private static final int DEFAULT_UPDATE_INTERVAL;
    private static final List<String> ENABLED_WORLDS;
    private static final String SCOREBOARD_TITLE;
    private static final List<String> WITH_FACTION_LINES;
    private static final List<String> NO_FACTION_LINES;
    private static final String ATTACK_INDICATOR;

    static {
        FileConfiguration config = Main.get().getConfig();
        DEFAULT_UPDATE_INTERVAL = config.getInt("Atualizacao", 10) * 20; // 10 seconds
        ENABLED_WORLDS = config.getStringList("MundosD");
        SCOREBOARD_TITLE = config.getString("ScoreBoard.Title", "");
        WITH_FACTION_LINES = config.getStringList("ScoreBoard.ComFac");
        NO_FACTION_LINES = config.getStringList("ScoreBoard.SemFac");
        ATTACK_INDICATOR = config.getString("EmAttack", "§c☣").replace("&", "§");
    }

    private static class ScoreboardCache {
        String title;
        List<String> lines;
        String zoneName;
        int power;
        int onlineMembers;

        ScoreboardCache(String title, List<String> lines, String zoneName, int power, int onlineMembers) {
            this.title = title;
            this.lines = lines;
            this.zoneName = zoneName;
            this.power = power;
            this.onlineMembers = onlineMembers;
        }

        boolean isDifferent(String newTitle, List<String> newLines, String newZoneName, int newPower, int newOnlineMembers) {
            return !title.equals(newTitle) || !lines.equals(newLines) || !zoneName.equals(newZoneName) ||
                   power != newPower || onlineMembers != newOnlineMembers;
        }
    }

    public static void start() {
        // Default update task (every 10 seconds)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    NDPlayer mp = DataManager.players.get(p.getName());
                    if (mp == null || !mp.hasFaction() || !SobAtaque.cooldown.contains(mp.getFaction())) {
                        updateScoreboard(p, null);
                    }
                }
            }
        }.runTaskTimer(Main.get(), 0L, DEFAULT_UPDATE_INTERVAL);

        // Fast update task for factions under attack (every 1 second)
        new BukkitRunnable() {
            @Override
            public void run() {
                synchronized (SobAtaque.cooldown) {
                    for (NDFaction faction : SobAtaque.cooldown) {
                        faction.getAllOnline().forEach(p -> updateScoreboard(p, null));
                    }
                }
            }
        }.runTaskTimer(Main.get(), 0L, 20L); // 1 second
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
            	updateScoreboard(p, p.getLocation());
                NDPlayer mp = DataManager.players.get(p.getName());
                if (mp != null && mp.hasFaction()) {
                    mp.getFaction().getAllOnline().forEach(other -> updateScoreboard(other, null));
                }
            }
        }.runTaskLater(Main.get(), 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        playerPower.remove(p);
        playerBoards.remove(p);
        scoreboardCache.remove(p);
        NDPlayer mp = DataManager.players.get(p.getName());
        if (mp != null && mp.hasFaction()) {
            mp.getFaction().getAllOnline().forEach(other -> updateScoreboard(other, null));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        NDPlayer mp = DataManager.players.get(p.getName());
        if (mp == null) return;

        int currentPower = mp.getPoder();
        boolean powerChanged = !playerPower.containsKey(p) || playerPower.get(p) != currentPower;
        if (powerChanged) {
            playerPower.put(p, currentPower);
        }

        ScoreUtils utils = new ScoreUtils(p);
        boolean zoneChanged = !utils.getLocationNameFaction(from).equalsIgnoreCase(utils.getLocationNameFaction(to));
        if (powerChanged || zoneChanged) {
            updateScoreboard(p, to);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        NDPlayer mp = DataManager.players.get(p.getName());
        if (mp == null) return;

        int currentPower = mp.getPoder();
        boolean powerChanged = !playerPower.containsKey(p) || playerPower.get(p) != currentPower;
        if (powerChanged) {
            playerPower.put(p, currentPower);
        }

        ScoreUtils utils = new ScoreUtils(p);
        boolean zoneChanged = !utils.getLocationNameFaction(e.getFrom()).equalsIgnoreCase(utils.getLocationNameFaction(e.getTo()));
        if (powerChanged || zoneChanged) {
            updateScoreboard(p, e.getTo());
        }
    }

    @EventHandler
    public void onMcMMOLevelUp(McMMOPlayerLevelUpEvent e) {
        updateScoreboard(e.getPlayer(), null);
    }

    public static void updateScoreboard(Player p, Location to) {
        if (p == null || !ENABLED_WORLDS.contains(p.getWorld().getName())) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            playerBoards.remove(p);
            scoreboardCache.remove(p);
            return;
        }

        NDPlayer mp = DataManager.players.get(p.getName());
        if (mp == null) return;
        NDFaction faction = mp.getFaction();
        ScoreUtils utils = new ScoreUtils(p);
        Location loc = to != null ? to : p.getLocation();
        String zoneName = utils.getLocationNameFaction(loc);
        int onlineMembers = faction != null ? getOnlineMembers(faction) : 0;

        // Prepare scoreboard title
        String title = SCOREBOARD_TITLE
                .replace("&", "§")
                .replace("{mundo}", p.getWorld().getName())
                .replace("{zona}", zoneName)
                .replace("{mcmmo}", String.valueOf(getMcMMOPowerLevel(p)))
                .replace("{power}", String.valueOf(mp.getPoder()))
                .replace("{powermax}", String.valueOf(mp.getPodermax()))
                .replace("{tag}", faction != null ? faction.getTag() : "N/A")
                .replace("{faction}", faction != null ? faction.getNome() : "Sem Facção")
                .replace("{online}", String.valueOf(onlineMembers))
                .replace("{facmembers}", faction != null ? String.valueOf(faction.getAll().size()) : "0")
                .replace("{powerfac}", faction != null ? String.valueOf(faction.getPoder()) : "0")
                .replace("{powerfacmax}", faction != null ? String.valueOf(faction.getPoderMax()) : "0")
                .replace("{terras}", faction != null ? String.valueOf(faction.getTerras().size()) : "0")
                .replace("{money}", utils.getPlayerCoins())
                .replace("{tempoattack}", faction != null ? getAttackTime(faction) : "-/-")
                .replace("{attack}", faction != null && isFactionUnderAttack(faction) ? ATTACK_INDICATOR : "§f");
        title = title.length() > 32 ? title.substring(0, 32) : title;

        // Prepare scoreboard lines
        List<String> configLines = faction != null ? WITH_FACTION_LINES : NO_FACTION_LINES;
        List<String> lines = new ArrayList<>();
        for (String line : configLines) {
            line = line.replace("&", "§")
                    .replace("{mundo}", p.getWorld().getName())
                    .replace("{zona}", zoneName)
                    .replace("{mcmmo}", String.valueOf(getMcMMOPowerLevel(p)))
                    .replace("{power}", String.valueOf(mp.getPoder()))
                    .replace("{powermax}", String.valueOf(mp.getPodermax()))
                    .replace("{tag}", faction != null ? faction.getTag() : "N/A")
                    .replace("{faction}", faction != null ? faction.getNome() : "Sem Facção")
                    .replace("{online}", String.valueOf(onlineMembers))
                    .replace("{facmembers}", faction != null ? String.valueOf(faction.getAll().size()) : "0")
                    .replace("{powerfac}", faction != null ? String.valueOf(faction.getPoder()) : "0")
                    .replace("{powerfacmax}", faction != null ? String.valueOf(faction.getPoderMax()) : "0")
                    .replace("{terras}", faction != null ? String.valueOf(faction.getTerras().size()) : "0")
                    .replace("{money}", utils.getPlayerCoins())
                    .replace("{tempoattack}", faction != null ? getAttackTime(faction) : "-/-")
                    .replace("{attack}", faction != null && isFactionUnderAttack(faction) ? ATTACK_INDICATOR : "§f");
            try {
                line = PlaceholderAPI.setPlaceholders(p, line);
            } catch (NoClassDefFoundError ignored) {
            	
            }
            lines.add(line.length() > 40 ? line.substring(0, 40) : line);
        }

        // Check if update is needed
        ScoreboardCache cache = scoreboardCache.get(p);
        if (cache != null && !cache.isDifferent(title, lines, zoneName, mp.getPoder(), onlineMembers)) {
            return; // No changes, skip update
        }
        scoreboardCache.put(p, new ScoreboardCache(title, lines, zoneName, mp.getPoder(), onlineMembers));

        // Update scoreboard
        Scoreboard board = playerBoards.computeIfAbsent(p, k -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective obj = board.getObjective("factions");
        if (obj == null) {
            obj = board.registerNewObjective("factions", "dummy"); // Apenas name e criteria
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        obj.setDisplayName(title); // Título definido separadamente


        // Update only changed entries
        Set<String> currentEntries = new HashSet<>(board.getEntries());
        int score = lines.size();
        for (String line : lines) {
            Team team = board.getTeam("line" + score);
            if (team == null) {
                team = board.registerNewTeam("line" + score);
            }
            if (!team.hasEntry(line)) {
                team.addEntry(line);
            }
            obj.getScore(line).setScore(score);
            currentEntries.remove(line);
            score--;
        }

        // Remove outdated entries
        for (String oldEntry : currentEntries) {
            board.resetScores(oldEntry);
            Team team = board.getTeam("line" + board.getObjective("factions").getScore(oldEntry).getScore());
            if (team != null) {
                team.unregister();
            }
        }

        p.setScoreboard(board);
    }

    private static int getMcMMOPowerLevel(Player p) {
        try {
            return ExperienceAPI.getPowerLevelOffline(p.getUniqueId());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getOnlineMembers(NDFaction faction) {
        if (faction == null) return 0;
        int online = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            NDPlayer mp = DataManager.players.get(p.getName());
            if (mp != null && mp.hasFaction() && mp.getFaction() == faction && !p.hasMetadata("Saiu")) {
                online++;
            }
        }
        return online;
    }

    public static String getAttackTime(NDFaction faction) {
        if (faction == null || !faction.isSobAtaque()) {
        	SobAtaque.attackStartTimes.remove(faction);
        	SobAtaque.lastExplosionTimes.remove(faction);
            return "-/-";
        }

        Long startTime = SobAtaque.attackStartTimes.get(faction);
        Long lastExplosionTime = SobAtaque.lastExplosionTimes.get(faction);

        if (startTime == null || lastExplosionTime == null) {
            return "-/-";
        }

        long now = System.currentTimeMillis();

        long remainingFromLastExplosion = (lastExplosionTime + 5 * 60 * 1000) - now; // 5 min
        long remainingFromStart = (startTime + 2 * 60 * 60 * 1000) - now; // 2h

        long remainingTime = Math.min(remainingFromLastExplosion, remainingFromStart);

        if (remainingTime <= 0) {
            SobAtaque.cooldown.remove(faction);
            SobAtaque.attackStartTimes.remove(faction);
            SobAtaque.lastExplosionTimes.remove(faction);
            return "-/-";
        }

        return formatTime(remainingTime);
    }


    private static boolean isFactionUnderAttack(NDFaction faction) {
        return faction != null && faction.isSobAtaque();
    }

    private static String formatTime(long tempo) {
        if (tempo <= 0) return "0s";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tempo);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(tempo) % 60;
        return (minutes > 0 ? minutes + "m " : "") + seconds + "s";
    }

}