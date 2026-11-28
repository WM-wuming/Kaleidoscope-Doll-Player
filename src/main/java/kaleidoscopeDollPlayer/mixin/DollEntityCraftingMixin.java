package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.kaleidoscopedoll.item.crafting.DollEntityCraftingRecipe;
import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import kaleidoscopeDollPlayer.item.DollItem;
import kaleidoscopeDollPlayer.registry.ModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 玩家皮肤玩偶 + 粘液球 → 实体玩偶：上游合成只存方块状态（皮肤玩偶实体身体不可见），
 * 这里改写为本 mod 玩偶方块 + 附带主人档案，实体渲染时用玩家皮肤模型。
 * 上游原版玩偶走原逻辑不受影响。
 */
@Mixin(value = DollEntityCraftingRecipe.class, remap = false)
public class DollEntityCraftingMixin {

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true, remap = true)
    private void kaleidoscopeDollPlayer$craftPlayerDollEntity(CraftingInput input, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack doll = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof DollItem) {
                doll = stack;
                break;
            }
        }
        if (doll == null) {
            return;
        }

        ItemStack result = DollEntityItem.createItemWithBlockState(ModBlocks.DOLL_BLOCK.defaultBlockState());
        CompoundTag entityTag = result.getOrDefault(ModDataComponent.TAG_DOLL_ENTITY, new CompoundTag());
        GameProfile profile = DollItem.getGameProfile(doll);
        if (profile != null) {
            entityTag.putString(PlayerDollEntity.OWNER_TAG, PlayerDollEntity.encodeProfile(profile));
        }
        result.set(ModDataComponent.TAG_DOLL_ENTITY, entityTag);
        cir.setReturnValue(result);
    }
}
