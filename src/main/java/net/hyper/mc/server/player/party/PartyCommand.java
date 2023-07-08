package net.hyper.mc.server.player.party;

import net.hyper.mc.server.bungeecord.BungeeManager;
import net.hyper.mc.spigot.bungeecord.BungeeAction;
import net.hyper.mc.spigot.player.party.Party;
import net.hyper.mc.spigot.player.party.PartyRole;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.api.Command;
import org.bukkit.command.api.paramter.Param;
import org.bukkit.entity.Player;

public class PartyCommand {

    @Command(names = {"party"}, permission = "spigot.party", playerOnly = true, description = "Junte-se aos amigos usando a party!")
    public void invite(Player player, @Param(name = "nickname", required = false) String target) {
        if (target == null) {
            CraftPartyManager.getInstance().showInfo(player);
        } else {
            Party party = CraftPartyManager.getInstance().getParty(player);
            if (party != null) {
                PartyRole role = party.getOwner() == player ? PartyRole.OWNER : party.getMembers().get(player);
                if (role == PartyRole.OWNER || role == PartyRole.MANAGER) {
                    BungeeManager.getInstance().requestUpdate(BungeeAction.SERVER_LIST, null, null);
                    BungeeManager.getInstance().requestUpdate(BungeeAction.PLAYER_LIST, null, null);
                    if (Bukkit.getPlayer(target) != null || BungeeManager.getInstance().hasPlayer(target)) {
                        CraftPartyManager.getInstance().inviteToParty(player, target, party);
                    }
                }
            } else {
                party = CraftPartyManager.getInstance().createParty(player);
                CraftPartyManager.getInstance().inviteToParty(player, target, party);
            }
        }

    }

    @Command(names = {"party aceitar", "party accept", "party entrar","party join"}, permission = "spigot.party", playerOnly = true, description = "Aceitar convite de party")
    public void accept(Player player, @Param(name = "party", required = true) String arg){
        CraftPartyManager.getInstance().accept(player, arg);
    }

    @Command(names = {"party puxar"}, permission = "spigot.party", playerOnly = true, description = "Puxar todos os membros da party para o servidor em que o dono está.")
    public void puxar(Player player){

    }

    @Command(names = {"party excluir", "party deletar"}, permission = "spigot.party", playerOnly = true, description = "Excluir a party.")
    public void excluir(Player player){
        CraftPartyManager.getInstance().delete(player);
    }

    @Command(names = {"party transferir"}, permission = "spigot.party", playerOnly = true)
    public void transferir(Player player){
    }

    @Command(names = {"party rename"}, permission = "spigot.party", playerOnly = true, description = "Renomear a party.")
    public void rename(Player player, @Param(name = "newName", required = false) String newName){
        CraftPartyManager.getInstance().rename(player, newName);
    }

    @Command(names = {"party sair", "party kitar", "party leave", "party exit"}, permission = "spigot.party", playerOnly = true, description = "Sair da party")
    public void sair(Player player){
        CraftPartyManager.getInstance().leave(player);
    }
}
