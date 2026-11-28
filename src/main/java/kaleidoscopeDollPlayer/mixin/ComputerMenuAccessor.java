package kaleidoscopeDollPlayer.mixin;

import com.kaleidoscopedoll.inventory.ComputerMenu;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露上游电脑菜单的私有输入/输出槽，供生成玩家玩偶的数据包处理使用
 */
@Mixin(value = ComputerMenu.class, remap = false)
public interface ComputerMenuAccessor {
    @Accessor("input")
    SimpleContainer kaleidoscopeDollPlayer$getInput();

    @Accessor("output")
    SimpleContainer kaleidoscopeDollPlayer$getOutput();
}
