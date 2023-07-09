package net.hyper.mc.server.player.scoreboard.tasks;

import balbucio.responsivescheduler.RSTask;
import lombok.RequiredArgsConstructor;
import net.hyper.mc.server.player.scoreboard.CraftBoardManager;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class BoardUpdateTask extends RSTask {

    private static final Predicate<UUID> PLAYER_IS_ONLINE = uuid -> Bukkit.getPlayer(uuid) != null;

    private final CraftBoardManager boardManager;

    @Override
    public void run() {
        boardManager.getScoreboards().
                entrySet().stream().filter(entrySet -> PLAYER_IS_ONLINE.test(entrySet.getKey())).forEach(entrySet -> entrySet.getValue().update());
    }
}
