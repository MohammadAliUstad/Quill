//
//  BeamShader.kt
//  BorderBeam
//
//  Author: ismnoiet
//  Minimum supported: Android 13 (API 33) for the shader; see BeamFallback.kt for older versions.
//
//  One GPU pass draws the whole beam (crisp edge + soft glow) for a rounded-rect / capsule / circle.
//  A direct port of the iOS Metal shader, written in AGSL. All lengths are in dp, so the beam looks
//  the same on every screen density.
//
package com.ismnoiet.borderbeam

import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope

internal const val BEAM_AGSL = """
uniform float2 size;          // view size in dp
uniform float density;        // pixels per dp
uniform float cornerRadius;   // dp, clamped to half the shortest side
uniform float phase;          // 0...1, loops
uniform float direction;      // +1 clockwise (left to right on top), -1 counter-clockwise
uniform float mode;           // 0 = orbit the border, 1 = sweep along one edge
uniform float side;           // mode 1: 1 top, 2 bottom, 3 left, 4 right
uniform float arc;            // mode 0: length of the comet (fraction of the lap)
uniform float glow;           // soft inner glow reach in dp
uniform float intensity;      // overall brightness 0...1
uniform float count;          // number of colors in the palette (1...8)
uniform float4 colors[8];     // palette, tail to head

const float PI = 3.14159265;

// Signed distance to a rounded box centred at the origin (negative inside).
float sdRoundedBox(float2 p, float2 hs, float r) {
    float2 q = abs(p) - (hs - r);
    return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

// Position along the border, measured clockwise from the right-edge midpoint, as 0...1.
// Using distance ALONG the edge (not the angle around the centre) keeps the beam's ends
// perpendicular to the border, so the glow never fans out toward the centre.
float perimeterParam(float2 p, float2 hs, float r) {
    float2 ih = max(hs - r, float2(0.0));
    float q = PI * r * 0.5;                          // one corner arc
    float L = 4.0 * ih.x + 4.0 * ih.y + 4.0 * q;
    float baseBL = ih.y + q + 2.0 * ih.x;            // bottom-left corner start
    float baseL = baseBL + q;                        // left edge start
    float baseTL = baseL + 2.0 * ih.y;               // top-left corner start
    float baseTR = baseTL + q + 2.0 * ih.x;          // top-right corner start
    float2 ap = abs(p);
    float s = 0.0;
    if (ap.x > ih.x && ap.y > ih.y) {
        // corner: angle measured from the horizontal axis through the arc's centre
        float2 v = ap - ih;
        float ang = atan(v.y, v.x);                  // 0 ... pi/2
        if (p.x >= 0.0 && p.y >= 0.0) {
            s = ih.y + r * ang;                      // bottom-right
        } else if (p.x < 0.0 && p.y >= 0.0) {
            s = baseBL + r * (PI * 0.5 - ang);       // bottom-left
        } else if (p.x < 0.0) {
            s = baseTL + r * ang;                    // top-left
        } else {
            s = baseTR + r * (PI * 0.5 - ang);       // top-right
        }
    } else if ((hs.x - ap.x) < (hs.y - ap.y)) {
        // left / right edge
        if (p.x >= 0.0) {
            s = p.y >= 0.0 ? p.y : L + p.y;
        } else {
            s = baseL + (ih.y - p.y);
        }
    } else {
        // top / bottom edge
        if (p.y >= 0.0) {
            s = ih.y + q + (ih.x - p.x);
        } else {
            s = baseTL + q + (p.x + ih.x);
        }
    }
    return fract(s / max(L, 0.001));
}

// Linear blend across the palette. t in 0...1. Written with constant array indices only.
float4 samplePalette(float t) {
    int n = int(count);
    float4 result = colors[0];
    if (n <= 1) {
        return result;
    }
    float x = clamp(t, 0.0, 1.0) * float(n - 1);
    for (int i = 0; i < 7; i++) {
        if (i + 1 < n) {
            result = mix(result, colors[i + 1], clamp(x - float(i), 0.0, 1.0));
        }
    }
    return result;
}

// Inverse of perimeterParam: the point on the border at distance `s` (dp), clockwise from
// the right-edge midpoint.
float2 boundaryPoint(float s, float2 hs, float r) {
    float2 ih = max(hs - r, float2(0.0));
    float q = PI * r * 0.5;
    float L = 4.0 * ih.x + 4.0 * ih.y + 4.0 * q;
    s = s - L * floor(s / max(L, 0.001));
    float l = ih.y;
    if (s < l) { return float2(hs.x, s); }
    s -= l;
    l = q;
    if (s < l) { float a = s / r; return float2(ih.x, ih.y) + r * float2(cos(a), sin(a)); }
    s -= l;
    l = 2.0 * ih.x;
    if (s < l) { return float2(ih.x - s, hs.y); }
    s -= l;
    l = q;
    if (s < l) { float a = PI * 0.5 + s / r; return float2(-ih.x, ih.y) + r * float2(cos(a), sin(a)); }
    s -= l;
    l = 2.0 * ih.y;
    if (s < l) { return float2(-hs.x, ih.y - s); }
    s -= l;
    l = q;
    if (s < l) { float a = PI + s / r; return float2(-ih.x, -ih.y) + r * float2(cos(a), sin(a)); }
    s -= l;
    l = 2.0 * ih.x;
    if (s < l) { return float2(-ih.x + s, -hs.y); }
    s -= l;
    l = q;
    if (s < l) { float a = PI * 1.5 + s / r; return float2(ih.x, -ih.y) + r * float2(cos(a), sin(a)); }
    s -= l;
    return float2(hs.x, -ih.y + s);
}

// Soft glow of the orbiting beam = a chain of round Gaussian blobs laid along the beam's path on
// the border (like blurring a stroke). It never asks "which edge is nearest", so it cannot make
// seams, triangles or inset rectangles, and corners come out naturally round on any shape.
// Returns premultiplied rgb + brightness, with a peak of about 1 on the border.
float4 orbitGlowBlobs(float2 p, float dist, float2 hs, float r, float sigmaBase) {
    float2 ih = max(hs - r, float2(0.0));
    float q = PI * r * 0.5;
    float L = 4.0 * ih.x + 4.0 * ih.y + 4.0 * q;

    float delta = arc * L / 24.0;                    // spacing between blobs along the border
    float sigma = max(sigmaBase, 0.8 * delta);       // keep blobs overlapping so there is no beading
    if (dist > 3.0 * sigma) { return float4(0.0); }  // far from the border: nothing to draw
    float inv2s2 = 0.5 / (sigma * sigma);
    float norm = delta / (sigma * 2.50663);          // makes the summed peak ~1 along the beam

    float start = 1.0 - arc;
    float4 acc = float4(0.0);
    for (int k = 0; k < 24; k++) {
        float t = (float(k) + 0.5) / 24.0;           // 0 = tail ... 1 = head
        float sp = fract(start + t * arc + direction * phase);
        float2 pt = boundaryPoint(sp * L, hs, r);
        float2 dv = p - pt;
        float g = exp(-dot(dv, dv) * inv2s2);
        if (g < 0.003) { continue; }
        float bell = pow(max(sin(PI * t), 0.0), 1.6); // lighter toward both ends
        float4 c4 = samplePalette(t);
        float v = g * norm * bell * clamp(c4.a, 0.0, 1.0);
        acc += float4(c4.rgb * v, v);
    }
    return acc;
}

half4 main(float2 fragCoord) {
    float2 position = fragCoord / density;           // dp
    float2 hs = size * 0.5;
    float r = min(cornerRadius, min(hs.x, hs.y));
    float2 p = position - hs;
    float dist = -sdRoundedBox(p, hs, r);            // dp inside the shape, measured from the edge
    if (dist < 0.0) { return half4(0.0); }

    float4 acc = float4(0.0);                        // premultiplied rgb + weight, summed over layers

    if (mode < 0.5) {
        // --- crisp line + tight halo, hugging the edge (nearest-edge param is fine this close) ---
        float a = perimeterParam(p, hs, r);
        float tt = (fract(a - direction * phase) - (1.0 - arc)) / arc;
        if (tt >= 0.0) {
            // bell shape: brightest around the middle of the beam, lighter toward both ends
            float profile = pow(max(sin(PI * clamp(tt, 0.0, 1.0)), 0.0), 1.2);
            float crisp = 1.0 - smoothstep(0.5, 2.25, dist);
            float halo = 0.6 * exp(-dist / max(glow * 0.1, 0.6));
            float4 c4 = samplePalette(tt);
            float w = (crisp + halo) * profile * clamp(c4.a, 0.0, 1.0);
            acc += float4(c4.rgb * w, w);
        }
        // --- wide soft glow: chain of blobs along the beam ---
        acc += 0.55 * orbitGlowBlobs(p, dist, hs, r, max(glow, 0.001) * 0.7);
    } else {
        // --- edge sweep: ping-pong along one side with ease in/out ---
        float u;
        float edgeDist;
        int s = int(side + 0.5);
        if (s == 1) {
            u = position.x / size.x;
            edgeDist = position.y;
        } else if (s == 2) {
            u = position.x / size.x;
            edgeDist = size.y - position.y;
        } else if (s == 3) {
            u = position.y / size.y;
            edgeDist = position.x;
        } else {
            u = position.y / size.y;
            edgeDist = size.x - position.x;
        }

        float len = 0.28;
        float tri = phase < 0.5 ? phase * 2.0 : (1.0 - phase) * 2.0;
        float eased = tri * tri * (3.0 - 2.0 * tri);
        float c = len * 0.5 + (1.0 - len) * eased;
        float k = (u - (c - len * 0.5)) / len;
        if (k < 0.0 || k > 1.0) { return half4(0.0); }
        float tp = 1.0 - abs(2.0 * k - 1.0);
        float profile = tp * tp * (3.0 - 2.0 * tp);
        float4 c4 = samplePalette(k);

        float crisp = 1.0 - smoothstep(0.5, 2.25, edgeDist);
        float halo = 0.8 * exp(-edgeDist / max(glow * 0.2, 0.5));
        float wide = 0.6 * exp(-pow(edgeDist / max(glow, 0.001), 1.4));
        float w = ((crisp + halo) * profile + wide * profile) * clamp(c4.a, 0.0, 1.0);
        acc += float4(c4.rgb * w, w);
    }

    float total = acc.a;
    if (total <= 0.0001) { return half4(0.0); }
    float3 rgb = acc.rgb / total;                    // average color of everything contributing here
    float a = clamp(total * intensity, 0.0, 1.0);
    return half4(half3(rgb) * half(a), half(a));     // premultiplied
}
"""

/** Owns the [RuntimeShader] for one beam and draws it over the current draw area. */
@RequiresApi(33)
internal class AgslBeam {
    private val shader = RuntimeShader(BEAM_AGSL)
    private val brush = ShaderBrush(shader)

    fun draw(
        scope: DrawScope,
        shape: BeamShape,
        style: BeamStyle,
        direction: BeamDirection,
        phase: Float,
        palette: PackedPalette,
        intensity: Float,
        alpha: Float,
    ) {
        with(scope) {
            val d = density
            shader.setFloatUniform("size", size.width / d, size.height / d)
            shader.setFloatUniform("density", d)
            shader.setFloatUniform("cornerRadius", shape.cornerRadius)
            shader.setFloatUniform("phase", phase)
            shader.setFloatUniform("direction", direction.shaderValue)
            shader.setFloatUniform("mode", if (style == BeamStyle.line) 1f else 0f)
            shader.setFloatUniform("side", 2f)   // edge for Line: bottom
            shader.setFloatUniform("arc", if (style == BeamStyle.small) 0.5f else 0.36f)
            shader.setFloatUniform("glow", glowReach(style))
            shader.setFloatUniform("intensity", intensity)
            shader.setFloatUniform("count", palette.count.toFloat())
            shader.setFloatUniform("colors", palette.data)
            drawRect(brush = brush, alpha = alpha)
        }
    }
}

/** Soft inner glow reach in dp. */
internal fun glowReach(style: BeamStyle): Float = when (style) {
    BeamStyle.large -> 20f
    BeamStyle.small -> 4f
    BeamStyle.line -> 10f
}
