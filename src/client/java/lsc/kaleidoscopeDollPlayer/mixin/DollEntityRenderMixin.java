package lsc.kaleidoscopeDollPlayer.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.render.DollEntityRender;
import com.github.ysbbbbbb.kaleidoscopedoll.render.DollEntityRenderState;
import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import lsc.kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import lsc.kaleidoscopeDollPlayer.duck.PlayerDollRenderState;
import lsc.kaleidoscopeDollPlayer.renderer.DollEntitySkinRenderer;
import lsc.kaleidoscopeDollPlayer.registry.ModBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 实体玩偶皮肤渲染：
 * extractRenderState 时把实体的主人档案写入渲染状态；
 * submit 时若方块是本 mod 玩偶方块且带主人档案，则用玩家皮肤模型替代方块烘焙模型。
 * 上游原版玩偶（无主人档案）走原逻辑不受影响。
 */
@Mixin(value = DollEntityRender.class, remap = false)
public class DollEntityRenderMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = true)
    private void kaleidoscopeDollPlayer$extractOwner(DollEntity entity, DollEntityRenderState state, float partialTick, CallbackInfo ci) {
        ((PlayerDollRenderState) state).kaleidoscopeDollPlayer$setOwnerProfile(
                ((PlayerDollEntity) entity).kaleidoscopeDollPlayer$getOwnerProfile());
    }

    @Redirect(method = "submit",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitMovingBlock(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/MovingBlockRenderState;I)V"),
            remap = true)
    private void kaleidoscopeDollPlayer$renderSkinBody(SubmitNodeCollector instance, PoseStack poseStack,
                                                         MovingBlockRenderState movingBlockRenderState, int outlineColor,
                                                         DollEntityRenderState state, PoseStack outerPoseStack,
                                                         SubmitNodeCollector collector, CameraRenderState camera) {
        GameProfile profile = ((PlayerDollRenderState) state).kaleidoscopeDollPlayer$getOwnerProfile();
        if (profile == null || movingBlockRenderState.blockState.getBlock() != ModBlocks.DOLL_BLOCK) {
            // 非本 mod 玩偶：走上游原逻辑
            instance.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor);
            return;
        }
        DollEntitySkinRenderer.render(profile, poseStack, collector, state.lightCoords);
    }
}
