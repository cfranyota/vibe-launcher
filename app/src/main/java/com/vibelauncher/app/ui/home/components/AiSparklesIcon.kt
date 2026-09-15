package com.vibelauncher.app.ui.home.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The AI tile's glyph: a big four-point sparkle with a small one at its upper right, drawn
 *  as outlines so it sits at the same weight as the Material Outlined icons beside it. */
val AiSparkles: ImageVector by lazy {
    ImageVector.Builder(
        name = "AiSparkles",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            sparkle(cx = 9.5f, cy = 14.5f, r = 8f)
            sparkle(cx = 18.5f, cy = 5.5f, r = 4f)
        }
    }.build()
}

/** Four points joined by curves bowed in toward the centre, which gives the pinched ✦ look. */
private fun PathBuilder.sparkle(cx: Float, cy: Float, r: Float) {
    val k = r * 0.14f
    moveTo(cx, cy - r)
    quadTo(cx + k, cy - k, cx + r, cy)
    quadTo(cx + k, cy + k, cx, cy + r)
    quadTo(cx - k, cy + k, cx - r, cy)
    quadTo(cx - k, cy - k, cx, cy - r)
    close()
}
