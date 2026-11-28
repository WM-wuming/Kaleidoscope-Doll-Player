package kaleidoscopeDollPlayer.renderer;

import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

abstract class LegacyDollModel<T extends GeoAnimatable> extends GeoModel<T> {
    private static final ResourceLocation STEVE = id("geo/steve.geo.json");
    private static final ResourceLocation ALEX = id("geo/alex.geo.json");
    private static final ResourceLocation ANIMATION = id("animations/doll.animation.json");

    protected abstract GameProfile getProfile(T doll, GeoRenderer<T> renderer);

    @Override
    public ResourceLocation getModelResource(T doll, GeoRenderer<T> renderer) {
        return SkinTextureCache.isSlim(getProfile(doll, renderer)) ? ALEX : STEVE;
    }

    @Override
    public ResourceLocation getTextureResource(T doll, GeoRenderer<T> renderer) {
        return SkinTextureCache.get(getProfile(doll, renderer));
    }

    @Override
    public ResourceLocation getAnimationResource(T doll) {
        return ANIMATION;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, path);
    }
}
