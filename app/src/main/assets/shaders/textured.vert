#version 300 es

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 texCord;

uniform mat4 PVM;
uniform mat4 VM;

out vec2 TexCord;
out vec3 Normal;


void main()
{
    gl_Position =  PVM * vec4(position,1.0f);
    TexCord = texCord;
    Normal = mat3(transpose(inverse(VM))) * normal;
}