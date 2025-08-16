package me.nd.factions.addons;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.nd.factions.factions.API;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.enums.Cargo;
import me.nd.factions.utils.ItemBuilder;

public class MembroPlus implements CommandExecutor, Listener {

    private static final String MEMBRO_PLUS_NOME = "§6+Membro";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("cmembroplus")) return false;
        if (!sender.hasPermission("nd.commands.factions.cmembroplus")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUtilize /cmembroplus <jogador> <quantia>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cNenhum jogador com este nick online.");
            return true;
        }

        int quantia;
        try {
            quantia = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cQuantidade inválida.");
            return true;
        }

        if (quantia <= 0) {
            sender.sendMessage("§cA quantidade deve ser maior que zero.");
            return true;
        }

        ItemBuilder builder = new ItemBuilder(Material.NETHER_STAR)
                .setAmount(quantia)
                .setName(MEMBRO_PLUS_NOME)
                .setLore("§7Ativando este item você aumenta um", "§7mebro no limite de membros da facção.");

        target.getInventory().addItem(builder.toItemStack());
        sender.sendMessage("§aItem Membro+ entregue para " + target.getName() + ".");
        target.sendMessage("§aVocê recebeu " + quantia + " Membro+ no seu inventário.");
        return true;
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String display = item.getItemMeta().getDisplayName();
        if (!display.equals(MEMBRO_PLUS_NOME)) return;

        Player player = e.getPlayer();
        NDPlayer ndPlayer = API.getPlayer(player.getName());
        NDFaction faction = ndPlayer.getFaction();

        if (faction == null) {
            player.sendMessage("§cVocê precisa estar em uma facção para usar este item.");
            return;
        }

        if (ndPlayer.getCargo() != Cargo.Lider) {
            player.sendMessage("§cVocê precisa ser Líder para usar este item.");
            return;
        }

        // Verificar se o limite de slots extras foi atingido
        if (faction.getExtraMemberSlots() >= 5) {
            player.sendMessage("§cSua facção já atingiu o limite máximo de 5 slots extras de membros (" + faction.getExtraMemberSlots() + "/5).");
            return;
        }

        // Adicionar slot extra e verificar se o salvamento foi bem-sucedido
        if (faction.addExtraMemberSlot()) {
            player.sendMessage("§eA capacidade de membros da facção foi aumentada em 1!");
            player.sendMessage("§eNovo limite: " + faction.getMaxMembers() + " (" + faction.getExtraMemberSlots() + "/5 slots extras).");
            consumirItem(player);
        } else {
            player.sendMessage("§cErro ao aumentar o limite de membros. Tente novamente ou contate um administrador.");
        }
    }

    private void consumirItem(Player player) {
        ItemStack inHand = player.getItemInHand();
        int amount = inHand.getAmount();
        if (amount > 1) {
            inHand.setAmount(amount - 1);
        } else {
            player.setItemInHand(new ItemStack(Material.AIR));
        }
    }
}