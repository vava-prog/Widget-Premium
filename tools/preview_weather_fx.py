#!/usr/bin/env python3
"""Xem trước hiệu ứng thời tiết trên máy (mô phỏng, KHÔNG phải bộ vẽ của Android).

Đọc fx_<tên>.xml (AnimatedVectorDrawable), vector, animator và nội suy trong app/src/main/res, tính giá trị
thuộc tính tại từng thời điểm rồi vẽ bằng SVG + Chromium (playwright). Dùng để kiểm tra hình dạng và chuyển động
trước khi build; hiệu năng và màu thật vẫn phải thử trên điện thoại.

  python3 tools/preview_weather_fx.py rain --times 0,6,12 --out /tmp/prev          # ảnh PNG
  python3 tools/preview_weather_fx.py rain --video /tmp/rain.mp4 --start 0 --end 20 --fps 20
"""
import argparse
import math
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
A = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"


def attr(el, name, default=None):
    return el.get(A + name, default)


def color(c, alpha=1.0):
    c = c.lstrip("#")
    if len(c) == 6:
        c = "FF" + c
    a, r, g, b = (int(c[i:i + 2], 16) for i in (0, 2, 4, 6))
    return "rgba(%d,%d,%d,%.3f)" % (r, g, b, a / 255 * alpha)


# ------------------------------------------------------------ nội suy
def interp(name, t):
    t = max(0.0, min(1.0, t))
    if name in (None, "linear"):
        return t
    if name == "accelerate_decelerate":
        return math.cos((t + 1) * math.pi) / 2 + 0.5
    if name == "accelerate_quad":
        return t * t
    if name == "decelerate_quad":
        return 1 - (1 - t) ** 2
    path = os.path.join(RES, "interpolator", name + ".xml")
    if os.path.isfile(path):
        root = ET.parse(path).getroot()
        tag = root.tag
        if tag == "accelerateInterpolator":
            return t ** (2 * float(attr(root, "factor", "1")))
        if tag == "decelerateInterpolator":
            return 1 - (1 - t) ** (2 * float(attr(root, "factor", "1")))
        if tag == "overshootInterpolator":
            k = float(attr(root, "tension", "2"))
            t -= 1
            return t * t * ((k + 1) * t + k) + 1
    raise ValueError("chưa hỗ trợ nội suy " + str(name))


def iname(ref):
    return None if ref is None else ref.split("/")[-1]


# ------------------------------------------------------------ animator
def load_animator(name):
    root = ET.parse(os.path.join(RES, "animator", name + ".xml")).getroot()
    items = []
    for oa in ([root] if root.tag == "objectAnimator" else root.findall("objectAnimator")):
        dur = int(attr(oa, "duration"))
        passes = int(attr(oa, "repeatCount", "0")) + 1
        base = iname(attr(oa, "interpolator"))
        holders = oa.findall("propertyValuesHolder")
        if holders:
            for h in holders:
                kfs = [(float(attr(k, "fraction")), float(attr(k, "value")), iname(attr(k, "interpolator"))) for k in h.findall("keyframe")]
                items.append(("kf", attr(h, "propertyName"), dur, base, kfs))
        else:
            items.append(("ft", attr(oa, "propertyName"), dur, base, (float(attr(oa, "valueFrom")), float(attr(oa, "valueTo")), passes, attr(oa, "repeatMode", "restart"))))
    return items


def value_at(item, t):
    kind, _, dur, base, data = item
    if kind == "kf":
        f = min(1.0, max(0.0, t / dur))
        f = interp(base, f)
        kfs = data
        if f <= kfs[0][0]:
            return kfs[0][1]
        for i in range(len(kfs) - 1):
            f0, v0, _ = kfs[i]
            f1, v1, ip = kfs[i + 1]
            if f <= f1:
                u = (f - f0) / (f1 - f0)
                return v0 + (v1 - v0) * interp(ip, u)
        return kfs[-1][1]
    a, b, passes, mode = data
    if t >= dur * passes:
        idx, fr = passes - 1, 1.0
    else:
        idx, fr = int(t // dur), (t % dur) / dur
    if mode == "reverse" and idx % 2 == 1:
        fr = 1 - fr
    return a + (b - a) * interp(base, fr)


# ------------------------------------------------------------ vector -> svg
class Scene:
    def __init__(self, layer):
        avd = ET.parse(os.path.join(RES, "drawable", "fx_%s.xml" % layer)).getroot()
        self.vec = ET.parse(os.path.join(RES, "drawable", "fx_%s_vec.xml" % layer)).getroot()
        self.anims = {}
        for tg in avd.findall("target"):
            self.anims[attr(tg, "name")] = load_animator(iname(attr(tg, "animation")))
        self.gid = 0

    def props(self, name, t):
        out = {}
        for item in self.anims.get(name, []):
            out[item[1]] = value_at(item, t)
        return out

    def gradient(self, g, defs):
        self.gid += 1
        gid = "g%d" % self.gid
        stops = [(0, attr(g, "startColor")), (0.5, attr(g, "centerColor")), (1, attr(g, "endColor"))]
        stops = "".join('<stop offset="%s" stop-color="%s"/>' % (o, color(c)) for o, c in stops if c)
        if attr(g, "type") == "radial":
            defs.append('<radialGradient id="%s" gradientUnits="userSpaceOnUse" cx="%s" cy="%s" r="%s">%s</radialGradient>' % (gid, attr(g, "centerX"), attr(g, "centerY"), attr(g, "gradientRadius"), stops))
        else:
            defs.append('<linearGradient id="%s" gradientUnits="userSpaceOnUse" x1="%s" y1="%s" x2="%s" y2="%s">%s</linearGradient>' % (gid, attr(g, "startX"), attr(g, "startY"), attr(g, "endX"), attr(g, "endY"), stops))
        return "url(#%s)" % gid

    def node(self, el, t, defs):
        out = ""
        for ch in el:
            if ch.tag == "group":
                name = attr(ch, "name")
                p = {"translateX": float(attr(ch, "translateX", 0)), "translateY": float(attr(ch, "translateY", 0)),
                     "scaleX": float(attr(ch, "scaleX", 1)), "scaleY": float(attr(ch, "scaleY", 1)), "rotation": float(attr(ch, "rotation", 0))}
                p.update(self.props(name, t))
                px, py = float(attr(ch, "pivotX", 0)), float(attr(ch, "pivotY", 0))
                tf = "translate(%.3f,%.3f) translate(%.3f,%.3f) rotate(%.3f) scale(%.4f,%.4f) translate(%.3f,%.3f)" % (
                    p["translateX"], p["translateY"], px, py, p["rotation"], p["scaleX"], p["scaleY"], -px, -py)
                out += '<g transform="%s">%s</g>' % (tf, self.node(ch, t, defs))
            elif ch.tag == "path":
                name = attr(ch, "name")
                p = {"fillAlpha": float(attr(ch, "fillAlpha", 1)), "strokeAlpha": float(attr(ch, "strokeAlpha", 1))}
                p.update(self.props(name, t))
                fill, stroke = "none", "none"
                fc = attr(ch, "fillColor")
                if fc:
                    fill = color(fc, p["fillAlpha"])
                else:
                    g = ch.find(AAPT + "attr/gradient")
                    if g is not None:
                        fill = self.gradient(g, defs)
                sc = attr(ch, "strokeColor")
                if sc:
                    stroke = color(sc, p["strokeAlpha"])
                join = attr(ch, "strokeLineJoin", "miter")
                cap = attr(ch, "strokeLineCap", "butt")
                out += '<path d="%s" fill="%s" stroke="%s" stroke-width="%s" stroke-linejoin="%s" stroke-linecap="%s"/>' % (
                    attr(ch, "pathData"), fill, stroke, attr(ch, "strokeWidth", 0), join, cap)
        return out

    def svg(self, t):
        defs = []
        body = self.node(self.vec, t, defs)
        return "<defs>%s</defs>%s" % ("".join(defs), body)


BACKDROP = (
    '<defs><linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#3b4658"/><stop offset="0.5" stop-color="#4d5c72"/><stop offset="1" stop-color="#63738a"/></linearGradient></defs>'
    '<rect width="240" height="120" rx="9" fill="url(#bg)"/>'
    '<text x="9" y="40" font-family="DejaVu Sans" font-size="28" fill="#fff">06:45</text>'
    '<text x="9" y="58" font-family="DejaVu Sans" font-size="9" fill="#e8edf5">Thứ Tư, 25 tháng 11</text>'
    '<text x="175" y="26" font-family="DejaVu Sans" font-size="20" fill="#fff">24°</text>'
    '<text x="12" y="108" font-family="DejaVu Sans" font-size="8" fill="#e8edf5">06:00    07:00    08:00    09:00    10:00    11:00</text>'
)


def frame_svg(scenes, t, scale):
    inner = "".join('<svg viewBox="0 0 240 120" width="240" height="120">%s</svg>' % s.svg(t) for s in scenes)
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 120" width="%d" height="%d">%s<g>%s</g></svg>'
            % (240 * scale, 120 * scale, BACKDROP, inner))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("layer", nargs="?", default="rain")
    ap.add_argument("--with", dest="extra", default="", help="lớp phủ thêm, ví dụ flash")
    ap.add_argument("--times", default="0", help="giây, cách nhau dấu phẩy")
    ap.add_argument("--out", default=".")
    ap.add_argument("--video")
    ap.add_argument("--start", type=float, default=0)
    ap.add_argument("--end", type=float, default=20)
    ap.add_argument("--fps", type=int, default=20)
    ap.add_argument("--scale", type=int, default=4)
    a = ap.parse_args()
    from playwright.sync_api import sync_playwright

    scenes = [Scene(a.layer)] + [Scene(x) for x in a.extra.split(",") if x]
    os.makedirs(a.out, exist_ok=True)
    with sync_playwright() as p:
        br = p.chromium.launch()
        pg = br.new_page(viewport={"width": 240 * a.scale, "height": 120 * a.scale})
        pg.set_content("<body style='margin:0;background:#222'><div id='c'></div></body>")

        def shot(t, path):
            pg.evaluate("s => { document.getElementById('c').innerHTML = s }", frame_svg(scenes, t * 1000, a.scale))
            pg.screenshot(path=path)

        if a.video:
            tmp = os.path.join(a.out, "frames")
            os.makedirs(tmp, exist_ok=True)
            n = int((a.end - a.start) * a.fps)
            for i in range(n):
                shot(a.start + i / a.fps, os.path.join(tmp, "f%05d.png" % i))
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(a.fps), "-i", os.path.join(tmp, "f%05d.png"),
                            "-c:v", "libx264", "-pix_fmt", "yuv420p", a.video], check=True)
            print("đã ghi", a.video)
        else:
            for ts in a.times.split(","):
                path = os.path.join(a.out, "%s_%s.png" % (a.layer, ts.replace(".", "_")))
                shot(float(ts), path)
                print(path)
        br.close()


if __name__ == "__main__":
    sys.exit(main())
