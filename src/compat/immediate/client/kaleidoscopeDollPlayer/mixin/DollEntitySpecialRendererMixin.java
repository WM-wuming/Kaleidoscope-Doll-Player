package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.kaleidoscopedoll.render.DollEntitySpecialRenderer;
import kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import kaleidoscopeDollPlayer.registry.ModBlocks;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DollEntitySpecialRenderer.class, remap = false)
public class DollEntitySpecialRendererMixin {
    @Inject(method = "extractArgument(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true, remap = true)
    private void kaleidoscopeDollPlayer$extractSkin(ItemStack stack, CallbackInfoReturnable<ItemStack> callback) {
        if (DollEntityItem.getBlockFromItemStack(stack) != ModBlocks.DOLL_BLOCK) return;
        ItemStack display = new ItemStack(ModItems.PLAYER_DOLL);
        var tag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        if (tag != null) {
            tag.getString(PlayerDollEntity.OWNER_TAG).map(PlayerDollEntity::decodeProfile)
                    .map(ResolvableProfile::new).ifPresent(profile -> display.set(DataComponents.PROFILE, profile));
        }
        callback.setReturnValue(display);
    }
}
