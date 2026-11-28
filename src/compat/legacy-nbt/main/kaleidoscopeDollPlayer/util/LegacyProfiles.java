package kaleidoscopeDollPlayer.util;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LegacyProfiles {
    private static final String PROFILE = "kaleidoscope_doll_player_profile";

    private LegacyProfiles() {}

    public static boolean has(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains(PROFILE, Tag.TAG_STRING);
    }

    public static GameProfile read(ItemStack stack) {
        return stack != null && has(stack) ? PlayerDollEntity.decodeProfile(stack.getTag().getString(PROFILE)) : null;
    }

    public static void write(ItemStack stack, GameProfile profile) {
        if (profile == null) stack.removeTagKey(PROFILE);
        else stack.getOrCreateTag().putString(PROFILE, PlayerDollEntity.encodeProfile(profile));
    }

    public static void copy(ItemStack source, ItemStack target) {
        write(target, read(source));
    }

    public static GameProfile unresolved(String name) {
        return new GameProfile(offlineId(name), name);
    }

    public static CompletableFuture<GameProfile> resolve(MinecraftServer server, String name) {
        return CompletableFuture.supplyAsync(() -> {
            var online = server.getPlayerList().getPlayerByName(name);
            return online == null ? null : online.getGameProfile();
        }, server);
    }

    public static GameProfile create(UUID id, String name, Multimap<String, Property> properties) {
        String safeName = name == null ? "" : name;
        GameProfile profile = new GameProfile(id == null ? offlineId(safeName) : id, safeName);
        profile.getProperties().putAll(properties);
        return profile;
    }

    private static UUID offlineId(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}
