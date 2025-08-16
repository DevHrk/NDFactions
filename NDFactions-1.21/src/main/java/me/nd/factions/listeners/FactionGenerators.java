package me.nd.factions.listeners;

import java.text.SimpleDateFormat;
import java.util.*;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import me.nd.factions.Main;
import me.nd.factions.api.Config;
import me.nd.factions.comandos.Comandos;
import me.nd.factions.enums.Cargo;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class FactionGenerators implements Listener {

    public static final Map<EntityType, String> GENERATOR_NAMES;
    public static Map<String, EntityType> awaitingSpawnerWithdraw = new HashMap<>();
    public static Map<String, EntityType> awaitingSpawnerStore = new HashMap<>();

    static {
        GENERATOR_NAMES = new HashMap<>();
        GENERATOR_NAMES.put(EntityType.PIG, "Porco");
        GENERATOR_NAMES.put(EntityType.SPIDER, "Aranha");
        GENERATOR_NAMES.put(EntityType.ZOMBIE, "Zumbi");
        GENERATOR_NAMES.put(EntityType.SKELETON, "Esqueleto");
        GENERATOR_NAMES.put(EntityType.CREEPER, "Creeper");
        GENERATOR_NAMES.put(EntityType.BLAZE, "Blaze");
        GENERATOR_NAMES.put(EntityType.CAVE_SPIDER, "Aranha da Caverna");
        GENERATOR_NAMES.put(EntityType.COW, "Vaca");
        GENERATOR_NAMES.put(EntityType.CHICKEN, "Galinha");
        GENERATOR_NAMES.put(EntityType.SHEEP, "Ovelha");
        GENERATOR_NAMES.put(EntityType.WITHER, "Wither");
        GENERATOR_NAMES.put(EntityType.WITCH, "Bruxa");
        GENERATOR_NAMES.put(EntityType.ENDER_DRAGON, "Dragão do Fim");
        GENERATOR_NAMES.put(EntityType.ENDERMAN, "Enderman");
        GENERATOR_NAMES.put(EntityType.WOLF, "Lobo");
        GENERATOR_NAMES.put(EntityType.IRON_GOLEM, "Golem de Ferro");
        GENERATOR_NAMES.put(EntityType.RABBIT, "Coelho");
        GENERATOR_NAMES.put(EntityType.OCELOT, "Jaguatirica");
        GENERATOR_NAMES.put(EntityType.ENDERMITE, "Endermite");
        GENERATOR_NAMES.put(EntityType.SNOW_GOLEM, "Golem de Neve");
        GENERATOR_NAMES.put(EntityType.MOOSHROOM, "Vaca de Cogumelo");
        GENERATOR_NAMES.put(EntityType.VILLAGER, "Aldeão");
        GENERATOR_NAMES.put(EntityType.PIGLIN, "Porco Zumbi");
        GENERATOR_NAMES.put(EntityType.SLIME, "Slime");
        GENERATOR_NAMES.put(EntityType.GUARDIAN, "Guardião");
        GENERATOR_NAMES.put(EntityType.SILVERFISH, "Silverfish");
        GENERATOR_NAMES.put(EntityType.MAGMA_CUBE, "Cubo de Magma");
        GENERATOR_NAMES.put(EntityType.BAT, "Morcego");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory viewInventory = event.getView().getTopInventory();
        if (viewInventory == null || !event.getView().getTitle().startsWith("Geradores - [")) return;

        event.setCancelled(true);

        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) {
            player.sendMessage(color(Config.get("Mensagens.SemFac").toString()));
            player.closeInventory();
            return;
        }

        if (!hasRequiredCargo(ndPlayer)) {
            String msg = Config.get("Mensagens.SemCargo").toString()
                    .replace("<cargo>", ndPlayer.getCargo().toString().toLowerCase());
            player.sendMessage(color(msg));
            player.closeInventory();
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();
        NDFaction faction = ndPlayer.getFaction();

        switch (slot) {
            case 10:
                if (clickedItem.getType() == Material.ARROW) {
                    player.closeInventory();
                    player.chat("/f");
                }
                break;
            case 12:
                if (clickedItem.getType() == Material.CHEST) {
                    switch (event.getClick()) {
                        case SHIFT_LEFT:
                            player.closeInventory();
                            player.sendMessage("");
                            player.sendMessage("§eClique com o botão direito no spawner que deseja armazenar.");
                            player.sendMessage("§7(O item deve estar no seu inventário)");
                            player.sendMessage("");
                            Bukkit.getScheduler().runTaskLater(Main.get(), () -> {
                                awaitingSpawnerStore.put(player.getName(), null); // Pegamos o tipo depois
                            }, 2L);
                            return;

                        case LEFT: {
                            // Armazenar todos os spawners
                            int totalStored = 0;
                            for (ItemStack item : player.getInventory().getContents()) {
                                if (item != null && item.getType() == Material.SPAWNER) {
                                    EntityType type = getSpawnerType(item);
                                    if (type != null && GENERATOR_NAMES.containsKey(type)) {
                                        int amount = item.getAmount();
                                        faction.addStoredGenerator(type, amount, player.getName());
                                        totalStored += amount;
                                        player.getInventory().removeItem(item);
                                    }
                                }
                            }

                            if (totalStored > 0) {
                                player.sendMessage("§aVocê armazenou §f" + totalStored + "§a geradores.");
                            } else {
                                player.sendMessage("§cVocê não possui geradores compatíveis no inventário.");
                            }
                            Comandos.openGeneratorsMenu(player, faction);
                            return;
                        }

                        case RIGHT: {
                            // Armazenar 1 spawner qualquer
                            Inventory inventory = player.getInventory();
                            for (int i = 0; i < inventory.getSize(); i++) {
                                ItemStack item = inventory.getItem(i);
                                if (item != null && item.getType() == Material.SPAWNER) {
                                    EntityType type = getSpawnerType(item);
                                    if (type != null && GENERATOR_NAMES.containsKey(type) && item.getAmount() >= 1) {
                                        faction.addStoredGenerator(type, 1, player.getName());
                                        item.setAmount(item.getAmount() - 1);
                                        if (item.getAmount() == 0) {
                                            inventory.setItem(i, null); // Remove o item do slot
                                        }
                                        player.updateInventory(); // Garante atualização do inventário

                                        player.sendMessage("§aVocê armazenou §f1§a gerador de §f" + GENERATOR_NAMES.get(type) + "§a.");
                                        Comandos.openGeneratorsMenu(player, faction);
                                        return;
                                    }
                                }
                            }

                            player.sendMessage("§cVocê não possui geradores compatíveis no inventário.");
                            return;
                        }

                        default:
                            storeSpawnersFromInventory(player, faction);
                            Comandos.openGeneratorsMenu(player, faction);
                            return;
                    }
                }
                break;

            case 14:
                if (clickedItem.getType() == Material.BOOK) {
                    showGeneratorLogs(player, faction);
                }
                break;
            case 13:
                if (clickedItem.getType() == Material.MAP) {
                    // Obter o chunk atual do jogador
                    Chunk chunk = player.getLocation().getChunk();
                    Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());

                    // Verificar se o terreno pertence à facção
                    if (!faction.ownsTerritory(terra)) {
                        player.sendMessage("§cEsta terra não pertence à sua facção.");
                        return;
                    }

                    // Verificar se a facção está sob ataque
                    if (faction.isSobAtaque()) {
                        player.sendMessage("§cNão é possível armazenar geradores enquanto a facção está sob ataque.");
                        return;
                    }
                    
                    // Verifica se a facção existe e se está bloqueada para quebrar spawners
                    if (faction != null && !faction.canBreakSpawners()) {
                        player.sendMessage("§cNão pode armazenar enquanto está bloqueado quebrar o spawner");
                        return; 
                    }

                    boolean stored;
                    // Diferenciar entre clique esquerdo e direito
                    if (event.getClick() == ClickType.LEFT) {
                        // Botão Esquerdo: Armazena todos os geradores
                        stored = faction.storeGeneratorsFromTerritory(terra, player.getName());
                        if (stored) {
                            player.sendMessage("§aTodos os geradores foram armazenados com sucesso!");
                        } else {
                            player.sendMessage("§cNenhum gerador encontrado nesta terra.");
                        }
                    } else if (event.getClick() == ClickType.RIGHT) {

                        // Botão Direito: Armazena um gerador
                        stored = faction.storeSingleGeneratorFromTerritory(terra, player.getName());
                        if (stored) {
                            player.sendMessage("§aUm gerador foi armazenado com sucesso!");
                        } else {
                            player.sendMessage("§cNenhum gerador encontrado nesta terra.");
                        }
                    }
                }
                break;
            default:
                // Interação com slots de spawner (cabeças)
            	if (clickedItem.getType() == Material.PLAYER_HEAD) {
                    EntityType type = getSpawnerTypes(clickedItem);
                    if (type == null || !GENERATOR_NAMES.containsKey(type)) {
                        player.sendMessage("§cErro: Tipo de gerador inválido.");
                        return;
                    }

                    int storedAmount = faction.getStoredGeneratorAmount(type);
                    if (storedAmount <= 0) {
                        player.sendMessage("§cVocê não possui geradores de §f" + GENERATOR_NAMES.get(type) + "§c armazenados.");
                        return;
                    }

                    int toWithdraw = 0;
                    switch (event.getClick()) {
                        case RIGHT:
                            toWithdraw = 1;
                            break;
                        case LEFT:
                            toWithdraw = storedAmount;
                            break;
                        case SHIFT_RIGHT:
                            player.closeInventory();
                            player.sendMessage("");
                            player.sendMessage("§eInforme quantos geradores de §f" + GENERATOR_NAMES.get(type) + "§e deseja retirar §7(1 - " + storedAmount + "):");

                            TextComponent cancelarMsg = new TextComponent("§7Digite ");
                            TextComponent cancelarClique = new TextComponent("§c[cancelar]");
                            cancelarClique.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "cancelar"));
                            cancelarClique.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder("§7Clique aqui para cancelar a operação.").create()));
                            cancelarMsg.addExtra(cancelarClique);
                            cancelarMsg.addExtra(" §7para cancelar.");

                            player.spigot().sendMessage(cancelarMsg);
                            player.sendMessage("");

                            Bukkit.getScheduler().runTask(Main.get(), () -> {
                                awaitingSpawnerWithdraw.put(player.getName(), type);
                            });
                            return;
                        default:
                            return;
                    }

                    // Verificar espaço no inventário
                    int canWithdraw = hasInventorySpace(player, toWithdraw);
                    if (canWithdraw == 0) {
                        player.sendMessage("§cSeu inventário está cheio. Libere espaço para retirar os geradores.");
                        return;
                    }

                    // Remover geradores da facção
                    faction.removeStoredGenerator(type, canWithdraw, player.getName());

                    // Executar o comando sgive com o formato correto
                    String command = "sgive " + player.getName() + " " + type.name().toLowerCase() + " " + canWithdraw;
                    boolean commandSuccess = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

                    if (!commandSuccess) {
                        // Reverter a remoção se o comando falhar
                        faction.addStoredGenerator(type, canWithdraw, player.getName());
                        player.sendMessage("§cErro ao executar o comando de retirada. Tente novamente.");
                        return;
                    }

                    // Mensagem de sucesso
                    player.sendMessage("§aVocê retirou §f" + canWithdraw + "§a gerador(es) de §f" + GENERATOR_NAMES.get(type) + "§a.");
                    if (canWithdraw < toWithdraw) {
                    	player.sendMessage("");
                        player.sendMessage("§eOs §f" + (toWithdraw - canWithdraw) + "§e geradores restantes continuam armazenados.");
                    }

                    // Atualizar o inventário
                    Comandos.openGeneratorsMenu(player, faction);
                }
                break;
        }
    }

    private void storeSpawnersFromInventory(Player player, NDFaction faction) {
        boolean storedAny = false;

        for (EntityType type : GENERATOR_NAMES.keySet()) {
            int amount = countSpawnersInInventory(player, type);
            if (amount > 0) {
                removeSpawnersFromInventory(player, type, amount);
                faction.addStoredGenerator(type, amount, player.getName());
                player.sendMessage("§aArmazenado " + amount + " gerador(es) de " + GENERATOR_NAMES.get(type) + ".");
                storedAny = true;
            }
        }

        if (!storedAny) {
            player.sendMessage("§cNenhum gerador compatível encontrado no inventário.");
            for (ItemStack item : player.getInventory().getContents()) {
                if (isSpawnerItem(item)) {
                    EntityType type = getSpawnerType(item);
                    Bukkit.getLogger().info("- Spawner: Amount=" + item.getAmount() + ", Type=" + (type != null ? type.name() : "Unknown"));
                }
            }
        }
    }
    
    private void handleSpawnerWithdraw(Player player, String message, EntityType type) {
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) {
            player.sendMessage("§cVocê não pertence a uma facção.");
            awaitingSpawnerWithdraw.remove(player.getName());
            return;
        }

        NDFaction faction = ndPlayer.getFaction();
        int storedAmount = faction.getStoredGeneratorAmount(type);

        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage("§cRetirada cancelada.");
            awaitingSpawnerWithdraw.remove(player.getName());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage("§cDigite um número válido ou 'cancelar'.");
            return;
        }

        if (amount <= 0 || amount > storedAmount) {
            player.sendMessage("§cQuantidade inválida. Máximo: §f" + storedAmount);
            return;
        }

        // Verificar espaço no inventário
        int canWithdraw = hasInventorySpace(player, amount);
        if (canWithdraw == 0) {
            player.sendMessage("§cSeu inventário está cheio. Libere espaço para retirar os geradores.");
            return;
        }

        // Se não puder retirar todos, informar ao jogador
        if (canWithdraw < amount) {
            player.sendMessage("§eAviso: Seu inventário só suporta §f" + canWithdraw + "§e de §f" + amount + "§e geradores. Os restantes permanecerão armazenados.");
        }

        // Remove os geradores da facção
        faction.removeStoredGenerator(type, canWithdraw, player.getName());

        // Executar o comando sgive com formato corrigido
        Bukkit.getScheduler().runTask(Main.get(), () -> {
            String command = "sgive " + player.getName() + " " + type.name().toLowerCase() + " " + canWithdraw;
            boolean commandSuccess = false;
            try {
                commandSuccess = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao executar comando: " + command);
                e.printStackTrace();
            }

            if (!commandSuccess) {
                // Reverter a remoção se o comando falhar
                faction.addStoredGenerator(type, canWithdraw, player.getName());
                player.sendMessage("§cErro ao executar o comando de retirada. Tente novamente.");
                return;
            }

            // Mensagem de sucesso
            player.sendMessage("§aVocê retirou §f" + canWithdraw + "§a gerador(es) de §f" + GENERATOR_NAMES.get(type) + "§a.");
            if (canWithdraw < amount) {
                player.sendMessage("§eOs §f" + (amount - canWithdraw) + "§e geradores restantes continuam armazenados.");
            }
            awaitingSpawnerWithdraw.remove(player.getName());
        });
    }


    private void handleSpawnerStore(Player player, String message, EntityType type) {
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) {
            player.sendMessage("§cVocê não pertence a uma facção.");
            awaitingSpawnerStore.remove(player.getName());
            return;
        }

        NDFaction faction = ndPlayer.getFaction();

        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage("§cArmazenamento cancelado.");
            awaitingSpawnerStore.remove(player.getName());
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage("§cDigite um número válido ou 'cancelar'.");
            return;
        }

        if (amount <= 0) {
            player.sendMessage("§cDigite um valor maior que 0.");
            return;
        }

        int available = countSpawnersInInventory(player, type);
        if (amount > available) {
            player.sendMessage("§cVocê não tem §f" + amount + "§c geradores de §f" + GENERATOR_NAMES.get(type) + "§c no inventário. Disponível: §f" + available);
            return;
        }

        removeSpawnersFromInventory(player, type, amount);
        faction.addStoredGenerator(type, amount, player.getName());
        player.sendMessage("§aVocê armazenou §f" + amount + "§a gerador(es) de §f" + GENERATOR_NAMES.get(type) + "§a.");
        awaitingSpawnerStore.remove(player.getName());

    }

    @EventHandler
    public void onPlayerRightClickSpawner(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!awaitingSpawnerStore.containsKey(player.getName())) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SPAWNER) return;

        event.setCancelled(true);
        EntityType type = getSpawnerType(item);
        if (type == null || !GENERATOR_NAMES.containsKey(type)) {
            player.sendMessage("§cSpawner inválido ou não suportado.");
            awaitingSpawnerStore.remove(player.getName());
            return;
        }
        player.sendMessage("");
        player.sendMessage("§eDigite a quantidade de geradores de §f" + GENERATOR_NAMES.get(type) + "§e para armazenar.");

        TextComponent cancelar = new TextComponent("§7Digite ");
        TextComponent clicarCancelar = new TextComponent("§c[cancelar]");
        clicarCancelar.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "cancelar"));
        clicarCancelar.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clique para cancelar.").create()));

        cancelar.addExtra(clicarCancelar);
        cancelar.addExtra(" §7para cancelar.");

        player.spigot().sendMessage(cancelar);
        player.sendMessage("");

        awaitingSpawnerStore.put(player.getName(), type);

    }

    @EventHandler
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Retirar
        if (awaitingSpawnerWithdraw.containsKey(player.getName())) {
            EntityType type = awaitingSpawnerWithdraw.get(player.getName());
            if (type == null) {
                player.sendMessage("§cNenhum tipo de spawner selecionado. Tente novamente.");
                awaitingSpawnerWithdraw.remove(player.getName());
                event.setCancelled(true);
                return;
            }
            if (message.equalsIgnoreCase("cancelar")) {
                event.setCancelled(true); // Impede a mensagem de aparecer no chat
            } else {
                try {
                    Integer.parseInt(message); // Verifica se é um número válido
                    event.setCancelled(true); // Impede o número de aparecer no chat
                } catch (NumberFormatException e) {
                    // Não cancela para mensagens não numéricas (exceto "cancelar")
                }
            }
            handleSpawnerWithdraw(player, message, type);
            return;
        }
        
        // Armazenar
        if (awaitingSpawnerStore.containsKey(player.getName())) {
            EntityType type = awaitingSpawnerStore.get(player.getName());
            if (type == null) {
                player.sendMessage("§cNenhum tipo de spawner selecionado. Clique novamente no spawner.");
                awaitingSpawnerStore.remove(player.getName());
                event.setCancelled(true);
                return;
            }
            if (message.equalsIgnoreCase("cancelar")) {
                event.setCancelled(true); // Impede a mensagem de aparecer no chat
            } else {
                try {
                    Integer.parseInt(message); // Verifica se é um número válido
                    event.setCancelled(true); // Impede o número de aparecer no chat
                } catch (NumberFormatException e) {
                    // Não cancela para mensagens não numéricas (exceto "cancelar")
                }
            }
            handleSpawnerStore(player, message, type);
        }
    }

    private EntityType getSpawnerTypes(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }

        if (item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (!meta.hasOwner()) {
                return null;
            }

            String skullOwner = meta.getOwner();
            if (skullOwner == null) {
                return null;
            }

            switch (skullOwner) {
                case "MHF_Zombie": return EntityType.ZOMBIE;
                case "MHF_Skeleton": return EntityType.SKELETON;
                case "MHF_Spider": return EntityType.SPIDER;
                case "MHF_CaveSpider": return EntityType.CAVE_SPIDER;
                case "MHF_Creeper": return EntityType.CREEPER;
                case "MHF_Enderman": return EntityType.ENDERMAN;
                case "MHF_Blaze": return EntityType.BLAZE;
                case "MHF_Ghast": return EntityType.GHAST;
                case "MHF_LavaSlime": return EntityType.MAGMA_CUBE;
                case "MHF_Slime": return EntityType.SLIME;
                case "MHF_WSkeleton": return EntityType.WITHER_SKELETON;
                case "MHF_PigZombie": return EntityType.PIGLIN;
                case "MHF_Pig": return EntityType.PIG;
                case "MHF_Cow": return EntityType.COW;
                case "MHF_Chicken": return EntityType.CHICKEN;
                case "MHF_Sheep": return EntityType.SHEEP;
                case "MHF_Villager": return EntityType.VILLAGER;
                case "MHF_Golem": return EntityType.IRON_GOLEM;
                default: return null;
            }
        }

        return null;
    }

    // Método para verificar e gerenciar espaço no inventário
    private int hasInventorySpace(Player player, int requestedAmount) {
        // Calcula o espaço disponível considerando o limite de stack (64 para spawners)
        int maxStackSize = 64;
        @SuppressWarnings("unused")
		int availableStacks = 0;
        int totalAvailableItems = 0;
        
        Inventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        
        for (int i = 0; i < 36; i++) { // Apenas slots do inventário principal (0-35)
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                availableStacks++;
                totalAvailableItems += maxStackSize;
            } else if (item.getType() == Material.SPAWNER && getSpawnerType(item) != null) {
                // Verifica se o slot contém spawners compatíveis que podem ser empilhados
                int remainingStackSpace = maxStackSize - item.getAmount();
                if (remainingStackSpace > 0) {
                    totalAvailableItems += remainingStackSpace;
                }
            }
        }
        
        return Math.min(totalAvailableItems, requestedAmount);
    }

    static EntityType getSpawnerType(ItemStack item) {
        if (!isSpawnerItem(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta bsm && bsm.hasBlockState()) {
            BlockState state = bsm.getBlockState();
            if (state instanceof CreatureSpawner cs) {
                EntityType type = cs.getSpawnedType();
                if (type != null && type.isSpawnable() && type.isAlive() && GENERATOR_NAMES.containsKey(type)) {
                    return type;
                }
            }
        }

        // Fallback to display name and lore
        if (meta != null) {
            if (meta.hasDisplayName()) {
                String name = ChatColor.stripColor(meta.getDisplayName()).toLowerCase();
                for (Map.Entry<EntityType, String> entry : GENERATOR_NAMES.entrySet()) {
                    String display = entry.getValue().toLowerCase();
                    if (name.contains(display) || name.contains(entry.getKey().name().toLowerCase())) {
                        return entry.getKey();
                    }
                }
            }

            if (meta.hasLore()) {
                for (String line : meta.getLore()) {
                    String clean = ChatColor.stripColor(line).toLowerCase();
                    for (Map.Entry<EntityType, String> entry : GENERATOR_NAMES.entrySet()) {
                        String display = entry.getValue().toLowerCase();
                        if (clean.contains(display) || clean.contains(entry.getKey().name().toLowerCase())) {
                            return entry.getKey();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSpawnerItem(ItemStack item) {
        return item != null && item.getType() == Material.SPAWNER;
    }

    private int countSpawnersInInventory(Player player, EntityType type) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSpawnerItem(item) && type == getSpawnerType(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeSpawnersFromInventory(Player player, EntityType type, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (isSpawnerItem(item) && getSpawnerType(item) == type) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    player.getInventory().setItem(i, null);
                    remaining -= stackAmount;
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }

        player.updateInventory();
    }

    private void showGeneratorLogs(Player player, NDFaction faction) {
        List<NDFaction.GeneratorLog> logs = faction.getGeneratorLogs();
        if (logs.isEmpty()) {
            player.sendMessage("§cNenhum registro de ações de geradores encontrado.");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        player.sendMessage("§eHistórico de ações de geradores:");

        for (NDFaction.GeneratorLog log : logs) {
            String action = "Armazenou";
            if ("place".equals(log.getAction())) {
                action = "Colocou";
            } else if ("withdraw".equals(log.getAction())) {
                action = "Retirou";
            }

            String mobName = GENERATOR_NAMES.getOrDefault(log.getGeneratorType(), log.getGeneratorType().name());
            String time = sdf.format(new Date(log.getTimestamp()));
            String terrainText = log.getAction().equals("place") ? "em um terreno " : "";

            player.sendMessage(String.format(
                    "§7- %s §f%s §7%s§f%d §7gerador(es) de §f%s §7em §f%s",
                    log.getPlayerName(), action, terrainText, log.getAmount(), mobName, time
            ));
        }
    }

    private boolean hasRequiredCargo(NDPlayer player) {
        return player.getCargo() == Cargo.Lider || player.getCargo() == Cargo.Capitão;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
    private ItemStack createSpawnerItem(EntityType type, int amount) {
        ItemStack spawner = new ItemStack(Material.MOB_SPAWNER, amount);
        ItemMeta meta = spawner.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eSpawner de " + GENERATOR_NAMES.get(type));
            // Set BlockStateMeta to ensure spawner type
            if (meta instanceof BlockStateMeta) {
                BlockStateMeta bsm = (BlockStateMeta) meta;
                CreatureSpawner creatureSpawner = (CreatureSpawner) bsm.getBlockState();
                creatureSpawner.setSpawnedType(type);
                bsm.setBlockState(creatureSpawner);
            }
            spawner.setItemMeta(meta);
        }
        return spawner;
    }
 */
}