#version 450

layout (location = 0)in  fragColor;
layout (location = 0)out outColor;

void main() {
    gl_FragColor = vec4(fragColor, 1.0);
}
