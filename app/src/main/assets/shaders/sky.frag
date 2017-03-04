#version 300 es

in vec4 FragPos;
in vec4 GlobalFragPos;

layout(location = 0) out vec4 diffuseColor;

//vec3 skyColour = vec3(0.5f, 0.162f, 0.114f);0.8f, 0.93f, 1.0f
vec3 skyColour = vec3(0.8f, 0.93f, 1.0f);
vec3 fogColour = vec3(0.6f,0.6f,0.5f);

vec3 sunRingColour = vec3(0.9961f, 0.5977f, 0.3984f);
vec3 sunColour = vec3(0.9961f,0.7969f,0.5977f);

vec3 sunpos = normalize(vec3(0.0f,1.0f,1.0f));

float fogConstant = 0.01f;
float sunIntencity = 3.0f;

void main(){

    float d = length(GlobalFragPos);
    float c = length(vec2(FragPos));

    float sunDis = distance(sunpos, vec3(GlobalFragPos)/d);
    float sunHaze = smoothstep( 0.05f, 0.4f ,sunDis);
    float sun = smoothstep( 0.01f, 0.05f ,sunDis);

    float f = 1.0f - clamp(exp( - d * fogConstant),0.0f,1.0f);
    float burn = clamp(exp( -GlobalFragPos.y * 0.1f),0.0f,1.0f);

    vec3 colouring = mix(sunColour*sunIntencity,mix(sunRingColour,skyColour,sunHaze),sun);
    vec3 sky = mix(colouring, fogColour, burn) * smoothstep(1.0f, 0.0f, (c-2.0f)/50.f);

    diffuseColor = vec4(pow(sky, vec3(1.0f/2.2f)),1.0f);
}