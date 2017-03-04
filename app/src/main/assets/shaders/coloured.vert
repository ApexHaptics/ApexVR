#version 300 es

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec3 colour;

uniform mat4 SPVM;
uniform mat4 PVM;
uniform mat4 VM;
uniform mat4 V;


out vec3 Colour;
out vec3 Normal;
out vec3 LightDir;
out vec4 FragPos;
out vec4 ShadowCord;

vec3 sun = normalize(vec3(0.0f,1.0f,1.0f));


void main()
{
    gl_Position =  PVM * vec4(position,1.0f);
    Colour = colour;
    Normal = mat3(transpose(inverse(VM))) * normal;
    LightDir = mat3(transpose(inverse(V))) * sun;
    FragPos = VM * vec4(position,1.0f);
    ShadowCord = SPVM * vec4(position,1.0f);
}