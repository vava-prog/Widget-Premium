#!/usr/bin/env python3
"""Xem trước TỔNG THỂ widget Lịch tuần (mô phỏng, KHÔNG phải bộ vẽ của Android): nền cửa kính ướt mưa (bản canvas 2D
của SkyArt.rainWindow trong tools/rain_window_preview.js), lớp hiệu ứng mưa (đọc từ res/ như preview_weather_fx.py),
và bố cục chữ theo widget_week.xml. Dùng để xem tỉ lệ và màu trước khi build; kích thước chữ, font và độ mờ thật phải
xem trên điện thoại.

  python3 tools/preview_widget_mock.py --times 0,1,2,3,4 --out /tmp/mock [--night] [--static] [--height 160]
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview_weather_fx as fx  # noqa: E402

PAGE = """<!doctype html><meta charset=utf-8>
<body style="margin:0;background:#1d232b"><div id=host style="padding:14px;display:inline-block">
<div id=w style="position:relative;width:%(W)dpx;height:%(H)dpx;border-radius:26px;overflow:hidden;font-family:Roboto,sans-serif;color:#fff">
<canvas id=bg width=%(BW)d height=%(BH)d style="position:absolute;left:0;top:0;width:100%%;height:100%%"></canvas>
<div id=fx style="position:absolute;left:8px;top:8px;right:8px;bottom:8px"></div>
<div style="position:absolute;inset:0;padding:8px 12px;display:flex;flex-direction:column;box-sizing:border-box;text-shadow:0 1px 3px rgba(0,0,0,.5)">
 <div style="display:flex;justify-content:space-between;align-items:flex-start">
  <div>
   <div style="font-size:40px;line-height:46px;font-weight:300">11:51<span style="font-size:16px;margin-left:3px;opacity:.8">%(SEC)s</span></div>
   <div style="font-size:14px;margin-top:3px;opacity:.9">Thứ Năm, 26 tháng 11</div>
   <div style="font-size:13px;margin-top:2px;display:flex;align-items:center;opacity:.85">
    <svg width=14 height=14 viewBox="0 0 24 24" style="margin-right:4px"><g fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5,5.5H19A2,2 0 0,1 21,7.5V19A2,2 0 0,1 19,21H5A2,2 0 0,1 3,19V7.5A2,2 0 0,1 5,5.5Z"/><path d="M3,10.5H21M8,2.8V7.2M16,2.8V7.2"/></g><path fill="#fff" d="M7,13.2h2v2h-2zM11,13.2h2v2h-2zM15,13.2h2v2h-2zM7,16.8h2v2h-2zM11,16.8h2v2h-2zM15,16.8h2v2h-2z"/></svg>Âm lịch: 18 tháng 10</div>
  </div>
  <div style="display:flex;flex-direction:column;align-items:flex-end">
   <div style="display:flex;align-items:flex-end"><span style="font-size:13px;margin-right:6px;opacity:.9">📍 Đà Nẵng</span><span style="font-size:%(TEMP)dpx;line-height:%(TEMPL)dpx">23°</span></div>
   <div style="margin-top:4px;display:flex;align-items:center;padding:3px 12px 3px 10px;border-radius:18px;border:1px solid rgba(255,255,255,.35);background:linear-gradient(rgba(255,255,255,.2),rgba(255,255,255,.1))">
    <span style="font-size:%(ICON)dpx;line-height:%(ICONL)dpx">🌧️</span>
    <div style="margin-left:8px"><div style="font-size:%(COND)dpx;font-weight:500">Mưa</div><div style="font-size:%(RANGE)dpx;margin-top:1px;opacity:.85">23°–24°</div></div>
   </div>
  </div>
 </div>
 <div style="flex:1"></div>
 <div style="margin-top:4px;display:flex;padding:5px 2px;border-radius:16px;border:1px solid rgba(255,255,255,.35);background:linear-gradient(rgba(255,255,255,.18),rgba(255,255,255,.08))">%(CELLS)s</div>
</div></div></div>
<script src="rain_window_preview.js"></script>
<script>
const k=%(BW)d/%(W)d, c=document.getElementById('bg').getContext('2d');
rainWindow(c,%(BW)d,%(BH)d,%(UNIT)s,1,%(NIGHT)s,%(ANIM)s);
glassRim(c,%(BW)d,%(BH)d,26*%(UNIT)s,%(UNIT)s,1);
</script>"""


def cells():
    hours = [("11:00", "🌧️", "23°"), ("12:00", "⛅", "23°"), ("13:00", "⛅", "23°"), ("14:00", "⛅", "23°"),
             ("15:00", "⛅", "23°"), ("16:00", "⛅", "23°"), ("17:00", "⛅", "23°")]
    out = ""
    for i, (h, ic, t) in enumerate(hours):
        if i:
            out += '<div style="width:1px;margin:9px 0;background:rgba(255,255,255,.18)"></div>'
        bg = "background:rgba(255,255,255,.25);border-radius:12px;" if i == 0 else ""
        out += ('<div style="flex:1;display:flex;flex-direction:column;align-items:center;%s"><div style="font-size:11px;opacity:.8">%s</div>'
                '<div style="font-size:19px;line-height:22px">%s</div><div style="font-size:12px">%s</div></div>') % (bg, h, ic, t)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--times", default="0,4")
    ap.add_argument("--out", default=".")
    ap.add_argument("--night", action="store_true")
    ap.add_argument("--static", action="store_true", help="không có lớp chuyển động (nền tự vẽ vệt nước)")
    ap.add_argument("--height", type=int, default=160)
    ap.add_argument("--width", type=int, default=322)
    ap.add_argument("--with", dest="extra", default="")
    a = ap.parse_args()
    from playwright.sync_api import sync_playwright

    tight = a.height < 150
    unit = 1.78
    page = PAGE % dict(
        W=a.width, H=a.height, BW=int(a.width * unit), BH=int(a.height * unit), UNIT=unit, NIGHT="true" if a.night else "false",
        ANIM="false" if a.static else "true", SEC="15", CELLS=cells(),
        TEMP=26 if tight else 32, TEMPL=30 if tight else 37, ICON=22 if tight else 28, ICONL=26 if tight else 33,
        COND=13 if tight else 15, RANGE=10 if tight else 12)
    open(os.path.join(HERE, "_mock.html"), "w", encoding="utf-8").write(page)
    os.makedirs(a.out, exist_ok=True)
    scenes = [fx.Scene("rain")] + [fx.Scene(x) for x in a.extra.split(",") if x]
    with sync_playwright() as p:
        br = p.chromium.launch()
        pg = br.new_page(viewport={"width": a.width + 28, "height": a.height + 28}, device_scale_factor=3)
        pg.goto("file://" + os.path.join(HERE, "_mock.html"))
        for ts in a.times.split(","):
            if not a.static:
                inner = "".join('<svg viewBox="0 0 240 120" preserveAspectRatio="none" width="100%%" height="100%%">%s</svg>' % s.svg(float(ts) * 1000) for s in scenes)
                pg.evaluate("s => { document.getElementById('fx').innerHTML = s }", inner)
            path = os.path.join(a.out, "mock_%s.png" % ts.replace(".", "_"))
            pg.locator("#host").screenshot(path=path)
            print(path)
        br.close()
    os.remove(os.path.join(HERE, "_mock.html"))


if __name__ == "__main__":
    main()
