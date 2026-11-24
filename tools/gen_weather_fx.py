#!/usr/bin/env python3
"""Sinh tài nguyên hiệu ứng thời tiết bán động cho widget.

Chạy: python3 tools/gen_weather_fx.py   (ghi vào app/src/main/res/drawable và res/animator)

Mỗi hiệu ứng là một AnimatedVectorDrawable (fx_<tên>.xml) trỏ tới vector tĩnh (fx_<tên>_vec.xml)
và các animator (fx_<tên>_<đích>.xml). Mọi animator chạy đúng TOTAL ms (số vòng nguyên, kiểu
reverse thì số lượt chẵn) nên hết giờ trở lại đúng khung ban đầu và dừng. Có test kiểm tra điều này.

Để chuyển động bớt "cứng": nhiều lớp tốc độ khác nhau (không đồng bộ), nét mảnh và nhiều mức độ mờ.
Mưa là giọt nước bám trên kính (như kính xe ôtô): hạt li ti, giọt bóng trượt từng nấc để lại vệt rồi tan,
giọt mới ngưng tụ lại; sấm chớp ở các thời điểm không đều (lớp fx_flash phủ lên lớp mưa).
Vector 240x120, widget kéo giãn vừa khung; nội dung nằm gọn trong khung để không tràn mép bo góc.
"""
import math
import os
import random

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
TOTAL = 60000
W, H = 240, 120
HEADER = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Sinh tự động bởi tools/gen_weather_fx.py, không sửa tay. -->\n'


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
    """targets: tên đích (animator là fx_<name>_<đích>) hoặc cặp (đích, tên animator có sẵn)."""
    t = ""
    for tg in targets:
        tn, res = tg if isinstance(tg, tuple) else (tg, "fx_%s_%s" % (name, tg))
        t += '    <target\n        android:name="%s"\n        android:animation="@animator/%s" />\n' % (tn, res)
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


def radial(d, cx, cy, r, start, center, end, stroke=None, sw=0.3):
    st = ('\n            android:strokeColor="%s"\n            android:strokeWidth="%s"' % (stroke, n(sw))) if stroke else ""
    return (
        '        <path\n            android:pathData="%s"%s>\n'
        '            <aapt:attr name="android:fillColor">\n'
        '                <gradient\n                    android:centerX="%s"\n                    android:centerY="%s"\n'
        '                    android:gradientRadius="%s"\n                    android:startColor="%s"\n'
        '                    android:centerColor="%s"\n                    android:endColor="%s"\n'
        '                    android:type="radial" />\n'
        '            </aapt:attr>\n        </path>\n'
    ) % (d, st, n(cx), n(cy), n(r), start, center, end)


def solid(d, color):
    return '        <path\n            android:fillColor="%s"\n            android:pathData="%s" />\n' % (color, d)


def ellipse(cx, cy, rx, ry):
    return "M%s,%sa%s,%s 0 1,0 %s,0a%s,%s 0 1,0 %s,0Z" % (n(cx - rx), n(cy), n(rx), n(ry), n(2 * rx), n(rx), n(ry), n(-2 * rx))


def stratified(rng, count, lo, hi):
    step = (hi - lo) / count
    xs = [lo + (i + rng.random()) * step for i in range(count)]
    rng.shuffle(xs)
    return xs


# ---------------------------------------------------------------- mưa trên kính (kiểu kính xe ôtô)


def multi(holders):
    """Animator keyframe nhiều thuộc tính trong một file. holders: {thuộc tính: [(ms, giá trị, nội suy|None)]}."""
    out = (
        HEADER
        + '<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:duration="%d"\n    android:interpolator="@android:interpolator/linear">\n' % TOTAL
    )
    for prop, pts in holders.items():
        pts = list(pts)
        if pts[0][0] > 0:
            pts.insert(0, (0, pts[0][1], None))
        if pts[-1][0] < TOTAL:
            pts.append((TOTAL, pts[-1][1], None))
        assert pts[0][0] == 0 and pts[-1][0] == TOTAL, prop
        assert all(pts[i][0] < pts[i + 1][0] for i in range(len(pts) - 1)), (prop, [p[0] for p in pts])
        out += '    <propertyValuesHolder\n        android:propertyName="%s"\n        android:valueType="floatType">\n' % prop
        for ms, v, ip in pts:
            ia = ('\n            android:interpolator="@android:interpolator/%s"' % ip) if ip else ""
            out += '        <keyframe\n            android:fraction="%s"\n            android:value="%s"%s />\n' % (nf(ms / TOTAL), n(v), ia)
        out += "    </propertyValuesHolder>\n"
    return out + "</objectAnimator>\n"


def place(rng, placed, r, y_lo=9, y_hi=111):
    """Chọn chỗ cho giọt bán kính r, không chồng lên giọt đã đặt."""
    import math as m
    for _ in range(400):
        x, y = rng.uniform(9, 231), rng.uniform(y_lo, y_hi)
        if all(m.hypot(x - px, y - py) >= (r + pr) * 1.2 for px, py, pr in placed):
            placed.append((x, y, r))
            return x, y
    raise RuntimeError("không đặt được giọt")


def drop_group(name, cx, cy, r):
    """Giọt nước như thấu kính trên kính: thân trong suốt viền sáng ở mép dưới phải, bóng đổ mềm,
    điểm sáng phản chiếu ở góc trên trái."""
    rx, ry = r, r * 1.12
    out = '    <group\n        android:name="%s"\n        android:pivotX="%s"\n        android:pivotY="%s">\n' % (name, n(cx), n(cy))
    big = r >= 2.2
    if big:
        out += radial(ellipse(cx + 0.3 * r, cy + 0.5 * r, rx * 1.1, ry * 1.05), cx + 0.3 * r, cy + 0.5 * r, r * 1.35, "#40000000", "#1A000000", "#00000000")
    out += radial(ellipse(cx, cy, rx, ry), cx - 0.18 * r, cy - 0.22 * r, r * 1.15, "#0FFFFFFF", "#22FFFFFF", "#66FFFFFF", "#8CFFFFFF", 0.3)
    out += solid(ellipse(cx - 0.36 * r, cy - 0.42 * r, 0.27 * r, 0.17 * r), "#E6FFFFFF")
    if big:
        out += solid(circle(cx + 0.42 * r, cy + 0.52 * r, 0.12 * r), "#70FFFFFF")
    return out + "    </group>\n"


def slide_profile(rng):
    """Giọt trượt theo từng nấc (dính rồi tuột): (phần thời gian, phần quãng đường)."""
    a1 = rng.uniform(0.16, 0.24)
    a2 = a1 + rng.uniform(0.10, 0.16)
    a3 = a2 + rng.uniform(0.26, 0.34)
    a4 = a3 + rng.uniform(0.06, 0.12)
    d1, d2 = rng.uniform(0.12, 0.20), rng.uniform(0.55, 0.65)
    return [(a1, d1), (a2, d1), (a3, d2), (a4, d2), (1.0, 1.0)]


def gen_rain():
    rng = random.Random(11)
    placed = []
    large = [(3.4 + rng.random() * 1.0) for _ in range(5)]
    medium = [(2.2 + rng.random() * 0.8) for _ in range(10)]
    small = [(1.3 + rng.random() * 0.7) for _ in range(20)]
    # Giọt trượt cần chỗ trống bên dưới nên đặt ở nửa trên; còn lại rải đều.
    drops = []  # (tên, x, y, r, loại)
    kinds = (["slide"] * 3 + ["churn"] + ["wobble"]) + (["slide"] * 3 + ["churn"] * 4 + ["wobble"] * 3) + (["churn"] * 8 + ["wobble"] * 4 + ["still"] * 8)
    sizes = large + medium + small
    for kind, r in zip(kinds, sizes):
        x, y = place(rng, placed, r, 10, 60 if kind == "slide" else 111)
        drops.append((kind, x, y, r))
    for i, (kind, x, y, r) in enumerate(drops):
        drops[i] = ("d%d" % (i + 1), kind, x, y, r)

    # Các hạt nước li ti bám dày: gộp mỗi loại vào một path để nhẹ.
    beads, glints = [], []
    for _ in range(46):
        x, y, r = rng.uniform(6, 234), rng.uniform(6, 114), rng.uniform(0.6, 1.2)
        beads.append(circle(x, y, r))
        glints.append(circle(x - 0.35 * r, y - 0.4 * r, 0.3))
    body = (
        '    <path\n        android:fillColor="#1FFFFFFF"\n        android:pathData="%s"\n        android:strokeColor="#4DFFFFFF"\n        android:strokeWidth="0.22" />\n' % "".join(beads)
        + '    <path\n        android:name="glint"\n        android:fillAlpha="0.55"\n        android:fillColor="#FFFFFF"\n        android:pathData="%s" />\n' % "".join(glints)
    )
    targets = [("glint", "fx_rain_glint")]
    anims = {"fx_rain_glint": loop("fillAlpha", 0.55, 1.0, 2500, 24, "accelerate_decelerate", True)}
    for k, (dur, passes) in enumerate([(2500, 24), (3000, 20), (3750, 16)]):
        anims["fx_rain_wobble_%s" % "abc"[k]] = anim_set([
            attrs_of("scaleX", 1, 1.07, dur, passes, "accelerate_decelerate", True),
            attrs_of("scaleY", 1, 0.93, dur, passes, "accelerate_decelerate", True),
        ])

    trails, groups, wob = "", "", 0
    ease = "accelerate_decelerate"
    for name, kind, x, y, r in sorted(drops, key=lambda d: d[4]):
        groups += drop_group(name, x, y, r)
        idx = name[1:]
        if kind == "slide":
            D = min(rng.uniform(30, 58), 112 - y - r)
            ts, dur, gap = rng.uniform(1500, 38000), rng.uniform(3500, 6500), rng.uniform(1500, 6000)
            t_end = ts + dur
            t_re = t_end + 800 + gap
            prof = slide_profile(rng)
            ty = [(ts, 0, None)] + [(ts + a * dur, d * D, ease) for a, d in prof] + [(t_end + 800, D, None), (t_end + 900, 0, None)]
            sc = [(0, 1, None), (ts - 1000, 1, None), (ts, 1.12, ease), (t_end, 0.62, ease), (t_end + 800, 0, ease), (t_re, 0, None), (t_re + 2500, 1, ease)]
            anims["fx_rain_" + name] = multi({"translateY": ty, "scaleX": sc, "scaleY": sc})
            targets.append((name, "fx_rain_" + name))
            ty_trail = [(0, 0, None), (ts, 0, None)] + [(ts + a * dur, d, ease) for a, d in prof] + [(t_end + 3500, 1, None), (t_end + 3600, 0, None)]
            sx_trail = [(0, 1, None), (t_end, 1, None), (t_end + 3500, 0, ease)]
            anims["fx_rain_t" + idx] = multi({"scaleY": ty_trail, "scaleX": sx_trail})
            targets.append(("t" + idx, "fx_rain_t" + idx))
            tw = 0.5 + r * 0.22
            trails += (
                '    <group\n        android:name="t%s"\n        android:pivotX="%s"\n        android:pivotY="%s"\n        android:scaleY="0">\n'
                '        <path\n            android:pathData="M%s,%sL%s,%s"\n            android:strokeAlpha="0.28"\n            android:strokeColor="#FFFFFF"\n'
                '            android:strokeLineCap="round"\n            android:strokeWidth="%s" />\n    </group>\n'
            ) % (idx, n(x), n(y), n(x), n(y), n(x), n(y + D), n(tw))
        elif kind == "churn":
            t_hide, shrink, gap, grow = rng.uniform(2500, 36000), rng.uniform(1200, 2200), rng.uniform(1500, 9000), rng.uniform(2200, 3500)
            sc = [(0, 1, None), (t_hide, 1, None), (t_hide + shrink, 0, ease), (t_hide + shrink + gap, 0, None), (t_hide + shrink + gap + grow, 1, ease)]
            anims["fx_rain_" + name] = multi({"scaleX": sc, "scaleY": sc})
            targets.append((name, "fx_rain_" + name))
        elif kind == "wobble":
            targets.append((name, "fx_rain_wobble_" + "abc"[wob % 3]))
            wob += 1

    write("drawable", "fx_rain_vec", vector(body + trails + groups, gradient=True))
    write("drawable", "fx_rain", avd("rain", targets))
    for res, xml in anims.items():
        write("animator", res, xml)


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


def gen_flash():
    """Lớp chớp sáng và tia sét cho giông, phủ cùng lớp giọt mưa."""
    bolt = "M139,30L125.8,63L137.8,63L127,90L151,54L139,54Z"
    body = (
        '    <path\n        android:name="flash"\n        android:fillAlpha="0"\n        android:fillColor="#FFFFFF"\n'
        '        android:pathData="M0,0H240V120H0Z" />\n'
        '    <path\n        android:name="bolt"\n        android:fillAlpha="0"\n        android:fillColor="#FFF3A8"\n'
        '        android:pathData="%s" />\n' % bolt
    )
    events = [(3000, 1.0), (11500, 0.7), (12200, 1.0), (27000, 0.9), (38500, 0.6), (39100, 1.0), (52000, 0.8)]
    pattern = flash_pattern(events)
    write("drawable", "fx_flash_vec", vector(body))
    write("drawable", "fx_flash", avd("flash", ["flash", "bolt"]))
    write("animator", "fx_flash_flash", keyframes("fillAlpha", [(f, 0.4 * v) for f, v in pattern]))
    write("animator", "fx_flash_bolt", keyframes("fillAlpha", [(f, 0.85 * v) for f, v in pattern]))


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
    gen_flash()
    gen_snow()
    gen_clouds()
    gen_fog()
    gen_stars()
    gen_sun()
    print("xong")
