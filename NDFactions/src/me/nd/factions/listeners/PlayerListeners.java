package me.nd.factions.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import me.nd.factions.Main;
import me.nd.factions.api.ActionBar;
import me.nd.factions.api.Config;
import me.nd.factions.comandos.AdminCommands;
import me.nd.factions.comandos.Comandos;
import me.nd.factions.enums.Cargo;
import me.nd.factions.enums.Protecao;
import me.nd.factions.enums.Relacao;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.scoreboard.ScoreUtils;
import me.nd.factions.utils.Utils;
import me.nd.factionsutils.listeners.itens.OlhoDeDeusListener;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class PlayerListeners implements Listener {

    public static HashMap<String, String> antiflood = new HashMap<>();
    private static final List<Material> BLOCKS = Arrays.asList(
            Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR, Material.JUNGLE_DOOR,
            Material.SPRUCE_DOOR, Material.WOOD_DOOR, Material.IRON_DOOR, Material.LEVER, Material.STONE_BUTTON,
            Material.WOOD_BUTTON, Material.CHEST, Material.TRAPPED_CHEST, Material.BURNING_FURNACE,
            Material.FURNACE, Material.BUCKET, Material.LAVA_BUCKET, Material.WATER_BUCKET, Material.DROPPER,
            Material.DISPENSER, Material.HOPPER, Material.RAILS, Material.MINECART, Material.STORAGE_MINECART,
            Material.HOPPER_MINECART, Material.POWERED_MINECART
    );

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.MOB_SPAWNER) {
            Player player = event.getPlayer();
            ItemStack item = event.getItemInHand();
            EntityType newSpawnerType = FactionGenerators.getSpawnerType(item);
            if (newSpawnerType == null) {
                event.setCancelled(true);
                return;
            }

            Terra terra = new Terra(event.getBlock().getWorld(), event.getBlock().getChunk().getX(), event.getBlock().getChunk().getZ());
            NDFaction faction = terra.getFaction();

            NDPlayer ndPlayer = DataManager.players.get(player.getName());
            if (ndPlayer == null || !ndPlayer.hasFaction()) {
                player.sendMessage(formatMessage("Mensagens.SemFaccao"));
                event.setCancelled(true);
                return;
            }

            NDFaction playerFaction = ndPlayer.getFaction();

            if (faction == null || !faction.equals(playerFaction)) {
                player.sendMessage(formatMessage("Mensagens.SemPermissaoTerritorio"));
                event.setCancelled(true);
                return;
            }

            if (playerFaction.getTemporarios().contains(terra)) {
                player.sendMessage(Config.get("Mensagens.NaoPodeColocarSpawnerProtecaoTemporaria").toString().replace("&", "§"));
                event.setCancelled(true);
                return;
            }

            Block newSpawnerBlock = event.getBlock();
            boolean hasSameTypeSpawners = false;
            boolean isAdjacentToSameType = false;

            List<Terra> allTerras = new ArrayList<>();
            allTerras.addAll(playerFaction.getTerras());
            allTerras.addAll(playerFaction.getTemporarios());

            for (Terra claimedTerra : allTerras) {
                Chunk chunk = claimedTerra.getChunk();
                World world = chunk.getWorld();
                int chunkX = chunk.getX() << 4;
                int chunkZ = chunk.getZ() << 4;

                for (int x = chunkX; x < chunkX + 16; x++) {
                    for (int z = chunkZ; z < chunkZ + 16; z++) {
                        for (int y = 0; y < 256; y++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() == Material.MOB_SPAWNER) {
                                CreatureSpawner existingSpawner = (CreatureSpawner) block.getState();
                                EntityType existingType = existingSpawner.getSpawnedType();

                                if (existingType == null) {
                                    event.setCancelled(true);
                                    return;
                                }

                                if (existingType == newSpawnerType) {
                                    hasSameTypeSpawners = true;
                                    if (areBlocksAdjacent(newSpawnerBlock, block)) {
                                        isAdjacentToSameType = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (playerFaction.getTerras().size() >= 40 && newSpawnerBlock.getY() >= 150) {
                String message = Config.get("Mensagens.SpawnerAcimaY150") != null
                        ? Config.get("Mensagens.SpawnerAcimaY150").toString().replace("&", "§")
                        : "§cNão é permitido colocar spawners acima de Y=150 com mais de 40 claims.";
                player.sendMessage(message);
                event.setCancelled(true);
                return;
            }

            String adjacencyMessage = Config.get("Mensagens.SpawnerNaoAdjacente") != null
                    ? Config.get("Mensagens.SpawnerNaoAdjacente").toString().replace("&", "§")
                    : Config.get("Mensagens.SpawnerAdjacenteIgual") != null
                    ? Config.get("Mensagens.SpawnerAdjacenteIgual").toString().replace("&", "§")
                    : "§cO spawner deve ser colocado adjacente a um spawner do mesmo tipo.";
            if (hasSameTypeSpawners && !isAdjacentToSameType) {
                player.sendMessage(adjacencyMessage);
                event.setCancelled(true);
                return;
            }

            CreatureSpawner spawner = (CreatureSpawner) newSpawnerBlock.getState();
            spawner.setSpawnedType(newSpawnerType);
            spawner.update();

            playerFaction.addPlacedGenerator(newSpawnerType, 1, player.getName());
            if (playerFaction.canActivateImmunity()) {
                playerFaction.activateImmunity();
            } else {

            }
        }
    }
    
    private boolean areBlocksAdjacent(Block block1, Block block2) {
        int xDiff = Math.abs(block1.getX() - block2.getX());
        int yDiff = Math.abs(block1.getY() - block2.getY());
        int zDiff = Math.abs(block1.getZ() - block2.getZ());
        return (xDiff == 1 && yDiff == 0 && zDiff == 0) ||
               (xDiff == 0 && yDiff == 1 && zDiff == 0) ||
               (xDiff == 0 && yDiff == 0 && zDiff == 1);
    }

    private static String formatMessage(String configKey) {
        return Config.get(configKey).toString().replace("&", "§");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.MOB_SPAWNER) {
            CreatureSpawner spawner = (CreatureSpawner) event.getBlock().getState();
            EntityType type = spawner.getSpawnedType();
            if (type == null) {
                event.setCancelled(true);
                return;
            }

            Player player = event.getPlayer();
            NDPlayer ndPlayer = DataManager.players.get(player.getName());
            if (ndPlayer == null || !ndPlayer.hasFaction()) {
                player.sendMessage(formatMessage("Mensagens.SemFaccao"));
                event.setCancelled(true);
                return;
            }

            // Check if player is in admin bypass mode
            if (AdminCommands.adminBypassPlayers.contains(player.getName())) {
                return; // Allow breaking spawners
            }

            Terra terra = new Terra(event.getBlock().getWorld(), event.getBlock().getChunk().getX(), event.getBlock().getChunk().getZ());
            NDFaction faction = terra.getFaction();
            NDFaction playerFaction = ndPlayer.getFaction();
            Protecao protection = Utils.getProtection(event.getBlock().getChunk(), player);
            Relacao relacao = FactionPermissionListener.getRelacao(playerFaction, faction);
            Cargo cargo = ndPlayer.getCargo();

            // Verificar se o jogador tem permissão para quebrar blocos (spawners)
            boolean hasPermission = false;
            if (protection == Protecao.Sua && cargo != Cargo.Lider) {
                hasPermission = playerFaction.hasPermissao(cargo, "quebrar_bloco");
            } else if (protection == Protecao.Aliada || protection == Protecao.Neutra || protection == Protecao.Inimiga) {
                hasPermission = faction != null && faction.hasPermissaoRelacao(relacao, "quebrar_bloco");
            } else if (protection == Protecao.Sua && cargo == Cargo.Lider) {
                hasPermission = true; // Líder sempre tem permissão no próprio terreno
            }else if (protection == Protecao.Livre) {
                hasPermission = true; // Free zone allows breaking
            }

            if (!hasPermission) {
                event.setCancelled(true);
                return;
            }

            // Verificar se a facção permite quebrar spawners (1 hora após a última colocação)
            if (faction != null && protection == Protecao.Sua && !faction.canBreakSpawners()) {
                long remainingTime = faction.getRemainingBreakDelay() / 1000; // Em segundos
                long minutes = remainingTime / 60;
                long seconds = remainingTime % 60;
                String message = Config.get("Mensagens.SpawnerBreakDelay") != null
                        ? Config.get("Mensagens.SpawnerBreakDelay").toString().replace("&", "§")
                                .replace("<minutos>", String.valueOf(minutes))
                                .replace("<segundos>", String.valueOf(seconds))
                        : String.format("§cVocê não pode quebrar spawners agora. Aguarde %d minutos e %d segundos.", minutes, seconds);
                player.sendMessage(message);
                event.setCancelled(true);
                return;
            }

            boolean success = faction != null && faction.removePlacedGenerator(type, 1, player.getName());
            if (!success && protection == Protecao.Sua) {
                player.sendMessage("§cNão foi possível remover o spawner. Verifique se há spawners suficientes registrados.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onArrow(EntityDamageByEntityEvent e) {
        if (e.getEntityType() == EntityType.ENDER_CRYSTAL) return;

        if (e.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Player) {
                Player shooter = (Player) projectile.getShooter();
                if (e.getEntity() instanceof Player) {
                    Player target = (Player) e.getEntity();
                    if (DataManager.players.get(shooter.getName()).hasFaction() && DataManager.players.get(target.getName()).hasFaction()) {
                        if (DataManager.players.get(shooter.getName()).getFaction() == DataManager.players.get(target.getName()).getFaction() ||
                            DataManager.players.get(shooter.getName()).getFaction().getAliados().contains(DataManager.players.get(target.getName()).getFaction())) {
                            e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!MenusListeners.hash.containsKey(e.getPlayer())) return;

        int step = MenusListeners.hash.get(e.getPlayer());
        e.setCancelled(true);

        switch (step) {
            case 0:
                handleFactionNameInput(e);
                break;
            case 1:
                handleFactionTagInput(e);
                break;
            case 2:
                handleFactionConfirmation(e);
                break;
        }
    }

    private void handleFactionNameInput(AsyncPlayerChatEvent e) {
        String message = e.getMessage();
        if (message.equalsIgnoreCase("cancelar")) {
            MenusListeners.hash.remove(e.getPlayer());
            return;
        }

        if (message.length() < 5 || message.length() > 16) {
            e.getPlayer().sendMessage(Config.get("Mensagens.NomeGrande").toString().replace("&", "§"));
            return;
        }

        if (Utils.containsSpecialCharacter(message)) {
            e.getPlayer().sendMessage("§cO nome da facção não pode conter caracteres especiais");
            return;
        }

        e.getPlayer().sendMessage("");
        e.getPlayer().sendMessage("§aQual será a tag de sua facção?");
        e.getPlayer().sendMessage("§7Caso queira cancelar, responda 'cancelar'.");
        e.getPlayer().sendMessage("");
        MenusListeners.nome.put(e.getPlayer(), message);
        MenusListeners.hash.put(e.getPlayer(), 1);
    }

    private void handleFactionTagInput(AsyncPlayerChatEvent e) {
        String message = e.getMessage();
        Player player = e.getPlayer();

        if (message.equalsIgnoreCase("cancelar")) {
            MenusListeners.hash.remove(player);
            MenusListeners.nome.remove(player);
            MenusListeners.tag.remove(player);
            player.sendMessage("§cCriação da facção cancelada.");
            return;
        }

        if (message.length() != 3) {
            player.sendMessage(Config.get("Mensagens.TagGrande").toString().replace("&", "§"));
            return;
        }

        if (Utils.containsSpecialCharacter(message)) {
            player.sendMessage("§cA tag da facção não pode conter caracteres especiais");
            return;
        }

        String factionTag = message.toUpperCase();
        String factionName = MenusListeners.nome.get(player);
        MenusListeners.tag.put(player, factionTag);
        MenusListeners.hash.put(player, 2);

        TextComponent messageComponent = new TextComponent("§aVocê confirma a criação da facção §f[" + factionTag + "] " + factionName + "?\n ");

        TextComponent confirmButton = new TextComponent("§a[Confirmar]");
        confirmButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "confirmar"));
        confirmButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClique para preencher 'confirmar' no chat").create()));

        TextComponent cancelButton = new TextComponent(" §c[Cancelar]");
        cancelButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "cancelar"));
        cancelButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cClique para preencher 'cancelar' no chat").create()));

        messageComponent.addExtra(confirmButton);
        messageComponent.addExtra(cancelButton);

        player.spigot().sendMessage(messageComponent);

        e.setCancelled(true);
    }

    private void handleFactionConfirmation(AsyncPlayerChatEvent e) {
        e.setCancelled(true);

        Player player = e.getPlayer();
        String msg = e.getMessage();

        if (msg.equalsIgnoreCase("cancelar")) {
            MenusListeners.hash.remove(player);
            player.sendMessage("§cVocê cancelou a criação de facção");
            return;
        }

        if (msg.equalsIgnoreCase("confirmar")) {
            MenusListeners.hash.remove(player);
            Comandos.create(DataManager.players.get(player.getName()),
                    MenusListeners.nome.get(player), MenusListeners.tag.get(player));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            Player target = (Player) e.getEntity();
            Player attacker = (Player) e.getDamager();

            NDPlayer attackerProfile = DataManager.players.get(attacker.getName());
            NDPlayer targetProfile = DataManager.players.get(target.getName());

            if (attackerProfile != null && targetProfile != null &&
                attackerProfile.hasFaction() && targetProfile.hasFaction()) {

                if (attackerProfile.getFaction() == targetProfile.getFaction() ||
                    attackerProfile.getFaction().getAliados().contains(targetProfile.getFaction())) {
                    e.setCancelled(true);
                }
            }
        }
    }


    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player deceased = e.getEntity();
        Player killer = deceased.getKiller();

        // Check if deceased player data exists
        NDPlayer deceasedNDPlayer = DataManager.players.get(deceased.getName());
        if (deceasedNDPlayer == null) {
            // Handle missing player data (e.g., log error or create new NDPlayer)
            return;
        }

        if (killer != null) {
            NDPlayer killerNDPlayer = DataManager.players.get(killer.getName());
            if (killerNDPlayer == null) {
                // Handle missing killer data
                return;
            }
            killerNDPlayer.setKills(killerNDPlayer.getKills() + 1);
            deceasedNDPlayer.setMortes(deceasedNDPlayer.getMortes() + 1);

            // Faction message logic
            String killerFactionTag = killerNDPlayer.hasFaction() ? "[" + killerNDPlayer.getFaction().getTag() + "]" : "";
            String deceasedFactionTag = deceasedNDPlayer.hasFaction() ? "[" + deceasedNDPlayer.getFaction().getTag() + "]" : "";
            String message = Config.get("Mensagens.JogadorMorto") != null
                    ? Config.get("Mensagens.JogadorMorto").toString().replace("&", "§")
                        .replace("<assassino>", killer.getName())
                        .replace("<tag_assassino>", killerFactionTag)
                        .replace("<vitima>", deceased.getName())
                        .replace("<tag_vitima>", deceasedFactionTag)
                    : "§e" + killerFactionTag + " " + killer.getName() + " §7matou §e" + deceasedFactionTag + " " + deceased.getName() + "§7!";

            double radiusSquared = 15.0 * 15.0;
            for (Player nearbyPlayer : deceased.getWorld().getPlayers()) {
                if (nearbyPlayer.equals(killer) || nearbyPlayer.equals(deceased)) {
                    continue;
                }
                if (nearbyPlayer.getLocation().distanceSquared(deceased.getLocation()) <= radiusSquared) {
                    nearbyPlayer.sendMessage(message);
                }
            }
        }

        // Power loss logic
        Object powerLossObj = Config.get("Geral.PerderPoderPorMorte");
        int powerLoss = (powerLossObj instanceof Integer) ? (int) powerLossObj : 0; // Default to 0 if null or invalid
        deceasedNDPlayer.setPoder(Math.max(0, deceasedNDPlayer.getPoder() - powerLoss));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Creeper)) return;

        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        NDPlayer killerNDPlayer = DataManager.players.get(killer.getName());
        String killerFactionTag = killerNDPlayer.hasFaction() ? "[" + killerNDPlayer.getFaction().getTag() + "]" : "";
        String message = Config.get("Mensagens.CreeperMorto") != null
                ? Config.get("Mensagens.CreeperMorto").toString().replace("&", "§")
                    .replace("<jogador>", killer.getName())
                    .replace("<tag>", killerFactionTag)
                : "§e" + killerFactionTag + " " + killer.getName() + " §7matou um Creeper!";

        double radiusSquared = 15.0 * 15.0;
        for (Player nearbyPlayer : entity.getWorld().getPlayers()) {
            if (nearbyPlayer.equals(killer)) {
                continue;
            }
            if (nearbyPlayer.getLocation().distanceSquared(entity.getLocation()) <= radiusSquared) {
                nearbyPlayer.sendMessage(message);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo().getChunk() != e.getFrom().getChunk() && DataManager.players.containsKey(e.getPlayer().getName())) {
            if (DataManager.players.get(e.getPlayer().getName()).isMapaLigado()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Utils.updateMap(e.getPlayer(), e.getPlayer().getLocation());
                    }
                }.runTaskLater(Main.getPlugin(Main.class), 5L);
            }
        }

        updateActionBar(e);
        handleFlightRestrictions(e);
    }

    private void updateActionBar(PlayerMoveEvent e) {
        String fac = getFactionMessage(e.getPlayer());
        if (antiflood.get(e.getPlayer().getName()) == null || !antiflood.get(e.getPlayer().getName()).equalsIgnoreCase(fac)) {
            antiflood.put(e.getPlayer().getName(), fac);
            ActionBar.sendActionBarMessage(e.getPlayer(), fac);
        }
    }

    private String getFactionMessage(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
        switch (Utils.getProtection(player.getLocation().getChunk(), player)) {
            case Aliada: return "§a" + "[" + terra.getFaction().getTag() + "] " + terra.getFaction().getNome();
            case Guerra: return "§4Zona de Guerra";
            case Inimiga: return "§c" + "[" + terra.getFaction().getTag() + "] " + terra.getFaction().getNome();
            case Livre: return "§2Zona Livre";
            case Neutra: return "§7" + "[" + terra.getFaction().getTag() + "] " + terra.getFaction().getNome();
            case Protegida: return "§6Zona Protegida";
            case Sua: return "§f" + "[" + terra.getFaction().getTag() + "] " + terra.getFaction().getNome();
            default: return "";
        }
    }

    private void handleFlightRestrictions(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        Location from = e.getFrom();
        Location to = e.getTo();

        // Ignora se não mudou de bloco
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        // Verifica se mudou de território
        ScoreUtils utils = new ScoreUtils(player);
        boolean zoneChanged = !utils.getLocationNameFaction(from).equalsIgnoreCase(utils.getLocationNameFaction(to));
        if (!zoneChanged) return;

        // Permissões
        boolean hasFlyAllyOwnPerm = player.hasPermission("nd.commands.factions.fly.allyown");
        boolean hasFlyProtectedPerm = player.hasPermission("nd.commands.factions.fly.protected");
        boolean hasFlyAllPerm = player.hasPermission("nd.commands.factions.fly.all");
        boolean hasFlyPerm = player.hasPermission("nd.commands.factions.fly");
        
        if (OlhoDeDeusListener.olhos.contains(player.getName())) {
        	return;
        }
        if (!hasFlyAllyOwnPerm && !hasFlyProtectedPerm && !hasFlyAllPerm && !hasFlyPerm) {
            if (player.isFlying()) player.setFlying(false);
            if (player.getAllowFlight()) player.setAllowFlight(false);
            return;
        }

        Terra currentTerritory = new Terra(player.getWorld(), to.getChunk().getX(), to.getChunk().getZ());
        Protecao protection = Utils.getProtection(to.getChunk(), player);

        boolean shouldEnableFly = false;

        if (hasFlyAllPerm) {
            shouldEnableFly = true;
        } else if (hasFlyAllyOwnPerm && (protection == Protecao.Sua || protection == Protecao.Aliada)) {
            NDFaction faction = currentTerritory.getFaction();
            shouldEnableFly = faction == null || !faction.isSobAtaque();
        } else if (hasFlyProtectedPerm && protection == Protecao.Protegida) {
            shouldEnableFly = true;
        } else if (hasFlyPerm && (protection == Protecao.Sua || protection == Protecao.Aliada || protection == Protecao.Protegida)) {
            if (protection == Protecao.Sua || protection == Protecao.Aliada) {
                NDFaction faction = currentTerritory.getFaction();
                shouldEnableFly = faction == null || !faction.isSobAtaque();
            } else {
                shouldEnableFly = true;
            }
        }

        if (shouldEnableFly) {
            if (!player.getAllowFlight()) player.setAllowFlight(true);
        } else {
            if (!hasFlyAllPerm) {
                if (player.isFlying()) player.setFlying(false);
                if (player.getAllowFlight()) player.setAllowFlight(false);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().hasPermission("nd.commands.factions.admin") || !e.hasBlock()) return;
        if ((e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK) && BLOCKS.contains(e.getClickedBlock().getType())) {
            handleBlockInteraction(e);
        }
    }

    @EventHandler
    public void onInteractWithItem(PlayerInteractEvent e) {
        if (e.getPlayer().hasPermission("nd.commands.factions.admin") || !e.hasItem()) return;
        if (BLOCKS.contains(e.getItem().getType())) {
            handleBlockInteraction(e);
        }
    }
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.getPlayer().hasPermission("nd.commands.factions.admin")) {
            return;
        }
        this.handleBlockModification(e.getPlayer(), e.getBlock().getLocation().getChunk(), (Event)e);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("nd.commands.factions.admin")) {
            return;
        }
        this.handleBlockModification(e.getPlayer(), e.getBlock().getLocation().getChunk(), (Event)e);
    }

    @SuppressWarnings("incomplete-switch")
	private void handleBlockModification(Player player, Chunk chunk, Event e) {
        Protecao protection = Utils.getProtection(chunk, player);
        boolean shouldCancel = false;
        switch (protection) {
            case Protegida: 
            case Guerra: 
            case Inimiga: 
            case Aliada: 
            case Neutra: {
                shouldCancel = true;
                break;
            }
            case Sua:
        }
        if (!shouldCancel) {
            return;
        }
        if (e instanceof BlockBreakEvent) {
            ((BlockBreakEvent)e).setCancelled(true);
        } else if (e instanceof BlockPlaceEvent) {
            ((BlockPlaceEvent)e).setCancelled(true);
        }
    }

    private void handleBlockInteraction(PlayerInteractEvent e) {
        Protecao protection = Utils.getProtection(e.getPlayer().getLocation().getChunk(), e.getPlayer());

        switch (protection) {
            case Aliada:
            case Guerra:
            case Inimiga:
            case Neutra:
            case Protegida:
                e.setCancelled(true);
                break;
            case Sua:
                if (DataManager.players.get(e.getPlayer().getName()).getCargo() == Cargo.Recruta) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(Config.get("Mensagens.SemCargo").toString().replace("&", "§").replace("<cargo>", "recruta"));
                }
                break;
            case Livre:
                break;
        }
    }
}