package net.hyper.mc.server.command;

import net.hyper.mc.server.CraftHyperSpigot;
import net.hyper.mc.spigot.lobbies.LobbyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.api.Command;
import org.bukkit.command.api.paramter.Param;
import org.bukkit.entity.Player;

public class SetSpawnCommand {

    private LobbyManager lobbyManager = Bukkit.getHyperSpigot().getLobbyManager();

    @Command(names = {"setspawn", "addspawn", "setlobby", "addlobby"}, permission = "perm.admin", playerOnly = true)
    public void setSpawn(Player player,
                         @Param(name = "event", required = false) String event,
                         @Param(name = "type", required = false) String type){
        if(lobbyManager.hasLobbyInWorld(player.getWorld())){
            player.sendMessage("§cJá existe um lobby neste mundo, portanto atualizaremos apenas a localização do spawn.");
            lobbyManager.getLobbyInWorld(player.getWorld()).setLocation(player.getLocation());
            return;
        }
        if(event == null){
            event = "default";
        }
        if(type == null){
            type = lobbyManager.getServerType();
        }
        lobbyManager.addSpawn(type, event, player.getLocation());
        player.sendMessage("§aUm lobby de "+type+" foi criado!");
    }
}
