package kaleidoscopeDollPlayer.gametest;

import com.geckolib.renderer.internal.GeckolibItemSpecialRenderer;
import com.kaleidoscopedoll.client.gui.ComputerMenuScreen;
import com.kaleidoscopedoll.render.DollEntityItemModel;
import com.kaleidoscopedoll.render.DollEntityRender;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

public class PlayerDollClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.waitForScreen(TitleScreen.class);
        context.runOnClient(client -> {
            ComputerMenuScreen.class.getDeclaredMethods();
            DollEntityItemModel.class.getDeclaredMethods();
            DollEntityRender.class.getDeclaredMethods();
            GeckolibItemSpecialRenderer.class.getDeclaredMethods();
            LivingEntityRenderer.class.getDeclaredMethods();
        });
    }
}
