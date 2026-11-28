package kaleidoscopeDollPlayer.menu;

import com.kaleidoscopedoll.datagen.TagItem;
import kaleidoscopeDollPlayer.network.ModMessages;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ComputerDollSlot extends Slot {
    private final Slot original;
    private final SimpleContainer output;

    public ComputerDollSlot(Slot original, SimpleContainer output) {
        super(original.container, original.getContainerSlot(), original.x, original.y);
        this.original = original;
        this.output = output;
    }

    public static SimpleContainer createOutput() {
        return new SimpleContainer(1) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return false;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override public int getMaxStackSize(ItemStack stack) {
                return ModMessages.isPaidDoll(stack) ? stack.getMaxStackSize() : super.getMaxStackSize(stack);
            }
        };
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container != output && stack.is(TagItem.COMPUTER_TOKENS);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return container == output ? output.getMaxStackSize(stack) : super.getMaxStackSize(stack);
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return original.getNoItemIcon();
    }

    @Override
    public void onTake(Player player, ItemStack taken) {
        if (ModMessages.isPaidDoll(container == output ? taken : output.getItem(0))) {
            super.onTake(player, taken);
        } else {
            original.onTake(player, taken);
        }
    }
}
