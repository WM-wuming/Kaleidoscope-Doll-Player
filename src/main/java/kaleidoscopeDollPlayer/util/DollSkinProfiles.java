package kaleidoscopeDollPlayer.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.item.DollItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class DollSkinProfiles {
    public static final int CHECK_INTERVAL_TICKS = 40;
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final CompletableFuture<Lookup> EMPTY = CompletableFuture.completedFuture(null);
    private static final Cache<MinecraftServer, Cache<String, CompletableFuture<Lookup>>> SERVERS =
            CacheBuilder.newBuilder().weakKeys().build();

    private record Lookup(ResolvableProfile profile, boolean fresh) {}

    private DollSkinProfiles() {}

    public static boolean hasTextures(@Nullable ResolvableProfile profile) {
        return profile != null && profile.partialProfile().properties().containsKey("textures");
    }

    public static boolean isPlayerName(@Nullable String name) {
        return name != null && PLAYER_NAME.matcher(name).matches();
    }

    public static @Nullable GameProfile updatedProfile(MinecraftServer server, @Nullable GameProfile current) {
        if (current == null || SkinSnapshot.isLocked(current) || !isPlayerName(current.name())) {
            return null;
        }
        Lookup resolved = lookup(server, current.name()).getNow(null);
        if (resolved == null || (!resolved.fresh() && SkinSnapshot.skin(current) != null)) {
            return null;
        }
        GameProfile updated = resolved.profile().partialProfile();
        return updated.equals(current) ? null : updated;
    }

    public static CompletableFuture<ResolvableProfile> resolve(MinecraftServer server, String name) {
        return lookup(server, name).thenApply(result -> result == null ? null : result.profile());
    }

    private static CompletableFuture<Lookup> lookup(MinecraftServer server, String name) {
        if (!isPlayerName(name)) {
            return EMPTY;
        }
        return profiles(server).computeIfAbsent(name.toLowerCase(Locale.ROOT),
                key -> fetch(server, name, 3).exceptionally(error -> null));
    }

    // Cache the lookup, not a reference to the slot that originally held the doll.
    private static ConcurrentMap<String, CompletableFuture<Lookup>> profiles(MinecraftServer server) {
        return SERVERS.asMap().computeIfAbsent(server, key -> CacheBuilder.newBuilder()
                .maximumSize(1024).expireAfterWrite(3, TimeUnit.MINUTES).<String, CompletableFuture<Lookup>>build())
                .asMap();
    }

    private static CompletableFuture<Lookup> fetch(MinecraftServer server, String name, int retries) {
        // Prefer a fresh session response over the vanilla profile resolver's cached skin.
        return CompletableFuture.supplyAsync(() -> {
            try {
                return PlayerProfileFetcher.fetchByName(name);
            } catch (Exception ignored) {
                return null;
            }
        })
                .thenCompose(profile -> {
                    if (SkinSnapshot.skin(profile) != null) {
                        return CompletableFuture.completedFuture(new Lookup(resolved(profile), true));
                    }
                    return ResolvableProfile.createUnresolved(name).resolveProfile(server.services().profileResolver())
                            .handle((fallback, error) -> SkinSnapshot.skin(fallback) instanceof SkinSnapshot.Remote
                                    ? new Lookup(resolved(fallback), false) : null);
                }).thenCompose(profile -> {
                    if (profile != null || retries == 0) {
                        return CompletableFuture.completedFuture(profile);
                    }
                    return CompletableFuture.completedFuture(null).thenComposeAsync(
                            ignored -> fetch(server, name, retries - 1),
                            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
                });
    }

    private static ResolvableProfile resolved(GameProfile profile) {
        return ResolvableProfile.createResolved(DollItem.normalizeProfile(profile));
    }
}
