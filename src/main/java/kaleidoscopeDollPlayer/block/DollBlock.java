package kaleidoscopeDollPlayer.block;

import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import kaleidoscopeDollPlayer.item.SkinLockItem;
import kaleidoscopeDollPlayer.registry.ModBlockEntities;
import kaleidoscopeDollPlayer.registry.ModItems;
import kaleidoscopeDollPlayer.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DollBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    // 粒子与音效参数
    private static final double PARTICLE_OFFSET = 0.25;
    private static final float NOTE_DIVISOR = 24.0F;

    public DollBlock(Properties properties) {
        super(properties.sound(SoundType.WOOL));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    // 中键拾取：保留皮肤数据
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(ModItems.PLAYER_DOLL);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof DollBlockEntity dollEntity) {
            GameProfile profile = dollEntity.getOwnerProfile();
            if (profile != null) {
                stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            }
        }
        return stack;
    }

    // 右键交互：播放声音和粒子，玩偶收缩一下（不挥动手臂）
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            spawnParticles(serverLevel, pos);
            playSound(serverLevel, pos);
            if (level.getBlockEntity(pos) instanceof DollBlockEntity doll) {
                doll.triggerAnim("squeeze", "squeeze");
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return stack.is(ModItems.SKIN_LOCK) ? SkinLockItem.useOnDoll(level, pos, player, hand)
                : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.DOLL_BLOCK_ENTITY) {
            return null;
        }
        return (world, pos, block, entity) -> DollBlockEntity.tick(world, pos, block, (DollBlockEntity) entity);
    }

    private void spawnParticles(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atBottomCenterOf(pos);
        float noteColor = level.getRandom().nextInt(4) / NOTE_DIVISOR;
        level.sendParticles(ParticleTypes.NOTE,
                center.x + (level.getRandom().nextFloat() - 0.5) * PARTICLE_OFFSET * 2,
                center.y + 1.0 + level.getRandom().nextFloat() * 0.2,
                center.z + (level.getRandom().nextFloat() - 0.5) * PARTICLE_OFFSET * 2,
                0, noteColor, 0, 0, 1);
    }

    private void playSound(ServerLevel level, BlockPos pos) {
        float pitch = 0.75f + level.getRandom().nextFloat() * 0.5f;
        level.playSound(null, pos, ModSounds.DUCK_TOY, SoundSource.BLOCKS, 1.0f, pitch);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // These hooks are available starting in 26.3.
    public void spawnDestroyParticles(Level level, BlockPos pos, BlockState state) {
    }

    public void spawnDestroyByEntityParticles(Level level, net.minecraft.world.entity.Entity entity, BlockPos pos, BlockState state) {
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DollBlockEntity(pos, state);
    }
}
