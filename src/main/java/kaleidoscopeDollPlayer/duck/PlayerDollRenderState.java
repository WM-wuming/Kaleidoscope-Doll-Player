package kaleidoscopeDollPlayer.duck;

import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

/**
 * 上游 DollEntityRenderState 的主人档案 duck 接口（由 {@code DollEntityRenderStateMixin} 实现）
 */
public interface PlayerDollRenderState {
    @Nullable
    GameProfile kaleidoscopeDollPlayer$getOwnerProfile();

    void kaleidoscopeDollPlayer$setOwnerProfile(@Nullable GameProfile profile);
}
