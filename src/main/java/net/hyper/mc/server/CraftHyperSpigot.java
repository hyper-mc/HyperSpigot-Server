package net.hyper.mc.server;

import lombok.Data;
import lombok.Getter;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.spigot.HyperSpigot;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;

import java.io.File;
import java.nio.file.Files;

@Data
@Getter
public class CraftHyperSpigot implements HyperSpigot {

    private File configFile = new File("/hyperspigot.yml");
    private Configuration configuration;
    private HyperMessageBroker messageBroker;
    private CraftServer server;

    public CraftHyperSpigot(CraftServer server) {
        this.server = server;
        createAndLoadFiles();
        configureMessageBroker();
    }

    private void createAndLoadFiles() {
        try {
            if (!configFile.exists()) {
                Files.copy(this.getClass().getResourceAsStream("/hyper-spigot.yml"), configFile.toPath());
            }
            configuration = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureMessageBroker(){
        if(configuration.getBoolean("hypermessagebroker.active")){
            messageBroker = new HyperMessageBroker(
                    configuration.getString("hypermessagebroker.ip"),
                    configuration.getInt("hypermessagebroker.port"),
                    server.getResponsiveScheduler());
        }
    }

    @Override
    public HyperMessageBroker getMessenger() {
        return null;
    }
}
