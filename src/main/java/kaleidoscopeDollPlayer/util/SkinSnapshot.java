package kaleidoscopeDollPlayer.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;

public final class SkinSnapshot {
    private static final String TEXTURES = "textures";
    private static final String PNG = "doll_png";
    private static final String LIVE = "doll_live";
    private static final ConcurrentMap<String, Optional<Skin>> PARSED = CacheBuilder.newBuilder()
            .maximumSize(512).<String, Optional<Skin>>build().asMap();

    private SkinSnapshot() {}

    public static GameProfile create(@Nullable GameProfile owner, byte[] png, boolean slim) {
        return create(owner, png, slim, true);
    }

    static GameProfile littleSkin(String name, byte[] png) {
        UUID id = UUID.nameUUIDFromBytes(("littleskin:" + name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
        return create(new GameProfile(id, name), png, false, false);
    }

    private static GameProfile create(@Nullable GameProfile owner, byte[] png, boolean slim, boolean locked) {
        JsonObject skin = new JsonObject();
        skin.addProperty(PNG, Base64.getEncoder().encodeToString(png));
        if (!locked) {
            skin.addProperty(LIVE, true);
        }
        if (slim) {
            JsonObject metadata = new JsonObject();
            metadata.addProperty("model", "slim");
            skin.add("metadata", metadata);
        }
        JsonObject textures = new JsonObject();
        textures.add("SKIN", skin);
        JsonObject root = new JsonObject();
        root.add(TEXTURES, textures);
        String payload = Base64.getEncoder().encodeToString(root.toString().getBytes(StandardCharsets.UTF_8));
        return new GameProfile(owner == null ? UUID.nameUUIDFromBytes(png) : owner.id(),
                owner == null ? "" : owner.name(),
                new PropertyMap(ImmutableMultimap.of(TEXTURES, new Property(TEXTURES, payload))));
    }

    public static boolean isLocked(@Nullable GameProfile profile) {
        return skin(profile) instanceof Embedded embedded && embedded.locked();
    }

    public static CompletableFuture<GameProfile> lock(GameProfile profile) {
        switch (skin(profile)) {
            case Embedded skin -> {
                return CompletableFuture.completedFuture(create(profile, skin.png(), skin.slim()));
            }
            case Remote skin -> {
                return SkinImages.download(skin.url()).thenApply(png -> create(profile, png, skin.slim()));
            }
            case null -> {
                return CompletableFuture.failedFuture(new IllegalArgumentException("No available skin"));
            }
        }
    }

    public static @Nullable Skin skin(@Nullable GameProfile profile) {
        if (profile == null) {
            return null;
        }
        var textures = profile.properties().get(TEXTURES).iterator();
        return textures.hasNext() ? parse(textures.next().value()) : null;
    }

    public static @Nullable Skin parse(String payload) {
        if (payload.isEmpty() || payload.length() > 32_767) {
            return null;
        }
        return PARSED.computeIfAbsent(payload, value -> Optional.ofNullable(decode(value))).orElse(null);
    }

    private static @Nullable Skin decode(String payload) {
        try {
            String json = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
            JsonObject skin = JsonParser.parseString(json).getAsJsonObject()
                    .getAsJsonObject(TEXTURES).getAsJsonObject("SKIN");
            JsonObject metadata = skin.getAsJsonObject("metadata");
            boolean slim = metadata != null && metadata.has("model")
                    && "slim".equals(metadata.get("model").getAsString());
            if (skin.has(PNG)) {
                byte[] png = Base64.getDecoder().decode(skin.get(PNG).getAsString());
                boolean locked = !skin.has(LIVE) || !skin.get(LIVE).getAsBoolean();
                return SkinImages.hasSkinHeader(png) && png.length <= SkinImages.MAX_SNAPSHOT_BYTES
                        ? new Embedded(png, slim, UUID.nameUUIDFromBytes(png), locked) : null;
            }
            return new Remote(SkinImages.mojangUrl(skin.get("url").getAsString()), slim);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public sealed interface Skin permits Embedded, Remote {
        boolean slim();
    }

    public record Embedded(byte[] png, boolean slim, UUID id, boolean locked) implements Skin {}

    public record Remote(java.net.URI url, boolean slim) implements Skin {}
}
