package kaleidoscopeDollPlayer.renderer;

import com.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class DollEntitySkinRenderer {
    private static BedrockModel steve;
    private static BedrockModel alex;

    private DollEntitySkinRenderer() {
    }

    public static void render(GameProfile profile, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BedrockModel model;
        if (SkinTextureCache.isSlim(profile)) {
            if (alex == null) alex = load("alex");
            model = alex;
        } else {
            if (steve == null) steve = load("steve");
            model = steve;
        }
        if (model == null) return;
        pose.pushPose();
        try {
            pose.translate(0.5, 1.5, 0.5);
            pose.mulPose(Axis.ZN.rotationDegrees(180));
            pose.mulPose(Axis.YN.rotationDegrees(180));
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutout(SkinTextureCache.get(profile))),
                    light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        } finally {
            pose.popPose();
        }
    }

    private static BedrockModel load(String name) {
        var id = ResourceLocation.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, "geckolib/models/" + name + ".geo.json");
        try (var stream = Minecraft.getInstance().getResourceManager().open(id)) {
            return new BedrockModel(stream);
        } catch (Exception e) {
            KaleidoscopeDollPlayer.LOGGER.error("Failed to load doll entity skin model {}", id, e);
            return null;
        }
    }
}
