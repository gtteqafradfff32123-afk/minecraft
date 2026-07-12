#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 OutSize;
uniform float Time;

varying vec2 texCoord;
varying vec2 oneTexel;

float randomNoise(vec2 point) {
    return fract(sin(dot(point, vec2(12.9898, 78.233)))
                 * 43758.5453);
}

void main() {
    vec4 source = texture2D(DiffuseSampler, texCoord);

    float gray = dot(source.rgb, vec3(0.299, 0.587, 0.114));
    vec3 color = mix(source.rgb, vec3(gray), 0.92);

    color *= vec3(0.73, 0.75, 0.78);
    color = (color - 0.5) * 1.08 + 0.5;

    vec2 centered = texCoord - vec2(0.5);
    float edge = dot(centered, centered) * 2.15;
    float vignette = smoothstep(0.20, 0.82, edge);
    color *= 1.0 - vignette * 0.38;

    float grain = randomNoise(
        gl_FragCoord.xy + vec2(Time * 391.7, Time * 173.3));
    color += (grain - 0.5) * 0.10;

    float flicker = sin(Time * 73.0) * 0.012
                  + sin(Time * 29.0) * 0.008;
    color *= 0.76 + flicker;

    gl_FragColor = vec4(clamp(color, 0.0, 1.0), source.a);
}
