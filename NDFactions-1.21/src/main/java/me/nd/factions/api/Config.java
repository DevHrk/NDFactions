package me.nd.factions.api;

import java.util.HashMap;
import java.util.Map;

import me.nd.factions.Main;

public class Config {

		public static Map<String, Object> cache = new HashMap<>();
		
		public static Object get(String path){
			Object value = cache.get(path);
			if (value==null){
				value = Main.getPlugin(Main.class).getConfig().get(path);
				cache.put(path, value);
			}
			return value;
		}
}