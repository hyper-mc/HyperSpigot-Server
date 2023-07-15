package net.hyper.mc.server.command;

import org.bukkit.command.api.Command;
import org.bukkit.command.api.paramter.Param;
import org.bukkit.entity.Player;

public class SetSpawnCommand {

    @Command(names = {"setspawn", "addspawn", "setlobby", "addlobby"}, permission = "perm.admin", playerOnly = true)
    public void setSpawn(Player player,
                         @Param(name = "event", required = false) String event,
                         @Param(name = "type", required = false) String type){

    }
}
