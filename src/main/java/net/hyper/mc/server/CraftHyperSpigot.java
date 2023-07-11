package net.hyper.mc.server;

import lombok.Data;
import lombok.Getter;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.server.bungeecord.CraftBungeeManager;
import net.hyper.mc.server.network.CraftNetworkManager;
import net.hyper.mc.server.player.PlayerContainer;
import net.hyper.mc.server.player.party.CraftPartyManager;
import net.hyper.mc.server.player.role.CraftRoleManager;
import net.hyper.mc.server.player.scoreboard.CraftBoardManager;
import net.hyper.mc.spigot.HyperSpigot;
import net.hyper.mc.spigot.bungeecord.BungeeManager;
import net.hyper.mc.spigot.player.FakePlayer;
import net.hyper.mc.spigot.player.scoreboard.BoardManager;
import net.hyper.mc.spigot.player.scoreboard.settings.BoardSettings;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;

import java.io.File;
import java.nio.file.Files;

@Data
@Getter
public class CraftHyperSpigot implements HyperSpigot {

    private File configFile = new File("hyperspigot.yml");
    private Configuration configuration;
    private HyperMessageBroker messageBroker;
    private CraftBungeeManager bungeeManager;
    private CraftPartyManager partyManager;
    private CraftRoleManager roleManager;
    private CraftNetworkManager networkManager;
    private CraftServer server;

    public CraftHyperSpigot(CraftServer server) {
        this.server = server;
        createAndLoadFiles();
    }
    public void loadAndConfigure(){
        if(configuration.getBoolean("hypermessagebroker.active")){
            this.messageBroker = new HyperMessageBroker(
                    configuration.getString("hypermessagebroker.ip"),
                    configuration.getInt("hypermessagebroker.port"),
                    server.getResponsiveScheduler());
        }
        if(configuration.getBoolean("party.active") && configuration.getBoolean("hypermessagebroker.active")){
            this.partyManager = new CraftPartyManager(server);
        }
        bungeeManager = new CraftBungeeManager(server);
        roleManager = new CraftRoleManager(messageBroker);
        networkManager = new CraftNetworkManager(messageBroker);
    }

    private void createAndLoadFiles() {
        try {
            if (!configFile.exists()) {
                Files.copy(this.getClass().getResourceAsStream("/hyperspigot.yml"), configFile.toPath());
            }
            configuration = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public HyperMessageBroker getMessenger() {
        return messageBroker;
    }

    @Override
    public BungeeManager getBungeeManager() {
        return bungeeManager;
    }

    public CraftRoleManager getRoleManager() {
        return roleManager;
    }

    @Override
    public BoardManager createBoardManager(BoardSettings settings) {
        return new CraftBoardManager(settings, this);
    }

    @Override
    public void setData(String name, String key, Object value) {
        PlayerContainer.setData(new FakePlayer(name), key, value);
    }
}
