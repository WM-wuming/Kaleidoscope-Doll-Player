package kaleidoscopeDollPlayer.renderer;

import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.item.DollItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class DollItemRenderer extends GeoItemRenderer<DollItem> {
    public DollItemRenderer() {
        super(new LegacyDollModel<>() {
            @Override
            protected GameProfile getProfile(DollItem doll, GeoRenderer<DollItem> renderer) {
                return renderer instanceof DollItemRenderer itemRenderer
                        ? DollItem.getGameProfile(itemRenderer.getCurrentItemStack()) : null;
            }
        });
    }
}
