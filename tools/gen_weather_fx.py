#!/usr/bin/env python3
"""Sinh tài nguyên hiệu ứng thời tiết bán động cho widget.

Chạy: python3 tools/gen_weather_fx.py   (ghi vào app/src/main/res/drawable và res/animator)

Mỗi hiệu ứng là một AnimatedVectorDrawable (fx_<tên>.xml) trỏ tới vector tĩnh (fx_<tên>_vec.xml)
và các animator (fx_<tên>_<đích>.xml). Mọi animator chạy đúng TOTAL ms (số vòng nguyên, kiểu
reverse thì số lượt chẵn) nên hết giờ trở lại đúng khung ban đầu và dừng. Có test kiểm tra điều này.

Để chuyển động bớt "cứng": nhiều lớp tốc độ khác nhau (không đồng bộ), gió lắc ngang chậm,
nét mảnh và nhiều mức độ mờ, sấm chớp ở các thời điểm không đều.
Vector 240x120, widget kéo giãn vừa khung; nội dung nằm gọn trong khung để không tràn mép bo góc.
"""
import math
import os
import random

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
TOTAL = 60000
W, H = 240, 120
HEADER = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Sinh tự động bởi tools/gen_weather_fx.py, không sửa tay. -->\n'
SLANT = 0.3  # độ nghiêng của hạt mưa (dx = -SLANT * độ dài)


def n(x):
    s = ("%.2f" % x).rstrip("0").rstrip(".")
    return "0" if s in ("", "-0") else s


def nf(x):
    s = ("%.5f" % x).rstrip("0").rstrip(".")
    return "0" if s in ("", "-0") else s


def write(kind, name, text):
    folder = os.path.join(ROOT, kind)
    os.makedirs(folder, exist_ok=True)
    with open(os.path.join(folder, name + ".xml"), "w", encoding="utf-8") as f:
        f.write(text)


def clean():
    for kind in ("drawable", "animator"):
        folder = os.path.join(ROOT, kind)
        if os.path.isdir(folder):
            for name in os.listdir(folder):
                if name.startswith("fx_"):
                    os.remove(os.path.join(folder, name))


def vector(body, gradient=False):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if gradient else ""
    return (
        HEADER
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"' + ns + "\n"
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


def anim_set(items):
    """Nhiều thuộc tính của cùng một đích (nhóm) chạy song song: một animator set gồm nhiều objectAnimator."""
    out = HEADER + '<set xmlns:android="http://schemas.android.com/apk/res/android"\n    android:ordering="together">\n'
    for attrs in items:
        out += "    <objectAnimator\n        " + "\n        ".join(attrs) + " />\n"
    return out + "</set>\n"


def scale_set(a, b, dur, passes):
    return anim_set([attrs_of(p, a, b, dur, passes, "accelerate_decelerate", True) for p in ("scaleX", "scaleY")])


def keyframes(prop, pts):
    assert pts[0][0] == 0 and pts[-1][0] == 1
    assert all(pts[i][0] < pts[i + 1][0] for i in range(len(pts) - 1)), "fraction phải tăng dần"
    kf = "".join('        <keyframe android:fraction="%s" android:value="%s" />\n' % (nf(f), n(v)) for f, v in pts)
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


# ---------------------------------------------------------------- mưa và giông


def streak_layer(rng, name, period, count, length, buckets, indent):
    """Một lớp hạt mưa lặp theo chu kỳ `period` (tịnh tiến đúng một chu kỳ thì khung hình y hệt).
    buckets: [(màu, độ mờ, độ dày)], mỗi hạt rơi ngẫu nhiên vào một mức."""
    rows = int(math.ceil(H / period)) + 1
    ds = [[] for _ in buckets]
    for x in stratified(rng, count, 4, 236):
        b = rng.randrange(len(buckets))
        ln = rng.uniform(*length)
        y = rng.uniform(0, period)
        for k in range(-1, rows):
            yy = y + k * period
            ds[b].append("M%s,%sL%s,%s" % (n(x), n(yy), n(x - SLANT * ln), n(yy + ln)))
    pad = " " * indent
    out = '%s<group android:name="%s">\n' % (pad, name)
    for d, (color, alpha, width) in zip(ds, buckets):
        if not d:
            continue
        out += (
            '%s    <path\n%s        android:pathData="%s"\n%s        android:strokeAlpha="%s"\n'
            '%s        android:strokeColor="%s"\n%s        android:strokeLineCap="round"\n'
            '%s        android:strokeWidth="%s" />\n'
        ) % (pad, pad, "".join(d), pad, n(alpha), pad, color, pad, pad, n(width))
    return out + pad + "</group>\n"


def rain_parts(seed, scale, boost):
    rng = random.Random(seed)
    far = streak_layer(rng, "far", 34, int(12 * scale), (5, 8), [("#DCE9F7", 0.16 * boost, 0.45), ("#E6F0FB", 0.26 * boost, 0.5)], 8)
    mid = streak_layer(rng, "mid", 48, int(8 * scale), (8, 12), [("#DCE9F7", 0.22 * boost, 0.6), ("#EAF3FF", 0.34 * boost, 0.65)], 8)
    near = streak_layer(rng, "near", 70, int(5 * scale), (12, 18), [("#EAF3FF", 0.3 * boost, 0.85), ("#FFFFFF", 0.44 * boost, 0.9)], 8)
    body = '    <group android:name="wind">\n' + far + mid + near + "    </group>\n"
    anims = {
        "wind": loop("translateX", 0, -6, 5000, 12, "accelerate_decelerate", True),
        "far": anim_set([attrs_of("translateY", 0, 34, 1500, 40), attrs_of("translateX", 0, 3, 3000, 20, "accelerate_decelerate", True)]),
        "mid": anim_set([attrs_of("translateY", 0, 48, 1250, 48), attrs_of("translateX", 0, -3, 2500, 24, "accelerate_decelerate", True)]),
        "near": anim_set([attrs_of("translateY", 0, 70, 1000, 60), attrs_of("translateX", 0, 4, 2000, 30, "accelerate_decelerate", True)]),
    }
    return body, anims


def gen_rain():
    body, anims = rain_parts(11, 1.0, 1.0)
    write("drawable", "fx_rain_vec", vector(body))
    write("drawable", "fx_rain", avd("rain", list(anims)))
    for k, v in anims.items():
        write("animator", "fx_rain_" + k, v)


def flash_pattern(events):
    """Các lần chớp (thời điểm ms, cường độ 0..1): bật nhanh, nhấp nháy, tắt dần. Trả về (phân số, giá trị)."""
    pts = [(0.0, 0.0)]
    for t, s in events:
        for dt, v in ((-1, 0.0), (60, s), (160, 0.15 * s), (240, 0.75 * s), (420, 0.0)):
            pts.append(((t + dt) / TOTAL, v))
    pts.append((1.0, 0.0))
    pts.sort(key=lambda p: p[0])
    out = []
    for f, v in pts:
        if out and f <= out[-1][0]:
            continue
        out.append((f, v))
    return out


def gen_storm():
    body, anims = rain_parts(23, 1.5, 1.2)
    bolt = "M139,30L125.8,63L137.8,63L127,90L151,54L139,54Z"
    body += (
        '    <path\n        android:name="flash"\n        android:fillAlpha="0"\n        android:fillColor="#FFFFFF"\n'
        '        android:pathData="M0,0H240V120H0Z" />\n'
        '    <path\n        android:name="bolt"\n        android:fillAlpha="0"\n        android:fillColor="#FFF3A8"\n'
        '        android:pathData="%s" />\n' % bolt
    )
    events = [(3000, 1.0), (11500, 0.7), (12200, 1.0), (27000, 0.9), (38500, 0.6), (39100, 1.0), (52000, 0.8)]
    pattern = flash_pattern(events)
    write("drawable", "fx_storm_vec", vector(body))
    write("drawable", "fx_storm", avd("storm", list(anims) + ["flash", "bolt"]))
    for k, v in anims.items():
        write("animator", "fx_storm_" + k, v)
    write("animator", "fx_storm_flash", keyframes("fillAlpha", [(f, 0.4 * v) for f, v in pattern]))
    write("animator", "fx_storm_bolt", keyframes("fillAlpha", [(f, 0.85 * v) for f, v in pattern]))


# ---------------------------------------------------------------- tuyết


def flake_layer(rng, name, period, count, r, alpha, indent):
    rows = int(math.ceil(H / period)) + 1
    d = []
    for x in stratified(rng, count, 6, 234):
        y = rng.uniform(0, period)
        rr = r * rng.uniform(0.8, 1.2)
        for k in range(-1, rows):
            d.append(circle(x, y + k * period, rr))
    pad = " " * indent
    return (
        '%s<group android:name="%s">\n%s    <path\n%s        android:fillAlpha="%s"\n%s        android:fillColor="#FFFFFF"\n'
        '%s        android:pathData="%s" />\n%s</group>\n'
    ) % (pad, name, pad, pad, n(alpha), pad, pad, "".join(d), pad)


def gen_snow():
    rng = random.Random(31)
    body = (
        flake_layer(rng, "far", 60, 14, 0.7, 0.55, 4)
        + flake_layer(rng, "mid", 70, 9, 1.1, 0.7, 4)
        + flake_layer(rng, "near", 84, 6, 1.6, 0.85, 4)
    )
    write("drawable", "fx_snow_vec", vector(body))
    write("drawable", "fx_snow", avd("snow", ["far", "mid", "near"]))
    write("animator", "fx_snow_far", anim_set([attrs_of("translateY", 0, 60, 5000, 12), attrs_of("translateX", -2, 2, 3000, 20, "accelerate_decelerate", True)]))
    write("animator", "fx_snow_mid", anim_set([attrs_of("translateY", 0, 70, 3750, 16), attrs_of("translateX", -3, 3, 2500, 24, "accelerate_decelerate", True)]))
    write("animator", "fx_snow_near", anim_set([attrs_of("translateY", 0, 84, 3000, 20), attrs_of("translateX", -4, 4, 2000, 30, "accelerate_decelerate", True)]))


# ---------------------------------------------------------------- mây và sương


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
    ease = "accelerate_decelerate"
    write("drawable", "fx_clouds_vec", vector(body, gradient=True))
    write("drawable", "fx_clouds", avd("clouds", ["a", "b", "c"]))
    write("animator", "fx_clouds_a", anim_set([attrs_of("translateX", 0, 16, 7500, 8, ease, True), attrs_of("translateY", 0, 2, 5000, 12, ease, True)]))
    write("animator", "fx_clouds_b", anim_set([attrs_of("translateX", 0, -14, 10000, 6, ease, True), attrs_of("translateY", 0, -2, 6000, 10, ease, True)]))
    write("animator", "fx_clouds_c", loop("translateX", 0, 10, 6000, 10, ease, True))


def gen_fog():
    def band(name, cx, cy, r, sy):
        g = '    <group\n        android:name="%s"\n        android:pivotX="%s"\n        android:pivotY="%s"\n        android:scaleY="%s">\n' % (name, n(cx), n(cy), n(sy))
        g += radial(circle(cx, cy, r), cx, cy, r, "#59FFFFFF", "#33FFFFFF", "#00FFFFFF")
        return g + "    </group>\n"

    body = band("a", 120, 24, 100, 0.24) + band("b", 150, 62, 110, 0.22) + band("c", 90, 98, 100, 0.24)
    ease = "accelerate_decelerate"
    write("drawable", "fx_fog_vec", vector(body, gradient=True))
    write("drawable", "fx_fog", avd("fog", ["a", "b", "c"]))
    write("animator", "fx_fog_a", loop("translateX", 0, 14, 7500, 8, ease, True))
    write("animator", "fx_fog_b", loop("translateX", 0, -16, 10000, 6, ease, True))
    write("animator", "fx_fog_c", loop("translateX", 0, 12, 6000, 10, ease, True))


# ---------------------------------------------------------------- sao và nắng


def sparkle(cx, cy, s):
    t = 0.3 * s
    pts = [(cx, cy - s), (cx + t, cy - t), (cx + s, cy), (cx + t, cy + t), (cx, cy + s), (cx - t, cy + t), (cx - s, cy), (cx - t, cy - t)]
    return "M" + "L".join("%s,%s" % (n(x), n(y)) for x, y in pts) + "Z"


def gen_stars():
    rng = random.Random(47)
    spots = [(x, rng.uniform(8, 78), rng.choice([2.2, 2.8, 3.4])) for x in stratified(rng, 15, 8, 184)]
    groups = [spots[i::5] for i in range(5)]
    specs = [  # (từ, tới, ms, số lượt): nhấp nháy chậm, mỗi nhóm một nhịp
        (0.25, 1.0, 1200, 50), (1.0, 0.3, 1500, 40), (0.4, 1.0, 2000, 30), (0.2, 0.9, 2500, 24), (1.0, 0.35, 3000, 20),
    ]
    glow = radial(circle(201, 24, 42), 201, 24, 42, "#59DCE6FF", "#26DCE6FF", "#00DCE6FF")
    body = '    <group\n        android:name="moon"\n        android:pivotX="201"\n        android:pivotY="24"\n        android:scaleX="0.9"\n        android:scaleY="0.9">\n' + glow + "    </group>\n"
    for i, grp in enumerate(groups):
        d = "".join(sparkle(x, y, s) for x, y, s in grp)
        body += '    <path\n        android:name="s%d"\n        android:fillAlpha="%s"\n        android:fillColor="#FFFFFF"\n        android:pathData="%s" />\n' % (i + 1, n(specs[i][0]), d)
    write("drawable", "fx_stars_vec", vector(body, gradient=True))
    write("drawable", "fx_stars", avd("stars", ["moon"] + ["s%d" % (i + 1) for i in range(5)]))
    write("animator", "fx_stars_moon", scale_set(0.9, 1.12, 3000, 20))
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
    write("animator", "fx_sun_glow", scale_set(0.88, 1.12, 3000, 20))
    write("animator", "fx_sun_rays", loop("rotation", -4, 4, 6000, 10, "accelerate_decelerate", True))


if __name__ == "__main__":
    clean()
    gen_rain()
    gen_storm()
    gen_snow()
    gen_clouds()
    gen_fog()
    gen_stars()
    gen_sun()
    print("xong")
