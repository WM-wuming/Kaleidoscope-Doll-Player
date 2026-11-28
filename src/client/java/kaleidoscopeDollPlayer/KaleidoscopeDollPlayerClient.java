package kaleidoscopeDollPlayer;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.GeoItemRenderer;
import com.google.common.base.Suppliers;
import kaleidoscopeDollPlayer.item.DollItem;
import kaleidoscopeDollPlayer.registry.ModBlockEntities;
import kaleidoscopeDollPlayer.registry.ModItems;
import kaleidoscopeDollPlayer.renderer.DollBlockModel;
import kaleidoscopeDollPlayer.renderer.DollItemRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class KaleidoscopeDollPlayerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 生成入口复用上游 kaleidoscope_doll 的电脑界面（见 ComputerMenuScreenMixin），无需注册自定义 GUI

        // 注册方块实体渲染器 (Geo)
        BlockEntityRendererRegistry.register(ModBlockEntities.DOLL_BLOCK_ENTITY, ctx -> new GeoBlockRenderer<>(ctx, new DollBlockModel()));

        // 注册物品动态渲染器（special model 由 GeckoLib 内部自动注册，物品 JSON 指向 geckolib:geckolib）
        ModItems.PLAYER_DOLL.geoRenderProvider.setValue(new GeoRenderProvider() {
            private final Supplier<GeoItemRenderer<DollItem>> renderer =
                    Suppliers.memoize(DollItemRenderer::new);

            @Override
            public @Nullable GeoItemRenderer<DollItem> getGeoItemRenderer() {
                return this.renderer.get();
            }
        });
    }
}
