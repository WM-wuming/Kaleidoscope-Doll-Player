package kaleidoscopeDollPlayer.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露容器屏幕的 leftPos/topPos，用于在上游电脑界面上定位生成按钮
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int kaleidoscopeDollPlayer$getLeftPos();

    @Accessor("topPos")
    int kaleidoscopeDollPlayer$getTopPos();
}
