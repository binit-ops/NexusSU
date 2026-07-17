package com.nexussu.manager.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------- Glass surface ----------
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val p = LocalNexusPalette.current
    val haze = LocalHazeState.current
    Box(
        modifier
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .let { m ->
                if (haze != null) {
                    m.hazeEffect(
                        state = haze,
                        style = HazeStyle(
                            backgroundColor = p.void,
                            tint = HazeTint(p.glassFill.copy(alpha = 0.5f)),
                            blurRadius = 20.dp
                        )
                    )
                } else {
                    m.background(Brush.linearGradient(listOf(p.glassFill, p.glassFill.copy(alpha = p.glassFill.alpha * 0.5f))))
                }
            }
            .border(1.dp, p.glassEdge, shape)
    ) {
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent))))
        content()
    }
}

// ---------- Hand-drawn icons ----------
@Composable
fun HomeIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(19.dp)) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val roof = Path().apply {
            moveTo(w * 0.12f, h * 0.5f); lineTo(w * 0.5f, h * 0.1f); lineTo(w * 0.88f, h * 0.5f)
        }
        drawPath(roof, tint, style = stroke)
        drawRoundRect(tint, topLeft = Offset(w * 0.27f, h * 0.45f), size = Size(w * 0.46f, h * 0.43f),
            cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
    }
}

@Composable
fun LogIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(19.dp)) {
        val w = size.width; val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(tint, topLeft = Offset(w * 0.15f, h * 0.1f), size = Size(w * 0.7f, h * 0.8f), cornerRadius = CornerRadius(3.dp.toPx()), style = stroke)
        drawLine(tint, Offset(w * 0.35f, h * 0.35f), Offset(w * 0.65f, h * 0.35f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.35f, h * 0.55f), Offset(w * 0.65f, h * 0.55f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun ShieldIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(19.dp)) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.06f); lineTo(w * 0.86f, h * 0.22f); lineTo(w * 0.86f, h * 0.5f)
            cubicTo(w * 0.86f, h * 0.76f, w * 0.7f, h * 0.9f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.3f, h * 0.9f, w * 0.14f, h * 0.76f, w * 0.14f, h * 0.5f)
            lineTo(w * 0.14f, h * 0.22f); close()
        }
        drawPath(path, tint, style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
fun ModuleIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(19.dp)) {
        val s = size.width * 0.32f; val gap = size.width * 0.1f
        val stroke = Stroke(width = 1.6.dp.toPx())
        listOf(Offset(0f, 0f), Offset(s + gap, 0f), Offset(0f, s + gap), Offset(s + gap, s + gap)).forEach {
            drawRoundRect(tint, topLeft = it, size = Size(s, s), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
        }
    }
}

@Composable
fun SettingsIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(17.dp)) {
        val w = size.width
        val rows = listOf(size.height * 0.25f to w * 0.62f, size.height * 0.5f to w * 0.4f, size.height * 0.75f to w * 0.62f)
        rows.forEach { (y, knobX) ->
            drawLine(tint, Offset(w * 0.1f, y), Offset(w * 0.9f, y), strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(tint, radius = 2.2.dp.toPx(), center = Offset(knobX, y), style = Stroke(width = 1.7.dp.toPx()))
        }
    }
}

@Composable
fun BackIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(17.dp)) {
        val w = size.width; val h = size.height
        val path = Path().apply { moveTo(w * 0.62f, h * 0.2f); lineTo(w * 0.35f, h * 0.5f); lineTo(w * 0.62f, h * 0.8f) }
        drawPath(path, tint, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ChevronIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val w = size.width; val h = size.height
        val path = Path().apply { moveTo(w * 0.38f, h * 0.2f); lineTo(w * 0.65f, h * 0.5f); lineTo(w * 0.38f, h * 0.8f) }
        drawPath(path, tint, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ---------- Root Lens ----------
@Composable
fun RootLens(modifier: Modifier = Modifier) {
    val p = LocalNexusPalette.current
    val infinite = rememberInfiniteTransition(label = "lensIdle")
    val idleX by infinite.animateFloat(-0.08f, 0.12f,
        infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "idleX")
    val idleY by infinite.animateFloat(-0.06f, 0.10f,
        infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "idleY")

    var dragOffset by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()
    var idleJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier.size(172.dp).pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, _ ->
                    idleJob?.cancel()
                    dragOffset = Offset((change.position.x / size.width) - 0.5f, (change.position.y / size.height) - 0.5f)
                },
                onDragEnd = { idleJob = scope.launch { delay(1100); dragOffset = null } }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.sweepGradient(listOf(p.accent, p.accent2, p.accent)))
            val hx = dragOffset?.x ?: idleX
            val hy = dragOffset?.y ?: idleY
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent)),
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * (0.5f + hx), size.height * (0.5f + hy)),
                blendMode = BlendMode.Softlight
            )
        }
        Box(Modifier.size(110.dp).clip(CircleShape).background(p.void.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROOT", color = p.dim, fontSize = 11.sp, letterSpacing = 1.sp)
                Text("Active", color = p.ink, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        }
    }
}

// ---------- Bottom nav with sliding indicator ----------
enum class Tab { Home, Log, Superuser, Module }

@Composable
fun LiquidTabBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalNexusPalette.current
    GlassCard(modifier.height(72.dp)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / Tab.entries.size
            val indicatorOffset by animateDpAsState(tabWidth * Tab.entries.indexOf(selected),
                spring(dampingRatio = 0.75f, stiffness = 300f), label = "indicator")
            
            Box(
                Modifier
                    .graphicsLayer { translationX = indicatorOffset.toPx() }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(p.accent, p.accent2)))
                    .alpha(0.25f)
            )
            
            Row(Modifier.fillMaxSize()) {
                Tab.entries.forEach { tab ->
                    val color = if (tab == selected) p.accent else p.dim
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (tab) {
                            Tab.Home -> HomeIcon(color)
                            Tab.Log -> LogIcon(color)
                            Tab.Superuser -> ShieldIcon(color)
                            Tab.Module -> ModuleIcon(color)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(tab.name, color = color, fontSize = 9.sp, fontWeight = if (tab == selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

// ---------- Toggle & segmented control ----------
@Composable
fun GlassToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val p = LocalNexusPalette.current
    // Grey out if disabled
    val trackColor = if (!enabled) p.dim.copy(alpha = 0.1f) else if (checked) p.accent else p.dim.copy(alpha = 0.25f)
    val knobColor = if (!enabled) p.dim.copy(alpha = 0.3f) else Color.White
    
    val track by animateColorAsState(trackColor, label = "track")
    val knob by animateDpAsState(if (checked) 16.dp else 0.dp, spring(dampingRatio = 0.6f), label = "knob")
    Box(
        modifier.size(width = 38.dp, height = 22.dp).clip(RoundedCornerShape(50)).background(track)
            .clickable(enabled = enabled, remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }
            .padding(2.dp)
    ) {
        Box(
            Modifier
                .graphicsLayer { translationX = knob.toPx() }
                .size(18.dp)
                .clip(CircleShape)
                .background(knobColor)
        )
    }
}

@Composable
fun GlassSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalNexusPalette.current
    GlassCard(modifier.height(42.dp), shape = RoundedCornerShape(18.dp)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val segWidth = maxWidth / options.size
            val offset by animateDpAsState(segWidth * selected, spring(dampingRatio = 0.8f), label = "seg")
            Box(
                Modifier
                    .graphicsLayer { translationX = offset.toPx() }
                    .width(segWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(p.accent, p.accent2)))
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, label ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (i == selected) Color(0xFF0A0E14) else p.dim,
                            fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
