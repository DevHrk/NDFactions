package me.nd.factions.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;

import me.nd.factions.comandos.AdminCommands;
import me.nd.factions.comandos.Comandos;
import me.nd.factions.enums.Cargo;
import me.nd.factions.enums.Protecao;
import me.nd.factions.enums.Relacao;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.objetos.NDFaction;
import me.nd.factions.objetos.NDPlayer;
import me.nd.factions.objetos.Terra;
import me.nd.factions.utils.ItemBuilder;
import me.nd.factions.utils.Utils;

public class FactionPermissionListener implements Listener {

	private static final Map<String, Integer> memberPageMap = new HashMap<>();

    private static void permissoesMembros(NDPlayer dplayer) {
        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();
        Inventory inv = Bukkit.createInventory(null, 36, "Perm. Membros - [" + faction.getTag() + "]");

        // Get all members and exclude the leader
        List<String> members = new ArrayList<>(faction.getAllMembers());
        String lider = faction.getLiderName();
        if (lider != null) {
            members.remove(lider);
        }

        // Check if there are no members
        if (members.isEmpty()) {
            ItemBuilder noMembers = new ItemBuilder(Material.COBWEB)
                    .setName("§cNenhum Membro")
                    .setLore("§7Não há membros na facção para gerenciar")
                    .addItemFlag(ItemFlag.HIDE_ENCHANTS);
            inv.setItem(13, noMembers.toItemStack());
        } else {
            // Pagination settings
            int membersPerPage = 21; // Slots: 10-16, 19-25, 28-34
            int page = memberPageMap.getOrDefault(player.getName(), 0); // Get current page from map

            // Define valid slots for members
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

            // Calculate start and end indices for the current page
            int startIndex = page * membersPerPage;
            int endIndex = Math.min(startIndex + membersPerPage, members.size());

            // Add members to inventory
            for (int i = startIndex; i < endIndex; i++) {
                String member = members.get(i);
                ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD, 1, (short) 3)
                        .setSkullOwner(member)
                        .setName("§e" + member)
                        .setLore(
                                "§7Gerenciar permissões do membro " + member,
                                "§fClique para abrir"
                        )
                        .addItemFlag(ItemFlag.HIDE_ENCHANTS);
                inv.setItem(slots[i - startIndex], builder.toItemStack());
            }

            // Add navigation arrows if necessary
            if (page > 0) {
                inv.setItem(19, new ItemBuilder(Material.ARROW)
                        .setName("§ePágina Anterior")
                        .setLore("§7Voltar para a página " + page)
                        .addItemFlag(ItemFlag.HIDE_ENCHANTS)
                        .toItemStack());
            }

            if (endIndex < members.size()) {
                inv.setItem(26, new ItemBuilder(Material.ARROW)
                        .setName("§ePróxima Página")
                        .setLore("§7Avançar para a página " + (page + 2))
                        .addItemFlag(ItemFlag.HIDE_ENCHANTS)
                        .toItemStack());
            }
        }

        // Back button
        inv.setItem(27, new ItemBuilder(Material.ARROW)
                .setName("§cVoltar")
                .setLore("§7Voltar ao menu de permissões")
                .toItemStack());

        player.openInventory(inv);
    }

    private static void permissoesMembroEspecifico(NDPlayer dplayer, String memberName) {
        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();
        Inventory inv = Bukkit.createInventory(null, 36, "Perm. Membro - " + memberName + " - [" + faction.getTag() + "]");

        String[] permissoes = {"abrir_bau", "abrir_porta", "apertar_botao", "teleportar", "colocar_bloco", "quebrar_bloco"};
        Material[] materiais = {
                Material.CHEST,
                Material.IRON_DOOR,
                Material.STONE_BUTTON,
                Material.ENDER_PEARL,
                Material.BRICK,
                Material.IRON_PICKAXE
        };
        String[] nomes = {
                "§fAbrir Baú",
                "§fAbrir Porta",
                "§fApertar Botão",
                "§fTeleportar",
                "§fColocar Bloco",
                "§fQuebrar Bloco"
        };

        for (int i = 0; i < permissoes.length; i++) {
            boolean permitido = faction.hasPermissaoMembro(memberName, permissoes[i]);
            ItemBuilder builder = new ItemBuilder(materiais[i])
                    .setName(nomes[i])
                    .setLore(
                            "§7Permissão para " + nomes[i].substring(2).toLowerCase(),
                            "§fEstado: " + (permitido ? "§aPermitido" : "§cNegado"),
                            "§fClique para alternar"
                    )
                    .addItemFlag(ItemFlag.HIDE_ENCHANTS);
            if (permitido) {
                builder.addEnchant(Enchantment.POWER, 1);
            }
            inv.setItem(10 + i, builder.toItemStack());
            inv.setItem(10 + i + 9, new ItemBuilder(permitido ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, 1, permitido ? (short) 5 : (short) 14)
                    .setName(permitido ? "§aAtivado" : "§cDesativado")
                    .toItemStack());
        }

        // Back button
        inv.setItem(31, new ItemBuilder(Material.ARROW)
                .setName("§cVoltar")
                .setLore("§7Voltar ao menu de membros")
                .toItemStack());

        player.openInventory(inv);
    }

    private static void permissoesEspecificas(NDPlayer dplayer, String permissao) {
        Player player = dplayer.getPlayer();
        NDFaction faction = dplayer.getFaction();
        Inventory inv = Bukkit.createInventory(null, 36, "Perm. - " + permissao + " - [" + faction.getTag() + "]");

        // Role permissions
        Cargo[] cargos = {Cargo.Capitão, Cargo.Membro, Cargo.Recruta};
        Material[] cargoMaterials = {Material.DIAMOND_HELMET, Material.IRON_HELMET, Material.LEATHER_HELMET};
        for (int i = 0; i < cargos.length; i++) {
            Cargo cargo = cargos[i];
            boolean permitido = faction.hasPermissao(cargo, permissao);
            ItemBuilder builder = new ItemBuilder(cargoMaterials[i])
                    .setName("§7" + cargo.toString())
                    .setLore(
                            "§7Permissão para " + cargo.toString().toLowerCase() + "s",
                            "§fEstado: " + (permitido ? "§aPermitido" : "§cNegado"),
                            "§fClique para alternar"
                    )
                    .addItemFlag(ItemFlag.HIDE_ENCHANTS);
            if (permitido) {
                builder.addEnchant(Enchantment.POWER, 1);
            }
            inv.setItem(10 + i, builder.toItemStack());
            inv.setItem(10 + i + 9, new ItemBuilder(permitido ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, 1, permitido ? (short) 5 : (short) 14)
                    .setName(permitido ? "§aAtivado" : "§cDesativado")
                    .toItemStack());
        }

        // Relationship permissions
        Relacao[] relacoes = {Relacao.Aliada, Relacao.Neutra, Relacao.Inimiga};
        Color[] relacaoColors = {Color.BLUE, Color.WHITE, Color.RED};
        for (int i = 0; i < relacoes.length; i++) {
            Relacao relacao = relacoes[i];
            boolean permitido = faction.hasPermissaoRelacao(relacao, permissao);
            ItemBuilder builder = new ItemBuilder(Material.LEATHER_CHESTPLATE)
                    .setLeatherArmorColor(relacaoColors[i])
                    .setName(relacao == Relacao.Aliada ? "§aAliada" : relacao == Relacao.Neutra ? "§fNeutra" : "§cInimiga")
                    .setLore(
                            "§7Permissão para facções " + relacao.toString().toLowerCase() + "s",
                            "§fEstado: " + (permitido ? "§aPermitido" : "§cNegado"),
                            "§fClique para alternar"
                    )
                    .addItemFlag(ItemFlag.HIDE_ENCHANTS);
            if (permitido) {
                builder.addEnchant(Enchantment.POWER, 1);
            }
            inv.setItem(14 + i, builder.toItemStack());
            inv.setItem(14 + i + 9, new ItemBuilder(permitido ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE, 1, permitido ? (short) 5 : (short) 14)
                    .setName(permitido ? "§aAtivado" : "§cDesativado")
                    .toItemStack());
        }

        // Back button
        inv.setItem(31, new ItemBuilder(Material.ARROW)
                .setName("§cVoltar")
                .setLore("§7Voltar ao menu de permissões")
                .toItemStack());

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        NDPlayer dplayer = DataManager.players.get(player.getName());
        if (dplayer == null) return;

        if (!event.getView().getTitle().startsWith("Gerenciar Permissões - [") &&
                !event.getView().getTitle().startsWith("Perm. - ") &&
                !event.getView().getTitle().startsWith("Perm. Membros - [") &&
                !event.getView().getTitle().startsWith("Permissões Geral - [") &&
                !event.getView().getTitle().startsWith("Perm. Membro - ")) {
            return;
        }

        event.setCancelled(true);
        int slot = event.getSlot();

        if (event.getView().getTitle().startsWith("Gerenciar Permissões - [")) {
            if (slot == 11) {
                String[] permissoes = {"abrir_bau", "abrir_porta", "apertar_botao", "teleportar", "colocar_bloco", "quebrar_bloco"};
                Inventory geralInv = Bukkit.createInventory(null, 36, "Permissões Geral - [" + dplayer.getFaction().getTag() + "]");
                Material[] materiais = {
                        Material.CHEST,
                        Material.IRON_DOOR,
                        Material.STONE_BUTTON,
                        Material.ENDER_PEARL,
                        Material.BRICK,
                        Material.IRON_PICKAXE
                };
                String[] nomes = {
                        "§fAbrir Baú",
                        "§fAbrir Porta",
                        "§fApertar Botão",
                        "§fTeleportar",
                        "§fColocar Bloco",
                        "§fQuebrar Bloco"
                };
                for (int i = 0; i < permissoes.length; i++) {
                    ItemBuilder builder = new ItemBuilder(materiais[i])
                            .setName(nomes[i])
                            .setLore("§7Clique para gerenciar", "§7permissões para " + nomes[i].substring(2).toLowerCase())
                            .addItemFlag(ItemFlag.HIDE_ENCHANTS);
                    geralInv.setItem(10 + i, builder.toItemStack());
                }
                geralInv.setItem(31, new ItemBuilder(Material.ARROW)
                        .setName("§cVoltar")
                        .setLore("§7Voltar ao menu principal")
                        .toItemStack());
                player.openInventory(geralInv);
            } else if (slot == 15) {
                memberPageMap.put(player.getName(), 0); // Reset page when opening member menu
                permissoesMembros(dplayer);
            } else if (slot == 31) {
                player.closeInventory();
                player.chat("/f");
            }
        } else if (event.getView().getTitle().startsWith("Permissões Geral - [")) {
            String[] permissoes = {"abrir_bau", "abrir_porta", "apertar_botao", "teleportar", "colocar_bloco", "quebrar_bloco"};
            if (slot >= 10 && slot <= 15) {
                int index = slot - 10;
                if (index >= 0 && index < permissoes.length) {
                    permissoesEspecificas(dplayer, permissoes[index]);
                }
            } else if (slot == 31) {
                Comandos.permissoes(dplayer);
            }
        } else if (event.getView().getTitle().startsWith("Perm. - ")) {
            String permissao = event.getView().getTitle().split(" - ")[1].toLowerCase();
            NDFaction faction = dplayer.getFaction();

            // Handle role permissions
            if (slot == 10 && dplayer.getCargo() != Cargo.Lider) {
                player.sendMessage("§cApenas o líder pode alterar permissões de capitães.");
                return;
            }
            Cargo[] cargos = {Cargo.Capitão, Cargo.Membro, Cargo.Recruta};
            if (slot >= 10 && slot <= 12) {
                int index = slot - 10;
                Cargo cargo = cargos[index];
                boolean currentState = faction.hasPermissao(cargo, permissao);
                faction.setPermissao(cargo, permissao, !currentState);
                player.sendMessage("§aPermissão '" + permissao + "' para " + cargo.toString().toLowerCase() + "s foi " +
                        (!currentState ? "ativada" : "desativada"));
                permissoesEspecificas(dplayer, permissao);
            }

            // Handle relationship permissions
            Relacao[] relacoes = {Relacao.Aliada, Relacao.Neutra, Relacao.Inimiga};
            if (slot >= 14 && slot <= 16) {
                int index = slot - 14;
                Relacao relacao = relacoes[index];
                boolean currentState = faction.hasPermissaoRelacao(relacao, permissao);
                faction.setPermissaoRelacao(relacao, permissao, !currentState);
                player.sendMessage("§aPermissão '" + permissao + "' para facções " + relacao.toString().toLowerCase() + "s foi " +
                        (!currentState ? "ativada" : "desativada"));
                permissoesEspecificas(dplayer, permissao);
            }

            // Handle member-specific permissions
            if (slot >= 19 && slot <= 25) {
                int index = slot - 19;
                List<String> members = new ArrayList<>(faction.getAllMembers());
                String lider = faction.getLiderName();
                if (lider != null) {
                    members.remove(lider);
                }
                if (index < members.size()) {
                    String member = members.get(index);
                    NDPlayer targetPlayer = DataManager.players.get(member);
                    if (targetPlayer != null && targetPlayer.getCargo() == Cargo.Capitão && dplayer.getCargo() != Cargo.Lider) {
                        player.sendMessage("§cApenas o líder pode alterar permissões de capitães.");
                        return;
                    }
                    boolean currentState = faction.hasPermissaoMembro(member, permissao);
                    faction.setPermissaoMembro(member, permissao, !currentState);
                    player.sendMessage("§aPermissão '" + permissao + "' para o membro " + member + " foi " +
                            (!currentState ? "ativada" : "desativada"));
                    permissoesEspecificas(dplayer, permissao);
                }
            }

            if (slot == 31) {
                String[] permissoes = {"abrir_bau", "abrir_porta", "apertar_botao", "teleportar", "colocar_bloco", "quebrar_bloco"};
                Inventory geralInv = Bukkit.createInventory(null, 36, "Permissões Geral - [" + faction.getTag() + "]");
                Material[] materiais = {
                        Material.CHEST,
                        Material.IRON_DOOR,
                        Material.STONE_BUTTON,
                        Material.ENDER_PEARL,
                        Material.BRICK,
                        Material.IRON_PICKAXE
                };
                String[] nomes = {
                        "§fAbrir Baú",
                        "§fAbrir Porta",
                        "§fApertar Botão",
                        "§fTeleportar",
                        "§fColocar Bloco",
                        "§fQuebrar Bloco"
                };
                for (int i = 0; i < permissoes.length; i++) {
                    ItemBuilder builder = new ItemBuilder(materiais[i])
                            .setName(nomes[i])
                            .setLore("§7Clique para gerenciar", "§7permissões para " + nomes[i].substring(2).toLowerCase())
                            .addItemFlag(ItemFlag.HIDE_ENCHANTS);
                    geralInv.setItem(10 + i, builder.toItemStack());
                }
                geralInv.setItem(31, new ItemBuilder(Material.ARROW)
                        .setName("§cVoltar")
                        .setLore("§7Voltar ao menu principal")
                        .toItemStack());
                player.openInventory(geralInv);
            }
        } else if (event.getView().getTitle().startsWith("Perm. Membros - [")) {
            List<String> members = new ArrayList<>(dplayer.getFaction().getAllMembers());
            String lider = dplayer.getFaction().getLiderName();
            if (lider != null) {
                members.remove(lider);
            }
            int membersPerPage = 21;
            int page = memberPageMap.getOrDefault(player.getName(), 0);
            int totalPages = (int) Math.ceil((double) members.size() / membersPerPage);
            if (slot == 27) {
                Comandos.permissoes(dplayer);
                memberPageMap.remove(player.getName()); // Clear page data when returning
            }
            if (slot == 19 && page > 0) {
                // Previous page
                memberPageMap.put(player.getName(), page - 1);
                permissoesMembros(dplayer);
            } else if (slot == 26 && (page + 1) < totalPages) {
                // Next page
                memberPageMap.put(player.getName(), page + 1);
                permissoesMembros(dplayer);
            } else if (slot >= 10 && slot <= 34 && (slot <= 16 || slot >= 19)) {
                // Handle member clicks
                int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
                int index = -1;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] == slot) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    index += page * membersPerPage;
                    if (index < members.size()) {
                        String member = members.get(index);
                        permissoesMembroEspecifico(dplayer, member);
                    }
                }
            }
        } else if (event.getView().getTitle().startsWith("Perm. Membro - ")) {
            String memberName = event.getView().getTitle().split(" - ")[1];
            String[] permissoes = {"abrir_bau", "abrir_porta", "apertar_botao", "teleportar", "colocar_bloco", "quebrar_bloco"};
            NDFaction faction = dplayer.getFaction();
            NDPlayer targetPlayer = DataManager.players.get(memberName);

            if (slot >= 10 && slot <= 15) {
                int index = slot - 10;
                if (index < permissoes.length) {
                    String permissao = permissoes[index];
                    if (targetPlayer != null && targetPlayer.getCargo() == Cargo.Capitão && dplayer.getCargo() != Cargo.Lider) {
                        player.sendMessage("§cApenas o líder pode alterar permissões de capitães.");
                        return;
                    }
                    boolean currentState = faction.hasPermissaoMembro(memberName, permissao);
                    faction.setPermissaoMembro(memberName, permissao, !currentState);
                    player.sendMessage("§aPermissão '" + permissao + "' para o membro " + memberName + " foi " +
                            (!currentState ? "ativada" : "desativada"));
                    permissoesMembroEspecifico(dplayer, memberName);
                }
            } else if (slot == 31) {
                permissoesMembros(dplayer);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        if (AdminCommands.adminBypassPlayers.contains(player.getName())) {
            return;
        }

        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (!ndPlayer.hasFaction()) return;

        Terra terra = new Terra(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        Protecao protecao = Utils.getProtection(terra.getChunk(), player);
        if (protecao != Protecao.Sua && protecao != Protecao.Aliada && protecao != Protecao.Neutra && protecao != Protecao.Inimiga) return;

        NDFaction playerFaction = ndPlayer.getFaction();
        NDFaction targetFaction = terra.getFaction();
        Relacao relacao = getRelacao(playerFaction, targetFaction);
        Cargo cargo = ndPlayer.getCargo();

        if (protecao == Protecao.Sua && cargo != Cargo.Lider) {
            if (block.getType() == Material.CHEST) {
                if (!playerFaction.hasPermissaoMembro(player.getName(), "abrir_bau") && !playerFaction.hasPermissao(cargo, "abrir_bau")) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não tem permissão para abrir baús neste território.");
                }
            } else if (isDoor(block.getType())) {
                if (!playerFaction.hasPermissaoMembro(player.getName(), "abrir_porta") && !playerFaction.hasPermissao(cargo, "abrir_porta")) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não tem permissão para abrir portas neste território.");
                }
            } else if (isButton(block.getType())) {
                if (!playerFaction.hasPermissaoMembro(player.getName(), "apertar_botao") && !playerFaction.hasPermissao(cargo, "apertar_botao")) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não tem permissão para apertar botões neste território.");
                }
            }
        } else if (protecao == Protecao.Aliada || protecao == Protecao.Neutra || protecao == Protecao.Inimiga) {
            if (block.getType() == Material.CHEST) {
                if (!targetFaction.hasPermissaoRelacao(relacao, "abrir_bau")) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não tem permissão para abrir baús no território de uma facção " + relacao.toString().toLowerCase() + ".");
                }
            } else if (isDoor(block.getType())) {
                if (!targetFaction.hasPermissaoRelacao(relacao, "abrir_porta")) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não tem permissão para abrir portas no território de uma facção " + relacao.toString().toLowerCase() + ".");
                }
            } else if (isButton(block.getType())) {
                if (!targetFaction.hasPermissaoRelacao(relacao, "apertar_botao")) {
                    event.setCancelled(true);
                    player.sendMessage("§cVocê não tem permissão para apertar botões no território de uma facção " + relacao.toString().toLowerCase() + ".");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (AdminCommands.adminBypassPlayers.contains(player.getName())) {
            return;
        }

        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (!ndPlayer.hasFaction()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() != to.getWorld()) return;

        Terra terraFrom = new Terra(from.getWorld(), from.getChunk().getX(), from.getChunk().getZ());
        Terra terraTo = new Terra(to.getWorld(), to.getChunk().getX(), to.getChunk().getZ());
        Protecao protecaoFrom = Utils.getProtection(terraFrom.getChunk(), player);
        Protecao protecaoTo = Utils.getProtection(terraTo.getChunk(), player);

        NDFaction playerFaction = ndPlayer.getFaction();
        NDFaction targetFaction = terraTo.getFaction();
        Relacao relacao = getRelacao(playerFaction, targetFaction);
        Cargo cargo = ndPlayer.getCargo();

        if (protecaoFrom == Protecao.Sua && protecaoTo != Protecao.Sua && cargo != Cargo.Lider) {
            if (!playerFaction.hasPermissaoMembro(player.getName(), "teleportar") && !playerFaction.hasPermissao(cargo, "teleportar")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não tem permissão para teleportar para fora deste território.");
            }
        } else if (protecaoTo == Protecao.Aliada || protecaoTo == Protecao.Neutra || protecaoTo == Protecao.Inimiga) {
            if (!targetFaction.hasPermissaoRelacao(relacao, "teleportar")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não tem permissão para teleportar para o território de uma facção " + relacao.toString().toLowerCase() + ".");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (AdminCommands.adminBypassPlayers.contains(player.getName())) {
            return;
        }

        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (!ndPlayer.hasFaction()) return;

        Block block = event.getBlockPlaced();
        Terra terra = new Terra(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        Protecao protecao = Utils.getProtection(terra.getChunk(), player);
        if (protecao != Protecao.Sua && protecao != Protecao.Aliada && protecao != Protecao.Neutra && protecao != Protecao.Inimiga) return;

        NDFaction playerFaction = ndPlayer.getFaction();
        NDFaction targetFaction = terra.getFaction();
        Relacao relacao = getRelacao(playerFaction, targetFaction);
        Cargo cargo = ndPlayer.getCargo();

        if (protecao == Protecao.Sua && cargo != Cargo.Lider) {
            if (!playerFaction.hasPermissaoMembro(player.getName(), "colocar_bloco") && !playerFaction.hasPermissao(cargo, "colocar_bloco")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não tem permissão para colocar blocos neste território.");
            }
        } else if (protecao == Protecao.Aliada || protecao == Protecao.Neutra || protecao == Protecao.Inimiga) {
            if (!targetFaction.hasPermissaoRelacao(relacao, "colocar_bloco")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não tem permissão para colocar blocos no território de uma facção " + relacao.toString().toLowerCase() + ".");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (AdminCommands.adminBypassPlayers.contains(player.getName())) {
            return;
        }

        NDPlayer ndPlayer = DataManager.players.get(player.getName());
        if (!ndPlayer.hasFaction()) return;

        Block block = event.getBlock();
        Terra terra = new Terra(block.getWorld(), block.getChunk().getX(), block.getChunk().getZ());
        Protecao protecao = Utils.getProtection(terra.getChunk(), player);
        if (protecao != Protecao.Sua && protecao != Protecao.Aliada && protecao != Protecao.Neutra && protecao != Protecao.Inimiga) return;

        NDFaction playerFaction = ndPlayer.getFaction();
        NDFaction targetFaction = terra.getFaction();
        Relacao relacao = getRelacao(playerFaction, targetFaction);
        Cargo cargo = ndPlayer.getCargo();

        if (protecao == Protecao.Sua && cargo != Cargo.Lider) {
            if (!playerFaction.hasPermissaoMembro(player.getName(), "quebrar_bloco") && !playerFaction.hasPermissao(cargo, "quebrar_bloco")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não tem permissão para quebrar blocos neste território.");
            }
        } else if (protecao == Protecao.Aliada || protecao == Protecao.Neutra || protecao == Protecao.Inimiga) {
            if (!targetFaction.hasPermissaoRelacao(relacao, "quebrar_bloco")) {
                event.setCancelled(true);
                player.sendMessage("§cVocê não tem permissão para quebrar blocos no território de uma facção " + relacao.toString().toLowerCase() + ".");
            }
        }
    }

    public static Relacao getRelacao(NDFaction playerFaction, NDFaction targetFaction) {
        if (targetFaction == null || playerFaction.equals(targetFaction)) {
            return Relacao.Neutra;
        }
        if (playerFaction.isAliada(targetFaction)) {
            return Relacao.Aliada;
        } else if (playerFaction.isInimigo(targetFaction)) {
            return Relacao.Inimiga;
        } else {
            return Relacao.Neutra;
        }
    }

    private boolean isDoor(Material material) {
        return material.name().contains("DOOR") || material == Material.LEGACY_TRAP_DOOR || material == Material.LEGACY_FENCE_GATE;
    }

    private boolean isButton(Material material) {
        return material == Material.STONE_BUTTON || material == Material.LEGACY_WOOD_BUTTON;
    }
}