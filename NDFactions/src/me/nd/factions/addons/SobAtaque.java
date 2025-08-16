package me.nd.factions.addons;

import me.nd.factions.Main;
import me.nd.factions.api.ActionBar;
import me.nd.factions.api.Config;
import me.nd.factions.comandos.AdminCommands;
import me.nd.factions.comandos.Tregua;
import me.nd.factions.factions.API;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.ColorData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SobAtaque implements Listener {

    public static final Set<NDFaction> cooldown = ConcurrentHashMap.newKeySet();
    public static final Set<Player> fastUpdatePlayers = ConcurrentHashMap.newKeySet();
    private static final Set<Material> blocosBloqueados = new HashSet<>();
    public static final Map<NDFaction, Long> attackStartTimes = new ConcurrentHashMap<>();
    public static final Map<NDFaction, Long> lastExplosionTimes = new ConcurrentHashMap<>();
    private static final Map<NDFaction, NDFaction> pausedAttacks = new ConcurrentHashMap<>();
    public static final long RESET_TIMER_ON_EXPLOSION = 5 * 60 * 1000L;

    static {
        List<String> raw = (List<String>) Config.get("SobAtaque.BloquearQuebrarBlocos");
        for (String materialName : raw) {
            Material mat = Material.getMaterial(materialName);
            if (mat != null) blocosBloqueados.add(mat);
        }
        DataManager.initAttacksFile();
    }

    public static long getMaxAttackDuration(NDFaction faction) {
        int memberCount = faction.getAllMembers().size();
        return memberCount <= 10 ? 2 * 60 * 60 * 1000L : 3 * 60 * 60 * 1000L;
    }

    public static void createTemporaryZone(NDFaction factionDefender, NDFaction factionAttacker, Terra attackedTerra) {
        if (factionAttacker == null) {
            Bukkit.getLogger().warning("Não foi possível criar zona temporária: facção atacante é nula.");
            return;
        }

        World world = attackedTerra.getWorld();
        int centerX = attackedTerra.getX();
        int centerZ = attackedTerra.getZ();

        // Calcular tamanho da zona com base na diferença de poder
        int powerDifference = Math.abs(factionAttacker.getPoder() - factionDefender.getPoder());
        int distance = Math.min(4, Math.max(2, 4 - powerDifference / 100)); // Ex.: 2 a 4 chunks, baseado no poder

        List<Terra> temporaryChunks = new ArrayList<>();
        for (int x = centerX - distance; x <= centerX + distance; x++) {
            for (int z = centerZ - distance; z <= centerZ + distance; z++) {
                Terra tempTerra = new Terra(world, x, z);
                if (tempTerra.getFaction() == null) {
                    temporaryChunks.add(tempTerra);
                }
            }
        }

        factionAttacker.getTemporarios().addAll(temporaryChunks);
        try {
            factionAttacker.save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar zonas temporárias para a facção " + factionAttacker.getNome() + ": " + e.getMessage());
        }

        String zoneSize = distance + "x" + distance;
        if (!temporaryChunks.isEmpty()) {
            factionAttacker.broadcast("§cUma zona temporária de ataque (" + zoneSize + ") foi criada ao redor do território de §f[" + factionDefender.getTag() + "]!");
            factionDefender.broadcast("§cA facção §f[" + factionAttacker.getTag() + "] §ccriou uma zona temporária (" + zoneSize + ") ao redor do seu território sob ataque!");
        } else {
            factionAttacker.broadcast("§cNenhuma zona temporária foi criada, pois todos os chunks ao redor já estão dominados!");
            factionDefender.broadcast("§cNenhuma zona temporária foi criada pela facção §f[" + factionAttacker.getTag() + "], pois todos os chunks ao redor já estão dominados!");
        }
    }

    public static long getAttackStartTime(NDFaction faction) {
        return attackStartTimes.getOrDefault(faction, -1L);
    }

    public static NDFaction getAttackingFaction(NDFaction defender) {
        String attackerName = DataManager.loadAttacker(defender.getNome());
        if (attackerName != null && !attackerName.equals("Desconhecido")) {
            NDFaction attacker = DataManager.factions.get(attackerName);
            if (attacker != null) {
                return attacker;
            }
        }
        return null;
    }

    private static NDFaction getAttackingFaction(Entity entity, Location location, NDFaction factionDefender) {
        NDFaction registeredAttacker = getAttackingFaction(factionDefender);
        if (registeredAttacker != null) {
            return registeredAttacker;
        }

        if (entity instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) entity;
            Entity source = tnt.getSource();
            if (source instanceof Player) {
                Player player = (Player) source;
                NDPlayer ndPlayer = API.getPlayer(player.getName());
                if (ndPlayer != null && ndPlayer.hasFaction()) {
                    NDFaction faction = ndPlayer.getFaction();
                    if (faction != factionDefender) {
                        return faction;
                    }
                }
            }
        } else if (entity instanceof Player) {
            NDPlayer ndPlayer = API.getPlayer(((Player) entity).getName());
            if (ndPlayer != null && ndPlayer.hasFaction()) {
                NDFaction faction = ndPlayer.getFaction();
                if (faction != factionDefender) {
                    return faction;
                }
            }
        }

        Map<NDFaction, Integer> factionPlayerCount = new HashMap<>();
        Map<NDFaction, Double> factionClosestDistance = new HashMap<>();
        if (location != null) {
            for (Entity nearby : entity.getWorld().getNearbyEntities(location, 40, 40, 40)) {
                if (nearby instanceof Player) {
                    Player nearbyPlayer = (Player) nearby;
                    NDPlayer ndPlayer = API.getPlayer(nearbyPlayer.getName());
                    if (ndPlayer != null && ndPlayer.hasFaction()) {
                        NDFaction faction = ndPlayer.getFaction();
                        if (faction != factionDefender) {
                            factionPlayerCount.merge(faction, 1, Integer::sum);
                            double distance = nearbyPlayer.getLocation().distanceSquared(location);
                            factionClosestDistance.compute(faction, (key, currentDistance) ->
                                    (currentDistance == null || distance < currentDistance) ? distance : currentDistance);
                        }
                    }
                }
            }
        }

        NDFaction attackingFaction = null;
        int maxPlayers = 0;
        for (Map.Entry<NDFaction, Integer> entry : factionPlayerCount.entrySet()) {
            if (entry.getValue() > maxPlayers) {
                maxPlayers = entry.getValue();
                attackingFaction = entry.getKey();
            }
        }

        if (maxPlayers > 0) {
            List<NDFaction> tiedFactions = new ArrayList<>();
            for (Map.Entry<NDFaction, Integer> entry : factionPlayerCount.entrySet()) {
                if (entry.getValue() == maxPlayers) {
                    tiedFactions.add(entry.getKey());
                }
            }
            if (tiedFactions.size() > 1) {
                double minDistance = Double.MAX_VALUE;
                for (NDFaction faction : tiedFactions) {
                    double distance = factionClosestDistance.getOrDefault(faction, Double.MAX_VALUE);
                    if (distance < minDistance) {
                        minDistance = distance;
                        attackingFaction = faction;
                    }
                }
            }
        }

        return attackingFaction;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        NDPlayer ndPlayer = API.getPlayer(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) return;

        NDFaction faction = ndPlayer.getFaction();
        Terra terra = new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
        NDFaction territoryOwner = terra.getFaction();

        if (territoryOwner != null && !territoryOwner.equals(faction) && Tregua.isTruceActive(faction, territoryOwner)) {
            event.setCancelled(true);
            player.sendMessage("§cVocê não pode colocar blocos no território de §f[" + territoryOwner.getTag() + "] §cdurante uma trégua!");
            return;
        }

        if (faction != null && cooldown.contains(faction) && blocosBloqueados.contains(event.getBlock().getType())) {
            event.setCancelled(true);
            player.sendMessage(Main.get().getConfig().getString("Mensagens.DuranteInvasao").replace("&", "§"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        NDPlayer ndPlayer = API.getPlayer(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) return;

        NDFaction faction = ndPlayer.getFaction();
        Terra terra = new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
        NDFaction territoryOwner = terra.getFaction();

        if (territoryOwner != null && !territoryOwner.equals(faction) && Tregua.isTruceActive(faction, territoryOwner)) {
            event.setCancelled(true);
            player.sendMessage("§cVocê não pode quebrar blocos no território de §f[" + territoryOwner.getTag() + "] §cdurante uma trégua!");
            return;
        }

        if (faction != null && cooldown.contains(faction) && blocosBloqueados.contains(event.getBlock().getType())) {
            event.setCancelled(true);
            player.sendMessage(Main.get().getConfig().getString("Mensagens.NaoPodeInvasao").replace("&", "§"));
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        try {
            if (event.getEntity() instanceof TNTPrimed && event.getEntity().hasMetadata("CanhaoTnT")) {
                return; // Skip attack logic for TNT with "SouTntImpulsao"
            }
            Terra terra = new Terra(event.getEntity().getWorld(), event.getEntity().getLocation().getChunk().getX(), event.getEntity().getLocation().getChunk().getZ());
            NDFaction factionDefender = terra.getFaction();

            if (AdminCommands.globalGraceExpiration > System.currentTimeMillis()) {
                event.setCancelled(true);
                return;
            }
            
            if (factionDefender == null) {
                return;
            }
            
            if (terra.isTemporario()) {
            	return;
            }
            
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

            NDFaction factionAttacker = getAttackingFaction(event.getEntity(), event.getLocation(), factionDefender);
            if (factionAttacker != null && Tregua.isTruceActive(factionDefender, factionAttacker)) {
                event.setCancelled(true);
                if (event.getEntity() instanceof TNTPrimed && ((TNTPrimed) event.getEntity()).getSource() instanceof Player) {
                    ((Player) ((TNTPrimed) event.getEntity()).getSource()).sendMessage("§cExplosões estão bloqueadas devido à trégua com §f[" + factionDefender.getTag() + "]!");
                }
                return;
            }

            long now = System.currentTimeMillis();

            // Track destroyed spawners
            Map<EntityType, Integer> destroyedInExplosion = new HashMap<>();
            Location centers = event.getLocation();
            World world = centers.getWorld();
            int radiuss = 5;
            int centerX = centers.getBlockX();
            int centerY = centers.getBlockY();
            int centerZ = centers.getBlockZ();

            for (int x = centerX - radiuss; x <= centerX + radiuss; x++) {
                for (int y = centerY - radiuss; y <= centerY + radiuss; y++) {
                    for (int z = centerZ - radiuss; z <= centerZ + radiuss; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.MOB_SPAWNER) {
                            CreatureSpawner spawner = (CreatureSpawner) block.getState();
                            EntityType type = spawner.getSpawnedType();
                            if (type != null) {
                                destroyedInExplosion.merge(type, 1, Integer::sum);
                                factionDefender.removePlacedGenerator(type, 1, null);
                            } else {
                                Bukkit.getLogger().warning("Spawner sem tipo definido encontrado em " + block.getLocation().toString());
                            }
                        }
                    }
                }
            }

            // Update attacker's destroyed spawners
            if (factionAttacker != null) {
                for (Map.Entry<EntityType, Integer> entry : destroyedInExplosion.entrySet()) {
                    factionAttacker.addDestroyedSpawner(entry.getKey(), entry.getValue());
                }
            }

            // Offer generation
            EntityType spawnerType = null;
            Location center = event.getLocation();
            int radius = 5;
            try {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block block = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                            if (block.getType() == Material.MOB_SPAWNER) {
                                CreatureSpawner spawner = (CreatureSpawner) block.getState();
                                spawnerType = spawner.getSpawnedType();
                                break;
                            }
                        }
                        if (spawnerType != null) break;
                    }
                    if (spawnerType != null) break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (spawnerType != null) {
                OfferManager.generateOffer(factionDefender, spawnerType);
            }

            lastExplosionTimes.put(factionDefender, now);

            if (!cooldown.contains(factionDefender)) {
                cooldown.add(factionDefender);
                attackStartTimes.put(factionDefender, now);
                fastUpdatePlayers.addAll(factionDefender.getAllOnline());

                createTemporaryZone(factionDefender, factionAttacker, terra);

                if (factionAttacker != null && factionAttacker != factionDefender) {
                    DataManager.saveAttack(factionDefender.getNome(), factionAttacker.getNome());
                    String message = "§7 * §f[" + factionAttacker.getTag() + "] §cestá atacando a §f[" + factionDefender.getTag() + "]";
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage(message);
                    Bukkit.broadcastMessage("");
                } else {
                    DataManager.saveAttack(factionDefender.getNome(), "Desconhecido");
                    String message = "§7 * §cA facção §f[" + factionDefender.getTag() + "] §cestá sob ataque!";
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

                        long maxAttackDuration = getMaxAttackDuration(factionDefender);
                        if (timeNow - start >= maxAttackDuration) {
                            encerrarAtaque(factionDefender, factionAttacker, true);
                            cancel();
                            return;
                        }

                        if (timeNow - last >= RESET_TIMER_ON_EXPLOSION) {
                            encerrarAtaque(factionDefender, factionAttacker, false);
                            cancel();
                            return;
                        }

                        data.next();
                        String msg = data.getMessage();
                        for (Player p : factionDefender.getAllOnline()) {
                            ActionBar.sendActionBarMessage(p, msg);
                        }
                    }
                }.runTaskTimerAsynchronously(Main.getPlugin(Main.class), 0L, 20L);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static long getAttackTime(NDFaction faction) {
        if (!cooldown.contains(faction)) return -1L;

        long start = attackStartTimes.getOrDefault(faction, -1L);
        if (start == -1L) return -1L;

        long elapsed = System.currentTimeMillis() - start;
        long duration = 5 * 60 * 1000L;
        long remaining = duration - elapsed;

        return Math.max(remaining / 1000L, 0L);
    }

    public static void encerrarAtaque(NDFaction factionDefender, NDFaction factionAttacker, boolean maxTempo) {
        cooldown.remove(factionDefender);
        attackStartTimes.remove(factionDefender);
        lastExplosionTimes.remove(factionDefender);
        fastUpdatePlayers.removeAll(factionDefender.getAllOnline());
        DataManager.removeAttack(factionDefender.getNome());
        pausedAttacks.remove(factionDefender);

        // Incrementar invasões bem-sucedidas se pelo menos um spawner foi destruído ou o tempo máximo foi atingido
        if (factionAttacker != null) {
                factionAttacker.incrementSuccessfulInvasions();
        } else {
            Bukkit.getLogger().warning("Nenhuma invasão bem-sucedida registrada: facção atacante é nula.");
        }

        // Remover zonas temporárias da facção atacante
        if (factionAttacker != null) {
            factionAttacker.getTemporarios().clear();
            try {
                factionAttacker.save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao salvar facção atacante " + factionAttacker.getNome() + " após remover zonas temporárias: " + e.getMessage());
            }
        }

        if (maxTempo && factionDefender.canActivateImmunity()) {
            factionDefender.activateImmunity();
            factionDefender.broadcast(Main.get().getConfig().getString("Mensagens.InvasaoDurouMuito")
                    .replace("&", "§")
                    .replace("<tag>", factionDefender.getTag())
                    .replace("<nome>", factionDefender.getNome()));
        } else if (maxTempo) {
            Bukkit.getLogger().warning("Falha ao ativar imunidade automática para facção " + factionDefender.getNome() + ": em cooldown.");
        }

        factionDefender.broadcast("§aA zona temporária de ataque foi removida!");
        if (factionAttacker != null) {
            factionAttacker.broadcast("§aO ataque contra §f[" + factionDefender.getTag() + "] §afoi encerrado!");
        }
    }

    private Location findSafeLocation(World world, int centerX, int centerZ, NDFaction factionDefender, NDFaction factionAttacker) {
        int maxDistance = 10;
        for (int distance = 5; distance <= maxDistance; distance++) {
            for (int x = centerX - distance; x <= centerX + distance; x++) {
                for (int z = centerZ - distance; z <= centerZ + distance; z++) {
                    if (Math.abs(x - centerX) != distance && Math.abs(z - centerZ) != distance) continue;

                    Terra tempTerra = new Terra(world, x, z);
                    if (tempTerra.getFaction() == null &&
                            (factionAttacker == null || !factionAttacker.getTemporarios().contains(tempTerra)) &&
                            !factionDefender.ownsTerritory(tempTerra)) {
                        Location center = new Location(world, (x << 4) + 8, 64, (z << 4) + 8);
                        Location safeLoc = world.getHighestBlockAt(center).getLocation().add(0, 1, 0);
                        Block block = safeLoc.getBlock();
                        Block above = safeLoc.getWorld().getBlockAt(safeLoc.getBlockX(), safeLoc.getBlockY() + 1, safeLoc.getBlockZ());

                        if (block.getType().isSolid() && above.getType() == Material.AIR) {
                            safeLoc.setY(block.getY() + 1);
                            return safeLoc;
                        }
                    }
                }
            }
        }

        Location spawn = world.getSpawnLocation();
        Block spawnBlock = spawn.getWorld().getHighestBlockAt(spawn);
        spawn.setY(spawnBlock.getY() + 1);
        return spawn;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        NDPlayer ndPlayer = API.getPlayer(player.getName());
        if (ndPlayer == null) return;

        Location to = event.getTo();
        if (to == null) return;

        Location from = event.getFrom();
        if (from.getWorld() == to.getWorld() && from.getChunk().getX() == to.getChunk().getX() && from.getChunk().getZ() == to.getChunk().getZ()) {
            return;
        }

        Terra toTerra = new Terra(to.getWorld(), to.getChunk().getX(), to.getChunk().getZ());

        for (NDFaction factionDefender : cooldown) {
            NDFaction factionAttacker = getAttackingFaction(factionDefender);
            if (factionAttacker != null && (factionAttacker.getTemporarios().contains(toTerra) || factionDefender.ownsTerritory(toTerra))) {
                NDFaction playerFaction = ndPlayer.hasFaction() ? ndPlayer.getFaction() : null;

                if (playerFaction != null && (playerFaction.equals(factionDefender) || playerFaction.equals(factionAttacker))) {
                    return;
                }

                Location safeLocation = findSafeLocation(to.getWorld(), toTerra.getX(), toTerra.getZ(), factionDefender, factionAttacker);
                player.teleport(safeLocation);
                player.sendMessage("§cVocê não pode entrar na zona de ataque de §f[" + factionDefender.getTag() + "] controlada por §f[" +
                        factionAttacker.getTag() + "]! Você foi teleportado para um local seguro.");
                return;
            }
        }
    }

    public static void pauseAttack(NDFaction defender, NDFaction attacker, long duration) {
        if (defender == null || attacker == null || duration <= 0) {
            Bukkit.getLogger().warning("Tentativa de pausar ataque com parâmetros inválidos: defender=" + defender + ", attacker=" + attacker + ", duration=" + duration);
            return;
        }

        if (!cooldown.contains(defender)) {
            Bukkit.getLogger().warning("Facção " + defender.getTag() + " não está sob ataque para pausar.");
            return;
        }

        pausedAttacks.put(defender, attacker);
        cooldown.remove(defender);
        fastUpdatePlayers.removeAll(defender.getAllOnline());
        if (attacker != null) {
            attacker.getTemporarios().clear();
            try {
                attacker.save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao salvar facção atacante " + attacker.getNome() + " após remover zonas temporárias: " + e.getMessage());
            }
        }

        defender.broadcast("§aO ataque de §f[" + attacker.getTag() + "] §afoi pausado devido à trégua por " + (duration / 60000) + " minutos!");
        attacker.broadcast("§aO ataque contra §f[" + defender.getTag() + "] §afoi pausado devido à trégua por " + (duration / 60000) + " minutos!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pausedAttacks.remove(defender) != null) {
                    cooldown.add(defender);
                    attackStartTimes.put(defender, System.currentTimeMillis());
                    fastUpdatePlayers.addAll(defender.getAllOnline());
                    DataManager.saveAttack(defender.getNome(), attacker.getNome());

                    String message = "§7 * §f[" + attacker.getTag() + "] §cestá atacando a §f[" + defender.getTag() + "] novamente após o fim da trégua!";
                    Bukkit.broadcastMessage("");
                    Bukkit.broadcastMessage(message);
                    Bukkit.broadcastMessage("");

                    ColorData data = new ColorData(Config.get("SobAtaque.Mensagem").toString().replace("&", "§"), "§4", "§c");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!cooldown.contains(defender)) {
                                cancel();
                                return;
                            }

                            long start = attackStartTimes.getOrDefault(defender, System.currentTimeMillis());
                            long last = lastExplosionTimes.getOrDefault(defender, System.currentTimeMillis());
                            long timeNow = System.currentTimeMillis();

                            long maxAttackDuration = getMaxAttackDuration(defender);
                            if (timeNow - start >= maxAttackDuration) {
                                encerrarAtaque(defender, attacker, true);
                                cancel();
                                return;
                            }

                            if (timeNow - last >= RESET_TIMER_ON_EXPLOSION) {
                                encerrarAtaque(defender, attacker, false);
                                cancel();
                                return;
                            }

                            data.next();
                            String msg = data.getMessage();
                            for (Player p : defender.getAllOnline()) {
                                ActionBar.sendActionBarMessage(p, msg);
                            }
                        }
                    }.runTaskTimerAsynchronously(Main.getPlugin(Main.class), 0L, 20L);
                }
            }
        }.runTaskLater(Main.getPlugin(Main.class), duration / 50L);
    }
}
