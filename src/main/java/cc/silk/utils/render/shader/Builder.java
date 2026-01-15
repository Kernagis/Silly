package cc.silk.utils.render.shader;

import cc.silk.utils.render.shader.builders.OutlineBuilder;
import cc.silk.utils.render.shader.builders.RectangleBuilder;
import lombok.Getter;

public final class Builder {
    @Getter
    private static final RectangleBuilder RECTANGLE_BUILDER = new RectangleBuilder();
    private static final OutlineBuilder OUTLINE_BUILDER = new OutlineBuilder();

    public static RectangleBuilder rectangle() {
        return RECTANGLE_BUILDER;
    }

    public static OutlineBuilder outline() {
        return OUTLINE_BUILDER;
    }
}
