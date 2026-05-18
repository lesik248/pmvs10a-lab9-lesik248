package com.example.weather.platform

enum class PlatformType { Android, Ios, Desktop, Web }

interface Platform {
    val type: PlatformType
    val name: String
}

expect fun currentPlatform(): Platform
