package net.hyper.mc.server.player.scoreboard;

import balbucio.responsivescheduler.RSTask;
import balbucio.responsivescheduler.ResponsiveScheduler;
import net.hyper.mc.server.CraftHyperSpigot;
import net.hyper.mc.server.event.EventHandler;
import net.hyper.mc.server.player.scoreboard.tasks.BoardUpdateTask;
import net.hyper.mc.spigot.player.scoreboard.board.Board;
import net.hyper.mc.spigot.player.scoreboard.settings.BoardSettings;
import net.hyper.mc.spigot.player.scoreboard.BoardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CraftBoardManager implements BoardManager {

    private BoardSettings boardSettings;
    private Map<UUID, Board> scoreboards;
    private BukkitTask updateTask;

    public CraftBoardManager(BoardSettings boardSettings, CraftHyperSpigot hyperSpigot) {
        this.boardSettings = boardSettings;
        this.scoreboards = new ConcurrentHashMap<>();
        ResponsiveScheduler scheduler = hyperSpigot.getServer().getResponsiveScheduler();
        scheduler.repeatTask(new BoardUpdateTask(this), 2L, 2L);
        if(boardSettings.isDefaultScoreboard())
            hyperSpigot.getServer().getOnlinePlayers().forEach(this::setup);
        EventHandler.add(e -> {
            if(e instanceof PlayerJoinEvent){
                scheduler.runTaskAfter(new RSTask() {
                    @Override
                    public void run() {
                        if(boardSettings.isDefaultScoreboard()) {
                            if (((PlayerJoinEvent) e).getPlayer().isOnline()) {
                                setup(((PlayerJoinEvent) e).getPlayer());
                            }
                        }
                    }
                }, 2L);
            } else if(e instanceof PlayerQuitEvent){
                this.remove(((PlayerQuitEvent)e).getPlayer());
            }
        });

    }

    public void setBoardSettings(BoardSettings boardSettings) {
        this.boardSettings = boardSettings;
        scoreboards.values().forEach(board -> board.setBoardSettings(boardSettings));
    }

    public boolean hasBoard(Player player) {
        return scoreboards.containsKey(player.getUniqueId());
    }

    public Optional<Board> getBoard(Player player) {
        return Optional.ofNullable(scoreboards.get(player.getUniqueId()));
    }

    public void setup(Player player) {
        Optional.ofNullable(scoreboards.remove(player.getUniqueId())).ifPresent(Board::resetScoreboard);
        if (player.getScoreboard() == Bukkit.getScoreboardManager().getMainScoreboard()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        scoreboards.put(player.getUniqueId(), new Board(player, boardSettings));
    }

    private void remove(Player player) {
        Optional.ofNullable(scoreboards.remove(player.getUniqueId())).ifPresent(Board::remove);
    }

    public Map<UUID, Board> getScoreboards() {
        return Collections.unmodifiableMap(scoreboards);
    }

}
