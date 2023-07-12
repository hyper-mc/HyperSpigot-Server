package net.hyper.mc.server.player.party.gui;

import net.hyper.mc.server.event.EventHandler;
import net.hyper.mc.spigot.player.party.Party;
import net.hyper.mc.spigot.player.party.PartyPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemCreator;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PartyListener implements EventHandler.Listener {
    public static Map<Inventory, Player> guis = new HashMap<>();

    @Override
    public void listener(Event evt) {
        if(evt instanceof InventoryClickEvent){
            Inventory inventory = ((InventoryClickEvent) evt).getInventory();
            Player clicker = (Player) ((InventoryClickEvent) evt).getWhoClicked();
            int slot = ((InventoryClickEvent) evt).getSlot();
            Party party = clicker.getParty();
            if (guis.containsKey(inventory)) {
                if (party == null) {
                    clicker.closeInventory();
                    guis.remove(inventory);
                    return;
                }
                ((InventoryClickEvent) evt).setCancelled(true);
                if (slot == 2) {

                } else if (slot == 3) {
                    showMembers(clicker, party);
                } else if (slot == 4) {

                } else if (slot == 5) {

                }
            }
        }
    }

    private void showMembers(Player player, Party party){
        Inventory inventory = Bukkit.createInventory(null, 6*9, "Membros da Party");
        int i = 1;
        int p = i+7;
        for(PartyPlayer pl : party.getMembers().keySet()){
            ItemStack item = Bukkit.createItemCreator(Material.SKULL_ITEM)
                    .withName("§a"+pl.getName())
                    .removeFlags()
                    .addLore(Arrays.asList("§7Cargo: §f"+party.getMembers().get(pl).getName(), "§7"))
                    .done();
            inventory.setItem(i, item);
            i++;
            if(i == p){
                i++;
                p = i+7;
            }
        }
        player.openInventory(inventory);
    }
}
