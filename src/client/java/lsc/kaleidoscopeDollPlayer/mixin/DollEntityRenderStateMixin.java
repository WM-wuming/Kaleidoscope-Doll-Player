package lsc.kaleidoscopeDollPlayer.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.render.DollEntityRenderState;
import com.mojang.authlib.GameProfile;
import lsc.kaleidoscopeDollPlayer.duck.PlayerDollRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 给上游 DollEntityRenderState 增加主人档案字段
 */
@Mixin(value = DollEntityRenderState.class, remap = false)
public class DollEntityRenderStateMixin implements PlayerDollRenderState {
    @Unique
    private GameProfile kaleidoscopeDollPlayer$ownerProfile;

    @Override
    public @Nullable GameProfile kaleidoscopeDollPlayer$getOwnerProfile() {
        return this.kaleidoscopeDollPlayer$ownerProfile;
    }

    @Override
    public void kaleidoscopeDollPlayer$setOwnerProfile(@Nullable GameProfile profile) {
        this.kaleidoscopeDollPlayer$ownerProfile = profile;
    }
}
