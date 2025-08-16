package me.nd.factions.listeners;

import me.nd.factions.Main;
import me.nd.factions.addons.SobAtaque;
import me.nd.factions.api.Config;
import me.nd.factions.api.Formatter;
import me.nd.factions.api.Vault;
import me.nd.factions.banners.Banners;
import me.nd.factions.comandos.Comandos;
import me.nd.factions.enums.Cargo;
import me.nd.factions.enums.Motivo;
import me.nd.factions.enums.Protecao;
import me.nd.factions.enums.Rank;
import me.nd.factions.enums.Relacao;
import me.nd.factions.eventos.doFactionPlayerChangeFaction;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.DataManager.FactionRankingType;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.ItemBuilder;
import me.nd.factions.utils.Utils;
import me.nd.factions.utils.menu.Ranking;
import me.nd.factions.utils.menu.ScrollerInventory;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MenuListeners implements Listener {

    public static HashMap<Player, Integer> hash = new HashMap<>();
    public static HashMap<Player, String> nome = new HashMap<>();
    public static HashMap<Player, String> tag = new HashMap<>();
    public static DecimalFormatSymbols DFS = new DecimalFormatSymbols(new Locale("pt", "BR"));

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player) || e.getCurrentItem() == null || e.getSlotType() == SlotType.OUTSIDE) {
            return;
        }

        Player p = (Player) e.getWhoClicked();
        String inventoryName = e.getView().getTitle();
        NDPlayer player = DataManager.players.get(p.getName());

        if (inventoryName.startsWith("Terrenos da")) {
            handleTerrenosMenu(p, e.getSlot());
            e.setCancelled(true);
        } else if (inventoryName.endsWith("Membros")) {
            handleMembrosMenu(p, e.getSlot(), e.getCurrentItem());
            e.setCancelled(true);
        } else if (inventoryName.startsWith("§r§r§e§c§r")) {
            handlePerfilMenu(e, p, e.getSlot(), inventoryName);
            e.setCancelled(true);
        } else if (inventoryName.startsWith("§r§f§a§c§r")) {
            handleFactionMenu(p, player, e.getSlot(), e.isLeftClick(), e.isRightClick(), e.isShiftClick());
            e.setCancelled(true);
        } else if (inventoryName.startsWith("§r§a§b§c§r")) {
            handleCreateFactionMenu(e, p, e.getSlot());
            e.setCancelled(true);
        } else if (inventoryName.equals("Convites de Facções")) {
            handleInvitesMenu(p, e.getSlot(), e.getCurrentItem(), e.isLeftClick());
            e.setCancelled(true);
        } else if (inventoryName.equals("Escolha uma categoria")) {
            handleCategoryMenu(p, e.getSlot());
            e.setCancelled(true);
        } else if (inventoryName.equals("Sair da facção")) {
            handleLeaveFactionMenu(p, e.getSlot(), player);
            e.setCancelled(true);
        } else if (inventoryName.equals("Desfazer facção")) {
            handleDisbandFactionMenu(p, e.getSlot(), player);
            e.setCancelled(true);
        } else if (inventoryName.equals("Expulsar jogador")) {
            handleKickPlayerMenu(p, e.getSlot(), e.getInventory());
            e.setCancelled(true);
        } else if (inventoryName.equals("Transferir liderança")) {
            handleTransferLeadershipMenu(p, e.getSlot(), e.getInventory());
            e.setCancelled(true);
        } else if (inventoryName.startsWith("Relações - [")) {
            handleRelationsMenu(p, e.getSlot(), inventoryName);
            e.setCancelled(true);
        } else if (inventoryName.startsWith("Pedido de aliança - ") ||
                inventoryName.startsWith("Pedido de neutralidade - ") ||
                inventoryName.startsWith("Confirmar neutralidade - ") ||
                inventoryName.startsWith("Declarar inimigo - ")) {
            handleRelationConfirmationMenu(p, e.getSlot(), inventoryName);
            e.setCancelled(true);
        } else if (inventoryName.equals("Pedidos de relações")) {
            handleRelationRequestsMenu(p, e.getSlot(), e.getCurrentItem(), e.isLeftClick());
            e.setCancelled(true);
        } else if (inventoryName.endsWith("- Relações")) {
            handleRelationsOverviewMenu(p, e.getSlot(), player);
            e.setCancelled(true);
        } else if (inventoryName.endsWith("- Inimigas") || inventoryName.endsWith("- Aliadas")) {
            handleAlliesEnemiesMenu(p, e.getSlot(), player);
            e.setCancelled(true);
        } else if (inventoryName.equals("Abandonar Território")) {
            handleAbandonTerrainMenu(p, e.getSlot(), player);
            e.setCancelled(true);
        } else if (inventoryName.equals("Abandonar todos terrenos")) {
            handleAbandonAllTerrainsMenu(p, e.getSlot(), player);
            e.setCancelled(true);
        } else if (isFactionInfoMenu(inventoryName)) {
            handleFactionInfoMenu(p, e.getSlot(), inventoryName);
            e.setCancelled(true);
        } else if (inventoryName.startsWith("§d§5§r")) {
            e.setCancelled(true); // Menu não especificado, cancelar interação
        } else if (inventoryName.equals("Facções online")) {
            ScrollerInventory scrollerInv = ScrollerInventory.users.get(p.getUniqueId());
            if (e.getSlot() == 26 && scrollerInv != null && scrollerInv.currpage < scrollerInv.pages.size() - 1) {
                // Next page
                scrollerInv.currpage++;
                p.openInventory(scrollerInv.pages.get(scrollerInv.currpage));
                e.setCancelled(true);
            } else if (e.getSlot() == 18 && scrollerInv != null && scrollerInv.currpage > 0) {
                // Previous page
                scrollerInv.currpage--;
                p.openInventory(scrollerInv.pages.get(scrollerInv.currpage));
                e.setCancelled(true);
            } else if (e.getSlot() == 49) {
                // Back button
                p.closeInventory();
                p.chat("/f");
                e.setCancelled(true);
            }
        } else if (inventoryName.startsWith("Ranking geral -")) {
            Ranking rankingInv = Ranking.users.get(p.getUniqueId());
            // Paginação
            if (e.getSlot() == 26 && rankingInv != null && rankingInv.currpage < rankingInv.pages.size() - 1) {
                rankingInv.currpage++;
                p.openInventory(rankingInv.pages.get(rankingInv.currpage));
                e.setCancelled(true);
            } else if (e.getSlot() == 18 && rankingInv != null && rankingInv.currpage > 0) {
                rankingInv.currpage--;
                p.openInventory(rankingInv.pages.get(rankingInv.currpage));
                e.setCancelled(true);
            } else if (inventoryName.equals("Ranking geral - Valor")) {
                // Menu de valor: apenas botão voltar
                if (e.getSlot() == 49) {
                    p.closeInventory();
                    openCategoryMenu(p);
                    e.setCancelled(true);
                }
            } else if (e.getSlot() >= 47 && e.getSlot() <= 51) {
                // Navegação entre rankings (exceto "Valor")
                p.closeInventory();

                if (e.getSlot() == 47) {
                    openCategoryMenu(p);
                    e.setCancelled(true);
                } else {
                    Rank rank = e.getSlot() == 48 ? Rank.KDR : e.getSlot() == 49 ? Rank.COINS : e.getSlot() == 50 ? Rank.SPAWNERS : Rank.PODER;
                    List<ItemStack> rankingItems = getRanking(rank);
                    if (rankingItems.isEmpty()) {
                        p.sendMessage("§eO ranking está sendo atualizado, tente novamente em breve!");
                        openCategoryMenu(p);
                        e.setCancelled(true);
                    } else {
                        new Ranking((ArrayList<ItemStack>) rankingItems, "Ranking geral - " + (rank == Rank.SPAWNERS ? "Geradores" : rank.toString()), p, rank);
                        e.setCancelled(true);
                    }
                }
            }
        }

        // Faction item click
        ItemStack item = e.getCurrentItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            String tag = extractTag(displayName);
            if (tag != null && !tag.isEmpty()) {
                p.closeInventory();
                p.chat("/f " + tag);
                e.setCancelled(true);
            } else if (inventoryName.equals("Facções online")) {
                e.setCancelled(true);
                }
            }
        }

    
    

    private void handleTerrenosMenu(Player p, int slot) {
        if (slot == 49) {
            closeInventoryAndShowHelp(p);
        }
    }

    private void handleMembrosMenu(Player p, int slot, ItemStack item) {
        if (slot == 49) {
            closeInventoryAndShowHelp(p);
        } else if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String nome = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            NDPlayer target = DataManager.players.get(nome);
            if (target != null && target.getFaction() == DataManager.players.get(p.getName()).getFaction()) {
                if (Utils.canDemote(p, target)) {
                    p.closeInventory();
                    openMemberProfileMenu(p, target);
                }
            }
        }
    }

    private void handlePerfilMenu(InventoryClickEvent e,Player p, int slot, String inventoryName) {
        String nome = ChatColor.stripColor(inventoryName.replace(" - Perfil", ""));
        NDPlayer player = DataManager.players.get(p.getName());

        if (slot == 21) {
            p.closeInventory();
            p.chat("/f expulsar " + nome);
        } else if (slot == 23) {
            p.closeInventory();
            p.chat("/f " + (e.isLeftClick() ? "promover " : "rebaixar ") + nome);
        } else if (slot == 40 && player.getCargo() == Cargo.Lider) {
            p.closeInventory();
            Utils.membros(player);
        }
    }

    private void handleFactionMenu(Player p, NDPlayer player, int slot, boolean isLeftClick, boolean isRightClick, boolean isShiftClick) {
        switch (slot) {
            case 14:
                p.closeInventory();
                openCategoryMenu(p);
                break;
            case 15:
                p.closeInventory();
                openOnlineFactionsMenu(p);
                break;
            case 16:
                p.closeInventory();
                p.chat("/f ajuda");
                break;
            case 28:
                Utils.membros(player);
                break;
            case 29:
                handleFactionTerritoryActions(p, player, isLeftClick, isShiftClick);
                break;
            case 30:
                handleBaseActions(p, player, isLeftClick, isRightClick, isShiftClick);
                break;
            case 31:
                p.closeInventory();
                openTerrainsMenu(p, player.getFaction());
                break;
            case 32:
                p.chat("/f permissoes");
                break;
            case 37:
                FactionsInvite.openInviteMenu(p);
                break;
            case 38:
                if (isLeftClick) {
                    p.chat("/f mapa");
                } else {
                    player.switchMapa();
                    p.closeInventory();
                    p.chat("/f");
                }
                break;
            case 39:
                player.switchVerTerras();
                p.closeInventory();
                p.chat("/f");
                break;
            case 40:
                p.closeInventory();
                p.chat("/f relacao");
                break;
            case 41:
                p.chat("/f geradores");
                break;
            case 43:
                p.closeInventory();
                p.chat("/f " + (player.getCargo() == Cargo.Lider ? "desfazer" : "sair"));
                break;
        }
    }

    private void handleCreateFactionMenu(InventoryClickEvent e, Player p, int slot) {
        switch (slot) {
            case 14:
                p.closeInventory();
                openCategoryMenu(p);
                break;
            case 15:
                p.closeInventory();
                openOnlineFactionsMenu(p);
                break;
            case 16:
                p.closeInventory();
                p.chat("/f ajuda");
                break;
            case 29:
                hash.put(p, 0);
                p.closeInventory();
                p.sendMessage(new String[]{"", "§aQual será o nome de sua facção?", "§7Caso queira cancelar, responda 'cancelar'.", ""});
                break;
            case 30:
                if (e.isLeftClick()) {
                    p.chat("/f mapa");
                } else {
                    DataManager.players.get(p.getName()).switchMapa();
                    p.closeInventory();
                    p.chat("/f");
                }
                break;
            case 32:
                if (DataManager.players.get(p.getName()).getConvites().isEmpty()) {
                    p.sendMessage(Config.get("Mensagens.SemConvites").toString().replace("&", "§"));
                } else {
                    Utils.openInvitesMenu(p);
                }
                break;
            case 33:
                DataManager.players.get(p.getName()).switchVerTerras();
                p.closeInventory();
                p.chat("/f");
                break;
        }
    }

    private void handleInvitesMenu(Player p, int slot, ItemStack item, boolean isLeftClick) {
        if (slot == 31) {
            closeInventoryAndShowHelp(p);
        } else if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            String tag = ChatColor.stripColor(item.getItemMeta().getDisplayName())
                    .replace("Convite de ", "").replace("[", "").replace("]", "").split(" ")[0];
            NDFaction fac = Utils.getFactionByTag(tag);
            if (fac == null) {
                p.closeInventory();
                Utils.openInvitesMenu(p);
                return;
            }

            p.closeInventory();
            if (isLeftClick) {
                p.chat("/f aceitar convite " + tag);
                closeInventoryAndShowHelp(p);
            } else {
                List<NDFaction> newList = DataManager.players.get(p.getName()).getConvites();
                newList.remove(fac);
                DataManager.players.get(p.getName()).setConvites(newList);
                p.closeInventory();
                if (newList.isEmpty()) {
                    closeInventoryAndShowHelp(p);
                } else {
                    Utils.openInvitesMenu(p);
                }
            }
        }
    }

    private void handleCategoryMenu(Player p, int slot) {
        if (slot == 31) {
            closeInventoryAndShowHelp(p);
        } else if (slot == 12) {
            p.closeInventory();
            openRankingValorMenu(p);
        } else if (slot == 14) {
            p.closeInventory();
            List<NDFaction> ranking = DataManager.getRanking(FactionRankingType.KDR);
            if (ranking.isEmpty()) {
                p.sendMessage("§eO ranking está sendo atualizado, tente novamente em breve!");
                p.closeInventory();
                openCategoryMenu(p);
                return;
            }
            new Ranking(getRanking(Rank.KDR), "Ranking geral - KDR", p, Rank.KDR);
        }
    }

    private void handleLeaveFactionMenu(Player p, int slot, NDPlayer player) {
        if (slot == 15) {
            p.closeInventory();
        } else if (slot == 11) {
            doFactionPlayerChangeFaction evento = new doFactionPlayerChangeFaction(player, Motivo.SAIR);
            Bukkit.getPluginManager().callEvent(evento);

            for (Player all : player.getFaction().getAllOnline()) {
                if (p != all) {
                    all.sendMessage(Config.get("Mensagens.AlguemSaiu").toString()
                            .replace("&", "§").replace("<jogador>", p.getName()));
                }
            }
            p.sendMessage(Config.get("Mensagens.VoceSaiu").toString()
                    .replace("&", "§").replace("<nome>", player.getFaction().getNome()));
            player.getFaction().kick(player);
            p.closeInventory();
        }
    }

    private void handleDisbandFactionMenu(Player p, int slot, NDPlayer player) {
        if (slot == 15) {
            p.closeInventory();
        } else if (slot == 11) {
            NDFaction f = player.getFaction();
            if (f == null) {
                p.sendMessage("§cErro: Facção não encontrada.");
                return;
            }

            if (f.getPlacedGenerator() > 0 || f.getStoreSpawner() > 0) {
                p.sendMessage("§cErro: A facção não pode ser desfeita enquanto houver geradores colocados ou armazenados.");
                return;
            }

            Inventory vault = DataManager.loadVault(f.getNome());
            if (Arrays.stream(vault.getContents()).anyMatch(Objects::nonNull)) {
                p.sendMessage("§cErro: A facção não pode ser desfeita enquanto houver itens no baú da facção.");
                return;
            }

            p.sendMessage(Config.get("Mensagens.FaccaoDesfeita").toString()
                    .replace("&", "§").replace("<nome>", f.getNome()));

            for (NDPlayer member : f.getAll()) {
                doFactionPlayerChangeFaction evento = new doFactionPlayerChangeFaction(member, Motivo.DESFEITA);
                Bukkit.getPluginManager().callEvent(evento);
            }

            f.disband();
            p.closeInventory();
        }
    }

    private void handleKickPlayerMenu(Player p, int slot, Inventory inv) {
        if (slot == 24) {
            p.closeInventory();
        } else if (slot == 20) {
            NDFaction f = DataManager.players.get(p.getName()).getFaction();
            if (f == null) return;

            String nome = ChatColor.stripColor(inv.getItem(13).getItemMeta().getDisplayName());
            NDPlayer target = DataManager.players.get(nome);
            if (target == null) return;

            f.kick(target);
            Player targetPlayer = Bukkit.getPlayer(nome);
            if (targetPlayer != null) {
                targetPlayer.sendMessage("§cVocê foi expulso da facção");
                doFactionPlayerChangeFaction evento = new doFactionPlayerChangeFaction(target, Motivo.EXPULSO);
                Bukkit.getPluginManager().callEvent(evento);
            }

            for (Player all : f.getAllOnline()) {
                all.sendMessage(Config.get("Mensagens.JogadorExpulso").toString()
                        .replace("&", "§").replace("<jogador>", nome));
            }
            p.closeInventory();
        }
    }

    private void handleTransferLeadershipMenu(Player p, int slot, Inventory inv) {
        if (slot == 24) {
            p.closeInventory();
        } else if (slot == 20) {
            NDPlayer lider = DataManager.players.get(p.getName());
            String nome = ChatColor.stripColor(inv.getItem(13).getItemMeta().getDisplayName());
            NDPlayer novoLider = DataManager.players.get(nome);

            if (novoLider == null || lider == null || lider.getFaction() == null) return;

            for (Player all : lider.getFaction().getAllOnline()) {
                all.sendMessage(Config.get("Mensagens.LiderencaTransferida").toString()
                        .replace("&", "§").replace("<lider>", lider.getNome()).replace("<novolider>", novoLider.getNome()));
            }

            lider.getFaction().setLider(novoLider);
            List<NDPlayer> newCapitoes = lider.getFaction().getCapitoes();
            newCapitoes.add(lider);
            lider.getFaction().setCapitoes(newCapitoes);
            p.closeInventory();
        }
    }

    private void handleRelationsMenu(Player p, int slot, String inventoryName) {
        String[] facs = inventoryName.replace("Relações - ", "").replace(" ", "").split(">>");
        String f1Tag = facs[0].replace("[", "").replace("]", "");
        String f2Tag = facs.length > 1 ? facs[1].replace("[", "").replace("]", "") : "";
        NDFaction f1 = Utils.getFactionByTag(f1Tag);
        NDFaction f2 = facs.length > 1 ? Utils.getFactionByTag(f2Tag) : null;

        if (f1 == null || (facs.length > 1 && f2 == null)) return;

        if (facs.length > 1 && Bukkit.getPlayer(f2.getLider().getNome()) == null) {
            p.sendMessage(Config.get("Mensagens.LiderNaoON").toString().replace("&", "§").replace("<nome>", f2.getNome()));
            return;
        }

        if (slot == 11 && f2 != null && !f1.isAliada(f2)) {
            p.closeInventory();
            openRelationConfirmationMenu(p, f2, "Pedido de aliança - " + f2.getTag(), "Deseja solicitar uma aliança com " + f2.getNome());
        } else if (slot == 13 && f2 != null) {
            p.closeInventory();
            if (!f1.isNeutra(f2) && f1.isInimigo(f2)) {
                openRelationConfirmationMenu(p, f2, "Pedido de neutralidade - " + f2.getTag(), "Deseja solicitar neutralidade com " + f2.getNome());
            } else if (!f1.isNeutra(f2) && f1.isAliada(f2)) {
                openRelationConfirmationMenu(p, f2, "Confirmar neutralidade - " + f2.getTag(), "Deseja mudar para neutralidade com " + f2.getNome());
            }
        } else if (slot == 15 && f2 != null && !f1.isInimigo(f2)) {
            p.closeInventory();
            openRelationConfirmationMenu(p, f2, "Declarar inimigo - " + f2.getTag(), "Deseja declarar " + f2.getNome() + " como inimiga");
        }
    }

    private void handleRelationConfirmationMenu(Player p, int slot, String inventoryName) {
        String targetFactionTag = inventoryName.split(" - ")[1];
        NDFaction f1 = DataManager.players.get(p.getName()).getFaction();
        NDFaction f2 = Utils.getFactionByTag(targetFactionTag);

        if (f1 == null || f2 == null || Bukkit.getPlayer(f2.getLider().getNome()) == null) {
            p.sendMessage(Config.get("Mensagens.LiderNaoON").toString().replace("&", "§").replace("<nome>", f2.getNome()));
            return;
        }

        if (slot == 15) {
            p.closeInventory();
            p.sendMessage("§cAção cancelada.");
        } else if (slot == 11) {
            p.closeInventory();
            if (inventoryName.startsWith("Pedido de aliança")) {
                sendRelationRequest(p, f1, f2, "aliança", f2.getPedidosRelacoesAliados());
            } else if (inventoryName.startsWith("Pedido de neutralidade")) {
                sendRelationRequest(p, f1, f2, "neutralidade", f2.getPedidosRelacoesNeutras());
            } else if (inventoryName.startsWith("Confirmar neutralidade")) {
                f1.declarar(Relacao.Neutra, f2);
                f2.declarar(Relacao.Neutra, f1);
                p.sendMessage("§f[" + f2.getTag() + "] " + f2.getNome() + " §eAgora é uma facção §fneutra§e.");
                f2.getLider().getPlayer().sendMessage("§f[" + f1.getTag() + "] " + f1.getNome() + " §edefiniu sua facção como uma facção §fneutra§e.");
            } else if (inventoryName.startsWith("Declarar inimigo")) {
                f1.declarar(Relacao.Inimiga, f2);
                f2.declarar(Relacao.Inimiga, f1);
                p.sendMessage("§f[" + f2.getTag() + "] " + f2.getNome() + " §eAgora é uma facção §cinimiga§e.");
                f2.getLider().getPlayer().sendMessage("§f[" + f1.getTag() + "] " + f1.getNome() + " §edefiniu sua facção como uma facção §cinimiga§e.");
            }
            try {
                f1.save();
                f2.save();
            } catch (Exception ex) {
                Bukkit.getLogger().severe("Erro ao salvar facções após mudança de relação: " + ex.getMessage());
            }
        }
    }

    private void handleRelationRequestsMenu(Player p, int slot, ItemStack item, boolean isLeftClick) {
        if (slot == 31) {
            closeInventoryAndShowHelp(p);
        } else if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getType() == Material.LEATHER_CHESTPLATE) {
            String tag = ChatColor.stripColor(item.getItemMeta().getDisplayName()).split(" ")[0].replace("[", "").replace("]", "");
            NDFaction relacao = Utils.getFactionByTag(tag);
            if (relacao == null) return;

            LeatherArmorMeta im = (LeatherArmorMeta) item.getItemMeta();
            Relacao r = im.getColor().asBGR() == Color.WHITE.asBGR() ? Relacao.Neutra : Relacao.Aliada;

            p.closeInventory();
            NDFaction playerFaction = DataManager.players.get(p.getName()).getFaction();
            if (isLeftClick) {
                playerFaction.declarar(r, relacao);
                relacao.declarar(r, playerFaction);
                p.sendMessage("§aVocê aceitou o pedido de " + tag);
            } else {
                playerFaction.removerPedidos(relacao);
                p.sendMessage("§cVocê rejeitou o pedido de " + tag);
            }
            closeInventoryAndShowHelp(p);
        }
    }

    private void handleRelationsOverviewMenu(Player p, int slot, NDPlayer player) {
        if (slot == 31) {
            closeInventoryAndShowHelp(p);
        } else if (slot == 11) {
            p.sendMessage(Config.get("Mensagens.SemRelacoes").toString().replace("&", "§"));
        } else if (slot == 13) {
            p.closeInventory();
            openAlliesMenu(p, player.getFaction());
        } else if (slot == 15) {
            p.closeInventory();
            openEnemiesMenu(p, player.getFaction());
        }
    }

    private void handleAlliesEnemiesMenu(Player p, int slot, NDPlayer player) {
        if (slot == 49) {
            p.closeInventory();
            openRelationsMenu(p, player.getFaction());
        }
    }

    private void handleAbandonTerrainMenu(Player p, int slot, NDPlayer player) {
        if (slot == 24) {
            p.closeInventory();
            return;
        }

        if (slot != 20) {
            return;
        }

        NDFaction faction = player.getFaction();
        if (faction == null) {
            p.sendMessage("§cErro: Você não está em uma facção.");
            p.closeInventory();
            return;
        }
        
        Chunk chunks = p.getLocation().getChunk();
        Terra terra = new Terra(chunks.getWorld(), chunks.getX(), chunks.getZ());
        if (!faction.ownsTerritory(terra)) {
            p.sendMessage("§cEste terreno não pertence à sua facção.");
            p.closeInventory();
            return;
        }

        // Verifica se o terreno conecta outros terrenos
        if (isTerrainBetweenConnectedTerrains(terra, faction)) {
            p.sendMessage("§cVocê não pode abandonar este terreno, pois ele conecta outros terrenos da sua facção!");
            p.closeInventory();
            return;
        }

        // Verifica geradores (spawners) no terreno
        Chunk chunk = terra.getChunk();
        World world = chunk.getWorld();
        int cx = chunk.getX() << 4, cz = chunk.getZ() << 4;
        boolean hasSpawners = false;
        outerLoop:
        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                for (int y = 0; y < world.getMaxHeight(); y++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.SPAWNER) {
                        hasSpawners = true;
                        break outerLoop;
                    }
                }
            }
        }

        if (hasSpawners) {
            p.sendMessage("§cVocê não pode abandonar este terreno enquanto houver geradores colocados!");
            p.closeInventory();
            return;
        }

        // Verifica proteção por bloco usando WorldGuard
        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                Location loc = new Location(world, x, world.getHighestBlockYAt(x, z), z);
                Protecao protection = Utils.getProtection(loc.getChunk(), p);
                if (protection == Protecao.Protegida || protection == Protecao.Guerra) {
                    p.sendMessage("§cVocê não pode abandonar este terreno enquanto houver blocos protegidos ou em zona de guerra!");
                    p.closeInventory();
                    return;
                }
            }
        }

        // Remove o terreno
        List<Terra> terras = faction.getTerras();
        List<Terra> temporarios = faction.getTemporarios();
        boolean removed = terras.remove(terra) || temporarios.remove(terra);

        if (!removed) {
            p.sendMessage("§cErro ao abandonar o terreno. Tente novamente.");
            p.closeInventory();
            return;
        }

        // Remove homes no terreno
        Map<String, Location> homes = DataManager.loadHomes(faction.getNome());
        boolean homeRemoved = homes.entrySet().removeIf(entry ->
                entry.getValue().getChunk().getX() == terra.getX() &&
                        entry.getValue().getChunk().getZ() == terra.getZ() &&
                        entry.getValue().getWorld().getUID().equals(terra.getWorld().getUID()));

        try {
            if (homeRemoved) {
                DataManager.saveHomes(faction.getNome(), homes);
            }
            faction.setTerras(terras);
            faction.setTemporarios(temporarios);
            faction.save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar facção ou homes ao abandonar terreno: " + e.getMessage());
            p.sendMessage("§cErro ao salvar alterações. Contate um administrador.");
            p.closeInventory();
            return;
        }

        p.sendMessage(Config.get("Mensagens.TerraAbandonada").toString().replace("&", "§"));
        p.closeInventory();
    }
    
    private void handleAbandonAllTerrainsMenu(Player p, int slot, NDPlayer player) {
        if (slot == 24) {
            p.closeInventory();
        } else if (slot == 20) {
            if (!player.getFaction().getPlacedGenerators().isEmpty()) {
                p.sendMessage("§cVocê não pode abandonar todos os terrenos enquanto houver geradores colocados na facção!");
                p.closeInventory();
                return;
            }

            p.sendMessage(Config.get("Mensagens.TerraAbandonada").toString().replace("&", "§"));
            player.getFaction().setTerras(new ArrayList<>());
            player.getFaction().setTemporarios(new ArrayList<>());
            DataManager.removeHomes(player.getFaction().getNome());

            try {
                player.getFaction().save();
            } catch (Exception ex) {
                Bukkit.getLogger().severe("Erro ao salvar facção após abandonar todos os terrenos: " + ex.getMessage());
            }
            p.closeInventory();
        }
    }

    private boolean isFactionInfoMenu(String inventoryName) {
        String stripped = ChatColor.stripColor(inventoryName);
        return stripped.startsWith("[") && stripped.contains("]");
    }

    private void handleFactionInfoMenu(Player p, int slot, String inventoryName) {
        String tag = extractTag(ChatColor.stripColor(inventoryName));
        if (tag == null || tag.isEmpty()) {
            return;
        }

        if (slot == 15 || slot == 22) {
            p.closeInventory();
            p.chat("/f membros " + tag);
        } else if (slot == 25) {
            p.closeInventory();
            p.chat("/f relacao " + tag);
        }
    }

    private void closeInventoryAndShowHelp(Player p) {
        p.closeInventory();
        new BukkitRunnable() {
            @Override
            public void run() {
                Comandos.help(DataManager.players.get(p.getName()));
            }
        }.runTaskLater(Main.getPlugin(Main.class), 2L);
    }

    private void openMemberProfileMenu(Player p, NDPlayer target) {
        Inventory inv = Bukkit.createInventory(null, 45, "§r§r§e§c§r" + target.getNome() + " - Perfil");
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD, 1, 3)
                .setSkullOwner(target.getNome())
                .setName("§7" + target.getNome())
                .setLore("", "§fPoder: §7" + target.getPoder() + "/" + target.getPodermax(),
                        "§fCargo: §7" + target.getCargo().toString(),
                        "§fKDR: §7" + target.getKDR(), "§fAbates: §7" + target.getKills(),
                        "§fMortes: §7" + target.getMortes(),
                        "§fStatus: " + (Bukkit.getPlayer(target.getNome()) == null ? "§cOffline" : "§aOnline"),
                        "§fÚltimo login: §7" + target.getLast())
                .toItemStack());
        inv.setItem(21, new ItemBuilder(Material.RED_WOOL, 1, 14)
                .setName("§cExpulsar")
                .setLore("§7Remover este jogador", "§7permanentemente da facção.")
                .toItemStack());
        inv.setItem(23, new ItemBuilder(Material.IRON_HELMET)
                .setName("§aCargo")
                .setLore("§7Altere o cargo desse jogador", "§7dentro da facção.", "",
                        "§7Botão esquerdo: §fpromover", "§7Botão direito: §frebaixar")
                .toItemStack());
        inv.setItem(40, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());
        p.openInventory(inv);
    }

    private void openCategoryMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 36, "Escolha uma categoria");
        inv.setItem(12, new ItemBuilder(Material.NETHER_STAR)
                .setName("§aRanking de Valor")
                .setLore("§7Veja as facções com mais valor", "§7no servidor")
                .toItemStack());
        inv.setItem(14, new ItemBuilder(Material.GRASS_BLOCK)
                .setName("§aRanking Geral")
                .setLore("§7Veja as facções com o melhor", "§7desempenho de modo geral no servidor.")
                .toItemStack());
        inv.setItem(31, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());
        p.openInventory(inv);
    }

    private void openOnlineFactionsMenu(Player p) {
        ArrayList<NDFaction> onlineFactions = (ArrayList<NDFaction>) Utils.getOnlineFactions();
        if (onlineFactions.isEmpty()) {
            p.sendMessage("§eNão há facções online no momento!");
            p.closeInventory();
            p.chat("/f");
            return;
        }

        ArrayList<ItemStack> items = new ArrayList<>();
        onlineFactions.forEach(f -> items.add(Utils.getFactionBanner(f)));
        new ScrollerInventory(items, "Facções online", p);
    }

    private void openRankingValorMenu(Player p) {
        List<NDFaction> ranking = DataManager.getRanking(FactionRankingType.VALOR);
        if (ranking.isEmpty()) {
            p.sendMessage("§eO ranking está sendo atualizado, tente novamente em breve!");
            p.closeInventory();
            openCategoryMenu(p);
            return;
        }

        ArrayList<ItemStack> items = new ArrayList<>();
        int position = 1;

        for (NDFaction faction : ranking) {
            List<String> lore = new ArrayList<>(Arrays.asList(
                    "§fValor da Facção: §6" + Formatter.formatNumber(faction.getMoneyTotal() + faction.getTotalMoneyEmSpawners()),
                    "",
                    "§fTotal em Coins: §7" + Formatter.formatNumber(faction.getMoneyTotal()),
                    "§fTotal em Geradores: §7" + Formatter.formatNumber(faction.getTotalMoneyEmSpawners()),
                    "§f • Armazenados: §7" + faction.getTotalStoredGenerators(),
                    "§f • Colocados: §7" + faction.getTotalPlacedGenerator()
            ));

            double spawnerValor = faction.getMoneyEmSpawners();
            double threshold = Main.get().getConfig().getDouble("Geral.SpawnerCoordenadas");
            if (spawnerValor >= threshold) {
                List<Terra> allTerras = new ArrayList<>();
                allTerras.addAll(faction.getTerras());
                allTerras.addAll(faction.getTemporarios());
                if (!allTerras.isEmpty()) {
                    Terra primeiraTerra = allTerras.get(0);
                    int blockX = primeiraTerra.getX() * 16;
                    int blockZ = primeiraTerra.getZ() * 16;
                    lore.addAll(Arrays.asList("", "§fCoordenadas:", "§f • §7X: " + blockX + ", Z: " + blockZ));
                }
            }

            ItemStack banner = Banners.getAlphabet(new ItemStack(Material.WHITE_BANNER), faction.getTag(), DyeColor.WHITE, DyeColor.BLACK);
            items.add(new ItemBuilder(banner)
                    .addItemFlag(ItemFlag.HIDE_DYE)
                    .setName("§f" + position + "º §7[" + faction.getTag() + "] " + faction.getNome())
                    .setLore(lore)
                    .toItemStack());
            position++;
        }

        new ScrollerInventory(items, "Ranking geral - Valor", p);
    }


    private void openTerrainsMenu(Player p, NDFaction faction) {
        Inventory inv = Bukkit.createInventory(null, 54, "Terrenos da " + faction.getNome());
        List<ItemStack> terras = new ArrayList<>();
        int index = 0;

        // Combine permanent and temporary terrains
        List<Terra> allTerras = new ArrayList<>();
        allTerras.addAll(faction.getTerras());
        allTerras.addAll(faction.getTemporarios());

        // Check if the faction is under attack
        boolean isUnderAttack = faction.isSobAtaque();
        long attackStartTime = SobAtaque.getAttackStartTime(faction);

        for (Terra t : allTerras) {
            Location loc = t.getChunk().getBlock(8, 64, 8).getLocation();
            // Use different materials for permanent and temporary terrains
            Material itemMaterial = t.isTemporario() ? Material.DIRT : (isUnderAttack ? Material.TNT : Material.GRASS_BLOCK);

            // Build lore
            List<String> lore = new ArrayList<>(Arrays.asList(
                "§7Localização: §aX: " + (int) loc.getX() + " Z: " + (int) loc.getZ(),
                "§7Situação: " + (t.isTemporario() ? "§aSeguro" : (isUnderAttack ? "§cSob Ataque!" : "§aSeguro"))
            ));

            // Add attack details only for permanent terrains if under attack
            if (isUnderAttack && !t.isTemporario() && attackStartTime > 0) {
                lore.add("");
                // Calculate remaining attack time
                long elapsedTime = System.currentTimeMillis() - attackStartTime;
                long remainingTime = elapsedTime;
                if (remainingTime > 0) {
                    // Convert to hours, minutes, seconds
                    long totalSeconds = remainingTime / 1000;
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;

                    // Format with leading zeros and include hours or minutes if applicable, else show seconds only
                    String timeString = hours > 0
                        ? String.format("%dh %02dm %02ds", hours, minutes, seconds)
                        : (minutes > 0
                            ? String.format("%02dm %02ds", minutes, seconds)
                            : String.format("%02ds", seconds));
                    lore.add("§7Tempo em ataque: §e" + timeString);
                } else {
                    lore.add("§7Tempo: §eExpirado");
                }
            }

            ItemBuilder itemBuilder = new ItemBuilder(itemMaterial)
                    .setName("§aTerra #" + (++index) + (t.isTemporario() ? " §7(Temporária)" : " §7(Permanente)"))
                    .setLore(lore.toArray(new String[0]));
            terras.add(itemBuilder.toItemStack());
        }

        int lastIndex = 0;
        for (int i = 0; i < 54 && lastIndex < terras.size(); i++) {
            if (i < 9 || i == 17 || i == 26 || i == 35 || i == 44 || i == 53 || i % 9 == 0) continue;
            inv.setItem(i, terras.get(lastIndex++));
        }

        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());
        p.openInventory(inv);
    }



    private void openRelationsMenu(Player p, NDFaction faction) {
        Inventory inv = Bukkit.createInventory(null, 36, faction.getNome() + " - Relações");
        inv.setItem(11, new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setLeatherArmorColor(Color.WHITE)
                .setAmount(0)
                .setName("§fNeutra")
                .setLore("§cSua facção não possui relações neutras.")
                .toItemStack());
        inv.setItem(13, new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setLeatherArmorColor(Color.BLUE)
                .setAmount(faction.getAliados().size())
                .setName("§aAliada")
                .setLore(faction.getAliados().isEmpty()
                        ? "§cSua facção não possui relações aliadas."
                        : "§aClique para ver suas relações aliadas.")
                .toItemStack());
        inv.setItem(15, new ItemBuilder(Material.LEATHER_CHESTPLATE)
                .setLeatherArmorColor(Color.RED)
                .setAmount(faction.getInimigos().size())
                .setName("§cInimiga")
                .setLore(faction.getInimigos().isEmpty()
                        ? "§cSua facção não possui relações inimigas."
                        : "§aClique para ver suas relações inimigas.")
                .toItemStack());
        inv.setItem(31, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());
        p.openInventory(inv);
    }

    private void openAlliesMenu(Player p, NDFaction faction) {
        Inventory inv = Bukkit.createInventory(null, 54, faction.getNome() + " - Aliadas");
        List<ItemStack> aliados = faction.getAliados().stream()
                .map(f -> new ItemBuilder(Material.LEATHER_CHESTPLATE)
                        .setLeatherArmorColor(Color.BLUE)
                        .setName("§7[" + f.getTag() + "] " + f.getNome())
                        .toItemStack())
                .collect(Collectors.toList());

        int lastIndex = 0;
        for (int i = 0; i < 54 && lastIndex < aliados.size(); i++) {
            if (i < 9 || i == 17 || i == 26 || i == 35 || i == 44 || i == 53 || i % 9 == 0) continue;
            inv.setItem(i, aliados.get(lastIndex++));
        }

        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());
        p.openInventory(inv);
    }

    private void openEnemiesMenu(Player p, NDFaction faction) {
        Inventory inv = Bukkit.createInventory(null, 54, faction.getNome() + " - Inimigas");
        List<ItemStack> inimigos = faction.getInimigos().stream()
                .map(f -> new ItemBuilder(Material.LEATHER_CHESTPLATE)
                        .setLeatherArmorColor(Color.RED)
                        .setName("§7[" + f.getTag() + "] " + f.getNome())
                        .toItemStack())
                .collect(Collectors.toList());

        int lastIndex = 0;
        for (int i = 0; i < 54 && lastIndex < inimigos.size(); i++) {
            if (i < 9 || i == 17 || i == 26 || i == 35 || i == 44 || i == 53 || i % 9 == 0) continue;
            inv.setItem(i, inimigos.get(lastIndex++));
        }

        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("§aVoltar").toItemStack());
        p.openInventory(inv);
    }

    private void openRelationConfirmationMenu(Player p, NDFaction f2, String title, String lore) {
        Inventory inv = Bukkit.createInventory(null, 27, title);
        inv.setItem(11, new ItemBuilder(Material.GREEN_WOOL, 1, 5)
                .setName("§aAceitar")
                .setLore("§7" + lore)
                .toItemStack());
        inv.setItem(15, new ItemBuilder(Material.RED_WOOL, 1, 14)
                .setName("§cNegar")
                .setLore("§7Cancelar esta operação")
                .toItemStack());
        p.openInventory(inv);
    }

    private void sendRelationRequest(Player p, NDFaction f1, NDFaction f2, String relacao, List<NDFaction> pedidos) {
        p.sendMessage(Config.get("Mensagens.PedidoEnviado").toString()
                .replace("&", "§").replace("<relacao>", "§" + (relacao.equals("aliança") ? "a" : "f") + relacao)
                .replace("<nome>", f2.getNome()).replace("<tag>", f2.getTag()));

        String mensagemBase = Config.get("Mensagens.PedidoRecebido").toString()
                .replace("&", "§").replace("<relacao>", "§" + (relacao.equals("aliança") ? "a" : "f") + relacao)
                .replace("<nome>", f1.getNome()).replace("<tag>", f1.getTag());

        TextComponent linha11 = new TextComponent("");
        TextComponent linha1 = new TextComponent(mensagemBase);
        TextComponent linha2 = new TextComponent("§f[Clique para ver]");
        linha2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f relacao verpedidos"));
        linha2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{new TextComponent("§eClique para ver o pedido de relação")}));

        Player lider = f2.getLider().getPlayer();
        lider.spigot().sendMessage(linha11);
        lider.spigot().sendMessage(linha1);
        lider.spigot().sendMessage(linha2);
        lider.spigot().sendMessage(linha11);
        pedidos.add(f1);
    }

    private void handleFactionTerritoryActions(Player p, NDPlayer player, boolean isLeftClick, boolean isShiftClick) {
        Protecao protection = Utils.getProtection(player.getPlayer().getLocation().getChunk(), player.getPlayer());
        boolean isLeader = player.getCargo() == Cargo.Lider;
        boolean isAuthorized = isLeader || player.getCargo() == Cargo.Membro || player.getCargo() == Cargo.Capitão;

        if (isShiftClick && isLeftClick && isLeader) {
            p.closeInventory();
            p.chat("/f abandonar todas");
        } else if (isAuthorized) {
            if (protection == Protecao.Sua && isLeftClick) {
                p.closeInventory();
                p.chat("/f abandonar");
            } else if (protection == Protecao.Livre) {
                p.closeInventory();
                p.chat(isLeftClick ? "/f dominar" : "/f proteger");
            }
        }
    }

    private void handleBaseActions(Player p, NDPlayer player, boolean isLeftClick, boolean isRightClick, boolean isShiftClick) {
        if (isLeftClick) {
            p.closeInventory();
            p.chat("/f base");
        } else if ((player.getCargo() == Cargo.Capitão || player.getCargo() == Cargo.Lider) && isRightClick) {
            if (!isShiftClick) {
                if (Utils.getProtection(p.getLocation().getChunk(), p) != Protecao.Sua) {
                    p.sendMessage("§cVocê só pode marcar a base de sua facção em seus territórios");
                } else {
                    p.sendMessage(Config.get("Mensagens.BaseSetada").toString().replace("&", "§"));
                    player.getFaction().setBase(p.getLocation());
                    p.closeInventory();
                    p.chat("/f");
                }
            } else {
                p.sendMessage(Config.get("Mensagens.BaseRemovida").toString().replace("&", "§"));
                player.getFaction().setBase(null);
                p.closeInventory();
                p.chat("/f");
            }
        }
    }

    private String extractTag(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }
        // Match [TAG] with alphanumeric and common special characters
        Matcher matcher = Pattern.compile("\\[([A-Za-z0-9!@#$%^&*()_\\-+=]+)\\]").matcher(displayName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Fallback: split on brackets
        int start = displayName.indexOf('[');
        int end = displayName.indexOf(']');
        if (start != -1 && end != -1 && start < end) {
            String tag = displayName.substring(start + 1, end).trim();
            if (!tag.isEmpty()) {
                return tag;
            }
        }
        return null;
    }

    public static ArrayList<ItemStack> getRanking(Rank r) {
        ArrayList<ItemStack> items = new ArrayList<>();
        List<NDFaction> ranking;
        int position = 1;

        switch (r) {
            case COINS:
                ranking = DataManager.getRanking(FactionRankingType.COINS);
                if (ranking.isEmpty()) {
                    return items;
                }
                for (NDFaction faction : ranking) {
                    List<String> lores = new ArrayList<>(Arrays.asList(
                            "",
                            "§7Membros:")); // Empty line for separation
                    lores.addAll(faction.getAll().stream()
                            .map(player -> " §f• " + player.getNome() + ": §7" + Formatter.formatNumber(Vault.getPlayerBalance(player.getNome())))
                            .collect(Collectors.toList()));
                    items.add(createRankingItem(faction, position++, "§fCoins: §7" + Formatter.formatNumber(faction.getMoneyTotal()), lores));
                }
                break;
            case KDR:
                ranking = DataManager.getRanking(FactionRankingType.KDR);
                if (ranking.isEmpty()) {
                    return items;
                }
                for (NDFaction faction : ranking) {
                    items.add(createRankingItem(faction, position++, "§fKDR: §7" + faction.getKdr(),
                            Arrays.asList("", "§fAbates: §7" + faction.getAbates(), "§fMortes: §7" + faction.getMortes())));
                }
                break;
            case PODER:
                ranking = DataManager.getRanking(FactionRankingType.PODER);
                if (ranking.isEmpty()) {
                    return items;
                }
                for (NDFaction faction : ranking) {
                    List<String> lores = new ArrayList<>(Arrays.asList(
                            "", // Empty line for separation
                            "§7Membros:")); // Members header
                    lores.addAll(faction.getAll().stream()
                            .map(player -> " §f• " + player.getNome() + ": §7" + player.getPoder() + "/" + player.getPodermax())
                            .collect(Collectors.toList()));
                    items.add(createRankingItem(faction, position++, "§fpoder: §7" + faction.getPoder() + "/" + faction.getPoderMax(), lores));
                }
                break;
            case SPAWNERS:
                ranking = DataManager.getRanking(FactionRankingType.SPAWNERS);
                if (ranking.isEmpty()) {
                    return items;
                }
                for (NDFaction faction : ranking) {
                    List<String> lores = new ArrayList<>(Arrays.asList(
                            "§fTotal de geradores: §7" + Formatter.formatNumber(faction.getTotalGenerators()),
                            "  §fColocados: §7" + faction.getTotalPlacedGenerator(),
                            "  §fArmazenados: §7" + faction.getTotalStoredGenerators(),
                            "",
                            "§fColocados:"));
                    lores.addAll(faction.getNameGenerators());
                    items.add(createRankingItem(faction, position++, null, lores));
                }
                break;
        }
        return items;
    }
    private static ItemStack createRankingItem(NDFaction faction, int position, String mainLore, List<String> additionalLores) {
        ItemBuilder builder = new ItemBuilder(Banners.getAlphabet(new ItemStack(Material.WHITE_BANNER), faction.getTag(), DyeColor.WHITE, DyeColor.BLACK))
                .addItemFlag(ItemFlag.HIDE_DYE)
                .setName("§f" + position + "º §7[" + faction.getTag() + "] " + faction.getNome());
        if (mainLore != null) {
            builder.setLore(mainLore);
        }
        builder.addLores(additionalLores);
        return builder.toItemStack();
    }

    private boolean isTerrainBetweenConnectedTerrains(Terra terra, NDFaction faction) {
        World world = terra.getWorld();
        int x = terra.getX(), z = terra.getZ();
        List<Terra> allTerrains = new ArrayList<>();
        allTerrains.addAll(faction.getTerras());
        allTerrains.addAll(faction.getTemporarios());

        boolean hasNorth = false, hasSouth = false, hasEast = false, hasWest = false;
        for (Terra t : allTerrains) {
            if (!t.equals(terra) && t.getWorld().getUID().equals(world.getUID())) {
                if (t.getX() == x && t.getZ() == z - 1) hasNorth = true;
                if (t.getX() == x && t.getZ() == z + 1) hasSouth = true;
                if (t.getX() == x + 1 && t.getZ() == z) hasEast = true;
                if (t.getX() == x - 1 && t.getZ() == z) hasWest = true;
            }
        }
        return (hasNorth && hasSouth) || (hasEast && hasWest);
    }
}