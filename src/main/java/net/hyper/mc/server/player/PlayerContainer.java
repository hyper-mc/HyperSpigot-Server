package net.hyper.mc.server.player;

import balbucio.sqlapi.model.ConditionValue;
import balbucio.sqlapi.sqlite.SQLiteInstance;
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

    public PlayerContainer(CraftServer server){
        setInstance(this);
        this.server = server;
        server.getSQLiteInstance().createTable("playercontainer", "name VARCHAR(255), data TEXT");
    }

    public static Object getData(Player player, String key){
        return instance.container.getOrDefault(player.getName(), new JSONObject()).optString(key, null);
    }

    public static void setData(Player player, String key, Object obj){
        instance.container.get(player.getName()).put(key, obj);
    }

    public static Map<String, Object> getMap(Player player){
        return instance.container.get(player.getName()).toMap();
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
            sqlite.insert("name, data", "'"+player.getName()+"', '"+new JSONObject()+"'", "playercontainer");
            instance.container.put(player.getName(), new JSONObject());
        }
    }

}
