package kaleidoscopeDollPlayer.gametest;

import com.kaleidoscopedoll.inventory.ComputerMenu;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import kaleidoscopeDollPlayer.menu.ComputerDollSlot;
import kaleidoscopeDollPlayer.registry.ModBlocks;
import kaleidoscopeDollPlayer.registry.ModItems;
import kaleidoscopeDollPlayer.util.SkinImages;
import kaleidoscopeDollPlayer.util.SkinSnapshot;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.ItemTags;

import java.io.IOException;
import java.util.List;

public final class PlayerDollTests {
    @GameTest
    public void computerKeepsPaidDolls(GameTestHelper test) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        var menu = new ComputerMenu(1, player.getInventory());
        var input = menu.getSlot(0);
        var output = menu.getSlot(1);
        check(input instanceof ComputerDollSlot && output instanceof ComputerDollSlot, "Computer slots were not patched");
        var paid = new ItemStack(ModItems.PLAYER_DOLL, 6);
        paid.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved("Player"));
        input.set(new ItemStack(BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.WOOL).iterator().next().value(), 3));
        output.set(paid);
        check(output.getItem().getCount() == 6, "Paid batch was truncated");
        check(menu.quickMoveStack(player, 1).getCount() == 6, "Paid batch was not moved");
        check(input.getItem().getCount() == 3, "Paid batch charged wool a second time");
        test.succeed();
    }

    @GameTest
    public void skinAndRecipesSurviveWorldLoad(GameTestHelper test) throws IOException {
        var relative = new BlockPos(1, 1, 1);
        test.setBlock(relative, ModBlocks.DOLL_BLOCK);
        var level = test.getLevel();
        var pos = test.absolutePos(relative);
        var doll = (DollBlockEntity) level.getBlockEntity(pos);
        byte[] png;
        try (var stream = PlayerDollTests.class.getResourceAsStream("/assets/kaleidoscope_doll_player/textures/doll_default_steve.png")) {
            png = SkinImages.normalize(java.util.Objects.requireNonNull(stream).readAllBytes());
        }
        var profile = SkinSnapshot.create(null, png, false);
        doll.setOwnerProfile(profile);
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, doll);
        check(drops.size() == 1 && drops.getFirst().is(ModItems.PLAYER_DOLL), "Block did not drop a player doll");
        var droppedProfile = drops.getFirst().get(DataComponents.PROFILE);
        check(droppedProfile != null && SkinSnapshot.isLocked(droppedProfile.partialProfile()), "Block drop lost the skin");

        var totem = DollFunctionRecipe.assembleResult(drops.getFirst(), new ItemStack(Items.TOTEM_OF_UNDYING));
        check(DollFunctionRecipe.isDollTotem(totem) && totem.has(DataComponents.PROFILE), "Totem lost its doll model or skin");

        var recipes = level.getServer().getRecipeManager();
        var lockRecipe = CraftingInput.of(2, 1, List.of(new ItemStack(Items.PAPER), new ItemStack(Items.AMETHYST_SHARD)));
        var lock = recipes.getRecipeFor(RecipeType.CRAFTING, lockRecipe, level).orElseThrow().value().assemble(lockRecipe);
        check(lock.is(ModItems.SKIN_LOCK), "Snapshot recipe did not load");
        var rideRecipe = CraftingInput.of(2, 1, List.of(drops.getFirst(), new ItemStack(Items.SLIME_BALL)));
        var ride = recipes.getRecipeFor(RecipeType.CRAFTING, rideRecipe, level).orElseThrow().value().assemble(rideRecipe);
        check(DollEntityItem.getBlockFromItemStack(ride) == ModBlocks.DOLL_BLOCK, "Rideable recipe lost its doll");
        test.succeed();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
