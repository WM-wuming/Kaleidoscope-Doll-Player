package kaleidoscopeDollPlayer.renderer;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class DollBlockModel extends GeoModel<DollBlockEntity> {
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
    public Identifier getAnimationResource(DollBlockEntity animatable) {
        return EMPTY_ANIMATION;
    }

    @Override
    public void addAdditionalStateData(DollBlockEntity animatable, @Nullable Object relatedObject, GeoRenderState renderState) {
        GameProfile profile = animatable.getOwnerProfile();
        if (profile != null) {
            renderState.addGeckolibData(SkinTextureCache.PROFILE_TICKET, profile);
        }
    }
}