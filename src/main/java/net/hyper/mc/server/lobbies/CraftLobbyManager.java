package net.hyper.mc.server.lobbies;

import balbucio.responsivescheduler.RSTask;
import balbucio.sqlapi.model.ConditionValue;
import balbucio.sqlapi.sqlite.SQLiteInstance;
import com.google.gson.Gson;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.server.bungeecord.CraftBungeeManager;
import net.hyper.mc.server.event.EventHandler;
import net.hyper.mc.spigot.bungeecord.BungeeAction;
import net.hyper.mc.spigot.lobbies.LobbyManager;
import net.hyper.mc.spigot.lobbies.ServerLobby;
import net.hyper.mc.spigot.lobbies.WorldLobby;
import net.hyper.mc.spigot.player.FakePlayer;
import net.hyper.mc.spigot.world.Position;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CraftLobbyManager extends RSTask implements LobbyManager{

    private static CraftLobbyManager instance;

    public static CraftLobbyManager getInstance() {
        return instance;
    }

    private CraftServer server;
    private HyperMessageBroker broker;
    private SQLiteInstance sqlite;
    private Gson gson = new Gson();
    private CopyOnWriteArrayList<WorldLobby> lobbies = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<ServerLobby> networkLobbies = new CopyOnWriteArrayList<>();
    private ConcurrentMap<String, Integer> networkOnline = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> teleport = new ConcurrentHashMap<>();

    public CraftLobbyManager(CraftServer server, HyperMessageBroker broker){
        instance = this;
        this.server = server;
        this.broker = broker;
        this.sqlite = server.getSQLiteInstance();
        sqlite.createTable("lobbies", "id VARCHAR(255), type VARCHAR(255), event VARCHAR(255), data TEXT");
        List<Object[]> objs = sqlite.getAllValuesFromColumns("lobbies", "id, type, event, data");
        for(Object[] o : objs){
            WorldLobby worldLobby = new WorldLobby();
            worldLobby.setId((String) o[0]);
            worldLobby.setType((String) o[1]);
            worldLobby.setEvent((String) o[2]);
            JSONObject data = new JSONObject((String) o[3]);
            worldLobby.setPrime(data.getBoolean("prime"));
            worldLobby.setLocation(gson.fromJson(data.getString("position"), Position.class).convertToLocation());
            lobbies.add(worldLobby);
        }
        broker.registerConsumer("hyperspigot-lobbies", m -> {
            JSONObject payload = (JSONObject) m.getValue();
            String channel = payload.getString("channel");
            if(channel.equalsIgnoreCase("server")){
                ServerLobby sl = gson.fromJson(payload.getString("data"), ServerLobby.class);
                networkLobbies.stream()
                        .filter(s -> s.getServerName().equalsIgnoreCase(sl.getServerName()))
                        .collect(Collectors.toList()).forEach(s -> networkLobbies.remove(s));
                networkLobbies.add(sl);
                sl.getLobbiesId().values().forEach(id -> broker.sendMessage("hyperspigot-lobbies", new JSONObject().put("channel", "online").put("id",id)));
            } else if(channel.equalsIgnoreCase("online")){
                String id = payload.getString("id");
                if (hasLobbyID(id)) {
                    broker.sendMessage("hyperspigot-lobbies", new JSONObject()
                            .put("channel", "update")
                                    .put("id", id)
                            .put("online", lobbies.stream().filter(w -> w.getId().equals(id))
                                    .findFirst().get().getLocation()
                                    .getWorld().getPlayers().size()));
                }
            } else if(channel.equalsIgnoreCase("update")){
                int online = payload.getInt("online");
                networkOnline.put(payload.getString("id"), online);
            } else if(channel.equalsIgnoreCase("connect")) {
                String player = payload.getString("player");
                String lobbyID = payload.getString("lobbyID");
                if (getLobbyWithId(lobbyID) != null) {
                    server.getHyperSpigot().getBungeeManager().requestUpdate(BungeeAction.CONNECT, new FakePlayer(player), server.getHyperSpigot().getBungeeManager().getServerName());
                    teleport.put(player, lobbyID);
                }
            }
        });
        server.getResponsiveScheduler().repeatTask(this, 0, 20000);
        EventHandler.add(evt -> {
            if(evt instanceof PlayerJoinEvent){
                Player player = ((PlayerJoinEvent) evt).getPlayer();
                if(teleport.containsKey(player.getName())){
                    getLobbyWithId(player.getName()).teleport(player);
                }
            }
        });
    }

    @Override
    public boolean hasLobbyID(String id) {
        return lobbies.stream().anyMatch(w -> w.getId().equals(id));
    }

    @Override
    public WorldLobby addSpawn(String type, String event, Location location) {
        WorldLobby wl = new WorldLobby(type, event, location);
        lobbies.add(wl);
        return wl;
    }

    @Override
    public WorldLobby getLobbyInWorld(World world) {
        return lobbies.stream()
                .filter(l -> world == l.getLocation().getWorld())
                .findFirst().orElse(null);
    }

    @Override
    public boolean hasLobbyInWorld(World world){
        return lobbies.stream().anyMatch(l -> world == l.getLocation().getWorld());
    }

    @Override
    public void removeSpawn(WorldLobby wl) {
        if(lobbies.contains(wl)){
            lobbies.remove(wl);
            sqlite.delete("id", wl.getId(), "lobbies");
        }
    }

    @Override
    public WorldLobby getLobbyWithId(String ID) {
        return lobbies.stream()
                .filter(l -> l.getId().equals(ID))
                .findFirst().orElse(null);
    }

    @Override
    public List<WorldLobby> getLobby(String type, String event) {
        return lobbies.stream().filter(wl -> wl.getType().equalsIgnoreCase(type) && wl.getEvent().equals(event)).collect(Collectors.toList());
    }

    @Override
    public List<String> getNetworkLobby(String type, String event) {
        List<String> ids = new ArrayList<>();
        networkLobbies.forEach(s -> {
            if(s.getLobbiesId().containsKey(type+"/"+event)){
                ids.addAll(s.getLobbiesId().get(type+"/"+event));
            }
        });
        return ids;
    }

    @Override
    public CopyOnWriteArrayList<WorldLobby> getLobbies() {
        return lobbies;
    }

    @Override
    public CopyOnWriteArrayList<ServerLobby> getNetworkLobbies() {
        return networkLobbies;
    }

    private String serverType = "main";

    @Override
    public void setServerType(String type) {
        this.serverType = type;
    }

    @Override
    public String getServerType() {
        return serverType;
    }

    @Override
    public int getOnlineInLobbyID(String id) {
        AtomicInteger online = new AtomicInteger();
        lobbies.stream().filter(l -> l.getId().equals(id)).findFirst().ifPresent(l -> online.set(l.getLocation().getWorld().getPlayers().size()));
        if(networkOnline.containsKey(id)){
            online.set(networkOnline.get(id));
        }
        return online.get();
    }

    @Override
    public void connectServerLobby(String lobbyID, Player player) {
        broker.sendMessage("hyperspigot-lobbies", new JSONObject().put("channel", "connect")
                .put("lobbyID", lobbyID)
                .put("player", player.getName()));
    }

    @Override
    public void save() {
        for(WorldLobby wl : lobbies){
            JSONObject data = new JSONObject();
            data.put("position", gson.toJson(wl.getLocation().convertToPosition()));
            data.put("prime", wl.isPrime());
            ConditionValue[] conditionValues = new ConditionValue[]{
                    new ConditionValue("id", ConditionValue.Conditional.EQUALS, wl.getId(), ConditionValue.Operator.AND),
                    new ConditionValue("type", ConditionValue.Conditional.EQUALS, wl.getType(), ConditionValue.Operator.AND),
                    new ConditionValue("event", ConditionValue.Conditional.EQUALS, wl.getEvent(), ConditionValue.Operator.NULL)
            };
            if(sqlite.exists(conditionValues, "lobbies")){
                sqlite.set(conditionValues, "data", data.toString(), "lobbies");
            } else {
                sqlite.insert("id, type, event, data", "'"+wl.getId()+"', '" + wl.getType() + "', '" + wl.getEvent() + "', '" + data.toString() + "'", "lobbies");
            }
        }
    }

    @Override
    public void run() {
        save();
        ServerLobby serverLobby = new ServerLobby();
        serverLobby.setServerName(CraftBungeeManager.getInstance().getServerName());
        Map<String, List<String>> lobbiesID = new HashMap<>();
        lobbies.forEach(w -> {
            if(lobbiesID.containsKey(w.getType()+"/"+w.getEvent())){
                lobbiesID.get(w.getType()+"/"+w.getEvent()).add(w.getId());
            } else{
                List<String> ids = new ArrayList<>();
                ids.add(w.getId());
                lobbiesID.put(w.getType()+"/"+w.getEvent(), ids);
            }
        });
        serverLobby.setLobbiesId(lobbiesID);
        broker.sendMessage("hyperspigot-lobbies", new JSONObject()
                .put("channel", "server")
                .put("data", gson.toJson(serverLobby)));
    }
}
