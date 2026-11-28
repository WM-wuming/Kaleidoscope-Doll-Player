package kaleidoscopeDollPlayer.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.inventory.ComputerMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import kaleidoscopeDollPlayer.menu.ComputerDollSlot;
import kaleidoscopeDollPlayer.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

@Mixin(value = ComputerMenu.class, remap = false)
public abstract class ComputerMenuMixin extends AbstractContainerMenu {
    @Shadow @Final private SimpleContainer input;
    @Shadow @Final @Mutable private SimpleContainer output;

    protected ComputerMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
            target = "Lcom/github/ysbbbbbb/kaleidoscopedoll/inventory/ComputerMenu;output:Lnet/minecraft/world/SimpleContainer;"))
    private void kaleidoscopeDollPlayer$allowPaidBatches(int id, net.minecraft.world.entity.player.Inventory inventory,
                                                          CallbackInfo ci) {
        output = ComputerDollSlot.createOutput();
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/github/ysbbbbbb/kaleidoscopedoll/inventory/ComputerMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;"))
    private Slot kaleidoscopeDollPlayer$wrapComputerSlots(ComputerMenu menu, Slot slot, Operation<Slot> original) {
        if (slot.container == input || slot.container == output) {
            slot = new ComputerDollSlot(slot, output);
        }
        return original.call(menu, slot);
    }

    @ModifyArg(method = "quickMoveStack", at = @At(value = "INVOKE",
            target = "Lcom/github/ysbbbbbb/kaleidoscopedoll/inventory/ComputerMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z",
            ordinal = 1), index = 2)
    private int kaleidoscopeDollPlayer$shiftIntoInputOnly(int endIndex) {
        return 1;
    }

    // Older builds charge directly here; newer ones charge through the output slot's onTake.
    @WrapWithCondition(method = "quickMoveStack", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private boolean kaleidoscopeDollPlayer$chargeOnlyForPreview(ItemStack token, int count,
                                                                  @Local(ordinal = 0) ItemStack result) {
        return !ModMessages.isPaidDoll(result);
    }

    @Inject(method = "quickMoveStack", at = @At("RETURN"))
    private void kaleidoscopeDollPlayer$clearUnpaidPreview(Player player, int index,
                                                             CallbackInfoReturnable<ItemStack> cir) {
        if (index == 0 && !cir.getReturnValue().isEmpty() && input.getItem(0).isEmpty()
                && !ModMessages.isPaidDoll(output.getItem(0))) {
            output.setItem(0, ItemStack.EMPTY);
        }
    }

    @Inject(method = "clickDollButton", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDollPlayer$keepPaidResult(CallbackInfoReturnable<Boolean> cir) {
        if (ModMessages.isPaidDoll(output.getItem(0))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void kaleidoscopeDollPlayer$returnPaidResult(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer && ModMessages.isPaidDoll(output.getItem(0))) {
            clearContainer(player, output);
        }
    }
}
