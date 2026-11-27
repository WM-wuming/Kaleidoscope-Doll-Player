package lsc.kaleidoscopeDollPlayer.registry;

import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;

public final class ModDataComponents {
    public static final DataComponentType<ResolvableProfile> HELMET_DOLL_PROFILE =
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                    Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "helmet_doll_profile"),
                    DataComponentType.<ResolvableProfile>builder()
                            .persistent(ResolvableProfile.CODEC)
                            .networkSynchronized(ResolvableProfile.STREAM_CODEC)
                            .build());

    private ModDataComponents() {}

    public static void register() {}
}
