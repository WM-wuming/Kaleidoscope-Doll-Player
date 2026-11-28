package kaleidoscopeDollPlayer.renderer;

import com.mojang.authlib.GameProfile;
import kaleidoscopeDollPlayer.block.entity.DollBlockEntity;

public final class DollBlockModel extends LegacyDollModel<DollBlockEntity> {
    @Override
    protected GameProfile getProfile(DollBlockEntity doll) {
        return doll.getOwnerProfile();
    }
}
