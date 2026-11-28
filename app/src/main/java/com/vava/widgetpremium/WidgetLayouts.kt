package com.vava.widgetpremium

/** Sinh tự động bởi tools/gen_font_layouts.py, không sửa tay. Bảng tra layout theo phông và độ đậm. */
object WidgetLayouts {
    fun clock(family: FontFamilyChoice, weight: FontWeightChoice): Int = when (family) {
        FontFamilyChoice.DEFAULT -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_clock_def_l
            FontWeightChoice.NORMAL -> R.layout.widget_clock
            FontWeightChoice.BOLD -> R.layout.widget_clock_def_b
        }
        FontFamilyChoice.SANS -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_clock_sans_l
            FontWeightChoice.NORMAL -> R.layout.widget_clock_sans_n
            FontWeightChoice.BOLD -> R.layout.widget_clock_sans_b
        }
        FontFamilyChoice.SERIF -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_clock_serif_l
            FontWeightChoice.NORMAL -> R.layout.widget_clock_serif_n
            FontWeightChoice.BOLD -> R.layout.widget_clock_serif_b
        }
        FontFamilyChoice.MONO -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_clock_mono_l
            FontWeightChoice.NORMAL -> R.layout.widget_clock_mono_n
            FontWeightChoice.BOLD -> R.layout.widget_clock_mono_b
        }
        FontFamilyChoice.ROUNDED -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_clock_rnd_l
            FontWeightChoice.NORMAL -> R.layout.widget_clock_rnd_n
            FontWeightChoice.BOLD -> R.layout.widget_clock_rnd_b
        }
    }

    fun info(family: FontFamilyChoice, weight: FontWeightChoice): Int = when (family) {
        FontFamilyChoice.DEFAULT -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_info_def_l
            FontWeightChoice.NORMAL -> R.layout.widget_info
            FontWeightChoice.BOLD -> R.layout.widget_info_def_b
        }
        FontFamilyChoice.SANS -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_info_sans_l
            FontWeightChoice.NORMAL -> R.layout.widget_info_sans_n
            FontWeightChoice.BOLD -> R.layout.widget_info_sans_b
        }
        FontFamilyChoice.SERIF -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_info_serif_l
            FontWeightChoice.NORMAL -> R.layout.widget_info_serif_n
            FontWeightChoice.BOLD -> R.layout.widget_info_serif_b
        }
        FontFamilyChoice.MONO -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_info_mono_l
            FontWeightChoice.NORMAL -> R.layout.widget_info_mono_n
            FontWeightChoice.BOLD -> R.layout.widget_info_mono_b
        }
        FontFamilyChoice.ROUNDED -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_info_rnd_l
            FontWeightChoice.NORMAL -> R.layout.widget_info_rnd_n
            FontWeightChoice.BOLD -> R.layout.widget_info_rnd_b
        }
    }

    fun week(family: FontFamilyChoice, weight: FontWeightChoice): Int = when (family) {
        FontFamilyChoice.DEFAULT -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_week_def_l
            FontWeightChoice.NORMAL -> R.layout.widget_week
            FontWeightChoice.BOLD -> R.layout.widget_week_def_b
        }
        FontFamilyChoice.SANS -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_week_sans_l
            FontWeightChoice.NORMAL -> R.layout.widget_week_sans_n
            FontWeightChoice.BOLD -> R.layout.widget_week_sans_b
        }
        FontFamilyChoice.SERIF -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_week_serif_l
            FontWeightChoice.NORMAL -> R.layout.widget_week_serif_n
            FontWeightChoice.BOLD -> R.layout.widget_week_serif_b
        }
        FontFamilyChoice.MONO -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_week_mono_l
            FontWeightChoice.NORMAL -> R.layout.widget_week_mono_n
            FontWeightChoice.BOLD -> R.layout.widget_week_mono_b
        }
        FontFamilyChoice.ROUNDED -> when (weight) {
            FontWeightChoice.LIGHT -> R.layout.widget_week_rnd_l
            FontWeightChoice.NORMAL -> R.layout.widget_week_rnd_n
            FontWeightChoice.BOLD -> R.layout.widget_week_rnd_b
        }
    }
}
