package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DollFunctionRecipe.class, remap = false)
public class DollFunctionRecipeMixin {

    @Inject(method = "assembleResult", at = @At("RETURN"))
    private static void kaleidoscopeDollPlayer$copyPlayerProfile(ItemStack doll, ItemStack source,
                                                               CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (!result.isEmpty() && doll.is(ModItems.PLAYER_DOLL)) {
            result.set(DataComponents.PROFILE, doll.get(DataComponents.PROFILE));
        }
    }

    @Inject(method = "isDollFunctionItem", at = @At("RETURN"), cancellable = true)
    private static void kaleidoscopeDollPlayer$recognizePlayerDoll(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // The base mod's block map only contains its own doll blocks.
        if (!cir.getReturnValueZ()
                && ModItems.PLAYER_DOLL_KEY.identifier().equals(stack.get(DataComponents.ITEM_MODEL))
                && DollFunctionRecipe.FUNCTION_ITEMS.contains(stack.getItem())) {
            cir.setReturnValue(true);
        }
    }
}
