#!/usr/bin/env python3
"""Sinh các layout widget theo phông và độ đậm từ ba layout gốc (widget_clock/info/week.xml), và bảng tra WidgetLayouts.kt.
Chạy: python3 tools/gen_font_layouts.py. Layout gốc = phông hệ thống + đậm thường; sửa layout gốc rồi chạy lại.
RemoteViews không đổi được phông khi chạy nên mỗi tổ hợp phông x độ đậm là một layout riêng."""
import os
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main")
LAY = os.path.join(ROOT, "res", "layout")
KT = os.path.join(ROOT, "java", "com", "vava", "widgetpremium", "WidgetLayouts.kt")
FAMILIES = [("DEFAULT", "def", None), ("SANS", "sans", "sans-serif"), ("SERIF", "serif", "serif"),
            ("MONO", "mono", "monospace"), ("ROUNDED", "rnd", "casual")]
WEIGHTS = [("LIGHT", "l"), ("NORMAL", "n"), ("BOLD", "b")]
BASES = ["clock", "info", "week"]
TEXT = re.compile(r"<(TextView|TextClock)\b(.*?)(/?)>", re.S)


def variant(src, family, weight):
    fam = dict((f[0], f[2]) for f in FAMILIES)[family]

    def fix(m):
        attrs = m.group(2)
        attrs = re.sub(r'\s*android:fontFamily="[^"]*"', "", attrs)
        face = fam
        if weight == "LIGHT" and family in ("DEFAULT", "SANS"):
            face = "sans-serif-light"
        if weight == "BOLD":
            attrs = re.sub(r'\s*android:textStyle="[^"]*"', "", attrs)
            attrs += '\n        android:textStyle="bold"'
        if face:
            attrs += '\n        android:fontFamily="%s"' % face
        return "<%s%s%s>" % (m.group(1), attrs, m.group(3))

    return TEXT.sub(fix, src)


def main():
    rows = []
    for base in BASES:
        src = open(os.path.join(LAY, "widget_%s.xml" % base), encoding="utf-8").read()
        for family, fcode, _ in FAMILIES:
            for weight, wcode in WEIGHTS:
                name = "widget_%s" % base if (family == "DEFAULT" and weight == "NORMAL") else "widget_%s_%s_%s" % (base, fcode, wcode)
                if name != "widget_%s" % base:
                    out = variant(src, family, weight)
                    out = out.replace("<!-- gen -->", "")
                    with open(os.path.join(LAY, name + ".xml"), "w", encoding="utf-8") as f:
                        f.write(out)
                rows.append((base, family, weight, name))
    lines = ["package com.vava.widgetpremium", "",
             "/** Sinh tự động bởi tools/gen_font_layouts.py, không sửa tay. Bảng tra layout theo phông và độ đậm. */",
             "object WidgetLayouts {"]
    for base in BASES:
        lines.append("    fun %s(family: FontFamilyChoice, weight: FontWeightChoice): Int = when (family) {" % base)
        for family, _, _ in FAMILIES:
            lines.append("        FontFamilyChoice.%s -> when (weight) {" % family)
            for weight, _ in WEIGHTS:
                name = [r[3] for r in rows if r[0] == base and r[1] == family and r[2] == weight][0]
                lines.append("            FontWeightChoice.%s -> R.layout.%s" % (weight, name))
            lines.append("        }")
        lines.append("    }")
        lines.append("")
    lines[-1] = "}"
    open(KT, "w", encoding="utf-8").write("\n".join(lines) + "\n")
    print("đã sinh %d layout" % (len(rows) - len(BASES)))


if __name__ == "__main__":
    main()
