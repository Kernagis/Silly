package cc.silk.utils.render.shader;

import cc.silk.utils.render.shader.renderers.BuiltOutline;
import cc.silk.utils.render.shader.renderers.BuiltRectangle;
import cc.silk.utils.render.shader.states.QuadColorState;
import cc.silk.utils.render.shader.states.QuadRadiusState;
import cc.silk.utils.render.shader.states.SizeState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.*;

public class ShaderRenderer {
    
    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BuiltRectangle rect = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .smoothness(1.0f)
                .build();
        rect.render(matrix, x, y, 0);
    }

    public static void drawRectWithCustomRadius(MatrixStack matrices, float x, float y, float width, float height, 
                                                 float[] cornerRadii, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BuiltRectangle rect = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(cornerRadii[0], cornerRadii[1], cornerRadii[2], cornerRadii[3]))
                .color(new QuadColorState(color))
                .smoothness(1.0f)
                .build();
        rect.render(matrix, x, y, 0);
    }

    public static void drawOutline(MatrixStack matrices, float x, float y, float width, float height, 
                                   float radius, Color color, float thickness) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BuiltOutline outline = Builder.outline()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(color))
                .thickness(thickness)
                .smoothness(1.0f)
                .build();
        outline.render(matrix, x, y, 0);
    }

    public static void drawOutlineWithCustomRadius(MatrixStack matrices, float x, float y, float width, float height, 
                                                   float[] cornerRadii, Color color, float thickness) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BuiltOutline outline = Builder.outline()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(cornerRadii[0], cornerRadii[1], cornerRadii[2], cornerRadii[3]))
                .color(new QuadColorState(color))
                .thickness(thickness)
                .smoothness(1.0f)
                .build();
        outline.render(matrix, x, y, 0);
    }

    public static void drawRectGradient(MatrixStack matrices, float x, float y, float width, float height, 
                                       float radius, Color colorTop, Color colorBottom) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BuiltRectangle rect = Builder.rectangle()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(colorTop, colorBottom, colorBottom, colorTop))
                .smoothness(1.0f)
                .build();
        rect.render(matrix, x, y, 0);
    }
}
