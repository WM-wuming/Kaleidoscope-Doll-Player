package kaleidoscopeDollPlayer.mixin;

import com.github.ysbbbbbb.kaleidoscopedoll.init.ModDataComponent;
import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import kaleidoscopeDollPlayer.registry.ModDataComponents;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityHelmetDollMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Unique
    private static final Identifier kaleidoscopeDollPlayer$HELMET_MODEL =
            Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "player_doll_on_helmet");

    @Shadow
    @Final
    protected ItemModelResolver itemModelResolver;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void kaleidoscopeDollPlayer$renderPlayerDollOnHelmet(T entity, S state, float partialTicks, CallbackInfo ci) {
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        String dollPath = head.get(ModDataComponent.DOLL_ON_HELMET);
        if (!ModItems.PLAYER_DOLL_KEY.identifier().getPath().equals(dollPath)) {
            return;
        }
        ItemStack doll = new ItemStack(ModItems.PLAYER_DOLL);
        doll.set(DataComponents.PROFILE, head.get(ModDataComponents.HELMET_DOLL_PROFILE));
        if (head.get(DataComponents.EQUIPPABLE) instanceof Equippable equippable && equippable.assetId().isPresent()) {
            // The upstream head layer already lifts the model above armor.
            doll.set(DataComponents.ITEM_MODEL, kaleidoscopeDollPlayer$HELMET_MODEL);
        }
        this.itemModelResolver.updateForLiving(state.headItem, doll, ItemDisplayContext.HEAD, entity);
    }
}
