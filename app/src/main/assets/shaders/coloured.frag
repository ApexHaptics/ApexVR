#version 300 es

in vec3 Colour;
in vec3 Normal;
in vec3 LightDir;
in vec4 FragPos;
in vec4 ShadowCord;

uniform sampler2D depthMap;

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

float texture2DCompare(sampler2D depths, vec2 uv, float compare){
    float depth = texture(depths, uv).r;
    return step(compare, depth);
}

float texture2DShadowLerp(sampler2D depths, vec2 size, vec2 uv, float compare){
    vec2 texelSize = vec2(1.0)/size;
    vec2 f = fract(uv*size+0.5);
    vec2 centroidUV = floor(uv*size+0.5)/size;

    float lb = texture2DCompare(depths, centroidUV+texelSize*vec2(0.0, 0.0), compare);
    float lt = texture2DCompare(depths, centroidUV+texelSize*vec2(0.0, 1.0), compare);
    float rb = texture2DCompare(depths, centroidUV+texelSize*vec2(1.0, 0.0), compare);
    float rt = texture2DCompare(depths, centroidUV+texelSize*vec2(1.0, 1.0), compare);
    float a = mix(lb, lt, f.y);
    float b = mix(rb, rt, f.y);
    float c = mix(a, b, f.x);
    return c;
}

vec2 mod7(vec2 x) {
  return x - floor(x / 7.0f) * 7.0;
}

float PCF(sampler2D depths, vec2 size, vec2 uv, float compare){
    float result = 0.0;
    for(int x=-1; x<=1; x++){
        for(int y=-1; y<=1; y++){
            vec2 off = (vec2(x,y) + (mod7(vec2(x,y)  + uv) - 3.5f)*2.0f)/size;
            result += texture2DShadowLerp(depths, size, uv+off, compare);
        }
    }
    return result/9.0;
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



    sun = sun * PCF(depthMap, vec2(textureSize(depthMap, 0)), ShadowCord.xy,ShadowCord.z - shadowBias);


    float d = sqrt(FragPos.z*FragPos.z+FragPos.x*FragPos.x+FragPos.y*FragPos.y);

    float f = clamp(exp( - d * fogConstant),0.0f,1.0f);

    float c = length(vec2(FragPos));

    vec3 colouring = Colour * (ambient + sun + sky + ground) * smoothstep(1.0f, 0.0f, (c-2.0f)/50.f);;

    diffuseColor = vec4(pow(mix(fogColour,colouring,f), vec3(1.0f/2.2f)),1.0f);

}

