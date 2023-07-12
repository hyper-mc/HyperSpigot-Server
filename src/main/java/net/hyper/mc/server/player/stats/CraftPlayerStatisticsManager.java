package net.hyper.mc.server.player.stats;

import balbucio.sqlapi.model.ConditionValue;
import balbucio.sqlapi.sqlite.SQLiteInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.hyper.mc.spigot.player.stats.PlayerStatisticsManager;
import net.hyper.mc.spigot.player.stats.StatisticsContainer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONObject;

@Data
public class CraftPlayerStatisticsManager implements PlayerStatisticsManager {

    private Player player;
    private SQLiteInstance sqlite;

    public CraftPlayerStatisticsManager(Player player){
        this.player = player;
        this.sqlite = Bukkit.getSQLite();
    }

    private ConditionValue[] getConditionValues(String key){
        ConditionValue[] v = new ConditionValue[]{
                new ConditionValue("name", ConditionValue.Conditional.EQUALS, player.getName(), ConditionValue.Operator.AND),
                new ConditionValue("key", ConditionValue.Conditional.EQUALS, key, ConditionValue.Operator.NULL)
        };
        return v;
    }

    @Override
    public StatisticsContainer getStats(String key) {
        if(sqlite.exists(getConditionValues(key), "hyperstats")){
            return new StatisticsContainer(this, new JSONObject((String) sqlite.get(getConditionValues(key), "data", "hyperstats")));
        }
        return new StatisticsContainer(this, new JSONObject());
    }

    @Override
    public void save(String key, StatisticsContainer container) {
        if(sqlite.exists(getConditionValues(key), "hyperstats")){
            sqlite.set(getConditionValues(key), "data", container.getJson().toString(), "hyperstats");
        } else{
            sqlite.insert("name, key, data", "'"+player.getName()+"','"+key+"','"+container.getJson().toString()+"'", "hyperstats");
        }
    }
}
