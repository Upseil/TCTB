#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP 
#endif

uniform float u_grayness;

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main()
{
    vec4 color = v_color * texture2D(u_texture, v_texCoords);
    float gray = dot(color.rgb, vec3(0.22, 0.707, 0.071));
    vec3 grayscaled = mix(color.rgb, vec3(gray), u_grayness);
    gl_FragColor = vec4(grayscaled, color.a);
}