package me.nd.factions.api;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nd.factions.Main;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.utils.StringUtils;

public class FExpansion extends PlaceholderExpansion {

	@Override
	public boolean canRegister() {
		return true;
	}
	
	@Override
	public String getAuthor() {
		return "NinjaDark99";
	}

	@Override
	public String getIdentifier() {
		return "nd";
	}

	@Override
	public String getVersion() {
		return Main.get().getDescription().getVersion();
	}
	
	@Override
	public String onPlaceholderRequest(Player player, String params) {
	    if (player == null) {
	        return "";
	    }

	    NDPlayer profile = DataManager.players.get(player.getName());
	    if (profile == null) {
	        return "";
	    }

	    if (params.startsWith("faction_")) {
	        return handleFactionPlaceholder(profile, params);
	    }

	    return null;
	}

	private String handleFactionPlaceholder(NDPlayer profile, String params) {
	    String value = params.replace("faction_", "").toLowerCase();
	    NDFaction faction = profile.getFaction();
	    // Placeholders do NDPlayer
	    switch (value) {
	        case "player_name":
	            return profile.getNome();
	        case "faction_name":
	            return faction != null ? faction.getNome() : "";
	        case "kills":
	            return StringUtils.formatNumber(profile.getKills());
	        case "deaths":
	            return StringUtils.formatNumber(profile.getMortes());
	        case "kdr":
	            return String.format("%.2f", profile.getKDR());
	        case "power":
	            return StringUtils.formatNumber(profile.getPoder());
	        case "max_power":
	            return StringUtils.formatNumber(profile.getPodermax());
	        case "last_online":
	            return profile.getLast();
	        case "role":
	            return profile.getCargo() != null ? profile.getCargo().toString() : "";
	        case "role_symbol":
	            return profile.getCargoSimbolo();
	        case "map_enabled":
	            return profile.isMapaLigado() ? "Sim" : "Não";
	        case "view_territories":
	            return profile.isVerTerras() ? "Sim" : "Não";
	        case "invites_count":
	            return StringUtils.formatNumber(profile.getConvites().size());
	    }

	    // Para o placeholder "tag", tratar explicitamente o caso de facção nula
	    if (value.equals("tag")) {
	        return faction != null ? "["+profile.getCargoSimbolo() + faction.getTag() + "]" : "";
	    }
	    if (value.equals("tagtab")) {
	        return faction != null ? "[" + faction.getTag() + "]" : "";
	    }
	    // Outros placeholders do NDFaction (requer que o jogador tenha uma facção)
	    if (faction == null) {
	        return "";
	    }

	    switch (value) {
	        case "name":
	            return faction.getNome();
	        case "bank":
	            return StringUtils.formatNumber(faction.getBanco());
	        case "base_x":
	            return faction.getBase() != null ? String.valueOf(faction.getBase().getBlockX()) : "N/A";
	        case "base_y":
	            return faction.getBase() != null ? String.valueOf(faction.getBase().getBlockY()) : "N/A";
	        case "base_z":
	            return faction.getBase() != null ? String.valueOf(faction.getBase().getBlockZ()) : "N/A";
	        case "base_world":
	            return faction.getBase() != null ? faction.getBase().getWorld().getName() : "N/A";
	        case "allies_count":
	            return StringUtils.formatNumber(faction.getAliados().size());
	        case "enemies_count":
	            return StringUtils.formatNumber(faction.getInimigos().size());
	        case "members_count":
	            return StringUtils.formatNumber(faction.getAll().size());
	        case "online_members_count":
	            return StringUtils.formatNumber(faction.getAllOnline().size());
	        case "leader":
	            return faction.getLider() != null ? faction.getLider().getNome() : "Nenhum";
	        case "captains_count":
	            return StringUtils.formatNumber(faction.getCapitoes().size());
	        case "members_only_count":
	            return StringUtils.formatNumber(faction.getMembros().size());
	        case "recruits_count":
	            return StringUtils.formatNumber(faction.getRecrutas().size());
	        case "territories_count":
	            return StringUtils.formatNumber(faction.getTerras().size());
	        case "temporary_territories_count":
	            return StringUtils.formatNumber(faction.getTemporarios().size());
	        case "power":
	            return StringUtils.formatNumber(faction.getPoder());
	        case "max_power":
	            return StringUtils.formatNumber(faction.getPoderMax());
	        case "kills":
	            return StringUtils.formatNumber(faction.getAbates());
	        case "deaths":
	            return StringUtils.formatNumber(faction.getMortes());
	        case "kdr":
	            return String.format("%.2f", faction.getKdr());
	        case "spawners_money":
	            return StringUtils.formatNumber(faction.getMoneyEmSpawners());
	        case "total_money":
	            return StringUtils.formatNumber(faction.getMoneyTotal());
	        case "stored_generators":
	            return StringUtils.formatNumber(faction.getTotalStoredGenerators());
	        case "placed_generators":
	            return faction.getTotalPlacedGenerator();
	        case "placed_generators_per_type":
	            return faction.getTotalPlacedGeneratorsPerGenerator();
	        case "total_spawners_money":
	            return StringUtils.formatNumber(faction.getTotalMoneyEmSpawners());
	        case "under_attack":
	            return faction.isSobAtaque() ? "Sim" : "Não";
	        case "max_members":
	            return StringUtils.formatNumber(faction.getMaxMembers());
	        default:
	            // Suporte para placeholders de geradores específicos (ex.: faction_spawners_ZOMBIE)
	            if (value.startsWith("spawners_")) {
	                String entityTypeStr = value.replace("spawners_", "").toUpperCase();
	                try {
	                    EntityType entityType = EntityType.valueOf(entityTypeStr);
	                    int stored = faction.getStoredGeneratorAmount(entityType);
	                    int placed = faction.getPlacedGenerators().getOrDefault(entityType, 0);
	                    return StringUtils.formatNumber(stored + placed);
	                } catch (IllegalArgumentException e) {
	                    return "0";
	                }
	            }
	            return null;
	    }
	}

}
