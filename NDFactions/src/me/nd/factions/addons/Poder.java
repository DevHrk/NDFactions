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

import me.nd.factions.api.Config;
import me.nd.factions.factions.API;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.utils.ItemBuilder;

public class Poder implements CommandExecutor, Listener {

	private static final String PODER_NOME = "§6+1 de Poder";
	private static final String PODER_MAX_NOME = "§6Poder Máximo";

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("cpower")) return false;
		if (!sender.hasPermission("nd.commands.factions.cpower")) return false;

		if (args.length < 3) {
			sender.sendMessage("§cUtilize /cpower <normal/max> <jogador> <quantia>");
			return true;
		}

		String tipo = args[0].toLowerCase();
		if (!tipo.equals("normal") && !tipo.equals("max")) {
			sender.sendMessage("§cUtilize /cpower <normal/max> <jogador> <quantia>");
			return true;
		}

		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			sender.sendMessage("§cNenhum jogador com este nick online.");
			return true;
		}

		int quantia;
		try {
			quantia = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			sender.sendMessage("§cQuantidade inválida.");
			return true;
		}

		ItemBuilder builder = new ItemBuilder(Material.NETHER_STAR)
				.setAmount(quantia);

		if (tipo.equals("normal")) {
			builder.setName(PODER_NOME)
			       .setLore("§fAtivando este item você", "§fganhar 1 ponto de poder");
		} else {
			builder.setName(PODER_MAX_NOME)
			       .setLore("§fAtivando este item você aumenta", "§fum ponto em seu limite de poder.", "","§7* §fLimite máximo de poder: 6");
		}

		target.getInventory().addItem(builder.toItemStack());
		sender.sendMessage("§aItem entregue para " + target.getName() + ".");
		return true;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		ItemStack item = e.getItem();
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

		String display = item.getItemMeta().getDisplayName();
		Player player = e.getPlayer();
		NDPlayer ndPlayer = API.getPlayer(player.getName());

		if (display.equals(PODER_NOME)) {
			if (ndPlayer.getPoder() >= ndPlayer.getPodermax()) {
				player.sendMessage("§cVocê atingiu o poder máximo.");
				return;
			}
			ndPlayer.setPoder(ndPlayer.getPoder() + 1);
			player.sendMessage("§eVocê recebeu +1 de poder.");
			consumirItem(player);
		}
		else if (display.equals(PODER_MAX_NOME)) {
			int limite = (int) Config.get("Padrao.PoderMaxLimite");
			if (ndPlayer.getPodermax() >= limite) {
				player.sendMessage("§cVocê atingiu o limite máximo.");
				return;
			}
			ndPlayer.setPodermax(ndPlayer.getPodermax() + 1);
			player.sendMessage("§eVocê ganhou +1 de poder máximo.");
			consumirItem(player);
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
