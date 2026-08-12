package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.ui.theme.SlateDarkBackground

@Composable
fun AppBackgroundWrapper(
    bgImageUri: String?,
    dimOpacity: Float = 0.45f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(SlateDarkBackground)) {
        if (!bgImageUri.isNullOrBlank()) {
            AsyncImage(
                model = bgImageUri,
                contentDescription = "Background Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimOpacity.coerceIn(0f, 0.9f)))
            )
        }
        content()
    }
}
