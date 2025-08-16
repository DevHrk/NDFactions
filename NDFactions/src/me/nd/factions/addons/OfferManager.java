package me.nd.factions.addons;

import me.nd.factions.Main;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class OfferManager implements Listener {

    private static final Random random = new Random();
    private static final int OFFER_CHANCE = Main.get().getConfig().getInt("Settings.chance_offer", 30);
    private static final long OFFER_DURATION = Main.get().getConfig().getInt("Settings.time", 10) * 60 * 1000L;
    private static final Map<EntityType, Double> MOB_COSTS = new HashMap<>();

    static {
        for (String key : Main.get().getConfig().getConfigurationSection("Mobs").getKeys(false)) {
            try {
                EntityType mob = EntityType.valueOf(key);
                String costStr = Main.get().getConfig().getString("Mobs." + key);
                double cost = parseCost(costStr);
                MOB_COSTS.put(mob, cost);
            } catch (IllegalArgumentException e) {
                Main.get().getLogger().warning("EntityType inválido na configuração: " + key);
            }
        }
        // Iniciar tarefa para limpar ofertas expiradas
        startOfferCleanupTask();
    }

    private static double parseCost(String costStr) {
        costStr = costStr.toLowerCase().replace("k", "000").replace("m", "000000");
        try {
            return Double.parseDouble(costStr);
        } catch (NumberFormatException e) {
            Main.get().getLogger().warning("Custo inválido: " + costStr);
            return 0.0;
        }
    }

    // Tarefa para limpar ofertas expiradas do arquivo de salvamento
    private static void startOfferCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String factionName : DataManager.getAllFactionNames()) {
                    List<Map<String, Object>> offers = DataManager.loadOffers(factionName);
                    if (offers == null || offers.isEmpty()) continue;

                    boolean modified = false;
                    for (int i = offers.size() - 1; i >= 0; i--) {
                        if (!DataManager.isOfferValid(factionName, i)) {
                            DataManager.removeOffer(factionName, i);
                            modified = true;
                        }
                    }
                    if (modified) {
                        NDFaction faction = DataManager.factions.get(factionName);
                        if (faction != null) {
                            faction.broadcast(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Messages.offer_expired", "&cUma ou mais ofertas expiraram!")));
                        }
                    }
                }
            }
        }.runTaskTimer(Main.get(), 0L, 20L * 60); // Executa a cada 1 minuto
    }

    public static void generateOffer(NDFaction faction, EntityType mob) {
        if (random.nextInt(100) < OFFER_CHANCE) {
            if (!MOB_COSTS.containsKey(mob)) {
                return;
            }
            int amount = random.nextInt(5) + 1;
            double cost = MOB_COSTS.getOrDefault(mob, 50000.0) * amount;
            long expirationTime = System.currentTimeMillis() + OFFER_DURATION;

            DataManager.saveOffer(faction.getNome(), mob, amount, cost, expirationTime);
            faction.broadcast(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Messages.new_offer", "&aNova oferta gerada para a facção!")));
        }
    }

    public static boolean openOfferMenu(Player player) {
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null || ndPlayer.getFaction() == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Messages.no_faction", "&cVocê não está em uma facção!")));
            return true;
        }

        NDFaction faction = ndPlayer.getFaction();
        Inventory inv = Bukkit.createInventory(null, 27, "Ofertas da Facção");

        List<Map<String, Object>> offers = DataManager.loadOffers(faction.getNome());
        if (offers == null || offers.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = emptyItem.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Inventory.empty.name", "&cSem Ofertas")));
            meta.setLore(formatLore(Main.get().getConfig().getStringList("Inventory.empty.lore"), null, 0, 0.0, 0));
            emptyItem.setItemMeta(meta);
            inv.setItem(13, emptyItem);
        } else {
            int slot = 10;
            for (int i = 0; i < offers.size() && slot <= 16; i++) {
                if (!DataManager.isOfferValid(faction.getNome(), i)) {
                    DataManager.removeOffer(faction.getNome(), i);
                    continue;
                }

                Map<String, Object> offer = offers.get(i);
                EntityType mob = (EntityType) offer.get("mob");
                int amount = (int) offer.get("amount");
                double cost = (double) offer.get("cost");
                long expirationTime = (long) offer.get("expirationTime");

                ItemStack offerItem = new ItemStack(Material.MOB_SPAWNER);
                ItemMeta meta = offerItem.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Inventory.item.name", "&aOferta de Spawner")));
                meta.setLore(formatLore(Main.get().getConfig().getStringList("Inventory.item.lore"), mob, amount, cost, expirationTime));
                offerItem.setItemMeta(meta);
                inv.setItem(slot, offerItem);
                slot++;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.getOpenInventory().getTitle().equals("Ofertas da Facção")) {
                        cancel();
                        return;
                    }
                    List<Map<String, Object>> currentOffers = DataManager.loadOffers(faction.getNome());
                    if (currentOffers == null || currentOffers.isEmpty()) {
                        player.closeInventory();
                        player.sendMessage(ChatColor.RED + "Nenhuma oferta disponível!");
                        cancel();
                        return;
                    }
                    int slot = 10;
                    for (int i = 0; i < currentOffers.size() && slot <= 16; i++) {
                        if (!DataManager.isOfferValid(faction.getNome(), i)) {
                            inv.setItem(slot, null);
                            DataManager.removeOffer(faction.getNome(), i);
                            continue;
                        }
                        Map<String, Object> offer = currentOffers.get(i);
                        EntityType mob = (EntityType) offer.get("mob");
                        int amount = (int) offer.get("amount");
                        double cost = (double) offer.get("cost");
                        long expirationTime = (long) offer.get("expirationTime");

                        ItemStack updatedItem = new ItemStack(Material.MOB_SPAWNER);
                        ItemMeta updatedMeta = updatedItem.getItemMeta();
                        updatedMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Inventory.item.name", "&aOferta de Spawner")));
                        updatedMeta.setLore(formatLore(Main.get().getConfig().getStringList("Inventory.item.lore"), mob, amount, cost, expirationTime));
                        updatedItem.setItemMeta(updatedMeta);
                        inv.setItem(slot, updatedItem);
                        slot++;
                    }
                    // Clear remaining slots if fewer offers are available
                    while (slot <= 16) {
                        inv.setItem(slot, null);
                        slot++;
                    }
                }
            }.runTaskTimer(Main.get(), 0L, 20L);
        }

        player.openInventory(inv);
        return true;
    }

    private static List<String> formatLore(List<String> lore, EntityType mob, int amount, double cost, long expirationTime) {
        List<String> formatted = new ArrayList<>();
        long remainingTime = Math.max(0, (expirationTime - System.currentTimeMillis()) / 1000);
        for (String line : lore) {
            line = line.replace("%mob%", mob != null ? mob.name() : "N/A")
                       .replace("%amount%", String.valueOf(amount))
                       .replace("%cost%", String.format("%.2f", cost))
                       .replace("%time%", formatTime(remainingTime));
            formatted.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return formatted;
    }

    private static String formatTime(long seconds) {
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Ofertas da Facção")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.BARRIER) return;

        Player player = (Player) event.getWhoClicked();
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null || ndPlayer.getFaction() == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Messages.no_faction", "&cVocê não está em uma facção!")));
            player.closeInventory();
            return;
        }

        NDFaction faction = ndPlayer.getFaction();
        int slot = event.getSlot();
        int offerIndex = slot - 10;
        List<Map<String, Object>> offers = DataManager.loadOffers(faction.getNome());
        if (offers == null || offerIndex < 0 || offerIndex >= offers.size() || !DataManager.isOfferValid(faction.getNome(), offerIndex)) {
            player.sendMessage(ChatColor.RED + "A oferta expirou ou não está disponível!");
            player.closeInventory();
            return;
        }

        Map<String, Object> offer = offers.get(offerIndex);
        double cost = (double) offer.get("cost");
        EntityType mob = (EntityType) offer.get("mob");
        int amount = (int) offer.get("amount");

        Economy economy = Main.get().getEconomy();
        if (economy == null) {
            player.sendMessage(ChatColor.RED + "Erro: Sistema de economia não encontrado!");
            player.closeInventory();
            return;
        }

        // Verificação e retirada do saldo do jogador
        double playerBalance = economy.getBalance(player);
        if (playerBalance < cost) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Messages.no_money", "&cVocê não tem saldo suficiente!")));
            player.closeInventory();
            return;
        }

        economy.withdrawPlayer(player, cost);

        String command = String.format("sgive %s %s %d", player.getName(), mob.name(), amount);
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', Main.get().getConfig().getString("Messages.bought", "&aOferta comprada com sucesso!")));
        DataManager.removeOffer(faction.getNome(), offerIndex);
        player.closeInventory();
    }

}