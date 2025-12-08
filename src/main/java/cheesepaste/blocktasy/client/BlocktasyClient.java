package cheesepaste.blocktasy.client;


import cheesepaste.blocktasy.Blocktasy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class BlocktasyClient implements ClientModInitializer {
    public static ShaderProgram GRAVITY_WARP;
    public static ShaderProgram trailGlowShader;
    @Override
    public void onInitializeClient() {

        /*CoreShaderRegistrationCallback.EVENT.register(context -> {

            context.register(
                    Identifier.of(Blocktasy.MOD_ID, "trail_glow"), // JSON 文件名 (不带后缀)
                    VertexFormats.POSITION_TEXTURE_COLOR,        // 顶点格式，必须与 JSON 里的 attributes 对应
                    program -> trailGlowShader = program         // 回调：加载成功后赋值给静态变量
            );
        });
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(
                    Identifier.of(Blocktasy.MOD_ID, "gravitational_distortion"),
                    VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, // 实体的标准顶点格式
                    program -> GRAVITY_WARP = program
            );
        });


        EntityRendererRegistry.register(ModEntities.FLYING_BLOCK_ENTITY,
                BaseBlockEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FollowingEntity,
                BaseBlockEntityRenderer::new);*/
    }

    public static ShaderProgram getTrailGlowShader() {
        return trailGlowShader;
    }
}

