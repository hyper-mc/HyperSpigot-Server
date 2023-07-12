package net.hyper.mc.server.bungeecord;

import net.hyper.mc.spigot.bungeecord.BungeeAction;
import net.hyper.mc.spigot.player.FakePlayer;
import net.hyper.mc.spigot.utils.PluginMessage;
import net.minecraft.server.EntityPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CraftBungeeManager implements net.hyper.mc.spigot.bungeecord.BungeeManager {

    private static CraftBungeeManager instance;

    public static CraftBungeeManager getInstance() {
        return instance;
    }

    private ConcurrentHashMap<String, Map<String, Object>> servers = new ConcurrentHashMap<>();
    private CraftServer server;

    public CraftBungeeManager(CraftServer server) {
        instance = this;
        this.server = server;
        server.getMessenger().registerOutgoingPluginChannel(null, "BungeeCord");
    }

    @Override
    public int getOnlineCount() {
        AtomicInteger v = new AtomicInteger(0);
        servers.forEach((s, i) -> v.addAndGet((int) i.get("count")));
        return v.get();
    }

    @Override
    public Map<String, Object> getServerInfo(String server) {
        return servers.getOrDefault(server, null);
    }

    @Override
    public boolean hasPlayer(String name) {
        requestUpdate(BungeeAction.SERVER_LIST, null, null);
        requestUpdate(BungeeAction.PLAYER_LIST, null, null);
        requestUpdate(BungeeAction.PLAYER_COUNT, null, null);
        AtomicBoolean has = new AtomicBoolean(false);
        servers.forEach((s, i) -> {
            String[] players = (String[]) i.get("playerList");
            has.set(Arrays.asList(players).contains(name));
        });
        return has.get();
    }

    @Override
    public void sendMessage(String name, String message) {
        requestUpdate(BungeeAction.MESSAGE, new FakePlayer(name), message);
    }

    @Override
    public void sendMessage(Player player, String message) {
        requestUpdate(BungeeAction.MESSAGE, player, message);
    }

    public void pluginMessage(EntityPlayer player, byte[] data) {
        try {
            DataInputStream msg = new DataInputStream(new ByteArrayInputStream(data));
            String subChannel = msg.readUTF();
            if(subChannel.equalsIgnoreCase("PlayerCount")){
                String server = msg.readUTF();
                int count = msg.readInt();
                if(servers.containsKey(server)){
                    servers.get(server).replace("count", count);
                } else{
                    HashMap<String, Object> info = new HashMap<>();
                    info.put("count", count);
                    info.put("playerList", new String[0]);
                    info.put("ip", "localhost");
                    info.put("port", 0);
                    servers.put(server, info);
                }
            } else if(subChannel.equalsIgnoreCase("PlayerList")){
                String server = msg.readUTF();
                String[] playerList = msg.readUTF().split(",");
                if(servers.containsKey(server)){
                    servers.get(server).replace("playerList", playerList);
                } else{
                    HashMap<String, Object> info = new HashMap<>();
                    info.put("count", 0);
                    info.put("playerList", playerList);
                    info.put("ip", "localhost");
                    info.put("port", 0);
                    servers.put(server, info);
                }
            } else if(subChannel.equalsIgnoreCase("GetServers")){
                String[] serverList = msg.readUTF().split(",");
                for(String sv : serverList){
                    if (!servers.containsKey(sv)) {
                        HashMap<String, Object> info = new HashMap<>();
                        info.put("count", 0);
                        info.put("playerList", new String[0]);
                        info.put("ip", "localhost");
                        info.put("port", 0);
                        servers.put(sv, info);
                    }
                }
            } else if(subChannel.equalsIgnoreCase("UUID")){
                player.getBukkitEntity().setBungeeUUID(msg.readUTF());
            } else if(subChannel.equalsIgnoreCase("ServerIP")){
                String server = msg.readUTF();
                String IP = msg.readUTF();
                int port = msg.readInt();

                if(servers.containsKey(server)){
                    servers.get(server).replace("ip", IP);
                    servers.get(server).replace("port", port);
                } else{
                    HashMap<String, Object> info = new HashMap<>();
                    info.put("count", 0);
                    info.put("playerList", new String[0]);
                    info.put("ip", IP);
                    info.put("port", port);
                    servers.put(server, info);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void requestUpdate(BungeeAction action, Player player, Object value) {
        PluginMessage msg;
        switch (action) {
            case CONNECT:
                if (player != null) {
                    player.sendPluginMessage("BungeeCord", new PluginMessage("Connect").add((String) value).getBytes());
                }
                break;
            case PLAYER_COUNT:
                if (value != null) {
                    sendPlayerCount(player, (String) value);
                } else{
                    servers.keySet().forEach(s -> sendPlayerCount(player, s));
                }
                break;
            case PLAYER_LIST:
                if (value != null) {
                    sendPlayerList(player, (String) value);
                } else{
                    servers.keySet().forEach(s -> sendPlayerList(player, s));
                }
                break;
            case SERVER_LIST:
                msg = new PluginMessage("GetServers");
                if (player != null) {
                    player.sendPluginMessage("BungeeCord", msg.getBytes());
                } else {
                    server.sendPluginMessage("BungeeCord", msg.getBytes());
                }
                break;
            case UUID:
                if (player != null) {
                    player.sendPluginMessage("BungeeCord", new PluginMessage("UUID").getBytes());
                }
                break;
            case SERVER_IP:
                if (value != null) {
                    sendServerIP(player, (String) value);
                } else{
                    servers.keySet().forEach(s -> sendServerIP(player, (String) value));
                }
                break;
            case KICK_PLAYER:
                if (player != null) {
                    msg = new PluginMessage("KickPlayer");
                    if(value != null){
                        msg.add((String) value);
                    }
                    player.sendPluginMessage("BungeeCord", msg.getBytes());
                }
                break;
            case MESSAGE:
                if(value != null) {
                    msg = new PluginMessage("Message").add(player.getName()).add((String) value);
                    server.sendPluginMessage("BungeeCord", msg.getBytes());
                }
        }
    }

    private void sendPlayerList(Player player, String server){
        PluginMessage msg = new PluginMessage("PlayerList").add(server);
        if (player != null) {
            player.sendPluginMessage("BungeeCord", msg.getBytes());
        } else {
            this.server.sendPluginMessage("BungeeCord", msg.getBytes());
        }
    }

    private void sendServerIP(Player player, String server){
        PluginMessage msg = new PluginMessage("ServerIP").add(server);
        if (player != null) {
            player.sendPluginMessage("BungeeCord", msg.getBytes());
        } else {
            this.server.sendPluginMessage("BungeeCord", msg.getBytes());
        }
    }

    private void sendPlayerCount(Player player, String server){
        PluginMessage msg = new PluginMessage("PlayerCount").add(server);
        if (player != null) {
            player.sendPluginMessage("BungeeCord", msg.getBytes());
        } else {
            this.server.sendPluginMessage("BungeeCord", msg.getBytes());
        }
    }
}
