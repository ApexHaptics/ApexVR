#version 300 es

layout(location = 0) out float depth;

void main() {

    depth = gl_FragCoord.z;
}
