package kaleidoscopeDollPlayer.mixin;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 调用 Screen 的受保护方法 addRenderableWidget，用于向上游电脑界面添加生成按钮
 */
@Mixin(Screen.class)
public interface ScreenInvoker {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener> T kaleidoscopeDollPlayer$addRenderableWidget(T widget);
}
