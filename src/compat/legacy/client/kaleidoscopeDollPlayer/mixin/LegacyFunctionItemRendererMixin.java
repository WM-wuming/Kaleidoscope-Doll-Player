package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public class LegacyFunctionItemRendererMixin {
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private ItemStack kaleidoscopeDollPlayer$renderPlayerModel(ItemStack stack) {
        if (!DollFunctionRecipe.isDollFunctionItem(stack)
                || !ModItems.PLAYER_DOLL_KEY.location().equals(DollFunctionRecipe.getDollModel(stack))) return stack;
        ItemStack doll = new ItemStack(ModItems.PLAYER_DOLL);
        doll.set(DataComponents.PROFILE, stack.get(DataComponents.PROFILE));
        return doll;
    }
}
