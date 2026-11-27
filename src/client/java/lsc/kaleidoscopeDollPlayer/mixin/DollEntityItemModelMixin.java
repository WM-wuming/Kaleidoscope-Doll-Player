package lsc.kaleidoscopeDollPlayer.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.init.ModDataComponent;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import com.github.ysbbbbbb.kaleidoscopedoll.render.DollEntityItemModel;
import com.google.common.cache.CacheBuilder;
import lsc.kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import lsc.kaleidoscopeDollPlayer.registry.ModBlocks;
import lsc.kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

@Mixin(value = DollEntityItemModel.class, remap = false)
public abstract class DollEntityItemModelMixin {
    @Unique
    private static final ConcurrentMap<String, Optional<ResolvableProfile>> kaleidoscopeDollPlayer$displayProfiles =
            CacheBuilder.newBuilder().maximumSize(1024).<String, Optional<ResolvableProfile>>build().asMap();

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void kaleidoscopeDollPlayer$renderPlayerDoll(ItemStackRenderState state, ItemStack stack,
            ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level,
            @Nullable ItemOwner owner, int seed, CallbackInfo ci) {
        CompoundTag entityTag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        if (entityTag == null || DollEntityItem.getBlockFromItemStack(stack) != ModBlocks.DOLL_BLOCK) {
            return;
        }

        // Render a display-only stack; the real item retains its entity data and behavior.
        ItemStack displayStack = new ItemStack(ModItems.PLAYER_DOLL);
        entityTag.getString(PlayerDollEntity.OWNER_TAG)
                .flatMap(json -> kaleidoscopeDollPlayer$displayProfiles.computeIfAbsent(json,
                        key -> Optional.ofNullable(PlayerDollEntity.decodeProfile(key))
                                .map(ResolvableProfile::createResolved)))
                .ifPresent(profile -> displayStack.set(DataComponents.PROFILE, profile));
        resolver.appendItemLayers(state, displayStack, context, level, owner, seed);
        ci.cancel();
    }
}
