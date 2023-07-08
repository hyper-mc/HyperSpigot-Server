package net.hyper.mc.server.event;

import net.hyper.mc.server.CraftHyperSpigot;
import net.hyper.mc.server.player.party.CraftPartyManager;
import org.bukkit.event.player.PlayerJoinEvent;

public class EventHandler {

    public static void onJoin(PlayerJoinEvent evt){
        if(CraftPartyManager.getInstance() != null){
            CraftPartyManager.getInstance().replace(evt.getPlayer());
        }
    }
}
