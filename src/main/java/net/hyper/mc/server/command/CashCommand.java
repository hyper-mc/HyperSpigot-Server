package net.hyper.mc.server.command;

import net.hyper.mc.server.player.PlayerContainer;
import net.hyper.mc.spigot.player.FakePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.api.Command;
import org.bukkit.command.api.paramter.Param;
import org.bukkit.entity.Player;

public class CashCommand {

    @Command(names = {"cash"}, description = "Compre itens com cash", playerOnly = true)
    public void cashCommand(Player player){
        if(!player.getPlayerData().containsKey("cash")){
            player.setData("cash", 0);
        }

        player.sendMessage("§aVocê tem §f"+player.getData("cash")+"§a Cash.");
    }

    @Command(names = {"cash set", "cash setar"}, description = "Sete uma quantidade de cash", permission = "perm.admin", playerOnly = true)
    public void setCashCommand(CommandSender sender, @Param(name = "player", required = true) String name, @Param(name = "value", required = true) int value){
        Bukkit.getHyperSpigot().setData(name, "cash", value);
        sender.sendMessage("§aSetado §7"+value+" §aCash para o §f"+name+"§a!");
    }

    @Command(names = {"cash add", "cash adicionar"}, description = "Adicione uma quantidade de cash", permission = "perm.admin", playerOnly = true)
    public void addCashCommand(CommandSender sender, @Param(name = "player", required = true) String name, @Param(name = "value", required = true) int value){
        long cash = (long) PlayerContainer.getData(new FakePlayer(name), "cash");
        cash += value;
        Bukkit.getHyperSpigot().setData(name, "cash", cash);
        sender.sendMessage("§aAdicionado §7"+value+" §aCash para o §f"+name+"§a!");
    }
}
