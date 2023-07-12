package net.hyper.mc.server.player.party;

import balbucio.sqlapi.sqlite.SQLiteInstance;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.server.network.CraftNetworkManager;
import net.hyper.mc.server.player.party.gui.PartyListener;
import net.hyper.mc.spigot.player.party.Party;
import net.hyper.mc.spigot.player.party.PartyManager;
import net.hyper.mc.spigot.player.party.PartyPlayer;
import net.hyper.mc.spigot.player.party.PartyRole;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.inventory.ItemCreator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CraftPartyManager implements PartyManager {

    private static CraftPartyManager instance;

    public static CraftPartyManager getInstance() {
        return instance;
    }

    private CraftServer server;
    private SQLiteInstance sqlite;
    private CopyOnWriteArrayList<Party> parties = new CopyOnWriteArrayList<>();
    private HyperMessageBroker messeger;

    public CraftPartyManager(CraftServer server) {
        instance = this;
        this.server = server;
        this.sqlite = server.getSQLiteInstance();
        Bukkit.registerCommand(PartyCommand.class);
        messeger = server.getHyperSpigot().getMessenger();
        messeger.sendMessage("party", new JSONObject().put("channel", "register").toString());
        messeger.registerConsumer("party", m -> {
            JSONObject payload = m.getValue() instanceof String ? new JSONObject((String) m.getValue()) : (JSONObject) m.getValue();
            String channel = payload.getString("channel");
            if (channel.equalsIgnoreCase("create")) {
                JSONObject data = payload.getJSONObject("data");
                Party party = Party.getPartyFromJson(data);
                parties.add(party);
            } else if (channel.equalsIgnoreCase("invite")) {
                Player target = Bukkit.getPlayer(payload.getString("target"));
                if (target != null) {
                    target.sendMessage("");
                    target.sendMessage("§d O jogador §f" + payload.getString("") + "§d te convidou para uma party.");
                    TextComponent component = new TextComponent("§a§lClique aqui §dpara aceitar ou ignore para negar!");
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party aceitar #" + payload.getString("party")));
                    target.spigot().sendMessage(component);
                    target.sendMessage("");
                    target.playSound(target.getLocation(), Sound.VILLAGER_YES, 2, 2);
                }
            } else if (channel.equalsIgnoreCase("update")) {
                parties.stream()
                        .filter(p -> p.getId().equalsIgnoreCase(payload.getJSONObject("data").getString("id")))
                        .findFirst().ifPresent(p -> p.update(payload.getJSONObject("data")));
            } else if (channel.equalsIgnoreCase("delete")) {
                parties.stream()
                        .filter(p -> p.getId().equalsIgnoreCase(payload.getString("id")))
                        .forEach(p -> parties.remove(p));
            }
        });


    }

    public Party getParty(Player player) {
        return parties.stream()
                .filter(p -> p.getMembers().containsKey(player) || p.getOwner().getName().equalsIgnoreCase(player.getName()))
                .findFirst().orElse(null);
    }

    public Party getPartyByID(String id) {
        return parties.stream().filter(p -> p.getId().equalsIgnoreCase(id.replace("#", ""))).findFirst().orElse(null);
    }

    public Party getPartyByMemberName(String name) {
        return parties.stream()
                .filter(p -> p.getOwner().getName().equalsIgnoreCase(name) || p.getMembers().keySet().stream().anyMatch(m -> m.getName().equalsIgnoreCase(name)))
                .findFirst().orElse(null);
    }

    public Party createParty(Player owner) {
        Party party = new Party(owner);
        JSONObject packet = new JSONObject();
        packet.put("channel", "create");
        packet.put("data", party.getPartyJson());
        messeger.sendMessage("party", packet.toString());
        parties.add(party);
        return party;
    }

    public void accept(Player player, String arg) {
        Party party;
        if (arg.contains("#")) {
            party = getPartyByID(arg);
        } else {
            party = getPartyByMemberName(arg);
        }

        if (party == null) {
            player.sendMessage("§cA party referenciada não existe!");
            return;
        }

        if (party.hasMember(player)) {
            player.sendMessage("§cVocê já está nesta party!");
            return;
        }

        if (party.getConvites().containsKey(player.getName()) || party.isOpen()) {
            long time = party.getConvites().get(player.getName());
            if (time <= Calendar.getInstance().getTimeInMillis() && !party.isOpen()) {
                player.sendMessage("§cO seu convite para entrar na party expirou!");
                return;
            }

            party.addMember(player);
            player.sendMessage("§aVocê entrou na party: §b" + party.getName());
            Bukkit.sendMessage((Player) party.getOwner(), "§aO §7" + player.getDisplayName() + " §aaceitou o convite e entrou na party.");
            party.broadcast("§aO §7" + player.getDisplayName() + "§a entrou na party.", false);
            messeger.sendMessage("party", new JSONObject()
                    .put("channel", "update")
                    .put("data", party.getPartyJson())
                    .toString());
        }
    }

    public void inviteToParty(Player player, String targetName, Party party) {
        Player target = Bukkit.getPlayer(targetName);
        if (target != null || CraftNetworkManager.getInstance().hasPlayer(targetName)) {
            if(party.hasMember(targetName)){
                player.sendMessage("§cEste jogador já é membro da party!");
             return;
            }
            if(party.getConvites().containsKey(targetName) && party.getConvites().get(targetName) > new Date().getTime()){
                player.sendMessage("§cVocê já convidou este jogador.");
                return;
            }
            party.addConvite(targetName);
            if (target != null) {
                target.sendMessage("");
                target.sendMessage("§d O jogador §f" + player.getDisplayName() + "§d te convidou para uma party.");
                TextComponent component = new TextComponent("§a§lClique aqui §dpara aceitar ou ignore para negar!");
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party aceitar " + party.getOwner().getName()));
                target.spigot().sendMessage(component);
                target.sendMessage("");
                target.playSound(target.getLocation(), Sound.VILLAGER_YES, 2, 2);
            } else {
                messeger.sendMessage("party", new JSONObject()
                        .put("channel", "update")
                        .put("data", party.getPartyJson())
                        .toString());
                messeger.sendMessage("party",
                        new JSONObject()
                                .put("channel", "invite")
                                .put("name", player.getDisplayName())
                                .put("target", targetName)
                                .put("party", party.getId())
                                .toString());
            }
            player.sendMessage("§aO jogador foi convidado!");
        } else {
            player.sendMessage("§cO player não está online ou este comando não existe!");
        }
    }

    public void delete(Player player) {
        Party party = getParty(player);
        if (party != null) {
            if (!party.getOwner().getName().equalsIgnoreCase(player.getName())) {
                player.sendMessage("§cSomente o dono pode deletar a party!");
                return;
            }
            party.broadcast("§cO dono deletou a party.", false);
            messeger.sendMessage("party", new JSONObject()
                    .put("channel", "delete")
                    .put("id", party.getId())
                    .toString());
        } else {
            player.sendMessage("§cVocê não tem uma party!");
        }
    }

    public void rename(Player player, String name) {
        Party party = getParty(player);
        if (party != null) {
            if (!party.getOwner().getName().equalsIgnoreCase(player.getName())) {
                player.sendMessage("§cSomente o dono pode renomear a party!");
                return;
            }
            party.setName(name.replace("&", "§"));
            party.broadcast("§aA party foi renomeada para: §7" + name.replace("&", "§"), false);
            messeger.sendMessage("party", new JSONObject()
                    .put("channel", "update")
                    .put("data", party.getPartyJson())
                    .toString());
            player.sendMessage("§aA party foi renomeada!");
        } else {
            player.sendMessage("§cVocê não tem uma party!");
        }
    }

    public void showInfo(Player player) {
        Party party = getParty(player);
        if (party == null) {
            player.sendMessage("§aCrie uma party enviando um convite!");
            player.sendMessage("§aPara enviar um convite, use: §7/party <jogador>");
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            Inventory inventory = Bukkit.createInventory(null, 9, "Informações da Party");
            ItemStack infoStack = Bukkit.createItemCreator(Material.PAPER)
                    .addLore(new ArrayList<>(Arrays.asList(
                            "§7Dono: §f" + party.getOwner().getName(),
                            "§7Nome: §f" + party.getName(),
                            "§7Limite de jogadores: §f" + party.getMembers().size() + "/" + party.getMaxSize(),
                            "§7Estado da Party: §f" + (party.isOpen() ? "Pública" : "Privada"),
                            "§7Estado do Chat: §f" + (party.isChatMuted() ? "Apenas para Moderadores" : "Liberado para Todos"),
                            "§7Criado em: §f" + dateFormat.format(party.getCreateTime()))))
                    .withName("§aInformações")
                    .removeFlags()
                    .done();
            inventory.addItem(infoStack);
            ItemStack convidarStack = Bukkit.createItemCreator(Material.SIGN)
                    .addLore(new ArrayList<>(Arrays.asList(
                            "§7Convide um jogador para",
                            "§7sua party §bclicando aqui§7.")))
                    .withName("§aConvidar um jogador")
                    .removeFlags().done();
            inventory.addItem(convidarStack);
            ItemStack members = Bukkit.createItemCreator(player.getItemHead())
                    .addLore(new ArrayList<>(Arrays.asList("§7Clique para ver os membros.")))
                    .withName("§aMembros da Party")
                    .removeFlags()
                    .done();
            inventory.addItem(members);
            ItemStack partidaExclsuiva = Bukkit.createItemCreator(Material.ENDER_PEARL)
                    .addLore(new ArrayList<>(Arrays.asList("§7Clique para criar uma partida privada.")))
                    .withName("§aPartida Privada")
                    .done();
            inventory.addItem(partidaExclsuiva);
            ItemStack convites = Bukkit.createItemCreator(Material.NAME_TAG)
                    .addLore(new ArrayList<>(Arrays.asList("§7Clique para ver os convites enviados.")))
                    .withName("§aConvites Enviados")
                    .done();
            inventory.addItem(convites);
            ItemStack excluir = Bukkit.createItemCreator(Material.BARRIER)
                    .addLore(new ArrayList<>(Arrays.asList("§7Clique para deletar a party.")))
                    .withName("§cDeletar Party").done();
            inventory.addItem(excluir);
            PartyListener.guis.put(inventory, player);
            player.openInventory(inventory);
        }
    }

    public void leave(Player player) {
        Party party = getParty(player);
        if (party != null) {
            party.getMembers().remove(player);
            party.broadcast("§cO " + player.getName() + " saiu da party.", true);
            messeger.sendMessage("party", new JSONObject()
                    .put("channel", "update")
                    .put("data", party.getPartyJson())
                    .toString());
        } else {
            player.sendMessage("§cVocê não está numa party.");
        }
    }

    public void transferir(Player player, String name) {
        Party party = getParty(player);
        if (party != null) {
            if (!party.getOwner().getName().equalsIgnoreCase(player.getName())) {
                player.sendMessage("§cSomente o dono pode renomear a party!");
                return;
            }
            PartyPlayer oldOwner = party.getOwner();

            PartyPlayer pp = party.getMembers().keySet().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(name))
                    .findFirst().orElse(null);

            if (pp != null) {
                party.setOwner(pp);
                party.getMembers().remove(pp);
                party.getMembers().put(oldOwner, PartyRole.MEMBER);
                messeger.sendMessage("party", new JSONObject()
                        .put("channel", "update")
                        .put("data", party.getPartyJson())
                        .toString());
            }
        }
    }

    public void replace(PartyPlayer player) {
        parties.forEach(p -> {
            if (p.getOwner().getName().equalsIgnoreCase(player.getName())) {
                p.setOwner(player);
            }
            p.getMembers().keySet().stream().filter(ap -> ap.getName().equalsIgnoreCase(player.getName())).forEach(ap -> {
                PartyRole role = p.getMembers().get(ap);
                p.getMembers().remove(ap);
                p.getMembers().put(player, role);
            });
        });
    }
}
