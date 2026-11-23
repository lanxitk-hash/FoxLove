package com.example.foxlove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 9, 24)
        window.navigationBarColor = android.graphics.Color.rgb(5, 9, 24)
        setContent {
            MaterialTheme {
                FoxLoveScene()
            }
        }
    }
}

private data class Snowflake(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,
    val drift: Float,
    val alpha: Float
)

@Composable
private fun FoxLoveScene() {
    var elapsed by remember { mutableFloatStateOf(0f) }
    var restartKey by remember { mutableIntStateOf(0) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(restartKey) {
        elapsed = 0f
        while (true) {
            delay(16)
            elapsed += 0.016f
        }
    }

    val snow = remember(restartKey) {
        List(190) {
            Snowflake(
                Random.nextFloat(),
                Random.nextFloat(),
                Random.nextFloat() * 2.1f + 0.5f,
                Random.nextFloat() * 0.23f + 0.10f,
                Random.nextFloat() * 0.12f - 0.06f,
                Random.nextFloat() * 0.55f + 0.25f
            )
        }
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF070C1D))
            .pointerInput(Unit) {
                detectTapGestures { restartKey++ }
            }
    ) {
        val w = size.width
        val h = size.height
        val t = elapsed
        val groundY = h * 0.79f

        drawNightSky(w, h, t)
        drawMoon(w * 0.77f, h * 0.17f, min(w, h) * 0.061f)
        drawStars(w, h, t)
        drawForest(w, h, groundY)
        drawSnow(snow, w, h, t)
        drawGround(w, h, groundY)

        // Story timeline: searching -> discovers -> sniffs -> stands -> message.
        val searchEnd = 8.0f
        val discoverEnd = 10.2f
        val sniffEnd = 13.0f
        val standEnd = 15.0f
        val loopLength = 23.0f
        val phaseTime = t % loopLength

        val foxX = when {
            phaseTime < searchEnd -> {
                val p = phaseTime / searchEnd
                w * (0.17f + 0.55f * p)
            }
            else -> w * 0.64f
        }

        val foxY = groundY - h * 0.035f
        val running = phaseTime < searchEnd
        val found = phaseTime >= searchEnd
        val sniffing = phaseTime in discoverEnd..sniffEnd
        val standing = phaseTime >= standEnd
        val foxBob = if (running) sin(t * 13f) * h * 0.012f else sin(t * 2f) * 2f

        // Tiny pawprints left behind while the fox searches.
        if (running) {
            for (i in 0..5) {
                val px = foxX - i * w * 0.055f
                drawCircle(
                    Color.White.copy(alpha = 0.12f),
                    radius = w * 0.006f,
                    center = Offset(px, groundY + h * 0.018f)
                )
            }
        }

        // Heart-shaped stone appears when the fox reaches it.
        val heartX = w * 0.69f
        val heartY = groundY - h * 0.025f
        if (found) {
            val reveal = ((phaseTime - searchEnd) / 0.8f).coerceIn(0f, 1f)
            drawHeartStone(heartX, heartY, w * 0.034f * reveal, t)
        }

        drawFox(
            center = Offset(foxX, foxY + foxBob),
            scale = min(w, h) * 0.0025f,
            time = t,
            running = running,
            sniffing = sniffing,
            standing = standing,
            lookingAtHeart = found
        )

        // Warm pool of light around the little heart.
        if (found) {
            val pulse = 0.5f + 0.5f * sin(t * 3f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0x55FF668A).copy(alpha = 0.18f + pulse * 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(heartX, heartY),
                    radius = w * 0.18f
                ),
                radius = w * 0.18f,
                center = Offset(heartX, heartY)
            )
        }

        // Final pose and glowing love message.
        if (standing) {
            val fade = ((phaseTime - standEnd) / 1.4f).coerceIn(0f, 1f)
            val alpha = fade * (0.88f + 0.12f * sin(t * 2.2f))
            val title = "Я тебя люблю"
            val measured = textMeasurer.measure(
                title,
                style = TextStyle(
                    color = Color(0xFFFFD9E4).copy(alpha = alpha),
                    fontSize = (w * 0.085f).sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            val textX = (w - measured.size.width) / 2f
            val textY = h * 0.25f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x55FF739A), Color.Transparent),
                    center = Offset(w * 0.5f, textY + measured.size.height / 2f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.5f, textY + measured.size.height / 2f)
            )

            drawText(
                measured,
                topLeft = Offset(textX, textY),
                shadow = Shadow(
                    Color(0xFFFF477A).copy(alpha = alpha),
                    Offset.Zero,
                    22f
                )
            )

            val subtitle = "♡  навсегда  ♡"
            val sub = textMeasurer.measure(
                subtitle,
                style = TextStyle(
                    color = Color(0xFFFFEAF0).copy(alpha = alpha * 0.8f),
                    fontSize = (w * 0.035f).sp,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center
                )
            )
            drawText(
                sub,
                topLeft = Offset((w - sub.size.width) / 2f, textY + measured.size.height + 12f)
            )
        }

        // Atmospheric vignette.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x99020813)),
                center = Offset(w * 0.5f, h * 0.48f),
                radius = max(w, h) * 0.78f
            )
        )
    }
}

private fun DrawScope.drawNightSky(w: Float, h: Float, t: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF030612), Color(0xFF101B35), Color(0xFF34445C)),
            endY = h
        )
    )
}

private fun DrawScope.drawMoon(x: Float, y: Float, r: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0x44DCE8FF), Color(0x00DCE8FF)),
            center = Offset(x, y),
            radius = r * 4f
        ),
        radius = r * 4f,
        center = Offset(x, y)
    )
    drawCircle(Color(0xFFE7EDFF), r, Offset(x, y))
    drawCircle(Color(0xFFC8D5EE), r * 0.82f, Offset(x + r * 0.34f, y - r * 0.13f))
    drawCircle(Color(0xFFE7EDFF), r * 0.78f, Offset(x - r * 0.08f, y + r * 0.03f))
}

private fun DrawScope.drawStars(w: Float, h: Float, t: Float) {
    val random = Random(21)
    repeat(115) {
        val x = random.nextFloat() * w
        val y = random.nextFloat() * h * 0.67f
        val r = random.nextFloat() * 1.7f + 0.35f
        val a = (0.35f + 0.35f * sin(t * 1.6f + it)).coerceIn(0.05f, 0.85f)
        drawCircle(Color.White.copy(alpha = a), r, Offset(x, y))
    }
}

private fun DrawScope.drawForest(w: Float, h: Float, groundY: Float) {
    val random = Random(44)
    for (i in 0 until 17) {
        val x = i * w / 15f - w * 0.04f
        val base = groundY + h * 0.01f
        val height = h * (0.16f + random.nextFloat() * 0.17f)
        val width = height * 0.42f
        val path = Path().apply {
            moveTo(x, base - height)
            lineTo(x - width * 0.22f, base - height * 0.57f)
            lineTo(x - width * 0.08f, base - height * 0.59f)
            lineTo(x - width * 0.48f, base - height * 0.23f)
            lineTo(x - width * 0.18f, base - height * 0.28f)
            lineTo(x - width * 0.58f, base)
            lineTo(x + width * 0.58f, base)
            lineTo(x + width * 0.18f, base - height * 0.28f)
            lineTo(x + width * 0.48f, base - height * 0.23f)
            lineTo(x + width * 0.08f, base - height * 0.59f)
            lineTo(x + width * 0.22f, base - height * 0.57f)
            close()
        }
        drawPath(path, Color(0xFF101D2B))
        drawLine(
            Color(0x668EA4BE),
            Offset(x, base - height * 0.92f),
            Offset(x - width * 0.25f, base - height * 0.28f),
            strokeWidth = 1.4f
        )
    }
}

private fun DrawScope.drawGround(w: Float, h: Float, y: Float) {
    val path = Path().apply {
        moveTo(0f, y)
        cubicTo(w * .24f, y - h * .07f, w * .43f, y + h * .035f, w * .63f, y)
        cubicTo(w * .79f, y - h * .045f, w * .9f, y + h * .02f, w, y - h * .015f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFFE4ECF6), Color(0xFFB9CBE0)),
            startY = y,
            endY = h
        )
    )
    repeat(75) {
        val x = (it * 97f % w)
        val yy = y + h * 0.05f + (it * 31f % (h * 0.17f))
        drawCircle(Color.White.copy(alpha = 0.35f), 1.2f, Offset(x, yy))
    }
}

private fun DrawScope.drawSnow(
    flakes: List<Snowflake>,
    w: Float,
    h: Float,
    t: Float
) {
    flakes.forEach { f ->
        val y = (f.y + t * f.speed) % 1.08f * h
        val x = ((f.x + sin(t * 0.7f + f.y * 15f) * f.drift + 1f) % 1f) * w
        drawCircle(
            Color(0xFFEAF3FF).copy(alpha = f.alpha),
            f.radius,
            Offset(x, y)
        )
    }
}

private fun DrawScope.drawHeartStone(x: Float, y: Float, size: Float, t: Float) {
    if (size <= 0f) return
    val pulse = 1f + 0.055f * sin(t * 3.4f)
    val s = size * pulse
    val path = Path().apply {
        moveTo(x, y + s * 0.72f)
        cubicTo(x - s * 0.28f, y + s * 0.43f, x - s * 1.02f, y - s * 0.05f, x - s * 0.72f, y - s * 0.55f)
        cubicTo(x - s * 0.48f, y - s * 0.98f, x - s * 0.08f, y - s * 0.77f, x, y - s * 0.42f)
        cubicTo(x + s * 0.17f, y - s * 0.89f, x + s * 0.75f, y - s * 0.95f, x + s * 0.84f, y - s * 0.42f)
        cubicTo(x + s * 0.94f, y - s * 0.02f, x + s * 0.35f, y + s * 0.48f, x, y + s * 0.72f)
        close()
    }
    drawCircle(Color(0x44FF5279), s * 1.8f, Offset(x, y), alpha = 0.45f)
    drawPath(
        path,
        Brush.linearGradient(
            listOf(Color(0xFFFFA0B6), Color(0xFFE93661), Color(0xFF9D183F)),
            start = Offset(x - s, y - s),
            end = Offset(x + s, y + s)
        )
    )
    drawLine(
        Color(0xFFFFD8E2).copy(alpha = 0.8f),
        Offset(x - s * .47f, y - s * .48f),
        Offset(x - s * .2f, y - s * .62f),
        strokeWidth = max(1f, s * .08f)
    )
}

private fun DrawScope.drawFox(
    center: Offset,
    scale: Float,
    time: Float,
    running: Boolean,
    sniffing: Boolean,
    standing: Boolean,
    lookingAtHeart: Boolean
) {
    val s = scale
    val bob = if (running) sin(time * 13f) * 5f * s else sin(time * 2f) * 1.5f * s
    val c = Offset(center.x, center.y + bob)
    val orange = Color(0xFFE8752D)
    val bright = Color(0xFFF49A48)
    val cream = Color(0xFFFFE9CB)
    val dark = Color(0xFF8D432A)

    // Soft shadow beneath the fox.
    drawOval(
        Color(0x33313D55),
        topLeft = Offset(c.x - 59*s, c.y + 48*s),
        size = Size(118*s, 17*s)
    )

    // Big fluffy tail, with a cream tip.
    val tailAngle = if (running) sin(time * 10f) * .26f - .18f else -.12f + sin(time * 1.7f)*.06f
    val tail = Path().apply {
        moveTo(c.x - 32*s, c.y + 17*s)
        cubicTo(c.x - 82*s, c.y - 42*s, c.x - 143*s, c.y - 23*s, c.x - 139*s, c.y + 26*s)
        cubicTo(c.x - 134*s, c.y + 82*s, c.x - 73*s, c.y + 73*s, c.x - 31*s, c.y + 37*s)
        close()
    }
    rotate(tailAngle * 35f, Offset(c.x - 40*s, c.y + 20*s)) {
        drawPath(tail, orange)
        val tip = Path().apply {
            moveTo(c.x - 135*s, c.y + 8*s)
            cubicTo(c.x - 145*s, c.y + 47*s, c.x - 110*s, c.y + 67*s, c.x - 82*s, c.y + 49*s)
            cubicTo(c.x - 100*s, c.y + 34*s, c.x - 109*s, c.y + 15*s, c.x - 105*s, c.y - 1*s)
            close()
        }
        drawPath(tip, cream)
        drawLine(Color(0xFFFFC17C), Offset(c.x - 63*s, c.y - 3*s), Offset(c.x - 115*s, c.y + 14*s), 2*s)
    }

    // Body shape.
    if (standing) {
        drawOval(orange, Offset(c.x - 24*s, c.y - 36*s), Size(49*s, 91*s))
        drawOval(cream, Offset(c.x - 9*s, c.y - 4*s), Size(26*s, 51*s))
    } else {
        drawOval(orange, Offset(c.x - 55*s, c.y - 9*s), Size(105*s, 59*s))
        drawOval(bright, Offset(c.x - 20*s, c.y + 7*s), Size(59*s, 35*s))
        drawOval(cream, Offset(c.x - 7*s, c.y + 15*s), Size(37*s, 23*s))
    }

    // Legs with alternating running stride.
    val stride = if (running) sin(time * 13f) * 17f * s else 0f
    if (standing) {
        drawOval(dark, Offset(c.x - 24*s, c.y + 40*s), Size(22*s, 17*s))
        drawOval(dark, Offset(c.x + 3*s, c.y + 40*s), Size(22*s, 17*s))
        drawOval(orange, Offset(c.x - 22*s, c.y - 26*s), Size(15*s, 51*s))
        drawOval(orange, Offset(c.x + 7*s, c.y - 26*s), Size(15*s, 51*s))
        drawOval(cream, Offset(c.x - 25*s, c.y + 12*s), Size(19*s, 11*s))
        drawOval(cream, Offset(c.x + 5*s, c.y + 12*s), Size(19*s, 11*s))
    } else {
        drawOval(orange, Offset(c.x - 38*s, c.y + 25*s + stride), Size(17*s, 39*s))
        drawOval(dark, Offset(c.x - 28*s, c.y + 30*s - stride), Size(17*s, 35*s))
        drawOval(orange, Offset(c.x + 14*s, c.y + 25*s - stride), Size(17*s, 39*s))
        drawOval(dark, Offset(c.x + 34*s, c.y + 26*s + stride), Size(17*s, 39*s))
        drawOval(cream, Offset(c.x - 43*s, c.y + 56*s + stride), Size(22*s, 9*s))
        drawOval(cream, Offset(c.x + 29*s, c.y + 57*s + stride), Size(22*s, 9*s))
    }

    // Head lowers when sniffing the heart.
    val headX = c.x + if (sniffing) 37*s else 34*s
    val headY = c.y + if (sniffing) -2*s else -29*s

    // Ears.
    val leftEar = Path().apply {
        moveTo(headX - 25*s, headY - 9*s)
        lineTo(headX - 31*s, headY - 63*s)
        lineTo(headX + 1*s, headY - 30*s)
        close()
    }
    val rightEar = Path().apply {
        moveTo(headX + 7*s, headY - 24*s)
        lineTo(headX + 38*s, headY - 62*s)
        lineTo(headX + 39*s, headY - 8*s)
        close()
    }
    drawPath(leftEar, orange)
    drawPath(rightEar, orange)

    val innerLeft = Path().apply {
        moveTo(headX - 23*s, headY - 22*s)
        lineTo(headX - 27*s, headY - 52*s)
        lineTo(headX - 8*s, headY - 28*s)
        close()
    }
    val innerRight = Path().apply {
        moveTo(headX + 19*s, headY - 25*s)
        lineTo(headX + 34*s, headY - 51*s)
        lineTo(headX + 32*s, headY - 17*s)
        close()
    }
    drawPath(innerLeft, Color(0xFF71352D))
    drawPath(innerRight, Color(0xFF71352D))

    // Cheek and head.
    drawOval(orange, Offset(headX - 32*s, headY - 36*s), Size(69*s, 65*s))
    drawOval(cream, Offset(headX - 4*s, headY - 5*s), Size(43*s, 33*s))

    // Pointed muzzle.
    val muzzle = Path().apply {
        moveTo(headX + 4*s, headY - 4*s)
        cubicTo(headX + 31*s, headY - 14*s, headX + 55*s, headY - 2*s, headX + 53*s, headY + 14*s)
        cubicTo(headX + 39*s, headY + 33*s, headX + 12*s, headY + 25*s, headX + 4*s, headY + 12*s)
        close()
    }
    drawPath(muzzle, cream)

    // Eyes and tiny highlights.
    val blink = (time % 4.7f) in 0.08f..0.19f
    if (!blink) {
        drawOval(Color(0xFF251923), Offset(headX + 8*s, headY - 12*s), Size(7*s, 10*s))
        drawCircle(Color.White, 2*s, Offset(headX + 11*s, headY - 10*s))
    } else {
        drawLine(Color(0xFF542B28), Offset(headX + 7*s, headY - 7*s), Offset(headX + 16*s, headY - 7*s), 2*s)
    }

    // Nose, smile, whiskers.
    drawCircle(Color(0xFF291B22), 5*s, Offset(headX + 52*s, headY + 12*s))
    drawLine(Color(0xFF9C5549), Offset(headX + 34*s, headY + 22*s), Offset(headX + 44*s, headY + 22*s), 1.4f*s)
    drawLine(Color(0x99FFF3E5), Offset(headX + 26*s, headY + 16*s), Offset(headX + 49*s, headY + 10*s), 1*s)
    drawLine(Color(0x99FFF3E5), Offset(headX + 26*s, headY + 23*s), Offset(headX + 48*s, headY + 20*s), 1*s)

    // Tiny breath in the cold air while sniffing.
    if (sniffing) {
        val breathAlpha = (0.3f + 0.3f * sin(time * 5f)).coerceIn(0.1f, 0.7f)
        drawCircle(Color.White.copy(alpha = breathAlpha), 5*s, Offset(headX + 62*s, headY + 13*s))
        drawCircle(Color.White.copy(alpha = breathAlpha * .55f), 8*s, Offset(headX + 70*s, headY + 8*s))
    }
}
