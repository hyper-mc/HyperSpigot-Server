package org.bukkit.craftbukkit.inventory;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.Material;
import org.bukkit.inventory.AbstractItemCreator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Base64;
import java.util.UUID;

public class ItemCreator extends AbstractItemCreator {
    public ItemCreator(ItemStack item) {
        super(item);
    }

    public ItemCreator(Material mat) {
        super(mat);
    }

    public ItemCreator(Material mat, int amount) {
        super(mat, amount);
    }

    public ItemCreator(Material mat, byte data) {
        super(mat, data);
    }

    public ItemCreator(Material mat, byte data, int amount) {
        super(mat, data, amount);
    }

    @Override
    public AbstractItemCreator withTexture(String url) throws NoSuchFieldException, IllegalAccessException {
        if (!this.item.getType().equals(Material.SKULL_ITEM) || url == null) return this;
        url = "http://textures.minecraft.net/texture/" + url;
        ItemMeta meta = item.getItemMeta();
        this.item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        item.setItemMeta(meta);
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        PropertyMap propertyMap = profile.getProperties();
        if (propertyMap == null) {
            throw new IllegalStateException("Profile doesn't contain a property map");
        }
        byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
        propertyMap.put("textures", new Property("textures", new String(encodedData)));
        ItemMeta headMeta = this.item.getItemMeta();
        Class<?> headMetaClass = headMeta.getClass();
        headMetaClass.getField("profile").set(headMeta, profile);
        this.item.setItemMeta(headMeta);
        return this;

    }
}
