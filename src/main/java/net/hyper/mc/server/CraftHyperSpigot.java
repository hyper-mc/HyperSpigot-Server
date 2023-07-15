package net.hyper.mc.server;

import lombok.Data;
import lombok.Getter;
import net.hyper.mc.inventories.InventoriesPlugin;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.server.bungeecord.CraftBungeeManager;
import net.hyper.mc.server.command.CashCommand;
import net.hyper.mc.server.lobbies.CraftLobbyManager;
import net.hyper.mc.server.network.CraftNetworkManager;
import net.hyper.mc.server.player.PlayerContainer;
import net.hyper.mc.server.player.party.CraftPartyManager;
import net.hyper.mc.server.player.role.CraftRoleManager;
import net.hyper.mc.server.player.scoreboard.CraftBoardManager;
import net.hyper.mc.spigot.HyperSpigot;
import net.hyper.mc.spigot.bungeecord.BungeeManager;
import net.hyper.mc.spigot.lobbies.LobbyManager;
import net.hyper.mc.spigot.lobbies.ServerLobby;
import net.hyper.mc.spigot.lobbies.WorldLobby;
import net.hyper.mc.spigot.player.FakePlayer;
import net.hyper.mc.spigot.player.scoreboard.BoardManager;
import net.hyper.mc.spigot.player.scoreboard.settings.BoardSettings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private CraftLobbyManager lobbyManager;
    private CraftServer server;

    public CraftHyperSpigot(CraftServer server) {
        this.server = server;
        createAndLoadFiles();
        createTables();
        registerCommands();
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
        lobbyManager = new CraftLobbyManager(server, messageBroker);
    }

    public void createTables(){
        server.getSQLiteInstance().createTable("hyperstats", "name VARCHAR(255), key VARCHAR(255), data TEXT");
    }

    public void registerCommands(){
        Bukkit.registerCommand(CashCommand.class);
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

    /**
     * Copia a pasta de um mundo para ser carregado depois
     *
     * @param source Pasta origem
     * @param target Pasta de destino
     */
    @Override
    public void copyWorldFolder(File source, File target){
        try {
            ArrayList<String> ignore = new ArrayList<String>(Arrays.asList("uid.dat", "session.dat"));
            if(!ignore.contains(source.getName())) {
                if(source.isDirectory()) {
                    if(!target.exists())
                        target.mkdirs();
                    String files[] = source.list();
                    for (String file : files) {
                        File srcFile = new File(source, file);
                        File destFile = new File(target, file);
                        copyWorldFolder(srcFile, destFile);
                    }
                } else {
                    InputStream in = new FileInputStream(source);
                    OutputStream out = new FileOutputStream(target);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0)
                        out.write(buffer, 0, length);
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cria um backup do Mundo para uma pasta privada do plugin
     *
     * @param pluginName Nome do seu Plugin
     * @param world Mundo a ser copiado
     * @return retorna o backup folder
     */
    @Override
    public File createWorldBackup(String pluginName, World world) {
        File folder = new File("plugins/"+pluginName+"/worldBackups/"+world.getName());
        copyWorldFolder(world.getWorldFolder(), folder);
        return folder;
    }

    /**
     * Carrega um mundo
     *
     * @param worldName Nome da pasta do mundo
     * @return retorna o mundo
     */
    @Override
    public World loadWorld(String worldName){
        WorldCreator creator = new WorldCreator(worldName);
        creator.generateStructures(false);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        return creator.createWorld();
    }

    @Override
    public CopyOnWriteArrayList<ServerLobby> getNetworkLobbies() {
        return lobbyManager.getNetworkLobbies();
    }

    @Override
    public CopyOnWriteArrayList<WorldLobby> getLobbies() {
        return lobbyManager.getLobbies();
    }

    @Override
    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public InventoriesPlugin getInventoryPlugin() {
        return ((InventoriesPlugin) Bukkit.getPluginManager().getPlugin("HyperSpigot-Inventories"));
    }
}
