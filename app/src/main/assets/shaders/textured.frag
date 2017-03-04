#version 300 es

in vec2 TexCord;
in vec3 Normal;

uniform sampler2D shaderTexture;

layout(location = 0) out vec4 diffuseColor;

vec3 diffuseColour = vec3(1.0f,0.8f,0.5f);
vec3 ambient = vec3(0.15f,0.15f,0.2f);

vec3 lightDir = normalize(vec3(0.3f,1.0f,-0.4f));
float diffuseStrength = 2.0f;

void main(){

    float df = clamp( dot( normalize(Normal),lightDir ), 0.0f, 1.0f );
    float sf = clamp( dot( vec3(0.0f,0.0f,-1.0f) ,reflect(-lightDir,Normal) ), 0.0f, 1.0f );

    vec3 diffuse = df * diffuseColour * diffuseStrength;

    vec4 colouring = texture(shaderTexture, TexCord) * vec4(ambient + diffuse,1.0f);

    diffuseColor = pow(colouring, vec4(1.0f/2.2f));
}