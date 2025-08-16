package me.nd.factions.api;

import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import me.nd.factions.utils.ItemBuilder;

public class Heads {

	public static ItemStack VERDE;
	public static ItemStack MAGENTA;
	public static ItemStack BRANCO;
	public static ItemStack AMARELO;
	public static ItemStack ROXO;
	public static ItemStack LARANJA;
	public static ItemStack CINZA;
	
	
	static {
		VERDE = getSkull("http://textures.minecraft.net/texture/361e5b333c2a3868bb6a58b6674a2639323815738e77e053977419af3f77");
		MAGENTA = new ItemBuilder(Material.SKULL_ITEM,1,3).setSkullOwner("diablo3pk").toItemStack();
		BRANCO = new ItemBuilder(Material.SKULL_ITEM,1,3).setSkullOwner("cy1337").toItemStack();
		LARANJA = new ItemBuilder(Material.SKULL_ITEM,1,3).setSkullOwner("wulfric17").toItemStack();
		AMARELO = getSkull("http://textures.minecraft.net/texture/14c4141c1edf3f7e41236bd658c5bc7b5aa7abf7e2a852b647258818acd70d8");
		ROXO = getSkull("http://textures.minecraft.net/texture/e9352bcabfc27edb44ceb51b04786542f26a299a0529475346186ee94738f");
		CINZA = getSkull("http://textures.minecraft.net/texture/f2f085c6b3cb228e5ba81df562c4786762f3c257127e9725c77b7fd301d37");
	}
	
    private static final Logger LOGGER = Bukkit.getLogger();
    private static final boolean IS_MODERN_API = isModernApiSupported();

    // Check if modern API (1.13+) is supported
    private static boolean isModernApiSupported() {
        try {
            // Check for Material.PLAYER_HEAD (introduced in 1.13)
            Material.valueOf("PLAYER_HEAD");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static ItemStack getSkull(String url) {
        // Use PLAYER_HEAD for 1.13+, fallback to SKULL_ITEM for older versions
        Material skullMaterial = IS_MODERN_API ? Material.valueOf("PLAYER_HEAD") : Material.SKULL_ITEM;
        ItemStack skull = new ItemStack(skullMaterial, 1, (short) (IS_MODERN_API ? 0 : 3));
        
        if (url == null || url.isEmpty()) {
            return skull;
        }

        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta == null) {
            LOGGER.warning("Failed to get SkullMeta for skull item.");
            return skull;
        }

        // Try modern API first (1.16+ supports setOwnerProfile directly)
        if (IS_MODERN_API && trySetProfile(skullMeta, url)) {
            skull.setItemMeta(skullMeta);
            return skull;
        }

        // Fallback to reflection for older versions
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            byte[] encodedData = java.util.Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
            profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
            
            java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            LOGGER.severe("Error setting skull texture via reflection: " + e.getMessage());
        }

        skull.setItemMeta(skullMeta);
        return skull;
    }

    private static boolean trySetProfile(SkullMeta skullMeta, String url) {
        try {
            // Check if setOwnerProfile is available (1.16+)
            Class<?> skullMetaClass = skullMeta.getClass();
            if (skullMetaClass.getMethod("setOwnerProfile", GameProfile.class) != null) {
                GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                byte[] encodedData = java.util.Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
                profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
                skullMetaClass.getMethod("setOwnerProfile", GameProfile.class).invoke(skullMeta, profile);
                return true;
            }
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            // Modern API not available or failed, fallback to reflection
        }
        return false;
    }
	
    // Método auxiliar para mapear EntityType para nomes MHF_
    public static String getMHFHeadName(EntityType mobType) {
        switch (mobType) {
            case ZOMBIE:
                return "MHF_Zombie";
            case SKELETON:
                return "MHF_Skeleton";
            case SPIDER:
                return "MHF_Spider";
            case CAVE_SPIDER:
                return "MHF_CaveSpider";
            case CREEPER:
                return "MHF_Creeper";
            case ENDERMAN:
                return "MHF_Enderman";
            case BLAZE:
                return "MHF_Blaze";
            case GHAST:
                return "MHF_Ghast";
            case MAGMA_CUBE:
                return "MHF_LavaSlime";
            case SLIME:
                return "MHF_Slime";
            case WITHER_SKULL:
                return "MHF_WSkeleton";
            case PIG_ZOMBIE:
                return "MHF_PigZombie";
            case PIG:
                return "MHF_Pig";
            case COW:
                return "MHF_Cow";
            case CHICKEN:
                return "MHF_Chicken";
            case SHEEP:
                return "MHF_Sheep";
            case VILLAGER:
                return "MHF_Villager";
            case IRON_GOLEM:
                return "MHF_Golem";
            case ENDER_DRAGON:
                return "MHF_Enderdragon";
            case WITHER:
                return "MHF_Wither";
            case SQUID:
                return "MHF_Squid";
            case OCELOT:
                return "MHF_Ocelot";
            default:
                return "MHF_Steve"; // Fallback para mobs sem MHF_ específico
        }
    }
}
