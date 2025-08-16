package me.nd.factions.runnables;

//import com.gmail.nossr50.database.DatabaseManager;
//import com.gmail.nossr50.database.DatabaseManagerFactory;
//import com.gmail.nossr50.datatypes.database.PlayerStat;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class McMMO extends BukkitRunnable {
	public static HashMap<String, Integer> PLAYER_POSITION = new HashMap<>();

	public static String getPlayerPosition(Player p) {
		return PLAYER_POSITION.get(p.getName()) == null ? "N/A"
				: ((Integer) PLAYER_POSITION.get(p.getName())).toString() + "�";
	}

	public void run() {
		// List<PlayerStat> list =
		// DatabaseManagerFactory.getDatabaseManager().readLeaderboard(null, 1, 10000);
		// for (int i = 0; i < list.size(); i++) {
		// PLAYER_POSITION.put(((PlayerStat)list.get(i)).name, Integer.valueOf(i + 1));
		// }
	}
}
