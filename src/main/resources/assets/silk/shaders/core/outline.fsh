#version 150

#moj_import <silk:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Thickness;
uniform float Smoothness;

out vec4 OutColor;

void main() {
    float outerAlpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec2 innerSize = Size - 2.0 * vec2(Thickness);
    vec2 innerCoord = (FragCoord * Size - vec2(Thickness)) / innerSize;

    if (innerCoord.x >= 0.0 && innerCoord.x <= 1.0 &&
        innerCoord.y >= 0.0 && innerCoord.y <= 1.0) {
        vec4 innerRadius = max(Radius - vec4(Thickness), vec4(0.0));
        float innerAlpha = ralpha(innerSize, innerCoord, innerRadius, Smoothness);
        float alpha = outerAlpha - innerAlpha;
        OutColor = vec4(FragColor.rgb, FragColor.a * alpha);
    } else {
        OutColor = vec4(FragColor.rgb, FragColor.a * outerAlpha);
    }

    if (OutColor.a == 0.0) {
        discard;
    }
}
