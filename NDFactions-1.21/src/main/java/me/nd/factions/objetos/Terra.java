package me.nd.factions.objetos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import me.nd.factions.mysql.DataManager;

public class Terra {

    private World world;
    private int x, z;
    private long dominationDate; // Timestamp when the terrain was claimed

    public Terra(World world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
        this.dominationDate = 0; // Initialize as unclaimed
    }

    public NDFaction getFaction() {
        for (NDFaction f : DataManager.factions.values()) {
            List<Terra> terras = new ArrayList<>();
            terras.addAll(f.getTerras());
            terras.addAll(f.getTemporarios());
            for (Terra t : terras) {
                if (this.equals(t)) { // Usar equals em vez de comparar chunks
                    return f;
                }
            }
        }
        return null;
    }

    public boolean isTemporario() {
        NDFaction faction = getFaction();
        if (faction == null) return false;
        for (Terra t : faction.getTemporarios()) {
            if (this.equals(t)) { // Usar equals em vez de comparar chunks
                return true;
            }
        }
        return false;
    }

    public boolean setFaction(NDFaction faction) {
        return setFaction(faction, false);
    }

    public boolean setFaction(NDFaction faction, boolean isTemporary) {
        NDFaction currentFaction = getFaction();

        if (currentFaction != null && currentFaction.equals(faction) && isTemporary == isTemporario()) {
            return true;
        }

        if (currentFaction != null) {
            if (isTemporario()) {
                currentFaction.getTemporarios().remove(this);
            } else {
                currentFaction.getTerras().remove(this);
            }
            try {
                currentFaction.save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao salvar facção " + currentFaction.getNome() + " ao remover terra: " + e.getMessage());
            }
        }

        if (faction == null) {
            this.dominationDate = 0; // Reset domination date when unclaimed
            return true;
        }

        if (!DataManager.factions.containsValue(faction)) {
            return false;
        }

        if (isTemporary) {
            faction.getTemporarios().add(this);
        } else {
            faction.getTerras().add(this);
        }

        // Set domination date when claimed (only if not already set)
        if (this.dominationDate == 0) {
            this.dominationDate = System.currentTimeMillis();
        }

        try {
            faction.save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar facção " + faction.getNome() + " ao adicionar terra: " + e.getMessage());
            return false;
        }

        return true;
    }

    public long getDominationDate() {
        return dominationDate;
    }

    public void setDominationDate(long timestamp) {
        this.dominationDate = timestamp;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Chunk getChunk() {
        return Bukkit.getWorld(world.getUID()).getChunkAt(x, z);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terra terra = (Terra) o;
        return x == terra.x &&
               z == terra.z &&
               dominationDate == terra.dominationDate &&
               Objects.equals(world.getUID(), terra.world.getUID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(world.getUID(), x, z, dominationDate);
    }

    @Override
    public String toString() {
        return "Terra{world=" + world.getName() + ", x=" + x + ", z=" + z + ", dominationDate=" + dominationDate + "}";
    }
}
