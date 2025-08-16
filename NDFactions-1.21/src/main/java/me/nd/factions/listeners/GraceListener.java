package me.nd.factions.listeners;

import me.nd.factions.api.Config;
import me.nd.factions.comandos.AdminCommands;
import me.nd.factions.factions.API;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class GraceListener implements Listener{
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
	    if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
	        return;
	    }

	    Player target = (Player) event.getEntity();
	    Player attacker = (Player) event.getDamager();

	    // Verificar se o grace global está ativo
	    if (AdminCommands.globalGraceExpiration > System.currentTimeMillis()) {
	        event.setCancelled(true);
	        attacker.sendMessage(Config.get("Mensagens.CombateBloqueado").toString().replace("&", "§"));
	        return;
	    }

	    // Obter perfis
	    NDPlayer targetProfile = API.getPlayer(target.getName());
	    NDPlayer attackerProfile = API.getPlayer(attacker.getName());

	    if (targetProfile == null || attackerProfile == null) {
	        return;
	    }

	    NDFaction targetFaction = targetProfile.getFaction();
	    NDFaction attackerFaction = attackerProfile.getFaction();

	    if ((targetFaction != null && AdminCommands.graceFactions.containsKey(targetFaction)) ||
	        (attackerFaction != null && AdminCommands.graceFactions.containsKey(attackerFaction))) {
	        event.setCancelled(true);
	        attacker.sendMessage(Config.get("Mensagens.CombateBloqueado").toString().replace("&", "§"));
	    }
	}

}
