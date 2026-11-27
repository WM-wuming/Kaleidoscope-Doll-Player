package lsc.kaleidoscopeDollPlayer.network;

import com.github.ysbbbbbb.kaleidoscopedoll.inventory.ComputerMenu;
import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import lsc.kaleidoscopeDollPlayer.item.DollItem;
import lsc.kaleidoscopeDollPlayer.mixin.ComputerMenuAccessor;
import lsc.kaleidoscopeDollPlayer.registry.ModItems;
import lsc.kaleidoscopeDollPlayer.util.DollSkinProfiles;
import lsc.kaleidoscopeDollPlayer.util.SkinImages;
import lsc.kaleidoscopeDollPlayer.util.SkinSnapshot;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ModMessages {
    public static final Identifier CRAFT_DOLL_ID = Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "craft_doll");
    private static final Set<ProfileRequest> PENDING_PROFILES = ConcurrentHashMap.newKeySet();

    private record ProfileRequest(UUID playerId, String name) {}

    public record CraftDollPayload(String targetName, boolean craftAll) implements CustomPacketPayload {
        public static final Type<CraftDollPayload> TYPE = new Type<>(CRAFT_DOLL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, CraftDollPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, CraftDollPayload::targetName,
                ByteBufCodecs.BOOL, CraftDollPayload::craftAll,
                CraftDollPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CraftSkinPayload(int menuId, byte[] png, boolean craftAll) implements CustomPacketPayload {
        public static final Type<CraftSkinPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "craft_skin"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CraftSkinPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CraftSkinPayload::menuId,
                ByteBufCodecs.byteArray(SkinImages.MAX_SNAPSHOT_BYTES), CraftSkinPayload::png,
                ByteBufCodecs.BOOL, CraftSkinPayload::craftAll,
                CraftSkinPayload::new);

        @Override
        public Type<CraftSkinPayload> type() {
            return TYPE;
        }
    }

    public static void registerC2SPackets() {
        PayloadTypeRegistry.serverboundPlay().register(CraftDollPayload.TYPE, CraftDollPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CraftDollPayload.TYPE, (payload, context) -> {
            MinecraftServer server = context.server();
            ServerPlayer player = context.player();
            server.execute(() -> handleCraftDoll(server, player, payload.targetName(), payload.craftAll()));
        });
        PayloadTypeRegistry.serverboundPlay().register(CraftSkinPayload.TYPE, CraftSkinPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CraftSkinPayload.TYPE,
                (payload, context) -> handleCraftSkin(context.player(), payload));
    }

    private static void handleCraftSkin(ServerPlayer player, CraftSkinPayload payload) {
        if (!(player.containerMenu instanceof ComputerMenu menu) || menu.containerId != payload.menuId()
                || player.isSpectator() || !menu.stillValid(player)) {
            return;
        }
        ComputerMenuAccessor accessor = (ComputerMenuAccessor) menu;
        if (!accessor.kaleidoscopeDollPlayer$getInput().getItem(0).is(ItemTags.WOOL)) {
            player.sendOverlayMessage(Component.translatable("gui.kaleidoscope_doll_player.computer.wool_required"));
            return;
        }
        try {
            byte[] png = SkinImages.normalize(payload.png());
            craft(player, menu, ResolvableProfile.createResolved(SkinSnapshot.create(null, png, false)), payload.craftAll());
        } catch (IOException e) {
            player.sendOverlayMessage(Component.translatable("gui.kaleidoscope_doll_player.computer.skin_failed"));
        }
    }

    private static void handleCraftDoll(MinecraftServer server, ServerPlayer player, String rawName, boolean craftAll) {
        if (!(player.containerMenu instanceof ComputerMenu menu) || player.isSpectator()) {
            return;
        }
        String targetName = rawName == null ? "" : rawName.trim();
        if (!DollSkinProfiles.isPlayerName(targetName)) {
            return;
        }

        ComputerMenuAccessor accessor = (ComputerMenuAccessor) menu;
        if (!accessor.kaleidoscopeDollPlayer$getInput().getItem(0).is(ItemTags.WOOL)) {
            return;
        }

        var matching = matchingDolls(player, targetName).toList();
        ResolvableProfile profile = matching.stream()
                .map(stack -> stack.get(DataComponents.PROFILE))
                .filter(DollSkinProfiles::hasTextures)
                .findFirst()
                .orElseGet(() -> matching.isEmpty() ? ResolvableProfile.createUnresolved(targetName)
                        : matching.getFirst().get(DataComponents.PROFILE));
        boolean resolved = DollSkinProfiles.hasTextures(profile);
        if (resolved) {
            matching.forEach(stack -> applyProfile(stack, profile));
        }
        craft(player, menu, profile, craftAll);
        if (!resolved) {
            requestSkin(server, player, targetName);
        }
    }

    private static void craft(ServerPlayer player, ComputerMenu menu, ResolvableProfile profile, boolean craftAll) {
        ComputerMenuAccessor accessor = (ComputerMenuAccessor) menu;
        SimpleContainer input = accessor.kaleidoscopeDollPlayer$getInput();
        SimpleContainer output = accessor.kaleidoscopeDollPlayer$getOutput();
        ItemStack inputStack = input.getItem(0);
        ItemStack doll = new ItemStack(ModItems.PLAYER_DOLL);
        doll.set(DataComponents.PROFILE, profile);
        int count = craftAll ? inputStack.getCount() : 1;
        inputStack.shrink(count);
        input.setChanged();
        ItemStack currentOutput = output.getItem(0);
        // An upstream preview cannot be redeemed after its last token is spent.
        if (craftAll) {
            putBatchInOutput(player, output, doll, count);
        } else if (currentOutput.isEmpty() || (inputStack.isEmpty() && !isPaidDoll(currentOutput))) {
            output.setItem(0, doll);
        } else {
            giveDolls(player, doll);
        }

        player.resetLastActionTime();
        menu.broadcastChanges();
    }

    private static void putBatchInOutput(ServerPlayer player, SimpleContainer output, ItemStack doll, int count) {
        ItemStack previous = output.getItem(0);
        int limit = output.getMaxStackSize(doll);
        int placed;
        if (ItemStack.isSameItemSameComponents(previous, doll)) {
            placed = Math.min(count, Math.max(0, limit - previous.getCount()));
            previous.grow(placed);
            output.setChanged();
        } else {
            placed = Math.min(count, limit);
            output.setItem(0, doll.copyWithCount(placed));
            if (isPaidDoll(previous)) {
                giveDolls(player, previous);
            }
        }
        for (int remaining = count - placed; remaining > 0; remaining -= limit) {
            giveDolls(player, doll.copyWithCount(Math.min(remaining, limit)));
        }
    }

    private static void giveDolls(ServerPlayer player, ItemStack dolls) {
        player.getInventory().add(dolls);
        if (!dolls.isEmpty()) {
            player.drop(dolls, false, Prediction.PREDICTED);
        }
    }

    public static boolean isPaidDoll(ItemStack stack) {
        return stack.is(ModItems.PLAYER_DOLL) && stack.has(DataComponents.PROFILE);
    }

    private static void requestSkin(MinecraftServer server, ServerPlayer player, String targetName) {
        ProfileRequest request = new ProfileRequest(player.getUUID(), targetName.toLowerCase(Locale.ROOT));
        if (!PENDING_PROFILES.add(request)) {
            return;
        }
        DollSkinProfiles.resolve(server, targetName).whenComplete((profile, error) -> server.execute(() -> {
            PENDING_PROFILES.remove(request);
            ServerPlayer currentPlayer = server.getPlayerList().getPlayer(request.playerId());
            if (profile != null && currentPlayer != null) {
                applyResolvedProfile(currentPlayer, targetName, profile);
            }
        }));
    }

    private static void applyResolvedProfile(ServerPlayer player, String targetName, ResolvableProfile profile) {
        matchingDolls(player, targetName).forEach(stack -> applyProfile(stack, profile));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static void applyProfile(ItemStack stack, ResolvableProfile profile) {
        if (!DollSkinProfiles.hasTextures(stack.get(DataComponents.PROFILE))) {
            stack.set(DataComponents.PROFILE, profile);
        }
    }

    private static Stream<ItemStack> matchingDolls(ServerPlayer player, String targetName) {
        ItemStack output = player.containerMenu instanceof ComputerMenuAccessor accessor
                ? accessor.kaleidoscopeDollPlayer$getOutput().getItem(0) : ItemStack.EMPTY;
        var inventory = player.getInventory();
        return Stream.concat(Stream.of(output, player.containerMenu.getCarried()),
                        IntStream.range(0, inventory.getContainerSize()).mapToObj(inventory::getItem))
                .filter(ModMessages::isPaidDoll)
                .filter(stack -> {
                    var profile = DollItem.getGameProfile(stack);
                    return profile != null && targetName.equalsIgnoreCase(profile.name()) && !SkinSnapshot.isLocked(profile);
                });
    }
}
