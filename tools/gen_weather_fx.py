#!/usr/bin/env python3
"""Sinh tài nguyên hiệu ứng thời tiết bán động cho widget.

Chạy: python3 tools/gen_weather_fx.py   (ghi vào app/src/main/res/drawable và res/animator)

Mỗi hiệu ứng là một AnimatedVectorDrawable (fx_<tên>.xml) trỏ tới vector tĩnh (fx_<tên>_vec.xml)
và các animator (fx_<tên>_<đích>.xml). Mọi animator chạy đúng TOTAL ms (số vòng nguyên, kiểu
reverse thì số lượt chẵn) nên hết giờ trở lại đúng khung ban đầu và dừng. Có test kiểm tra điều này.

Để chuyển động bớt "cứng": nhiều lớp tốc độ khác nhau (không đồng bộ), nét mảnh và nhiều mức độ mờ.
Mưa là giọt nước bám trên kính (như kính xe ôtô), chạy MỘT LƯỢT ngắn RAIN_MS (4 giây) mỗi lần widget hiện ra rồi
đứng yên: hạt li ti; giọt hình giọt (đuôi nhọn) có sẵn và giọt mới ngưng tụ ngẫu nhiên nhưng trải đều; giọt lớn tuột xuống
nhanh dần như có trọng lực (có lúc khựng rồi tuột tiếp), để lại vệt nước mảnh. Khung cuối khác khung đầu (vệt nước ở lại,
như kính vừa ướt), nên khung đầu chỉ có giọt đứng yên. Sấm chớp (lớp fx_flash phủ lên lớp mưa) cũng chạy trong RAIN_MS.
Nội suy rơi/hãm/nảy nằm ở res/interpolator/fx_*. Các hiệu ứng còn lại chạy LONG_MS (1 phút) và trở về đúng khung đầu.
Vector 240x120, widget kéo giãn vừa khung; nội dung nằm gọn trong khung để không tràn mép bo góc.
"""
import math
import os
import random

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
LONG_MS = 60000
RAIN_MS = 4000
TOTAL = LONG_MS  # thời lượng của bộ hiệu ứng đang sinh; gen_rain và gen_flash tạm đổi sang RAIN_MS
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
    for kind in ("drawable", "animator", "interpolator"):
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

# Nội suy riêng (res/interpolator) để không phụ thuộc mức API: rơi nhanh dần (trọng lực), hãm lại, nảy khi hiện ra.
FALL, BRAKE, POP = "fx_fall", "fx_brake", "fx_pop"
EASE = "accelerate_decelerate"
DMAX = 80.0  # độ dài vệt nước vẽ sẵn: scaleY của vệt = quãng đường giọt đã đi / DMAX
XMIN, XMAX, YMIN, YMAX = 10, 230, 10, 110
PROPS = {"tx": "translateX", "ty": "translateY", "sx": "scaleX", "sy": "scaleY", "rot": "rotation"}


def interpolators():
    ns = 'xmlns:android="http://schemas.android.com/apk/res/android"'
    write("interpolator", FALL, HEADER + '<accelerateInterpolator %s\n    android:factor="1" />\n' % ns)
    write("interpolator", BRAKE, HEADER + '<decelerateInterpolator %s\n    android:factor="1" />\n' % ns)
    write("interpolator", POP, HEADER + '<overshootInterpolator %s\n    android:tension="3" />\n' % ns)


def iref(ip):
    return "@interpolator/%s" % ip if ip.startswith("fx_") else "@android:interpolator/%s" % ip


def multi(holders):
    """Animator keyframe nhiều thuộc tính trong một file. holders: {thuộc tính: [(ms, giá trị, nội suy|None)]}.
    Nội suy của một keyframe áp cho đoạn kết thúc tại keyframe đó."""
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
            ia = ('\n            android:interpolator="%s"' % iref(ip)) if ip else ""
            out += '        <keyframe\n            android:fraction="%s"\n            android:value="%s"%s />\n' % (nf(ms / TOTAL), n(v), ia)
        out += "    </propertyValuesHolder>\n"
    return out + "</objectAnimator>\n"


def linear(d, x0, y0, x1, y1, start, center, end, stroke=None, sw=0.25, join=False):
    st = ""
    if stroke:
        st = '\n            android:strokeColor="%s"\n            android:strokeWidth="%s"' % (stroke, n(sw))
        if join:
            st += '\n            android:strokeLineJoin="round"'
    return (
        '        <path\n            android:pathData="%s"%s>\n'
        '            <aapt:attr name="android:fillColor">\n'
        '                <gradient\n                    android:startX="%s"\n                    android:startY="%s"\n'
        '                    android:endX="%s"\n                    android:endY="%s"\n'
        '                    android:startColor="%s"\n                    android:centerColor="%s"\n                    android:endColor="%s"\n'
        '                    android:type="linear" />\n'
        '            </aapt:attr>\n        </path>\n'
    ) % (d, st, n(x0), n(y0), n(x1), n(y1), start, center, end)


def drop_path(cx, cy, r, a, tilt):
    """Giọt nước: thân tròn bán kính r, đuôi nhọn kéo lên phía trên cách tâm a (a > r), nghiêng tilt độ."""
    k = r / a
    px, py = r * math.sqrt(1 - k * k), -r * k  # tiếp điểm của hai tiếp tuyến kẻ từ đỉnh đuôi tới đường tròn
    t = math.radians(tilt)

    def rot(x, y):
        return cx + x * math.cos(t) - y * math.sin(t), cy + x * math.sin(t) + y * math.cos(t)

    tip, p1, p2 = rot(0, -a), rot(px, py), rot(-px, py)
    return "M%s,%sL%s,%sA%s,%s 0 1,1 %s,%sZ" % (n(tip[0]), n(tip[1]), n(p1[0]), n(p1[1]), n(r), n(r), n(p2[0]), n(p2[1]))


def drop_group(name, cx, cy, r, a, tilt, cls, hidden):
    """Giọt nước như thấu kính trên kính: đỉnh hơi tối (khúc xạ), đáy sáng, viền mảnh, điểm sáng ở góc trên trái.
    Đã vẽ sẵn ở dạng giọt có đuôi nhọn (không tròn); chuyển động do animator điều khiển."""
    hid = '\n        android:scaleX="0"\n        android:scaleY="0"' if hidden else ""
    out = '    <group\n        android:name="%s"\n        android:pivotX="%s"\n        android:pivotY="%s"%s>\n' % (name, n(cx), n(cy), hid)
    body = drop_path(cx, cy, r, a, tilt)
    out += linear(body, cx, cy - r, cx, cy + r, "#2E000000", "#0AFFFFFF", "#5CFFFFFF", "#73FFFFFF", 0.24, True)
    if cls != "S":
        out += solid(ellipse(cx + 0.02 * r, cy + 0.68 * r, 0.52 * r, 0.18 * r), "#38FFFFFF")
    out += solid(ellipse(cx - 0.38 * r, cy - 0.3 * r, 0.26 * r, 0.16 * r), "#EEFFFFFF")
    if cls == "L":
        out += solid(circle(cx + 0.4 * r, cy + 0.42 * r, 0.1 * r), "#80FFFFFF")
    return out + "    </group>\n"


def trail_group(name, bx, by, w0, rng):
    """Vệt nước giọt để lại: dải mảnh hơi uốn, hẹp dần về phía giọt. Vẽ sẵn dài DMAX, scaleY kéo dài theo giọt."""
    steps = 16
    phase, amp = rng.uniform(0, 6.28), rng.uniform(0.2, 0.55)
    left, right = [], []
    for i in range(steps + 1):
        f = i / steps
        w = w0 * (1.0 - 0.55 * f) * (1 + 0.22 * math.sin(i * 1.9 + phase))
        xc = bx + amp * math.sin(f * 6.5 + phase)
        left.append((xc - w, by + DMAX * f))
        right.append((xc + w, by + DMAX * f))
    pts = left + right[::-1]
    d = "M" + "L".join("%s,%s" % (n(x), n(y)) for x, y in pts) + "Z"
    return (
        '    <group\n        android:name="%s"\n        android:pivotX="%s"\n        android:pivotY="%s"\n        android:scaleX="0"\n        android:scaleY="0">\n'
        '        <path\n            android:fillColor="#2EFFFFFF"\n            android:pathData="%s"\n            android:strokeColor="#47FFFFFF"\n            android:strokeLineJoin="round"\n            android:strokeWidth="0.16" />\n    </group>\n'
    ) % (name, n(bx), n(by), d)


# ---- sắp xếp các "sự kiện giọt": giọt mới ngưng tụ ngẫu nhiên nhưng trải đều trên kính

def seg_dist(px, py, x0, y0, x1, y1):
    dx, dy = x1 - x0, y1 - y0
    l2 = dx * dx + dy * dy
    u = 0.0 if l2 == 0 else max(0.0, min(1.0, ((px - x0) * dx + (py - y0) * dy) / l2))
    return math.hypot(px - (x0 + u * dx), py - (y0 + u * dy))


def clearance(occs, x, y, r, t0, t1):
    """Khoảng trống tới các giọt tồn tại cùng lúc (trừ bán kính); càng lớn càng thoáng."""
    best = 1e9
    for x0, y0, x1, y1, ro, o0, o1 in occs:
        if o1 + 500 < t0 or t1 + 500 < o0:
            continue
        best = min(best, seg_dist(x, y, x0, y0, x1, y1) - (r + ro) * 1.1)
    return best


def pick(rng, occs, r, t0, t1, tries=30, ymax=YMAX):
    """Chọn chỗ thoáng nhất trong vài điểm ngẫu nhiên (best-candidate): phân bố đều mà vẫn ngẫu nhiên."""
    best, best_c = None, -1e9
    for _ in range(tries):
        x, y = rng.uniform(XMIN, XMAX), rng.uniform(YMIN, ymax)
        border = min(x - XMIN, XMAX - x, y - YMIN, YMAX - y)
        c = min(clearance(occs, x, y, r, t0, t1), 2 * border + 4)
        if c > best_c:
            best, best_c = (x, y), c
    return best


def free_run(occs, x, y, r, theta, t0, t1, limit):
    """Quãng đường tối đa giọt trượt được mà không đè lên giọt khác."""
    th = math.radians(theta)
    d = 0.0
    while d + 2 <= limit:
        if clearance(occs, x - (d + 2) * math.sin(th), y + (d + 2) * math.cos(th), r, t0, t1) < 1.0:
            break
        d += 2
    return d


def add(lst, t, v, ip=None):
    t = int(round(t))
    assert 0 <= t <= TOTAL, (t, TOTAL)
    assert not lst or t > lst[-1][0], (t, lst[-1])
    lst.append((t, v, ip))


# Số giọt: (lớp, có sẵn từ đầu, ngưng tụ thêm trong lúc chạy). Tổng 35 giọt, 18 giọt lớn/vừa có thể tuột.
SPEC = {"L": (5, 2), "M": (8, 4), "S": (10, 6)}
RADII = {"L": (3.4, 4.3), "M": (2.3, 3.0), "S": (1.3, 2.0)}
TAIL = {"L": (1.35, 2.1), "M": (1.3, 1.9), "S": (1.12, 1.55)}
LAST_RUN_END = RAIN_MS - 450  # giọt tuột xong và co lại (nảy) trước khi hết giờ


def make_sprites(rng):
    sprites = []
    for cls in ("L", "M", "S"):
        have, later = SPEC[cls]
        for i in range(have + later):
            r = rng.uniform(*RADII[cls])
            sprites.append({"cls": cls, "r": r, "a": r * rng.uniform(*TAIL[cls]), "tilt": rng.uniform(-14, 14),
                            "initial": i < have, "t_in": 0, "runs": [], "s": 1.0, "th": rng.uniform(-4, 4),
                            "base": None, "x1": None, "y1": None})
    return sprites


def plan_runs(rng, sp, x, y, occs, t_from):
    """Lên lịch các lần tuột của giọt: bắt đầu từ t_from trở đi, nhanh dần, có thể khựng rồi tuột tiếp."""
    cls, r, th = sp["cls"], sp["r"], sp["th"]
    sp["runs"], sp["D"] = [], 0.0
    if cls == "S" or (cls == "M" and rng.random() > 0.7):
        return
    ts = t_from + rng.uniform(0, 1500)
    limit = min(72.0, 113 - y - r)
    D = min(free_run(occs, x, y, r, th, ts - 100, TOTAL, limit), rng.uniform(26, 66))
    if D < 16:
        return
    n_runs = 1 if D < 34 else rng.choice([1, 2, 2])
    w = [rng.uniform(0.6, 1.4) for _ in range(n_runs)]
    g = rng.uniform(240, 420)  # gia tốc (đơn vị vector / s²): nhanh dần như rơi có trọng lực
    t, used = ts, 0.0
    for i in range(n_runs):
        d = D * w[i] / sum(w)
        ta = int(1000 * math.sqrt(2 * 0.8 * d / g))  # 80% quãng đường tăng tốc đều
        tb = max(60, int(ta * 0.25))  # 20% cuối hãm lại để dừng
        if t + ta + tb > LAST_RUN_END:
            break
        sp["runs"].append((int(t), d, ta, tb))
        used += d
        t += ta + tb + rng.uniform(380, 520)  # khựng một chút rồi tuột tiếp
    sp["D"] = used


def schedule(rng, sprites):
    occs = []
    order = {"L": 0, "M": 1, "S": 2}
    for sp in sorted((s for s in sprites if s["initial"]), key=lambda s: order[s["cls"]]):
        ymax = 70 if sp["cls"] != "S" else YMAX  # giọt có thể tuột thì đặt ở nửa trên để còn chỗ chạy
        x, y = pick(rng, occs, sp["r"], 0, TOTAL, ymax=ymax)
        sp["base"] = (x, y)
        plan_runs(rng, sp, x, y, occs, 300)
        sp["x1"], sp["y1"] = x - sp["D"] * math.sin(math.radians(sp["th"])), y + sp["D"] * math.cos(math.radians(sp["th"]))
        occs.append((x, y, sp["x1"], sp["y1"], sp["r"], 0, TOTAL))
    later = [s for s in sprites if not s["initial"]]
    rng.shuffle(later)
    slots = [200 + 2800 * (i + rng.random()) / len(later) for i in range(len(later))]  # giọt mới rải đều theo thời gian
    for sp, t_in in zip(later, slots):
        sp["t_in"] = int(t_in)
        sp["s"] = rng.uniform(0.9, 1.12)
        x, y = pick(rng, occs, sp["r"], sp["t_in"], TOTAL, ymax=70 if sp["cls"] != "S" else YMAX)
        sp["base"] = (x, y)
        plan_runs(rng, sp, x, y, occs, sp["t_in"] + 500)
        sp["x1"], sp["y1"] = x - sp["D"] * math.sin(math.radians(sp["th"])), y + sp["D"] * math.cos(math.radians(sp["th"]))
        occs.append((x, y, sp["x1"], sp["y1"], sp["r"] * sp["s"], sp["t_in"], TOTAL))


def timelines(sp):
    """Keyframe cho giọt (H) và vệt nước của nó (T). Giọt có sẵn đứng yên từ đầu; giọt mới hiện ra (nảy nhẹ) lúc t_in."""
    H = {k: [] for k in PROPS}
    T = {k: [] for k in PROPS}
    th, sn, cs_ = sp["th"], math.sin(math.radians(sp["th"])), math.cos(math.radians(sp["th"]))
    s, cur = sp["s"], sp["s"]
    if sp["initial"]:
        for k, v in (("tx", 0), ("ty", 0), ("rot", 0), ("sx", 1), ("sy", 1)):
            add(H[k], 0, v)
        cur = 1.0
    else:
        t_in = sp["t_in"]
        for k, v in (("tx", 0), ("ty", 0), ("rot", 0)):
            add(H[k], 0, v)
        add(H["sx"], 0, 0)
        add(H["sy"], 0, 0)
        add(H["sx"], t_in, 0)
        add(H["sy"], t_in, 0)
        add(H["sx"], t_in + 320, s, POP)  # giọt ngưng tụ: hiện ra, nảy nhẹ
        add(H["sy"], t_in + 320, s, POP)
    if not sp["runs"]:
        return H, T
    add(T["sx"], 0, 0)
    add(T["sy"], 0, 0)
    add(T["tx"], 0, 0)
    add(T["ty"], 0, 0)
    add(T["rot"], 0, th)
    cx_, cy_, f = 0.0, 0.0, 0.0
    for i, (ts, d, ta, tb) in enumerate(sp["runs"]):
        nxt = max(0.62 * s, cur * 0.9)  # trượt thì giọt nhỏ dần vì để lại nước trên vệt
        pre = 200 if i == 0 else 120
        add(H["sx"], ts - pre, cur)  # chuẩn bị: giọt dãn dài theo chiều rơi
        add(H["sy"], ts - pre, cur)
        add(H["sx"], ts, 0.9 * cur, EASE)
        add(H["sy"], ts, 1.22 * cur, EASE)
        if i == 0:
            add(H["rot"], ts - 200, 0)
            add(H["rot"], ts, th, EASE)
            add(T["sx"], ts - 100, 0)
            add(T["sx"], ts, s)
        add(H["tx"], ts, cx_)
        add(H["ty"], ts, cy_)
        add(H["tx"], ts + ta, cx_ - 0.8 * d * sn, FALL)  # tăng tốc đều: quãng đường ~ t²
        add(H["ty"], ts + ta, cy_ + 0.8 * d * cs_, FALL)
        add(H["tx"], ts + ta + tb, cx_ - d * sn, BRAKE)  # hãm lại
        add(H["ty"], ts + ta + tb, cy_ + d * cs_, BRAKE)
        add(T["sy"], ts, f)
        add(T["sy"], ts + ta, f + 0.8 * d / DMAX, FALL)
        add(T["sy"], ts + ta + tb, f + d / DMAX, BRAKE)
        end = ts + ta + tb
        add(H["sx"], end, 0.9 * nxt)
        add(H["sy"], end, 1.22 * nxt)
        last = i == len(sp["runs"]) - 1
        add(H["sx"], end + (320 if last else 200), nxt, POP if last else EASE)  # dừng: co lại (cuối cùng thì nảy nhẹ)
        add(H["sy"], end + (320 if last else 200), nxt, POP if last else EASE)
        cur, cx_, cy_, f = nxt, cx_ - d * sn, cy_ + d * cs_, f + d / DMAX
    return H, T


def gen_rain():
    global TOTAL
    TOTAL = RAIN_MS
    rng = random.Random(23)
    interpolators()
    sprites = make_sprites(rng)
    schedule(rng, sprites)

    # Hạt nước li ti bám dày (nền Canvas còn vẽ thêm giọt li ti): gộp vào một path để nhẹ.
    beads, glints = [], []
    for _ in range(40):
        x, y, r = rng.uniform(6, 234), rng.uniform(6, 114), rng.uniform(0.5, 1.0)
        beads.append(circle(x, y, r))
        glints.append(circle(x - 0.35 * r, y - 0.4 * r, 0.28))
    body = (
        '    <path\n        android:fillColor="#1FFFFFFF"\n        android:pathData="%s"\n        android:strokeColor="#47FFFFFF"\n        android:strokeWidth="0.2" />\n' % "".join(beads)
        + '    <path\n        android:name="glint"\n        android:fillAlpha="0.55"\n        android:fillColor="#FFFFFF"\n        android:pathData="%s" />\n' % "".join(glints)
    )
    targets = [("glint", "fx_rain_glint")]
    anims = {"fx_rain_glint": loop("fillAlpha", 0.55, 1.0, 1000, 4, "accelerate_decelerate", True)}

    trails, groups, peak = "", "", 0.0
    for idx, sp in enumerate(sprites, 1):
        sp["name"] = "d%d" % idx
        H, T = timelines(sp)
        bx, by = sp["base"]
        keep = ("tx", "ty", "sx", "sy", "rot") if sp["cls"] != "S" else ("tx", "ty", "sx", "sy")
        anims["fx_rain_d%d" % idx] = multi({PROPS[k]: H[k] for k in keep})
        targets.append(("d%d" % idx, "fx_rain_d%d" % idx))
        for k in ("sx", "sy"):
            peak = max([peak] + [v for _, v, _ in H[k]])
        if T["sy"]:
            anims["fx_rain_t%d" % idx] = multi({PROPS[k]: T[k] for k in PROPS})
            targets.append(("t%d" % idx, "fx_rain_t%d" % idx))
            trails += trail_group("t%d" % idx, bx, by, 0.17 * sp["r"] + 0.14, rng)
        sp["body"] = drop_group("d%d" % idx, bx, by, sp["r"], sp["a"], sp["tilt"], sp["cls"], not sp["initial"])
    assert peak <= 1.6, "giọt phồng quá lớn: %.2f" % peak
    for sp in sorted(sprites, key=lambda s: s["r"]):
        groups += sp["body"]

    write("drawable", "fx_rain_vec", vector(body + trails + groups, gradient=True))
    write("drawable", "fx_rain", avd("rain", targets))
    for res, xml in anims.items():
        write("animator", res, xml)
    slides = sum(1 for sp in sprites if sp["runs"])
    print("mưa: %d giọt (%d ngưng tụ thêm), %d giọt tuột để lại vệt, %d tệp animator, %d giây" % (
        len(sprites), sum(1 for sp in sprites if not sp["initial"]), slides, len(anims), TOTAL // 1000))
    TOTAL = LONG_MS


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
    """Lớp chớp sáng và tia sét cho giông, phủ cùng lớp giọt mưa; chạy RAIN_MS như lớp mưa."""
    global TOTAL
    TOTAL = RAIN_MS
    bolt = "M139,30L125.8,63L137.8,63L127,90L151,54L139,54Z"
    body = (
        '    <path\n        android:name="flash"\n        android:fillAlpha="0"\n        android:fillColor="#FFFFFF"\n'
        '        android:pathData="M0,0H240V120H0Z" />\n'
        '    <path\n        android:name="bolt"\n        android:fillAlpha="0"\n        android:fillColor="#FFF3A8"\n'
        '        android:pathData="%s" />\n' % bolt
    )
    events = [(600, 1.0), (1500, 0.7), (2300, 1.0), (3300, 0.8)]
    pattern = flash_pattern(events)
    write("drawable", "fx_flash_vec", vector(body))
    write("drawable", "fx_flash", avd("flash", ["flash", "bolt"]))
    write("animator", "fx_flash_flash", keyframes("fillAlpha", [(f, 0.4 * v) for f, v in pattern]))
    write("animator", "fx_flash_bolt", keyframes("fillAlpha", [(f, 0.85 * v) for f, v in pattern]))
    TOTAL = LONG_MS


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
