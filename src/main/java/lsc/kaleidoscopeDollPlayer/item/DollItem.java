package lsc.kaleidoscopeDollPlayer.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.authlib.GameProfile;
import lsc.kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import lsc.kaleidoscopeDollPlayer.registry.ModBlocks;
import lsc.kaleidoscopeDollPlayer.registry.ModSounds;
import lsc.kaleidoscopeDollPlayer.util.DollSkinProfiles;
import lsc.kaleidoscopeDollPlayer.util.SkinSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.function.Consumer;

/**
 * 玩家皮肤玩偶物品。继承上游 com.github.ysbbbbbb.kaleidoscopedoll.item.DollItem：
 * 上游的功能合成（图腾/雪球/鸡蛋/末影珍珠/风弹）、实体玩偶合成（粘液球）、
 * 铁砧头盔+玩偶、抛射物粒子修正等系统均按 instanceof DollItem 判断，继承后自动生效。
 */
public class DollItem extends com.github.ysbbbbbb.kaleidoscopedoll.item.DollItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public final MutableObject<Object> geoRenderProvider = new MutableObject<>();

    public DollItem(Block block, Properties properties) {
        super(block, "player", properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.kaleidoscope_doll_player.player_doll");
    }

    // 放置方块逻辑：传递皮肤数据；仅潜行（Shift）时允许放置
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext placementContext = new BlockPlaceContext(context);
        Level level = context.getLevel();
        BlockPos pos = placementContext.getClickedPos();

        if (placementContext.canPlace() && level.getBlockState(pos).canBeReplaced(placementContext)) {
            BlockState state = ModBlocks.DOLL_BLOCK.getStateForPlacement(placementContext);

            if (state != null && level.setBlock(pos, state, Block.UPDATE_ALL)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DollBlockEntity dollBe) {
                    GameProfile profile = getGameProfile(context.getItemInHand());
                    if (profile != null) {
                        dollBe.setOwnerProfile(profile);
                    }
                }

                SoundType soundType = state.getSoundType();
                level.playSound(player, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

                if (!player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // 右键空气：普通玩偶不挥手臂、收缩一下并播放鸭子叫
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            this.triggerAnim(player, GeoItem.getId(stack), "squeeze", "squeeze");
        } else {
            level.playSound(null, player.blockPosition(), ModSounds.DUCK_TOY,
                    SoundSource.PLAYERS, 1.0F, 0.75F + level.getRandom().nextFloat() * 0.5F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        GameProfile profile = getGameProfile(stack);
        if (profile != null && profile.name() != null && !profile.name().isBlank()) {
            tooltip.accept(Component.translatable("tooltip.kaleidoscope_doll_player.player_doll.owner", profile.name())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (SkinSnapshot.isLocked(profile)) {
            tooltip.accept(Component.translatable("tooltip.kaleidoscope_doll_player.skin_locked").withStyle(ChatFormatting.GRAY));
        }
    }

    public static GameProfile getGameProfile(ItemStack stack) {
        ResolvableProfile resolvable = stack.get(DataComponents.PROFILE);
        return resolvable == null ? null : resolvable.partialProfile();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if ((level.getGameTime() + entity.getId()) % DollSkinProfiles.CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        GameProfile profile = DollSkinProfiles.updatedProfile(level.getServer(), getGameProfile(stack));
        if (profile != null) {
            stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            if (entity instanceof Player player) {
                player.getInventory().setChanged();
            }
        }
    }

    /**
     * 剥掉 textures 属性里的时间戳再存储：同一玩家同一皮肤每次抓取的时间戳都不同，
     * 会让数据组件不相等、玩偶无法堆叠。URL 才是皮肤的身份标识。
     * 签名随原文失效，一并丢弃（皮肤渲染不校验签名）。
     */
    public static GameProfile normalizeProfile(GameProfile profile) {
        com.mojang.authlib.properties.Property textures = profile.properties().get("textures").stream().findFirst().orElse(null);
        if (textures == null) {
            return profile;
        }
        try {
            String json = new String(java.util.Base64.getDecoder().decode(textures.value()), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("timestamp")) {
                return profile;
            }
            root.remove("timestamp");
            String normalized = java.util.Base64.getEncoder().encodeToString(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            com.google.common.collect.Multimap<String, com.mojang.authlib.properties.Property> mutable = com.google.common.collect.ArrayListMultimap.create();
            mutable.putAll(profile.properties());
            mutable.removeAll("textures");
            mutable.put("textures", new com.mojang.authlib.properties.Property("textures", normalized));
            return new GameProfile(profile.id(), profile.name(), new com.mojang.authlib.properties.PropertyMap(mutable));
        } catch (Exception e) {
            return profile;
        }
    }

    // --- GeckoLib 接口 ---

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void createGeoRenderer(Consumer consumer) {
        consumer.accept(this.geoRenderProvider.getValue());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<DollItem>("squeeze", 0, state -> PlayState.STOP)
                .triggerableAnim("squeeze", RawAnimation.begin().thenPlay("animation.doll.squeeze")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
