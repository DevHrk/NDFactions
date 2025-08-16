package me.nd.factions.api;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.profile.PlayerProfile;

public class Heads {

    public static ItemStack VERDE;
    public static ItemStack MAGENTA;
    public static ItemStack BRANCO;
    public static ItemStack AMARELO;
    public static ItemStack ROXO;
    public static ItemStack LARANJA;
    public static ItemStack CINZA;

    private static final Logger LOGGER = Bukkit.getLogger();

    static {
        // Initialize skulls with custom textures or owners
        VERDE = getSkull("http://textures.minecraft.net/texture/361e5b333c2a3868bb6a58b6674a2639323815738e77e053977419af3f77");
        MAGENTA = getSkull("http://textures.minecraft.net/texture/287569fb3128f5e6a91a9816aa807e5ddc2dce48aa2e151710195b7de8774dd4");
        BRANCO = getSkull("http://textures.minecraft.net/texture/8003c207c403af0693a9e2261e681d50ec57cf82be2f5d386b0b9d2707f71209");
        LARANJA = getSkull("http://textures.minecraft.net/texture/cede0c2aa6a7d273078e8dccdefc9dea465f7e2b495129e6da06f34a61329e52");
        AMARELO = getSkull("http://textures.minecraft.net/texture/14c4141c1edf3f7e41236bd658c5bc7b5aa7abf7e2a852b647258818acd70d8");
        ROXO = getSkull("http://textures.minecraft.net/texture/e9352bcabfc27edb44ceb51b04786542f26a299a0529475346186ee94738f");
        CINZA = getSkull("http://textures.minecraft.net/texture/f2f085c6b3cb228e5ba81df562c4786762f3c257127e9725c77b7fd301d37");
    }

    public static ItemStack getSkull(String url) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        if (skullMeta == null) return skull;

        try {
            UUID uuid = UUID.nameUUIDFromBytes(("Skull-" + url).getBytes(StandardCharsets.UTF_8));
            GameProfile profile = new GameProfile(uuid, "Skull");

            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
            String encodedData = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

            profile.getProperties().put("textures", new Property("textures", encodedData));

            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);

            skull.setItemMeta(skullMeta);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return skull;
    }



    public static ItemStack getSkullByOwner(String owner) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        if (skullMeta == null) {
            return skull;
        }

        if (owner != null && !owner.isEmpty()) {
            try {
                // Use setOwningPlayer for player heads (modern API, 1.13+)
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
                skull.setItemMeta(skullMeta);
            } catch (Exception e) {
            }
        }

        return skull;
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
            case PIGLIN:
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