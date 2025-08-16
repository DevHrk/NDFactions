package me.nd.factions.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.nd.factions.Main;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.utils.ItemBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class FactionsInvite implements Listener {

    private final Map<UUID, Boolean> awaitingInviteInput = new HashMap<>();

    public static void openInviteMenu(Player player) {
    	NDPlayer ndPlayer = DataManager.players.get(player.getName());
    	
        int pendingInvites = 0;
        if (ndPlayer != null && ndPlayer.hasFaction()) {
            NDFaction faction = ndPlayer.getFaction();
            for (NDPlayer target : DataManager.players.values()) {
                if (target.getConvites().contains(faction)) {
                    pendingInvites++;
                }
            }
        }
        Inventory inviteMenu = Bukkit.createInventory(null, 27, "Gerenciar Convites");

        // Botão de Enviar Convite
        inviteMenu.setItem(11, new ItemBuilder(Material.NAME_TAG)
                .setName("§aEnviar Convite")
                .setLore(
                    "§7Clique para enviar um convite para um jogador",
                    "§7juntar-se à sua facção ou use",
                    "§7o comando '§f/f convite enviar <jogador>§7'")
                .toItemStack());

        // Botão de Remover Convites Enviados
        inviteMenu.setItem(15, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName("§eGerenciar Convites")
                .setLore(
                    "§7Sua facção possui §e"+ pendingInvites + " §7convite pendente.",
                    "§7Clique para gerenciar o convite pendente.")
                .toItemStack());

        player.openInventory(inviteMenu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().startsWith("Convites Enviados")) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

            Player clicker = (Player) event.getWhoClicked();
            NDPlayer ndClicker = DataManager.players.get(clicker.getName());
            if (ndClicker == null || !ndClicker.hasFaction()) return;

            NDFaction faction = ndClicker.getFaction();
            ItemMeta meta = clicked.getItemMeta();
            String displayName = meta.getDisplayName();

            // Verifica se é botão de página anterior
            if (clicked.getType() == Material.ARROW && displayName.contains("Página anterior")) {
                int currentPage = getPageFromTitle(event.getView().getTitle());
                int newPage = Math.max(0, currentPage - 1);
                List<NDPlayer> invites = DataManager.players.values().stream()
                        .filter(p -> p.getConvites().contains(faction))
                        .collect(Collectors.toList());
                openInviteManagerMenu(clicker, invites, newPage);
                return;
            }

            // Verifica se é botão de próxima página
            if (clicked.getType() == Material.ARROW && displayName.contains("Próxima página")) {
                int currentPage = getPageFromTitle(event.getView().getTitle());
                int newPage = currentPage + 1;
                List<NDPlayer> invites = DataManager.players.values().stream()
                        .filter(p -> p.getConvites().contains(faction))
                        .collect(Collectors.toList());
                openInviteManagerMenu(clicker, invites, newPage);
                return;
            }

            // Verifica se clicou em um jogador
            if (clicked.getType() == Material.PLAYER_HEAD && displayName.startsWith("§a")) {
                String targetName = displayName.replace("§a", "");
                NDPlayer target = DataManager.players.get(targetName);
                if (target == null || !target.getConvites().contains(faction)) {
                    clicker.sendMessage("§cJogador inválido ou convite já removido.");
                    return;
                }

                switch (event.getClick()) {
                    case LEFT:
                        clicker.sendMessage("§eInformações do jogador §f" + target.getNome() + "§e:");
                        clicker.sendMessage("§7- Online: " + (target.getPlayer() != null ? "§aSim" : "§cNão"));
                        clicker.sendMessage("§7- Nome: §f" + target.getNome());
                        clicker.sendMessage("§7- Poder: §f" + target.getPoder()+"/"+target.getPodermax());
                        clicker.sendMessage("§7- KDR: §f" + target.getKDR());
                        clicker.sendMessage("§7- Abates: §f" + target.getKills());
                        clicker.sendMessage("§7- mortes: §f" + target.getMortes());
                        clicker.closeInventory();
                        break;
                    case RIGHT:
                        target.removeConvite(faction);
                        clicker.sendMessage("§cConvite removido de §f" + target.getNome());

                        // Reabrir a mesma página atualizada
                        int page = getPageFromTitle(event.getView().getTitle());
                        List<NDPlayer> updatedList = DataManager.players.values().stream()
                                .filter(p -> p.getConvites().contains(faction))
                                .collect(Collectors.toList());

                        Bukkit.getScheduler().runTaskLater(Main.get(), () -> {
                            if (!updatedList.isEmpty()) {
                                openInviteManagerMenu(clicker, updatedList, Math.min(page, (updatedList.size() - 1) / 21));
                            } else {
                                clicker.closeInventory();
                                clicker.sendMessage("§7Nenhum convite restante.");
                            }
                        }, 1L);
                        break;
                    default:
                        break;
                }
            }
        }

        if (!event.getView().getTitle().equals("Gerenciar Convites")) return;

        event.setCancelled(true); // Impede que os itens sejam movidos

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

        if (itemName.equals("§aEnviar Convite")) {
            NDPlayer ndPlayer = DataManager.players.get(player.getName());
            if (ndPlayer == null || !ndPlayer.hasFaction()) {
                player.sendMessage("§cVocê não está em uma facção!");
                return;
            }
            NDFaction faction = ndPlayer.getFaction();
            if (faction.getLiderName() == null || (!faction.getLiderName().equals(player.getName()) && !faction.getCapitoes().stream().anyMatch(p -> p.getNome().equals(player.getName())))) {
                player.sendMessage("§cApenas o líder ou capitães podem enviar convites!");
                return;
            }


            player.closeInventory();
            player.sendMessage("");
            player.sendMessage("§eQual o nome do player que você deseja convidar?");

            TextComponent base = new TextComponent("§7Caso queira cancelar, responda ");
            TextComponent cancel = new TextComponent("§c[cancelar]");

            // Clica executa o comando /cancelar
            cancel.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "cancelar"));

            // Passar o mouse mostra "Clique para cancelar"
            cancel.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clique para §ccancelar").create()));

            // Junta as partes
            base.addExtra(cancel);

            // Envia tudo na mesma linha
            player.spigot().sendMessage(base);
            player.sendMessage("");

            awaitingInviteInput.put(player.getUniqueId(), true);
        } else if (itemName.equals("§eGerenciar Convites")) {
            NDPlayer ndPlayer = DataManager.players.get(player.getName());
            if (ndPlayer == null || !ndPlayer.hasFaction()) {
                player.sendMessage("§cVocê não está em uma facção!");
                return;
            }

            NDFaction faction = ndPlayer.getFaction();
            if (faction.getLiderName() == null || (!faction.getLiderName().equals(player.getName()) &&
                    faction.getCapitoes().stream().noneMatch(p -> p.getNome().equals(player.getName())))) {
                player.sendMessage("§cApenas o líder ou capitães podem gerenciar convites!");
                return;
            }

            List<NDPlayer> pendingInvites = DataManager.players.values().stream()
                    .filter(p -> p.getConvites().contains(faction))
                    .collect(Collectors.toList());

            if (pendingInvites.isEmpty()) {
                player.sendMessage("§7Nenhum convite pendente.");
                return;
            }

            int page = 0; // você pode salvar a página em um Map<Player, Integer> depois
            openInviteManagerMenu(player, pendingInvites, page);
        }

    }
    
    public static void openInviteManagerMenu(Player player, List<NDPlayer> invites, int page) {
        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (ndPlayer == null || !ndPlayer.hasFaction()) {
            player.sendMessage("§cVocê não está em uma facção!");
            player.closeInventory();
            return;
        }
        NDFaction faction = ndPlayer.getFaction();
        if (faction == null) {
            player.sendMessage("§cErro: Facção não encontrada!");
            player.closeInventory();
            return;
        }

        Inventory menu = Bukkit.createInventory(null, 54, "Convites Enviados - Página " + (page + 1));

        final int[] inviteSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        int start = page * inviteSlots.length;
        int end = Math.min(start + inviteSlots.length, invites.size());

        for (int i = start; i < end; i++) {
            NDPlayer target = invites.get(i);
            if (target == null || !target.getConvites().contains(faction)) {
                Bukkit.getLogger().warning("Convite inválido encontrado para jogador " + (target != null ? target.getNome() : "nulo") + " na facção " + faction.getNome());
                continue;
            }
            int slot = inviteSlots[i - start];

            String convidado = target.getNome();
            String quemConvidou = target.getQuemConvidou().getOrDefault(faction, "§cDesconhecido");
            long enviadoEm = target.getConviteTimestamp().getOrDefault(faction, 0L);

            // Log warning if inviter or timestamp is missing
            if (quemConvidou.equals("§cDesconhecido")) {
                Bukkit.getLogger().warning("Inviter desconhecido para convite de " + convidado + " na facção " + faction.getNome());
            }
            if (enviadoEm == 0L) {
                Bukkit.getLogger().warning("Timestamp inválido para convite de " + convidado + " na facção " + faction.getNome());
            }

            ItemBuilder item = new ItemBuilder(Material.PLAYER_HEAD, 1, (short) 3)
                .setSkullOwner(convidado)
                .setName("§a" + convidado)
                .setLore(
                    "§7Player convidado: §f" + convidado,
                    "§7Quem convidou: §f" + quemConvidou,
                    "",
                    "§fBotão Direito: §7Deletar convite",
                    "§fBotão Esquerdo: §7Ver informações"
                );

            menu.setItem(slot, item.toItemStack());
        }

        // Botão anterior
        if (page > 0) {
            menu.setItem(18, new ItemBuilder(Material.ARROW)
                .setName("§ePágina anterior")
                .setLore("§7Clique para voltar para a página " + page)
                .toItemStack());
        }

        // Botão próximo
        if (end < invites.size()) {
            menu.setItem(26, new ItemBuilder(Material.ARROW)
                .setName("§ePróxima página")
                .setLore("§7Clique para ir para a página " + (page + 2))
                .toItemStack());
        }

        player.openInventory(menu);
    }

    private int getPageFromTitle(String title) {
        try {
            return Integer.parseInt(title.replace("Convites Enviados - Página ", "")) - 1;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!awaitingInviteInput.getOrDefault(playerId, false)) return;

        event.setCancelled(true); // Cancela a mensagem no chat para não ser vista por outros jogadores
        String message = event.getMessage().trim();
        if (message.equalsIgnoreCase("cancelar")) {
        	awaitingInviteInput.remove(playerId);
            player.sendMessage("§cOperação cancelada.");
            return;
        }
        // Executar a lógica de convite na thread principal
        Bukkit.getScheduler().runTask(Main.get(), () -> {
            processInviteInput(player, message);
            awaitingInviteInput.remove(playerId);
        });

    }

    private void processInviteInput(Player player, String playerName) {
        NDPlayer sender = DataManager.players.get(player.getName());
        if (sender == null || !sender.hasFaction()) {
            player.sendMessage("§cVocê não está em uma facção!");
            return;
        }

        NDFaction senderFaction = sender.getFaction();
        if (senderFaction.getLiderName() == null || (!senderFaction.getLiderName().equals(player.getName()) && !senderFaction.getCapitoes().stream().anyMatch(p -> p.getNome().equals(player.getName())))) {
            player.sendMessage("§cApenas o líder ou capitães podem enviar convites!");
            return;
        }

        NDPlayer target = DataManager.players.get(playerName);
        if (target == null) {
            player.sendMessage("§cJogador §7'§f" + playerName + "§7'§c não encontrado!");
            return;
        }

        if (target.hasFaction()) {
            player.sendMessage("§cO jogador §7'§f" + target.getNome() + "§7'§c já está em uma facção!");
            return;
        }

        if (target.getConvites().contains(senderFaction)) {
            player.sendMessage("§cJá existe um convite pendente para o jogador §7'§f" + target.getNome() + "§7'§c!");
            return;
        }

        // Add the invitation with inviter and timestamp
        target.addConvite(senderFaction, sender.getNome());
        player.sendMessage("§eConvite enviado para o jogador §7'§f" + target.getNome() + "§7'§e!");

        // Notify the invited player, if online, with clickable JSON message
        Player targetPlayer = target.getPlayer();
        if (targetPlayer != null) {
            String factionTag = senderFaction.getTag();
            targetPlayer.spigot().sendMessage(new TextComponent(""));
            TextComponent message = new TextComponent("§eVocê foi convidado por §f" + sender.getNome() + "§e para a facção §f[" + factionTag + "].\n ");

            TextComponent acceptButton = new TextComponent("§a[Aceitar]");
            acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f aceitar convite " + factionTag));
            acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClique para aceitar o convite").create()));

            TextComponent denyButton = new TextComponent(" §c[Negar]");
            denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f recusar convite " + factionTag));
            denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§cClique para recusar o convite").create()));

            message.addExtra(acceptButton);
            message.addExtra(denyButton);

            targetPlayer.spigot().sendMessage(message);
            targetPlayer.spigot().sendMessage(new TextComponent(""));
        }
    }
}