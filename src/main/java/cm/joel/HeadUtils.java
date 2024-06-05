package cm.joel;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeadUtils {
    private static final Map<EntityType, HeadConfiguration> headConfigurations = new HashMap<>();

    public static void setHeadConfiguration(EntityType entityType, double dropProbability, String headTexture, String headName) {
        headConfigurations.put(entityType, new HeadConfiguration(dropProbability, headTexture, headName));
    }

    public static double getDropProbability(EntityType entityType) {
        HeadConfiguration configuration = headConfigurations.get(entityType);
        return (configuration != null) ? configuration.getDropProbability() : 0;
    }

    public static ItemStack getHead(EntityType entityType) {
        HeadConfiguration configuration = headConfigurations.get(entityType);
        if (configuration != null) {
            return createHead(configuration.getHeadTexture(), configuration.getHeadName());
        }
        return new ItemStack(Material.AIR);
    }

    private static ItemStack createHead(String texture, String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (texture != null && !texture.isEmpty()) {
                GameProfile profile = new GameProfile(UUID.randomUUID(), null);
                profile.getProperties().put("textures", new Property("textures", texture));
                Field profileField;
                try {
                    profileField = meta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(meta, profile);
                } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    private static class HeadConfiguration {
        private final double dropProbability;
        private final String headTexture;
        private final String headName;

        public HeadConfiguration(double dropProbability, String headTexture, String headName) {
            this.dropProbability = dropProbability;
            this.headTexture = headTexture;
            this.headName = headName;
        }

        public double getDropProbability() {
            return dropProbability;
        }

        public String getHeadTexture() {
            return headTexture;
        }

        public String getHeadName() {
            return headName;
        }
    }
}
