package me.nd.factions.comandos;

import me.nd.factions.Main;
import me.nd.factions.addons.SobAtaque;
import me.nd.factions.api.TitleAPI;
import me.nd.factions.api.ActionBar;
import me.nd.factions.api.Config;
import me.nd.factions.listeners.FactionGenerators;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.TaxLog;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.utils.ColorData;
import me.nd.factions.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import me.nd.factions.objetos.NDFaction.TaxResult;

public class AdminCommands implements CommandExecutor {

    public static final Map<NDFaction, Long> graceFactions = new ConcurrentHashMap<>();
    public static final Map<NDFaction, Boolean> sobAtaqueDisabled = new ConcurrentHashMap<>();
    public static volatile long globalGraceExpiration = 0L;
    public static final Set<String> adminBypassPlayers = new HashSet<>();
    public static final Map<String, TaxLog> taxLogs = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("sgive")) {
            return handleSGiveCommand(sender, args);
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores.");
            return false;
        }
        Player player = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("grace")) {
            return handleGraceCommand(player, args);
        } else if (cmd.getName().equalsIgnoreCase("togglesobataque")) {
            return handleToggleSobAtaqueCommand(player, args);
        } else if (cmd.getName().equalsIgnoreCase("fs") && args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            return handleFAdminCommand(player);
        } else if (cmd.getName().equalsIgnoreCase("fs") && args.length > 1 && args[0].equalsIgnoreCase("taxar")) {
            return handleTaxarCommand(player, args);
        } else if (cmd.getName().equalsIgnoreCase("fs") && args.length > 1 && args[0].equalsIgnoreCase("devolvertaxa")) {
            return handleDevolverTaxaCommand(player, args);
        }

        return false;
    }

    private boolean handleSGiveCommand(CommandSender player, String[] args) {
        if (!player.hasPermission("nd.admin.sgive")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando.");
            return false;
        }

        if (args.length < 1) {
            player.sendMessage("§cUso: /sgive [<player>] <mob> [<quantia>]");
            return false;
        }

        Player targetPlayer;
        String mobName;
        int amount = 1;

        if (args.length >= 2 && Bukkit.getPlayer(args[0]) != null) {
            targetPlayer = Bukkit.getPlayer(args[0]);
            mobName = args[1].toUpperCase();
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount <= 0) {
                        player.sendMessage("§cA quantidade deve ser maior que 0.");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cQuantidade inválida. Use um número inteiro.");
                    return false;
                }
            }
        } else {
            targetPlayer = (Player) player;
            mobName = args[0].toUpperCase();
            if (args.length >= 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount <= 0) {
                        player.sendMessage("§cA quantidade deve ser maior que 0.");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§cQuantidade inválida. Use um número inteiro.");
                    return false;
                }
            }
        }

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(mobName);
            if (!entityType.isSpawnable() || !entityType.isAlive() || !FactionGenerators.GENERATOR_NAMES.containsKey(entityType)) {
                player.sendMessage("§cO mob §e" + mobName + " §cnão pode ser usado em spawners.");
                return false;
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cMob inválido: §e" + mobName + "§c. Use um nome válido (ex.: ZOMBIE, SKELETON).");
            return false;
        }

        // Create spawner
        ItemStack spawner = new ItemStack(Material.SPAWNER, amount);
        BlockStateMeta bsm = (BlockStateMeta) spawner.getItemMeta();
        CreatureSpawner cs = (CreatureSpawner) bsm.getBlockState();
        cs.setSpawnedType(entityType);
        bsm.setBlockState(cs);
        String mobNameFormatted = FactionGenerators.GENERATOR_NAMES.getOrDefault(entityType, entityType.name());
        bsm.setDisplayName("§eSpawner de " + mobNameFormatted);
        bsm.setLore(Collections.singletonList("§7Mob: §e" + mobNameFormatted));
        spawner.setItemMeta(bsm);

        // Add to inventory or drop
        HashMap<Integer, ItemStack> remainingItems = targetPlayer.getInventory().addItem(spawner);
        if (!remainingItems.isEmpty()) {
            for (ItemStack item : remainingItems.values()) {
                targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), item);
            }
            player.sendMessage("§cInventário cheio! Alguns spawners foram dropados no chão para §e" + targetPlayer.getName() + "§c.");
        }

        String mobFormatted = mobNameFormatted.substring(0, 1).toUpperCase() + mobNameFormatted.substring(1).toLowerCase();
        player.sendMessage("§aVocê deu §e" + amount + " §aspawner(s) de §e" + mobFormatted + " §apara §e" + targetPlayer.getName() + "§a.");
        if (!targetPlayer.equals(player)) {
        }

        return true;
    }


    private boolean handleGraceCommand(Player player, String[] args) {
        // Sem argumentos: mostra tempo da própria facção
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("tempo"))) {
            NDFaction playerFaction = DataManager.players.get(player.getName()).getFaction();
            if (playerFaction != null && graceFactions.containsKey(playerFaction)) {
                long remainingMillis = graceFactions.get(playerFaction) - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    String time = formatTime(remainingMillis / 1000);
                    player.sendMessage("§aGrace restante para [" + playerFaction.getTag() + "]: §e" + time);
                    return true;
                }
            }
            if (globalGraceExpiration > System.currentTimeMillis()) {
                long remainingMillis = globalGraceExpiration - System.currentTimeMillis();
                String time = formatTime(remainingMillis / 1000);
                player.sendMessage("§aGrace restante: §e" + time);
                return true;
            }
            player.sendMessage("§cO grace não está ativo.");
            return true;
        }

        // /grace tempo <tag> → apenas para admins
        if (args.length == 2 && args[0].equalsIgnoreCase("tempo")) {
            if (!player.hasPermission("nd.admin.grace")) {
                player.sendMessage("§cVocê não tem permissão para ver o grace de outras facções.");
                return false;
            }

            NDFaction faction = Utils.getFactionByTag(args[1].toUpperCase());
            if (faction == null) {
                player.sendMessage("§cFacção não encontrada.");
                return false;
            }
            if (graceFactions.containsKey(faction)) {
                long remainingMillis = graceFactions.get(faction) - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    String time = formatTime(remainingMillis / 1000);
                    player.sendMessage("§aGrace restante: §e" + time);
                    return true;
                }
            }
            player.sendMessage("§cGrace não está ativo para [" + faction.getTag() + "].");
            return true;
        }

        // A partir daqui, exige permissão para ativar
        if (!player.hasPermission("nd.admin.grace")) {
            // Simula /grace tempo padrão
            NDFaction playerFaction = DataManager.players.get(player.getName()).getFaction();
            if (playerFaction != null && graceFactions.containsKey(playerFaction)) {
                long remainingMillis = graceFactions.get(playerFaction) - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    String time = formatTime(remainingMillis / 1000);
                    player.sendMessage("§aGrace restante para [" + playerFaction.getTag() + "]: §e" + time);
                    return true;
                }
            }
            if (globalGraceExpiration > System.currentTimeMillis()) {
                long remainingMillis = globalGraceExpiration - System.currentTimeMillis();
                String time = formatTime(remainingMillis / 1000);
                player.sendMessage("§aGrace restante: §e" + time);
                return true;
            }
            player.sendMessage("§cO grace não está ativo.");
            return true;
        }
        
        // /grace remover <tag|global>
        if (args.length >= 1 && args[0].equalsIgnoreCase("remover")) {
            if (args.length != 2) {
                player.sendMessage("§cUso correto: /grace remover <tag|global>");
                return false;
            }

            if (!player.hasPermission("nd.admin.grace")) {
                player.sendMessage("§cVocê não tem permissão para remover grace.");
                return false;
            }

            if (args[1].equalsIgnoreCase("global")) {
                globalGraceExpiration = 0L;
                graceFactions.clear();
                Bukkit.broadcastMessage("§cO periodo de graça foi removido por um admin.");
                return true;
            }

            NDFaction faction = Utils.getFactionByTag(args[1].toUpperCase());
            if (faction == null) {
                player.sendMessage("§cFacção não encontrada.");
                return false;
            }

            if (graceFactions.remove(faction) != null) {
                faction.broadcast("§cO grace da facção foi removido pelo administrador.");
                player.sendMessage("§aGrace removido da facção [" + faction.getTag() + "].");
            } else {
                player.sendMessage("§cEssa facção não está com grace ativo.");
            }
            return true;
        }

        // /grace <tag> → ativa com duração padrão (24 horas)
        // /grace <tag|global> <horas> → ativa com duração especificada
        if (args.length < 1 || args.length > 2) {
            player.sendMessage("§cUso: /grace <tag> [horas] ou /grace global <horas>");
            return false;
        }

        int hours;
        boolean isDefaultDuration = args.length == 1; // /grace <tag> usa duração padrão
        if (isDefaultDuration) {
            hours = 24; // Duração padrão de 24 horas
        } else {
            try {
                hours = Integer.parseInt(args[1]);
                if (hours <= 0) {
                    player.sendMessage("§cO tempo deve ser maior que 0 horas.");
                    return false;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cTempo inválido. Use um número de horas.");
                return false;
            }
        }

        long expireTime = System.currentTimeMillis() + (hours * 60 * 60 * 1000L);
        String titleMessage = Config.get("Mensagens.GraceAtivado").toString()
                .replace("&", "§")
                .replace("<horas>", String.valueOf(hours));
        String subtitleMessage = "§eProteção ativada por " + hours + " horas!";

        if (args[0].equalsIgnoreCase("global") || args[0].equalsIgnoreCase("all")) {
            if (isDefaultDuration) {
                player.sendMessage("§cPara grace global, especifique o número de horas: /grace global <horas>");
                return false;
            }
            globalGraceExpiration = expireTime;
            for (NDFaction faction : DataManager.factions.values()) {
                graceFactions.put(faction, expireTime);
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                TitleAPI.sendTitle(p, 20, 60, 20, titleMessage, subtitleMessage);
            }

            player.sendMessage("§aPeríodo de grace ativado por " + hours + " horas.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    globalGraceExpiration = 0L;
                    String expireTitle = Config.get("Mensagens.GraceGlobalExpirado").toString().replace("&", "§");
                    String expireSubtitle = "§cProteção global encerrada!";
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        TitleAPI.sendTitle(p, 20, 60, 20, expireTitle, expireSubtitle);
                    }
                    graceFactions.clear();
                }
            }.runTaskLater(Main.get(), hours * 3600 * 20L);

            return true;
        } else {
            NDFaction faction = Utils.getFactionByTag(args[0].toUpperCase());
            if (faction == null) {
                player.sendMessage("§cFacção não encontrada.");
                return false;
            }

            // Verifica se a facção já está em grace
            if (graceFactions.containsKey(faction)) {
                long remainingMillis = graceFactions.get(faction) - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    String time = formatTime(remainingMillis / 1000);
                    player.sendMessage("§cA facção [" + faction.getTag() + "] já está em grace. Tempo restante: §e" + time);
                    player.sendMessage("§cUse /grace remover " + faction.getTag() + " para remover o grace atual.");
                    return false;
                }
            }

            graceFactions.put(faction, expireTime);
            for (Player p : faction.getAllOnline()) {
                TitleAPI.sendTitle(p, 20, 60, 20, titleMessage, subtitleMessage);
            }
            faction.broadcast(titleMessage);
            player.sendMessage("§aPeríodo de grace ativado para [" + faction.getTag() + "] por " + hours + " horas.");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (graceFactions.remove(faction) != null) {
                        String expireTitle = Config.get("Mensagens.GraceExpirado").toString().replace("&", "§");
                        String expireSubtitle = "§cProteção da facção encerrada!";
                        for (Player p : faction.getAllOnline()) {
                            TitleAPI.sendTitle(p, 20, 60, 20, expireTitle, expireSubtitle);
                        }
                        faction.broadcast(expireTitle);
                        Bukkit.getLogger().info("Grace period expired for faction [" + faction.getTag() + "].");
                    }
                }
            }.runTaskLater(Main.get(), hours * 3600 * 20L);

            return true;
        }
    }


    private boolean handleFAdminCommand(Player player) {
        if (!player.hasPermission("nd.admin.fadmin")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando.");
            return false;
        }

        String playerName = player.getName();
        if (adminBypassPlayers.contains(playerName)) {
            adminBypassPlayers.remove(playerName);
            player.sendMessage("§cModo admin desativado.");
        } else {
            adminBypassPlayers.add(playerName);
            player.sendMessage("§aModo admin ativado.");
        }

        return true;
    }

    private boolean handleToggleSobAtaqueCommand(Player player, String[] args) {
        if (!player.hasPermission("nd.admin.togglesobataque")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando.");
            return false;
        }

        if (args.length < 1) {
            player.sendMessage("§cUso: /togglesobataque <tag>");
            return false;
        }

        NDFaction faction = Utils.getFactionByTag(args[0].toUpperCase());
        if (faction == null) {
            player.sendMessage("§cFacção não encontrada.");
            return false;
        }

        // Verifica se a facção possui território
        if (faction.getTerras().isEmpty()) {
            player.sendMessage("§cEssa facção não possui territórios para ser colocada sob ataque.");
            return false;
        }

        boolean isUnderAttack = SobAtaque.cooldown.contains(faction);

        if (isUnderAttack) {
            // Remove sob ataque manualmente
            SobAtaque.cooldown.remove(faction);
            SobAtaque.attackStartTimes.remove(faction);
            SobAtaque.lastExplosionTimes.remove(faction);
            SobAtaque.fastUpdatePlayers.removeAll(faction.getAllOnline());

            // Clear action bar for online players
            for (Player p : faction.getAllOnline()) {
                ActionBar.sendActionBarMessage(p, "");
            }

            player.sendMessage("§aFacção §e[" + faction.getTag() + "]§a não está mais sob ataque.");
        } else {
            // Inicia sob ataque manualmente
            long now = System.currentTimeMillis();
            SobAtaque.cooldown.add(faction);
            SobAtaque.attackStartTimes.put(faction, now);
            SobAtaque.lastExplosionTimes.put(faction, now); // Align with explosion reset logic
            SobAtaque.fastUpdatePlayers.addAll(faction.getAllOnline());

            // Start action bar updates
            ColorData data = new ColorData(Config.get("SobAtaque.Mensagem").toString().replace("&", "§"), "§4", "§c");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!SobAtaque.cooldown.contains(faction)) {
                        cancel();
                        return;
                    }

                    long start = SobAtaque.attackStartTimes.getOrDefault(faction, now);
                    long last = SobAtaque.lastExplosionTimes.getOrDefault(faction, now);
                    long timeNow = System.currentTimeMillis();

                    // Verifica se passou o tempo total de ataque (2 horas)
                    if (timeNow - start >= SobAtaque.MAX_ATTACK_DURATION) {
                        SobAtaque.encerrarAtaque(faction, true);
                        cancel();
                        return;
                    }

                    // Verifica se passou o tempo sem explosões (5 minutos)
                    if (timeNow - last >= SobAtaque.RESET_TIMER_ON_EXPLOSION) {
                        SobAtaque.encerrarAtaque(faction, false);
                        cancel();
                        return;
                    }

                    data.next();
                    String msg = data.getMessage();
                    for (Player p : faction.getAllOnline()) {
                        ActionBar.sendActionBarMessage(p, msg);
                    }
                }
            }.runTaskTimerAsynchronously(Main.getPlugin(Main.class), 0L, 5L);

            player.sendMessage("§aFacção §e[" + faction.getTag() + "]§a foi colocada sob ataque.");
        }

        return true;
    }

    	private boolean handleTaxarCommand(Player player, String[] args) {
    	    if (!player.hasPermission("nd.admin.taxar")) {
    	        player.sendMessage("§cVocê não tem permissão para usar este comando.");
    	        return false;
    	    }

    	    if (args.length < 3) {
    	        player.sendMessage("§cUso correto: §e/fs taxar <tag> <porcentagem>");
    	        return false;
    	    }

    	    NDFaction faction = Utils.getFactionByTag(args[1].toUpperCase());
    	    if (faction == null) {
    	        player.sendMessage("§cFacção §e" + args[1] + " §cnão encontrada.");
    	        return false;
    	    }

    	    double percentage;
    	    try {
    	        percentage = Double.parseDouble(args[2]);
    	        if (percentage <= 0 || percentage > 100) {
    	            player.sendMessage("§cA porcentagem deve estar entre §e0 e 100§c.");
    	            return false;
    	        }
    	    } catch (NumberFormatException e) {
    	        player.sendMessage("§cPorcentagem inválida. Use um número como §e25§c para representar 25%.");
    	        return false;
    	    }

    	    TaxResult result = faction.applyTax(percentage);
    	    String tag = faction.getTag() != null ? faction.getTag() : faction.getNome();

    	    if (result.isSuccess()) {
    	        TaxLog taxLog = new TaxLog(faction, percentage, result.getMoneyDeducted(), result.getSpawnersDeducted());
    	        taxLogs.put(taxLog.getId(), taxLog);

    	        faction.broadcast(Config.get("Mensagens.FaccaoTaxada").toString()
    	            .replace("&", "§")
    	            .replace("<porcentagem>", String.format("%.2f", percentage))
    	            .replace("<tag>", tag));

    	        player.sendMessage("§aFacção §e[" + tag + "] §ataxada em §e" + String.format("%.2f", percentage) + "%§a com sucesso.");
    	        player.sendMessage("§7ID da taxação: §f" + taxLog.getId());

    	    } else {
    	        player.sendMessage("§cNão foi possível taxar a facção §e[" + tag + "]§c. Saldo insuficiente ou erro interno.");
    	    }

    	    return true;
    	}


    	private boolean handleDevolverTaxaCommand(Player player, String[] args) {
    	    if (!player.hasPermission("nd.admin.devolvertaxa")) {
    	        player.sendMessage("§cVocê não tem permissão para usar este comando.");
    	        return false;
    	    }

    	    if (args.length < 2) {
    	        player.sendMessage("§cUso correto: §e/fs devolvertaxa <ID>");
    	        return false;
    	    }

    	    String taxId = args[1];
    	    TaxLog taxLog = taxLogs.get(taxId);

    	    if (taxLog == null) {
    	        player.sendMessage("§cID de taxação §e" + taxId + " §cnão encontrado.");
    	        return false;
    	    }

    	    NDFaction faction = taxLog.getFaction();
    	    if (faction == null || DataManager.factions.get(faction.getNome()) == null) {
    	        player.sendMessage("§cA facção associada ao ID §e" + taxId + " §cnão existe mais.");
    	        return false;
    	    }

    	    // Refund money
    	    faction.setBanco(faction.getBanco() + taxLog.getMoneyDeducted());

    	    // Refund spawners
    	    for (Map.Entry<EntityType, Integer> entry : taxLog.getSpawnersDeducted().entrySet()) {
    	        faction.addStoredGenerator(entry.getKey(), entry.getValue(), null);
    	    }

    	    try {
    	        faction.save();
    	        faction.updateSpawners();
    	    } catch (Exception e) {
    	        String tag = faction.getTag() != null ? faction.getTag() : faction.getNome();
    	        player.sendMessage("§cErro ao salvar alterações da facção §e[" + tag + "]§c após devolução da taxa.");
    	        Bukkit.getLogger().severe("Erro ao salvar facção [" + tag + "] após devolução da taxa (ID: " + taxId + "): " + e.getMessage());
    	        return false;
    	    }

    	    taxLogs.remove(taxId);

    	    String tag = faction.getTag() != null ? faction.getTag() : faction.getNome();
    	    faction.broadcast(Config.get("Mensagens.TaxaDevolvida").toString()
    	        .replace("&", "§")
    	        .replace("<porcentagem>", String.format("%.2f", taxLog.getPercentage()))
    	        .replace("<tag>", tag));

    	    player.sendMessage("§aTaxa devolvida com sucesso para a facção §e[" + tag + "]§a.");
    	    player.sendMessage("§7ID da devolução: §f" + taxId);

    	    return true;
    	}


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
}