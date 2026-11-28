package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.mixin.ItemCombinerMenuAccessor;
import kaleidoscopeDollPlayer.registry.ModDataComponents;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;

    // Run before the upstream cancellable HEAD injector (default order 1000).
    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true, order = 900)
    private void kaleidoscopeDollPlayer$combinePlayerDollWithHelmet(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemCombinerMenuAccessor slots = (ItemCombinerMenuAccessor) menu;
        ItemStack head = slots.kaleidoscopeDoll$getInputSlots().getItem(0);
        ItemStack doll = slots.kaleidoscopeDoll$getInputSlots().getItem(1);
        Equippable equippable = head.get(DataComponents.EQUIPPABLE);
        if (!doll.is(ModItems.PLAYER_DOLL) || head.getCount() != 1
                || equippable == null || equippable.slot() != EquipmentSlot.HEAD) {
            return;
        }

        ItemStack result = head.copy();
        result.set(ModDataComponent.DOLL_ON_HELMET, ModItems.PLAYER_DOLL_KEY.identifier().getPath());
        result.set(ModDataComponents.HELMET_DOLL_PROFILE, doll.get(DataComponents.PROFILE));
        slots.kaleidoscopeDoll$getResultSlots().setItem(0, result);
        this.repairItemCountCost = 1;
        this.cost.set(1);
        menu.broadcastChanges();
        ci.cancel();
    }
}
