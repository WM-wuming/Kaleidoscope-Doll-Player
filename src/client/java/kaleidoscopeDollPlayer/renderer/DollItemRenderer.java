package kaleidoscopeDollPlayer.renderer;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.item.DollItem;

public final class DollItemRenderer extends GeoItemRenderer<DollItem> {
    public DollItemRenderer() {
        super(new DollItemModel());
    }

    @Override
    public void addRenderData(DollItem item, RenderData data, GeoRenderState state, float partialTick) {
        super.addRenderData(item, data, state, partialTick);
        GameProfile profile = DollItem.getGameProfile(data.itemStack());
        if (profile != null) {
            state.addGeckolibData(SkinTextureCache.PROFILE_TICKET, profile);
        }
    }
}
