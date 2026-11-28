package kaleidoscopeDollPlayer.gametest;

import com.kaleidoscopedoll.inventory.ComputerMenu;
import com.kaleidoscopedoll.init.ModDataComponent;
import com.kaleidoscopedoll.item.DollEntityItem;
import com.kaleidoscopedoll.item.crafting.DollFunctionRecipe;
import kaleidoscopeDollPlayer.block.DollBlock;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import kaleidoscopeDollPlayer.duck.PlayerDollEntity;
import kaleidoscopeDollPlayer.item.DollItem;
import kaleidoscopeDollPlayer.menu.ComputerDollSlot;
import kaleidoscopeDollPlayer.registry.ModBlocks;
import kaleidoscopeDollPlayer.registry.ModItems;
import kaleidoscopeDollPlayer.util.DollSkinProfiles;
import kaleidoscopeDollPlayer.util.SkinImages;
import kaleidoscopeDollPlayer.util.SkinSnapshot;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
    public void placedDollKeepsWaterSource(GameTestHelper test) {
        var relative = new BlockPos(1, 1, 1);
        test.setBlock(relative.below(), Blocks.STONE);
        test.setBlock(relative, Blocks.WATER);
        var level = test.getLevel();
        var pos = test.absolutePos(relative);

        placeDoll(test, pos, new ItemStack(ModItems.PLAYER_DOLL));

        var state = level.getBlockState(pos);
        check(state.is(ModBlocks.DOLL_BLOCK) && state.getValue(DollBlock.WATERLOGGED), "Doll was not waterlogged");
        var fluid = level.getFluidState(pos);
        check(fluid.is(Fluids.WATER) && fluid.isSource(), "Waterlogged doll lost its water source");
        test.succeed();
    }

    @GameTest
    public void dollPlacementRespectsEntityCollision(GameTestHelper test) {
        var relative = new BlockPos(1, 1, 1);
        test.setBlock(relative.below(), Blocks.STONE);
        var pigType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.fromNamespaceAndPath("minecraft", "pig")).orElseThrow();
        var pig = test.spawn(pigType, relative);
        var level = test.getLevel();
        var pos = test.absolutePos(relative);
        var stack = new ItemStack(ModItems.PLAYER_DOLL);

        placeDoll(test, pos, stack);
        check(level.getBlockState(pos).isAir() && stack.getCount() == 1, "Blocked placement changed the block or consumed the doll");
        pig.discard();
        placeDoll(test, pos, stack);
        check(level.getBlockState(pos).is(ModBlocks.DOLL_BLOCK) && stack.isEmpty(), "Unobstructed placement did not consume the doll");
        test.succeed();
    }

    private static void placeDoll(GameTestHelper test, BlockPos pos, ItemStack stack) {
        var player = test.makeMockPlayer(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
        ModItems.PLAYER_DOLL.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
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
        var restored = PlayerDollEntity.decodeProfile(PlayerDollEntity.encodeProfile(profile));
        check(restored != null && DollSkinProfiles.sameSkinProfile(profile, restored), "Reloaded skin was incorrectly treated as changed");
        check(!DollSkinProfiles.sameSkinProfile(profile, SkinSnapshot.create(profile, png, true)), "A changed skin model was ignored");
        doll.setOwnerProfile(profile);
        var drops = Block.getDrops(level.getBlockState(pos), level, pos, doll);
        check(drops.size() == 1 && drops.getFirst().is(ModItems.PLAYER_DOLL), "Block did not drop a player doll");
        check(SkinSnapshot.isLocked(DollItem.getGameProfile(drops.getFirst())), "Block drop lost the skin");

        var totem = DollFunctionRecipe.assembleResult(drops.getFirst(), new ItemStack(Items.TOTEM_OF_UNDYING));
        check(DollFunctionRecipe.isDollTotem(totem) && SkinSnapshot.isLocked(DollItem.getGameProfile(totem)),
                "Totem lost its doll model or skin");

        var recipes = level.getServer().getRecipeManager();
        var lockRecipe = CraftingInput.of(2, 1, List.of(new ItemStack(Items.PAPER), new ItemStack(Items.AMETHYST_SHARD)));
        var lock = recipes.getRecipeFor(RecipeType.CRAFTING, lockRecipe, level).orElseThrow().value().assemble(lockRecipe);
        check(lock.is(ModItems.SKIN_LOCK), "Snapshot recipe did not load");
        var rideRecipe = CraftingInput.of(2, 1, List.of(drops.getFirst(), new ItemStack(Items.SLIME_BALL)));
        var ride = recipes.getRecipeFor(RecipeType.CRAFTING, rideRecipe, level).orElseThrow().value().assemble(rideRecipe);
        check(DollEntityItem.getBlockFromItemStack(ride) == ModBlocks.DOLL_BLOCK, "Rideable recipe lost its doll");
        var riderData = ride.get(ModDataComponent.TAG_DOLL_ENTITY);
        check(riderData != null && StringTag.valueOf(PlayerDollEntity.encodeProfile(profile)).equals(riderData.get(PlayerDollEntity.OWNER_TAG)),
                "Rideable recipe lost its skin");
        test.succeed();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
