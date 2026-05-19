package com.example.weather.platform

import platform.UIKit.UIDevice

private object IosPlatform : Platform {
    override val type: PlatformType = PlatformType.Ios
    override val name: String =
        "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"
}

actual fun currentPlatform(): Platform = IosPlatform
