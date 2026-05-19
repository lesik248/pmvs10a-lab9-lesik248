package com.example.weather.platform

import android.os.Build

private object AndroidPlatform : Platform {
    override val type: PlatformType = PlatformType.Android
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun currentPlatform(): Platform = AndroidPlatform
