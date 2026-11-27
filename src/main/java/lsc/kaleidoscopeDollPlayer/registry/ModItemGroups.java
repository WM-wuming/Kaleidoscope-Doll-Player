package lsc.kaleidoscopeDollPlayer.registry;

import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    // 注册创造模式物品栏
    public static final CreativeModeTab DOLL_GROUP = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "doll_group"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.kaleidoscope_doll_player.group"))
                    .icon(() -> new ItemStack(ModItems.PLAYER_DOLL))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(ModItems.PLAYER_DOLL);
                        entries.accept(ModItems.SKIN_LOCK);
                    }).build());

    public static void registerItemGroups() {
    }
}
