#!/usr/bin/env python3
"""Sinh tài nguyên hiệu ứng thời tiết bán động cho widget.

Chạy: python3 tools/gen_weather_fx.py   (ghi vào app/src/main/res/drawable và res/animator)

Mỗi hiệu ứng là một AnimatedVectorDrawable (fx_<tên>.xml) trỏ tới vector tĩnh (fx_<tên>_vec.xml)
và các animator (fx_<tên>_<đích>.xml). Mọi animator chạy đúng TOTAL ms (số vòng nguyên, kiểu
reverse thì số lượt chẵn) nên hết giờ trở lại đúng khung ban đầu và dừng. Có test kiểm tra điều này.
Vector 240x120, widget kéo giãn vừa khung; nội dung nằm gọn trong khung để không tràn mép bo góc.
"""
import math
import os
import random

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
TOTAL = 3600
W, H = 240, 120
HEADER = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Sinh tự động bởi tools/gen_weather_fx.py, không sửa tay. -->\n'


def n(x):
    s = ("%.2f" % x).rstrip("0").rstrip(".")
    return "0" if s in ("", "-0") else s


def write(kind, name, text):
    folder = os.path.join(ROOT, kind)
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(folder, name + ".xml"), "w", encoding="utf-8") as f:
        f.write(text)


def vector(body, gradient=False):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if gradient else ""
    return (
        HEADER
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"' + ns + '\n'
        + '    android:width="240dp"\n    android:height="120dp"\n'
        + '    android:viewportWidth="240"\n    android:viewportHeight="120">\n'
        + '    <clip-path android:pathData="M0,0H240V120H0Z" />\n'
        + body + "</vector>\n"
    )


def avd(name, targets):
    t = "".join(
        '    <target\n        android:name="%s"\n        android:animation="@animator/fx_%s_%s" />\n' % (tn, name, tn)
        for tn in targets
    )
    return (
        HEADER
        + '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:drawable="@drawable/fx_%s_vec">\n' % name + t + "</animated-vector>\n"
    )


def attrs_of(prop, a, b, dur, passes, interp="linear", reverse=False):
    assert dur * passes == TOTAL, (prop, dur, passes)
    assert not reverse or passes % 2 == 0, "reverse cần số lượt chẵn để về đúng khung đầu"
    attrs = [
        'android:duration="%d"' % dur,
        'android:interpolator="@android:interpolator/%s"' % interp,
        'android:propertyName="%s"' % prop,
    ]
    if passes > 1:
        attrs.append('android:repeatCount="%d"' % (passes - 1))
        attrs.append('android:repeatMode="%s"' % ("reverse" if reverse else "restart"))
    attrs += ['android:valueFrom="%s"' % n(a), 'android:valueTo="%s"' % n(b), 'android:valueType="floatType"']
    return attrs


def loop(prop, a, b, dur, passes, interp="linear", reverse=False):
    attrs = attrs_of(prop, a, b, dur, passes, interp, reverse)
    return HEADER + '<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"\n    ' + "\n    ".join(attrs) + " />\n"


def scale_set(a, b, dur, passes):
    """Phóng/thu đều theo cả hai trục: một đích (nhóm) cần một animator set gồm hai objectAnimator."""
    out = HEADER + '<set xmlns:android="http://schemas.android.com/apk/res/android"\n    android:ordering="together">\n'
    for prop in ("scaleX", "scaleY"):
        attrs = attrs_of(prop, a, b, dur, passes, "accelerate_decelerate", True)
        out += "    <objectAnimator\n        " + "\n        ".join(attrs) + " />\n"
    return out + "</set>\n"


def keyframes(prop, pts):
    assert pts[0][0] == 0 and pts[-1][0] == 1
    kf = "".join('        <keyframe android:fraction="%s" android:value="%s" />\n' % (n(f), n(v)) for f, v in pts)
    return (
        HEADER
        + '<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:duration="%d"\n    android:interpolator="@android:interpolator/linear">\n' % TOTAL
        + '    <propertyValuesHolder\n        android:propertyName="%s"\n        android:valueType="floatType">\n' % prop
        + kf + "    </propertyValuesHolder>\n</objectAnimator>\n"
    )


def circle(cx, cy, r):
    return "M%s,%sa%s,%s 0 1,0 %s,0a%s,%s 0 1,0 %s,0Z" % (n(cx - r), n(cy), n(r), n(r), n(2 * r), n(r), n(r), n(-2 * r))


def radial(d, cx, cy, r, start, center, end):
    return (
        '        <path android:pathData="%s">\n'
        '            <aapt:attr name="android:fillColor">\n'
        '                <gradient\n                    android:centerX="%s"\n                    android:centerY="%s"\n'
        '                    android:gradientRadius="%s"\n                    android:startColor="%s"\n'
        '                    android:centerColor="%s"\n                    android:endColor="%s"\n'
        '                    android:type="radial" />\n'
        '            </aapt:attr>\n        </path>\n'
    ) % (d, n(cx), n(cy), n(r), start, center, end)


def stratified(rng, count, lo, hi):
    step = (hi - lo) / count
    xs = [lo + (i + rng.random()) * step for i in range(count)]
    rng.shuffle(xs)
    return xs


def streak_path(rng, count, period, length, dx):
    rows = int(math.ceil(H / period)) + 1
    segs = []
    for x in stratified(rng, count, 4, 236):
        y = rng.uniform(0, period - length * 0.2)
        for k in range(-1, rows):
            yy = y + k * period
            segs.append("M%s,%sL%s,%s" % (n(x), n(yy), n(x + dx), n(yy + length)))
    return "".join(segs)


def streak_group(name, d, color, alpha, width):
    return (
        '    <group android:name="%s">\n'
        '        <path\n            android:pathData="%s"\n            android:strokeAlpha="%s"\n'
        '            android:strokeColor="%s"\n            android:strokeLineCap="round"\n'
        '            android:strokeWidth="%s" />\n    </group>\n'
    ) % (name, d, n(alpha), color, n(width))


def rain_layers(seed, far_count, near_count):
    rng = random.Random(seed)
    far = streak_path(rng, far_count, 30, 8, -2.6)
    near = streak_path(rng, near_count, 48, 14, -4.5)
    body = streak_group("far", far, "#DCE9F7", 0.38, 0.7) + streak_group("near", near, "#EAF3FF", 0.5, 1.1)
    anims = {
        "far": loop("translateY", 0, 30, 600, 6),
        "near": loop("translateY", 0, 48, 450, 8),
    }
    return body, anims


def gen_rain():
    body, anims = rain_layers(11, 22, 12)
    write("drawable", "fx_rain_vec", vector(body))
    write("drawable", "fx_rain", avd("rain", ["far", "near"]))
    for k, v in anims.items():
        write("animator", "fx_rain_" + k, v)


def gen_storm():
    body, anims = rain_layers(23, 26, 16)
    bolt = "M139,30L125.8,63L137.8,63L127,90L151,54L139,54Z"
    body += (
        '    <path\n        android:name="flash"\n        android:fillAlpha="0"\n        android:fillColor="#FFFFFF"\n'
        '        android:pathData="M0,0H240V120H0Z" />\n'
        '    <path\n        android:name="bolt"\n        android:fillAlpha="0"\n        android:fillColor="#FFF3A8"\n'
        '        android:pathData="%s" />\n' % bolt
    )
    pattern = [(0, 0), (0.10, 0), (0.115, 1), (0.15, 0.15), (0.17, 0.75), (0.23, 0), (0.55, 0), (0.565, 0.6), (0.60, 0), (1, 0)]
    write("drawable", "fx_storm_vec", vector(body))
    write("drawable", "fx_storm", avd("storm", ["far", "near", "flash", "bolt"]))
    for k, v in anims.items():
        write("animator", "fx_storm_" + k, v)
    write("animator", "fx_storm_flash", keyframes("fillAlpha", [(f, 0.45 * p) for f, p in pattern]))
    write("animator", "fx_storm_bolt", keyframes("fillAlpha", [(f, 0.9 * p) for f, p in pattern]))


def flake_path(rng, count, period, r, rows_extra=1):
    rows = int(math.ceil(H / period)) + rows_extra
    d = []
    for x in stratified(rng, count, 6, 234):
        y = rng.uniform(0, period)
        for k in range(-1, rows):
            d.append(circle(x, y + k * period, r))
    return "".join(d)


def gen_snow():
    rng = random.Random(31)
    far = flake_path(rng, 16, 60, 0.9)
    near = flake_path(rng, 9, 80, 1.5)

    def layer(move, sway, d, alpha):
        return (
            '    <group android:name="%s">\n        <group android:name="%s">\n'
            '            <path\n                android:fillAlpha="%s"\n                android:fillColor="#FFFFFF"\n'
            '                android:pathData="%s" />\n        </group>\n    </group>\n'
        ) % (move, sway, n(alpha), d)

    body = layer("far", "far_sway", far, 0.7) + layer("near", "near_sway", near, 0.9)
    write("drawable", "fx_snow_vec", vector(body))
    write("drawable", "fx_snow", avd("snow", ["far", "far_sway", "near", "near_sway"]))
    write("animator", "fx_snow_far", loop("translateY", 0, 60, 1800, 2))
    write("animator", "fx_snow_far_sway", loop("translateX", -2, 2, 900, 4, "accelerate_decelerate", True))
    write("animator", "fx_snow_near", loop("translateY", 0, 80, 1200, 3))
    write("animator", "fx_snow_near_sway", loop("translateX", -3.5, 3.5, 600, 6, "accelerate_decelerate", True))


def gen_clouds():
    def cloud(name, puffs, alpha_hex):
        out = '    <group android:name="%s">\n' % name
        for cx, cy, r in puffs:
            out += radial(circle(cx, cy, r), cx, cy, r, "#" + alpha_hex[0] + "FFFFFF", "#" + alpha_hex[1] + "FFFFFF", "#00FFFFFF")
        return out + "    </group>\n"

    body = (
        cloud("a", [(46, 30, 24), (70, 26, 30), (98, 32, 24), (36, 38, 18)], ("66", "30"))
        + cloud("b", [(168, 72, 28), (194, 66, 24), (146, 78, 22), (184, 82, 26)], ("5A", "2A"))
        + cloud("c", [(70, 100, 22), (96, 96, 18), (52, 104, 16)], ("4D", "24"))
    )
    write("drawable", "fx_clouds_vec", vector(body, gradient=True))
    write("drawable", "fx_clouds", avd("clouds", ["a", "b", "c"]))
    write("animator", "fx_clouds_a", loop("translateX", 0, 14, 1800, 2, "accelerate_decelerate", True))
    write("animator", "fx_clouds_b", loop("translateX", 0, -12, 1800, 2, "accelerate_decelerate", True))
    write("animator", "fx_clouds_c", loop("translateX", 0, 8, 900, 4, "accelerate_decelerate", True))


def gen_fog():
    def band(name, cx, cy, r, sy):
        g = '    <group\n        android:name="%s"\n        android:pivotX="%s"\n        android:pivotY="%s"\n        android:scaleY="%s">\n' % (name, n(cx), n(cy), n(sy))
        g += radial(circle(cx, cy, r), cx, cy, r, "#59FFFFFF", "#33FFFFFF", "#00FFFFFF")
        return g + "    </group>\n"

    body = band("a", 120, 24, 100, 0.24) + band("b", 150, 62, 110, 0.22) + band("c", 90, 98, 100, 0.24)
    write("drawable", "fx_fog_vec", vector(body, gradient=True))
    write("drawable", "fx_fog", avd("fog", ["a", "b", "c"]))
    write("animator", "fx_fog_a", loop("translateX", 0, 12, 1800, 2, "accelerate_decelerate", True))
    write("animator", "fx_fog_b", loop("translateX", 0, -14, 900, 4, "accelerate_decelerate", True))
    write("animator", "fx_fog_c", loop("translateX", 0, 10, 1800, 2, "accelerate_decelerate", True))


def sparkle(cx, cy, s):
    t = 0.3 * s
    pts = [(cx, cy - s), (cx + t, cy - t), (cx + s, cy), (cx + t, cy + t), (cx, cy + s), (cx - t, cy + t), (cx - s, cy), (cx - t, cy - t)]
    return "M" + "L".join("%s,%s" % (n(x), n(y)) for x, y in pts) + "Z"


def gen_stars():
    rng = random.Random(47)
    spots = []
    for x in stratified(rng, 15, 8, 184):
        spots.append((x, rng.uniform(8, 78), rng.choice([2.2, 2.8, 3.4])))
    groups = [spots[i::5] for i in range(5)]
    specs = [  # (từ, tới, ms, số lượt)
        (0.25, 1.0, 600, 6), (1.0, 0.3, 900, 4), (0.4, 1.0, 450, 8), (0.2, 0.9, 1800, 2), (1.0, 0.35, 600, 6),
    ]
    body = radial(circle(201, 24, 42), 201, 24, 42, "#59DCE6FF", "#26DCE6FF", "#00DCE6FF")
    body = '    <group\n        android:name="moon"\n        android:pivotX="201"\n        android:pivotY="24"\n        android:scaleX="0.9"\n        android:scaleY="0.9">\n' + body + "    </group>\n"
    for i, grp in enumerate(groups):
        d = "".join(sparkle(x, y, s) for x, y, s in grp)
        body += '    <path\n        android:name="s%d"\n        android:fillAlpha="%s"\n        android:fillColor="#FFFFFF"\n        android:pathData="%s" />\n' % (i + 1, n(specs[i][0]), d)
    write("drawable", "fx_stars_vec", vector(body, gradient=True))
    write("drawable", "fx_stars", avd("stars", ["moon"] + ["s%d" % (i + 1) for i in range(5)]))
    write("animator", "fx_stars_moon", scale_set(0.9, 1.12, 1800, 2))
    for i, (a, b, dur, passes) in enumerate(specs):
        write("animator", "fx_stars_s%d" % (i + 1), loop("fillAlpha", a, b, dur, passes, "accelerate_decelerate", True))


def gen_sun():
    cx, cy, L = 201, 13, 150
    rays = []
    for a in [98 + i * 13 for i in range(8)]:
        d = 2.4
        p1 = (cx + L * math.cos(math.radians(a - d)), cy + L * math.sin(math.radians(a - d)))
        p2 = (cx + L * math.cos(math.radians(a + d)), cy + L * math.sin(math.radians(a + d)))
        rays.append("M%s,%sL%s,%sL%s,%sZ" % (n(cx), n(cy), n(p1[0]), n(p1[1]), n(p2[0]), n(p2[1])))
    glow = (
        '    <group\n        android:name="glow"\n        android:pivotX="%d"\n        android:pivotY="%d"\n        android:scaleX="0.88"\n        android:scaleY="0.88">\n' % (cx, cy)
        + radial(circle(cx, cy, 60), cx, cy, 60, "#66FFF1C2", "#26FFE9A8", "#00FFE9A8")
        + "    </group>\n"
    )
    rays_xml = (
        '    <group\n        android:name="rays"\n        android:pivotX="%d"\n        android:pivotY="%d"\n        android:rotation="-4">\n' % (cx, cy)
        + radial("".join(rays), cx, cy, L, "#52FFF6D0", "#26FFF6D0", "#00FFF6D0")
        + "    </group>\n"
    )
    write("drawable", "fx_sun_vec", vector(glow + rays_xml, gradient=True))
    write("drawable", "fx_sun", avd("sun", ["glow", "rays"]))
    write("animator", "fx_sun_glow", scale_set(0.88, 1.12, 900, 4))
    write("animator", "fx_sun_rays", loop("rotation", -4, 4, 1800, 2, "accelerate_decelerate", True))


if __name__ == "__main__":
    gen_rain()
    gen_storm()
    gen_snow()
    gen_clouds()
    gen_fog()
    gen_stars()
    gen_sun()
    print("xong")
