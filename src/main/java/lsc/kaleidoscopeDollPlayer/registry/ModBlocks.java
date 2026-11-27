package lsc.kaleidoscopeDollPlayer.registry;

import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import lsc.kaleidoscopeDollPlayer.block.DollBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {
    public static final ResourceKey<Block> DOLL_KEY = key("player_doll");

    // 注册玩偶方块：羊毛材质，非透明（生成入口使用上游 kaleidoscope_doll 的电脑）
    public static final Block DOLL_BLOCK = Registry.register(BuiltInRegistries.BLOCK, DOLL_KEY,
            new DollBlock(BlockBehaviour.Properties.of()
                    .setId(DOLL_KEY)
                    .mapColor(MapColor.WOOL)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .sound(SoundType.WOOL)
                    .strength(0f, 10f)
                    .noOcclusion()));

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, name));
    }

    public static void registerModBlocks() {
    }
}
