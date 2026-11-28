package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.client.render.LegacyDollItems;
import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.mojang.blaze3d.vertex.PoseStack;
import kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import kaleidoscopeDollPlayer.registry.ModBlocks;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LegacyDollItems.class, remap = false)
public class LegacyDollItemsMixin {
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private static void kaleidoscopeDollPlayer$renderSkin(ItemStack stack, ItemDisplayContext context, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        if (DollEntityItem.getBlockFromItemStack(stack) != ModBlocks.DOLL_BLOCK) return;
        ItemStack doll = new ItemStack(ModItems.PLAYER_DOLL);
        var tag = stack.get(ModDataComponent.TAG_DOLL_ENTITY);
        var profile = tag == null ? null : PlayerDollEntity.decodeProfile(tag.getString(PlayerDollEntity.OWNER_TAG));
        if (profile != null) doll.set(DataComponents.PROFILE, new ResolvableProfile(profile));
        Minecraft client = Minecraft.getInstance();
        pose.pushPose();
        try {
            pose.translate(0.5, 0.5, 0.5);
            client.getItemRenderer().renderStatic(doll, ItemDisplayContext.GUI, light, overlay, pose, buffers, client.level, 0);
        } finally {
            pose.popPose();
        }
        callback.cancel();
    }
}
