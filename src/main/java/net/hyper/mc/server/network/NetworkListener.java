package net.hyper.mc.server.network;

import lombok.AllArgsConstructor;
import net.hyper.mc.server.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

@AllArgsConstructor
public class NetworkListener implements EventHandler.Listener {

    private CraftNetworkManager networkManager;

    @Override
    public void listener(Event evt) {
        if(evt instanceof PlayerJoinEvent){
            Player player = ((PlayerJoinEvent) evt).getPlayer();
            networkManager.getOnlinePlayersInNetwork().add(player.getName());
            networkManager.getBroker().sendMessage("hyperspigot-network", new JSONObject()
                    .put("channel", "newplayer")
                    .put("name", player.getName())
                    .toString());
        } else if(evt instanceof PlayerQuitEvent){
            Player player = ((PlayerQuitEvent) evt).getPlayer();
            networkManager.getOnlinePlayersInNetwork().remove(player.getName());
            networkManager.getBroker().sendMessage("hyperspigot-network", new JSONObject()
                    .put("channel", "quitplayer")
                    .put("name", player.getName())
                    .toString());
        }
    }
}
