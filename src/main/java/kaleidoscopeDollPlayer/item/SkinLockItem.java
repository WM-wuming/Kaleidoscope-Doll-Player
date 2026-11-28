package kaleidoscopeDollPlayer.item;

import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import kaleidoscopeDollPlayer.registry.ModItems;
import kaleidoscopeDollPlayer.util.SkinSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SkinLockItem extends Item {
    private static final Set<ServerPlayer> PENDING = ConcurrentHashMap.newKeySet();

    public SkinLockItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack lock, Slot slot, ClickAction action, Player player) {
        ItemStack doll = slot.getItem();
        if (action != ClickAction.SECONDARY || !doll.is(ModItems.PLAYER_DOLL)
                || slot.container != player.getInventory() || !slot.allowModification(player)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            var menu = player.containerMenu;
            GameProfile original = DollItem.getGameProfile(doll);
            lockSkin(serverPlayer, lock, original, () -> player.containerMenu == menu && menu.getCarried() == lock
                    && slot.getItem() == doll && !doll.isEmpty() && slot.allowModification(player)
                    && Objects.equals(DollItem.getGameProfile(doll), original), frozen -> {
                ResolvableProfile profile = ResolvableProfile.createResolved(frozen);
                if (doll.getCount() == 1) {
                    doll.set(DataComponents.PROFILE, profile);
                } else {
                    ItemStack single = doll.split(1);
                    single.set(DataComponents.PROFILE, profile);
                    player.getInventory().placeItemBackInInventory(single, Prediction.SERVER_ONLY);
                }
                slot.setChanged();
            });
        }
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS
                : useOnDoll(context.getLevel(), context.getClickedPos(), player, context.getHand());
    }

    public static InteractionResult useOnDoll(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof DollBlockEntity doll)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack lock = player.getItemInHand(hand);
            GameProfile original = doll.getOwnerProfile();
            lockSkin(serverPlayer, lock, original, () -> player.level() == level && player.getItemInHand(hand) == lock
                    && level.hasChunkAt(pos) && level.getBlockEntity(pos) == doll
                    && player.isWithinBlockInteractionRange(pos, 0.0)
                    && level.mayInteract(player, pos) && Objects.equals(doll.getOwnerProfile(), original), doll::setOwnerProfile);
        }
        return InteractionResult.SUCCESS;
    }

    private static void lockSkin(ServerPlayer player, ItemStack lock, GameProfile original,
                                 BooleanSupplier stillValid, Consumer<GameProfile> save) {
        if (player.isSpectator() || !lock.is(ModItems.SKIN_LOCK) || !stillValid.getAsBoolean()) {
            return;
        }
        if (SkinSnapshot.isLocked(original)) {
            message(player, "already_locked");
            return;
        }
        if (SkinSnapshot.skin(original) == null) {
            message(player, "unavailable");
            return;
        }
        if (!PENDING.add(player)) {
            message(player, "pending");
            return;
        }
        message(player, "pending");
        var server = player.level().getServer();
        var work = SkinSnapshot.lock(original);
        work.whenComplete((frozen, error) -> server.execute(() -> {
            PENDING.remove(player);
            if (server.getPlayerList().getPlayer(player.getUUID()) != player || !player.isAlive()) {
                return;
            }
            if (error != null) {
                message(player, "unavailable");
            } else if (player.isSpectator() || !lock.is(ModItems.SKIN_LOCK) || !stillValid.getAsBoolean()) {
                message(player, "cancelled");
            } else {
                save.accept(frozen);
                if (!player.getAbilities().instabuild) {
                    lock.shrink(1);
                }
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                message(player, "success");
            }
        }));
    }

    private static void message(Player player, String status) {
        player.sendOverlayMessage(Component.translatable("message.kaleidoscope_doll_player.skin_lock." + status));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.kaleidoscope_doll_player.skin_lock"));
    }
}
