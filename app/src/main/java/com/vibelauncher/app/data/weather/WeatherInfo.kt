package com.vibelauncher.app.data.weather

import kotlin.math.roundToInt

data class WeatherInfo(
    val condition: String,
    val tempF: Int
)

/** The temperature in whichever unit Settings asks for. Weather is always fetched in °F and
 *  converted here at display time, so flipping the setting shows the other unit instantly
 *  instead of waiting on a refetch. */
fun WeatherInfo.displayTemp(useCelsius: Boolean): Int =
    if (useCelsius) ((tempF - 32) * 5.0 / 9.0).roundToInt() else tempF
