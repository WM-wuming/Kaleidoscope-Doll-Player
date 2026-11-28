package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.entity.DollEntity;
import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.init.ModItems;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DollEntityItem.class, remap = false)
public abstract class DollEntityItemMixin {
    @Inject(method = "getBlockFromItemStack", at = @At("HEAD"), cancellable = true)
    private static void kaleidoscopeDollPlayer$readDroppedDollBlock(ItemStack stack, CallbackInfoReturnable<Block> cir) {
        if (!stack.is(ModItems.DOLL_ENTITY_ITEM)) {
            return;
        }
        CompoundTag entityTag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        // BlockState.CODEC writes a bare block ID for default states saved from entities.
        if (entityTag != null && entityTag.get(DollEntity.TAG_BLOCK_STATE) instanceof StringTag) {
            entityTag.read(DollEntity.TAG_BLOCK_STATE, BlockState.CODEC)
                    .filter(state -> !state.isAir())
                    .map(BlockState::getBlock)
                    .ifPresent(cir::setReturnValue);
        }
    }

    @WrapOperation(method = "appendHoverText", remap = true,
            at = @At(value = "INVOKE", ordinal = 0,
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent kaleidoscopeDollPlayer$showOwnerName(String key, Operation<MutableComponent> original,
            @Local(argsOnly = true) ItemStack stack) {
        if (!"tooltip.kaleidoscope_doll.doll.player".equals(key)) {
            return original.call(key);
        }

        CompoundTag entityTag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        GameProfile profile = entityTag == null ? null : entityTag.getString(PlayerDollEntity.OWNER_TAG)
                .map(PlayerDollEntity::decodeProfile).orElse(null);
        if (profile != null && profile.name() != null && !profile.name().isBlank()) {
            return Component.literal(profile.name());
        }
        return Component.translatable("item.kaleidoscope_doll_player.player_doll");
    }
}
