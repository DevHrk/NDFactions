package me.nd.factions.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import me.nd.factions.api.Config;
import me.nd.factions.api.Heads;
import me.nd.factions.banners.Banners;
import me.nd.factions.enums.Cargo;
import me.nd.factions.enums.Protecao;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.enums.ParticleEffect;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility methods for the factions plugin, optimized for performance and clarity.
 */
public final class Utils {
	
	private static final Set<String> SUBSTANCE_CHARS = new HashSet<>(Arrays.asList(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    ));
    private static final BiMap<DyeColor, Integer> DYE_COLOR_MAP = HashBiMap.create();

    static {
        DYE_COLOR_MAP.put(DyeColor.WHITE, 15);
        DYE_COLOR_MAP.put(DyeColor.ORANGE, 14);
        DYE_COLOR_MAP.put(DyeColor.MAGENTA, 13);
        DYE_COLOR_MAP.put(DyeColor.LIGHT_BLUE, 12);
        DYE_COLOR_MAP.put(DyeColor.YELLOW, 11);
        DYE_COLOR_MAP.put(DyeColor.LIME, 10);
        DYE_COLOR_MAP.put(DyeColor.PINK, 9);
        DYE_COLOR_MAP.put(DyeColor.GRAY, 8);
        DYE_COLOR_MAP.put(DyeColor.SILVER, 7);
        DYE_COLOR_MAP.put(DyeColor.CYAN, 6);
        DYE_COLOR_MAP.put(DyeColor.PURPLE, 5);
        DYE_COLOR_MAP.put(DyeColor.BLUE, 4);
        DYE_COLOR_MAP.put(DyeColor.BROWN, 3);
        DYE_COLOR_MAP.put(DyeColor.GREEN, 2);
        DYE_COLOR_MAP.put(DyeColor.RED, 1);
        DYE_COLOR_MAP.put(DyeColor.BLACK, 0);
    }
    private static final List<Integer> INV_SLOTS = Arrays.asList(
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
    );
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("[a-zA-Z0-9].*");

    /**
     * Sorts map entries by values in descending order.
     */
    public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<>(new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                int cmp = e2.getValue().compareTo(e1.getValue());
                return cmp != 0 ? cmp : 1;
            }
        });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    public static short toShort(DyeColor dyeColor) {
        return DYE_COLOR_MAP.getOrDefault(dyeColor, 0).shortValue();
    }

    public static DyeColor fromInt(int number) {
        return DYE_COLOR_MAP.inverse().getOrDefault(number, DyeColor.WHITE);
    }

    public static String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return String.format("%f;%f;%f;%f;%f;%s",
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), loc.getWorld().getUID());
    }

    public static Location deserializeLocation(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] parts = s.split(";");
            if (parts.length != 6) return null;
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float yaw = Float.parseFloat(parts[3]);
            float pitch = Float.parseFloat(parts[4]);
            World world = Bukkit.getWorld(UUID.fromString(parts[5]));
            return world != null ? new Location(world, x, y, z, yaw, pitch) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isZonaProtegida(Location location, NDPlayer player) {
        if (location == null || player == null) {
            return false;
        }

        // Verifica o status de proteção do terreno
        Protecao protection = Utils.getProtection(location.getChunk(), player.getPlayer());
        
        // Bloqueia colocação de blocos em terrenos protegidos ou em zona de guerra
        return !(protection == Protecao.Protegida);
    }
    
    public static boolean isZonaProtegida(Location location) {
        if (location == null) {
            return false;
        }
        StateFlag.State pvpFlag = Utils.getWorldGuardPvpFlag(location);
        return !(pvpFlag == StateFlag.State.DENY);
    }
    
    public static boolean isZonaGuerra(Location location) {
        if (location == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk());
        return !(protection == Protecao.Guerra);
    }
    
    public static boolean isZonaLivre(Location location) {
        if (location == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk());
        return !(protection == Protecao.Livre);
    }

    public static boolean isAliada(Location location) {
        if (location == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk());
        return !(protection == Protecao.Aliada);
    }
    
    public static boolean isInimiga(Location location) {
        if (location == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk());
        return !(protection == Protecao.Inimiga);
    }
    
    public static boolean isSua(Location location) {
        if (location == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk());
        return !(protection == Protecao.Sua);
    }
    
    public static boolean isNeutra(Location location) {
        if (location == null) {
            return false;
        }

        Protecao protection = Utils.getProtection(location.getChunk());
        return !(protection == Protecao.Neutra);
    }
    
    public static List<NDFaction> getOnlineFactions() {
        Set<NDFaction> online = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            NDPlayer ndPlayer = DataManager.players.get(p.getName());
            if (ndPlayer != null && ndPlayer.hasFaction()) {
                online.add(ndPlayer.getFaction());
            }
        }
        return new ArrayList<>(online);
    }

    public static boolean containsSpecialCharacter(String s) {
        return s != null && !ALPHANUMERIC_PATTERN.matcher(s).matches();
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static void openInvitesMenu(Player player) {
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null) return;

        List<NDFaction> invites = ndPlayer.getConvites();
        if (invites.isEmpty()) {
            player.sendMessage(Config.get("Mensagens.SemConvites").toString().replace("&", "§"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 36, "Convites de Facções");
        int slotIndex = 0;
        for (NDFaction faction : invites) {
            if (slotIndex >= INV_SLOTS.size()) break;
            inv.setItem(INV_SLOTS.get(slotIndex++), new ItemBuilder(
                    Banners.getAlphabet(new ItemStack(Material.BANNER), faction.getTag(), DyeColor.WHITE, DyeColor.BLACK))
                    .addItemFlag(ItemFlag.HIDE_POTION_EFFECTS)
                    .setName("§aConvite de [" + faction.getTag() + "] " + faction.getNome())
                    .setLore(
                            "§fTerras: §7" + faction.getTerras().size(),
                            "§fPoder: §7" + faction.getPoder(),
                            "§fPoder máximo: §7" + faction.getPoderMax(),
                            "§fLíder: §7[#" + faction.getTag() + "] §f" + faction.getLider().getNome(),
                            "§fMembros: §7" + faction.getAll().size() + "/" + Config.get("Padrao.MaximoMembrosEmFac"),
                            "§fMembros online: §7" + faction.getAllOnline().size(),
                            "§fKDR: §7" + faction.getKdr(),
                            "§fAbates: §7" + faction.getAbates(),
                            "§fMortes: §7" + faction.getMortes(),
                            "",
                            "§7Botão esquerdo: §fEntrar na facção",
                            "§7Botão direito: §fNegar convite"
                    ).toItemStack());
        }

        inv.setItem(31, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124")).setName("§aVoltar").toItemStack());
        player.openInventory(inv);
    }

    public static void openMembersMenu(Player player, NDFaction faction) {
        Inventory inv = Bukkit.createInventory(null, 54, faction.getNome() + " - Membros");
        NDPlayer ndPlayer = DataManager.players.get(player.getName());

        // Copiar a lista para evitar alterações externas afetando a visualização
        List<NDPlayer> membros = new ArrayList<>(faction.getAll());
        List<ItemStack> items = new ArrayList<>();
        inv.setItem(19, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/4e159dab94b55e61b86a3f6fe7f6a4c7d937fe5aa9731293e314747c8f51019d"))
                .setName("§fNível §f§lI")
                .setLore("§7Considerada uma facção pequena,",
                		 "§7tendo que defender sua base de",
                		 "§7um ataque por até 2 horas",
                		 "",
                		 "§4OBS: O ataque e finalizado",
                		 "§4caso não haja um ataque constante.")
                .toItemStack());
        inv.setItem(28, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/e1f27337d1e1a97b67bf33d4517ce0c21de82b70e3bfd45a22201a4d056b600c"))
                .setName("§fNível §f§lII")
                .setLore("§7Considerada uma facção grande,",
               		 	"§7tendo que defender sua base de",
               		 	"§7um ataque por até 3 horas",
               		 	"",
               		 	"§4OBS: O ataque e finalizado",
                		"§4caso não haja um ataque constante.")
                .toItemStack());
        for (NDPlayer member : membros) {
            boolean canDemote = ndPlayer != null && canDemote(player, member);

            ItemBuilder builder = new ItemBuilder(Material.SKULL_ITEM,1 , (short)3)
                    .setSkullOwner(member.getNome())
                    .setName("§7" + member.getNome())
                    .setLore(
                            "§fPoder: §7" + member.getPoder() + "/" + member.getPodermax(),
                            "§fCargo: §7" + member.getCargoSimbolo() + member.getCargo(),
                            "§fKDR: §7" + member.getKDR(),
                            "§fAbates: §7" + member.getKills(),
                            "§fMortes: §7" + member.getMortes(),
                            "§fStatus: " + (Bukkit.getPlayer(member.getNome()) == null ? "§cOffline" : "§aOnline"),
                            "§fÚltimo login: §7" + member.getLast(),
                            canDemote ? "§aClique para mais informações" : ""
                    );

            items.add(builder.toItemStack());
        }

        // Preencher espaços vagos se necessário
        int maxSlots = 15;
        for (int i = items.size(); i < maxSlots; i++) {
            items.add(new ItemBuilder(Material.SKULL_ITEM,1 , (short)3).setName("§7Vago").toItemStack());
        }

        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        for (int i = 0; i < slots.length && i < items.size(); i++) {
            inv.setItem(slots[i], items.get(i));
        }

        inv.setItem(49, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124")).setName("§aVoltar").toItemStack());
        player.openInventory(inv);
    }

    public static boolean canDemote(Player player, NDPlayer target) {
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null || target.equals(ndPlayer)) return false;
        if (ndPlayer.getCargo() != Cargo.Capitão && ndPlayer.getCargo() != Cargo.Lider) return false;
        if (target.getCargo() == Cargo.Lider) return false;
        return !(target.getCargo() == Cargo.Capitão && ndPlayer.getCargo() == Cargo.Capitão);
    }

    public static boolean isTagTaken(String tag) {
        return tag != null && DataManager.factions.values().stream()
                .anyMatch(f -> f.getTag().equalsIgnoreCase(tag));
    }

    public static ItemStack getFactionBanner(NDFaction faction) {
        String allies = faction.getAliados().isEmpty() ? "§7Nenhum" :
                faction.getAliados().stream().map(NDFaction::getTag).collect(Collectors.joining(", ")) + ".";
        String enemies = faction.getInimigos().isEmpty() ? "§7Nenhum" :
                faction.getInimigos().stream().map(NDFaction::getTag).collect(Collectors.joining(", ")) + ".";

        return new ItemBuilder(
                Banners.getAlphabet(new ItemStack(Material.BANNER), faction.getTag(), DyeColor.WHITE, DyeColor.BLACK))
                .addItemFlag(ItemFlag.HIDE_POTION_EFFECTS)
                .setName("§e" + faction.getTag() + " - " + faction.getNome())
                .setLore(
                        "§fTerras: §7" + faction.getTerras().size(),
                        "§fPoder: §7" + faction.getPoder(),
                        "§fPoder máximo: §7" + faction.getPoderMax(),
                        "§fLíder: §7[#" + faction.getTag() + "] §f" + faction.getLider().getNome(),
                        "§fMembros: §7" + faction.getAll().size() + "/" + faction.getMaxMembers(),
                        "§fMembros online: §7" + faction.getAllOnline().size(),
                        "§fKDR: §7" + faction.getKdr(),
                        "§fAbates: §7" + faction.getAbates(),
                        "§fMortes: §7" + faction.getMortes(),
                        "§fAliados: §7" + allies,
                        "§fInimigos: §7" + enemies
                ).toItemStack();
    }

    public static NDFaction getFactionByTag(String tag) {
        if (tag == null) return null;
        return DataManager.factions.values().stream()
                .filter(f -> f.getTag().equalsIgnoreCase(tag))
                .findFirst().orElse(null);
    }

    public static boolean isNameTaken(String name) {
        return name != null && DataManager.factions.values().stream()
                .anyMatch(f -> f.getNome().equalsIgnoreCase(name));
    }

    public static List<String> getAsciiCompass(double degrees, String colorActive, String colorDefault) {
        Point direction = getCompassPointForDirection(degrees);
        List<String> compass = new ArrayList<>(3);
        compass.add(Point.NW.toString(direction == Point.NW, colorActive, colorDefault) +
                   Point.N.toString(direction == Point.N, colorActive, colorDefault) +
                   Point.NE.toString(direction == Point.NE, colorActive, colorDefault));
        compass.add(Point.W.toString(direction == Point.W, colorActive, colorDefault) +
                   colorDefault + "+" +
                   Point.E.toString(direction == Point.E, colorActive, colorDefault));
        compass.add(Point.SW.toString(direction == Point.SW, colorActive, colorDefault) +
                   Point.S.toString(direction == Point.S, colorActive, colorDefault) +
                   Point.SE.toString(direction == Point.SE, colorActive, colorDefault));
        return compass;
    }

    public static Point getCompassPointForDirection(double degrees) {
        // Normalize degrees to [0, 360)
        double normalized = degrees % 360.0;
        if (normalized < 0) normalized += 360.0;

        // Minecraft yaw: 0° = South, 90° = West, 180° = North, 270° = East
        // Divide 360° into 8 segments (45° each), centered on cardinal directions
        if (normalized >= 337.5 || normalized < 22.5) return Point.S;  // -22.5° to 22.5°
        if (normalized < 67.5) return Point.SW;                       // 22.5° to 67.5°
        if (normalized < 112.5) return Point.W;                       // 67.5° to 112.5°
        if (normalized < 157.5) return Point.NW;                      // 112.5° to 157.5°
        if (normalized < 202.5) return Point.N;                       // 157.5° to 202.5°
        if (normalized < 247.5) return Point.NE;                      // 202.5° to 247.5°
        if (normalized < 292.5) return Point.E;                       // 247.5° to 292.5°
        return Point.SE;                                              // 292.5° to 337.5°
    }

    public enum Point {
        N("Norte", 'N'),      // North
        NE("Nordeste", '/'),  // Northeast
        E("Leste", 'L'),      // East
        SE("Sudeste", '\\'),   // Southeast
        S("Sul", 'S'),        // South
        SW("Sudoeste", '/'),  // Southwest
        W("Oeste", 'O'),      // West
        NW("Noroeste", '\\');  // Northwest

        @SuppressWarnings("unused")
		private final String name;
        private final char asciiChar;

        Point(String name, char asciiChar) {
            this.name = name;
            this.asciiChar = asciiChar;
        }

        @Override
        public String toString() {
            return String.valueOf(asciiChar);
        }

        public String toString(boolean isActive, String colorActive, String colorDefault) {
            return (isActive ? colorActive : colorDefault) + asciiChar;
        }
    }

    public static EntityType getCreatureType(Entity entity) {
        return entity instanceof Creature ? entity.getType() : null;
    }

    public static StateFlag.State getWorldGuardPvpFlag(Location location) {
        ApplicableRegionSet regions = WGBukkit.getRegionManager(location.getWorld()).getApplicableRegions(location);
        return regions.queryValue(null, DefaultFlag.PVP);
    }

    public static StringBuilder appendMapData(StringBuilder builder, int line, List<String> compass) {
        switch (line) {
            case 4:
                builder.append("  ").append(compass.get(0));
                break;
            case 5:
                builder.append("  ").append(compass.get(1));
                break;
            case 6:
                builder.append("  ").append(compass.get(2));
                break;
            case 8:
                builder.append("  §a\u2588§f Aliada");
                break;
            case 9:
                builder.append("  §f\u2588§f Neutra");
                break;
            case 10:
                builder.append("  §c\u2588§f Inimiga");
                break;
            case 11:
                builder.append("  §7\u2588§f Zona Livre");
                break;
            case 12:
                builder.append("  §6\u2588§f Zona protegida");
                break;
            case 13:
                builder.append("  §4\u2588§f Zona de Guerra");
                break;
            case 14:
                builder.append("  §8\u2588§f Sua facção");
                break;
            case 15:
                builder.append("  §9\u2588§f Terrenos temporários");
                break;
            case 16:
                builder.append("  §d\u2588§f Sob Ataque");
                break;
            default:
                // No action, return builder unchanged
                break;
        }
        return builder;
    }
    public static Object[][] rotateMatrixClockwise(Object[][] matrix) {
        int width = matrix.length;
        int height = matrix[0].length;
        Object[][] result = new Object[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                result[i][j] = matrix[width - j - 1][i];
            }
        }
        return result;
    }

    public static Object[][] rotateMatrixCounterClockwise(Object[][] matrix) {
        int width = matrix.length;
        int height = matrix[0].length;
        Object[][] result = new Object[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                result[i][j] = matrix[j][height - i - 1];
            }
        }
        return result;
    }

    public static Object[][] listToArray(List<Object[]> list) {
        return list.toArray(new Object[0][]);
    }
    
    public static void updateMap(Player player, Location location) {
        try {
            List<Object[]> mapData = getMap(player, location, 19, 19);
            float yaw = player.getLocation().getYaw();
            Point direction = getCompassPointForDirection(yaw);
            List<String> compass = getAsciiCompass(yaw, "§c§l", "§6§l");

            Object[][] matrix = listToArray(mapData);
            Object[][] rotatedMatrix;

            switch (direction) {
                case W:
                case NE:
                    rotatedMatrix = rotateMatrixClockwise(matrix);
                    break;
                case S:
                case SE:
                    rotatedMatrix = rotateMatrixClockwise(rotateMatrixClockwise(matrix));
                    break;
                case E:
                case SW:
                    rotatedMatrix = rotateMatrixCounterClockwise(matrix);
                    break;
                default:
                    rotatedMatrix = matrix;
            }

            player.sendMessage(""); // espaço acima do mapa

            Locations center = new Locations(location);
            int half = 19 / 2;
            Locations topLeft = center.getRelative(-half, -half);

            NDPlayer ndPlayer = DataManager.players.get(player.getName());
            NDFaction playerFaction = (ndPlayer != null && ndPlayer.hasFaction()) ? ndPlayer.getFaction() : null;

            for (int i = 0; i < rotatedMatrix.length; i++) {
                TextComponent line = new TextComponent("");
                StringBuilder plainLine = new StringBuilder();

                for (int j = 0; j < rotatedMatrix[i].length; j++) {
                    Object raw = rotatedMatrix[i][j];
                    TextComponent cell;

                    if (raw instanceof TextComponent) {
                        cell = (TextComponent) raw;
                    } else if (raw instanceof String) {
                        cell = new TextComponent((String) raw);
                    } else {
                        cell = new TextComponent("§f?");
                    }

                    String cellText = cell.getText();
                    plainLine.append(cellText);

                    Locations current = getRotatedPosition(topLeft, j, i, direction);
                    Chunk chunk = player.getWorld().getChunkAt((int) current.getX(), (int) current.getZ());

                    String hover = getHoverText(chunk, player, playerFaction);
                    if (hover != null) {
                        cell.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder(hover).create()));
                    }

                    if ("§7\u2588".equals(cellText) && playerFaction != null) {
                        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
                        StateFlag.State pvp = getWorldGuardPvpFlag(getLocation(chunk, player.getLocation().getBlockY()));
                        if (terra.getFaction() == null && (pvp == null || pvp != StateFlag.State.DENY)) {
                            cell.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/f claim " + chunk.getX() + " " + chunk.getZ()));
                        }
                    }

                    line.addExtra(cell);
                }

                // Adicionar info lateral (bússola/legenda)
                StringBuilder fullLine = appendMapData(new StringBuilder(plainLine), i + 1, compass);
                String extraText = fullLine.substring(plainLine.length());
                if (!extraText.isEmpty()) {
                    line.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', extraText)));
                }

                player.spigot().sendMessage(line);
            }

        } catch (Exception e) {
            player.sendMessage("§cErro ao gerar mapa.");
            e.printStackTrace();
        }
    }

    
    private static Locations getRotatedPosition(Locations topLeft, int x, int y, Point direction) {
        // Corrigir os offsets com base na rotação
        int newX = x;
        int newY = y;

        if (direction == Point.W || direction == Point.NE) {
            // 90° Clockwise
            newX = y;
            newY = 18 - x;
        } else if (direction == Point.S || direction == Point.SE) {
            // 180°
            newX = 18 - x;
            newY = 18 - y;
        } else if (direction == Point.E || direction == Point.SW) {
            // 90° CounterClockwise
            newX = 18 - y;
            newY = x;
        }

        return topLeft.getRelative(newX, newY);
    }

    
    private static String getHoverText(Chunk chunk, Player player, NDFaction playerFaction) {
        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        Location loc = getLocation(chunk, (int) player.getLocation().getY());
        StateFlag.State pvpFlag = getWorldGuardPvpFlag(loc);

        // Verificar se o chunk está fora do limite do mundo
        WorldBorder border = chunk.getWorld().getWorldBorder();
        int blockX = chunk.getX() * 16;
        int blockZ = chunk.getZ() * 16;
        double borderMinX = border.getCenter().getX() - border.getSize() / 2;
        double borderMaxX = border.getCenter().getX() + border.getSize() / 2;
        double borderMinZ = border.getCenter().getZ() - border.getSize() / 2;
        double borderMaxZ = border.getCenter().getZ() + border.getSize() / 2;

        if (blockX < borderMinX || blockX > borderMaxX || blockZ < borderMinZ || blockZ > borderMaxZ) {
            return ChatColor.DARK_GRAY + "Borda do mundo";
        }

        // Terreno dominado por uma facção
        if (terra.getFaction() != null) {
            return ChatColor.WHITE + "[" + terra.getFaction().getTag() + "] " + terra.getFaction().getNome();
        }

        // Zona protegida (ex: spawn ou região segura)
        if (pvpFlag == StateFlag.State.DENY) {
            return ChatColor.GOLD + "Zona protegida";
        }

        // Zona de guerra
        if (pvpFlag == StateFlag.State.ALLOW) {
            return ChatColor.RED + "Zona de Guerra";
        }

        // Terreno livre
        if (ndPlayer != null && ndPlayer.hasFaction()) {
            return ChatColor.GREEN + "Pode dominar";
        }

        return ChatColor.GRAY + "Zona livre";
    }
    public static boolean hasRequiredCargo(NDPlayer player, Cargo... requiredCargos) {
        if (Arrays.asList(requiredCargos).contains(player.getCargo())) return true;
        player.getPlayer().sendMessage(Config.get("Mensagens.SemCargo").toString()
                .replace("&", "§")
                .replace("<cargo>", player.getCargo().toString().toLowerCase()));
        return false;
    }

    private static boolean validateFaction(NDPlayer player, boolean needsFaction) {
        if (needsFaction && !player.hasFaction()) {
            player.getPlayer().sendMessage(Config.get("Mensagens.SemFac").toString().replace("&", "§"));
            return false;
        }
        return true;
    }
    
    public static boolean claimTerritory(Player player, NDPlayer dplayer, int chunkX, int chunkZ) {
        NDPlayer ndPlayer = DataManager.players.get(player.getName());

        // Validação de facção e cargo do jogador
        if (!validateFaction(dplayer, true) || !hasRequiredCargo(dplayer, Cargo.Capitão, Cargo.Lider, Cargo.Membro)) {
            return false;
        }

        if (ndPlayer == null || !ndPlayer.hasFaction()) {
            player.sendMessage(ChatColor.RED + "Você precisa estar em uma facção para reivindicar territórios.");
            return false;
        }

        Chunk chunk = player.getWorld().getChunkAt(chunkX, chunkZ);
        Terra terra = new Terra(chunk.getWorld(), chunkX, chunkZ);
        NDFaction playerFaction = ndPlayer.getFaction();

        // Verifica se o chunk está fora do limite do mundo
        WorldBorder border = player.getWorld().getWorldBorder();
        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;
        double borderMinX = border.getCenter().getX() - border.getSize() / 2;
        double borderMaxX = border.getCenter().getX() + border.getSize() / 2;
        double borderMinZ = border.getCenter().getZ() - border.getSize() / 2;
        double borderMaxZ = border.getCenter().getZ() + border.getSize() / 2;

        if (blockX < borderMinX || blockX > borderMaxX || blockZ < borderMinZ || blockZ > borderMaxZ) {
            player.sendMessage(ChatColor.RED + "Você não pode reivindicar territórios fora do limite do mundo.");
            return false;
        }

        // Verifica se o chunk já é dominado pela facção do jogador
        if (isChunkOwnedByPlayerFaction(terra, player)) return false;

        // Verifica se o chunk está em uma zona protegida ou de guerra
        if (isInProtectedOrWarZone(chunk, player)) return false;

        // Verifica a proximidade de zonas protegidas ou de guerra
        if (isNearProtectedOrWarZone(chunk, player)) return false;

        // Verifica a proximidade de qualquer outra facção
        if (isNearOtherFactionBase(chunk, playerFaction, player)) return false;

        // Verifica o poder da facção para dominar mais territórios
        if (playerFaction.getPoder() < playerFaction.getTerras().size() + 1) {
            player.sendMessage(formatMessage("Mensagens.SemPoderDominar"));
            return false;
        }

        // Verifica se o território está adjacente a outros territórios da facção
        if (!isAdjacentToOwnTerritory(chunk, playerFaction)) {
            player.sendMessage(formatMessage("Mensagens.FaccoesPertos", "sua própria facção"));
            return false;
        }

        // Reivindica o território
        claimChunkForFaction(terra, playerFaction);
        player.sendMessage(formatMessage("Mensagens.TerraDominada"));
        return true;
    }

    private static boolean isChunkOwnedByPlayerFaction(Terra terra, Player player) {
        if (Utils.getProtection(terra.getChunk(), player) == Protecao.Sua) {
            player.sendMessage(formatMessage("Mensagens.JaDominada"));
            return true;
        }
        if (terra.getFaction() != null) {
            player.sendMessage(formatMessage("Mensagens.JaDominada"));
            return true;
        }
        return false;
    }

    private static boolean isInProtectedOrWarZone(Chunk chunk, Player player) {
        Location loc = Utils.getLocation(chunk, (int) player.getLocation().getY());
        StateFlag.State pvpFlag = Utils.getWorldGuardPvpFlag(loc);
        if (pvpFlag == StateFlag.State.DENY || pvpFlag == StateFlag.State.ALLOW) {
            player.sendMessage(formatMessage("Mensagens.FaccoesPertos", pvpFlag == StateFlag.State.DENY ? "zona protegida" : "zona de guerra"));
            return true;
        }
        return false;
    }

    private static boolean isNearProtectedOrWarZone(Chunk chunk, Player player) {
        List<Terra> nearbyProtectedChunks = Utils.getNearbyChunks(chunk, 6);
        for (Terra nearbyTerra : nearbyProtectedChunks) {
            Location nearbyLoc = Utils.getLocation(nearbyTerra.getChunk(), (int) player.getLocation().getY());
            StateFlag.State nearbyPvpFlag = Utils.getWorldGuardPvpFlag(nearbyLoc);
            if (nearbyPvpFlag == StateFlag.State.DENY || nearbyPvpFlag == StateFlag.State.ALLOW) {
                int distance = Utils.getDistance(chunk, nearbyTerra.getChunk());
                if (distance < 6) {
                    String zoneName = nearbyPvpFlag == StateFlag.State.DENY ? "zona protegida" : "zona de guerra";
                    player.sendMessage(formatMessage("Mensagens.FaccoesPertos", zoneName));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isNearOtherFactionBase(Chunk chunk, NDFaction playerFaction, Player player) {
        List<Terra> nearbyChunks = Utils.getNearbyChunks(chunk, 5);
        for (Terra nearbyTerra : nearbyChunks) {
            NDFaction nearbyFaction = nearbyTerra.getFaction();
            if (nearbyFaction != null && !nearbyFaction.equals(playerFaction)) {
                int distance = Utils.getDistance(chunk, nearbyTerra.getChunk());
                if (distance < 5) {
                    player.sendMessage(formatMessage("Mensagens.FaccoesPertos", nearbyFaction.getNome()));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAdjacentToOwnTerritory(Chunk chunk, NDFaction playerFaction) {
        List<Terra> adjacentChunks = Utils.getNearbyChunks(chunk, 1);
        for (Terra nearbyTerra : adjacentChunks) {
            if (playerFaction.equals(nearbyTerra.getFaction())) {
                return true;
            }
        }
        return playerFaction.getTerras().isEmpty(); // Se não tiver terras, não precisa ser adjacente
    }

    private static void claimChunkForFaction(Terra terra, NDFaction playerFaction) {
        List<Terra> newTerras = new ArrayList<>(playerFaction.getTerras());
        newTerras.add(terra);
        playerFaction.setTerras(newTerras);
        terra.setFaction(playerFaction);
    }

    private static String formatMessage(String path) {
        return formatMessage(path, "");
    }

    private static String formatMessage(String path, String replacement) {
        return Config.get(path).toString().replace("&", "§").replace("<nomes>", replacement);
    }
    
    public static int getDistance(Chunk chunk1, Chunk chunk2) {
        int dx = Math.abs(chunk1.getX() - chunk2.getX());
        int dz = Math.abs(chunk1.getZ() - chunk2.getZ());
        return Math.max(dx, dz); // Distância em chunks (baseada no eixo maior)
    }
    
    public static List<Object[]> getMap(Player player, Location location, int width, int height) throws Exception {
        Locations loc = new Locations(location);
        List<Object[]> map = new ArrayList<>();
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        width = halfWidth * 2 + 1;
        height = halfHeight * 2 + 1;

        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        NDFaction playerFaction = ndPlayer != null && ndPlayer.hasFaction() ? ndPlayer.getFaction() : null;

        // Obter o limite do mundo
        WorldBorder border = player.getWorld().getWorldBorder();
        double borderMinX = border.getCenter().getX() - border.getSize() / 2;
        double borderMaxX = border.getCenter().getX() + border.getSize() / 2;
        double borderMinZ = border.getCenter().getZ() - border.getSize() / 2;
        double borderMaxZ = border.getCenter().getZ() + border.getSize() / 2;

        Locations topLeft = loc.getRelative(-halfWidth, -halfHeight);
        for (int dz = 0; dz < height; dz++) {
            Object[] row = new Object[width];
            for (int dx = 0; dx < width; dx++) {
                Locations current = topLeft.getRelative(dx, dz);
                int chunkX = (int) current.getX();
                int chunkZ = (int) current.getZ();

                // Converter coordenadas de chunk para blocos (cada chunk tem 16x16 blocos)
                int blockX = chunkX * 16;
                int blockZ = chunkZ * 16;

                // Verificar se o chunk está dentro do limite do mundo
                if (blockX < borderMinX || blockX > borderMaxX || blockZ < borderMinZ || blockZ > borderMaxZ) {
                    // Chunk fora do limite do mundo, usar um símbolo específico
                    row[dx] = new TextComponent("§0\u2588"); // Preto para indicar fora do limite
                    continue;
                }

                Chunk chunk = player.getWorld().getChunkAt(chunkX, chunkZ);

                String symbol;
                if (dx == halfWidth && dz == halfHeight) {
                    symbol = "§e\u2588"; // Centro
                } else {
                    symbol = "§" + getChunkColor(chunk, player) + "\u2588";
                }

                TextComponent cellComponent = new TextComponent(symbol);

                // Tooltip
                String hoverText = getHoverText(chunk, player, playerFaction);
                if (hoverText != null) {
                    BaseComponent[] hoverComponents = new ComponentBuilder(hoverText).create();
                    cellComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents));
                }

                row[dx] = cellComponent;
            }
            map.add(row);
        }
        return map;
    }

    public static char getChunkColor(Chunk chunk, Player player) {
        // Verificar se o chunk está fora do limite do mundo
        WorldBorder border = chunk.getWorld().getWorldBorder();
        int blockX = chunk.getX() * 16;
        int blockZ = chunk.getZ() * 16;
        double borderMinX = border.getCenter().getX() - border.getSize() / 2;
        double borderMaxX = border.getCenter().getX() + border.getSize() / 2;
        double borderMinZ = border.getCenter().getZ() - border.getSize() / 2;
        double borderMaxZ = border.getCenter().getZ() + border.getSize() / 2;

        if (blockX < borderMinX || blockX > borderMaxX || blockZ < borderMinZ || blockZ > borderMaxZ) {
            return '0'; // Preto para fora do limite
        }

        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        Location loc = getLocation(chunk, (int) player.getLocation().getY());

        if (terra.getChunk().equals(player.getLocation().getChunk())) return 'e';
        StateFlag.State pvpFlag = getWorldGuardPvpFlag(loc);
        if (pvpFlag == StateFlag.State.DENY) return '6';
        if (pvpFlag == StateFlag.State.ALLOW) return '4';
        if (terra.getFaction() == null) return '7';
        if (!ndPlayer.hasFaction()) return 'f';

        NDFaction playerFaction = ndPlayer.getFaction();
        if (terra.isTemporario()) return '9';
        if (playerFaction.equals(terra.getFaction())) return '8';
        if (playerFaction.getInimigos().contains(terra.getFaction())) return 'c';
        if (playerFaction.getAliados().contains(terra.getFaction())) return 'a';
        return 'f';
    }

    public static void showChunkBorders(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        chunk.load(true);
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;
        int y = (int) player.getLocation().getY();

        int[][] corners = {
                {chunkX, chunkZ}, {chunkX + 15, chunkZ}, {chunkX, chunkZ + 15}, {chunkX + 15, chunkZ + 15},
                {chunkX + 7, chunkZ + 15}, {chunkX + 15, chunkZ + 7}, {chunkX, chunkZ + 7}, {chunkX + 7, chunkZ}
        };

        for (int[] corner : corners) {
            for (int i = 0; i < 5; i++) {
                ParticleEffect.EXPLOSION_NORMAL.display(
                        0.0f, 2.0f, 0.0f, 0.0f, 15,
                        chunk.getBlock(corner[0], y + i, corner[1]).getLocation(), player
                );
            }
        }
    }
    
    public static Location getDeserializedLocation(String s) {
		if (s == null || s.length() < 3)
			return null;
		String[] parts = s.split(";");
		double x = Double.parseDouble(parts[0]);
		double y = Double.parseDouble(parts[1]);
		double z = Double.parseDouble(parts[2]);
		float yaw = Float.parseFloat(parts[3]);
		float pitch = Float.parseFloat(parts[4]);
		UUID u = UUID.fromString(parts[5]);
		World w = Bukkit.getServer().getWorld(u);
		return new Location(w, x, y, z, yaw, pitch);
	}
    
    public static String getSerializedLocation(Location loc) {
		if (loc == null)
			return "";
		return loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch() + ";"
				+ loc.getWorld().getUID();
	}
    
    public static List<Terra> getNearbyChunks(Chunk chunk, int distance) {
        List<Terra> nearby = new ArrayList<>();
        int minX = chunk.getX() - distance;
        int maxX = chunk.getX() + distance;
        int minZ = chunk.getZ() - distance;
        int maxZ = chunk.getZ() + distance;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                nearby.add(new Terra(chunk.getWorld(), x, z));
            }
        }
        return nearby;
    }
    
    public static boolean membros(NDPlayer player) {
        Player p = player.getPlayer();
        Inventory inv = Bukkit.createInventory(null, 54, player.getFaction().getNome() + " - Membros");
        inv.setItem(19, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/4e159dab94b55e61b86a3f6fe7f6a4c7d937fe5aa9731293e314747c8f51019d"))
                .setName("§fNível §f§lI")
                .setLore("§7Considerada uma facção pequena,",
                		 "§7tendo que defender sua base de",
                		 "§7um ataque por até 2 horas",
                		 "",
                		 "§4OBS: O ataque e finalizado",
                		 "§4caso não haja um ataque constante.")
                .toItemStack());
        inv.setItem(28, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/e1f27337d1e1a97b67bf33d4517ce0c21de82b70e3bfd45a22201a4d056b600c"))
                .setName("§fNível §f§lII")
                .setLore("§7Considerada uma facção grande,",
               		 	"§7tendo que defender sua base de",
               		 	"§7um ataque por até 3 horas",
               		 	"",
               		 	"§4OBS: O ataque e finalizado",
                		"§4caso não haja um ataque constante.")
                .toItemStack());
        List<ItemStack> players = new ArrayList<>();
        for (NDPlayer all : player.getFaction().getAll()) {
            ItemBuilder builder = new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3)
                    .setSkullOwner(all.getNome())
                    .setName("§7" + all.getNome())
                    .setLore(
                            "§fPoder: §7" + all.getPoder() + "/" + all.getPodermax(),
                            "§fCargo: §7" + all.getCargoSimbolo() + "" + all.getCargo().toString(),
                            "§fKDR: §7" + all.getKDR(),
                            "§fAbates: §7" + all.getKills(),
                            "§fMortes: §7" + all.getMortes(),
                            "§fStatus: " + (Bukkit.getPlayer(all.getNome()) == null ? "§cOffline" : "§aOnline"),
                            "§fÚltimo login: §7" + all.getLast()
                    );

            if (canDemote(player.getPlayer(), all)) {
                builder.addLoreLine("§aClique para mais informações");
            }

            players.add(builder.toItemStack());
        }

        // Preencher slots vazios
        for (int i = 0; i < 15 - player.getFaction().getAll().size(); i++) {
            players.add(new ItemBuilder(Material.SKULL_ITEM, 1, (short) 3).setName("§7Vago").toItemStack());
        }

        int lastIndex = 0;
        int[] slots = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        for (int i = 0; i < slots.length && lastIndex < players.size(); i++) {
            inv.setItem(slots[i], players.get(lastIndex++));
        }

        inv.setItem(49, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/223fb67429716b21bc6e8e7d669ceddf65b13e0790a5ce55b2e077b82d19e124")).setName("§aVoltar").toItemStack());
        p.openInventory(inv);
        return true;
    }
    
    public static void efeito(final Player p) {
        final Location playerLoc = p.getLocation();
        final Chunk chunk = playerLoc.getChunk();
        chunk.load(true);

        final int baseX = chunk.getX() << 4;
        final int baseZ = chunk.getZ() << 4;
        final int baseY = playerLoc.getBlockY();

        // Canto e meios da borda do chunk (8 posições)
        int[][] offsets = {
            {0, 0}, {15, 0}, {0, 15}, {15, 15}, // Cantos
            {7, 0}, {15, 7}, {0, 7}, {7, 15}    // Meios
        };

        for (int[] offset : offsets) {
            int x = baseX + offset[0];
            int z = baseZ + offset[1];
            for (int y = 0; y < 5; y++) {
                Location loc = new Location(p.getWorld(), x + 0.5, baseY + y, z + 0.5);
                ParticleEffect.EXPLOSION_NORMAL.display(0f, 2f, 0f, 0f, 15, loc, p);
            }
        }
    }

    public static Protecao getProtection(Chunk chunk, Player player) {
        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
        Location loc = getLocation(chunk, (int) (player != null ? player.getLocation().getY() : 64)); // Default Y if player is null

        StateFlag.State pvpFlag = getWorldGuardPvpFlag(loc);
        if (pvpFlag == StateFlag.State.DENY) return Protecao.Protegida;
        if (pvpFlag == StateFlag.State.ALLOW) return Protecao.Guerra;
        if (terra.getFaction() == null) return Protecao.Livre;

        if (player == null) {
            // No player context, treat as neutral or free
            return terra.getFaction() != null ? Protecao.Neutra : Protecao.Livre;
        }

        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (!ndPlayer.hasFaction()) return Protecao.Neutra;

        NDFaction playerFaction = ndPlayer.getFaction();
        if (playerFaction.equals(terra.getFaction())) return Protecao.Sua;
        if (playerFaction.getInimigos().contains(terra.getFaction())) return Protecao.Inimiga;
        if (playerFaction.getAliados().contains(terra.getFaction())) return Protecao.Aliada;
        return Protecao.Neutra;
    }
    
    public static Protecao getProtection(Chunk chunk) {
        if (chunk == null) {
            return Protecao.Livre;
        }

        Terra terra = new Terra(chunk.getWorld(), chunk.getX(), chunk.getZ());
        Location loc = getLocation(chunk, 64); // Usa Y=64 como padrão

        StateFlag.State pvpFlag = getWorldGuardPvpFlag(loc);
        if (pvpFlag == StateFlag.State.DENY) {
            return Protecao.Protegida;
        }
        if (pvpFlag == StateFlag.State.ALLOW) {
            return Protecao.Guerra;
        }
        if (terra.getFaction() == null) {
            return Protecao.Livre;
        }

        return Protecao.Neutra;
    }

    public static Location getLocation(Chunk chunk, int y) {
        return chunk.getBlock(0, y, 0).getLocation();
    }

    public static long[] range(long start, long end) {
        int size = (int) Math.abs(end - start) + 1;
        long[] values = new long[size];
        long step = start <= end ? 1 : -1;
        for (int i = 0; i < size; i++) {
            values[i] = start + i * step;
        }
        return values;
    }

    public static String getComparisonString(String str) {
        if (str == null) return "";
        return ChatColor.stripColor(str).toLowerCase()
                .chars()
                .filter(c -> SUBSTANCE_CHARS.contains(String.valueOf((char) c)))
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
    }

}