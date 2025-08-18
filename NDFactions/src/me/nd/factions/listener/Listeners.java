package me.nd.factions.listener;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import me.nd.factions.Main;
import me.nd.factions.addons.BlockCommands;
import me.nd.factions.addons.MembroPlus;
import me.nd.factions.addons.OfferManager;
import me.nd.factions.addons.Poder;
import me.nd.factions.addons.SobAtaque;
import me.nd.factions.listeners.FactionChest;
import me.nd.factions.listeners.FactionGenerators;
import me.nd.factions.listeners.FactionPermissionListener;
import me.nd.factions.listeners.FactionRelacao;
import me.nd.factions.listeners.FactionRoster;
import me.nd.factions.listeners.FactionSetSpawn;
import me.nd.factions.listeners.FactionsInvite;
import me.nd.factions.listeners.HomeListener;
import me.nd.factions.listeners.MenusListeners;
import me.nd.factions.listeners.PlayerListeners;
import me.nd.factions.listeners.onJoin;
import me.nd.factions.scoreboard.Board;

public class Listeners {

	public static void setupListeners() {
		PluginManager pm = Bukkit.getPluginManager();
		// Crie uma lista de classes de listeners
		List<Class<? extends Listener>> listenerClasses = Arrays.asList(
				FactionChest.class,
				FactionPermissionListener.class,
				FactionGenerators.class,
				PlayerListeners.class,
				onJoin.class,
				SobAtaque.class,
				Poder.class,
				MembroPlus.class,
				Board.class,
				FactionSetSpawn.class,
				FactionRoster.class,
				FactionRelacao.class,
				FactionsInvite.class,
				MenusListeners.class,
				OfferManager.class,
				BlockCommands.class,
				HomeListener.class);
		// Registre todos os listeners em um loop
		
		listenerClasses.forEach(listenerClass -> {
			if (Listener.class.isAssignableFrom(listenerClass)) {
				try {
					Listener listenerInstance = listenerClass.getDeclaredConstructor().newInstance();
					pm.registerEvents(listenerInstance, Main.get());
				} catch (ReflectiveOperationException e) {
					Bukkit.getLogger().severe(
							"Failed to register listener: " + listenerClass.getSimpleName() + " - " + e.getMessage());
				}
			} else {
				Bukkit.getLogger().warning("Class " + listenerClass.getSimpleName() + " does not implement Listener!");
			}
		});

	}
}
