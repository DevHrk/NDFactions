package me.nd.factions.scoreboard;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import me.nd.factions.Main;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.Utils;

public class Score {
	
	public static me.nd.factions.api.Vault Vault;

	public static String MINA_WORLD = "";
	public static String NORMAL_WORLD = "";
	public static String VIP = "";
	public static String URL = "";

	public static void register() {
		MINA_WORLD = Main.get().getConfig().getString("Mundos.Mineracao");
		NORMAL_WORLD = Main.get().getConfig().getString("Mundos.Padrao");
		;
		VIP = Main.get().getConfig().getString("Mundos.VIP");
		URL = Main.get().getConfig().getString("URL");
		Vault = new me.nd.factions.api.Vault();
		// setPlayerPoints();
	}

	public static String getLocationName(Player p) {
		if (p.getWorld().getName().equalsIgnoreCase(MINA_WORLD))
			return " §7Mundo de Mineração";
		else if (p.getWorld().getName().equalsIgnoreCase(VIP))
			return " §6Mundo VIP";
		else {
			String fac = "";
			Chunk c = p.getLocation().getChunk();
			Terra terra = new Terra(c.getWorld(), c.getX(), c.getZ());
			switch (Utils.getProtection(p.getLocation().getChunk(), p)) {
			case Aliada:
				fac = "§a" + terra.getFaction().getNome();
				break;
			case Guerra:
				fac = "§4Zona de Guerra";
				break;
			case Inimiga:
				fac = "§c" + terra.getFaction().getNome();
				break;
			case Livre:
				fac = "§2Zona Livre";
				break;
			case Neutra:
				fac = "§7" + terra.getFaction().getNome();
				break;
			case Protegida:
				fac = "§6Zona Protegida";
				break;
			case Sua:
				fac = "§f" + terra.getFaction().getNome();
				break;
			}
			return fac;

		}
	}


}
