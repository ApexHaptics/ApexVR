 #version 300 es

layout (location = 0) in vec3 position;

uniform mat4 PR;
uniform float skyDist;

out vec4 FragPos;
out vec4 GlobalFragPos;


void main()
{
    gl_Position =  vec4(position,1.0f);
    FragPos = vec4(position,1.0f);
    GlobalFragPos = PR * vec4(position.x,position.y,1,1.0f);
}