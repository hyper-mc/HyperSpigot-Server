package net.hyper.mc.server.event;

import net.hyper.mc.server.CraftHyperSpigot;
import net.hyper.mc.server.player.party.CraftPartyManager;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

public class EventHandler {

    private static List<Listener> list = new ArrayList<>();

    public static void add(Listener listener){
        list.add(listener);
    }

    public static void onJoin(PlayerJoinEvent evt){
        if(CraftPartyManager.getInstance() != null){
            CraftPartyManager.getInstance().replace(evt.getPlayer());
        }
        list.forEach(l -> l.listener(evt));
    }

    public static void onQuit(PlayerQuitEvent evt){
        list.forEach(l -> l.listener(evt));
    }

    public interface Listener{
        void listener(Event evt);
    }
}
