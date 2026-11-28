package kaleidoscopeDollPlayer.renderer;

import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.item.DollItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class DollItemRenderer extends GeoItemRenderer<DollItem> {
    public DollItemRenderer() {
        this(new ItemModel());
    }

    private DollItemRenderer(ItemModel model) {
        super(model);
        model.renderer = this;
    }

    private static final class ItemModel extends LegacyDollModel<DollItem> {
        private DollItemRenderer renderer;

        @Override
        protected GameProfile getProfile(DollItem doll) {
            var stack = renderer.getCurrentItemStack();
            return stack == null ? null : DollItem.getGameProfile(stack);
        }
    }
}
