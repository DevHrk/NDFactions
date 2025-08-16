package me.nd.factions.scoreboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.nd.factions.api.Formatter;
import me.nd.factions.api.Vault;
import me.nd.factions.enums.Protecao;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;

public class ScoreUtils {

	public static Integer getPos(NDPlayer cp) {
		HashMap<NDFaction, Double> top = new HashMap<>();
		for (NDFaction fac : DataManager.factions.values()) {
			top.put(fac, fac.getMoneyEmSpawners() + fac.getMoneyTotal());
		}
		int i = 0;
		for (Entry<NDFaction, Double> a : me.nd.factions.utils.Utils.entriesSortedByValues(top)) {
			i++;
			if (cp.getFaction().getNome().equals(a.getKey().getNome())) {
				break;
			}
		}
		return i;
	}

	public static HashSet<UUID> CLAN_SCORE = new HashSet<>();

	private Player p;

	public ScoreUtils(Player p) {
		this.p = p;
	}

	public boolean hasFaction() {
		return DataManager.players.get(p.getName()).hasFaction();
	}

	public String getPlayerPower() {
		return DataManager.players.get(p.getName()).getPoder() + "/"
				+ DataManager.players.get(p.getName()).getPodermax();
	}

	public String getPlayerFactionName() {
		return DataManager.players.get(p.getName()).getFaction().getNome();
	}

	public String getPlayerFactionPlayersOnlines() {
		return DataManager.players.get(p.getName()).getFaction().getAllOnline().size() + "/"
				+ DataManager.players.get(p.getName()).getFaction().getMaxMembers();

	}

	public String getPlayerFactionPower() {
		return DataManager.players.get(p.getName()).getFaction().getPoder() + "/"
				+ DataManager.players.get(p.getName()).getFaction().getPoderMax();
	}

	public String getLocationNameFaction() {
		return Score.getLocationName(p);
	}

	public int getPlayerFactionLands() {
		return DataManager.players.get(p.getName()).getFaction().getTerras().size();
	}

	public String getPlayerCoins() {
		return Formatter.formatNumber(Vault.getPlayerBalance(p));
	}

	public String getAttackSimbol() {
		return "";
	}

    public String getLocationNameFaction(Location from) {
        if (from == null || from.getWorld() == null) {
            return "§7Nenhum";
        }

        String worldName = from.getWorld().getName();

        // Verificar mundos especiais
        if (worldName.equalsIgnoreCase(Score.MINA_WORLD)) {
            return "§7Mundo de Mineração";
        } else if (worldName.equalsIgnoreCase(Score.VIP)) {
            return "§6Mundo VIP";
        }

        // Obter o chunk da localização
        Chunk chunk = from.getChunk();
        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());

        // Determinar o estado de proteção usando o jogador da instância
        Protecao protection = me.nd.factions.utils.Utils.getProtection(chunk, p);

        switch (protection) {
            case Aliada:
                return "§a" + terra.getFaction().getNome();
            case Guerra:
                return "§4Zona de Guerra";
            case Inimiga:
                return "§c" + terra.getFaction().getNome();
            case Livre:
                return "§2Zona Livre";
            case Neutra:
                return "§7" + terra.getFaction().getNome();
            case Protegida:
                return "§6Zona Protegida";
            case Sua:
                return "§f" + terra.getFaction().getNome();
            default:
                return "§7Nenhum";
        }
    }


}
