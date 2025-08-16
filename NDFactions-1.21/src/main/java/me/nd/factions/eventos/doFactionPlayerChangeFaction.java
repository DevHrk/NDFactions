package me.nd.factions.eventos;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.nd.factions.enums.Motivo;
import me.nd.factions.objetos.NDPlayer;

public final class doFactionPlayerChangeFaction extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private NDPlayer player;
    private Motivo m;
    private boolean cancelled; // Variável para rastrear o estado de cancelamento

    public doFactionPlayerChangeFaction(NDPlayer p, Motivo motivo) {
        this.player = p;
        this.m = motivo;
        this.cancelled = false; // Inicialmente, o evento não está cancelado
    }

    /**
     * @return the player
     */
    public NDPlayer getPlayer() {
        return player;
    }

    /**
     * @return the m
     */
    public Motivo getM() {
        return m;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}