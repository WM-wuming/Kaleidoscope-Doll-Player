package kaleidoscopeDollPlayer.renderer;

import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class DollBlockModel extends LegacyDollModel<DollBlockEntity> {
    @Override
    protected GameProfile getProfile(DollBlockEntity doll, GeoRenderer<DollBlockEntity> renderer) {
        return doll.getOwnerProfile();
    }
}
