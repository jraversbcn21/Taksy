package com.example.taksy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taksy.R
import com.example.taksy.domain.PlantStage
import com.example.taksy.ui.theme.HomePalette
import com.example.taksy.ui.theme.PlusJakartaSans

private val LeafGreen = Color(0xFF43A047)
private val LeafLight = Color(0xFF66BB6A)
private val Trunk = Color(0xFF8D6E63)
private val Flower = Color(0xFFFFB74D)
private val Seed = Color(0xFFD9B77E)

/**
 * La planta de Estadísticas: crece por etapas con las tareas completadas (ver [PlantStage]).
 * Dibujada con vectores, sin imágenes.
 */
@Composable
fun PlantCard(completed: Int, palette: HomePalette, modifier: Modifier = Modifier) {
    val stage = PlantStage.of(completed)
    val progress by animateFloatAsState(stage.progress(completed), tween(600), label = "plant")
    val shape = RoundedCornerShape(20.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .border(1.dp, palette.border, shape)
            .padding(16.dp)
    ) {
        PlantIllustration(stage, palette, Modifier.size(104.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.stats_plant).uppercase(),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = palette.muted
            )
            Text(
                text = stringResource(stageName(stage)),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp,
                color = palette.ink
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.track)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(LeafGreen)
                )
            }
            val next = stage.next
            Text(
                text = if (next == null) {
                    stringResource(R.string.plant_complete)
                } else {
                    val left = stage.remaining(completed)
                    pluralStringResource(R.plurals.plant_to_next, left, left, stringResource(stageName(next)))
                },
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = palette.muted
            )
        }
    }
}

private fun stageName(stage: PlantStage): Int = when (stage) {
    PlantStage.SEED -> R.string.plant_stage_seed
    PlantStage.SPROUT -> R.string.plant_stage_sprout
    PlantStage.SEEDLING -> R.string.plant_stage_seedling
    PlantStage.PLANT -> R.string.plant_stage_plant
    PlantStage.TREE -> R.string.plant_stage_tree
}

/** Dibujo de cada etapa en un cuadro de 100 x 100 que se escala al tamaño del componente. */
@Composable
private fun PlantIllustration(stage: PlantStage, palette: HomePalette, modifier: Modifier = Modifier) {
    val soil = if (palette.isDark) Color(0xFF8D6E63) else Color(0xFF6D4C41)
    Canvas(modifier) {
        val s = size.minDimension / 100f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)

        drawOval(soil, topLeft = p(18f, 80f), size = Size(64f * s, 16f * s))

        // Hoja que sale del tallo en (x, y): [side] = 1 a la derecha, -1 a la izquierda
        fun leaf(x: Float, y: Float, side: Int, w: Float, h: Float, color: Color = LeafGreen) {
            val pivot = p(x, y)
            rotate(degrees = -side * 30f, pivot = pivot) {
                val left = if (side > 0) x else x - w
                drawOval(color, topLeft = p(left, y - h / 2), size = Size(w * s, h * s))
            }
        }

        fun stem(top: Float, width: Float = 3f) = drawLine(
            LeafGreen, start = p(50f, 88f), end = p(50f, top), strokeWidth = width * s, cap = StrokeCap.Round
        )

        when (stage) {
            PlantStage.SEED -> drawOval(Seed, topLeft = p(42f, 74f), size = Size(16f * s, 11f * s))
            PlantStage.SPROUT -> {
                stem(68f)
                leaf(50f, 68f, 1, 16f, 8f, LeafLight)
                leaf(50f, 68f, -1, 16f, 8f, LeafLight)
            }
            PlantStage.SEEDLING -> {
                stem(50f)
                leaf(50f, 70f, 1, 18f, 9f)
                leaf(50f, 70f, -1, 18f, 9f)
                leaf(50f, 58f, 1, 18f, 9f, LeafLight)
                leaf(50f, 58f, -1, 18f, 9f, LeafLight)
                drawOval(LeafLight, topLeft = p(46f, 40f), size = Size(8f * s, 14f * s))
            }
            PlantStage.PLANT -> {
                stem(34f, 3.5f)
                leaf(50f, 76f, 1, 22f, 10f)
                leaf(50f, 76f, -1, 22f, 10f)
                leaf(50f, 64f, 1, 22f, 10f, LeafLight)
                leaf(50f, 64f, -1, 22f, 10f, LeafLight)
                leaf(50f, 52f, 1, 18f, 9f)
                leaf(50f, 52f, -1, 18f, 9f)
                drawCircle(Flower, radius = 7f * s, center = p(50f, 30f))
                drawCircle(Color.White.copy(alpha = 0.6f), radius = 2.5f * s, center = p(50f, 30f))
            }
            PlantStage.TREE -> {
                drawLine(Trunk, start = p(50f, 88f), end = p(50f, 48f), strokeWidth = 9f * s, cap = StrokeCap.Round)
                crown(::p, s)
            }
        }
    }
}

private fun DrawScope.crown(p: (Float, Float) -> Offset, s: Float) {
    drawCircle(LeafGreen, radius = 22f * s, center = p(50f, 34f))
    drawCircle(LeafGreen, radius = 16f * s, center = p(32f, 46f))
    drawCircle(LeafGreen, radius = 16f * s, center = p(68f, 46f))
    drawCircle(LeafLight, radius = 12f * s, center = p(44f, 28f))
    drawCircle(LeafLight, radius = 8f * s, center = p(62f, 40f))
}
