#version 300 es

layout (location = 0) in vec3 position;

uniform mat4 PVM;
uniform mat4 VM;
uniform mat4 M;

out vec4 FragPos;
out vec4 GlobalFragPos;


void main()
{
    gl_Position =  PVM * vec4(position,1.0f);
    FragPos = VM * vec4(position,1.0f);
    GlobalFragPos = M * vec4(position,1.0f);
}