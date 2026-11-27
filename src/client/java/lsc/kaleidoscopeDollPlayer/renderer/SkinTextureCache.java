package lsc.kaleidoscopeDollPlayer.renderer;

import com.geckolib.constant.dataticket.DataTicket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import lsc.kaleidoscopeDollPlayer.util.DollSkinProfiles;
import lsc.kaleidoscopeDollPlayer.util.PlayerProfileFetcher;
import lsc.kaleidoscopeDollPlayer.util.SkinImages;
import lsc.kaleidoscopeDollPlayer.util.SkinSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 直接从物品/方块数据组件里的 {@code textures} 属性解析并托管玩偶皮肤纹理。
 *
 * <p>刻意不经过原版皮肤加载路径：皮肤补丁类模组会接管那条路径，并且对
 * 同一个 {@link GameProfile} 可能返回空纹理，导致玩偶退化成默认皮肤。
 * 玩偶的皮肤数据本来就完整地存在档案属性里，直接用它即可。
 *
 * <p>URL 到纹理标识符的映射带缓存，同一个皮肤只会下载并注册一次。
 */
public final class SkinTextureCache {

    /** 渲染状态中传递玩偶档案的 DataTicket（GeckoLib 5 两阶段渲染）。 */
    public static final DataTicket<GameProfile> PROFILE_TICKET = DataTicket.create("doll_profile", GameProfile.class);

    private static final Identifier DEFAULT_TEXTURE =
            Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "textures/doll_default_steve.png");

    private static final int RETRY_DELAY_SECONDS = 30;
    private static final ThreadPoolExecutor SKIN_IO = new ThreadPoolExecutor(
            4, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128),
            Thread.ofPlatform().daemon().name("kaleidoscope-doll-player-skin-", 0).factory());

    /** 皮肤 URL → 已注册的纹理标识符。 */
    private static final Map<String, Identifier> TEXTURES = new ConcurrentHashMap<>();

    /** 正在下载中的 URL，避免每帧重复发起请求。 */
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Cache<String, Boolean> FAILED_DOWNLOADS = retryCache();

    /** 客户端兜底解析结果（服务端没抓到 textures 时按名字直接查 Mojang 公共 API）。 */
    private static final Cache<String, SkinSnapshot.Skin> CLIENT_RESOLVED = CacheBuilder.newBuilder()
            .maximumSize(2048).expireAfterWrite(10, TimeUnit.MINUTES).build();
    private static final Set<String> PENDING_NAMES = ConcurrentHashMap.newKeySet();
    private static final Cache<String, Boolean> FAILED_LOOKUPS = retryCache();

    private SkinTextureCache() {
    }

    private static Cache<String, Boolean> retryCache() {
        return CacheBuilder.newBuilder().maximumSize(2048)
                .expireAfterWrite(RETRY_DELAY_SECONDS, TimeUnit.SECONDS).build();
    }

    /**
     * 取该档案对应的皮肤纹理。
     *
     * <p>数据缺失或尚未下载完成时返回默认皮肤；下载完成后会自动切换到真实皮肤。
     */
    public static Identifier get(GameProfile profile) {
        SkinSnapshot.Skin data = skinData(profile);
        if (data == null) {
            return DEFAULT_TEXTURE;
        }

        String key = data instanceof SkinSnapshot.Embedded embedded ? "snapshot:" + embedded.id()
                : ((SkinSnapshot.Remote) data).url().toString();
        Identifier cached = TEXTURES.get(key);
        if (cached != null) {
            return cached;
        }

        requestTexture(key, data);
        return DEFAULT_TEXTURE;
    }

    /** 判断该档案是否使用瘦模型（slim）。 */
    public static boolean isSlim(GameProfile profile) {
        SkinSnapshot.Skin data = skinData(profile);
        return data != null && data.slim();
    }

    private static SkinSnapshot.Skin skinData(GameProfile profile) {
        if (profile == null) {
            return null;
        }
        SkinSnapshot.Skin data = SkinSnapshot.skin(profile);
        if (data != null) {
            return data;
        }
        String name = profile.name();
        return DollSkinProfiles.isPlayerName(name) ? clientResolved(name) : null;
    }

    private static SkinSnapshot.Skin clientResolved(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        SkinSnapshot.Skin cached = CLIENT_RESOLVED.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        if (FAILED_LOOKUPS.getIfPresent(key) == null && PENDING_NAMES.add(key)) {
            enqueue(key, PENDING_NAMES, FAILED_LOOKUPS, () -> {
                try {
                    SkinSnapshot.Skin data = SkinSnapshot.skin(PlayerProfileFetcher.fetchByName(name));
                    if (data == null) {
                        FAILED_LOOKUPS.put(key, true);
                    } else {
                        CLIENT_RESOLVED.put(key, data);
                    }
                } catch (Exception e) {
                    FAILED_LOOKUPS.put(key, true);
                } finally {
                    PENDING_NAMES.remove(key);
                }
            });
        }
        return null;
    }

    private static void requestTexture(String key, SkinSnapshot.Skin skin) {
        if (FAILED_DOWNLOADS.getIfPresent(key) != null || !PENDING.add(key)) {
            return;
        }
        if (skin instanceof SkinSnapshot.Embedded embedded) {
            enqueue(key, PENDING, FAILED_DOWNLOADS, () -> loadTexture(key, embedded.png()));
        } else if (skin instanceof SkinSnapshot.Remote remote) {
            SkinImages.download(remote.url()).whenComplete((png, error) -> {
                if (error == null) {
                    loadTexture(key, png);
                } else {
                    failTexture(key);
                }
            });
        }
    }

    private static void loadTexture(String key, byte[] png) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
            Minecraft minecraft = Minecraft.getInstance();
            try {
                minecraft.execute(() -> registerTexture(minecraft, key, skinIdentifier(key), image));
            } catch (RuntimeException e) {
                image.close();
                throw e;
            }
        } catch (Exception e) {
            failTexture(key);
        }
    }

    private static void failTexture(String key) {
        FAILED_DOWNLOADS.put(key, true);
        PENDING.remove(key);
    }

    private static void registerTexture(Minecraft minecraft, String url, Identifier id, NativeImage image) {
        DynamicTexture texture = null;
        try {
            texture = new DynamicTexture(() -> "doll_skin", image);
            minecraft.getTextureManager().register(id, texture);
            TEXTURES.put(url, id);
        } catch (RuntimeException e) {
            FAILED_DOWNLOADS.put(url, true);
            if (texture == null) {
                image.close();
            } else {
                texture.close();
            }
            KaleidoscopeDollPlayer.LOGGER.debug("Could not register doll skin {}", id, e);
        } finally {
            PENDING.remove(url);
        }
    }

    private static void enqueue(String key, Set<String> pending, Cache<String, Boolean> failures, Runnable task) {
        try {
            SKIN_IO.execute(task);
        } catch (RejectedExecutionException e) {
            // Back off instead of retrying the same submission on every rendered frame.
            failures.put(key, true);
            pending.remove(key);
        }
    }

    private static Identifier skinIdentifier(String url) {
        return Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "doll_skin/" + digest(url));
    }

    /**
     * 用 SHA-1 前 16 位作为纹理 ID 的后缀。
     *
     * <p>不用 {@code String.hashCode()}：它只有 32 位，皮肤一多就可能碰撞，
     * 而碰撞的两个 URL 会共用同一个已注册纹理（纹理注册对已存在的 ID 不会覆盖），
     * 表现为「不同玩偶显示同一张皮」。
     */
    private static String digest(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(url.hashCode()); // 理论上不会发生
        }
    }

}
