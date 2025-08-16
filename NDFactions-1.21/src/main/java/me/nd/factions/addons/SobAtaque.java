package me.nd.factions.addons;

import me.nd.factions.Main;
import me.nd.factions.api.ActionBar;
import me.nd.factions.api.Config;
import me.nd.factions.comandos.AdminCommands;
import me.nd.factions.factions.API;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.ColorData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SobAtaque implements Listener {

    public static final Set<NDFaction> cooldown = ConcurrentHashMap.newKeySet();
    public static final Set<Player> fastUpdatePlayers = ConcurrentHashMap.newKeySet();
    private static final Set<Material> blocosBloqueados = new HashSet<>();
    public static final Map<NDFaction, Long> attackStartTimes = new ConcurrentHashMap<>();
    public static final Map<NDFaction, Long> lastExplosionTimes = new ConcurrentHashMap<>();

    public static final long MAX_ATTACK_DURATION = 2 * 60 * 60 * 1000L; // 2 horas em ms
    public static final long RESET_TIMER_ON_EXPLOSION = 5 * 60 * 1000L; // 5 minutos em ms

    static {
        List<?> raw = (List<?>) Config.get("SobAtaque.BloquearQuebrarBlocos");
        for (Object o : raw) {
            try {
                Material mat = Material.getMaterial(String.valueOf(Integer.parseInt(o.toString())));
                if (mat != null) blocosBloqueados.add(mat);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static long getAttackStartTime(NDFaction faction) {
        return attackStartTimes.getOrDefault(faction, -1L);
    }

    @EventHandler
    public void onTNTPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.TNT) {
            event.getBlock().setMetadata("placed_by", new FixedMetadataValue(Main.get(), event.getPlayer().getName()));
        }
    }

    @EventHandler
    public void onTNTPrime(EntitySpawnEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getEntity();
            Block block = tnt.getLocation().getBlock();
            if (block.hasMetadata("placed_by")) {
                String name = block.getMetadata("placed_by").get(0).asString();
                tnt.setMetadata("placed_by", new FixedMetadataValue(Main.get(), name));
                block.removeMetadata("placed_by", Main.get());
            }
        }
    }

    private Player getExploder(Entity entity) {
        if (entity.hasMetadata("placed_by")) {
            List<MetadataValue> metadata = entity.getMetadata("placed_by");
            if (!metadata.isEmpty()) {
                String playerName = metadata.get(0).asString();
                return Bukkit.getPlayerExact(playerName);
            }
        }
        return null;
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        NDFaction faction = API.getPlayer(player.getName()).getFaction();
        if (faction != null && cooldown.contains(faction) && blocosBloqueados.contains(event.getBlock().getType())) {
            event.setCancelled(true);
            player.sendMessage(Main.get().getConfig().getString("Mensagens.DuranteInvasao").replace("&", "§"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        NDFaction faction = API.getPlayer(player.getName()).getFaction();

        if (faction != null && cooldown.contains(faction) && blocosBloqueados.contains(event.getBlock().getType())) {
            event.setCancelled(true);
            player.sendMessage(Main.get().getConfig().getString("Mensagens.NaoPodeInvasao").replace("&", "§"));
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        try {
            Terra terra = new Terra(event.getEntity().getWorld(), event.getEntity().getLocation().getChunk().getX(), event.getEntity().getLocation().getChunk().getZ());
            NDFaction factionDefender = terra.getFaction();

            if (AdminCommands.globalGraceExpiration > System.currentTimeMillis()) {
                event.setCancelled(true);
                return;
            }

            if (factionDefender == null) return;
            if (factionDefender.hasImmunity()) {
                event.setCancelled(true);
                return;
            }

            if (AdminCommands.graceFactions.containsKey(factionDefender)) {
                event.setCancelled(true);
                return;
            }

            if (AdminCommands.sobAtaqueDisabled.getOrDefault(factionDefender, false)) {
                event.setCancelled(true);
                return;
            }

            // Identificar a facção atacante
            NDFaction factionAttacker = null;
            if (event.getEntity().getType() == EntityType.TNT || event.getEntity().getType() == EntityType.CREEPER) {
                // Tentar encontrar o jogador que causou a explosão
                Player attacker = getExploder(event.getEntity());
                if (event.getEntity().hasMetadata("placed_by")) {
                    String playerName = event.getEntity().getMetadata("placed_by").get(0).asString();
                    attacker = Bukkit.getPlayer(playerName);
                } else if (attacker == null && event.getEntity().getType() == EntityType.CREEPER) {
                    for (Entity nearby : event.getEntity().getNearbyEntities(5, 5, 5)) {
                        if (nearby instanceof Player) {
                            attacker = (Player) nearby;
                            break;
                        }
                    }
                }

                if (attacker != null) {
                    factionAttacker = API.getPlayer(attacker.getName()).getFaction();
                }
            }

            long now = System.currentTimeMillis();

            // Atualiza o tempo da última explosão sempre
            lastExplosionTimes.put(factionDefender, now);

            if (!cooldown.contains(factionDefender)) {
                cooldown.add(factionDefender);
                attackStartTimes.put(factionDefender, now);
                fastUpdatePlayers.addAll(factionDefender.getAllOnline());

                // Enviar mensagem global se houver uma facção atacante identificada
                if (factionAttacker != null && !factionAttacker.equals(factionDefender)) {
                    String message = "§7 * §f[" + factionAttacker.getTag() + "] §cestá atacando a §f[" + factionDefender.getTag() + "]";
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage(message);
                    Bukkit.broadcastMessage("");
                }

                ColorData data = new ColorData(Config.get("SobAtaque.Mensagem").toString().replace("&", "§"), "§4", "§c");

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!cooldown.contains(factionDefender)) {
                            cancel();
                            return;
                        }

                        long start = attackStartTimes.getOrDefault(factionDefender, now);
                        long last = lastExplosionTimes.getOrDefault(factionDefender, now);
                        long timeNow = System.currentTimeMillis();

                        // Verifica se passou o tempo total de ataque (2 horas)
                        if (timeNow - start >= MAX_ATTACK_DURATION) {
                            encerrarAtaque(factionDefender, true);
                            cancel();
                            return;
                        }

                        // Verifica se passou o tempo sem explosões (5 minutos)
                        if (timeNow - last >= RESET_TIMER_ON_EXPLOSION) {
                            encerrarAtaque(factionDefender, false);
                            cancel();
                            return;
                        }

                        data.next();
                        String msg = data.getMessage();
                        for (Player p : factionDefender.getAllOnline()) {
                            ActionBar.sendActionBarMessage(p, msg);
                        }
                    }
                }.runTaskTimerAsynchronously(Main.getPlugin(Main.class), 0L, 5L); // 100 ticks = 5 segundos
            }

        } catch (Exception ex) {
            Bukkit.getLogger().warning("[SobAtaque] Erro ao processar explosão: " + ex.getMessage());
        }
    }

    public static long getAttackTime(NDFaction faction) {
        if (!cooldown.contains(faction)) return -1L;

        long start = attackStartTimes.getOrDefault(faction, -1L);
        if (start == -1L) return -1L;

        long elapsed = System.currentTimeMillis() - start;
        long duration = 5 * 60 * 1000L; // 5 minutos em ms
        long remaining = duration - elapsed;

        return Math.max(remaining / 1000L, 0L); // retorna o tempo restante em segundos
    }

    public static void encerrarAtaque(NDFaction faction, boolean maxTempo) {
        cooldown.remove(faction);
        attackStartTimes.remove(faction);
        lastExplosionTimes.remove(faction);
        fastUpdatePlayers.removeAll(faction.getAllOnline());

        if (maxTempo && faction.canActivateImmunity()) {
            faction.activateImmunity();
            faction.broadcast(Main.get().getConfig().getString("Mensagens.InvasaoDurouMuito")
                    .replace("&", "§")
                    .replace("<tag>", faction.getTag())
                    .replace("<nome>", faction.getNome()));
        } else if (maxTempo) {
            Bukkit.getLogger().warning("Falha ao ativar imunidade automática para facção " + faction.getNome() + ": em cooldown.");
        }
    }
}