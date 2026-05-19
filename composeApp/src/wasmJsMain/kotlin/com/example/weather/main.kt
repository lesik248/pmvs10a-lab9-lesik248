package com.example.weather

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    document.getElementById("loader")?.remove()
    ComposeViewport(document.body!!) {
        App()
    }
}
