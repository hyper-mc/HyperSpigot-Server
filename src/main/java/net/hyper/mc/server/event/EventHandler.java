package net.hyper.mc.server.event;

import net.hyper.mc.server.CraftHyperSpigot;
import net.hyper.mc.server.player.party.CraftPartyManager;
import net.hyper.mc.spigot.player.FakePlayer;
import net.hyper.mc.spigot.player.party.PartyPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventHandler {

    private static CopyOnWriteArrayList<Listener> list = new CopyOnWriteArrayList<>();

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
        if(CraftPartyManager.getInstance() != null){
            CraftPartyManager.getInstance().replace(new FakePlayer(evt.getPlayer().getName()));
        }
        list.forEach(l -> l.listener(evt));
    }

    public static void onInventoryClick(InventoryClickEvent evt){
        list.forEach(l -> l.listener(evt));
    }

    public static void onInventoryMoveItem(InventoryMoveItemEvent evt){
        evt.getDestination().getViewers().forEach(h -> {
            if(h instanceof Player) {
                Player player = (Player) h;
                if (h.getInventory() == evt.getDestination()) {
                    if (!player.getHotBarConfig().isMoveItems()){
                        evt.setCancelled(true);
                    }
                }
            }
        });

        evt.getSource().getViewers().forEach(h -> {
            if(h instanceof Player) {
                Player player = (Player) h;
                if (h.getInventory() == evt.getSource()) {
                    if (!player.getHotBarConfig().isMoveItems()){
                        evt.setCancelled(true);
                    }
                }
            }
        });
        list.forEach(l -> l.listener(evt));
    }

    public static void onInventoryPickUp(InventoryPickupItemEvent evt){
        evt.getInventory().getViewers().forEach(h -> {
            if(h instanceof Player) {
                Player player = (Player) h;
                if (h.getInventory() == evt.getInventory()) {
                    if (!player.getHotBarConfig().isMoveItems()){
                        evt.setCancelled(true);
                    }
                }
            }
        });
        list.forEach(l -> l.listener(evt));
    }

    public static void onDropEvent(PlayerDropItemEvent evt){
        if(!evt.getPlayer().getHotBarConfig().isDropItems()){
            evt.setCancelled(true);
        }
        list.forEach(l -> l.listener(evt));
    }

    public interface Listener{
        void listener(Event evt);
    }
}
