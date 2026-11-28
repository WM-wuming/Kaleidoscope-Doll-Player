package kaleidoscopeDollPlayer.gametest;

import com.kaleidoscopedoll.client.gui.ComputerMenuScreen;
import com.kaleidoscopedoll.client.render.LegacyDollItems;
import com.kaleidoscopedoll.render.DollEntityRender;
import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import kaleidoscopeDollPlayer.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.item.ItemStack;

public final class LegacyClientSmoke implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("kaleidoscope.doll.player.legacyClientSmoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Fresh profiles can show the accessibility onboarding screen before the title screen.
            if (client.screen == null || client.getOverlay() != null) return;
            ComputerMenuScreen.class.getDeclaredMethods();
            LegacyDollItems.class.getDeclaredMethods();
            DollEntityRender.class.getDeclaredMethods();
            CustomHeadLayer.class.getDeclaredMethods();
            if (!client.getItemRenderer().getModel(new ItemStack(ModItems.PLAYER_DOLL), null, null, 0).isCustomRenderer()) {
                throw new AssertionError("Player doll item did not load its built-in renderer");
            }
            KaleidoscopeDollPlayer.LOGGER.info("Legacy player doll client smoke passed");
            client.stop();
        });
    }
}
