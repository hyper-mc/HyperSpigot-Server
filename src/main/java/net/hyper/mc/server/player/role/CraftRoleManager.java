package net.hyper.mc.server.player.role;

import com.google.gson.Gson;
import net.hyper.mc.msgbrokerapi.HyperMessageBroker;
import net.hyper.mc.spigot.player.role.Role;
import net.hyper.mc.spigot.player.role.RoleManager;
import org.bukkit.entity.Player;
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

    @Override
    public Role getRole(Player player) {
        Role role;
        role = roles.values().stream()
                .filter(r -> player.hasPermission(r.getPermission())).findFirst().orElse(getDefault());
        return role;
    }

    @Override
    public Role getDefault(){
        Role role;
        role = roles.values().stream().filter(r -> r.getOrder() == (roles.size()+1)).findFirst().orElse(null);
        return role;
    }
}
