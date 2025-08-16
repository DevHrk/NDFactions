package me.nd.factions.comandos;

import me.nd.factions.Main;
import me.nd.factions.addons.SobAtaque;
import me.nd.factions.api.Vault;
import me.nd.factions.factions.API;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tregua implements CommandExecutor {

    private static final Map<UUID, TruceRequest> pendingTruceRequests = new ConcurrentHashMap<>();
    private static final long TRUCE_REQUEST_TIMEOUT = 2 * 60 * 1000L; // 2 minutos para aceitar
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+\\.?\\d*)([kKmMtT]?)");

    public static class TruceRequest {
        private final NDFaction defender;
        private final NDFaction attacker;
        private final long timestamp;
        private final double cost;
        private final long duration;
        private final String defenderLeader;

        public TruceRequest(NDFaction defender, NDFaction attacker, double cost, long duration, String defenderLeader) {
            this.defender = defender;
            this.attacker = attacker;
            this.timestamp = System.currentTimeMillis();
            this.cost = cost;
            this.duration = duration;
            this.defenderLeader = defenderLeader;
        }

        public NDFaction getDefender() { return defender; }
        public NDFaction getAttacker() { return attacker; }
        public long getTimestamp() { return timestamp; }
        public double getCost() { return cost; }
        public long getDuration() { return duration; }
        public String getDefenderLeader() { return defenderLeader; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;
        NDPlayer ndPlayer = API.getPlayer(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) {
            player.sendMessage("§cVocê não está em uma facção!");
            return true;
        }

        NDFaction faction = ndPlayer.getFaction();
        if (!faction.getLiderName().equals(player.getName())) {
            player.sendMessage("§cApenas o líder da facção pode propor ou aceitar uma trégua!");
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("aceitar")) {
                // Aceitar trégua
                acceptTruce(player);
            } else {
                // Propor trégua com quantia (exige que a facção esteja sob ataque)
                if (!faction.isSobAtaque()) {
                    player.sendMessage("§cSua facção não está sob ataque!");
                    return true;
                }
                proposeTruce(player, faction, args[0]);
            }
        } else {
            player.sendMessage("§cUso: /tregua <quantia> | /tregua aceitar");
            return true;
        }

        return true;
    }

    private double parseAmount(String input) {
        try {
            Matcher matcher = AMOUNT_PATTERN.matcher(input.toLowerCase());
            if (!matcher.matches()) {
                return -1;
            }

            double value = Double.parseDouble(matcher.group(1));
            String suffix = matcher.group(2);

            switch (suffix.toLowerCase()) {
                case "k":
                    return value * 1_000;
                case "m":
                    return value * 1_000_000;
                case "t":
                    return value * 1_000_000_000;
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void proposeTruce(Player player, NDFaction defender, String amountInput) {
        // Parsear a quantia
        double truceCost = parseAmount(amountInput);
        if (truceCost <= 0) {
            player.sendMessage("§cQuantia inválida! Use um número (ex.: 1000, 50k, 10m, 50t).");
            return;
        }

        // Obter a facção atacante do attacks.yml
        String attackerName = DataManager.loadAttacker(defender.getNome());
        if (attackerName == null) {
            player.sendMessage("§cNão foi possível identificar a facção atacante!");
            return;
        } 
        
        NDFaction attacker = DataManager.factions.get(attackerName);
        if (attacker == null) {
            player.sendMessage("§cFacção atacante '" + attackerName + "' não encontrada!");
            return;
        }

        // Verificar se já existe um pedido de trégua pendente
        for (TruceRequest request : pendingTruceRequests.values()) {
            if (request.getDefender().equals(defender) && request.getAttacker().equals(attacker)) {
                player.sendMessage("§cJá existe um pedido de trégua pendente para esta facção!");
                return;
            }
        }

        // Obter configurações da trégua
        long truceDuration = Main.get().getConfig().getInt("Tregua.Duracao");

        // Verificar se o líder da facção defensora tem saldo suficiente
        double leaderBalance = Vault.getPlayerBalance(player.getName());
        if (leaderBalance < truceCost) {
            player.sendMessage("§cVocê não tem saldo suficiente (necessário: $" + truceCost + ")!");
            return;
        }

        // Criar pedido de trégua
        TruceRequest request = new TruceRequest(defender, attacker, truceCost, truceDuration, player.getName());
        pendingTruceRequests.put(UUID.randomUUID(), request);

        // Notificar o líder da facção atacante
        Player attackerLeader = Bukkit.getPlayer(attacker.getLiderName());
        if (attackerLeader != null && attackerLeader.isOnline()) {
            attackerLeader.sendMessage("§eA facção §f[" + defender.getTag() + "] §epropôs uma trégua!");
            attackerLeader.sendMessage("§eCusto: §f$" + truceCost + " §e| Duração: §f" + (truceDuration / 60000) + " minutos");
            attackerLeader.sendMessage("§eUse §f/tregua aceitar §epara aceitar em até 2 minutos!");
        } else {
            player.sendMessage("§cO líder da facção atacante não está online!");
            pendingTruceRequests.values().removeIf(r -> r.getDefender().equals(defender));
            return;
        }

        // Notificar a facção defensora
        defender.broadcast("§eO líder §f" + player.getName() + " §epropôs uma trégua à facção §f[" + attacker.getTag() + "] por §f$" + truceCost + "!");

        // Agendar expiração do pedido
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingTruceRequests.values().removeIf(r -> r.getDefender().equals(defender) && r.getTimestamp() + TRUCE_REQUEST_TIMEOUT < System.currentTimeMillis())) {
                    defender.broadcast("§cO pedido de trégua para §f[" + attacker.getTag() + "] §cexpira!");
                    if (attackerLeader != null && attackerLeader.isOnline()) {
                        attackerLeader.sendMessage("§cO pedido de trégua de §f[" + defender.getTag() + "] §cexpira!");
                    }
                }
            }
        }.runTaskLater(Main.getPlugin(Main.class), TRUCE_REQUEST_TIMEOUT / 50L); // Converte ms para ticks
    }

    private void acceptTruce(Player player) {
        NDPlayer ndPlayer = API.getPlayer(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) {
            player.sendMessage("§cVocê não está em uma facção!");
            return;
        }

        NDFaction attacker = ndPlayer.getFaction();
        if (!attacker.getLiderName().equals(player.getName())) {
            player.sendMessage("§cApenas o líder da facção pode aceitar uma trégua!");
            return;
        }

        TruceRequest request = null;
        for (TruceRequest r : pendingTruceRequests.values()) {
            if (r.getAttacker().equals(attacker)) {
                request = r;
                break;
            }
        }

        if (request == null) {
            player.sendMessage("§cNenhum pedido de trégua pendente para sua facção!");
            return;
        }

        NDFaction defender = request.getDefender();
        double cost = request.getCost();
        long duration = request.getDuration();
        String defenderLeaderName = request.getDefenderLeader();
        if (defenderLeaderName == null) {
            player.sendMessage("§cErro: Não foi possível identificar o líder da facção defensora!");
            pendingTruceRequests.values().removeIf(r -> r.getDefender().equals(defender));
            return;
        }

        // Cobrar o custo da trégua do líder da facção defensora
        double defenderLeaderBalance = Vault.getPlayerBalance(defenderLeaderName);
        if (defenderLeaderBalance < cost) {
            player.sendMessage("§cO líder da facção defensora não tem mais saldo suficiente!");
            pendingTruceRequests.values().removeIf(r -> r.getDefender().equals(defender));
            return;
        }

        // Efetuar a transação
        Main.get().getEconomy().withdrawPlayer(defenderLeaderName, cost);
        Main.get().getEconomy().depositPlayer(player.getName(), cost);

        // Ativar trégua
        defender.activateTruce(attacker, duration);
        attacker.activateTruce(defender, duration);

        // Remover pedido de trégua
        pendingTruceRequests.values().removeIf(r -> r.getDefender().equals(defender));

        // Notificar ambas as facções
        defender.broadcast("§aTrégua aceita com §f[" + attacker.getTag() + "] §apor " + (duration / 60000) + " minutos!");
        attacker.broadcast("§aTrégua aceita com §f[" + defender.getTag() + "] §apor " + (duration / 60000) + " minutos!");
        Bukkit.broadcastMessage("§7 * §aTrégua estabelecida entre §f[" + defender.getTag() + "] §ae §f[" + attacker.getTag() + "] §apor " + (duration / 60000) + " minutos!");

        // Pausar estado de ataque, se aplicável
        if (SobAtaque.cooldown.contains(defender)) {
            SobAtaque.pauseAttack(defender, attacker, duration);
        }
    }

    public static boolean isTruceActive(NDFaction faction1, NDFaction faction2) {
        return faction1.hasTruceWith(faction2) && faction2.hasTruceWith(faction1);
    }
}