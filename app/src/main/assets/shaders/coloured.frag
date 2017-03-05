#version 300 es

in vec3 Colour;
in vec3 Normal;
in vec3 LightDir;
in vec4 FragPos;
in vec4 ShadowCord;

uniform sampler2DShadow depthMap;

layout(location = 0) out vec4 diffuseColor;

vec3 sunColour = vec3(1.0f, 0.919f, 0.364f);
//vec3 skyColour = vec3(0.28f, 0.162f, 0.114f);0.779f, 0.883f, 0.346f, 1.0f
vec3 skyColour = vec3(0.8f, 0.93f, 1.0f);
vec3 groundColour = vec3(0.4f, 0.28f, 0.2f);
vec3 ambientColour = vec3(0.016f, 0.007f, 0.007f);

//vec4 fogColour = vec4(0.779f, 0.883f, 0.346f, 1.0f);

vec3 fogColour = vec3(0.6f,0.6f,0.5f);

float sunStrength = 2.24f;
float skyStrength = 1.0f;
float groundStrength = 0.2f;
float ambientStrength = 1.0f;
float fogConstant = 0.01f;

vec2 mod7(vec2 x) {
  return x - floor(x / 7.0f) * 7.0;
}

float offset_lookup(sampler2DShadow depths, vec4 loc, vec2 offset, vec2 texmapscale) {
    return textureProj(depths, vec4(loc.xy + offset*texmapscale*loc.w, loc.zw));
}

float PCF(sampler2DShadow depths, vec4 ShadowCord, vec2 texmapscale){
    return (offset_lookup(depths, ShadowCord, vec2(-1.5,0.5), texmapscale) +
            offset_lookup(depths, ShadowCord, vec2(0.5,0.5), texmapscale) +
            offset_lookup(depths, ShadowCord, vec2(-1.5,-1.5), texmapscale) +
            offset_lookup(depths, ShadowCord, vec2(0.5,-1.5), texmapscale)) * 0.25;
}

void main(){
    vec3 groundDir = normalize(vec3(-LightDir.x,0.0f,-LightDir.z));

    float nDotL = dot( normalize(Normal),LightDir);
    float dfsun = clamp( nDotL, 0.0f, 1.0f );
    float dfsky = clamp( dot( normalize(Normal),vec3(0.0f,-1.0f,0.0f) ), 0.0f, 1.0f );
    float dfground = clamp( dot( normalize(Normal),groundDir), 0.0f, 1.0f );

    vec3 sun = dfsun * sunColour * sunStrength;
    vec3 sky = dfsky * skyColour * skyStrength;
    vec3 ground = dfground * groundColour * groundStrength;
    vec3 ambient = ambientColour * ambientStrength;


    float shadowBias = max(0.05f * (1.0f - nDotL), 0.005f);

    vec2 texmapscale = 1.0f/vec2(textureSize(depthMap, 0));
    sun = sun * PCF(depthMap, ShadowCord - vec4(0,0,shadowBias,0), texmapscale);
    //sun = sun * texture(depthMap, vec3(ShadowCord.xy, ShadowCord.z - shadowBias));


    float d = sqrt(FragPos.z*FragPos.z+FragPos.x*FragPos.x+FragPos.y*FragPos.y);

    float f = clamp(exp( - d * fogConstant),0.0f,1.0f);

    float c = length(vec2(FragPos));

    vec3 colouring = Colour * (ambient + sun + sky + ground) * smoothstep(1.0f, 0.0f, (c-2.0f)/50.f);;

    diffuseColor = vec4(pow(mix(fogColour,colouring,f), vec3(1.0f/2.2f)),1.0f);

}

