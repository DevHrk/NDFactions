package me.nd.factions.utils.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.nd.factions.api.Heads;
import me.nd.factions.enums.Rank;
import me.nd.factions.utils.ItemBuilder;

public class Ranking{

    public ArrayList<Inventory> pages = new ArrayList<Inventory>();
    public UUID id;
    public int currpage = 0;
    public int atual = 1;
    public static HashMap<UUID, Ranking> users = new HashMap<UUID, Ranking>();
    private Rank rank;
   //Running this will open a paged inventory for the specified player, with the items in the arraylist specified.
    public Ranking(ArrayList<ItemStack> items, String name, Player p, Rank r){
        rank = r;

    	this.id = UUID.randomUUID();
    	//create new blank page
        Inventory page = getBlankPage(name);
        atual++;
        //According to the items in the arraylist, add items to the ScrollerInventory
		int slot = 0;
        int a = -1;
        for(int b = 0;b < 1000; b++){
        	if (a +1  == items.size()) {
        		break;
        	}
        	if(slot == 35){
                pages.add(page);
                page = getBlankPage(name);
                atual++;
                slot = 0;
                int i = slot;
                if (i < 9 || i == 17 || i == 26 || i == 35 || i == 44 || i == 53 || i % 9 == 0) continue;
                a++;
				page.setItem(slot,items.get(a));
            }else{
            	slot++;
            	int i = slot;
				if (i < 9 || i == 17 || i == 26 || i == 35 || i == 44 || i == 53 || i % 9 == 0) continue;
				a++;
				page.setItem(slot,items.get(a));
            }
        }
        pages.add(page);
        //open page 0 for the specified player
        p.openInventory(pages.get(currpage));
        users.put(p.getUniqueId(), this);
    }


   //This creates a blank page with the next and prev buttons
    private Inventory getBlankPage(String name){
        Inventory page = Bukkit.createInventory(null, 54, name);

        ItemStack nextpage =  new ItemStack(Material.ARROW, 1);
        ItemMeta meta = nextpage.getItemMeta();
        meta.setDisplayName("§aPagina "+(atual + 1));
        nextpage.setItemMeta(meta);

        ItemStack prevpage = new ItemStack(Material.ARROW, 1);
        meta = prevpage.getItemMeta();
        meta.setDisplayName("§aPagina "+(atual - 1));
        prevpage.setItemMeta(meta);

        page.setItem(47, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());
        switch (this.rank) {
		case COINS:
	        page.setItem(48, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por KDR").setLore("§7Clique para ordernar por KDR.").toItemStack());
	        page.setItem(49, new ItemBuilder(Heads.VERDE.clone()).setName("§eOrdenar por Coins").setLore("§7Clique para ordernar por coins.").toItemStack());
	        page.setItem(50, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Geradores").setLore("§7Clique para ordernar por geradores.").toItemStack());
	        page.setItem(51, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Poder").setLore("§7Clique para ordernar por poder.").toItemStack());
			break;
		case KDR:
	        page.setItem(48, new ItemBuilder(Heads.VERDE.clone()).setName("§eOrdenar por KDR").setLore("§7Clique para ordernar por KDR.").toItemStack());
	        page.setItem(49, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Coins").setLore("§7Clique para ordernar por coins.").toItemStack());
	        page.setItem(50, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Geradores").setLore("§7Clique para ordernar por geradores.").toItemStack());
	        page.setItem(51, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Poder").setLore("§7Clique para ordernar por poder.").toItemStack());
			break;
		case PODER:
	        page.setItem(48, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por KDR").setLore("§7Clique para ordernar por KDR.").toItemStack());
	        page.setItem(49, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Coins").setLore("§7Clique para ordernar por coins.").toItemStack());
	        page.setItem(50, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Geradores").setLore("§7Clique para ordernar por geradores.").toItemStack());
	        page.setItem(51, new ItemBuilder(Heads.VERDE.clone()).setName("§eOrdenar por Poder").setLore("§7Clique para ordernar por poder.").toItemStack());
			break;
		case SPAWNERS:
	        page.setItem(48, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por KDR").setLore("§7Clique para ordernar por KDR.").toItemStack());
	        page.setItem(49, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Coins").setLore("§7Clique para ordernar por coins.").toItemStack());
	        page.setItem(50, new ItemBuilder(Heads.VERDE.clone()).setName("§eOrdenar por Geradores").setLore("§7Clique para ordernar por geradores.").toItemStack());
	        page.setItem(51, new ItemBuilder(Heads.CINZA.clone()).setName("§eOrdenar por Poder").setLore("§7Clique para ordernar por poder.").toItemStack());
			break;
        }
        
        page.setItem(26, nextpage);
        if (atual != 1)
        page.setItem(18, prevpage);
        return page;
    }
}