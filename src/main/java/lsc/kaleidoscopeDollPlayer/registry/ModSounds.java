package lsc.kaleidoscopeDollPlayer.registry;

import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final Identifier DUCK_TOY_ID = Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "block.duck_toy");
    public static final SoundEvent DUCK_TOY = SoundEvent.createVariableRangeEvent(DUCK_TOY_ID);

    public static void registerSounds() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, DUCK_TOY_ID, DUCK_TOY);
    }
}