package org.bukkit.craftbukkit.inventory;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.server.ItemCarrotStick;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemCreator;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class AbstractItemCreator extends ItemCreator {

    public AbstractItemCreator(ItemStack item) {
        super(item);
    }

    public AbstractItemCreator(Material mat) {
        super(mat);
    }

    public AbstractItemCreator(Material mat, int amount) {
        super(mat, amount);
    }

    public AbstractItemCreator(Material mat, byte data) {
        super(mat, data);
    }

    public AbstractItemCreator(Material mat, byte data, int amount) {
        super(mat, data, amount);
    }

    @Override
    public ItemCreator withTexture(String url) {
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
        try {
            headMetaClass.getField("profile").set(headMeta, profile);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        this.item.setItemMeta(headMeta);
        return this;
    }
}
