#version 300 es

in vec4 FragPos;
in vec4 GlobalFragPos;

layout(location = 0) out vec4 diffuseColor;

//vec3 skyColour = vec3(0.5f, 0.162f, 0.114f);0.8f, 0.93f, 1.0f
vec3 skyBlue = vec3(0.153f, 0.279f, 0.393f);
//vec3 skyOrange = pow(vec3(255, 132, 90)/255.0, vec3(2.2));
//vec3 skyPurple = pow(vec3(118, 87, 92)/255.0, vec3(2.2));
vec3 fogColourBlue = vec3(0.5,0.6,0.7);

//vec3 red = pow(vec3(187, 117, 93)/255.0, vec3(2.2));
//vec3 gray = pow(1.05*vec3(225, 213, 200)/255.0, vec3(2.2));
//vec3 blue = pow(1.2*vec3(102, 123, 152)/255.0, vec3(2.2));

vec3 sunRingColour = vec3(0.9961f, 0.5977f, 0.3984f);
vec3 sunColour = vec3(0.9961f,0.7969f,0.5977f);

vec3 sunpos = normalize(vec3(0, 7, 24));

float sunIntencity = 3.0f;

void main(){

    float d = length(GlobalFragPos);
    float c = length(vec2(FragPos));
    float zen_amount = dot(normalize(GlobalFragPos.xyz), vec3(0, 1, 0));
    vec3 skyColour = mix(fogColourBlue, skyBlue, max(zen_amount, 0.f));

    float sunDis = distance(sunpos, vec3(GlobalFragPos)/d);
    float sunHaze = smoothstep( 0.05f, 0.4f ,sunDis);
    float sun = smoothstep( 0.01f, 0.05f ,sunDis);

    //float burn = clamp(exp( -GlobalFragPos.y * 0.1f),0.0f,1.0f);

    vec3 colouring = skyColour + mix(sunColour*sunIntencity,mix(sunRingColour,vec3(0),sunHaze),sun);
    vec3 sky = colouring * smoothstep(1.0f, 0.0f, (c-2.0f)/120.0f);

    diffuseColor = vec4(pow(sky, vec3(1.0f/2.2f)),1.0f);
}
