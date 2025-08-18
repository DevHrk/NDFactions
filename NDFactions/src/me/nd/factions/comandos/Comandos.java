package me.nd.factions.comandos;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldguard.protection.flags.StateFlag;

import me.nd.factions.Main;
import me.nd.factions.addons.OfferManager;
import me.nd.factions.addons.SobAtaque;
import me.nd.factions.api.Config;
import me.nd.factions.api.Heads;
import me.nd.factions.api.Vault;
import me.nd.factions.banners.Banners;
import me.nd.factions.enums.Cargo;
import me.nd.factions.enums.Motivo;
import me.nd.factions.enums.Protecao;
import me.nd.factions.eventos.doFactionPlayerChangeFaction;
import me.nd.factions.listeners.FactionGenerators;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDFaction.VaultLog;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.scoreboard.Board;
import me.nd.factions.utils.Helper;
import me.nd.factions.utils.ItemBuilder;
import me.nd.factions.utils.Utils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Comandos implements CommandExecutor {
	
	private enum CommandType {
	    CRIAR, ACEITAR, MAPA, CONVIDAR, VERTERRAS, CONVITES, SAIR, DESFAZER, DOMINAR,
	    EXPULSAR, AJUDA, PROMOVER, REBAIXAR, TRANSFERIR, BASE, MEMBROS, INFO,
	    PERFIL, RELACAO, ABANDONAR, LISTAR, PROTEGER, FACTION_INFO, CLAIM,
	    PERMISSOES, RECUSAR, GERADORES, TOP, SETSPAWN, SETSPAWNPOINT, VOAR,
	    GERADORES_LISTAR, ROSTER, ENTRAR, F, HOME, HOMES, SETHOME,
	    HOME_LISTAR, DELHOME, BAU, ATAQUE, OFERTAS, INV
	}

	private static final EnumMap<CommandType, Integer> REQUIRED_ARGS = new EnumMap<>(CommandType.class);
	static {
	    REQUIRED_ARGS.put(CommandType.CRIAR, 3);
	    REQUIRED_ARGS.put(CommandType.ACEITAR, 3);
	    REQUIRED_ARGS.put(CommandType.CONVIDAR, 2);
	    REQUIRED_ARGS.put(CommandType.EXPULSAR, 2);
	    REQUIRED_ARGS.put(CommandType.PROMOVER, 2);
	    REQUIRED_ARGS.put(CommandType.REBAIXAR, 2);
	    REQUIRED_ARGS.put(CommandType.TRANSFERIR, 2);
	    REQUIRED_ARGS.put(CommandType.MEMBROS, 2);
	    REQUIRED_ARGS.put(CommandType.INFO, 1);
	    REQUIRED_ARGS.put(CommandType.PERFIL, 2);
	    REQUIRED_ARGS.put(CommandType.RELACAO, 1);
	    REQUIRED_ARGS.put(CommandType.LISTAR, 1);
	    REQUIRED_ARGS.put(CommandType.CLAIM, 3);
	    REQUIRED_ARGS.put(CommandType.PERMISSOES, 1);
	    REQUIRED_ARGS.put(CommandType.RECUSAR, 3);
	    REQUIRED_ARGS.put(CommandType.GERADORES, 1);
	    REQUIRED_ARGS.put(CommandType.TOP, 1);
	    REQUIRED_ARGS.put(CommandType.VOAR, 1);
	    REQUIRED_ARGS.put(CommandType.SETSPAWN, 1);
	    REQUIRED_ARGS.put(CommandType.SETSPAWNPOINT, 7);
	    REQUIRED_ARGS.put(CommandType.GERADORES_LISTAR, 1); 
	    REQUIRED_ARGS.put(CommandType.ROSTER, 1);
	    REQUIRED_ARGS.put(CommandType.ENTRAR, 2);
	    REQUIRED_ARGS.put(CommandType.HOME, 0);
	    REQUIRED_ARGS.put(CommandType.HOMES, 0);
        REQUIRED_ARGS.put(CommandType.SETHOME, 2); // /f sethome <name>
        REQUIRED_ARGS.put(CommandType.HOME_LISTAR, 1); // /f home listar
        REQUIRED_ARGS.put(CommandType.DELHOME, 2); // /f delhome <name>
        REQUIRED_ARGS.put(CommandType.BAU, 0);
        REQUIRED_ARGS.put(CommandType.ATAQUE, 0);
        REQUIRED_ARGS.put(CommandType.OFERTAS, 0);
        REQUIRED_ARGS.put(CommandType.INV, 1);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	    if (!(sender instanceof Player) || !cmd.getName().equalsIgnoreCase("f")) {
	        return false;
	    }

	    Player player = (Player) sender;
	    NDPlayer dplayer = DataManager.players.get(player.getName());

	    if (args.length == 0) {
	        return help(dplayer);
	    }

	    return handleCommand(dplayer, args);
	}

	private boolean handleCommand(NDPlayer dplayer, String[] args) {
	    Player player = dplayer.getPlayer();
	    if (args.length == 0) {
	        player.sendMessage("§cComando incompleto.");
	        return false;
	    }

	    String command = args[0].toLowerCase();

	    try {
	    	CommandType type;

            try {
                type = CommandType.valueOf(command.toUpperCase());
            } catch (IllegalArgumentException ex) {
                if (command.length() == 3 && Utils.getFactionByTag(command.toUpperCase()) != null) {
                    type = CommandType.FACTION_INFO;
                } else if (command.equalsIgnoreCase("home") && args.length > 1 && args[1].equalsIgnoreCase("listar")) {
                    type = CommandType.HOME_LISTAR;
                } else {
                    throw ex;
                }
            }

            if (type == CommandType.SETHOME && args.length < 2) {
                player.sendMessage("§cUtilize /f sethome [nome]");
                return false;
            }
            if (type == CommandType.DELHOME && args.length < 2) {
                player.sendMessage("§cUtilize /f delhome [nome]");
                return false;
            }
            
	        int requiredArgs = REQUIRED_ARGS.getOrDefault(type, 1);
	        if (args.length < requiredArgs) {
	            sendInsufficientArgsMessage(player, type);
	            return false;
	        }

	        switch (type) {
        		case INV:
        			return handleInv(dplayer, args);
            	case OFERTAS:
            		return OfferManager.openOfferMenu(player);
	            case CRIAR:
	            	return create(dplayer, Helper.toMessage(args, 2), args[1].toUpperCase());
	            case CLAIM:
	                return handleClaim(player, dplayer, args);
	            case TOP:
	                return handleTop(dplayer, args);
	            case ROSTER:
	                return roster(dplayer, args);
	            case ACEITAR:
	                return aceitar(dplayer, args[2].toUpperCase());
	            case RECUSAR:
	                return recusar(dplayer, args[2].toUpperCase());
	            case MAPA:
	                return handleMapa(dplayer, args.length > 1 ? args[1].toLowerCase() : "");
	            case CONVIDAR:
	                return convidar(dplayer, args[1]);
	            case VERTERRAS:
	                return toggleVerTerras(dplayer);
	            case CONVITES:
	                Utils.openInvitesMenu(player);
	                return false;
	            case SAIR:
	                return sair(dplayer);
	            case DESFAZER:
	                return desfazer(dplayer);
	            case DOMINAR:
	                return dominar(dplayer);
	            case EXPULSAR:
	                return expulsar(dplayer, args[1]);
	            case AJUDA:
	                showHelp(player, dplayer);
	                return false;
	            case PROMOVER:
	                return promover(dplayer, args[1]);
	            case REBAIXAR:
	                return rebaixar(dplayer, args[1]);
	            case TRANSFERIR:
	                return transferir(dplayer, args[1]);
	            case BASE:
	                return base(dplayer);
	            case GERADORES_LISTAR:
	                return handleGeradoresListar(dplayer, args);
	            case MEMBROS:
	                return membros(player, args.length > 1 ? args[1] : dplayer.getFaction().getTag());
	            case F:
	                return info(player, args.length > 1 ? args[1] : (dplayer.hasFaction() ? dplayer.getFaction().getTag() : null));
	            case INFO:
	                return info(player, args.length > 1 ? args[1] : (dplayer.hasFaction() ? dplayer.getFaction().getTag() : null));
	            case PERFIL:
	                return perfil(player, args[1]);
	            case RELACAO:
	                return relacao(dplayer, args);
	            case ABANDONAR:
	                return args.length > 1 && args[1].equalsIgnoreCase("todas") ? abandonarall(dplayer) : abandonar(dplayer);
	            case LISTAR:
	                return listar(player, (args.length > 1 && Utils.isInteger(args[1])) ? Integer.parseInt(args[1]) : 1);
	            case PROTEGER:
	                return proteger(dplayer);
	            case FACTION_INFO:
	                return showFactionInfo(player, dplayer, args[0]);
	            case PERMISSOES:
	                return permissoes(dplayer);
	            case GERADORES:
	                return handleGeradores(dplayer, args);
	            case SETSPAWN:
                    return handleSetSpawn(dplayer);
	            case SETSPAWNPOINT:
	                return setSpawnPoint(dplayer, args);
	            case VOAR:
	                return fly(dplayer, args);
	            case ENTRAR:
	                return entrar(dplayer, args[1].toUpperCase());
                case HOME:
                    if (args.length == 1) {
                        return homeListar(dplayer);
                    } else {
                        return home(dplayer, args[1]);
                    }
                case HOMES:
                    if (args.length == 1) {
                        return homeListar(dplayer);
                    } else {
                        return home(dplayer, args[1]);
                    }
                case SETHOME:
                    return sethome(dplayer, args[1]);
                case HOME_LISTAR:
                    return homeListar(dplayer);
                case DELHOME:
                    return homeDelete(dplayer, args[1]);
                case BAU:
	                return bau(dplayer, args);
                case ATAQUE:
                    return ataque(dplayer, args.length > 1 ? args[1].toUpperCase() : null);
	            default:
	                return false;
	        }

	    } catch (IllegalArgumentException e) {
	        player.sendMessage(Config.get("Mensagens.ComandoInvalido").toString().replace("&", "§"));
	        return false;
	    }
	}
	
    public static boolean create(NDPlayer dplayer, String name, String tag) {
        Player player = dplayer.getPlayer();
        if (dplayer.hasFaction()) {
            player.sendMessage(Config.get("Mensagens.SemPermissao").toString().replace("&", "§").replace("<nome>", name));
            return false;
        }
        if (tag.length() != 3) {
            player.sendMessage(Config.get("Mensagens.TagGrande").toString().replace("&", "§"));
            return false;
        }
        if (name.length() < 5 || name.length() > 16) {
            player.sendMessage(Config.get("Mensagens.NomeGrande").toString().replace("&", "§"));
            return false;
        }
        if (Utils.isTagTaken(tag)) {
            player.sendMessage(Config.get("Mensagens.JaExisteTag").toString().replace("&", "§").replace("<tag>", tag));
            return false;
        }
        if (Utils.isNameTaken(name)) {
            player.sendMessage(Config.get("Mensagens.JaExisteNome").toString().replace("&", "§").replace("<nome>", name));
            return false;
        }
        if (Utils.containsSpecialCharacter(tag) || Utils.containsSpecialCharacter(name)) {
            player.sendMessage("§cA tag/nome da facção não pode conter caracteres especiais");
            return false;
        }

        player.sendMessage(Config.get("Mensagens.FacCriada").toString().replace("&", "§"));
        NDFaction faction = new NDFaction(name, null, 0, new ArrayList<>(), new ArrayList<>(), 
                dplayer.getNome(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), tag, new ArrayList<>());
        
        dplayer.setFaction(faction);
        DataManager.factions.put(name, faction);

        doFactionPlayerChangeFaction event = new doFactionPlayerChangeFaction(dplayer, Motivo.CRIAR);
        Bukkit.getPluginManager().callEvent(event);
        return false;
    }
	
	private boolean ataque(NDPlayer dplayer, String tag) {
	    Player player = dplayer.getPlayer();
	    NDFaction faction;

	    // Determine which faction to display
	    if (tag == null) {
	        faction = dplayer.getFaction();
	        if (faction == null) {
	            player.sendMessage("§cUtilize /f ataque <tag>");
	            return false;
	        }
	    } else {
	        faction = Utils.getFactionByTag(tag);
	        if (faction == null) {
	            player.sendMessage("§cFacção com tag '" + tag + "' não encontrada!");
	            return false;
	        }
	    }

	    // Create inventory (27 slots, 3 rows)
	    Inventory menu = Bukkit.createInventory(null, 27, "Status de Ataque - [" + faction.getTag() + "] " + faction.getNome());

	    // Create TNT item
	    ItemStack headItem = new ItemStack(Material.TNT);
	    ItemMeta headMeta = headItem.getItemMeta();
	    headMeta.setDisplayName("§6[" + faction.getTag() + "] " + faction.getNome());

	    List<String> lore = new ArrayList<>();
	    lore.add(""); // Empty line

	    // Spawner status
	    boolean canBreakSpawners = faction.canBreakSpawners();
	    lore.add(" §eSpawners: " + (canBreakSpawners ? "§cPodem ser quebrados" : "§aProtegido"));

	    // Immunity status
	    boolean hasImmunity = faction.hasImmunity();
	    String immunityTime = hasImmunity ? formatTime(DataManager.getRemainingImmunityTime(faction)) : "-/-";
	    lore.add(" §eImunidade: §7" + immunityTime);

	    lore.add(""); // Empty line

	    // Attack status
	    boolean isUnderAttack = SobAtaque.cooldown.contains(faction);
	    String attackTime;

	    if (isUnderAttack) {
	        long attackStartTime = SobAtaque.getAttackStartTime(faction);
	        if (attackStartTime > 0) {
	            long elapsedTime = System.currentTimeMillis() - attackStartTime;
	            long totalSeconds = elapsedTime / 1000;
	            long hours = totalSeconds / 3600;
	            long minutes = (totalSeconds % 3600) / 60;
	            long seconds = totalSeconds % 60;

	            if (hours > 0) {
	                attackTime = String.format("%dh %02dm %02ds", hours, minutes, seconds);
	            } else if (minutes > 0) {
	                attackTime = String.format("%02dm %02ds", minutes, seconds);
	            } else {
	                attackTime = String.format("%02ds", seconds);
	            }
	        } else {
	            attackTime = "-/-";
	        }
	    } else {
	        attackTime = "-/-";
	    }
	    
	    String attackTimes = isUnderAttack ? Board.getAttackTime(faction) : "-/-";
	    lore.add(" §eTempo de Ataque: §7" + attackTimes);
	    lore.add(" §eTempo em Ataque: §7" + attackTime);


	    lore.add(""); // Empty line

	    // Immunity reactivation time
	    boolean canActivateImmunity = DataManager.canActivateImmunity(faction);
	    String immunityCooldown = "";

	    if (!canActivateImmunity) {
	        long remainingTime;
	        if (hasImmunity) {
	            remainingTime = DataManager.getRemainingImmunityTime(faction);
	            immunityCooldown = " §cTempo de imunidade: §7" + formatTime(remainingTime);
	        } else {
	            remainingTime = DataManager.getImmunityCooldown(faction);
	            immunityCooldown = " §cTempo para imunidade: §7" + formatTime(remainingTime);
	        }
	    }

	    if (!immunityCooldown.isEmpty()) {
	        lore.add(immunityCooldown);
	    }

	    headMeta.setLore(lore);
	    headItem.setItemMeta(headMeta);
	    menu.setItem(13, headItem); // Center slot

	    player.openInventory(menu);
	    return true;
	}


	// Helper method to format time in MM:SS
    private String formatTime(long seconds) {
        if (seconds <= 0) return "Nenhum";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }
	
	   // Existing /f bau command
    private boolean bau(NDPlayer dplayer, String[] args) {
        if (!validateFaction(dplayer, true)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();

        // Check permissions based on role
        if (!hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
            player.sendMessage("§cApenas líderes e capitães podem acessar o baú da facção.");
            return false;
        }

        // Handle subcommands
        if (args.length > 1) {
            String subCommand = args[1].toLowerCase();
            if (subCommand.equals("log")) {
                return bauLog(dplayer);
            } else {
                player.sendMessage("§cSubcomando inválido. Use: /f bau [log]");
                return false;
            }
        }

        // No subcommand: open the faction vault inventory
        Inventory vaultInventory = DataManager.loadVault(faction.getNome());
        player.openInventory(vaultInventory);
        player.sendMessage("§aBaú da facção aberto!");
        return true;
    }

    private boolean bauLog(NDPlayer dplayer) {
        if (!validateFaction(dplayer, true)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();

        // Check permissions based on role
        if (!hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
            player.sendMessage("§cApenas líderes e capitães podem ver os logs do baú.");
            return false;
        }

        List<VaultLog> logs = faction.getVaultLogs();
        if (logs.isEmpty()) {
            player.sendMessage("§eNenhum acesso ao baú registrado para a facção " + faction.getNome() + ".");
            return true;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        player.sendMessage("§6=== Logs do Baú da Facção " + faction.getNome() + " ===");
        for (VaultLog log : logs) {
            String timestamp = sdf.format(new Date(log.getTimestamp()));
            player.sendMessage("§7- §f" + log.getPlayerName() + " §7" + log.getAction() + " em §f" + timestamp);
        }
        return true;
    }

	
    // Updated command: /f home [name]
    private boolean home(NDPlayer dplayer, String homeName) {
        if (!validateFaction(dplayer, true)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();

        // Verify if the home exists in DataManager
        Location home = DataManager.getHome(faction.getNome(), homeName);
        if (home == null) {
            player.sendMessage(Config.get("Mensagens.SemBase").toString()
                .replace("&", "§")
                .replace("base", "home '" + homeName + "'"));
            return false;
        }

        // Verify faction cache consistency
        if (!faction.hasHome(homeName)) {
            faction.addHome(homeName, home); // Sync cache
        }

        Terra terra = new Terra(home.getWorld(), home.getChunk().getX(), home.getChunk().getZ());
        if (!faction.ownsTerritory(terra) && !faction.getTemporarios().contains(terra)) {
            player.sendMessage("§cA home '" + homeName + "' está em um território que não pertence mais à sua facção.");
            return false;
        }

        player.sendMessage(Config.get("Mensagens.TeleportadoParaBase").toString()
            .replace("&", "§")
            .replace("base", "home '" + homeName + "'"));
        player.teleport(home);
        return true;
    }

    // Updated command: /f sethome <name>
    private boolean sethome(NDPlayer dplayer, String homeName) {
        if (!validateFaction(dplayer, true) || !hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();

        // Validate home name
        if (homeName.length() > 16 || homeName.contains(" ")) {
            player.sendMessage("§cO nome da home deve ter até 16 caracteres e não pode conter espaços.");
            return false;
        }

        // Check maximum homes limit (configurable, default 5)
        int maxHomes = (int) Config.get("Padrao.MaximoHomesPorFac");
        Map<String, Location> homes = DataManager.loadHomes(faction.getNome());
        if (homes.size() >= maxHomes && !homes.containsKey(homeName)) {
            player.sendMessage("§cSua facção atingiu o limite de " + maxHomes + " homes.");
            return false;
        }

        // Check if location is in faction territory
        Location location = player.getLocation();
        Terra terra = new Terra(location.getWorld(), location.getChunk().getX(), location.getChunk().getZ());
        if (!faction.ownsTerritory(terra) && !faction.getTemporarios().contains(terra)) {
            player.sendMessage("§cVocê só pode definir uma home em um território da sua facção.");
            return false;
        }

        // Set the home
        DataManager.setHome(faction.getNome(), homeName, location);
        faction.addHome(homeName, location);

        player.sendMessage("§aHome '" + homeName + "' definida com sucesso em X: " + location.getBlockX() +
                ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ() + ".");
        return true;
    }

    // Updated command: /f home listar
    private boolean homeListar(NDPlayer dplayer) {
        if (!validateFaction(dplayer, true)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();

        openHomesMenu(player, faction);
        return true;
    }

    // Updated command: /f delhome <name>
    private boolean homeDelete(NDPlayer dplayer, String homeName) {
        if (!validateFaction(dplayer, true) || !hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();

        // Verify if the home exists in DataManager
        if (DataManager.getHome(faction.getNome(), homeName) == null) {
            player.sendMessage("§cA home '" + homeName + "' não existe.");
            return false;
        }

        // Confirm deletion via inventory
        Inventory inv = Bukkit.createInventory(null, 27, "Deletar Home: " + homeName);
        inv.setItem(11, new ItemBuilder(Material.WOOL, 1, 5)
                .setName("§aConfirmar")
                .setLore("§7Confirmar a exclusão da home '" + homeName + "'")
                .toItemStack());
        inv.setItem(15, new ItemBuilder(Material.WOOL, 1, 14)
                .setName("§cCancelar")
                .setLore("§7Cancelar a exclusão")
                .toItemStack());

        player.openInventory(inv);
        return true;
    }

    // Updated menu for listing homes
    private void openHomesMenu(Player player, NDFaction faction) {
        Inventory menu = Bukkit.createInventory(null, 36, "Homes - [" + faction.getTag() + "]");

        Map<String, Location> homes = DataManager.loadHomes(faction.getNome());
        if (homes.isEmpty()) {
            ItemStack noHomes = new ItemBuilder(Material.BARRIER)
                    .setName("§cNenhuma Home Definida")
                    .setLore("§7Use /f sethome <nome> para criar uma home.")
                    .toItemStack();
            menu.setItem(13, noHomes);
        } else {
            int slot = 10;
            List<Integer> forbiddenSlots = Arrays.asList(13, 22, 31);

            for (Map.Entry<String, Location> entry : homes.entrySet()) {
                String homeName = entry.getKey();
                Location loc = entry.getValue();

                ItemStack item = new ItemBuilder(Material.ENDER_PEARL)
                        .setName("§aHome: " + homeName)
                        .setLore(
                                "§7X: §f" + loc.getBlockX() + " Y: §f" + loc.getBlockY() + " Z: §f" + loc.getBlockZ(),
                                "",
                                "§e▎ §fEsquerdo: §7Teleportar",
                                "§e▎ §fDireito: §7Deletar (Líder/Capitão)"
                        )
                        .toItemStack();

                while (slot < 26 && forbiddenSlots.contains(slot)) {
                    slot++;
                }
                if (slot < 26) {
                    menu.setItem(slot++, item);
                }
            }
        }

        // Close button
        menu.setItem(31, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124"))
                .setName("§cFechar")
                .setLore("§7Fechar o menu de homes")
                .toItemStack());

        player.openInventory(menu);
    }
	
	private boolean entrar(NDPlayer dplayer, String tag) {
	    Player player = dplayer.getPlayer();
	    if (dplayer.hasFaction()) {
	        player.sendMessage(Config.get("Mensagens.SemPermissao").toString().replace("&", "§"));
	        return false;
	    }

	    NDFaction faction = Utils.getFactionByTag(tag);
	    if (faction == null) {
	        player.sendMessage(Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§"));
	        return false;
	    }

	    // Verificar limite de membros
	    int maxMembers = (int) Config.get("Padrao.MaximoMembrosEmFac");
	    if (faction.getAll().size() >= maxMembers) {
	        // Procurar por um jogador offline no roster para expulsar
	        NDPlayer playerToKick = null;
	        for (NDPlayer member : faction.getAll()) {
	            if (Bukkit.getPlayer(member.getNome()) == null && faction.isInRoster(member.getNome())) {
	                playerToKick = member;
	                break;
	            }
	        }

	        if (playerToKick == null) {
	            player.sendMessage(Config.get("Mensagens.MaxMembros").toString().replace("&", "§"));
	            return false;
	        }

	        // Expulsar o jogador offline
	        faction.getRecrutas().remove(playerToKick);
	        faction.getMembros().remove(playerToKick);
	        faction.getCapitoes().remove(playerToKick);
	        faction.removeFromRoster(playerToKick.getNome());
	        playerToKick.setFaction(null);

	        // Notificar membros online sobre a expulsão
	        faction.getAllOnline().forEach(p -> p.sendMessage(Config.get("Mensagens.JogadorExpulso").toString()
	                .replace("&", "§")
	                .replace("<jogador>", p.getName())));

	        // Disparar evento de expulsão
	        doFactionPlayerChangeFaction kickEvent = new doFactionPlayerChangeFaction(playerToKick, Motivo.EXPULSO);
	        Bukkit.getPluginManager().callEvent(kickEvent);
	    }

	    // Verificar permissão de admin
	    boolean isAdmin = player.hasPermission("nd.commands.factions.entrar");
	    if (!isAdmin && !dplayer.getConvites().contains(faction) && !faction.isInRoster(player.getName())) {
	        player.sendMessage(Config.get("Mensagens.SemConvite").toString().replace("&", "§").replace("<tag>", "[" + tag + "] " + faction.getNome()));
	        return false;
	    }

	    // Adicionar o jogador como recruta
	    List<NDPlayer> newRecruits = faction.getRecrutas();
	    newRecruits.add(dplayer);
	    faction.setRecrutas(newRecruits);

	    // Remover convite, se existir (não aplicável para admins ou roster)
	    if (!isAdmin && dplayer.getConvites().contains(faction)) {
	        List<NDFaction> newInvites = dplayer.getConvites();
	        newInvites.remove(faction);
	        dplayer.setConvites(newInvites);
	    }

	    // Vincular jogador à facção
	    dplayer.setFaction(faction);

	    // Notificar membros online
	    faction.getAllOnline().forEach(p -> p.sendMessage(Config.get("Mensagens.EntrouNaFac").toString()
	            .replace("&", "§")
	            .replace("<jogador>", dplayer.getNome())));

	    // Notificar o jogador
	    player.sendMessage(Config.get("Mensagens.EntrouFac").toString()
	            .replace("&", "§")
	            .replace("<tag>", "[" + tag + "] " + faction.getNome()));

	    // Disparar evento
	    doFactionPlayerChangeFaction event = new doFactionPlayerChangeFaction(dplayer, Motivo.ENTRAR);
	    Bukkit.getPluginManager().callEvent(event);
	    return true;
	}
	
	private boolean roster(NDPlayer dplayer, String[] args) {
	    Player player = dplayer.getPlayer();

	    // Caso o jogador não tenha facção, exibir menu de facções com roster
	    if (!dplayer.hasFaction()) {
	        List<NDFaction> rosterFactions = getRosterFactions(player.getName());
	        if (rosterFactions.isEmpty()) {
	            player.sendMessage(Config.get("Mensagens.SemRoster").toString().replace("&", "§"));
	            return false;
	        }
	        // Verificar se o jogador especificou um número de página
	        int page = 1;
	        if (args.length > 1 && Utils.isInteger(args[1])) {
	            page = Integer.parseInt(args[1]);
	        }
	        openRosterMenu(player, rosterFactions, page);
	        return true;
	    }

	    // Caso o jogador tenha facção, verificar cargo e subcomando
	    if (!validateFaction(dplayer, true) || !hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
	        return false;
	    }

	    NDFaction faction = dplayer.getFaction();

	    if (args.length < 2) {
	        player.sendMessage("§cUso: /f roster <add/remove/listar> [jogador]");
	        return false;
	    }

	    String subCommand = args[1].toLowerCase();
	    if (!subCommand.equals("add") && !subCommand.equals("remove") && !subCommand.equals("listar")) {
	        player.sendMessage("§cSubcomando inválido. Use: /f roster <add/remove/listar> [jogador]");
	        return false;
	    }

	    if (subCommand.equals("listar")) {
	        List<String> roster = faction.getRoster();
	        if (roster.isEmpty()) {
	            player.sendMessage("§7O roster da facção " + faction.getNome() + " está vazio.");
	        } else {
	            player.sendMessage("§aRoster da facção " + faction.getNome() + " (" + roster.size() + "):");
	            player.sendMessage("§7" + String.join(", ", roster));
	        }
	        return true;
	    }

	    if (args.length < 3) {
	        player.sendMessage("§cUso: /f roster " + subCommand + " <jogador>");
	        return false;
	    }

	    String targetName = args[2];
	    if (DataManager.players.get(targetName) == null) {
	        player.sendMessage("§cJogador " + targetName + " não encontrado.");
	        return false;
	    }

	    if (subCommand.equals("add")) {
	        if (faction.isInRoster(targetName)) {
	            player.sendMessage("§cO jogador " + targetName + " já está no roster.");
	            return false;
	        }
	        faction.addToRoster(targetName);
	        player.sendMessage("§aJogador " + targetName + " adicionado ao roster da facção " + faction.getNome() + ".");
	        return true;
	    } else {
	        if (!faction.isInRoster(targetName)) {
	            player.sendMessage("§cO jogador " + targetName + " não está no roster.");
	            return false;
	        }
	        faction.removeFromRoster(targetName);
	        player.sendMessage("§aJogador " + targetName + " removido do roster da facção " + faction.getNome() + ".");
	        return true;
	    }
	}

	// Método para coletar facções onde o jogador está no roster
	public static List<NDFaction> getRosterFactions(String playerName) {
	    List<NDFaction> rosterFactions = new ArrayList<>();
	    for (NDFaction faction : DataManager.factions.values()) {
	        if (faction.isInRoster(playerName)) {
	            rosterFactions.add(faction);
	        }
	    }
	    return rosterFactions;
	}

	// Método para abrir o menu de facções com roster com suporte a múltiplas páginas
	public static void openRosterMenu(Player player, List<NDFaction> factions, int page) {
	    // Configurações de paginação
	    final int ITEMS_PER_PAGE = 21; // Limitado aos 21 slots especificados
	    final int maxPages = (int) Math.ceil((double) factions.size() / ITEMS_PER_PAGE);
	    // Garantir que a página solicitada seja válida
	    page = Math.max(1, Math.min(page, maxPages));

	    // Criar inventário com tamanho fixo de 54 slots
	    Inventory menu = Bukkit.createInventory(null, 54, "Facções com Roster - Página " + page);

	    // Slots permitidos para os itens das facções
	    final int[] ALLOWED_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

	    // Calcular índices de início e fim para a página atual
	    int startIndex = (page - 1) * ITEMS_PER_PAGE;
	    int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, factions.size());

	    // Preencher o inventário com as facções da página atual nos slots permitidos
	    for (int i = startIndex; i < endIndex; i++) {
	        NDFaction faction = factions.get(i);
	        ItemStack item = new ItemBuilder(Banners.getAlphabet(new ItemStack(Material.BANNER), faction.getTag(), DyeColor.WHITE, DyeColor.BLACK))
	                .setName("§a[" + faction.getTag() + "] " + faction.getNome())
	                .addItemFlag(ItemFlag.HIDE_POTION_EFFECTS)
	                .setLore(
	                        "§7Membros: §f" + faction.getAll().size() + "/" + Config.get("Padrao.MaximoMembrosEmFac"),
	                        "§7Terras: §f" + faction.getTerras().size(),
	                        "",
	                        "§eClique para entrar na facção!"
	                )
	                .toItemStack();
	        // Mapear o índice da facção para um slot permitido
	        int slotIndex = i - startIndex;
	        if (slotIndex < ALLOWED_SLOTS.length) {
	            menu.setItem(ALLOWED_SLOTS[slotIndex], item);
	        }
	    }

	    // Adicionar botões de navegação
	    // Botão "Anterior" (slot 45, se não for a primeira página)
	    if (page > 1) {
	        ItemStack previousButton = new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/da1d55b3f989410a34752650e248c9b6c1783a7ec2aa3fd7787bdc4d0e637d39"))
	                .setName("§aPágina Anterior")
	                .setLore("§7Voltar para a página " + (page - 1))
	                .toItemStack();
	        menu.setItem(45, previousButton);
	    }

	    // Botão "Fechar" (slot 49, centralizado)
	    ItemStack closeButton = new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124"))
	            .setName("§cFechar")
	            .setLore("§7Fechar o menu")
	            .toItemStack();
	    menu.setItem(49, closeButton);

	    // Botão "Próxima" (slot 53, se não for a última página)
	    if (page < maxPages) {
	        ItemStack nextButton = new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/fa87e3d96e1cfeb9ccfb3ba53a217faf5249e285533b271a2fb284c30dbd9829"))
	                .setName("§aPróxima Página")
	                .setLore("§7Avançar para a página " + (page + 1))
	                .toItemStack();
	        menu.setItem(53, nextButton);
	    }

	    player.openInventory(menu);
	}
	
	private boolean handleGeradores(NDPlayer dplayer, String[] args) {
	    Player player = dplayer.getPlayer();

	    // /f geradores listar [tag] → não exige facção
	    if (args.length > 1 && args[1].equalsIgnoreCase("listar")) {
	        return handleGeradoresListar(dplayer, args);
	    }

	    // A partir daqui, exige facção e cargo
	    if (!validateFaction(dplayer, true) || !hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
	        return false;
	    }

	    NDFaction faction = dplayer.getFaction();

	    // /f geradores
	    if (args.length == 1) {
	        openGeneratorsMenu(player, faction);
	        return true;
	    }

	    // /f geradores store <chunkX> <chunkZ>
	    if (args[1].equalsIgnoreCase("store")) {
	        if (args.length < 4) {
	            player.sendMessage("§cUso: /f geradores store <chunkX> <chunkZ>");
	            return false;
	        }
	        try {
	            int chunkX = Integer.parseInt(args[2]);
	            int chunkZ = Integer.parseInt(args[3]);
	            Terra terra = new Terra(player.getWorld(), chunkX, chunkZ);

	            if (!faction.getTerras().contains(terra)) {
	                player.sendMessage("§cEsta terra não pertence à sua facção.");
	                return false;
	            }
	            if (faction.isSobAtaque()) {
	                player.sendMessage("§cNão é possível armazenar geradores enquanto a facção está sob ataque.");
	                return false;
	            }
	            boolean stored = faction.storeGeneratorsFromTerritory(terra, player.getName());
	            if (stored) {
	                player.sendMessage("§aGeradores armazenados com sucesso!");
	            } else {
	                player.sendMessage("§cNenhum gerador encontrado nesta terra.");
	            }
	            return stored;
	        } catch (NumberFormatException e) {
	            player.sendMessage("§cCoordenadas inválidas. Use apenas números.");
	            return false;
	        }
	    }

	    player.sendMessage("§cSubcomando inválido. Uso: /f geradores listar [tag]");
	    return false;
	}


    private boolean handleGeradoresListar(NDPlayer dplayer, String[] args) {
        Player player = dplayer.getPlayer();

        // Verificar se a tag da facção foi fornecida
        if (args.length < 3) {
            player.sendMessage("§cUtilize /f geradores listar <tag>");
            return false;
        }

        // Verificar se é uma tag de facção
        String tag = args[2].toUpperCase();
        NDFaction faction = Utils.getFactionByTag(tag);
        if (faction == null) {
            player.sendMessage("§cFacção com a tag " + tag + " não encontrada.");
            return false;
        }

        // Exibir geradores da facção específica
        displayFactionGenerators(player, faction);
        return true;
    }

    private void displayFactionGenerators(Player player, NDFaction faction) {
        Map<EntityType, Integer> stored = faction.getStoredGenerators();
        Map<EntityType, Integer> placed = faction.getPlacedGenerators();
        int totalStored = faction.getTotalStoredGenerators();
        int totalPlaced = faction.getTotalPlacedGenerators();

        // Cabeçalho da facção
        player.sendMessage("§a[" + faction.getTag() + "] " + faction.getNome());

        // Geradores armazenados
        if (stored.isEmpty()) {
            player.sendMessage(" §7Armazenados: Nenhum");
        } else {
            player.sendMessage(" §7Armazenados (" + totalStored + "):");
            for (Map.Entry<EntityType, Integer> entry : stored.entrySet()) {
                String mobName = FactionGenerators.GENERATOR_NAMES.getOrDefault(entry.getKey(), entry.getKey().name());
                player.sendMessage("  §f- " + mobName + ": " + entry.getValue());
            }
        }

        // Geradores colocados
        if (placed.isEmpty()) {
            player.sendMessage(" §7Colocados: Nenhum");
        } else {
            player.sendMessage(" §7Colocados (" + totalPlaced + "):");
            for (Map.Entry<EntityType, Integer> entry : placed.entrySet()) {
                String mobName = FactionGenerators.GENERATOR_NAMES.getOrDefault(entry.getKey(), entry.getKey().name());
                player.sendMessage("  §f- " + mobName + ": " + entry.getValue());
            }
        }

        player.sendMessage("");
    }
	
	private boolean fly(NDPlayer dplayer, String[] args) {
	    Player player = dplayer.getPlayer();

	    boolean hasFlyAllyOwnPerm = player.hasPermission("nd.commands.factions.fly.allyown");
	    boolean hasFlyProtectedPerm = player.hasPermission("nd.commands.factions.fly.protected");
	    boolean hasFlyAllPerm = player.hasPermission("nd.commands.factions.fly.all");
	    boolean hasFlyPerm = player.hasPermission("nd.commands.factions.fly"); // Mantida para compatibilidade
	    Terra terra = new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
	    
	    if (terra.isTemporario()) {
	    	player.sendMessage("§cVocê não pode voar em terreno temporario!");
	    	return true;
	    }
	    // Verifica se o jogador tem alguma permissão de voo
	    if (!hasFlyAllyOwnPerm && !hasFlyProtectedPerm && !hasFlyAllPerm && !hasFlyPerm) {
	        player.sendMessage(color(Config.get("Mensagens.SemPermissao")));
	        return true;
	    }

	    Protecao protecao = Utils.getProtection(player.getLocation().getChunk(), player);

	    // Verifica se o jogador pode ativar/desativar voo com base no território e permissões
	    boolean canToggleFly = false;
	    if (hasFlyAllPerm) {
	        canToggleFly = true; // fly.all permite voar em qualquer lugar
	    } else if (hasFlyAllyOwnPerm && (protecao == Protecao.Sua || protecao == Protecao.Aliada)) {
	        NDFaction faction = new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).getFaction();
	        if (faction != null && faction.isSobAtaque()) {
	            player.sendMessage("§cVocê não pode voar em territórios que estejam sob ataque.");
	            return true;
	        }
	        canToggleFly = true;
	    } else if (hasFlyProtectedPerm && protecao == Protecao.Protegida) {
	        canToggleFly = true;
	    } else if (hasFlyPerm && (protecao == Protecao.Sua || protecao == Protecao.Aliada || protecao == Protecao.Protegida)) {
	        // Compatibilidade com a permissão antiga
	        if (protecao == Protecao.Sua || protecao == Protecao.Aliada) {
	            NDFaction faction = new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ()).getFaction();
	            if (faction != null && faction.isSobAtaque()) {
	                player.sendMessage("§cVocê não pode voar em territórios que estejam sob ataque.");
	                return true;
	            }
	        }
	        canToggleFly = true;
	    }

	    if (canToggleFly) {
	        boolean flying = player.getAllowFlight();
	        player.setAllowFlight(!flying);
	        player.sendMessage(color(Config.get("Mensagens.FlyAtivado")).replace("<ativou>", flying ? "desativou" : "ativou"));
	    } else {
	        player.sendMessage(color(Config.get("Mensagens.NaoPodeVoar")));
	    }

	    return true;
	}

	private String color(Object message) {
		return message == null ? "" : message.toString().replace("&", "§");
	}
	
	private boolean setSpawnPoint(NDPlayer dplayer, String[] args) {
	    Player player = dplayer.getPlayer();
	    if (!validateFaction(dplayer, true) || !hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
	        return false;
	    }

	    NDFaction faction = dplayer.getFaction();
	    EntityType mobType;
	    double x, y, z;

	    try {
	        mobType = EntityType.valueOf(args[1].toUpperCase());
	        x = Double.parseDouble(args[2]) + 0.5; // Centralizar no bloco
	        y = Double.parseDouble(args[3]);
	        z = Double.parseDouble(args[4]) + 0.5; // Centralizar no bloco
	    } catch (NumberFormatException e) {
	        player.sendMessage("§cCoordenadas inválidas. Use apenas números.");
	        return false;
	    } catch (IllegalArgumentException e) {
	        player.sendMessage(Config.get("Mensagens.MobTypeInvalido").toString()
	                .replace("&", "§")
	                .replace("<mobType>", args[1]));
	        return false;
	    }

	    // Verificar se a facção possui pelo menos um spawner do tipo especificado
	    Map<EntityType, Integer> spawnerTypes = getSpawnerTypesInAllClaims(faction);
	    if (!spawnerTypes.containsKey(mobType) || spawnerTypes.get(mobType) == 0) {
	        player.sendMessage("§cNenhum spawner de " + mobType.name() + " encontrado nos claims da facção.");
	        return false;
	    }

	    // Definir o ponto de spawn global
	    Location location = new Location(player.getWorld(), x, y, z);
	    faction.setSpawnPoint(mobType, location);

	    player.sendMessage(Config.get("Mensagens.PontoSpawnDefinido").toString()
	            .replace("&", "§")
	            .replace("<mobType>", mobType.name()));
	    return true;
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

	    // Obter a contagem de spawners por tipo em todos os claims da facção
	    Map<EntityType, Integer> spawnerTypes = getSpawnerTypesInAllClaims(faction);
	    if (spawnerTypes.isEmpty()) {
	        player.sendMessage(Config.get("Mensagens.NenhumSpawner").toString().replace("&", "§"));
	        return false;
	    }

	    Inventory menu = Bukkit.createInventory(player, 45, "Gerenciar Spawners");
	    int slot = 10; // Começar do slot 10
	    List<Integer> forbiddenSlots = Arrays.asList(17, 18, 26, 27, 35); // Slots proibidos

	    for (Map.Entry<EntityType, Integer> entry : spawnerTypes.entrySet()) {
	        EntityType mobType = entry.getKey();
	        int count = entry.getValue();

	        // Obter a cabeça MHF_ correspondente ao tipo de mob
	        String mhfName = Heads.getMHFHeadName(mobType);
	        ItemStack item = new ItemBuilder(Material.SKULL_ITEM, 1, 3)
	                .setSkullOwner(mhfName)
	                .toItemStack();

	        ItemMeta meta = item.getItemMeta();
	        meta.setDisplayName("§aSpawner de " + mobType.toString() + " (" + count + ")");
	        List<String> lore = new ArrayList<>();
	        lore.add("  §e▎ §fB. Direito: §7definir ponto de spawn.");
	        lore.add("  §e▎ §fB. Esquerdo: §7remover ponto de spawn.");
	        lore.add("");
	        // Verificar o ponto de spawn global para o mobType
	        Location spawnPoint = faction.getSpawnPoint(mobType);
	        if (spawnPoint != null) {
	            lore.add("§aSpawn: §fX: " + spawnPoint.getBlockX() + ", Y: " + spawnPoint.getBlockY() + ", Z: " + spawnPoint.getBlockZ());
	        } else {
	            lore.add("§cNenhum ponto de spawn definido.");
	        }
	        meta.setLore(lore);
	        item.setItemMeta(meta);

	        // Encontrar o próximo slot válido
	        while (slot < 45 && forbiddenSlots.contains(slot)) {
	            slot++;
	        }

	        // Adicionar o item no slot atual, se não exceder o tamanho do inventário
	        if (slot < 45) {
	            menu.setItem(slot, item);
	            slot++; // Incrementar o slot para o próximo item
	        }
	    }

	    player.openInventory(menu);
	    return true;
	}

	private Map<EntityType, Integer> getSpawnerTypesInAllClaims(NDFaction faction) {
	    Map<EntityType, Integer> spawnerTypes = new HashMap<>();
	    
	    // Combinar terras permanentes e temporárias
	    List<Terra> allTerras = new ArrayList<>();
	    allTerras.addAll(faction.getTerras());
	    allTerras.addAll(faction.getTemporarios());

	    // Iterar sobre todos os claims (Terras) da facção
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
	
	private boolean handleInv(NDPlayer dplayer, String[] args) {
	    Player player = dplayer.getPlayer();

	    // Verificar se o jogador tem facção
	    if (!validateFaction(dplayer, true)) {
	        return false;
	    }

	    // Verificar se o jogador tem o cargo de Líder ou Capitão
	    if (!hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
	        return false;
	    }

	    // Verificar se foi fornecido o nome do jogador alvo
	    if (args.length < 2) {
	        player.sendMessage("§cUtilize /f inv <jogador>");
	        return false;
	    }

	    String targetName = args[1];
	    NDPlayer target = DataManager.players.get(targetName);

	    // Verificar se o jogador alvo existe
	    if (target == null) {
	        player.sendMessage(Config.get("Mensagens.UsuarioNaoExiste").toString().replace("&", "§"));
	        return false;
	    }

	    // Verificar se o jogador alvo está na mesma facção
	    if (!dplayer.getFaction().equals(target.getFaction())) {
	        player.sendMessage("§cO jogador " + targetName + " não é membro da sua facção.");
	        return false;
	    }

	    // Verificar se o jogador alvo está online
	    Player targetPlayer = Bukkit.getPlayer(targetName);
	    if (targetPlayer == null) {
	        player.sendMessage(Config.get("Mensagens.NaoEstaOnline").toString().replace("&", "§").replace("<jogador>", targetName));
	        return false;
	    }

	    // Criar um inventário "proxy" apenas para visualização
	    Inventory viewOnlyInventory = Bukkit.createInventory(null, 36, "Inventário de " + targetName);
	    // Copiar os itens do inventário do jogador alvo
	    for (int i = 0; i < targetPlayer.getInventory().getSize(); i++) {
	        ItemStack item = targetPlayer.getInventory().getItem(i);
	        if (item != null) {
	            viewOnlyInventory.setItem(i, item.clone()); // Clonar para evitar modificações
	        }
	    }

	    // Abrir o inventário apenas para visualização
	    player.openInventory(viewOnlyInventory);
	    return true;
	}
	
	private boolean handleTop(NDPlayer dplayer, String[] args) {
		 Player p = dplayer.getPlayer();
		Inventory inv = Bukkit.createInventory(null, 36, "Escolha uma categoria");

		inv.setItem(12, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/9fd108383dfa5b02e86635609541520e4e158952d68c1c8f8f200ec7e88642d")).setName("§aRanking de Valor")
				.setLore("§7Veja as facções com mais valor", "§7no servidor").toItemStack());
		inv.setItem(14,
				new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/61b635bc92dd20011d6e7d01257e6ea737b49fb7de941d3bbd1877104fc5eb1e")).setName("§aRanking Geral")
						.setLore("§7Veja as facções com o melhor", "§7desempenho de modo geral no servidor.")
						.toItemStack());
		inv.setItem(31, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124")).setName("§aVoltar").toItemStack());

		p.openInventory(inv);
		return false;
		
	}

	    public static void openGeneratorsMenu(Player player, NDFaction faction) {
	        Inventory inv = Bukkit.createInventory(null, 54, "Geradores - [" + faction.getTag() + "]");

	        // Store Spawners button
	        ItemStack storeItem = new ItemBuilder(Material.CHEST)
	            .setName("§eArmazenar Geradores")
	            .setLore(
	                "  §7Clique para armazenar geradores",
	                "  §7do seu inventário",
	                "",
	                "  §e▎ §fB. Esquerdo: §7Adiciona tudo",
	                "  §e▎ §fB. Direito: §7Adiciona um",
	                "  §e▎ §fDireito + Shift: §7escolher quantidade"
	            )
	            .toItemStack();
	        inv.setItem(12, storeItem);

	        // Logs button
	        ItemStack logsItem = new ItemStack(Material.BOOK);
	        ItemMeta meta1 = logsItem.getItemMeta();

	        if (meta1 == null) return;

	        meta1.setDisplayName("§aVer Logs");

	        List<String> lore = new ArrayList<>();
	        lore.add("§7Clique para visualizar o histórico");
	        lore.add("§7de ações de geradores");
	        lore.add(""); // blank line for separation

	        List<NDFaction.GeneratorLog> logs = faction.getGeneratorLogs();

	        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	        int start = Math.max(logs.size() - 5, 0);
	        List<NDFaction.GeneratorLog> lastLogs = logs.subList(start, logs.size());

	        for (NDFaction.GeneratorLog log : lastLogs) {
	            String action = "§aArmazenou";
	            if ("place".equals(log.getAction())) {
	                action = "§aColocou";
	            } else if ("withdraw".equals(log.getAction())) {
	                action = "§cRetirou";
	            }

	            String mobName = FactionGenerators.GENERATOR_NAMES.getOrDefault(log.getGeneratorType(), log.getGeneratorType().name());
	            String time = sdf.format(new Date(log.getTimestamp()));

	            lore.add("§7- §f" + log.getPlayerName() + " " + action + " §f" + log.getAmount() + "§7 gerador(es) de §f" + mobName + "§7 em §f" + time);
	        }

	        meta1.setLore(lore);
	        logsItem.setItemMeta(meta1);

	        inv.setItem(14, logsItem);

	        // Store from Territory button
	        ItemStack territoryItem = new ItemBuilder(Material.EMPTY_MAP)
	            .setName("§eArmazenar do Terreno")
	            .setLore(
	                "  §7Clique para armazenar geradores",
	                "  §7de um terreno da facção",
	                "",
	                "  §e▎ §fB. Esquerdo: §7Retirar tudo",
	                "  §e▎ §fB. Direito: §7Retirar um"
	            )
	            .toItemStack();
	        inv.setItem(13, territoryItem);

	        // Display stored spawners
	        int slot = 28;
	        for (Map.Entry<EntityType, Integer> entry : faction.getStoredGenerators().entrySet()) {
	            if (slot >= 44) break;
	            EntityType type = entry.getKey();
	            String name = FactionGenerators.GENERATOR_NAMES.getOrDefault(type, type.name());
	            int stored = entry.getValue();
	            int placed = faction.getPlacedGenerators().getOrDefault(type, 0); // Quantidade colocada para este tipo

	            // Obter a cabeça MHF_ correspondente ao tipo de mob
	            String mhfName = Heads.getMHFHeadName(type);
	            ItemStack item = new ItemBuilder(Material.SKULL_ITEM, 1, 3)
	                    .setSkullOwner(mhfName)
	                    .toItemStack();

	            ItemMeta meta = item.getItemMeta();
	            meta.setDisplayName("§a" + name + " Spawner");
	            meta.setLore(Arrays.asList(
	                "  §7Armazenados: §f" + stored,
	                "  §7Colocados: §f" + placed,
	                "",
	                "  §e▎ §fB. Esquerdo: §7Retirar tudo",
	                "  §e▎ §fB. Direito: §7Retirar um",
	                "  §e▎ §fDireito + Shift: §7escolher quantidade"
	            ));
	            item.setItemMeta(meta);
	            inv.setItem(slot++, item);
	        }

	        // Close button
	        inv.setItem(10, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/da1d55b3f989410a34752650e248c9b6c1783a7ec2aa3fd7787bdc4d0e637d39"))
	            .setName("§cFechar")
	            .setLore("§7Fechar o menu de geradores")
	            .toItemStack());

	        player.openInventory(inv);
	    }

    
    private boolean handleClaim(Player player, NDPlayer dplayer, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUse: /f claim <chunkX> <chunkZ>");
            return false;
        }

        try {
            int chunkX = Integer.parseInt(args[1]);
            int chunkZ = Integer.parseInt(args[2]);
            return Utils.claimTerritory(player, dplayer, chunkX, chunkZ);
        } catch (NumberFormatException e) {
            player.sendMessage("§cCoordenadas inválidas. Use apenas números.");
            return false;
        }
    }

    
    public static boolean permissoes(NDPlayer dplayer) {
        if (!Comandos.validateFaction(dplayer, true) || !Comandos.hasRequiredCargo(dplayer, Cargo.Lider, Cargo.Capitão)) {
            return false;
        }

        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();
        Inventory inv = Bukkit.createInventory(null, 36, "Gerenciar Permissões - [" + faction.getTag() + "]");

        // Item for "Permissões Geral"
        ItemBuilder geralBuilder = new ItemBuilder(Material.BOOK_AND_QUILL)
                .setName("§fPermissões Gerais")
                .setLore("§7Gerenciar permissões para cargos e relações", "§7Clique para abrir")
                .addItemFlag(ItemFlag.HIDE_ENCHANTS);
        inv.setItem(11, geralBuilder.toItemStack());

        // Item for "Permissões Membros"
        ItemBuilder membrosBuilder = new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/2e6ea4baef6943a3e5fd90565be5b4354cb07cc82ecb195572eddb6360cd6f14"))
                .setName("§fPermissões Membros")
                .setLore("§7Gerenciar permissões individuais por membro", "§7Clique para abrir")
                .addItemFlag(ItemFlag.HIDE_ENCHANTS);
        inv.setItem(15, membrosBuilder.toItemStack());

        // Close button
        inv.setItem(31, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124"))
                .setName("§cFechar")
                .setLore("§7Fechar o menu de permissões")
                .toItemStack());

        player.openInventory(inv);
        return true;
    }
    

    @SuppressWarnings("incomplete-switch")
	private void sendInsufficientArgsMessage(Player player, CommandType type) {
        String command = "/f " + type.name().toLowerCase();
        switch (type) {
            case CRIAR:
                command += " <tag> <nome>";
                break;
            case ACEITAR:
                command += " convite <tag>";
                break;
            case CONVIDAR:
            case EXPULSAR:
            case PROMOVER:
            case REBAIXAR:
            case TRANSFERIR:
                command += " <jogador>";
                break;
            case MEMBROS:
            case INFO:
            case CLAIM:
            case RELACAO:
                command += " <tag>";
                break;
            case PERFIL:
                command += " <jogador>";
                break;
            case PERMISSOES: 
                break;
            case SETSPAWN:
                command += " <chunkX> <chunkZ> <mobType>";
                break;
        }
        player.sendMessage(Config.get("Mensagens.ArgsInsuficientes").toString()
                .replace("&", "§")
                .replace("<comando>", command));
    }

    private static boolean hasRequiredCargo(NDPlayer player, Cargo... requiredCargos) {
        if (Arrays.asList(requiredCargos).contains(player.getCargo())) return true;
        player.getPlayer().sendMessage(Config.get("Mensagens.SemCargo").toString()
                .replace("&", "§")
                .replace("<cargo>", player.getCargo().toString().toLowerCase()));
        return false;
    }

    private static boolean validateFaction(NDPlayer player, boolean needsFaction) {
        if (needsFaction && !player.hasFaction()) {
            player.getPlayer().sendMessage(Config.get("Mensagens.SemFac").toString().replace("&", "§"));
            return false;
        }
        return true;
    }

    private boolean validatePlayer(NDPlayer player, String targetName, boolean needsOnline) {
        NDPlayer target = DataManager.players.get(targetName);
        if (target == null) {
            player.getPlayer().sendMessage(Config.get("Mensagens.UsuarioNaoExiste").toString().replace("&", "§"));
            return false;
        }
        if (needsOnline && Bukkit.getPlayer(targetName) == null) {
            player.getPlayer().sendMessage(Config.get("Mensagens.NaoEstaOnline").toString().replace("&", "§"));
            return false;
        }
        return true;
    }

    private static ItemStack createPlayerSkull(NDPlayer player) {
        return new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                .setSkullOwner(player.getNome())
                .setName("§7" + player.getNome())
                .setLore(
                        "§fPoder: §7" + player.getPoder() + "/" + player.getPodermax(),
                        "§fCargo: §7" + player.getCargo().toString(),
                        "§fKDR: §7" + player.getKDR(),
                        "§fAbates: §7" + player.getKills(),
                        "§fMortes: §7" + player.getMortes(),
                        "§fStatus: " + (Bukkit.getPlayer(player.getNome()) == null ? "§cOffline" : "§aOnline"),
                        "§fÚltimo login: §7" + player.getLast()
                )
                .toItemStack();
    }

    private boolean showFactionInfo(Player player, NDPlayer dplayer, String tag) {
        NDFaction faction = Utils.getFactionByTag(tag);
        if (faction == null) {
            player.sendMessage(Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§"));
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, dplayer.hasFaction() ? 36 : 27,
        		"§" + (dplayer.hasFaction() ? "2" : "6") + "§2§r§2§r[" + faction.getTag() + "] " + faction.getNome());

        if (dplayer.hasFaction()) {
            inv.setItem(12, createPlayerSkull(dplayer));
            inv.setItem(14, Utils.getFactionBanner(dplayer.getFaction()));
            inv.setItem(19, Utils.getFactionBanner(faction));
            inv.setItem(22, new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                    .setAmount(faction.getAll().size())
                    .setName("§aMembros")
                    .setLore("§7Mostrar membros da §f[" + faction.getTag() + "] " + faction.getNome())
                    .toItemStack());
            inv.setItem(25, new ItemBuilder(Material.CHAINMAIL_CHESTPLATE)
                    .setName("§cMudar relação")
                    .setLore("§7Clique para alterar a relação com a [" + faction.getTag() + "] " + faction.getNome())
                    .toItemStack());
        } else {
            inv.setItem(11, Utils.getFactionBanner(faction));
            inv.setItem(13, createPlayerSkull(dplayer));
            inv.setItem(15, new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                    .setAmount(faction.getAll().size())
                    .setName("§aMembros")
                    .setLore("§7Mostrar membros da §f[" + faction.getTag() + "] " + faction.getNome())
                    .toItemStack());
        }

        player.openInventory(inv);
        return false;
    }

    private boolean handleMapa(NDPlayer dplayer, String option) {
        Player player = dplayer.getPlayer();
        if (option.equals("on")) {
            Utils.updateMap(player, player.getLocation());
            dplayer.setLigado(true);
            player.sendMessage("§aMapa ligado");
        } else if (option.equals("off")) {
            dplayer.setLigado(false);
            player.sendMessage("§cMapa desligado");
        } else {
            Utils.updateMap(player, player.getLocation());
        }
        return false;
    }

    private boolean toggleVerTerras(NDPlayer dplayer) {
        dplayer.switchVerTerras();
        dplayer.getPlayer().sendMessage(Config.get(dplayer.isVerTerras() ? 
                "Mensagens.AtivarVerTerras" : "Mensagens.DesativarVerTerras")
                .toString().replace("&", "§"));
        return false;
    }

    private void showHelp(Player player, NDPlayer dplayer) {
        List<String> messages = (List<String>) Config.get(dplayer.hasFaction() ? 
                "Mensagens.Ajuda.ComFac" : "Mensagens.Ajuda.SemFac");
        messages.forEach(s -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', s)));
    }

    private boolean info(Player player, String tag) {
        NDFaction faction;

        // Se nenhum tag for fornecido, usar a facção do jogador
        if (tag == null || tag.trim().isEmpty()) {
            NDPlayer ndPlayer = DataManager.players.get(player.getName());
            if (ndPlayer == null || !ndPlayer.hasFaction()) {
                String message = Config.get("Mensagens.NaoPossuiFaccao") != null
                        ? Config.get("Mensagens.NaoPossuiFaccao").toString().replace("&", "§")
                        : "§cVocê não possui uma facção.";
                player.sendMessage(message);
                return false;
            }
            faction = ndPlayer.getFaction();
        } else {
            // Caso contrário, buscar a facção pelo tag fornecido
            faction = Utils.getFactionByTag(tag);
            if (faction == null) {
                player.sendMessage(Config.get("Mensagens.FacNaoExiste") != null
                        ? Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§")
                        : "§cEssa facção não existe.");
                return false;
            }
        }

        String allies = faction.getAliados().isEmpty() ? "§7Nenhum" : 
                String.join(", ", faction.getAliados().stream().map(NDFaction::getTag).toArray(String[]::new)) + ".";
        String enemies = faction.getInimigos().isEmpty() ? "§7Nenhum" : 
                String.join(", ", faction.getInimigos().stream().map(NDFaction::getTag).toArray(String[]::new)) + ".";
        String onlineMembers = String.join(" ", faction.getAllOnline().stream()
                .map(p -> DataManager.players.get(p.getName()).getCargoSimbolo() + p.getName())
                .toArray(String[]::new));

        player.sendMessage("");
        player.sendMessage("          §e" + faction.getTag() + " - " + faction.getNome());
        player.sendMessage("§2Terras/§4Poder§2/§4Poder máximo§2: §7" + faction.getTerras().size() + "/" + 
                faction.getPoder() + "/" + faction.getPoderMax());
        player.sendMessage("§2Líder: §7" + faction.getLider().getNome());
        player.sendMessage("§2Membros: §7" + faction.getAll().size() + "/" + 
                Config.get("Padrao.MaximoMembrosEmFac"));
        player.sendMessage("§2Membros online: §7" + faction.getAllOnline().size());
        player.sendMessage(" §7" + onlineMembers);
        player.sendMessage("§2KDR: §7" + faction.getKdr());
        player.sendMessage("§2Abates: §7" + faction.getAbates());
        player.sendMessage("§2Mortes: §7" + faction.getMortes());
        player.sendMessage("§2Aliados: §7" + allies);
        player.sendMessage("§2Inimigos: §7" + enemies);
        return true;
    }
    
    private boolean listar(Player player, int page) {
        List<NDFaction> onlineFactions = Utils.getOnlineFactions();
        int itemsPerPage = 10;
        int totalPages = (int) Math.ceil((double) onlineFactions.size() / itemsPerPage);

        if (page < 1 || page > totalPages) {
            player.sendMessage("§cNúmero de página inválida");
            return false;
        }

        player.sendMessage("          §eFacções Online - " + page + "/" + totalPages);
        
        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, onlineFactions.size());
        
        for (NDFaction faction : onlineFactions.subList(start, end)) {
            player.sendMessage(" §a" + faction.getTag() + " §8- §7" + faction.getNome() + 
                    " §8- §f" + faction.getAllOnline().size() + "/" + 
                    Config.get("Padrao.MaximoMembrosEmFac") + " §7online");
        }
        
        return true;
    }

    private boolean abandonar(NDPlayer player) {
        if (!validateFaction(player, true) || !hasRequiredCargo(player, Cargo.Capitão, Cargo.Lider)) return false;

        Terra terra = new Terra(player.getPlayer().getLocation().getChunk().getWorld(),
                player.getPlayer().getLocation().getChunk().getX(),
                player.getPlayer().getLocation().getChunk().getZ());

        if (Utils.getProtection(terra.getChunk(), player.getPlayer()) != Protecao.Sua) {
            player.getPlayer().sendMessage(Config.get("Mensagens.NaoPodeAbandonar").toString().replace("&", "§"));
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, 36, "Abandonar");
        inv.setItem(13, new ItemBuilder(Material.getMaterial("GRASS")).setName("§fDeseja abandonar esta terra?").toItemStack());
        inv.setItem(20, new ItemBuilder(Material.getMaterial("WOOL"), 1, (short) 5).setName("§aAceitar").toItemStack());
        inv.setItem(24, new ItemBuilder(Material.getMaterial("WOOL"), 1, (short) 14)
                .setName("§cNegar")
                .setLore("§7Cancelar esta operação.")
                .toItemStack());

        player.getPlayer().openInventory(inv);
        return true;
    }

    private boolean abandonarall(NDPlayer player) {
        if (!validateFaction(player, true) || !hasRequiredCargo(player, Cargo.Lider)) return false;

        if (player.getFaction().getTerras().isEmpty()) {
            player.getPlayer().sendMessage(Config.get("Mensagens.SemTerras").toString().replace("&", "§"));
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, 36, "Abandonar todos terrenos");
        inv.setItem(13, new ItemBuilder(Material.GRASS)
                .setName("§fDeseja abandonar todas as terras da sua facção?")
                .toItemStack());
        inv.setItem(20, new ItemBuilder(Material.WOOL, 1, 5).setName("§aAceitar").toItemStack());
        inv.setItem(24, new ItemBuilder(Material.WOOL, 1, 14)
                .setName("§cNegar")
                .setLore("§7Cancelar esta operação.")
                .toItemStack());

        player.getPlayer().openInventory(inv);
        return true;
    }

    public boolean relacao(NDPlayer lider, String[] args) {
        if (!validateFaction(lider, true) || !hasRequiredCargo(lider, Cargo.Lider)) return false;

        Player player = lider.getPlayer();
        NDFaction faction = lider.getFaction();

        if (args.length == 1) {
            // Open the new relation menu when no tag is provided
            Inventory inv = Bukkit.createInventory(null, 27, "Relações - [" + faction.getTag() + "]");
            inv.setItem(11, new ItemBuilder(Heads.VERDE)
                    .setName("§aDefinir Relação")
                    .setLore("§7Clique para definir uma relação com",
                    		"§7alguma facção ou se preferir use",
                    		"§7o comando '§f/f relacao <tag>§7'")
                    .toItemStack());
            
            // Calculate pending requests
            int pedidos = faction.getPedidosRelacoesAliados().size() + faction.getPedidosRelacoesNeutras().size();
            inv.setItem(13, new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.LIME)
                    .setName("§aPedidos de relação")
                    .setAmount(pedidos)
                    .setLore(pedidos == 0 ? "§cSua facção não possui pedidos de relação."
                                          : "§7Mostrar os pedidos de relações de outras facções.")
                    .toItemStack());
            
            inv.setItem(15, new ItemBuilder(Material.BOOK)
                    .setName("§eVer Relações")
                    .setLore("§7Veja o histórico de mudanças de relações")
                    .toItemStack());
            player.openInventory(inv);
            return true;
        }
        
        if (args.length == 2 && args[1].equalsIgnoreCase("verpedidos")) {
            int pedidos = faction.getPedidosRelacoesAliados().size() + faction.getPedidosRelacoesNeutras().size();
            if (pedidos == 0) {
                player.sendMessage("§cSua facção não possui pedidos de relação.");
                return true;
            }

            Inventory inv = Bukkit.createInventory(null, 36, "Pedidos de relações");
            List<ItemStack> relacoes = new ArrayList<>();

            for (NDFaction f : faction.getPedidosRelacoesAliados()) {
                relacoes.add(new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.BLUE)
                        .setName("§a[" + f.getTag() + "] " + f.getNome())
                        .setLore("§7Relação: §aAliada", "§7Poder: §a" + f.getPoder() + "/" + f.getPoderMax(),
                                "§7KDR: §a" + f.getKdr(), "§7Membros: §a" + f.getAll().size(), "",
                                "§7Botão esquerdo: §fAceitar", "§7Botão direito: §fNegar")
                        .toItemStack());
            }

            for (NDFaction f : faction.getPedidosRelacoesNeutras()) {
                relacoes.add(new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.WHITE)
                        .setName("§f[" + f.getTag() + "] " + f.getNome())
                        .setLore("§7Relação: §fNeutra", "§7Poder: §a" + f.getPoder() + "/" + f.getPoderMax(),
                                "§7KDR: §a" + f.getKdr(), "§7Membros: §a" + f.getAll().size(), "",
                                "§7Botão esquerdo: §fAceitar", "§7Botão direito: §fNegar")
                        .toItemStack());
            }

            int lastIndex = 0;
            for (int i = 0; i < 54; i++) {
                if (i < 9 || i == 17 || i == 26 || i == 35 || i == 44 || i == 53 || i % 9 == 0) continue;
                if (lastIndex >= relacoes.size() || lastIndex > 13) break;
                inv.setItem(i, relacoes.get(lastIndex));
                lastIndex++;
            }

            inv.setItem(31, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124")).setName("§aVoltar").toItemStack());
            player.openInventory(inv);
            return true;
        }


        // Handle /f relacao <tag>
        String tag = args[1].toUpperCase();
        NDFaction targetFaction = Utils.getFactionByTag(tag);
        if (targetFaction == null) {
            player.sendMessage(Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§"));
            return false;
        }
        if (faction == targetFaction) {
            player.sendMessage(Config.get("Mensagens.NaoPodeRelacao").toString().replace("&", "§"));
            return false;
        }
        if (targetFaction.getPedidosRelacoesAliados().contains(faction) ||
                targetFaction.getPedidosRelacoesNeutras().contains(faction) ||
                faction.getPedidosRelacoesAliados().contains(targetFaction) ||
                faction.getPedidosRelacoesNeutras().contains(targetFaction)) {
            player.sendMessage("§cJá há um pedido de relação com essa facção pendente");
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, 27,
                "Relações - [" + faction.getTag() + "] >> [" + targetFaction.getTag() + "]");

        ItemBuilder aliada = new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setLeatherArmorColor(Color.BLUE)
                .setName("§aAliada")
                .addItemFlag(ItemFlag.HIDE_ENCHANTS);
        if (faction.isAliada(targetFaction)) {
            aliada.addEnchant(Enchantment.ARROW_DAMAGE, 1);
        }
        inv.setItem(11, aliada.toItemStack());

        ItemBuilder neutra = new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setLeatherArmorColor(Color.WHITE)
                .setName("§fNeutra")
                .addItemFlag(ItemFlag.HIDE_ENCHANTS);
        if (faction.isNeutra(targetFaction)) {
            neutra.addEnchant(Enchantment.ARROW_DAMAGE, 1);
        }
        inv.setItem(13, neutra.toItemStack());

        ItemBuilder inimiga = new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setLeatherArmorColor(Color.RED)
                .setName("§cInimiga")
                .addItemFlag(ItemFlag.HIDE_ENCHANTS);
        if (faction.isInimigo(targetFaction)) {
            inimiga.addEnchant(Enchantment.ARROW_DAMAGE, 1);
        }
        inv.setItem(15, inimiga.toItemStack());

        player.openInventory(inv);
        return true;
    }

    private boolean perfil(Player player, String targetName) {
        NDPlayer target = DataManager.players.get(targetName);
        if (target == null) {
            player.sendMessage(Config.get("Mensagens.UsuarioNaoExiste").toString().replace("&", "§"));
            return false;
        }

        player.sendMessage("");
        player.sendMessage(target.hasFaction() ? 
                "          §e" + target.getFaction().getTag() + " " + target.getCargoSimbolo() + target.getNome() : 
                "          §e" + target.getNome());
        player.sendMessage("");
        player.sendMessage("§fPoder: §7" + target.getPoder() + "/" + target.getPodermax());
        player.sendMessage("§fFacção: §7" + (target.hasFaction() ? 
                target.getFaction().getTag() + " - " + target.getFaction().getNome() : "§7Nenhuma"));
        player.sendMessage("§fCargo: §7" + target.getCargoSimbolo() + target.getCargo().toString());
        player.sendMessage("§fAbates: §7" + target.getKills());
        player.sendMessage("§fMortes: §7" + target.getMortes());
        player.sendMessage("§fStatus: §7" + (Bukkit.getPlayer(target.getNome()) == null ? "§cOffline" : "§aOnline"));
        player.sendMessage("§fÚltimo login: §7" + target.getLast());
        player.sendMessage("");
        return true;
    }

    private boolean membros(Player player, String tag) {
        NDFaction faction = Utils.getFactionByTag(tag);
        if (faction == null) {
            player.sendMessage(Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§"));
            return false;
        }

        // Depuração: Verificar se o jogador está na lista
        NDPlayer dplayer = DataManager.players.get(player.getName());
        if (dplayer != null && dplayer.hasFaction() && dplayer.getFaction().equals(faction)) {
            Bukkit.getLogger().info("Membros da facção " + faction.getNome() + ": " + faction.getAll().stream()
                    .map(NDPlayer::getNome)
                    .collect(Collectors.joining(", ")));
        }

        Utils.openMembersMenu(player, faction);
        return true;
    }

    private boolean base(NDPlayer player) {
        if (!validateFaction(player, true)) return false;
        if (!player.getFaction().hasBase()) {
            player.getPlayer().sendMessage(Config.get("Mensagens.SemBase").toString().replace("&", "§"));
            return false;
        }

        player.getPlayer().sendMessage(Config.get("Mensagens.TeleportadoParaBase").toString().replace("&", "§"));
        player.getPlayer().teleport(player.getFaction().getBase());
        return true;
    }

    private boolean transferir(NDPlayer lider, String newLeaderName) {
        if (!validateFaction(lider, true) || !hasRequiredCargo(lider, Cargo.Lider) ||
                !validatePlayer(lider, newLeaderName, true)) return false;

        NDPlayer newLeader = DataManager.players.get(newLeaderName);
        if (lider.getFaction() != newLeader.getFaction()) {
            lider.getPlayer().sendMessage(Config.get("Mensagens.NaoEstaNaFac").toString().replace("&", "§"));
            return false;
        }
        if (lider.equals(newLeader)) {
            lider.getPlayer().sendMessage(Config.get("Mensagens.NaoPodeSePromover").toString().replace("&", "§"));
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, 36, "Transferir liderança");
        inv.setItem(13, createPlayerSkull(newLeader));
        inv.setItem(20, new ItemBuilder(Material.WOOL, 1, 5)
                .setName("§aAceitar (Leia abaixo)")
                .setLore("§cVocê tem certeza que deseja transferir a liderança para " + newLeaderName + "?")
                .toItemStack());
        inv.setItem(24, new ItemBuilder(Material.WOOL, 1, 14)
                .setName("§cNegar")
                .setLore("§7Cancelar esta operação")
                .toItemStack());

        lider.getPlayer().openInventory(inv);
        return true;
    }

    private boolean promover(NDPlayer promoter, String targetName) {
        // Validações iniciais
        if (!validateFaction(promoter, true) || !hasRequiredCargo(promoter, Cargo.Capitão, Cargo.Lider)) {
            return false;
        }
        if (!validatePlayer(promoter, targetName, true)) {
            return false;
        }

        NDPlayer target = DataManager.players.get(targetName);
        Player bukkitPlayer = promoter.getPlayer();

        // Verificar se o alvo está na mesma facção
        if (!promoter.getFaction().equals(target.getFaction())) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.NaoEstaNaFac")
                    .toString()
                    .replace("&", "§"));
            return false;
        }

        // Verificar se o jogador está tentando se promover
        if (promoter.equals(target)) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.NaoPodeSePromover")
                    .toString()
                    .replace("&", "§"));
            return false;
        }

        // Verificar se o jogador alvo está online
        Player targetPlayer = target.getPlayer();
        if (targetPlayer == null) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.NaoEstaOnline")
                    .toString()
                    .replace("&", "§"));
            return false;
        }

        Cargo currentCargo = target.getCargo();
        Cargo newCargo;

        // Determinar o novo cargo
        switch (currentCargo) {
            case Recruta:
                newCargo = Cargo.Membro;
                break;
            case Membro:
                if (promoter.getCargo() != Cargo.Lider) {
                    bukkitPlayer.sendMessage("§cApenas o líder pode promover membros a capitão.");
                    return false;
                }
                newCargo = Cargo.Capitão;
                break;
            case Capitão:
                bukkitPlayer.sendMessage("§cNão é possível promover acima de capitão.");
                return false;
            case Lider:
                bukkitPlayer.sendMessage(Config.get("Mensagens.NaoPodePromover")
                        .toString()
                        .replace("&", "§")
                        .replace("<jogador>", targetName));
                return false;
            default:
                bukkitPlayer.sendMessage("§cCargo inválido para promoção.");
                return false;
        }

        // Realizar a promoção usando o método da classe NDFaction
        boolean success = target.getFaction().promover(target);
        if (!success) {
            bukkitPlayer.sendMessage("§cFalha ao promover o jogador. Tente novamente.");
            return false;
        }

        // Atualizar o cargo do jogador
        target.setCargo(newCargo);

        // Enviar mensagens de sucesso
        String newRoleName = newCargo.toString().toLowerCase();
        bukkitPlayer.sendMessage(Config.get("Mensagens.JogadorPromovido")
                .toString()
                .replace("&", "§")
                .replace("<jogador>", targetName)
                .replace("<cargo>", newRoleName));
        targetPlayer.sendMessage(Config.get("Mensagens.VoceFoiPromovido")
                .toString()
                .replace("&", "§")
                .replace("<jogador>", promoter.getNome())
                .replace("<cargo>", newRoleName));

        return true;
    }

    private boolean rebaixar(NDPlayer promoter, String targetName) {
        // Validações iniciais
        if (!validateFaction(promoter, true) || !hasRequiredCargo(promoter, Cargo.Capitão, Cargo.Lider)) {
            return false;
        }
        if (!validatePlayer(promoter, targetName, true)) {
            return false;
        }

        NDPlayer target = DataManager.players.get(targetName);
        Player bukkitPlayer = promoter.getPlayer();

        // Verificar se o alvo está na mesma facção
        if (!promoter.getFaction().equals(target.getFaction())) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.NaoEstaNaFac")
                    .toString()
                    .replace("&", "§"));
            return false;
        }

        // Verificar se o jogador está tentando se rebaixar
        if (promoter.equals(target)) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.NaoPodeSePromover")
                    .toString()
                    .replace("&", "§"));
            return false;
        }

        // Verificar se o jogador alvo está online
        Player targetPlayer = target.getPlayer();
        if (targetPlayer == null) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.NaoEstaOnline")
                    .toString()
                    .replace("&", "§"));
            return false;
        }

        Cargo currentCargo = target.getCargo();
        Cargo newCargo;

        // Determinar o novo cargo
        switch (currentCargo) {
            case Capitão:
                if (promoter.getCargo() != Cargo.Lider) {
                    bukkitPlayer.sendMessage("§cApenas o líder pode rebaixar capitães.");
                    return false;
                }
                newCargo = Cargo.Membro;
                break;
            case Membro:
                newCargo = Cargo.Recruta;
                break;
            case Recruta:
                bukkitPlayer.sendMessage(Config.get("Mensagens.NaoPodeRebaixar")
                        .toString()
                        .toString()
                        .replace("&", "§")
                        .replace("<jogador>", targetName));
                return false;
            case Lider:
                bukkitPlayer.sendMessage("§cNão é possível rebaixar o líder.");
                return false;
            default:
                bukkitPlayer.sendMessage("§cCargo inválido para rebaixamento.");
                return false;
        }


        // Realizar o rebaixamento usando o método da classe NDFaction
        boolean success = target.getFaction().rebaixar(target);
        if (!success) {
            bukkitPlayer.sendMessage("§cFalha ao rebaixar o jogador. Tente novamente.");
            return false;
        }

        // Atualizar o cargo do jogador
        target.setCargo(newCargo);

        // Salvar as alterações no banco de dados
        try {
            target.save();
            target.getFaction().save();
        } catch (Exception e) {
            bukkitPlayer.sendMessage("§cFalha ao salvar rebaixamento. Tente novamente.");
            return false;
        }

        // Enviar mensagens de sucesso
        String newRoleName = newCargo.toString().toLowerCase();
        bukkitPlayer.sendMessage(Config.get("Mensagens.JogadorRebaixado")
                .toString()
                .replace("&", "§")
                .replace("<jogador>", targetName)
                .replace("<cargo>", newRoleName));
        targetPlayer.sendMessage(Config.get("Mensagens.VoceFoiRebaixado")
                .toString()
                .replace("&", "§")
                .replace("<jogador>", promoter.getNome())
                .replace("<cargo>", newRoleName));

        return true;
    }

    private boolean expulsar(NDPlayer expeller, String targetName) {
        if (!validateFaction(expeller, true) || !hasRequiredCargo(expeller, Cargo.Capitão, Cargo.Lider) ||
                !validatePlayer(expeller, targetName, false)) return false;

        NDPlayer target = DataManager.players.get(targetName);
        if (expeller.getFaction() != target.getFaction()) {
            expeller.getPlayer().sendMessage(Config.get("Mensagens.NaoEstaNaFac").toString().replace("&", "§"));
            return false;
        }
        if (expeller.equals(target)) {
            expeller.getPlayer().sendMessage(Config.get("Mensagens.NaoPodeSeExpulsar").toString().replace("&", "§"));
            return false;
        }
        if (target.getCargo() == Cargo.Lider || 
                (target.getCargo() == Cargo.Capitão && expeller.getCargo() == Cargo.Capitão)) {
            expeller.getPlayer().sendMessage(Config.get("Mensagens.NaoPodeExpulsar").toString().replace("&", "§"));
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, 36, "Expulsar jogador");
        inv.setItem(13, createPlayerSkull(target));
        inv.setItem(20, new ItemBuilder(Material.WOOL, 1, 5)
                .setName("§aAceitar (Leia abaixo)")
                .setLore("§cVocê tem certeza que deseja remover " + targetName + "?")
                .toItemStack());
        inv.setItem(24, new ItemBuilder(Material.WOOL, 1, 14)
                .setName("§cNegar")
                .setLore("§7Cancelar esta operação")
                .toItemStack());

        expeller.getPlayer().openInventory(inv);
        return true;
    }

    private boolean dominar(NDPlayer player) {
        if (!validateFaction(player, true) || !hasRequiredCargo(player, Cargo.Capitão, Cargo.Lider, Cargo.Membro)) {
            return false;
        }

        Player bukkitPlayer = player.getPlayer();
        Chunk chunk = bukkitPlayer.getLocation().getChunk();
        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
        NDFaction playerFaction = player.getFaction();

        // Verificar se o chunk já pertence à facção do jogador
        if (Utils.getProtection(terra.getChunk(), bukkitPlayer) == Protecao.Sua) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.JaDominada").toString().replace("&", "§"));
            return false;
        }

        // Verificar se o chunk pertence a outra facção
        if (terra.getFaction() != null) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.JaDominada").toString().replace("&", "§"));
            return false;
        }

        Location loc = Utils.getLocation(chunk, (int) bukkitPlayer.getLocation().getY());
        StateFlag.State pvpFlag = Utils.getWorldGuardPvpFlag(loc);

        // Verificar se o chunk atual está em zona protegida ou de guerra
        if (pvpFlag == StateFlag.State.DENY || pvpFlag == StateFlag.State.ALLOW) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.FaccoesPertos").toString()
                    .replace("&", "§")
                    .replace("<nomes>", pvpFlag == StateFlag.State.DENY ? "zona protegida" : "zona de guerra"));
            return false;
        }

        // Verificar proximidade com zonas protegidas ou de guerra (6 chunks)
        List<Terra> nearbyProtectedChunks = Utils.getNearbyChunks(chunk, 6);
        for (Terra nearbyTerra : nearbyProtectedChunks) {
            Location nearbyLoc = Utils.getLocation(nearbyTerra.getChunk(), (int) bukkitPlayer.getLocation().getY());
            StateFlag.State nearbyPvpFlag = Utils.getWorldGuardPvpFlag(nearbyLoc);
            if (nearbyPvpFlag == StateFlag.State.DENY || nearbyPvpFlag == StateFlag.State.ALLOW) {
                int distance = Utils.getDistance(chunk, nearbyTerra.getChunk());
                if (distance < 6) {
                    String name = nearbyPvpFlag == StateFlag.State.DENY ? "zona protegida" : "zona de guerra";
                    bukkitPlayer.sendMessage(Config.get("Mensagens.FaccoesPertos").toString()
                            .replace("&", "§")
                            .replace("<nomes>", name));
                    return false;
                }
            }
        }

        // Verificar proximidade com qualquer facção (5 chunks)
        List<Terra> nearbyChunks = Utils.getNearbyChunks(chunk, 5);
        for (Terra nearbyTerra : nearbyChunks) {
            NDFaction nearbyFaction = nearbyTerra.getFaction();
            if (nearbyFaction != null && !nearbyFaction.equals(playerFaction)) {
                int distance = Utils.getDistance(chunk, nearbyTerra.getChunk());
                if (distance < 5) {
                    bukkitPlayer.sendMessage(Config.get("Mensagens.FaccoesPertos").toString()
                            .replace("&", "§")
                            .replace("<nomes>", nearbyFaction.getNome()));
                    return false;
                }
            }
        }

        // Verificar poder da facção
        if (playerFaction.getPoder() < playerFaction.getTerras().size() + 1) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.SemPoderDominar").toString().replace("&", "§"));
            return false;
        }

        // Verificar proximidade com outros territórios da facção
        List<Terra> adjacentChunks = Utils.getNearbyChunks(chunk, 1);
        boolean isAdjacent = false;
        for (Terra nearbyTerra : adjacentChunks) {
            if (playerFaction.equals(nearbyTerra.getFaction())) {
                isAdjacent = true;
                break;
            }
        }
        if (!isAdjacent && !playerFaction.getTerras().isEmpty()) {
            bukkitPlayer.sendMessage(Config.get("Mensagens.FaccoesPertos").toString()
                    .replace("&", "§")
                    .replace("<nomes>", "sua própria facção"));
            return false;
        }

        // Reivindicar o território
        List<Terra> newTerras = playerFaction.getTerras();
        newTerras.add(terra);
        playerFaction.setTerras(newTerras);
        terra.setFaction(playerFaction);
        bukkitPlayer.sendMessage(Config.get("Mensagens.TerraDominada").toString().replace("&", "§"));
        return true;
    }
    
    private boolean proteger(NDPlayer player) {
        if (!validateFaction(player, true) || !hasRequiredCargo(player, Cargo.Capitão, Cargo.Lider, Cargo.Membro)) 
            return false;

        NDFaction faction = player.getFaction();
        Terra terra = new Terra(player.getPlayer().getLocation().getChunk().getWorld(),
                player.getPlayer().getLocation().getChunk().getX(),
                player.getPlayer().getLocation().getChunk().getZ());

        Protecao protection = Utils.getProtection(terra.getChunk(), player.getPlayer());
        if (protection == Protecao.Sua) {
            player.getPlayer().sendMessage(Config.get("Mensagens.JaDominada").toString().replace("&", "§"));
            return false;
        }
        if (protection != Protecao.Livre) {
            player.getPlayer().sendMessage(Config.get("Mensagens.NaoPodeProteger").toString().replace("&", "§"));
            return false;
        }

        int protectionCost = (int) Config.get("Geral.ProtegerTemporariamentePreco");
        if (Vault.getPlayerBalance(player.getPlayer()) < protectionCost) {
            player.getPlayer().sendMessage(Config.get("Mensagens.SemDinheiro").toString().replace("&", "§"));
            return false;
        }

        Vault.take(player.getPlayer(), protectionCost);
        player.getPlayer().sendMessage(Config.get("Mensagens.TerraDominada").toString().replace("&", "§"));
        List<Terra> newTerras = faction.getTemporarios();
        newTerras.add(terra);
        faction.setTemporarios(newTerras);

        new BukkitRunnable() {
            @Override
            public void run() {
                List<Terra> updatedTerras = faction.getTemporarios();
                updatedTerras.remove(terra);
                faction.setTemporarios(updatedTerras);
                if (Bukkit.getPlayer(player.getPlayer().getName()) != null) {
                    player.getPlayer().sendMessage(Config.get("Mensagens.TerrenoExpirou").toString().replace("&", "§"));
                }
            }
        }.runTaskLaterAsynchronously(Main.getPlugin(Main.class),
                20L * 60 * ((int) Config.get("Geral.TempoDeProtecaoTemporaria")));

        return true;
    }

    private boolean sair(NDPlayer player) {
        if (!validateFaction(player, true)) return false;
        if (player.getCargo() == Cargo.Lider) {
            player.getPlayer().sendMessage(Config.get("Mensagens.NaoPodeSair").toString().replace("&", "§"));
            return false;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "Sair da facção");
        inv.setItem(11, new ItemBuilder(Material.WOOL, 1, 5)
                .setName("§aAceitar (Leia abaixo)")
                .setLore("§7Você tem certeza que deseja sair da facção " + player.getFaction().getNome())
                .toItemStack());
        inv.setItem(15, new ItemBuilder(Material.WOOL, 1, 14)
                .setName("§cNegar")
                .setLore("§7Cancelar esta operação")
                .toItemStack());

        player.getPlayer().openInventory(inv);
        return true;
    }

    private boolean desfazer(NDPlayer player) {
        if (!validateFaction(player, true) || !hasRequiredCargo(player, Cargo.Lider)) return false;

        Inventory inv = Bukkit.createInventory(null, 27, "Desfazer facção");
        inv.setItem(11, new ItemBuilder(Material.WOOL, 1, 5)
                .setName("§aAceitar (Leia abaixo)")
                .setLore("§7Você tem certeza que deseja desfazer a facção " + player.getFaction().getNome())
                .toItemStack());
        inv.setItem(15, new ItemBuilder(Material.WOOL, 1, 14)
                .setName("§cNegar")
                .setLore("§7Cancelar esta operação")
                .toItemStack());

        player.getPlayer().openInventory(inv);
        return true;
    }

    public static boolean help(NDPlayer player) {
        Player bukkitPlayer = player.getPlayer();
        bukkitPlayer.closeInventory();

        boolean hasFaction = player.hasFaction();
        NDFaction faction = player.getFaction();
        boolean isLider = player.getCargo() == Cargo.Lider;
        boolean isCapitao = player.getCargo() == Cargo.Capitão;
        boolean isStaff = isLider || isCapitao;

        Inventory inv = Bukkit.createInventory(null, hasFaction ? 54 : 45,
                "§r§" + (hasFaction ? "f§a§c§r[" + faction.getTag() + "] " + faction.getNome()
                                   : "a§b§c§r" + "Sem Facção"));

        inv.setItem(10, createPlayerSkull(player));

        if (hasFaction) {
            inv.setItem(28, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/5a140a8dfb0f1f5ab979241e58c253a94d65e725f180ab52396e1d8e4c81ce37"))
                    .setName("§aMembros")
                    .setLore("§7Mostrar membros da facção.")
                    .setAmount(faction.getAll().size())
                    .toItemStack());
            
            if (isStaff) inv.setItem(41, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/97b2189300f339ba050e01ae2a540b7e89ef896a55c7916dcc9e5851af86411e"))
                    .setName("§aGeradores")
                    .setLore("§7Gerencie e armazene os geradores", "§7da facção.")
                    .toItemStack());

            inv.setItem(30, criarItemBase(faction, player));
            inv.setItem(31, criarItemTerrenos(faction));
            if (isStaff) inv.setItem(32, criarItemPermissao());

            if (isLider) {
                inv.setItem(40, new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.LIME)
                        .setName("§aRelações")
                        .setAmount(1)
                        .setLore("§7Clique para gerenciar todas",
                        		"§7as relações da sua facção")
                        .toItemStack());
            }

            inv.setItem(37, new ItemBuilder(Material.PAPER)
                    .setName("§eGerenciar Convites")
                    .setAmount(1)
                    .setLore("§7Clique para gerenciar os",
                    		"§7Convites da sua facção.")
                    .toItemStack());
            
            inv.setItem(11, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/94f90c7bd60bfd0dfc31808d0484d8c2db9959f68df91fbf29423a3da62429a6"))
                    .setName("§eInvasão")
                    .setAmount(1)
                    .setLore("  §7Clique para ver o",
                    		 "  §7status da invasão e",
                    		 "  §7ofertas de invasão.",
                    		 "",
                    		 "  §e▎ §fB. Direito: §7Ofertas de invasão.",
                    		 "  §e▎ §fB. Esquerdo: §7Ver status da invasão."
                    		)
                    .toItemStack());
            
            inv.setItem(12, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/12d7a751eb071e08dbbc95bc5d9d66e5f51dc6712640ad2dfa03defbb68a7f3a"))
                    .setName("§eHomes")
                    .setAmount(1)
                    .setLore("  §7Clique para ver as",
                    		 "  §7homes da sua facção",
                    		 "  §7e o baú da facção.",
                    		 "",
                    		 "  §e▎ §fB. Direito: §7Ver baú.",
                    		 "  §e▎ §fB. Esquerdo: §7Ver homes.")
                    .toItemStack());
            
            inv.setItem(34, Utils.getFactionBanner(faction));

            inv.setItem(43, new ItemBuilder(Material.WOOD_DOOR)
                    .setName(isLider ? "§cDesfazer facção" : "§cSair")
                    .setLore("§7Utilize para " + (isLider ? "desfazer sua facção" : "sair da sua facção"))
                    .toItemStack());

        } else {
            inv.setItem(29, new ItemBuilder(Material.BANNER)
                    .setName("§eCriar facção")
                    .setLore("§7Crie sua própria facção!")
                    .toItemStack());

            inv.setItem(32, new ItemBuilder(Material.PAPER)
                    .setAmount(player.getConvites().size())
                    .setName("§aConvites de Facções")
                    .setLore(player.getConvites().isEmpty() ?
                            "§7Você não possui nenhum convite pendente" :
                            "§7Ver convites pendentes")
                    .toItemStack());
        }

        inv.setItem(hasFaction ? 38 : 30, criarItemMapa(player));
        inv.setItem(hasFaction ? 39 : 33, criarItemVerTerras(player));
        inv.setItem(16, criarItemAjuda());
        inv.setItem(15, criarItemFaccoesOnline());
        inv.setItem(14, criarItemRankingFaccoes());
        inv.setItem(hasFaction ? 29 : 31, criarItemTerreno(player, faction, hasFaction, isLider, isCapitao));

        bukkitPlayer.openInventory(inv);
        return false;
    }
    private static ItemStack criarItemPermissao() {
        return new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/6d0f4061bfb767a7f922a6ca7176f7a9b20709bd0512696beb15ea6fa98ca55c"))
                .setName("§aPermissões")
                .setLore("§7Gerenciar permissões dos cargos", "§7dentro do território da facção")
                .toItemStack();
    }

    private static ItemStack criarItemBase(NDFaction faction, NDPlayer player) {
        boolean isStaff = player.getCargo() == Cargo.Lider || player.getCargo() == Cargo.Capitão;
        ItemBuilder base = new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/ffccfe5096a335b9ab78ab4f778ae499f4ccab4e2c95fa349227fd060759baaf")).setName("§aBase");

        if (faction.hasBase()) {
            if (isStaff) {
                base.setLore("§7Clique para ir até a base da sua facção", "",
                             "§fBotão esquerdo: §7Ir para a base",
                             "§fBotão direito: §7Definir base.",
                             "§fShift + botão direito: §7Remover base.");
            } else {
                base.setLore("§7Clique para ir até a base da sua facção");
            }
        } else {
        	base.setLore(
        		    isStaff 
        		        ? new String[] { "§7Sua facção não possui base.", "§fBotão direito: §7Definir base" } 
        		        : new String[] { "§7Sua facção não possui base." }
        		);
        }

        return base.toItemStack();
    }

    private static ItemStack criarItemTerrenos(NDFaction faction) {
        return new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/5eaa9ac15758d5177a896605985e98beac8fee0e6b2c68a8dc1f3c91c079fb89"))
                .setAmount(faction.getTerras().size())
                .setName("§aTerrenos")
                .setLore(faction.getTerras().isEmpty() ? "§7Sua facção não tem terrenos" : 
                        "§7Mostrar os terrenos conquistados")
                .toItemStack();
    }

    private static ItemStack criarItemMapa(NDPlayer player) {
        return new ItemBuilder(Material.MAP)
                .setName("§aMapa")
                .setLore("§7Veja o mapa da região próxima a você", "",
                         "§fBotão esquerdo: §7Ver o mapa",
                         "§fBotão direito: §7" + (player.isMapaLigado() ? "Desligar" : "Ligar") + " modo mapa", "",
                         "§fModo mapa: " + (player.isMapaLigado() ? "§aLigado" : "§cDesligado"))
                .toItemStack();
    }

    private static ItemStack criarItemVerTerras(NDPlayer player) {
        boolean ligado = player.isVerTerras();
        return new ItemBuilder((ligado ? Heads.VERDE : Heads.CINZA).clone())
                .setName("§aVer terras")
                .setLore("§7Clique para " + (ligado ? "ocultar" : "ver") + " as delimitações das terras", "",
                         "§fVer terras:" + (ligado ? "§a Ligado" : "§c Desligado"))
                .toItemStack();
    }

    private static ItemStack criarItemAjuda() {
        return new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/d295a929236c1779eab8f57257a86071498a4870196941f4bfe1951e8c6ee21a"))
                .setName("§eAjuda")
                .setLore("§7Todas as ações disponíveis neste menu",
                         "§7também podem ser realizadas por",
                         "§7comando. Utilize o comando '§f/f ajuda§7'")
                .toItemStack();
    }

    private static ItemStack criarItemFaccoesOnline() {
        return new ItemBuilder(Heads.LARANJA.clone())
                .setName("§eFacções online")
                .setLore("§7Clique para ver as facções online.")
                .toItemStack();
    }

    private static ItemStack criarItemRankingFaccoes() {
        return new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/55dfa284aa15324e5178561f803f5976228d95115583ab031266ae24ee1a99d1"))
                .setName("§eRanking de facções")
                .setLore("§7Clique para ver o ranking com", "§7as melhores facções do servidor.")
                .toItemStack();
    }

    private static ItemStack criarItemTerreno(NDPlayer player, NDFaction faction, boolean hasFaction, boolean isLider, boolean isCapitao) {
        Player bukkitPlayer = player.getPlayer();
        Chunk chunk = bukkitPlayer.getLocation().getChunk();
        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
        Protecao protecao = Utils.getProtection(chunk, bukkitPlayer);

        // Definir URLs de texturas diferentes para cada tipo de proteção
        String textureUrl;
        switch (protecao) {
            case Livre:
                textureUrl = "http://textures.minecraft.net/texture/5ae6a911fa2b9f4cb2a365eca8d0b60ccd1b0e3ba4809d934537518309c1b4fc"; // Textura para Zona Livre
                break;
            case Guerra:
                textureUrl = "http://textures.minecraft.net/texture/852d3c5c8c528e65008e260ea40a4476027abe22611d9fc78101071bf0564050"; // Textura para Zona de Guerra (exemplo)
                break;
            case Protegida:
                textureUrl = "http://textures.minecraft.net/texture/c8c758ab08cbe59730972c9c2941f95475804858ce4b0a2b49f5b5c5027d66c"; // Textura para Zona Protegida (exemplo)
                break;
            case Sua:
                textureUrl = "http://textures.minecraft.net/texture/5eaa9ac15758d5177a896605985e98beac8fee0e6b2c68a8dc1f3c91c079fb89"; // Textura para Terreno da própria facção (exemplo)
                break;
            case Aliada:
                textureUrl = "http://textures.minecraft.net/texture/5eaa9ac15758d5177a896605985e98beac8fee0e6b2c68a8dc1f3c91c079fb89"; // Textura para Terreno aliado (exemplo)
                break;
            case Inimiga:
                textureUrl = "http://textures.minecraft.net/texture/852d3c5c8c528e65008e260ea40a4476027abe22611d9fc78101071bf0564050"; // Textura para Terreno inimigo (exemplo)
                break;
            case Neutra:
                textureUrl = "http://textures.minecraft.net/texture/5eaa9ac15758d5177a896605985e98beac8fee0e6b2c68a8dc1f3c91c079fb89"; // Textura para Terreno neutro (exemplo)
                break;
            default:
                textureUrl = "http://textures.minecraft.net/texture/5eaa9ac15758d5177a896605985e98beac8fee0e6b2c68a8dc1f3c91c079fb89"; // Textura padrão
                break;
        }

        // Criar o ItemStack usando Heads.getSkull
        ItemStack skull = Heads.getSkull(textureUrl);
        ItemBuilder item = new ItemBuilder(skull);
        List<String> lore = new ArrayList<>();

        // Definir o nome e a lore com base na proteção
        switch (protecao) {
            case Aliada:
            case Inimiga:
            case Neutra:
                item.setName("§" + (protecao == Protecao.Inimiga ? "c" : protecao == Protecao.Neutra ? "f" : "a") +
                             "Terreno da facção [" + terra.getFaction().getTag() + "] " + terra.getFaction().getNome());
                break;
            case Guerra:
                item.setName("§aTerreno da Zona de Guerra");
                break;
            case Protegida:
                item.setName("§aTerreno da Zona Protegida");
                break;
            case Livre:
                item.setName("§aTerreno da Zona Livre");
                if (hasFaction && (isLider || isCapitao || player.getCargo() == Cargo.Membro)) {
                    lore.add("§fBotão esquerdo: §7Dominar");
                    lore.add("§fBotão direito: §7Proteger §f(Custo: " + Config.get("Geral.ProtegerTemporariamentePreco") + ")");
                }
                break;
            case Sua:
                item.setName("§aTerreno da sua facção");
                if (isLider || isCapitao) {
                    lore.add("§fBotão esquerdo: §7Abandonar");
                }
                break;
        }

        if (hasFaction && isLider && protecao != Protecao.Livre) {
            lore.add("§fShift + esquerdo: §7Abandonar todas");
        }

        if (!lore.isEmpty()) {
            item.setLore(lore);
        }

        return item.toItemStack();
    }

    private boolean convidar(NDPlayer inviter, String targetName) {
        Player player = inviter.getPlayer();
        if (!validateFaction(inviter, true) || !hasRequiredCargo(inviter, Cargo.Lider, Cargo.Capitão)) {
            player.sendMessage(Config.get("Mensagens.SemPermissao").toString().replace("&", "§"));
            return false;
        }

        NDFaction faction = inviter.getFaction();
        NDPlayer target = DataManager.players.get(targetName);
        if (target == null) {
            player.sendMessage(Config.get("Mensagens.UsuarioNaoExiste").toString().replace("&", "§"));
            return false;
        }

        if (target.hasFaction()) {
            player.sendMessage(Config.get("Mensagens.JaTemFac").toString()
                    .replace("&", "§")
                    .replace("<jogador>", targetName));
            return false;
        }

        if (faction.getAll().size() >= (int) Config.get("Padrao.MaximoMembrosEmFac") + faction.getExtraMemberSlots()) {
            player.sendMessage(Config.get("Mensagens.MaxMembros").toString().replace("&", "§"));
            return false;
        }

        if (target.getConvites().contains(faction)) {
            player.sendMessage(Config.get("Mensagens.JaConvidou").toString()
                    .replace("&", "§")
                    .replace("<jogador>", targetName));
            return false;
        }

        // Add the invitation with inviter and timestamp
        target.addConvite(faction, inviter.getNome());

        // Notify the inviter
        player.sendMessage(Config.get("Mensagens.ConviteEnviado").toString()
                .replace("&", "§")
                .replace("<jogador>", targetName));

        // Notify the target player if online with clickable JSON message
        Player targetPlayer = target.getPlayer();
        if (targetPlayer != null) {
            String factionTag = faction.getTag();
            String factionName = faction.getNome();
            TextComponent message = new TextComponent("§eVocê foi convidado por §f" + inviter.getNome() + "§e para a facção §f[" + factionTag + "] " + factionName + ".\n ");

            TextComponent acceptButton = new TextComponent("§a[Aceitar]");
            acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f aceitar convite " + factionTag));
            acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClique para aceitar o convite").create()));

            TextComponent denyButton = new TextComponent(" §c[Negar]");
            denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f recusar convite " + factionTag));
            denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cClique para recusar o convite").create()));

            message.addExtra(acceptButton);
            message.addExtra(denyButton);

            targetPlayer.spigot().sendMessage(message);
        }

        return true;
    }

    private boolean aceitar(NDPlayer dplayer, String tag) {
        Player player = dplayer.getPlayer();
        NDFaction faction = Utils.getFactionByTag(tag);
        if (faction == null) {
            player.sendMessage(Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§"));
            return false;
        }

        if (dplayer.hasFaction()) {
            player.sendMessage(Config.get("Mensagens.JaTemFaccao").toString().replace("&", "§"));
            return false;
        }

        boolean hasInvite = dplayer.getConvites().contains(faction);
        boolean isInRoster = faction.isInRoster(dplayer.getNome());
        if (!hasInvite && !isInRoster) {
            player.sendMessage(Config.get("Mensagens.SemConvite").toString()
                    .replace("&", "§")
                    .replace("<tag>", "[" + tag + "] " + faction.getNome()));
            return false;
        }

        // Add player to the faction
        if (!faction.addMember(dplayer)) {
            player.sendMessage("§cNão foi possível entrar na facção: limite de membros atingido ou erro interno.");
            return false;
        }

        // Clear all invitations
        List<NDFaction> convites = new ArrayList<>(dplayer.getConvites());
        for (NDFaction f : convites) {
            dplayer.removeConvite(f);
        }

        // Remove from roster if present
        if (isInRoster) {
            faction.removeFromRoster(dplayer.getNome());
        }

        try {
            faction.save();
            dplayer.save();
        } catch (Exception e) {
            player.sendMessage("§cErro ao salvar dados da facção: " + e.getMessage());
            Bukkit.getLogger().severe("Erro ao salvar aceitação de convite para " + dplayer.getNome() + ": " + e.getMessage());
            return false;
        }

        // Notify the player
        player.sendMessage(Config.get("Mensagens.EntrouFac").toString()
                .replace("&", "§")
                .replace("<tag>", "[" + tag + "] " + faction.getNome()));

        // Notify other online members
        faction.getAllOnline().stream()
                .filter(p -> !p.getName().equals(dplayer.getNome()))
                .forEach(p -> p.sendMessage(Config.get("Mensagens.EntrouNaFac").toString()
                        .replace("&", "§")
                        .replace("<jogador>", dplayer.getNome())));

        // Fire event
        doFactionPlayerChangeFaction event = new doFactionPlayerChangeFaction(dplayer, Motivo.ENTRAR);
        Bukkit.getPluginManager().callEvent(event);

        return true;
    }

    private boolean recusar(NDPlayer dplayer, String tag) {
        Player player = dplayer.getPlayer();
        NDFaction faction = Utils.getFactionByTag(tag);
        if (faction == null) {
            player.sendMessage(Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§"));
            return false;
        }

        if (!dplayer.getConvites().contains(faction)) {
            player.sendMessage(Config.get("Mensagens.SemConvite").toString()
                    .replace("&", "§")
                    .replace("<tag>", "[" + tag + "] " + faction.getNome()));
            return false;
        }

        // Remove the invitation
        dplayer.removeConvite(faction);

        try {
            dplayer.save();
        } catch (Exception e) {
            player.sendMessage("§cErro ao recusar o convite: " + e.getMessage());
            Bukkit.getLogger().severe("Erro ao salvar recusa de convite de " + dplayer.getNome() + ": " + e.getMessage());
            return false;
        }

        // Notify the player
        player.sendMessage("§cVocê recusou o convite da facção [" + tag + "] " + faction.getNome());

        // Notify online faction members
        faction.getAllOnline().forEach(p -> p.sendMessage("§c" + dplayer.getNome() + " recusou o convite para a facção."));

        return true;
    }
}