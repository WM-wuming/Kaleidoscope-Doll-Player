package kaleidoscopeDollPlayer.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 从 Mojang 公共接口按名字解析完整玩家档案（id + name + textures 属性）。
 *
 * <p>两跳均无需登录，服务端（整合/专用）与客户端共用：
 * 名字 → UUID → sessionserver 的 textures 属性。
 * sessionserver 的 properties 是「[{name, value}]」数组格式，解析时需按数组处理。
 */
public final class PlayerProfileFetcher {
    private static final Pattern UUID_HEX = Pattern.compile("[a-fA-F0-9]{32}");
    private static final int MAX_PROFILE_BYTES = 65_536;

    private PlayerProfileFetcher() {
    }

    /** 按名字解析完整档案；查无此人或接口不可用时返回 null。 */
    @Nullable
    public static GameProfile fetchByName(String name) throws Exception {
        if (!DollSkinProfiles.isPlayerName(name)) {
            return null;
        }
        String id = lookupId(name);
        if (id == null) {
            byte[] png = SkinImages.fetch(URI.create("https://littleskin.cn/skin/" + name + ".png"));
            return SkinSnapshot.littleSkin(name, png);
        }

        String profileJson = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
        if (profileJson == null) {
            return null;
        }

        JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
        JsonArray properties = profile.getAsJsonArray("properties");
        if (properties == null) {
            return null;
        }

        ArrayListMultimap<String, Property> mutable = ArrayListMultimap.create();
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if (!property.has("name") || !property.has("value")) {
                continue;
            }
            String propertyName = property.get("name").getAsString();
            String value = property.get("value").getAsString();
            if ("textures".equals(propertyName) && !value.isEmpty()) {
                mutable.put(propertyName, new Property(propertyName, value));
            }
        }
        if (!mutable.containsKey("textures")) {
            return null;
        }

        String profileName = profile.has("name") ? profile.get("name").getAsString() : name;
        try {
            UUID uuid = UUID.fromString(withDashes(profile.has("id") ? profile.get("id").getAsString() : id));
            return new GameProfile(uuid, profileName, new PropertyMap(mutable));
        } catch (IllegalArgumentException e) {
            return new GameProfile(null, profileName, new PropertyMap(mutable));
        }
    }

    /** Only an explicit not-found response permits trying a different skin provider. */
    @Nullable
    private static String lookupId(String name) throws IOException {
        String json;
        try {
            json = httpGet("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name);
        } catch (IOException error) {
            json = httpGet("https://api.mojang.com/users/profiles/minecraft/" + name);
        }
        return readId(json);
    }

    @Nullable
    private static String readId(@Nullable String json) throws IOException {
        if (json == null) {
            return null;
        }
        JsonElement id = JsonParser.parseString(json).getAsJsonObject().get("id");
        if (id == null || !id.isJsonPrimitive() || !UUID_HEX.matcher(id.getAsString()).matches()) {
            throw new IOException("Invalid player ID response");
        }
        return id.getAsString();
    }

    /** Mojang 返回的是无连字符 UUID，转成标准 8-4-4-4-12 格式。 */
    private static String withDashes(String id) {
        if (id.length() != 32) {
            return id;
        }
        return id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                + "-" + id.substring(16, 20) + "-" + id.substring(20);
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("User-Agent", KaleidoscopeDollPlayer.MOD_ID);
        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND || status == HttpURLConnection.HTTP_NO_CONTENT) {
                return null;
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Profile lookup returned HTTP " + status);
            }
            try (var in = connection.getInputStream()) {
                byte[] bytes = in.readNBytes(MAX_PROFILE_BYTES + 1);
                if (bytes.length > MAX_PROFILE_BYTES) {
                    throw new IOException("Player profile is too large");
                }
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
