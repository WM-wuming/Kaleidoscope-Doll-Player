package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.init.ModDataComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import kaleidoscopeDollPlayer.registry.ModDataComponents;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class LegacyHeadLayerMixin<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    protected LegacyHeadLayerMixin(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"))
    private void kaleidoscopeDollPlayer$renderHelmetDoll(PoseStack pose, MultiBufferSource buffers, int light, T entity,
            float limbAngle, float limbDistance, float partialTick, float age, float yaw, float pitch, CallbackInfo callback) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (!ModItems.PLAYER_DOLL_KEY.location().getPath().equals(head.get(ModDataComponent.DOLL_ON_HELMET))) return;
        ItemStack doll = new ItemStack(ModItems.PLAYER_DOLL);
        doll.set(DataComponents.PROFILE, head.get(ModDataComponents.HELMET_DOLL_PROFILE));
        pose.pushPose();
        try {
            getParentModel().getHead().translateAndRotate(pose);
            pose.translate(0, -2 / 16.0, 0);
            CustomHeadLayer.translateToHead(pose, false);
            Minecraft.getInstance().getItemRenderer().renderStatic(doll, ItemDisplayContext.HEAD, light,
                    OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        } finally {
            pose.popPose();
        }
    }
}
