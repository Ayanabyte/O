package com.ayanabyte.one.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayanabyte.one.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Sky = Color(0xFF4DDCF5)
private val Subtitle = Color(0xFF999999)

/**
 * Pure vector animation; no video decoder and no bundled sound.
 * Sound design will be integrated only after approval of the analog-tuning effect.
 */
@Composable
fun BrandIntro(onFinished: () -> Unit) {
    val fusion = remember { Animatable(0f) }
    var showSlogans by remember { mutableStateOf(false) }
    val sloganOpacity by animateFloatAsState(
        targetValue = if (showSlogans) 1f else 0f,
        animationSpec = tween(1100),
        label = "tagline opacity"
    )

    LaunchedEffect(Unit) {
        delay(1200)
        fusion.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3600, easing = FastOutSlowInEasing)
        )
        showSlogans = true
        delay(3000)
        onFinished()
    }

    BackHandler(onBack = onFinished)

    Box(
        modifier = Modifier.fillMaxSize().background(Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 900.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoSymbol(fusion.value)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.brand_full),
                color = White,
                fontSize = 19.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(((fusion.value - 0.58f) / 0.42f).coerceIn(0f, 1f))
            )
            Spacer(Modifier.height(54.dp))
            Text(
                text = stringResource(R.string.tagline_en),
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(sloganOpacity)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.tagline_es),
                color = Subtitle,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(sloganOpacity)
            )
        }
    }
}

@Composable
private fun LogoSymbol(progress: Float) {
    Canvas(modifier = Modifier.size(width = 260.dp, height = 190.dp)) {
        val sx = size.width / 260f
        val sy = size.height / 190f

        // Ring moves to the centre while the exclamation mark dissolves.
        val ringX = (100f + 30f * progress) * sx
        drawCircle(
            color = White,
            radius = 48f * sx,
            center = Offset(ringX, 77f * sy),
            style = Stroke(width = 21f * sx)
        )

        val barOpacity = (1f - progress * 2.15f).coerceIn(0f, 1f)
        drawRect(
            color = White.copy(alpha = barOpacity),
            topLeft = Offset(162f * sx, 22f * sy),
            size = Size(16f * sx, 68f * sy)
        )

        // The cyan dot travels slowly from the right to directly below O.
        val dotX = (170f - 40f * progress + 9f * sin(progress * PI).toFloat()) * sx
        val dotY = (115f + 40f * progress) * sy
        drawCircle(
            color = Sky,
            radius = 10f * sx,
            center = Offset(dotX, dotY)
        )
    }
}
