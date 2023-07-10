package net.hyper.mc.server.player.scoreboard;

import net.hyper.mc.server.event.EventHandler;
import net.hyper.mc.spigot.player.scoreboard.TeamManager;
import net.hyper.mc.spigot.player.role.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftTeamManager implements TeamManager {

    private Player player;
    private Scoreboard scoreboard;
    private Map<Role, Team> teamByRole = new HashMap<>();
    private Map<String, List<Team>> customTeams = new HashMap<>();

    public CraftTeamManager(Player player){
        this.player = player;
        setScoreboard(player.getScoreboard());
    }
    public void setScoreboard(Scoreboard scoreboard){
        this.scoreboard = scoreboard;
        registerRoles();
        EventHandler.add(e -> {
            if(e instanceof PlayerJoinEvent){
                Player p = ((PlayerJoinEvent) e).getPlayer();
                if(p.getRole() != null){
                    teamByRole.computeIfPresent(p.getRole(), (r, t) -> {
                        t.addPlayer(p);
                        return t;
                    });
                }
            }
        });
    }

    public Team setCustomTeam(int order, String name, String key){
        Team team = scoreboard.registerNewTeam(order+name);
        team.setNameTagVisibility(NameTagVisibility.ALWAYS);
        if (!customTeams.containsKey(key)) {
            customTeams.put(key, new ArrayList<>());
        }
        customTeams.get(key).add(team);
        return team;
    }

    public void removeCustomTeam(int order, String name, String key){
        scoreboard.getTeams().stream().filter(t -> t.getName().equalsIgnoreCase(order+name)).findFirst().ifPresent(t -> {
            customTeams.get(key).remove(t);
            t.unregister();
        });
    }

    private void registerRoles(){
        teamByRole.clear();
        List<Role> roles = Bukkit.getHyperSpigot().getRoleManager().getRoles();
        roles.forEach(r -> {
            Team team;
            if(!scoreboard.getTeams().stream().anyMatch(t -> t.getName().equalsIgnoreCase(r.getOrder()+r.getName()))){
                team = scoreboard.registerNewTeam(r.getOrder() + r.getName());
                team.setPrefix(r.getTag() + " ");
                team.setNameTagVisibility(NameTagVisibility.ALWAYS);
            } else {
                team = scoreboard.getTeam(r.getOrder()+ r.getName());
                team.setPrefix(r.getTag() + " ");
                team.setNameTagVisibility(NameTagVisibility.ALWAYS);
            }
            teamByRole.put(r, team);
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.getRole() == r && !team.hasEntry(p.getName())) {
                    team.addPlayer(p);
                }
            });
        });
    }
}
