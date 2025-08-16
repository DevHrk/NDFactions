package me.nd.factions.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import me.nd.factions.Main;
import me.nd.factions.api.Config;
import me.nd.factions.api.Heads;
import me.nd.factions.enums.Cargo;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.ItemBuilder;

public class FactionSetSpawn implements Listener {

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER || event.isCancelled()) {
            return;
        }

        Location spawnLocation = event.getLocation();
        Terra terra = new Terra(spawnLocation.getWorld(), spawnLocation.getChunk().getX(), spawnLocation.getChunk().getZ());
        NDFaction faction = terra.getFaction();

        if (faction == null) {
            return;
        }

        Location spawnerLoc = findSpawnerLocation(spawnLocation);
        if (spawnerLoc == null) {
            return;
        }

        BlockState state = spawnerLoc.getBlock().getState();
        if (!(state instanceof CreatureSpawner)) {
            return;
        }

        CreatureSpawner spawner = (CreatureSpawner) state;
        EntityType mobType = spawner.getSpawnedType();
        if (mobType == null) {
            return;
        }

        Location customSpawnPoint = faction.getSpawnPoint(mobType);
        if (customSpawnPoint == null) {
            // No custom spawn point defined; allow default spawning
            return;
        }

        // Ensure the custom spawn point is in a faction-owned chunk
        Terra spawnTerra = new Terra(customSpawnPoint.getWorld(), customSpawnPoint.getChunk().getX(), customSpawnPoint.getChunk().getZ());
        if (!faction.ownsTerritory(spawnTerra)) {
            return;
        }

        // Cancel the original spawn
        event.setCancelled(true);

        // Ensure the chunk is loaded
        Chunk spawnChunk = customSpawnPoint.getChunk();
        if (!spawnChunk.isLoaded()) {
            spawnChunk.load();
        }

        // Look for existing entity of the same type to stack
        Entity existing = null;
        int existingCount = 1;

        for (Entity nearby : customSpawnPoint.getWorld().getNearbyEntities(customSpawnPoint, 3, 3, 3)) {
            if (nearby.getType() == mobType && nearby.hasMetadata("NDFactionSpawned")) {
                existing = nearby;
                if (nearby.hasMetadata("StackAmount")) {
                    existingCount = nearby.getMetadata("StackAmount").get(0).asInt();
                }
                break;
            }
        }

        if (existing != null) {
            // Add to existing stack
            int newCount = existingCount + 1;
            existing.setMetadata("StackAmount", new FixedMetadataValue(Main.get(), newCount));
            existing.setCustomName("§e" + newCount + "x " + formatMobName(mobType));
            existing.setCustomNameVisible(true);
        } else {
            // Create new stacked entity
            Entity entity = customSpawnPoint.getWorld().spawnEntity(customSpawnPoint, mobType);
            entity.setMetadata("NDFactionSpawned", new FixedMetadataValue(Main.get(), true));
            entity.setMetadata("StackAmount", new FixedMetadataValue(Main.get(), 1));
            entity.setCustomName("§e1x " + formatMobName(mobType));
            entity.setCustomNameVisible(true);
            disableAI(entity);
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        if (!entity.hasMetadata("NDFactionSpawned") || !entity.hasMetadata("StackAmount")) return;

        int count = entity.getMetadata("StackAmount").get(0).asInt();
        Player killer = event.getEntity().getKiller();
        boolean killAll = killer != null && killer.isSneaking();

        if (count <= 1 || !killAll) {
            // Normal stack: decrease stack
            if (count > 1) {
                EntityType type = entity.getType();
                Location loc = entity.getLocation();
                Entity newEntity = loc.getWorld().spawnEntity(loc, type);
                newEntity.setMetadata("NDFactionSpawned", new FixedMetadataValue(Main.get(), true));
                newEntity.setMetadata("StackAmount", new FixedMetadataValue(Main.get(), count - 1));
                newEntity.setCustomName("§e" + (count - 1) + "x " + formatMobName(type));
                newEntity.setCustomNameVisible(true);
                disableAI(newEntity);
            }
            return;
        }

        // Kill all: multiply drops
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear(); // Clear original drops

        for (ItemStack drop : drops) {
            ItemStack multiplied = drop.clone();
            multiplied.setAmount(Math.min(drop.getAmount() * count, drop.getType().getMaxStackSize()));
            entity.getWorld().dropItemNaturally(entity.getLocation(), multiplied);
        }

        // Give proportional XP
        event.setDroppedExp(event.getDroppedExp() * count);
    }

    private void disableAI(Entity entity) {
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            living.setAI(false);
        }
    }

    private String formatMobName(EntityType type) {
        String name = type.name().toLowerCase().replace("_", " ");
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private Location findSpawnerLocation(Location spawnLocation) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    Location checkLoc = spawnLocation.clone().add(x, y, z);
                    if (checkLoc.getBlock().getType() == Material.SPAWNER) {
                        return checkLoc;
                    }
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!event.getView().getTitle().equals("Gerenciar Spawners")) {
            return;
        }

        event.setCancelled(true); // Prevent moving items in the inventory

        Player player = (Player) event.getWhoClicked();
        NDPlayer dplayer = DataManager.players.get(player.getName());
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        NDFaction faction = dplayer.getFaction();
        if (faction == null) {
            player.sendMessage(Config.get("Mensagens.NaoEmFaccao").toString().replace("&", "§"));
            player.closeInventory();
            return;
        }

        Terra terra = new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
        if (!faction.ownsTerritory(terra)) {
            player.sendMessage(Config.get("Mensagens.TerraNaoPertence").toString().replace("&", "§"));
            player.closeInventory();
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        String displayName = meta.getDisplayName();
        String mobTypeName = displayName.replace("§aSpawner de ", "").trim().split(" \\(")[0];

        try {
            EntityType mobType = EntityType.valueOf(mobTypeName);

            if (event.isRightClick()) {
                // Set global spawn point for this mob type
                faction.setSpawnPoint(mobType, player.getLocation());
                player.sendMessage(Config.get("Mensagens.PontoSpawnDefinido").toString()
                        .replace("&", "§")
                        .replace("<mobType>", mobTypeName));
            } else if (event.isLeftClick()) {
                // Remove global spawn point
                if (faction.getSpawnPoint(mobType) != null) {
                    faction.removeSpawnPoint(mobType);
                    player.sendMessage(Config.get("Mensagens.PontoSpawnRemovido").toString()
                            .replace("&", "§")
                            .replace("<mobType>", mobTypeName));
                } else {
                    player.sendMessage(Config.get("Mensagens.NenhumPontoSpawn").toString()
                            .replace("&", "§")
                            .replace("<mobType>", mobTypeName));
                }
            }

            // Update the menu
            player.closeInventory();
            handleSetSpawn(dplayer); // Reopen the updated menu

        } catch (IllegalArgumentException e) {
            player.sendMessage(Config.get("Mensagens.MobTypeInvalido").toString()
                    .replace("&", "§")
                    .replace("<mobType>", mobTypeName));
        }
    }

    private boolean handleSetSpawn(NDPlayer dplayer) {
        if (!validateFaction(dplayer, true) || !hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();
        Terra terra = new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());

        if (!faction.ownsTerritory(terra)) {
            player.sendMessage(Config.get("Mensagens.TerraNaoPertence").toString().replace("&", "§"));
            return false;
        }

        // Get the count of spawners by type across all claims
        Map<EntityType, Integer> spawnerTypes = getSpawnerTypesInAllClaims(faction);
        if (spawnerTypes.isEmpty()) {
            player.sendMessage(Config.get("Mensagens.NenhumSpawner").toString().replace("&", "§"));
            return false;
        }

        Inventory menu = Bukkit.createInventory(player, 45, "Gerenciar Spawners");
        int slot = 10; // Start from slot 10
        List<Integer> forbiddenSlots = Arrays.asList(17, 18, 26, 27, 35); // Forbidden slots

        for (Map.Entry<EntityType, Integer> entry : spawnerTypes.entrySet()) {
            EntityType mobType = entry.getKey();
            int count = entry.getValue();

            // Get the MHF_ head corresponding to the mob type
            String mhfName = Heads.getMHFHeadName(mobType);
            ItemStack item = new ItemBuilder(Material.PLAYER_HEAD, 1, 3)
                    .setSkullOwner(mhfName)
                    .toItemStack();

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§aSpawner de " + mobType.toString() + " (" + count + ")");
            List<String> lore = new ArrayList<>();
            lore.add("  §e▎ §fB. Direito: §7definir ponto de spawn.");
            lore.add("  §e▎ §fB. Esquerdo: §7remover ponto de spawn.");
            lore.add("");
            Location spawnPoint = faction.getSpawnPoint(mobType); // Use global spawn point
            if (spawnPoint != null) {
                lore.add("§aSpawn: §fX: " + spawnPoint.getBlockX() + ", Y: " + spawnPoint.getBlockY() + ", Z: " + spawnPoint.getBlockZ());
            } else {
                lore.add("§cNenhum ponto de spawn definido.");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);

            // Find the next valid slot
            while (slot < 45 && forbiddenSlots.contains(slot)) {
                slot++;
            }

            // Add the item to the current slot, if within inventory size
            if (slot < 45) {
                menu.setItem(slot, item);
                slot++; // Increment slot for the next item
            }
        }

        player.openInventory(menu);
        return true;
    }

    private Map<EntityType, Integer> getSpawnerTypesInAllClaims(NDFaction faction) {
        Map<EntityType, Integer> spawnerTypes = new HashMap<>();

        // Combine permanent and temporary territories
        List<Terra> allTerras = new ArrayList<>();
        allTerras.addAll(faction.getTerras());
        allTerras.addAll(faction.getTemporarios());

        // Iterate over all claims (Terras) of the faction
        for (Terra terra : allTerras) {
            Chunk chunk = terra.getWorld().getChunkAt(terra.getX(), terra.getZ());
            for (BlockState state : chunk.getTileEntities()) {
                if (state instanceof CreatureSpawner) {
                    CreatureSpawner spawner = (CreatureSpawner) state;
                    EntityType mobType = spawner.getSpawnedType();
                    if (mobType != null) {
                        spawnerTypes.merge(mobType, 1, Integer::sum);
                    }
                }
            }
        }

        return spawnerTypes;
    }

    private boolean validateFaction(NDPlayer dplayer, boolean sendMessage) {
        if (dplayer == null || !dplayer.hasFaction()) {
            if (sendMessage && dplayer.getPlayer() != null) {
                dplayer.getPlayer().sendMessage(Config.get("Mensagens.NaoEmFaccao").toString().replace("&", "§"));
            }
            return false;
        }
        return true;
    }

    private boolean hasRequiredCargo(NDPlayer dplayer, Cargo... requiredCargos) {
        if (dplayer == null || dplayer.getFaction() == null) return false;
        Cargo playerCargo = dplayer.getCargo();
        for (Cargo cargo : requiredCargos) {
            if (playerCargo == cargo) return true;
        }
        if (dplayer.getPlayer() != null) {
            dplayer.getPlayer().sendMessage(Config.get("Mensagens.SemPermissao").toString().replace("&", "§"));
        }
        return false;
    }
}