package kaleidoscopeDollPlayer.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import kaleidoscopeDollPlayer.item.DollItem;
import net.minecraft.resources.Identifier;

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

}
