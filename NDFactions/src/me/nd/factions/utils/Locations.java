package me.nd.factions.utils;

import org.bukkit.entity.*;
import org.bukkit.block.*;
import org.bukkit.*;
import java.util.*;

public class Locations
{
    private String worldName;
    private int x;
    private int z;
    
    public Locations() {
        this.worldName = "world";
        this.x = 0;
        this.z = 0;
    }
    
    public Locations(final String worldName, final int x, final int z) {
        this.worldName = "world";
        this.x = 0;
        this.z = 0;
        this.worldName = worldName;
        this.x = x;
        this.z = z;
    }
    
    public Locations(final Location location) {
        this(location.getWorld().getName(), blockToChunk(location.getBlockX()), blockToChunk(location.getBlockZ()));
    }
    
    public Locations(final Player player) {
        this(player.getLocation());
    }
    
    public Locations(final Block block) {
        this(block.getLocation());
    }
    
    public String getWorldName() {
        return this.worldName;
    }
    
    public World getWorld() {
        return Bukkit.getWorld(this.worldName);
    }
    
    public void setWorldName(final String worldName) {
        this.worldName = worldName;
    }
    
    public long getX() {
        return this.x;
    }
    
    public void setX(final int x) {
        this.x = x;
    }
    
    public long getZ() {
        return this.z;
    }
    
    public void setZ(final int z) {
        this.z = z;
    }
    
    public String getCoordString() {
        return String.valueOf(this.x) + "," + this.z;
    }
    
    @Override
    public String toString() {
        return "[" + this.getWorldName() + "," + this.getCoordString() + "]";
    }
    
    public static int blockToChunk(final int blockVal) {
        return blockVal >> 4;
    }
    
    public static int blockToRegion(final int blockVal) {
        return blockVal >> 9;
    }
    
    public static int chunkToRegion(final int chunkVal) {
        return chunkVal >> 5;
    }
    
    public static int chunkToBlock(final int chunkVal) {
        return chunkVal << 4;
    }
    
    public static int regionToBlock(final int regionVal) {
        return regionVal << 9;
    }
    
    public static int regionToChunk(final int regionVal) {
        return regionVal << 5;
    }
    
    public Locations getRelative(final int dx, final int dz) {
        return new Locations(this.worldName, this.x + dx, this.z + dz);
    }
    
    public double getDistanceTo(final Locations that) {
        final double dx = that.x - this.x;
        final double dz = that.z - this.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    public Set<Locations> getCircle(final double radius) {
        final Set<Locations> ret = new LinkedHashSet<Locations>();
        if (radius <= 0.0) {
            return ret;
        }
        final int xfrom = (int)Math.floor(this.x - radius);
        final int xto = (int)Math.ceil(this.x + radius);
        final int zfrom = (int)Math.floor(this.z - radius);
        final int zto = (int)Math.ceil(this.z + radius);
        for (int x = xfrom; x <= xto; ++x) {
            for (int z = zfrom; z <= zto; ++z) {
                final Locations potential = new Locations(this.worldName, x, z);
                if (this.getDistanceTo(potential) <= radius) {
                    ret.add(potential);
                }
            }
        }
        return ret;
    }
    
    public static HashSet<Locations> getArea(final Locations from, final Locations to) {
        final HashSet<Locations> ret = new HashSet<Locations>();
        long[] arrayOfLong1;
        for (int j = (arrayOfLong1 = Utils.range(from.getX(), to.getX())).length, i = 0; i < j; ++i) {
            final long x = arrayOfLong1[i];
            long[] arrayOfLong2;
            for (int m = (arrayOfLong2 = Utils.range(from.getZ(), to.getZ())).length, k = 0; k < m; ++k) {
                final long z = arrayOfLong2[k];
                ret.add(new Locations(from.getWorldName(), (int)x, (int)z));
            }
        }
        return ret;
    }
    
    @Override
    public int hashCode() {
        return (this.x << 9) + this.z + ((this.worldName != null) ? this.worldName.hashCode() : 0);
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Locations)) {
            return false;
        }
        final Locations that = (Locations)obj;
        if (this.x == that.x && this.z == that.z) {
            if (this.worldName == null) {
                if (that.worldName != null) {
                    return false;
                }
            }
            else if (!this.worldName.equals(that.worldName)) {
                return false;
            }
            return true;
        }
        return false;
    }
}
