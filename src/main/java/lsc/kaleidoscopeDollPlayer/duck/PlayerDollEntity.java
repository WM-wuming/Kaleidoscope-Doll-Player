package lsc.kaleidoscopeDollPlayer.duck;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 上游 DollEntity 的主人档案 duck 接口（由 {@code DollEntityOwnerMixin} 实现）：
 * 实体玩偶携带玩家皮肤档案，渲染时用于显示玩家皮肤。
 */
public interface PlayerDollEntity {
    /** 存档 NBT 与同步字符串共用的键 */
    String OWNER_TAG = "workshop_owner";

    @Nullable
    GameProfile kaleidoscopeDollPlayer$getOwnerProfile();

    void kaleidoscopeDollPlayer$setOwnerProfile(@Nullable GameProfile profile);

    /** 玩家档案 → JSON 字符串（实体同步数据与存档 NBT 共用该编码） */
    static String encodeProfile(GameProfile profile) {
        JsonObject obj = new JsonObject();
        if (profile.id() != null) {
            obj.addProperty("id", profile.id().toString());
        }
        if (profile.name() != null) {
            obj.addProperty("name", profile.name());
        }
        profile.properties().get("textures").stream().findFirst()
                .ifPresent(p -> obj.addProperty("textures", p.value()));
        return obj.toString();
    }

    @Nullable
    static GameProfile decodeProfile(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            UUID id = obj.has("id") ? UUID.fromString(obj.get("id").getAsString()) : null;
            String name = obj.has("name") ? obj.get("name").getAsString() : null;
            com.google.common.collect.ArrayListMultimap<String, Property> mutable = com.google.common.collect.ArrayListMultimap.create();
            if (obj.has("textures")) {
                mutable.put("textures", new Property("textures", obj.get("textures").getAsString()));
            }
            return new GameProfile(id, name, new com.mojang.authlib.properties.PropertyMap(mutable));
        } catch (Exception e) {
            return null;
        }
    }
}
