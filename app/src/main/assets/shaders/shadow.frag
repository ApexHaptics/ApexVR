#version 300 es

layout(location = 0) out vec2 depth;

void main() {

    float dx = dFdx(gl_FragCoord.z);
    float dy = dFdy(gl_FragCoord.z);
    depth = vec2(gl_FragCoord.z, pow(gl_FragCoord.z, 2.0) + 0.25*(dx*dx + dy*dy));
}
