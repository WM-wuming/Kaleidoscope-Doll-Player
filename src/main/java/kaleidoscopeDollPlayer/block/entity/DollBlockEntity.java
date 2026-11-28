package kaleidoscopeDollPlayer.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.registry.ModBlockEntities;
import kaleidoscopeDollPlayer.util.DollSkinProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DollBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private GameProfile ownerProfile = null;

    public DollBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOLL_BLOCK_ENTITY, pos, state);
    }

    // --- 数据持久化 ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.ownerProfile != null) {
            output.store("Owner", ResolvableProfile.CODEC, ResolvableProfile.createResolved(this.ownerProfile));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.ownerProfile = input.read("Owner", ResolvableProfile.CODEC).map(ResolvableProfile::partialProfile).orElse(null);
    }

    // --- 数据同步 ---

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (this.ownerProfile != null) {
            builder.set(net.minecraft.core.component.DataComponents.PROFILE, ResolvableProfile.createResolved(this.ownerProfile));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- 访问器 ---

    public GameProfile getOwnerProfile() {
        return ownerProfile;
    }

    public void setOwnerProfile(GameProfile profile) {
        this.ownerProfile = kaleidoscopeDollPlayer.item.DollItem.normalizeProfile(profile);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DollBlockEntity doll) {
        if (level instanceof ServerLevel serverLevel
                && (level.getGameTime() + pos.hashCode()) % DollSkinProfiles.CHECK_INTERVAL_TICKS == 0) {
            GameProfile updated = DollSkinProfiles.updatedProfile(serverLevel.getServer(), doll.ownerProfile);
            if (updated != null) {
                doll.setOwnerProfile(updated);
            }
        }
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DollBlockEntity>("squeeze", 0, state -> PlayState.STOP)
                .triggerableAnim("squeeze", RawAnimation.begin().thenPlay("animation.doll.squeeze")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
