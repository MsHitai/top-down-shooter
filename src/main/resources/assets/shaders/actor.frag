#ifdef GL_ES
precision mediump float;
#endif
uniform sampler2D u_texture;
uniform vec2 u_step;
uniform vec2 u_origin;
uniform vec4 u_bounds;
uniform float u_flash;
varying vec4 v_color;
varying vec2 v_uv;
void main() {
    vec2 uv = u_origin + (floor((v_uv - u_origin) / u_step) + 0.5) * u_step;
    vec4 body = texture2D(u_texture, uv);
    float left = texture2D(u_texture, uv - vec2(u_step.x, 0.0)).a;
    float right = texture2D(u_texture, uv + vec2(u_step.x, 0.0)).a;
    float down = texture2D(u_texture, uv - vec2(0.0, u_step.y)).a;
    float up = texture2D(u_texture, uv + vec2(0.0, u_step.y)).a;
    float nearby = max(max(left, right), max(down, up));
    // Only opaque material gets ink. Glows, fading ghosts and warnings keep their transparency.
    float ink = smoothstep(0.80, 0.98, nearby) * (1.0 - body.a);
    vec3 base = body.rgb / max(body.a, 0.001);
    vec2 local = clamp((uv - u_bounds.xy) / u_bounds.zw, 0.0, 1.0);
    // Three connected shade planes, like the authored kin; no smooth plastic gradient.
    float plane = local.y - local.x * 0.25;
    float light = plane > 0.63 ? 1.06 : plane < 0.22 ? 0.80 : 0.95;
    float material = smoothstep(0.12, 0.30, dot(base, vec3(0.299, 0.587, 0.114)));
    // White eyes, highlights, emissive red signals and damage flashes remain crisp.
    float white = smoothstep(0.82, 0.97, min(min(base.r, base.g), base.b));
    float signal = step(base.g * 1.8 + 0.15, base.r);
    float shade = material * (1.0 - white) * (1.0 - signal) * (1.0 - u_flash);
    shade *= smoothstep(0.96, 1.0, body.a);
    vec3 surface = mix(base, base * light * vec3(1.02, 1.0, 0.99), shade);
    // A narrow upper-left rim gives rounded shapes a connected highlight instead of noise.
    float rim = (1.0 - up) * left * 0.07 * shade;
    surface += vec3(rim);
    gl_FragColor = vec4(surface * body.a + vec3(0.105, 0.085, 0.125) * ink,
                        body.a + ink) * v_color;
}
