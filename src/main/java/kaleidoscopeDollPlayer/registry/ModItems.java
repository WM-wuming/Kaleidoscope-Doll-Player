package kaleidoscopeDollPlayer.registry;

import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import kaleidoscopeDollPlayer.item.DollItem;
import kaleidoscopeDollPlayer.item.SkinLockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final ResourceKey<Item> PLAYER_DOLL_KEY =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "player_doll"));
    public static final ResourceKey<Item> SKIN_LOCK_KEY =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "skin_lock"));

    // 注册玩家玩偶物品（BlockItem，指向玩偶方块以支持上游的实体玩偶合成），最大堆叠64，可装备在头部
    public static final DollItem PLAYER_DOLL = registerItem(PLAYER_DOLL_KEY,
            new DollItem(ModBlocks.DOLL_BLOCK, new Item.Properties()
                    .setId(PLAYER_DOLL_KEY)
                    .stacksTo(64)
                    .equippableUnswappable(EquipmentSlot.HEAD)));
    public static final SkinLockItem SKIN_LOCK = registerItem(SKIN_LOCK_KEY,
            new SkinLockItem(new Item.Properties().setId(SKIN_LOCK_KEY)));

    private static <T extends Item> T registerItem(ResourceKey<Item> key, T item) {
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void registerModItems() {
    }
}
