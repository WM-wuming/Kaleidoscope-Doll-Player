package lsc.kaleidoscopeDollPlayer.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.mojang.authlib.GameProfile;
import lsc.kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给上游 DollEntity 增加"主人档案"数据：
 * 追加一个同步字段（STRING，JSON 编码，保证客户端可见），
 * NBT 读写保证存档与实体玩偶物品往返时保留皮肤。
 */
@Mixin(value = DollEntity.class, remap = false)
public class DollEntityOwnerMixin implements PlayerDollEntity {
    @Unique
    private static final EntityDataAccessor<String> kaleidoscopeDollPlayer$DATA_OWNER =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.STRING);
    @Unique
    private String kaleidoscopeDollPlayer$decodedOwnerJson = "";
    @Unique
    private @Nullable GameProfile kaleidoscopeDollPlayer$decodedOwner;

    @Inject(method = "defineSynchedData", at = @At("TAIL"), remap = true)
    private void kaleidoscopeDollPlayer$defineOwnerData(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(kaleidoscopeDollPlayer$DATA_OWNER, "");
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), remap = true)
    private void kaleidoscopeDollPlayer$readOwner(ValueInput input, CallbackInfo ci) {
        input.getString(PlayerDollEntity.OWNER_TAG).ifPresent(this::kaleidoscopeDollPlayer$setOwnerProfileJson);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), remap = true)
    private void kaleidoscopeDollPlayer$writeOwner(ValueOutput output, CallbackInfo ci) {
        String json = this.kaleidoscopeDollPlayer$getOwnerProfileJson();
        if (!json.isEmpty()) {
            output.putString(PlayerDollEntity.OWNER_TAG, json);
        }
    }

    @Unique
    private String kaleidoscopeDollPlayer$getOwnerProfileJson() {
        return ((DollEntity) (Object) this).getEntityData().get(kaleidoscopeDollPlayer$DATA_OWNER);
    }

    @Unique
    private void kaleidoscopeDollPlayer$setOwnerProfileJson(String json) {
        ((DollEntity) (Object) this).getEntityData().set(kaleidoscopeDollPlayer$DATA_OWNER, json);
    }

    @Override
    public @Nullable GameProfile kaleidoscopeDollPlayer$getOwnerProfile() {
        String json = this.kaleidoscopeDollPlayer$getOwnerProfileJson();
        if (!json.equals(this.kaleidoscopeDollPlayer$decodedOwnerJson)) {
            this.kaleidoscopeDollPlayer$decodedOwner = json.isEmpty() ? null : PlayerDollEntity.decodeProfile(json);
            this.kaleidoscopeDollPlayer$decodedOwnerJson = json;
        }
        return this.kaleidoscopeDollPlayer$decodedOwner;
    }

    @Override
    public void kaleidoscopeDollPlayer$setOwnerProfile(@Nullable GameProfile profile) {
        this.kaleidoscopeDollPlayer$setOwnerProfileJson(profile == null ? "" : PlayerDollEntity.encodeProfile(profile));
    }
}
