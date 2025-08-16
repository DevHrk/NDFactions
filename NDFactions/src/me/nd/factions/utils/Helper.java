package me.nd.factions.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * @author phaed
 */
public class Helper {

    private Helper() {
    }

    /**
     * Check for integer
     *
     * @param o
     * @return
     */
    public static boolean isInteger(Object o) {
        return o instanceof java.lang.Integer;
    }

	public static String format(double coins) {
		boolean isWholeNumber = coins == (double)Math.round(coins);
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
		formatSymbols.setDecimalSeparator('.');
		String pattern = isWholeNumber ? "###,###.###" : "###,##0.00";
		DecimalFormat df = new DecimalFormat(pattern, formatSymbols);
		return df.format(coins);
	}
    
	@SuppressWarnings("unused")
    public static boolean isDouble(String o) {
    	try {
			double d = Double.parseDouble(o);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return false;
    }

    /**
     * Check for byte
     *
     * @param input
     * @return
     */
    public static boolean isByte(String input) {
        try {
            Byte.parseByte(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Check for short
     *
     * @param input
     * @return
     */
    public static boolean isShort(String input) {
        try {
            Short.parseShort(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Check for integer
     *
     * @param input
     * @return
     */
    public static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Check for float
     *
     * @param input
     * @return
     */
    public static boolean isFloat(String input) {
        try {
            Float.parseFloat(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Check for string
     *
     * @param o
     * @return
     */
    public static boolean isString(Object o) {
        return o instanceof java.lang.String;
    }

    /**
     * Check for boolean
     *
     * @param o
     * @return
     */
    public static boolean isBoolean(Object o) {
        return o instanceof java.lang.Boolean;
    }

    /**
     * Remove a character from a string
     *
     * @param s
     * @param c
     * @return
     */
    public static String removeChar(String s, char c) {
        String r = "";

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != c) {
                r += s.charAt(i);
            }
        }

        return r;
    }

    /**
     * Remove first character from a string
     *
     * @param s
     * @param c
     * @return
     */
    public static String removeFirstChar(String s, char c) {
        String r = "";

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != c) {
                r += s.charAt(i);
                break;
            }
        }

        return r;
    }

    /**
     * Capitalize first word of sentence
     *
     * @param content
     * @return
     */
    public static String capitalize(String content) {
        if (content.length() < 2) {
            return content;
        }

        String first = content.substring(0, 1).toUpperCase();
        return first + content.substring(1);
    }

    /**
     * Return plural word if count is bigger than one
     *
     * @param count
     * @param word
     * @param ending
     * @return
     */
    public static String plural(int count, String word, String ending) {
        return count == 1 ? word : word + ending;
    }

    /**
     * Hex value to ChatColor
     *
     * @param hexValue
     * @return
     */
    public static String toColor(String hexValue) {
        if (hexValue == null) {
            return "";
        }

        return ChatColor.getByChar(hexValue).toString();
    }

    /**
     * Converts string array to ArrayList<String>, remove empty strings
     *
     * @param values
     * @return
     */
    public static List<String> fromArray(String... values) {
        List<String> results = new ArrayList<>();
        Collections.addAll(results, values);
        results.remove("");
        return results;
    }

    /**
     * Converts string array to HashSet<String>, remove empty strings
     *
     * @param values
     * @return
     */
    public static Set<String> fromArray2(String... values) {
        HashSet<String> results = new HashSet<>();
        Collections.addAll(results, values);
        results.remove("");
        return results;
    }

    /**
     * Converts a player array to ArrayList<Player>
     *
     * @param values
     * @return
     */
    public static List<Player> fromPlayerArray(Player... values) {
        List<Player> results = new ArrayList<>();
        Collections.addAll(results, values);
        return results;
    }

    /**
     * Converts ArrayList<String> to string array
     *
     * @param list
     * @return
     */
    public static String[] toArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    /**
     * Removes first item from a string array
     *
     * @param args
     * @return
     */
    public static String[] removeFirst(String[] args) {
        List<String> out = fromArray(args);

        if (!out.isEmpty()) {
            out.remove(0);
        }
        return toArray(out);
    }

    /**
     * Converts a string array to a space separated string
     *
     * @param args
     * @return
     */
    public static String toMessage(String[] args) {
        String out = "";

        for (String arg : args) {
            out += arg + " ";
        }

        return out.trim();
    }

    /**
     * Converts a string array to a string with custom separators
     *
     * @param args
     * @param sep
     * @return
     */
    public static String toMessage(String[] args, String sep) {
        String out = "";

        for (String arg : args) {
            out += arg + ", ";
        }

        return stripTrailing(out, ", ");
    }

    /**
     * Converts a string array to a string with custom separators
     *
     * @param args
     * @param sep
     * @return
     */
    public static String toMessage(List<String> args, String sep) {
        String out = "";

        for (String arg : args) {
            out += arg + sep;
        }

        return stripTrailing(out, sep);
    }

    /**
     * Convert color hex values with ampersand to special character
     *
     * @param msg
     * @return
     */
    public static String parseColors(String msg) {
        return msg.replace("&", "\u00a7");
    }

    /**
     * Removes color codes from strings
     *
     * @param msg
     * @return
     */
    public static String stripColors(String msg) {
        String out = msg.replaceAll("[&][0-9a-f]", "");
        out = out.replaceAll(String.valueOf((char) 194), "");
        return out.replaceAll("[\u00a7][0-9a-f]", "");
    }

    /*
     * Retrieves the last color code @param msg @return
     */

    /**
     * @param msg
     * @return
     */
    public static String getLastColorCode(String msg) {
        msg = msg.replaceAll(String.valueOf((char) 194), "").trim();

        if (msg.length() < 2) {
            return "";
        }

        String one = msg.substring(msg.length() - 2, msg.length() - 1);
        String two = msg.substring(msg.length() - 1);

        if (one.equals("\u00a7")) {
            return one + two;
        }

        if (one.equals("&")) {
            return Helper.toColor(two);
        }


        return "";
    }

    /**
     * Cleans up the tag from color codes and makes it lowercase
     *
     * @param tag
     * @return
     */
    public static String cleanTag(String tag) {
        return stripColors(tag).toLowerCase();
    }

    /**
     * Removes trailing separators
     *
     * @param msg
     * @param sep
     * @return
     */
    public static String stripTrailing(String msg, String sep) {
        if (msg.length() < sep.length()) {
            return msg;
        }

        String out = msg;
        String first = msg.substring(0, sep.length());
        String last = msg.substring(msg.length() - sep.length(), msg.length());

        if (first.equals(sep)) {
            out = msg.substring(sep.length());
        }

        if (last.equals(sep)) {
            out = msg.substring(0, msg.length() - sep.length());
        }

        return out;
    }

    /**
     * Generates page separator line
     *
     * @param sep
     * @return
     */
    public static String generatePageSeparator(String sep) {
        String out = "";

        for (int i = 0; i < 320; i++) {
            out += sep;
        }
        return out;
    }

    /**
     * Check whether a player is online
     *
     * @param playerName
     * @return
     */
    @Deprecated
    public static boolean isOnline(String playerName) {
        Collection<Player> online = getOnlinePlayers();

        for (Player o : online) {
            if (o.getName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Check whether a player is online
     *
     * @param playerUniqueId
     * @return
     */
    public static boolean isOnline(UUID playerUniqueId) {
        Collection<Player> online = getOnlinePlayers();

        for (Player o : online) {
            if (o.getUniqueId().equals(playerUniqueId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Test if a url is valid
     *
     * @param strUrl
     * @return
     */
    public static boolean testURL(String strUrl) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.connect();

            if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Escapes single quotes
     *
     * @param str
     * @return
     */
    public static String escapeQuotes(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''");
    }

    /**
     * Returns a prettier coordinate, does not include world
     *
     * @param loc
     * @return
     */
    public static String toLocationString(Location loc) {
        return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + loc.getWorld().getName();
    }

    /**
     * Whether the two locations refer to the same block
     *
     * @param loc
     * @param loc2
     * @return
     */
    public static boolean isSameBlock(Location loc, Location loc2) {
        return loc.getBlockX() == loc2.getBlockX() && loc.getBlockY() == loc2.getBlockY() && loc.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Whether the two locations refer to the same location, ignoring pitch and
     * yaw
     *
     * @param loc
     * @param loc2
     * @return
     */
    public static boolean isSameLocation(Location loc, Location loc2) {
        return loc.getX() == loc2.getX() && loc.getY() == loc2.getY() && loc.getZ() == loc2.getZ();
    }

    /**
     * Sort hashmap by value
     *
     * @param map
     * @return Map
     */
	public static Map sortByValue(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {

			@Override
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static boolean isVanished(Player player) {
        if (player != null && player.hasMetadata("vanished") && !player.getMetadata("vanished").isEmpty()) {
            return player.getMetadata("vanished").get(0).asBoolean();
        }
        return false;
    }

	public static Collection<Player> getOnlinePlayers() {
        try {
            Method method = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
            Object players = method.invoke(null);

            if (players instanceof Player[]) {
                return new ArrayList<>(Arrays.asList((Player[]) players));
            } else {
                return (Collection<Player>) players;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

	public static String toMessage(String[] args, int start) {
	    if (args == null || start >= args.length) return "";
	    return String.join(" ", Arrays.copyOfRange(args, start, args.length));
	}

}
