package me.nd.factions.comandos;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.nd.factions.api.Config;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;

public class Chat implements CommandExecutor{

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg2, String[] args) {
		if (sender instanceof Player) {
			if (cmd.getName().equals(".") || cmd.getName().equals("c")) {
				Player p = (Player)sender;
				NDPlayer player = DataManager.players.get(p.getName());
				if (!player.hasFaction()) {
					player.getPlayer().sendMessage(Config.get("Mensagens.SemFac").toString().replace("&", "§"));
					return false;
				}
				String msg = getStrings(args, 0);
				for (Player all : player.getFaction().getAllOnline()) {
					all.sendMessage("§a"+player.getCargoSimbolo()+"§f"+player.getNome()+": §e"+msg);
				}
			}
			if (cmd.getName().equals("a")) {
				Player p = (Player)sender;
				NDPlayer player = DataManager.players.get(p.getName());
				if (!player.hasFaction()) {
					player.getPlayer().sendMessage(Config.get("Mensagens.SemFac").toString().replace("&", "§"));
					return false;
				}
				String msg = getStrings(args, 0);
				List<Player> online = new ArrayList<>();
				online.addAll(player.getFaction().getAllOnline());
				for (NDFaction f : player.getFaction().getAliados()) {
					online.addAll(f.getAllOnline());
				}
				for (Player all : online) {
					all.sendMessage("§8[§a"+player.getCargoSimbolo()+player.getFaction().getTag()+"§8] §f"+player.getNome()+": §e"+msg);
				}
			}
		}
		return false;
	}

	
	private String getStrings(String[] argumentos, int inicio)
	{
	StringBuilder sb = new StringBuilder();
	for (int i = inicio; i < argumentos.length; i++)
	{
	if (i != inicio) {
	sb.append(" ");
	}
	sb.append(argumentos[i]);
	}
	return sb.toString();
	}
}
