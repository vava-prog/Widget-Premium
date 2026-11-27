#!/usr/bin/env python3
"""Tạo hai tệp SharedPreferences (weather.xml, style.xml) để máy ảo chạy đúng trạng thái người dùng: thành phố đã chọn,
dự báo đã lưu theo mã thời tiết WMO cho trước, giao diện Theo thời tiết, widget Lịch tuần ở chế độ theo giờ.
Dùng: seed.py <thư mục ra> <mã WMO> <AUTO|DAY|NIGHT>"""
import datetime
import os
import struct
import sys
import time
from xml.sax.saxutils import escape, quoteattr

out, code, mode = sys.argv[1], int(sys.argv[2]), sys.argv[3]
os.makedirs(out, exist_ok=True)


def bits(x):
    return struct.unpack("<q", struct.pack("<d", x))[0]


now = datetime.datetime.now(datetime.timezone.utc).replace(minute=0, second=0, microsecond=0)
hours = [now + datetime.timedelta(hours=i) for i in range(-1, 47)]
days = [now.date() + datetime.timedelta(days=i) for i in range(7)]
q = lambda s: '"%s"' % s
forecast = (
    '{"utc_offset_seconds":0,'
    '"current":{"temperature_2m":23.0,"relative_humidity_2m":90,"weather_code":%d,"apparent_temperature":24.5},'
    '"daily":{"time":[%s],"weather_code":[%s],"temperature_2m_max":[%s],"temperature_2m_min":[%s],'
    '"sunrise":[%s],"sunset":[%s],"precipitation_probability_max":[%s]},'
    '"hourly":{"time":[%s],"temperature_2m":[%s],"weather_code":[%s]}}'
) % (
    code,
    ",".join(q(d.isoformat()) for d in days),
    ",".join([str(code)] * 7), ",".join(["24.0"] * 7), ",".join(["22.0"] * 7),
    ",".join(q(d.isoformat() + "T06:00") for d in days),
    ",".join(q(d.isoformat() + "T18:00") for d in days),
    ",".join(["80"] * 7),
    ",".join(q(h.strftime("%Y-%m-%dT%H:%M")) for h in hours),
    ",".join(["23.0"] * len(hours)),
    ",".join([str(code)] * len(hours)),
)
fetched = int(time.time() * 1000)
weather = f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="name">Đà Nẵng</string>
    <string name="admin"></string>
    <string name="country">Việt Nam</string>
    <long name="lat" value="{bits(16.0678)}" />
    <long name="lon" value="{bits(108.2208)}" />
    <string name="forecast">{escape(forecast)}</string>
    <long name="fetched_at" value="{fetched}" />
</map>
"""
style = f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="theme">WEATHER</string>
    <string name="week_mode">HOURLY</string>
    <string name="mode">{mode}</string>
    <boolean name="weather_motion" value="true" />
</map>
"""
open(os.path.join(out, "weather.xml"), "w", encoding="utf-8").write(weather)
open(os.path.join(out, "style.xml"), "w", encoding="utf-8").write(style)
