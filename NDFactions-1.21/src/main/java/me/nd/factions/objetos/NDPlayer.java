package me.nd.factions.objetos;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.nd.factions.enums.Cargo;
import me.nd.factions.mysql.DataManager;
import me.nd.factions.mysql.Methods;

public class NDPlayer {

    private String nome;
    private String faction;
    private int kills, mortes, poder, podermax;
    private long online;
    private boolean mapaligado, verterras;
    private List<NDFaction> convites;
    private Map<NDFaction, String> quemConvidou; // Tracks who invited the player to each faction
    private Map<NDFaction, Long> conviteTimestamps; // Tracks when each invitation was sent

    public NDPlayer(String nome, String faction, int kills, int mortes, int poder, int podermax, long online) {
        this.nome = nome;
        this.faction = faction != null ? faction : "";
        this.kills = kills;
        this.mortes = mortes;
        this.poder = poder;
        this.podermax = podermax;
        this.online = online;
        this.convites = new ArrayList<>();
        this.quemConvidou = new HashMap<>();
        this.conviteTimestamps = new HashMap<>();
    }

    public String getNome() {
        return nome;
    }

    public List<NDFaction> getConvites() {
        return new ArrayList<>(convites);
    }

    public Terra getTerraAtual() {
        Player player = Bukkit.getPlayer(nome);
        if (player == null) return null;
        return new Terra(player.getWorld(), player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
    }

    public void setConvites(List<NDFaction> convites) {
        this.convites = new ArrayList<>(convites != null ? convites : new ArrayList<>());
        // Synchronize quemConvidou and conviteTimestamps
        quemConvidou.keySet().retainAll(this.convites);
        conviteTimestamps.keySet().retainAll(this.convites);
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar convites de NDPlayer " + nome + ": " + e.getMessage());
        }
    }

    public void addConvite(NDFaction faction, String inviter) {
        if (faction != null && !convites.contains(faction)) {
            convites.add(faction);
            quemConvidou.put(faction, inviter != null ? inviter : "Desconhecido");
            conviteTimestamps.put(faction, System.currentTimeMillis());
            try {
                save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao adicionar convite para NDPlayer " + nome + ": " + e.getMessage());
            }
        }
    }

    public void removeConvite(NDFaction faction) {
        if (faction != null) {
            convites.remove(faction);
            quemConvidou.remove(faction);
            conviteTimestamps.remove(faction);
            try {
                save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao remover convite de NDPlayer " + nome + ": " + e.getMessage());
            }
        }
    }

    public Map<NDFaction, String> getQuemConvidou() {
        return new HashMap<>(quemConvidou);
    }

    public Map<NDFaction, Long> getConviteTimestamp() {
        return new HashMap<>(conviteTimestamps);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(nome);
    }
    
    public boolean isInOwnTerritory() {
        if (!hasFaction()) {
            return false; // Jogador sem facção não pode estar em seu próprio território
        }
        Terra currentTerra = getTerraAtual();
        if (currentTerra == null) {
            return false; // Jogador offline ou localização inválida
        }
        NDFaction faction = getFaction();
        return faction.ownsTerritory(currentTerra);
    }
    
    public boolean isNone() {
        return getFaction() == null;
    }

    public boolean isMapaLigado() {
        return mapaligado;
    }

    public void setLigado(boolean b) {
        mapaligado = b;
    }

    public void switchMapa() {
        setLigado(!mapaligado);
    }

    public boolean isVerTerras() {
        return verterras;
    }

    public void setLigadoVerTerras(boolean b) {
        verterras = b;
    }

    public void switchVerTerras() {
        setLigadoVerTerras(!verterras);
    }

    public boolean hasFaction() {
        return getFaction() != null;
    }

    public void save() throws Exception {
        Methods.updatePlayer(this);
    }

    public String getLast() {
        Date date = new Date(online);
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return format.format(date);
    }

    public long getLastMilis() {
        return online;
    }

    public void setLast() {
        this.online = System.currentTimeMillis();
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar última conexão de NDPlayer " + nome + ": " + e.getMessage());
        }
    }

    public Cargo getCargo() {
        if (!hasFaction()) return Cargo.Nenhum;

        NDFaction faction = getFaction();
        if (faction.getLider() != null && faction.getLider().equals(this)) return Cargo.Lider;
        if (faction.getCapitoes().contains(this)) return Cargo.Capitão;
        if (faction.getMembros().contains(this)) return Cargo.Membro;
        if (faction.getRecrutas().contains(this)) return Cargo.Recruta;
        return Cargo.Recruta;
    }

    public String getCargoSimbolo() {
        if (!hasFaction()) return "";
        NDFaction faction = getFaction();
        if (faction.getLider() == this) return "#";
        if (faction.getCapitoes().contains(this)) return "*";
        if (faction.getMembros().contains(this)) return "+";
        if (faction.getRecrutas().contains(this)) return "-";
        return "";
    }

    public void setNome(String nome) {
        if (nome != null) {
            this.nome = nome;
            try {
                save();
            } catch (Exception e) {
                Bukkit.getLogger().severe("Erro ao salvar nome de NDPlayer " + this.nome + ": " + e.getMessage());
            }
        }
    }

    public NDFaction getFaction() {
        return faction.isEmpty() ? null : DataManager.factions.get(faction);
    }

    public void setFaction(NDFaction faction) {
        this.faction = (faction == null) ? "" : faction.getNome();
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar facção de NDPlayer " + nome + ": " + e.getMessage());
        }
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar kills de NDPlayer " + nome + ": " + e.getMessage());
        }
    }

    public double getKDR() {
        return mortes == 0 ? kills : (double) kills / mortes;
    }

    public int getMortes() {
        return mortes;
    }

    public void setMortes(int mortes) {
        this.mortes = mortes;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar mortes de NDPlayer " + nome + ": " + e.getMessage());
        }
    }

    public int getPoder() {
        return poder;
    }

    public void setPoder(int poder) {
        this.poder = poder;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar poder de NDPlayer " + nome + ": " + e.getMessage());
        }
    }

    public int getPodermax() {
        return podermax;
    }

    public void setPodermax(int podermax) {
        this.podermax = podermax;
        try {
            save();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao salvar podermax de NDPlayer " + nome + ": " + e.getMessage());
        }
    }

    public void setCargo(Cargo novoCargo) {
        if (!hasFaction()) return;

        NDFaction faction = getFaction();
        Cargo cargoAtual = getCargo();

        if (cargoAtual == Cargo.Lider) {
            faction.setLider(null);
        } else if (cargoAtual == Cargo.Capitão) {
            faction.getCapitoes().remove(this);
        } else if (cargoAtual == Cargo.Membro) {
            faction.getMembros().remove(this);
        } else if (cargoAtual == Cargo.Recruta) {
            faction.getRecrutas().remove(this);
        }

        switch (novoCargo) {
            case Lider:
                faction.setLider(this);
                break;
            case Capitão:
                faction.getCapitoes().add(this);
                break;
            case Membro:
                faction.getMembros().add(this);
                break;
            case Recruta:
                faction.getRecrutas().add(this);
                break;
            case Nenhum:
                faction.kick(this);
                this.faction = "";
                break;
            default:
                return;
        }

        try {
            Methods.updateFaction(faction);
            Methods.updatePlayer(this);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao atualizar cargo do jogador " + nome + ": " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "NDPlayer [nome=" + nome + ", faction=" + faction + ", kills=" + kills + ", mortes=" + mortes
                + ", poder=" + poder + ", podermax=" + podermax + ", online=" + online + ", mapaligado=" + mapaligado
                + ", verterras=" + verterras + ", convites=" + convites + ", quemConvidou=" + quemConvidou
                + ", conviteTimestamps=" + conviteTimestamps + "]";
    }
}