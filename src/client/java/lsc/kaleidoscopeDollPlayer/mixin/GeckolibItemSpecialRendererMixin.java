package lsc.kaleidoscopeDollPlayer.mixin;

import com.geckolib.renderer.internal.GeckolibItemSpecialRenderer;
import com.github.ysbbbbbb.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import lsc.kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GeckolibItemSpecialRenderer.class, remap = false)
public class GeckolibItemSpecialRendererMixin {
    @WrapOperation(
            method = "extractArgument(Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
                    + "Lnet/minecraft/world/item/ItemDisplayContext;"
                    + "Lnet/minecraft/client/multiplayer/ClientLevel;"
                    + "Lnet/minecraft/world/entity/ItemOwner;)"
                    + "Lcom/geckolib/renderer/internal/GeckolibItemSpecialRenderer$RenderData;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;")
    )
    private Item kaleidoscopeDollPlayer$selectDollRenderer(ItemStack stack, Operation<Item> original) {
        Item item = original.call(stack);
        if (ModItems.PLAYER_DOLL_KEY.identifier().equals(stack.get(DataComponents.ITEM_MODEL))
                && DollFunctionRecipe.isDollFunctionItem(stack)) {
            // GeckoLib still reads the skin profile from the original stack.
            return ModItems.PLAYER_DOLL;
        }
        return item;
    }
}
