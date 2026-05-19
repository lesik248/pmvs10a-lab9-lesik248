package com.example.weather.platform

private object WebPlatform : Platform {
    override val type: PlatformType = PlatformType.Web
    override val name: String = "Web (Wasm)"
}

actual fun currentPlatform(): Platform = WebPlatform
