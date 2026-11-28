package kaleidoscopeDollPlayer.registry;

import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static BlockEntityType<DollBlockEntity> DOLL_BLOCK_ENTITY;

    public static void registerBlockEntities() {
        DOLL_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "doll_block_entity"),
                FabricBlockEntityTypeBuilder.create(DollBlockEntity::new, ModBlocks.DOLL_BLOCK).build()
        );
    }
}
