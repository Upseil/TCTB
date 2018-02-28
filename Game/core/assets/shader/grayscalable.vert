attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

attribute float a_grayness;

uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_texCoords;

void main()
{
    float gray = dot(a_color.rgb, vec3(0.22, 0.707, 0.071));
    vec3 grayscaled = mix(a_color.rgb, vec3(gray), a_grayness);
    v_color = vec4(grayscaled, a_color.a * (255.0/254.0));
    v_texCoords = a_texCoord0;
    gl_Position =  u_projTrans * a_position;
}