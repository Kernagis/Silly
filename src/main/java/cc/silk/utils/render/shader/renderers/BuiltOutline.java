package cc.silk.utils.render.shader.renderers;

import cc.silk.utils.render.shader.IRenderer;
import cc.silk.utils.render.shader.ResourceProvider;
import cc.silk.utils.render.shader.states.QuadColorState;
import cc.silk.utils.render.shader.states.QuadRadiusState;
import cc.silk.utils.render.shader.states.SizeState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

public record BuiltOutline(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float thickness,
        float smoothness
) implements IRenderer {

    private static final ShaderProgramKey OUTLINE_SHADER_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("outline"),
            VertexFormats.POSITION_COLOR,
            Defines.EMPTY
    );

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        float width = this.size.width(), height = this.size.height();
        ShaderProgram shader = RenderSystem.setShader(OUTLINE_SHADER_KEY);
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(),
                this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Thickness").set(this.thickness);
        shader.getUniform("Smoothness").set(this.smoothness);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
