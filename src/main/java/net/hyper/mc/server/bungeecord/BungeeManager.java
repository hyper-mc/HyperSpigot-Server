package net.hyper.mc.server.bungeecord;

import net.hyper.mc.spigot.bungeecord.IBungeeManager;
import org.bukkit.craftbukkit.CraftServer;

public class BungeeManager implements IBungeeManager {

    private static BungeeManager instance;

    public static BungeeManager getInstance() {
        return instance;
    }

    private CraftServer server;

    public BungeeManager(CraftServer server){
        instance = this;
        this.server = server;
        server.getMessenger().registerOutgoingPluginChannel(null, "BungeeCord");
    }

    public void pluginMessage(byte[] data){

    }
}
