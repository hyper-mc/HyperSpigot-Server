package net.hyper.mc.server.player.role;

import com.google.gson.Gson;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.spigot.player.role.Role;
import net.hyper.mc.spigot.player.role.RoleManager;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class CraftRoleManager implements RoleManager {

    private static CraftRoleManager instance;

    public static CraftRoleManager getInstance() {
        return instance;
    }

    private ConcurrentHashMap<String, Role> roles = new ConcurrentHashMap<>();
    private Gson gson = new Gson();

    public CraftRoleManager(HyperMessageBroker broker){
        broker.registerConsumer("hyperspigot-roles", m -> {
            JSONObject data = new JSONObject((String) m.getValue());
            for(Object g : data.getJSONArray("roles")){
                Role r = gson.fromJson((String) g, Role.class);
                roles.put(r.getName(), r);
            }
        });
    }
}
