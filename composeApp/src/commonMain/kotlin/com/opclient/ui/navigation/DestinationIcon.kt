package com.opclient.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DestinationIcon(
    destination: Destination,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = Modifier.size(size).then(modifier)) {
        val s = this.size.width
        val strokeWidth = 1.5.dp.toPx()
        when (destination) {
            Destination.SEARCH -> {
                drawCircle(
                    color = tint,
                    radius = s * 0.32f,
                    center = Offset(s * 0.42f, s * 0.42f),
                    style = Stroke(width = strokeWidth),
                )
                drawLine(
                    color = tint,
                    start = Offset(s * 0.66f, s * 0.66f),
                    end = Offset(s * 0.88f, s * 0.88f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            Destination.BROWSE -> {
                val r = 2.dp.toPx()
                listOf(
                    Offset(s * 0.30f, s * 0.30f),
                    Offset(s * 0.70f, s * 0.30f),
                    Offset(s * 0.30f, s * 0.70f),
                    Offset(s * 0.70f, s * 0.70f),
                ).forEach { drawCircle(color = tint, radius = r, center = it) }
            }
            Destination.LIBRARY -> {
                val x0 = s * 0.15f
                val x1 = s * 0.85f
                listOf(0.30f, 0.50f, 0.70f).forEach { y ->
                    drawLine(
                        color = tint,
                        start = Offset(x0, s * y),
                        end = Offset(x1, s * y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
            Destination.CHANGES -> {
                drawCircle(
                    color = tint,
                    radius = s * 0.35f,
                    center = Offset(s * 0.5f, s * 0.5f),
                    style = Stroke(width = strokeWidth),
                )
                drawLine(
                    color = tint,
                    start = Offset(s * 0.5f, s * 0.5f),
                    end = Offset(s * 0.5f, s * 0.26f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(s * 0.5f, s * 0.5f),
                    end = Offset(s * 0.68f, s * 0.5f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
