package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.client.gui.ComputerMenuScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import kaleidoscopeDollPlayer.network.ModMessages;
import kaleidoscopeDollPlayer.util.DollSkinProfiles;
import kaleidoscopeDollPlayer.util.SkinImages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URI;

@Mixin(value = ComputerMenuScreen.class, remap = false)
public abstract class ComputerMenuScreenMixin {
    @Shadow(remap = false)
    private EditBox searchField;

    @Unique
    private boolean kaleidoscopeDollPlayer$downloading;

    @WrapOperation(method = "initSearch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/EditBox;setValue(Ljava/lang/String;)V"))
    private void kaleidoscopeDollPlayer$restoreFullInput(EditBox field, String input, Operation<Void> original) {
        field.setMaxLength(SkinImages.MAX_URL_LENGTH);
        original.call(field, input);
    }

    @ModifyArg(method = "initSearch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V"))
    private int kaleidoscopeDollPlayer$allowSkinUrls(int previousLimit) {
        return SkinImages.MAX_URL_LENGTH;
    }

    @Inject(method = "initDollButtons", at = @At("TAIL"), remap = false)
    private void kaleidoscopeDollPlayer$addGenerateButton(CallbackInfo ci) {
        ComputerMenuScreen self = (ComputerMenuScreen) (Object) this;
        AbstractContainerScreenAccessor container = (AbstractContainerScreenAccessor) self;
        int x = container.kaleidoscopeDollPlayer$getLeftPos() + 129;
        int y = container.kaleidoscopeDollPlayer$getTopPos() + 98;

        Button button = Button.builder(Component.literal("✔"), b -> this.kaleidoscopeDollPlayer$sendCraft())
                .bounds(x, y, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.kaleidoscope_doll_player.computer.generate")))
                .build();
        ((ScreenInvoker) self).kaleidoscopeDollPlayer$addRenderableWidget(button);
    }

    @Unique
    private void kaleidoscopeDollPlayer$sendCraft() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || this.kaleidoscopeDollPlayer$downloading) {
            return;
        }
        String input = this.searchField.getValue().trim();
        if (input.isEmpty()) {
            input = minecraft.getUser().getName();
        }
        this.searchField.setFocused(false);
        boolean craftAll = minecraft.hasShiftDown();
        if (DollSkinProfiles.isPlayerName(input)) {
            ClientPlayNetworking.send(new ModMessages.CraftDollPayload(input, craftAll));
            return;
        }

        URI url;
        try {
            url = SkinImages.parseUserUrl(input);
        } catch (IllegalArgumentException e) {
            minecraft.player.sendOverlayMessage(Component.translatable("gui.kaleidoscope_doll_player.computer.invalid_input"));
            return;
        }
        ComputerMenuScreen screen = (ComputerMenuScreen) (Object) this;
        if (!screen.getMenu().getSlot(0).getItem().is(ItemTags.WOOL)) {
            minecraft.player.sendOverlayMessage(Component.translatable("gui.kaleidoscope_doll_player.computer.wool_required"));
            return;
        }
        var connection = minecraft.getConnection();
        int menuId = screen.getMenu().containerId;
        this.kaleidoscopeDollPlayer$downloading = true;
        minecraft.player.sendOverlayMessage(Component.translatable("gui.kaleidoscope_doll_player.computer.downloading"));
        SkinImages.download(url).whenComplete((png, error) -> minecraft.execute(() -> {
            this.kaleidoscopeDollPlayer$downloading = false;
            if (minecraft.getConnection() != connection || minecraft.player == null
                    || minecraft.player.containerMenu != screen.getMenu()) {
                return;
            }
            if (error != null) {
                minecraft.player.sendOverlayMessage(Component.translatable("gui.kaleidoscope_doll_player.computer.skin_failed"));
                return;
            }
            ClientPlayNetworking.send(new ModMessages.CraftSkinPayload(menuId, png, craftAll));
            minecraft.player.sendOverlayMessage(Component.empty());
        }));
    }
}
