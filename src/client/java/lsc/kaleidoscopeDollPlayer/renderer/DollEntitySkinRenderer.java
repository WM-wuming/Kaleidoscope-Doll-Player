package lsc.kaleidoscopeDollPlayer.renderer;

import com.github.ysbbbbbb.kaleidoscopedoll.client.bedrock.BedrockModel;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lsc.kaleidoscopeDollPlayer.KaleidoscopeDollPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;

/**
 * 实体玩偶的玩家皮肤渲染：复用上游 Bedrock 模型系统加载本 mod 的 steve/alex 几何模型，
 * 贴图来自 SkinTextureCache（皮肤下载完成后自动切换）。
 */
public final class DollEntitySkinRenderer {
    private static BedrockModel steveModel;
    private static BedrockModel alexModel;

    private DollEntitySkinRenderer() {
    }

    public static void render(GameProfile profile, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
        BedrockModel model = SkinTextureCache.isSlim(profile) ? getAlexModel() : getSteveModel();
        if (model == null) {
            return;
        }
        Identifier texture = SkinTextureCache.get(profile);

        poseStack.pushPose();
        // 与上游 renderCustom 相同的模型坐标变换
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.rotate(Axis.ZN.rotationDegrees(180));
        poseStack.rotate(Axis.YN.rotationDegrees(180));

        final BedrockModel finalModel = model;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), (pose, buffer) -> {
            PoseStack modelPose = new PoseStack();
            modelPose.last().set(pose);
            finalModel.renderToBuffer(modelPose, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        });
        poseStack.popPose();
    }

    private static BedrockModel getSteveModel() {
        if (steveModel == null) {
            steveModel = loadModel("geckolib/models/steve.geo.json");
        }
        return steveModel;
    }

    private static BedrockModel getAlexModel() {
        if (alexModel == null) {
            alexModel = loadModel("geckolib/models/alex.geo.json");
        }
        return alexModel;
    }

    private static BedrockModel loadModel(String path) {
        try {
            ResourceManager manager = Minecraft.getInstance().getResourceManager();
            return manager.getResource(Identifier.fromNamespaceAndPath(KaleidoscopeDollPlayer.MOD_ID, path))
                    .map(resource -> {
                        try (InputStream stream = resource.open()) {
                            return new BedrockModel(stream);
                        } catch (Exception e) {
                            KaleidoscopeDollPlayer.LOGGER.error("Failed to load doll entity skin model {}", path, e);
                            return null;
                        }
                    })
                    .orElse(null);
        } catch (Exception e) {
            KaleidoscopeDollPlayer.LOGGER.error("Failed to load doll entity skin model {}", path, e);
            return null;
        }
    }
}
