package net.hyper.mc.server.network;

import lombok.Data;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.server.event.EventHandler;
import net.hyper.mc.spigot.network.NetworkManager;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class CraftNetworkManager implements NetworkManager {

    private static CraftNetworkManager instance;

    public static CraftNetworkManager getInstance() {
        return instance;
    }

    private HyperMessageBroker broker;
    private CopyOnWriteArrayList<String> onlinePlayersInNetwork = new CopyOnWriteArrayList<>();

    public CraftNetworkManager(HyperMessageBroker broker) {
        instance = this;
        this.broker = broker;
        broker.registerConsumer("hyperspigot-network", m -> {
            JSONObject data = new JSONObject((String) m.getValue());
            String channel = data.getString("channel");
            if(channel.equalsIgnoreCase("newplayer")){
                onlinePlayersInNetwork.add(data.getString("name"));
            } else if(channel.equalsIgnoreCase("quitplayer")){
                onlinePlayersInNetwork.remove(data.getString("name"));
            }
        });
        EventHandler.add(new NetworkListener(this));
    }

    @Override
    public boolean hasPlayer(String name) {
        return onlinePlayersInNetwork.contains(name);
    }

    @Override
    public List<String> getOnlinePlayers() {
        return (List<String>) onlinePlayersInNetwork;
    }

    @Override
    public int getOnlineCount() {
        return onlinePlayersInNetwork.size();
    }
}
