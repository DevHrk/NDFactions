package me.nd.factions.listeners;

import me.nd.factions.Main;
import me.nd.factions.api.Config;
import me.nd.factions.api.Heads;
import me.nd.factions.banners.Banners;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.utils.ItemBuilder;
import me.nd.factions.utils.Utils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FactionRelacao implements Listener {
    private final Map<Player, Boolean> awaitingTagInput = new HashMap<>();
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        NDPlayer p = DataManager.players.get(player.getName());
        if (p == null) return;

        String invName = e.getInventory().getName();
        if (invName == null) return;

        // Handle new relation menu
        if (invName.startsWith("Relações - [") && !invName.contains(">>")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

            NDFaction faction = p.getFaction();
            if (faction == null || !faction.getLiderName().equals(p.getNome())) return;

            if (e.getSlot() == 11) {
                player.closeInventory();
                player.sendMessage("");
                player.sendMessage("§eQual a tag da facção que desejá alterar a relação?");

                TextComponent cancelMsg = new TextComponent("§7Caso queira cancelar, responda ");
                TextComponent cancelButton = new TextComponent("§7'§c[cancelar]§7'");

                cancelButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "cancelar"));

                cancelButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[] { new TextComponent("§cClique para cancelar a seleção") }));

                cancelMsg.addExtra(cancelButton);
                player.spigot().sendMessage(cancelMsg);
                player.sendMessage("");
                
                awaitingTagInput.put(player, true);
            } else if (e.getSlot() == 13) {
				if (p.getFaction().getPedidosRelacoesAliados().size()
						+ p.getFaction().getPedidosRelacoesNeutras().size() != 0) {
					player.closeInventory();
					Inventory inv = Bukkit.createInventory(null, 36, "Pedidos de relações");
					List<ItemStack> relacoes = new ArrayList<>();
					for (NDFaction f : p.getFaction().getPedidosRelacoesAliados()) {
						relacoes.add(new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.BLUE)
								.setName("§a[" + f.getTag() + "] " + f.getNome())
								.setLore("§7Relação: §aAliada", "§7Poder: §a" + f.getPoder() + "/" + f.getPoderMax(),
										"§7KDR: §a" + f.getKdr(), "§7Membros: §a" + f.getAll().size(), "",
										"§7Botão esquerdo: §fAceitar", "§7Botão direito: §fNegar")
								.toItemStack());
					}

					for (NDFaction f : p.getFaction().getPedidosRelacoesNeutras()) {
						relacoes.add(new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.WHITE)
								.setName("§f[" + f.getTag() + "] " + f.getNome())
								.setLore("§7Relação: §aNeutra", "§7Poder: §a" + f.getPoder() + "/" + f.getPoderMax(),
										"§7KDR: §a" + f.getKdr(), "§7Membros: §a" + f.getAll().size(), "",
										"§7Botão esquerdo: §fAceitar", "§7Botão direito: §fNegar")
								.toItemStack());
					}

					int lastIndex = 0;

					for (int i = 0; i < 54; i++) {
						if (i < 9 || i == 17 || i == 26 || i == 35 || i == 44 || i == 53 || i % 9 == 0)
							continue;
						if (lastIndex >= relacoes.size() || lastIndex > 13)
							break;
						inv.setItem(i, relacoes.get(lastIndex));
						lastIndex++;
					}

					inv.setItem(31, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());

					player.openInventory(inv);
				} else {
					player.getPlayer().sendMessage("§cNão há pedidos de relação.");
				}
            } else if (e.getSlot() == 15) {
                // Log de Relações
                player.closeInventory();
                openRelationLogMenu(player, faction);
            }
        }
        // Handle log menu (read-only)
        else if (e.getView().getTitle().startsWith("Relações - Página")) {
            e.setCancelled(true);
            int slot = e.getSlot();
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

            if (slot == 26) {
                openRelationLogMenuPage(player, p.getFaction(), currentPage + 1);
            } else if (slot == 18) {
                openRelationLogMenuPage(player, p.getFaction(), currentPage - 1);
            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!awaitingTagInput.getOrDefault(player, false)) return;

        e.setCancelled(true);

        String message = e.getMessage().trim();
        // Check if the input is a command
        if (message.startsWith("/")) {
            awaitingTagInput.remove(player);
            player.sendMessage("§cEntrada de tag cancelada devido a um comando.");
            return;
        }

        // Check if the input is "cancelar"
        if (message.equalsIgnoreCase("cancelar")) {
            awaitingTagInput.remove(player);
            player.sendMessage("§cOperação cancelada.");
            return;
        }

        String tag = message.toUpperCase();
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null) {
            awaitingTagInput.remove(player);
            return;
        }

        // Check if the faction exists
        NDFaction targetFaction = Utils.getFactionByTag(tag);
        if (targetFaction == null) {
            player.sendMessage(Config.get("Mensagens.FacNaoExiste").toString().replace("&", "§"));
            awaitingTagInput.remove(player);
            // Keep awaitingTagInput true to allow re-entry
            return;
        }

        // Valid tag, proceed with command execution
        awaitingTagInput.remove(player);
        // Schedule command execution on the main thread
        Bukkit.getScheduler().runTask(Main.get(), () -> {
            player.chat("/f relacao " + tag);
        });
    }

    private static final int[] DISPLAY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private void openRelationLogMenu(Player player, NDFaction faction) {
        openRelationLogMenuPage(player, faction, 0);
    }

    private void openRelationLogMenuPage(Player player, NDFaction faction, int page) {
        List<NDFaction> relations = new ArrayList<>();
        relations.addAll(faction.getAliados());
        relations.addAll(faction.getInimigos());

        if (relations.isEmpty()) {
            // Inventário simples mostrando que não há relações
            Inventory inv = Bukkit.createInventory(null, 27, "Relações - Página 1");
            inv.setItem(13, new ItemBuilder(Material.WEB)
                    .setName("§cNão possui nenhuma relação")
                    .toItemStack());
            player.openInventory(inv);
            // Remove a página salva, pois não tem paginação
            playerPages.remove(player.getUniqueId());
            return;
        }

        int totalPages = (int) Math.ceil((double) relations.size() / DISPLAY_SLOTS.length);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int relationCount = relations.size();
        int lines = relationCount <= 7 ? 3 : relationCount <= 14 ? 5 : 6;
        int size = lines * 9;

        Inventory inv = Bukkit.createInventory(null, size, "Relações - Página " + (page + 1));

        int startIndex = page * DISPLAY_SLOTS.length;
        int endIndex = Math.min(startIndex + DISPLAY_SLOTS.length, relations.size());

        for (int i = startIndex, slotIndex = 0; i < endIndex; i++, slotIndex++) {
            NDFaction rel = relations.get(i);
            // Usa banner customizado para mostrar a tag da facção rel
            ItemStack baseBanner = new ItemStack(Material.BANNER);
            DyeColor baseColor = DyeColor.WHITE;
            DyeColor patternColor = DyeColor.BLACK;

            // Cria o banner com o método Banners.getAlphabet
            ItemStack banner = Banners.getAlphabet(baseBanner, rel.getTag(), baseColor, patternColor);

            // Define o nome e lore no banner usando o ItemBuilder
            String relacao = faction.isAliada(rel) ? "§aAliada" : "§cInimiga";
            String nomeColor = faction.isAliada(rel) ? "§a" : "§c";

            banner = new ItemBuilder(banner)
            	    .setName(nomeColor + "["+rel.getTag()+"] " + rel.getNome())
            	    .setLore(
            	        "§fRelação: §7" + relacao,
            	        "§fTag: §7" + rel.getTag(),
            	        "§fPoder: §7" + rel.getPoder() + "/" + rel.getPoderMax(),
            	        "§fKDR: §7" + rel.getKdr(),
            	        "§fMembros: §7" + rel.getAll().size(), 
            	        ""
            	    )
            	    .toItemStack();

            	// Remove todas as flags do banner
            ItemMeta meta = banner.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_POTION_EFFECTS,
                    ItemFlag.HIDE_UNBREAKABLE
                );
                banner.setItemMeta(meta);
            }

            	inv.setItem(DISPLAY_SLOTS[slotIndex], banner);

        }

        // Paginação
        if (page > 0) {
            inv.setItem(18, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/da1d55b3f989410a34752650e248c9b6c1783a7ec2aa3fd7787bdc4d0e637d39")).setName("§aPágina anterior").toItemStack());
        }
        if (page < totalPages - 1) {
            inv.setItem(26, new ItemBuilder(Heads.getSkull("http://textures.minecraft.net/texture/fa87e3d96e1cfeb9ccfb3ba53a217faf5249e285533b271a2fb284c30dbd9829")).setName("§aPróxima página").toItemStack());
        }

        player.openInventory(inv);

        // Salva contexto para controle da página
        playerPages.put(player.getUniqueId(), page);
    }


}