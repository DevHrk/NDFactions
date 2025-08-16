package me.nd.factions.chats;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatStaff implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			Player p = (Player) sender;
			
			if (!p.hasPermission("nd.grupos.staff")) {
				p.sendMessage("§cVocê precisa ser Ajudante ou superior para executar este comando.");
				return false;
			}
			
			if (args.length == 0) {
				p.sendMessage("§cUtilize /s <mensagem>");
				return false;
			}
			
			if (args.length > 0) {
				String msg = "";
				for (String s : args) {
					msg = msg + s + " ";
				}
				
				for (Player t : Bukkit.getOnlinePlayers()) {
					if (t.hasPermission("nd.grupos.staff")) {
						t.sendMessage("§d[STAFF] " + p.getName() + ":§f " + msg);
					}
				}
			}
		}
		return false;
	}

}
