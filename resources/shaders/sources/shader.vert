#version 450

layout (location = 0) in vec2 inPosition;
layout (location = 1) in vec3 inColor;

layout (location = 0) out vec3 fragColor;

// layout (binding = 0) uniform UniformBufferObject {
//     mat4 model;
//     mat4 view;
//     mat4 projection;
// } mvp;

void main(){
//mvp.projection * mvp.view * mvp.view *
    gl_Position = vec4(inPosition, 0.0, 1.0);
    fragColor = inColor;
}