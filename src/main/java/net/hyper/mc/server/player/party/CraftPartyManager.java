package net.hyper.mc.server.player.party;

import balbucio.sqlapi.model.ConditionValue;
import balbucio.sqlapi.sqlite.SQLiteInstance;
import net.hyper.mc.server.bungeecord.BungeeManager;
import net.hyper.mc.spigot.bungeecord.BungeeAction;
import net.hyper.mc.spigot.player.party.Party;
import net.hyper.mc.spigot.player.party.PartyManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;

public class CraftPartyManager implements PartyManager {

    private static CraftPartyManager instance;

    public static CraftPartyManager getInstance() {
        return instance;
    }

    private CraftServer server;
    private SQLiteInstance sqlite;
    private CopyOnWriteArrayList<Party> parties = new CopyOnWriteArrayList<>();
    public CraftPartyManager(CraftServer server){
        instance = this;
        this.server = server;
        this.sqlite = server.getSQLiteInstance();
        sqlite.createTable("party", "owner VARCHAR(255), name VARCHAR(255), id VARCHAR(255), data TEXT");
        this.server.getHyperSpigot().getMessenger().registerConsumer("party", m -> {
            JSONObject payload = new JSONObject((String) m.getValue());
            String channel = payload.getString("channel");
            if(channel.equalsIgnoreCase("create")){
                JSONObject data = payload.getJSONObject("data");
                Party party = Party.getPartyFromJson(data);
                parties.add(party);
            } else if(channel.equalsIgnoreCase("invite")){
                Player target = Bukkit.getPlayer(payload.getString("target"));
                if(target != null){
                    target.sendMessage("");
                    target.sendMessage("§d O jogador §f"+payload.getString("")+"§d te convidou para uma party.");
                    TextComponent component = new TextComponent("§a§lClique aqui §dpara aceitar ou ignore para negar!" );
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party aceitar #"+payload.getString("party")));
                    target.spigot().sendMessage(component);
                    target.sendMessage("");
                }
            } else if(channel.equalsIgnoreCase(payload.getString("update"))){

            }
        });
    }

    public Party getParty(Player player){
        return parties.stream()
                .filter(p -> p.getMembers().containsKey(player) && p.getOwner() == player)
                .findFirst().orElse(null);
    }

    public Party getPartyByID(String id){
        return parties.stream().filter(p -> p.getId().equalsIgnoreCase(id.replace("#", ""))).findFirst().orElse(null);
    }

    public Party getPartyByMemberName(String name){
        return parties.stream()
                .filter(p -> p.getOwner().getName().equalsIgnoreCase(name) || p.getMembers().keySet().stream().anyMatch(m -> m.getName().equalsIgnoreCase(name)))
                .findFirst().orElse(null);
    }

    public Party createParty(Player owner){
        Party party = new Party(owner);
        JSONObject packet = new JSONObject();
        packet.put("channel", "create");
        packet.put("data", party.getPartyJson().toString());
        this.server.getHyperSpigot().getMessenger().sendMessage("party", packet);
        parties.add(party);
        return party;
    }

    public void accept(Player player, String arg){
        Party party;
        if(arg.contains("#")){
            party = getPartyByID(arg);
        } else {
            party = getPartyByMemberName(arg);
        }

        if(party == null){
            player.sendMessage("§cA party referenciada não existe!");
            return;
        }

        if(party.hasMember(player)){
            player.sendMessage("§cVocê já está nesta party!");
            return;
        }

        if(party.getConvites().containsKey(player.getName()) || party.isOpen()){
            long time = party.getConvites().get(player.getName());
            if(time <= Calendar.getInstance().getTimeInMillis() && !party.isOpen()){
                player.sendMessage("§cO seu convite para entrar na party expirou!");
                return;
            }

            party.addMember(player);
            player.sendMessage("§aVocê entrou na party: §b"+party.getName());
            Bukkit.sendMessage((Player) party.getOwner(), "§aO §7"+player.getDisplayName()+" §aaceitou o convite e entrou na party.");
            party.broadcast("§aO §7"+player.getDisplayName()+"§a entrou na party.");
            this.server.getHyperSpigot().getMessenger().sendMessage("party", new JSONObject()
                    .put("channel", "update")
                    .put("data", party.getPartyJson())
                    .toString());
        }
    }

    public void inviteToParty(Player player, String targetName, Party party){
        party.addConvite(targetName);
        Player target = Bukkit.getPlayer(targetName);
        if(target != null){
            target.sendMessage("");
            target.sendMessage("§d O jogador §f"+player.getDisplayName()+"§d te convidou para uma party.");
            TextComponent component = new TextComponent("§a§lClique aqui §dpara aceitar ou ignore para negar!" );
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party aceitar "+party.getOwner().getName()));
            target.spigot().sendMessage(component);
            target.sendMessage("");
        } else{
            this.server.getHyperSpigot().getMessenger().sendMessage("party", new JSONObject()
                    .put("channel", "update")
                    .put("data", party.getPartyJson())
                    .toString());
            this.server.getHyperSpigot().getMessenger().sendMessage("party",
                    new JSONObject()
                            .put("channel", "invite")
                            .put("name", player.getDisplayName())
                            .put("target", targetName)
                            .put("party", party.getId())
                            .toString());
        }
        player.sendMessage("§aO jogador foi convidado!");
    }
}
