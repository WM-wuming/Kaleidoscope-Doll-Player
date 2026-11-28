package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.entity.DollEntity;
import com.kaleidoscopedoll.render.DollEntityRender;
import com.kaleidoscopedoll.render.LegacyDollRender;
import com.mojang.blaze3d.vertex.PoseStack;
import kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import kaleidoscopeDollPlayer.duck.PlayerDollRenderState;
import kaleidoscopeDollPlayer.registry.ModBlocks;
import kaleidoscopeDollPlayer.renderer.DollEntitySkinRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DollEntityRender.class, remap = false)
public class DollEntityRenderMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kaleidoscopeDollPlayer$captureSkin(DollEntity doll, DollEntityRender.State state, float partialTick, CallbackInfo callback) {
        ((PlayerDollRenderState) (Object) state).kaleidoscopeDollPlayer$setOwnerProfile(
                ((PlayerDollEntity) doll).kaleidoscopeDollPlayer$getOwnerProfile());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/kaleidoscopedoll/render/LegacyDollRender;block(Lnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), remap = true)
    private void kaleidoscopeDollPlayer$renderSkin(BlockState block, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                                 DollEntityRender.State state, PoseStack outerPose, MultiBufferSource outerBuffers, int outerLight) {
        var profile = ((PlayerDollRenderState) (Object) state).kaleidoscopeDollPlayer$getOwnerProfile();
        if (block.getBlock() == ModBlocks.DOLL_BLOCK && profile != null) {
            DollEntitySkinRenderer.render(profile, pose, buffers, light, overlay);
        } else {
            LegacyDollRender.block(block, pose, buffers, light, overlay);
        }
    }
}
