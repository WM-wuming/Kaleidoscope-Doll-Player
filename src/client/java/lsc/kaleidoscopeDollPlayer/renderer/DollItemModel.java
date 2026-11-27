package lsc.kaleidoscopeDollPlayer.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.authlib.GameProfile;
import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import lsc.kaleidoscopeDollPlayer.item.DollItem;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class DollItemModel extends GeoModel<DollItem> {
    private static final Identifier STEVE_MODEL = Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "steve");
    private static final Identifier ALEX_MODEL = Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "alex");
    private static final Identifier EMPTY_ANIMATION = Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "doll");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        GameProfile profile = renderState.getGeckolibData(SkinTextureCache.PROFILE_TICKET);
        return SkinTextureCache.isSlim(profile) ? ALEX_MODEL : STEVE_MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return SkinTextureCache.get(renderState.getGeckolibData(SkinTextureCache.PROFILE_TICKET));
    }

    @Override
    public Identifier getAnimationResource(DollItem animatable) {
        return EMPTY_ANIMATION;
    }

    @Override
    public void addAdditionalStateData(DollItem animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
        if (relatedObject instanceof GeoItemRenderer.RenderData renderData) {
            GameProfile profile = DollItem.getGameProfile(renderData.itemStack());
            if (profile != null) {
                renderState.addGeckolibData(SkinTextureCache.PROFILE_TICKET, profile);
            }
        }
    }
}