package com.example.weather.platform

private object DesktopPlatform : Platform {
    override val type: PlatformType = PlatformType.Desktop
    override val name: String = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
}

actual fun currentPlatform(): Platform = DesktopPlatform
