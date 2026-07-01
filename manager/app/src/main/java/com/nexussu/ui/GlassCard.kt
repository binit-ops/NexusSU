@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val blurTarget = 16f
    val blur by animateFloatAsState(
        targetValue = blurTarget,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP)
            }
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .shadow(8.dp, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
