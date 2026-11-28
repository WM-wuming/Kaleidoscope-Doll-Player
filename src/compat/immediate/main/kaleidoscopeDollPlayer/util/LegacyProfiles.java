package kaleidoscopeDollPlayer.util;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Optional;
import java.util.UUID;

public final class LegacyProfiles {
    private LegacyProfiles() {
    }

    public static ResolvableProfile unresolved(String name) {
        return new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap());
    }

    public static GameProfile create(UUID id, String name, Multimap<String, Property> properties) {
        GameProfile profile = new GameProfile(id, name);
        profile.getProperties().putAll(properties);
        return profile;
    }
}
