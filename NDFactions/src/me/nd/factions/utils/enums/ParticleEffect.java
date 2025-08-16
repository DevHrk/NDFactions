package me.nd.factions.utils.enums;

import java.util.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import me.nd.factions.utils.ReflectionUtils;

import java.lang.reflect.*;
import org.bukkit.*;

public enum ParticleEffect
{
    EXPLOSION_NORMAL("EXPLOSION_NORMAL", 0, "explode", 0, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    EXPLOSION_LARGE("EXPLOSION_LARGE", 1, "largeexplode", 1, -1, new ParticleProperty[0]), 
    EXPLOSION_HUGE("EXPLOSION_HUGE", 2, "hugeexplosion", 2, -1, new ParticleProperty[0]), 
    FIREWORKS_SPARK("FIREWORKS_SPARK", 3, "fireworksSpark", 3, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    WATER_BUBBLE("WATER_BUBBLE", 4, "bubble", 4, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL, ParticleProperty.REQUIRES_WATER }), 
    WATER_SPLASH("WATER_SPLASH", 5, "splash", 5, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    WATER_WAKE("WATER_WAKE", 6, "wake", 6, 7, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    SUSPENDED("SUSPENDED", 7, "suspended", 7, -1, new ParticleProperty[] { ParticleProperty.REQUIRES_WATER }), 
    SUSPENDED_DEPTH("SUSPENDED_DEPTH", 8, "depthSuspend", 8, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    CRIT("CRIT", 9, "crit", 9, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    CRIT_MAGIC("CRIT_MAGIC", 10, "magicCrit", 10, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    SMOKE_NORMAL("SMOKE_NORMAL", 11, "smoke", 11, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    SMOKE_LARGE("SMOKE_LARGE", 12, "largesmoke", 12, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    SPELL("SPELL", 13, "spell", 13, -1, new ParticleProperty[0]), 
    SPELL_INSTANT("SPELL_INSTANT", 14, "instantSpell", 14, -1, new ParticleProperty[0]), 
    SPELL_MOB("SPELL_MOB", 15, "mobSpell", 15, -1, new ParticleProperty[] { ParticleProperty.COLORABLE }), 
    SPELL_MOB_AMBIENT("SPELL_MOB_AMBIENT", 16, "mobSpellAmbient", 16, -1, new ParticleProperty[] { ParticleProperty.COLORABLE }), 
    SPELL_WITCH("SPELL_WITCH", 17, "witchMagic", 17, -1, new ParticleProperty[0]), 
    DRIP_WATER("DRIP_WATER", 18, "dripWater", 18, -1, new ParticleProperty[0]), 
    DRIP_LAVA("DRIP_LAVA", 19, "dripLava", 19, -1, new ParticleProperty[0]), 
    VILLAGER_ANGRY("VILLAGER_ANGRY", 20, "angryVillager", 20, -1, new ParticleProperty[0]), 
    VILLAGER_HAPPY("VILLAGER_HAPPY", 21, "happyVillager", 21, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    TOWN_AURA("TOWN_AURA", 22, "townaura", 22, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    NOTE("NOTE", 23, "note", 23, -1, new ParticleProperty[] { ParticleProperty.COLORABLE }), 
    PORTAL("PORTAL", 24, "portal", 24, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    ENCHANTMENT_TABLE("ENCHANTMENT_TABLE", 25, "enchantmenttable", 25, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    FLAME("FLAME", 26, "flame", 26, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    LAVA("LAVA", 27, "lava", 27, -1, new ParticleProperty[0]), 
    FOOTSTEP("FOOTSTEP", 28, "footstep", 28, -1, new ParticleProperty[0]), 
    CLOUD("CLOUD", 29, "cloud", 29, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    REDSTONE("REDSTONE", 30, "reddust", 30, -1, new ParticleProperty[] { ParticleProperty.COLORABLE }), 
    SNOWBALL("SNOWBALL", 31, "snowballpoof", 31, -1, new ParticleProperty[0]), 
    SNOW_SHOVEL("SNOW_SHOVEL", 32, "snowshovel", 32, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL }), 
    SLIME("SLIME", 33, "slime", 33, -1, new ParticleProperty[0]), 
    HEART("HEART", 34, "heart", 34, -1, new ParticleProperty[0]), 
    BARRIER("BARRIER", 35, "barrier", 35, 8, new ParticleProperty[0]), 
    ITEM_CRACK("ITEM_CRACK", 36, "iconcrack", 36, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL, ParticleProperty.REQUIRES_DATA }), 
    BLOCK_CRACK("BLOCK_CRACK", 37, "blockcrack", 37, -1, new ParticleProperty[] { ParticleProperty.DIRECTIONAL, ParticleProperty.REQUIRES_DATA }), 
    BLOCK_DUST("BLOCK_DUST", 38, "blockdust", 38, 7, new ParticleProperty[] { ParticleProperty.DIRECTIONAL, ParticleProperty.REQUIRES_DATA }), 
    WATER_DROP("WATER_DROP", 39, "droplet", 39, 8, new ParticleProperty[0]), 
    ITEM_TAKE("ITEM_TAKE", 40, "take", 40, 8, new ParticleProperty[0]), 
    MOB_APPEARANCE("MOB_APPEARANCE", 41, "mobappearance", 41, 8, new ParticleProperty[0]);
    
    private static final Map<String, ParticleEffect> NAME_MAP;
    private static final Map<Integer, ParticleEffect> ID_MAP;
    private final String name;
    private final int id;
    private final int requiredVersion;
    private final List<ParticleProperty> properties;
    
    static {
        NAME_MAP = new HashMap<String, ParticleEffect>();
        ID_MAP = new HashMap<Integer, ParticleEffect>();
        ParticleEffect[] values;
        for (int length = (values = values()).length, i = 0; i < length; ++i) {
            final ParticleEffect effect = values[i];
            ParticleEffect.NAME_MAP.put(effect.name, effect);
            ParticleEffect.ID_MAP.put(effect.id, effect);
        }
    }
    
    private ParticleEffect(final String s, final int n, final String name, final int id, final int requiredVersion, final ParticleProperty... properties) {
        this.name = name;
        this.id = id;
        this.requiredVersion = requiredVersion;
        this.properties = Arrays.asList(properties);
    }
    
    public String getName() {
        return this.name;
    }
    
    public int getId() {
        return this.id;
    }
    
    public int getRequiredVersion() {
        return this.requiredVersion;
    }
    
    public boolean hasProperty(final ParticleProperty property) {
        return this.properties.contains(property);
    }
    
    public boolean isSupported() {
        return this.requiredVersion == -1 || ParticlePacket.getVersion() >= this.requiredVersion;
    }
    
    public static ParticleEffect fromName(final String name) {
        for (final Map.Entry<String, ParticleEffect> entry : ParticleEffect.NAME_MAP.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(name)) {
                continue;
            }
            return entry.getValue();
        }
        return null;
    }
    
    public static ParticleEffect fromId(final int id) {
        for (final Map.Entry<Integer, ParticleEffect> entry : ParticleEffect.ID_MAP.entrySet()) {
            if (entry.getKey() != id) {
                continue;
            }
            return entry.getValue();
        }
        return null;
    }
    
    private static boolean isWater(final Location location) {
        final Material material = location.getBlock().getType();
        return material == Material.WATER || material == Material.STATIONARY_WATER;
    }
    
    private static boolean isLongDistance(final Location location, final List<Player> players) {
        for (final Player player : players) {
            if (player.getLocation().distanceSquared(location) < 65536.0) {
                continue;
            }
            return true;
        }
        return false;
    }
    
    private static boolean isDataCorrect(final ParticleEffect effect, final ParticleData data) {
        return ((effect == ParticleEffect.BLOCK_CRACK || effect == ParticleEffect.BLOCK_DUST) && data instanceof BlockData) || (effect == ParticleEffect.ITEM_CRACK && data instanceof ItemData);
    }
    
    private static boolean isColorCorrect(final ParticleEffect effect, final ParticleColor color) {
        return ((effect == ParticleEffect.SPELL_MOB || effect == ParticleEffect.SPELL_MOB_AMBIENT || effect == ParticleEffect.REDSTONE) && color instanceof OrdinaryColor) || (effect == ParticleEffect.NOTE && color instanceof NoteColor);
    }
    
    public void display(final float offsetX, final float offsetY, final float offsetZ, final float speed, final int amount, final Location center, final double range) throws ParticleVersionException, ParticleDataException, IllegalArgumentException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect requires additional data");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_WATER) && !isWater(center)) {
            throw new IllegalArgumentException("There is no water at the center location");
        }
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount, range > 256.0, null).sendTo(center, range);
    }
    
    public void display(final float offsetX, final float offsetY, final float offsetZ, final float speed, final int amount, final Location center, final List<Player> players) throws ParticleVersionException, ParticleDataException, IllegalArgumentException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect requires additional data");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_WATER) && !isWater(center)) {
            throw new IllegalArgumentException("There is no water at the center location");
        }
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount, isLongDistance(center, players), null).sendTo(center, players);
    }
    
    public void display(final float offsetX, final float offsetY, final float offsetZ, final float speed, final int amount, final Location center, final Player... players) throws ParticleVersionException, ParticleDataException, IllegalArgumentException {
        this.display(offsetX, offsetY, offsetZ, speed, amount, center, Arrays.asList(players));
    }
    
    public void display(final Vector direction, final float speed, final Location center, final double range) throws ParticleVersionException, ParticleDataException, IllegalArgumentException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect requires additional data");
        }
        if (!this.hasProperty(ParticleProperty.DIRECTIONAL)) {
            throw new IllegalArgumentException("This particle effect is not directional");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_WATER) && !isWater(center)) {
            throw new IllegalArgumentException("There is no water at the center location");
        }
        new ParticlePacket(this, direction, speed, range > 256.0, null).sendTo(center, range);
    }
    
    public void display(final Vector direction, final float speed, final Location center, final List<Player> players) throws ParticleVersionException, ParticleDataException, IllegalArgumentException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect requires additional data");
        }
        if (!this.hasProperty(ParticleProperty.DIRECTIONAL)) {
            throw new IllegalArgumentException("This particle effect is not directional");
        }
        if (this.hasProperty(ParticleProperty.REQUIRES_WATER) && !isWater(center)) {
            throw new IllegalArgumentException("There is no water at the center location");
        }
        new ParticlePacket(this, direction, speed, isLongDistance(center, players), null).sendTo(center, players);
    }
    
    public void display(final Vector direction, final float speed, final Location center, final Player... players) throws ParticleVersionException, ParticleDataException, IllegalArgumentException {
        this.display(direction, speed, center, Arrays.asList(players));
    }
    
    public void display(final ParticleColor color, final Location center, final double range) throws ParticleVersionException, ParticleColorException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (!this.hasProperty(ParticleProperty.COLORABLE)) {
            throw new ParticleColorException("This particle effect is not colorable");
        }
        if (!isColorCorrect(this, color)) {
            throw new ParticleColorException("The particle color type is incorrect");
        }
        new ParticlePacket(this, color, range > 256.0).sendTo(center, range);
    }
    
    public void display(final ParticleColor color, final Location center, final List<Player> players) throws ParticleVersionException, ParticleColorException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (!this.hasProperty(ParticleProperty.COLORABLE)) {
            throw new ParticleColorException("This particle effect is not colorable");
        }
        if (!isColorCorrect(this, color)) {
            throw new ParticleColorException("The particle color type is incorrect");
        }
        new ParticlePacket(this, color, isLongDistance(center, players)).sendTo(center, players);
    }
    
    public void display(final ParticleColor color, final Location center, final Player... players) throws ParticleVersionException, ParticleColorException {
        this.display(color, center, Arrays.asList(players));
    }
    
    public void display(final ParticleData data, final float offsetX, final float offsetY, final float offsetZ, final float speed, final int amount, final Location center, final double range) throws ParticleVersionException, ParticleDataException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (!this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect does not require additional data");
        }
        if (!isDataCorrect(this, data)) {
            throw new ParticleDataException("The particle data type is incorrect");
        }
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount, range > 256.0, data).sendTo(center, range);
    }
    
    public void display(final ParticleData data, final float offsetX, final float offsetY, final float offsetZ, final float speed, final int amount, final Location center, final List<Player> players) throws ParticleVersionException, ParticleDataException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (!this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect does not require additional data");
        }
        if (!isDataCorrect(this, data)) {
            throw new ParticleDataException("The particle data type is incorrect");
        }
        new ParticlePacket(this, offsetX, offsetY, offsetZ, speed, amount, isLongDistance(center, players), data).sendTo(center, players);
    }
    
    public void display(final ParticleData data, final float offsetX, final float offsetY, final float offsetZ, final float speed, final int amount, final Location center, final Player... players) throws ParticleVersionException, ParticleDataException {
        this.display(data, offsetX, offsetY, offsetZ, speed, amount, center, Arrays.asList(players));
    }
    
    public void display(final ParticleData data, final Vector direction, final float speed, final Location center, final double range) throws ParticleVersionException, ParticleDataException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (!this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect does not require additional data");
        }
        if (!isDataCorrect(this, data)) {
            throw new ParticleDataException("The particle data type is incorrect");
        }
        new ParticlePacket(this, direction, speed, range > 256.0, data).sendTo(center, range);
    }
    
    public void display(final ParticleData data, final Vector direction, final float speed, final Location center, final List<Player> players) throws ParticleVersionException, ParticleDataException {
        if (!this.isSupported()) {
            throw new ParticleVersionException("This particle effect is not supported by your server version");
        }
        if (!this.hasProperty(ParticleProperty.REQUIRES_DATA)) {
            throw new ParticleDataException("This particle effect does not require additional data");
        }
        if (!isDataCorrect(this, data)) {
            throw new ParticleDataException("The particle data type is incorrect");
        }
        new ParticlePacket(this, direction, speed, isLongDistance(center, players), data).sendTo(center, players);
    }
    
    public void display(final ParticleData data, final Vector direction, final float speed, final Location center, final Player... players) throws ParticleVersionException, ParticleDataException {
        this.display(data, direction, speed, center, Arrays.asList(players));
    }
    
    public enum ParticleProperty
    {
        REQUIRES_WATER("REQUIRES_WATER", 0), 
        REQUIRES_DATA("REQUIRES_DATA", 1), 
        DIRECTIONAL("DIRECTIONAL", 2), 
        COLORABLE("COLORABLE", 3);
        
        private ParticleProperty(final String s, final int n) {
        }
    }
    
    public abstract static class ParticleData
    {
        private final Material material;
        private final byte data;
        private final int[] packetData;
        
		public ParticleData(final Material material, final byte data) {
            this.material = material;
            this.data = data;
            this.packetData = new int[] { material.getId(), data };
        }
        
        public Material getMaterial() {
            return this.material;
        }
        
        public byte getData() {
            return this.data;
        }
        
        public int[] getPacketData() {
            return this.packetData;
        }
        
        public String getPacketDataString() {
            return "_" + this.packetData[0] + "_" + this.packetData[1];
        }
    }
    
    public static final class ItemData extends ParticleData
    {
        public ItemData(final Material material, final byte data) {
            super(material, data);
        }
    }
    
    public static final class BlockData extends ParticleData
    {
        public BlockData(final Material material, final byte data) throws IllegalArgumentException {
            super(material, data);
            if (!material.isBlock()) {
                throw new IllegalArgumentException("The material is not a block");
            }
        }
    }
    
    public abstract static class ParticleColor
    {
        public abstract float getValueX();
        
        public abstract float getValueY();
        
        public abstract float getValueZ();
    }
    
    public static final class OrdinaryColor extends ParticleColor
    {
        private final int red;
        private final int green;
        private final int blue;
        
        public OrdinaryColor(final int red, final int green, final int blue) throws IllegalArgumentException {
            if (red < 0) {
                throw new IllegalArgumentException("The red value is lower than 0");
            }
            if (red > 255) {
                throw new IllegalArgumentException("The red value is higher than 255");
            }
            this.red = red;
            if (green < 0) {
                throw new IllegalArgumentException("The green value is lower than 0");
            }
            if (green > 255) {
                throw new IllegalArgumentException("The green value is higher than 255");
            }
            this.green = green;
            if (blue < 0) {
                throw new IllegalArgumentException("The blue value is lower than 0");
            }
            if (blue > 255) {
                throw new IllegalArgumentException("The blue value is higher than 255");
            }
            this.blue = blue;
        }
        
        public int getRed() {
            return this.red;
        }
        
        public int getGreen() {
            return this.green;
        }
        
        public int getBlue() {
            return this.blue;
        }
        
        @Override
        public float getValueX() {
            return this.red / 255.0f;
        }
        
        @Override
        public float getValueY() {
            return this.green / 255.0f;
        }
        
        @Override
        public float getValueZ() {
            return this.blue / 255.0f;
        }
    }
    
    public static final class NoteColor extends ParticleColor
    {
        private final int note;
        
        public NoteColor(final int note) throws IllegalArgumentException {
            if (note < 0) {
                throw new IllegalArgumentException("The note value is lower than 0");
            }
            if (note > 24) {
                throw new IllegalArgumentException("The note value is higher than 24");
            }
            this.note = note;
        }
        
        @Override
        public float getValueX() {
            return this.note / 24.0f;
        }
        
        @Override
        public float getValueY() {
            return 0.0f;
        }
        
        @Override
        public float getValueZ() {
            return 0.0f;
        }
    }
    
    private static final class ParticleDataException extends RuntimeException
    {
        private static final long serialVersionUID = 3203085387160737484L;
        
        public ParticleDataException(final String message) {
            super(message);
        }
    }
    
    private static final class ParticleColorException extends RuntimeException
    {
        private static final long serialVersionUID = 3203085387160737484L;
        
        public ParticleColorException(final String message) {
            super(message);
        }
    }
    
    private static final class ParticleVersionException extends RuntimeException
    {
        private static final long serialVersionUID = 3203085387160737484L;
        
        public ParticleVersionException(final String message) {
            super(message);
        }
    }
    
    public static final class ParticlePacket
    {
        private static int version;
        private static Class<?> enumParticle;
        private static Constructor<?> packetConstructor;
        private static Method getHandle;
        private static Field playerConnection;
        private static Method sendPacket;
        private static boolean initialized;
        private final ParticleEffect effect;
        private final float offsetX;
        private final float offsetY;
        private final float offsetZ;
        private final float speed;
        private final int amount;
        private final boolean longDistance;
        private final ParticleData data;
        private Object packet;
        
        public ParticlePacket(final ParticleEffect effect, final float offsetX, final float offsetY, final float offsetZ, final float speed, final int amount, final boolean longDistance, final ParticleData data) throws IllegalArgumentException {
            initialize();
            if (speed < 0.0f) {
                throw new IllegalArgumentException("The speed is lower than 0");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("The amount is lower than 0");
            }
            this.effect = effect;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
            this.amount = amount;
            this.longDistance = longDistance;
            this.data = data;
        }
        
        public ParticlePacket(final ParticleEffect effect, final Vector direction, final float speed, final boolean longDistance, final ParticleData data) throws IllegalArgumentException {
            this(effect, (float)direction.getX(), (float)direction.getY(), (float)direction.getZ(), speed, 0, longDistance, data);
        }
        
        public ParticlePacket(final ParticleEffect effect, final ParticleColor color, final boolean longDistance) {
            this(effect, color.getValueX(), color.getValueY(), color.getValueZ(), 1.0f, 0, longDistance, null);
        }
        
        public static void initialize() throws VersionIncompatibleException {
            if (ParticlePacket.initialized) {
                return;
            }
            try {
                ParticlePacket.version = Integer.parseInt(Character.toString(ReflectionUtils.PackageType.getServerVersion().charAt(3)));
                if (ParticlePacket.version > 7) {
                    ParticlePacket.enumParticle = ReflectionUtils.PackageType.MINECRAFT_SERVER.getClass("EnumParticle");
                }
                final Class<?> packetClass = ReflectionUtils.PackageType.MINECRAFT_SERVER.getClass((ParticlePacket.version < 7) ? "Packet63WorldParticles" : "PacketPlayOutWorldParticles");
                ParticlePacket.packetConstructor = ReflectionUtils.getConstructor(packetClass, (Class<?>[])new Class[0]);
                ParticlePacket.getHandle = ReflectionUtils.getMethod("CraftPlayer", ReflectionUtils.PackageType.CRAFTBUKKIT_ENTITY, "getHandle", (Class<?>[])new Class[0]);
                ParticlePacket.playerConnection = ReflectionUtils.getField("EntityPlayer", ReflectionUtils.PackageType.MINECRAFT_SERVER, false, "playerConnection");
                ParticlePacket.sendPacket = ReflectionUtils.getMethod(ParticlePacket.playerConnection.getType(), "sendPacket", ReflectionUtils.PackageType.MINECRAFT_SERVER.getClass("Packet"));
            }
            catch (Exception exception) {
                throw new VersionIncompatibleException("Your current bukkit version seems to be incompatible with this library", exception);
            }
            ParticlePacket.initialized = true;
        }
        
        public static int getVersion() {
            return ParticlePacket.version;
        }
        
        public static boolean isInitialized() {
            return ParticlePacket.initialized;
        }
        
        private void initializePacket(final Location center) throws PacketInstantiationException {
            if (this.packet != null) {
                return;
            }
            try {
                this.packet = ParticlePacket.packetConstructor.newInstance(new Object[0]);
                if (ParticlePacket.version < 8) {
                    String name = this.effect.getName();
                    if (this.data != null) {
                        name = String.valueOf(name) + this.data.getPacketDataString();
                    }
                    ReflectionUtils.setValue(this.packet, true, "a", name);
                }
                else {
                    ReflectionUtils.setValue(this.packet, true, "a", ParticlePacket.enumParticle.getEnumConstants()[this.effect.getId()]);
                    ReflectionUtils.setValue(this.packet, true, "j", this.longDistance);
                    if (this.data != null) {
                        ReflectionUtils.setValue(this.packet, true, "k", this.data.getPacketData());
                    }
                }
                ReflectionUtils.setValue(this.packet, true, "b", (float)center.getX());
                ReflectionUtils.setValue(this.packet, true, "c", (float)center.getY());
                ReflectionUtils.setValue(this.packet, true, "d", (float)center.getZ());
                ReflectionUtils.setValue(this.packet, true, "e", this.offsetX);
                ReflectionUtils.setValue(this.packet, true, "f", this.offsetY);
                ReflectionUtils.setValue(this.packet, true, "g", this.offsetZ);
                ReflectionUtils.setValue(this.packet, true, "h", this.speed);
                ReflectionUtils.setValue(this.packet, true, "i", this.amount);
            }
            catch (Exception exception) {
                throw new PacketInstantiationException("Packet instantiation failed", exception);
            }
        }
        
        public void sendTo(final Location center, final Player player) throws PacketInstantiationException, PacketSendingException {
            this.initializePacket(center);
            try {
                ParticlePacket.sendPacket.invoke(ParticlePacket.playerConnection.get(ParticlePacket.getHandle.invoke(player, new Object[0])), this.packet);
            }
            catch (Exception exception) {
                throw new PacketSendingException("Failed to send the packet to player '" + player.getName() + "'", exception);
            }
        }
        
        public void sendTo(final Location center, final List<Player> players) throws IllegalArgumentException {
            if (players.isEmpty()) {
                throw new IllegalArgumentException("The player list is empty");
            }
            for (final Player player : players) {
                this.sendTo(center, player);
            }
        }
        
        public void sendTo(final Location center, final double range) throws IllegalArgumentException {
            if (range < 1.0) {
                throw new IllegalArgumentException("The range is lower than 1");
            }
            final String worldName = center.getWorld().getName();
            final double squared = range * range;
            for (final Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getName().equals(worldName)) {
                    if (player.getLocation().distanceSquared(center) > squared) {
                        continue;
                    }
                    this.sendTo(center, player);
                }
            }
        }
        
        private static final class VersionIncompatibleException extends RuntimeException
        {
            private static final long serialVersionUID = 3203085387160737484L;
            
            public VersionIncompatibleException(final String message, final Throwable cause) {
                super(message, cause);
            }
        }
        
        private static final class PacketInstantiationException extends RuntimeException
        {
            private static final long serialVersionUID = 3203085387160737484L;
            
            public PacketInstantiationException(final String message, final Throwable cause) {
                super(message, cause);
            }
        }
        
        private static final class PacketSendingException extends RuntimeException
        {
            private static final long serialVersionUID = 3203085387160737484L;
            
            public PacketSendingException(final String message, final Throwable cause) {
                super(message, cause);
            }
        }
    }
}
