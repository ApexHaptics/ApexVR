#version 300 es

in vec3 Colour;
in vec3 Normal;
in vec3 LightDir;
in vec4 FragPos;
in vec4 ShadowCord;

uniform sampler2D depthMap;

layout(location = 0) out vec4 diffuseColor;

vec3 sunColour = vec3(1.0f, 0.919f, 0.364f);
vec3 skyColour = vec3(0.28f, 0.162f, 0.114f);
vec3 groundColour = vec3(0.4f, 0.28f, 0.2f);
vec3 ambientColour = vec3(0.016f, 0.007f, 0.007f);

//vec4 fogColour = vec4(0.779f, 0.883f, 0.346f, 1.0f);

vec3 fogColour = vec3(0.6f,0.6f,0.5f);

float sunStrength = 2.24f;
float skyStrength = 1.0f;
float groundStrength = 0.2f;
float ambientStrength = 1.0f;
float fogConstant = 0.01f;

float LinearizeDepth(vec2 uv)
{
  float n = -20.0f; // camera z near
  float f = 20.0f; // camera z far
  float z = texture(depthMap, uv).w;
  return (2.0f * n) / (f + n - z * (f - n));
}

void main(){
    vec3 groundDir = normalize(vec3(-LightDir.x,0.0f,-LightDir.z));

    float dfsun = clamp( dot( normalize(Normal),LightDir ), 0.0f, 1.0f );
    float dfsky = clamp( dot( normalize(Normal),vec3(0.0f,-1.0f,0.0f) ), 0.0f, 1.0f );
    float dfground = clamp( dot( normalize(Normal),groundDir), 0.0f, 1.0f );

    vec3 sun = dfsun * sunColour * sunStrength;
    vec3 sky = dfsky * skyColour * skyStrength;
    vec3 ground = dfground * groundColour * groundStrength;
    vec3 ambient = ambientColour * ambientStrength;

    if(texture( depthMap, ShadowCord.xy ).z < ShadowCord.z){
        sun = sun * 0.3;
    }

    float d = sqrt(FragPos.z*FragPos.z+FragPos.x*FragPos.x+FragPos.y*FragPos.y);

    float f = clamp(exp( - d * fogConstant),0.0f,1.0f);

    float c = length(vec2(FragPos));

    vec3 colouring = Colour * (ambient + sun + sky + ground) * smoothstep(1.0f, 0.0f, (c-2.0f)/50.f);;

    diffuseColor = vec4(pow(mix(fogColour,colouring,f), vec3(1.0f/2.2f)),1.0f);

    //diffuseColor = vec4(vec3(ShadowCord.z)+0.0f*colouring,1.0f);

    //diffuseColor = vec4(vec3(ShadowCord.xy,0.0f)+0.0f*colouring,1.0f);


    //diffuseColor = vec4(vec3(LinearizeDepth(ShadowCord.xy)),1.0f) + 0.0f * vec4(colouring,1.0f);
/*
    if(texture( depthMap, ShadowCord.xy ).z < ShadowCord.z){
            diffuseColor = vec4(1.0f,0.0f,0.0f,1.0f);
    }
    */
}

