package net.hyper.mc.server.player;

import balbucio.sqlapi.model.ConditionValue;
import balbucio.sqlapi.sqlite.SQLiteInstance;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.server.CraftHyperSpigot;
import net.minecraft.server.EntityPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerContainer {

    private static PlayerContainer instance;

    public static PlayerContainer getInstance() {
        return instance;
    }

    public static void setInstance(PlayerContainer instance) {
        PlayerContainer.instance = instance;
    }

    private ConcurrentHashMap<String, JSONObject> container = new ConcurrentHashMap<>();
    private CraftServer server;
    private HyperMessageBroker broker;
    private boolean sync = false;

    public PlayerContainer(CraftServer server){
        setInstance(this);
        this.server = server;
        server.getSQLiteInstance().createTable("playercontainer", "name VARCHAR(255), data TEXT");
        broker = server.getHyperSpigot().getMessenger();
        sync = ((CraftHyperSpigot) server.getHyperSpigot()).getConfiguration().getBoolean("playerContainer.sync") && broker != null;
        if(sync){
            broker.registerConsumer("hyperspigot-playercontainer", m -> {
                JSONObject update = new JSONObject(m.getValue());
                String player = update.getString("player");

                if(container.containsKey(player)){
                    container.get(player).put(update.getString("key"), update.get("value"));
                }

                SQLiteInstance sqlite = server.getSQLiteInstance();

                if(sqlite.exists(new ConditionValue[]{
                        new ConditionValue("name", ConditionValue.Conditional.EQUALS, player, ConditionValue.Operator.NULL)
                }, "playercontainer")){
                    JSONObject existent = new JSONObject(sqlite.get(new ConditionValue[]{
                            new ConditionValue("name", ConditionValue.Conditional.EQUALS, player, ConditionValue.Operator.NULL)
                    }, "data", "playercontainer"));

                    sqlite.set(new ConditionValue[]{
                            new ConditionValue("name", ConditionValue.Conditional.EQUALS, player, ConditionValue.Operator.NULL)
                    }, "data", existent.put(update.getString("key"), update.get("value")).toString(), "playercontainer");

                } else{
                    sqlite.insert("name, data", "'"+player+"', '"+new JSONObject().put(update.getString("key"), update.get("value")).toString()+"'", "playercontainer");
                }
            });
        }
    }

    public static Object getData(Player player, String key){
        return instance.container.getOrDefault(player.getName(), new JSONObject()).optString(key, null);
    }

    public static void setData(Player player, String key, Object obj){
        JSONObject json = instance.container.getOrDefault(player.getName(), new JSONObject());
        json.put(key, obj);
        instance.container.replace(player.getName(), json);
        instance.server.getSQLiteInstance().set(new ConditionValue[]{
                new ConditionValue("name", ConditionValue.Conditional.EQUALS, player.getName(), ConditionValue.Operator.NULL)
        }, "data", instance.container.get(player.getName()).toString(), "playercontainer");

        if(instance.sync){
            instance.broker.sendMessage("hyperspigot-playercontainer", new JSONObject().put("key", key).put("value", obj).put("player", player.getName()).toString());
        }
    }

    public static Map<String, Object> getMap(Player player){
        return instance.container.getOrDefault(player.getName(), new JSONObject()).toMap();
    }

    public static void addPlayer(EntityPlayer player){
        SQLiteInstance sqlite = instance.server.getSQLiteInstance();
        if(sqlite.exists(new ConditionValue[]{
                new ConditionValue("name", ConditionValue.Conditional.EQUALS, player.getName(), ConditionValue.Operator.NULL)
        }, "playercontainer")){
            instance.container.put(player.getName(), new JSONObject(sqlite.get(new ConditionValue[]{
                    new ConditionValue("name", ConditionValue.Conditional.EQUALS, player.getName(), ConditionValue.Operator.NULL)
            }, "data", "playercontainer")));
        } else{
            sqlite.insert("name, data", "'"+player.getName()+"', '"+new JSONObject().toString()+"'", "playercontainer");
            instance.container.put(player.getName(), new JSONObject());
        }
    }

}
