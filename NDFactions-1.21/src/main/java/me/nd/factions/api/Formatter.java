package me.nd.factions.api;

import org.bukkit.configuration.file.*;

import me.nd.factions.Main;

import java.util.*;
import java.text.*;
import java.util.regex.*;

public class Formatter {
	
	static FileConfiguration config1 = Main.get().getConfig();
    @SuppressWarnings("unused")
	private static Main main;
    private static final Pattern PATTERN;
    @SuppressWarnings("unused")
	private static FileConfiguration config;
    private static List<String> suffixes;
    
    public static void changeSuffixes(final List<String> suffixes) {
    	Formatter.suffixes = suffixes;
    }
    
    public static String formatNumber(double value) {
        int index;
        double tmp;
        for (index = 0; (tmp = value / 1000.0) >= 1.0; value = tmp, ++index) {}
        final DecimalFormat decimalFormat = new DecimalFormat("#.#");
        return String.valueOf(decimalFormat.format(value)) + Formatter.suffixes.get(index);
    }
    
    public static double parseString(final String value) throws Exception {
        try {
            return Double.parseDouble(value);
        }
        catch (Exception exception) {
            final Matcher matcher = Formatter.PATTERN.matcher(value);
            if (!matcher.find()) {
                throw new Exception("Invalid format");
            }
            final double amount = Double.parseDouble(matcher.group(1));
            final String suffix = matcher.group(2);
            final int index = Formatter.suffixes.indexOf(suffix.toUpperCase());
            return amount * Math.pow(1000.0, index);
        }
    }
    
    static {
    	Formatter.main = (Main)Main.getPlugin((Class)Main.class);
        PATTERN = Pattern.compile("^(\\d+\\.?\\d*)(\\D+)");
        Formatter.config = Main.get().getConfig();
        Formatter.suffixes = (List<String>)Formatter.config1.getStringList("Formatação");
    }
}