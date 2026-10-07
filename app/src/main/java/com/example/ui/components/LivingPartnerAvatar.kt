package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AvatarExpression
import com.example.ui.theme.MesraBorderSubtle
import com.example.ui.theme.MesraCardSurface
import com.example.ui.theme.MesraDarkBg
import com.example.ui.theme.MesraMagentaAccent
import com.example.ui.theme.MesraPinkPrimary
import com.example.ui.theme.MesraSoftPink
import com.example.ui.theme.OnlineMint
import com.example.ui.theme.SadBlue
import com.example.ui.theme.ShyBlush
import com.example.ui.theme.SpeakingGold
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondarySoft
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LivingPartnerAvatarHeader(
    expression: AvatarExpression,
    isSpeaking: Boolean,
    isTyping: Boolean,
    mouthOpenness: Float,
    isTtsEnabled: Boolean,
    canReplayVoice: Boolean,
    userCallName: String,
    onExpressionSelected: (AvatarExpression) -> Unit,
    onToggleTts: () -> Unit,
    onReplayVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-blinking coroutine loop: natural blink every 2.8s - 4.4s, with occasional double blink
    var blinkProgress by remember { mutableFloatStateOf(0f) } // 0f = open, 1f = closed
    var tapHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val waitTime = (2800L..4400L).random()
            delay(waitTime)
            blinkProgress = 1f
            delay(110L)
            blinkProgress = 0f
            if ((0..3).random() == 0) {
                delay(140L)
                blinkProgress = 1f
                delay(95L)
                blinkProgress = 0f
            }
        }
    }

    LaunchedEffect(tapHeartBurst) {
        if (tapHeartBurst) {
            delay(900L)
            tapHeartBurst = false
        }
    }

    val animatedBlink by animateFloatAsState(
        targetValue = blinkProgress,
        animationSpec = tween(durationMillis = 85, easing = FastOutSlowInEasing),
        label = "eyelidBlink"
    )

    val animatedMouth by animateFloatAsState(
        targetValue = if (isSpeaking) mouthOpenness.coerceIn(0.08f, 1f) else 0f,
        animationSpec = tween(durationMillis = 55, easing = LinearEasing),
        label = "lipSyncMouth"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "avatarIdleMotion")

    val breathOffset by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val headTiltDegrees by infiniteTransition.animateFloat(
        initialValue = -2.4f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headTilt"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 520 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraPulse"
    )

    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )

    val moodAccentColor by animateColorAsState(
        targetValue = when {
            isSpeaking -> SpeakingGold
            expression == AvatarExpression.HAPPY -> MesraPinkPrimary
            expression == AvatarExpression.SHY -> ShyBlush
            expression == AvatarExpression.SAD -> SadBlue
            else -> MesraSoftPink
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "moodAccentColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("avatar_stage_card"),
        shape = RoundedCornerShape(26.dp),
        color = MesraCardSurface,
        tonalElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2B1035),
                            MesraCardSurface,
                            MesraDarkBg
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            moodAccentColor.copy(alpha = 0.65f),
                            MesraBorderSubtle.copy(alpha = 0.35f),
                            MesraMagentaAccent.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Overlay Row: Status Pill & Voice Controls (Toggle TTS + Replay TTS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MesraDarkBg.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = moodAccentColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSpeaking -> SpeakingGold
                                            isTyping -> MesraPinkPrimary
                                            else -> OnlineMint
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isSpeaking -> "MesraAI sedang berbicara..."
                                    isTyping -> "MesraAI sedang mengetik..."
                                    else -> "${expression.emoji} ${expression.labelId}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimaryLight
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onReplayVoice,
                            enabled = canReplayVoice,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MesraDarkBg.copy(alpha = 0.68f))
                                .testTag("replay_tts_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.GraphicEq else Icons.Filled.Replay,
                                contentDescription = "Ulangi Suara Jawaban",
                                tint = if (canReplayVoice) MesraSoftPink else TextSecondarySoft.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleTts,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTtsEnabled) MesraPinkPrimary.copy(alpha = 0.25f)
                                    else MesraDarkBg.copy(alpha = 0.68f)
                                )
                                .testTag("toggle_tts_button")
                        ) {
                            Icon(
                                imageVector = if (isTtsEnabled) {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                },
                                contentDescription = if (isTtsEnabled) "Nonaktifkan Suara" else "Aktifkan Suara",
                                tint = if (isTtsEnabled) MesraPinkPrimary else TextSecondarySoft,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Interactive Living Avatar Canvas
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            tapHeartBurst = true
                            val nextExpr = when (expression) {
                                AvatarExpression.NORMAL -> AvatarExpression.SMILE
                                AvatarExpression.SMILE -> AvatarExpression.HAPPY
                                AvatarExpression.HAPPY -> AvatarExpression.SHY
                                AvatarExpression.SHY -> AvatarExpression.SMILE
                                AvatarExpression.SAD -> AvatarExpression.SMILE
                            }
                            onExpressionSelected(nextExpr)
                        }
                        .testTag("living_partner_avatar_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (size.width > 8f && size.height > 8f) {
                            drawLivingCompanionAvatar(
                                expression = expression,
                                isSpeaking = isSpeaking,
                                blinkAmount = animatedBlink,
                                mouthOpenness = animatedMouth,
                                breathOffset = breathOffset,
                                headTiltDegrees = headTiltDegrees,
                                auraScale = auraPulse,
                                particlePhase = particlePhase,
                                moodColor = moodAccentColor,
                                showExtraHearts = tapHeartBurst
                            )
                        }
                    }

                    if (isSpeaking) {
                        Surface(
                            color = MesraPinkPrimary,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Berbicara ke $userCallName",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Expression Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarExpression.entries.forEach { item ->
                        val isSelected = item == expression
                        Surface(
                            color = if (isSelected) {
                                MesraPinkPrimary.copy(alpha = 0.28f)
                            } else {
                                MesraDarkBg.copy(alpha = 0.55f)
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onExpressionSelected(item) }
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MesraPinkPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                                .testTag("expr_chip_${item.name.lowercase()}")
                        ) {
                            Text(
                                text = "${item.emoji} ${item.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) TextPrimaryLight else TextSecondarySoft,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLivingCompanionAvatar(
    expression: AvatarExpression,
    isSpeaking: Boolean,
    blinkAmount: Float,
    mouthOpenness: Float,
    breathOffset: Float,
    headTiltDegrees: Float,
    auraScale: Float,
    particlePhase: Float,
    moodColor: Color,
    showExtraHearts: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseRadius = (size.minDimension * 0.42f).coerceAtLeast(16f)
    val safeAuraRadius = (baseRadius * 1.32f * auraScale.coerceAtLeast(0.5f)).coerceAtLeast(10f)

    // 1. Pulsing Romantic Aura Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                moodColor.copy(alpha = if (isSpeaking) 0.42f else 0.24f),
                MesraMagentaAccent.copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = safeAuraRadius
        ),
        radius = safeAuraRadius,
        center = Offset(cx, cy)
    )

    if (isSpeaking) {
        drawCircle(
            color = MesraPinkPrimary.copy(alpha = 0.35f),
            radius = (baseRadius * 1.08f * auraScale.coerceAtLeast(0.5f)).coerceAtLeast(8f),
            center = Offset(cx, cy),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }

    // 2. Back Long Hair
    val hairBackColor = Color(0xFF28112B)
    val hairHighlightColor = Color(0xFF4D2153)
    val backHairPath = Path().apply {
        moveTo(cx - baseRadius * 0.78f, cy - baseRadius * 0.25f + breathOffset * 0.5f)
        cubicTo(
            cx - baseRadius * 0.96f, cy + baseRadius * 0.45f,
            cx - baseRadius * 0.82f, cy + baseRadius * 0.98f,
            cx - baseRadius * 0.48f, cy + baseRadius * 1.05f + breathOffset
        )
        lineTo(cx + baseRadius * 0.48f, cy + baseRadius * 1.05f + breathOffset)
        cubicTo(
            cx + baseRadius * 0.82f, cy + baseRadius * 0.98f,
            cx + baseRadius * 0.96f, cy + baseRadius * 0.45f,
            cx + baseRadius * 0.78f, cy - baseRadius * 0.25f + breathOffset * 0.5f
        )
        close()
    }
    drawPath(backHairPath, color = hairBackColor)

    // 3. Neck & Shoulders
    val skinColor = Color(0xFFFFE5D9)
    val skinShadow = Color(0xFFF3C6B6)
    val neckWidth = baseRadius * 0.28f
    drawRoundRect(
        color = skinShadow,
        topLeft = Offset(cx - neckWidth / 2f, cy + baseRadius * 0.44f + breathOffset * 0.6f),
        size = Size(neckWidth, baseRadius * 0.36f),
        cornerRadius = CornerRadius(12f, 12f)
    )

    val shoulderPath = Path().apply {
        moveTo(cx - baseRadius * 0.72f, cy + baseRadius * 1.04f)
        quadraticTo(
            cx - baseRadius * 0.45f,
            cy + baseRadius * 0.66f + breathOffset,
            cx,
            cy + baseRadius * 0.74f + breathOffset
        )
        quadraticTo(
            cx + baseRadius * 0.45f,
            cy + baseRadius * 0.66f + breathOffset,
            cx + baseRadius * 0.72f,
            cy + baseRadius * 1.04f
        )
        close()
    }
    drawPath(
        path = shoulderPath,
        brush = Brush.verticalGradient(
            colors = listOf(MesraPinkPrimary, Color(0xFF9C1453))
        )
    )

    // 4. Head Group with subtle Head Tilt & Breathing
    val extraTilt = when (expression) {
        AvatarExpression.SHY -> 3.5f
        AvatarExpression.SAD -> -2.5f
        AvatarExpression.HAPPY -> headTiltDegrees * 1.3f
        else -> 0f
    }

    withTransform({
        rotate(
            degrees = headTiltDegrees + extraTilt,
            pivot = Offset(cx, cy + baseRadius * 0.35f)
        )
        translate(left = 0f, top = breathOffset * 0.85f)
    }) {
        val faceWidth = baseRadius * 1.24f
        val faceHeight = baseRadius * 1.34f
        drawOval(
            color = skinColor,
            topLeft = Offset(cx - faceWidth / 2f, cy - faceHeight * 0.54f),
            size = Size(faceWidth, faceHeight)
        )

        val blushAlpha = when (expression) {
            AvatarExpression.SHY -> 0.62f
            AvatarExpression.HAPPY -> 0.45f
            AvatarExpression.SMILE -> 0.35f
            AvatarExpression.NORMAL -> 0.22f
            AvatarExpression.SAD -> 0.15f
        }
        val leftCheekCenter = Offset(cx - baseRadius * 0.34f, cy + baseRadius * 0.14f)
        val rightCheekCenter = Offset(cx + baseRadius * 0.34f, cy + baseRadius * 0.14f)
        val cheekRadius = (baseRadius * 0.22f).coerceAtLeast(4f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF6090).copy(alpha = blushAlpha), Color.Transparent),
                center = leftCheekCenter,
                radius = cheekRadius
            ),
            radius = cheekRadius,
            center = leftCheekCenter
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF6090).copy(alpha = blushAlpha), Color.Transparent),
                center = rightCheekCenter,
                radius = cheekRadius
            ),
            radius = cheekRadius,
            center = rightCheekCenter
        )

        if (expression == AvatarExpression.SHY) {
            val slashColor = Color(0xFFFF4081).copy(alpha = 0.65f)
            for (i in -1..1) {
                val dx = i * 6.dp.toPx()
                drawLine(
                    color = slashColor,
                    start = Offset(leftCheekCenter.x + dx - 3f, leftCheekCenter.y - 4f),
                    end = Offset(leftCheekCenter.x + dx + 3f, leftCheekCenter.y + 5f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = slashColor,
                    start = Offset(rightCheekCenter.x + dx - 3f, rightCheekCenter.y - 4f),
                    end = Offset(rightCheekCenter.x + dx + 3f, rightCheekCenter.y + 5f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 5. Eyes
        val eyeY = cy - baseRadius * 0.04f
        val leftEyeX = cx - baseRadius * 0.27f
        val rightEyeX = cx + baseRadius * 0.27f
        val eyeRadiusX = (baseRadius * 0.13f).coerceAtLeast(3f)
        val eyeRadiusY = (baseRadius * 0.15f).coerceAtLeast(3f)

        drawCompanionEye(
            centerX = leftEyeX,
            centerY = eyeY,
            radiusX = eyeRadiusX,
            radiusY = eyeRadiusY,
            blinkAmount = blinkAmount,
            expression = expression,
            isLeft = true
        )
        drawCompanionEye(
            centerX = rightEyeX,
            centerY = eyeY,
            radiusX = eyeRadiusX,
            radiusY = eyeRadiusY,
            blinkAmount = blinkAmount,
            expression = expression,
            isLeft = false
        )

        // 6. Eyebrows
        val browY = eyeY - eyeRadiusY * 1.45f
        val browColor = Color(0xFF3A163E)
        drawCompanionEyebrow(
            centerX = leftEyeX,
            centerY = browY,
            width = eyeRadiusX * 2.1f,
            expression = expression,
            isLeft = true,
            color = browColor
        )
        drawCompanionEyebrow(
            centerX = rightEyeX,
            centerY = browY,
            width = eyeRadiusX * 2.1f,
            expression = expression,
            isLeft = false,
            color = browColor
        )

        drawLine(
            color = skinShadow,
            start = Offset(cx, cy + baseRadius * 0.06f),
            end = Offset(cx - 2.dp.toPx(), cy + baseRadius * 0.13f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 7. Mouth + Lip Sync
        val mouthY = cy + baseRadius * 0.29f
        drawCompanionMouth(
            centerX = cx,
            centerY = mouthY,
            baseRadius = baseRadius,
            expression = expression,
            isSpeaking = isSpeaking,
            mouthOpenness = mouthOpenness
        )

        // 8. Front Hair Bangs & Heart Clip
        val frontHairPath = Path().apply {
            moveTo(cx - faceWidth * 0.54f, cy - faceHeight * 0.05f)
            cubicTo(
                cx - faceWidth * 0.52f, cy - faceHeight * 0.66f,
                cx + faceWidth * 0.52f, cy - faceHeight * 0.66f,
                cx + faceWidth * 0.54f, cy - faceHeight * 0.05f
            )
            cubicTo(
                cx + faceWidth * 0.32f, cy - faceHeight * 0.26f,
                cx + faceWidth * 0.15f, cy - faceHeight * 0.12f,
                cx, cy - faceHeight * 0.24f
            )
            cubicTo(
                cx - faceWidth * 0.15f, cy - faceHeight * 0.12f,
                cx - faceWidth * 0.32f, cy - faceHeight * 0.26f,
                cx - faceWidth * 0.54f, cy - faceHeight * 0.05f
            )
            close()
        }
        drawPath(
            path = frontHairPath,
            brush = Brush.verticalGradient(
                colors = listOf(hairHighlightColor, hairBackColor)
            )
        )

        drawMiniHeart(
            center = Offset(cx + baseRadius * 0.44f, cy - baseRadius * 0.36f),
            size = (baseRadius * 0.14f).coerceAtLeast(3f),
            color = MesraPinkPrimary
        )
    }

    // 9. Floating Romantic Hearts
    val showHearts = expression == AvatarExpression.HAPPY ||
        expression == AvatarExpression.SMILE ||
        expression == AvatarExpression.SHY ||
        isSpeaking ||
        showExtraHearts

    if (showHearts) {
        val heartPositions = listOf(
            Triple(-0.78f, -0.45f, 0f),
            Triple(0.80f, -0.38f, 2.1f),
            Triple(-0.68f, 0.32f, 4.2f),
            Triple(0.74f, 0.28f, 1.3f)
        )
        heartPositions.forEachIndexed { idx, (ox, oy, phaseShift) ->
            val bobY = sin(particlePhase + phaseShift) * 8.dp.toPx()
            val pulse = (0.85f + 0.25f * cos(particlePhase + idx)).coerceAtLeast(0.4f)
            drawMiniHeart(
                center = Offset(cx + baseRadius * ox, cy + baseRadius * oy + bobY),
                size = (baseRadius * 0.13f * pulse).coerceAtLeast(3f),
                color = if (idx % 2 == 0) MesraPinkPrimary.copy(alpha = 0.82f)
                else MesraSoftPink.copy(alpha = 0.75f)
            )
        }
    }
}

private fun DrawScope.drawCompanionEye(
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    blinkAmount: Float,
    expression: AvatarExpression,
    isLeft: Boolean
) {
    val isCurvedHappyEye = expression == AvatarExpression.HAPPY && blinkAmount < 0.5f
    val effectiveClosure = if (isCurvedHappyEye) 0.88f else blinkAmount.coerceIn(0f, 1f)

    if (effectiveClosure > 0.78f) {
        val arcPath = Path().apply {
            val archSign = if (expression == AvatarExpression.HAPPY || expression == AvatarExpression.SMILE) -1f else 1f
            moveTo(centerX - radiusX, centerY)
            quadraticTo(
                centerX,
                centerY + archSign * radiusY * 0.65f,
                centerX + radiusX,
                centerY
            )
        }
        drawPath(
            path = arcPath,
            color = Color(0xFF2D1030),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        return
    }

    val openRatio = (1f - effectiveClosure).coerceIn(0.15f, 1f)
    val currentHeight = (radiusY * 2f * openRatio).coerceAtLeast(2f)

    drawOval(
        color = Color(0xFFFFFBFD),
        topLeft = Offset(centerX - radiusX, centerY - currentHeight / 2f),
        size = Size(radiusX * 2f, currentHeight)
    )

    val gazeShiftX = if (expression == AvatarExpression.SHY) radiusX * 0.22f else 0f
    val irisRadius = (radiusX * 0.76f).coerceAtLeast(2f)
    val irisCenter = Offset(centerX + gazeShiftX, centerY)

    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4A154B), Color(0xFFD81B60), Color(0xFFFF80AB))
        ),
        radius = (irisRadius * openRatio.coerceAtLeast(0.55f)).coerceAtLeast(1.5f),
        center = irisCenter
    )

    drawCircle(
        color = Color(0xFF1F0824),
        radius = (irisRadius * 0.42f * openRatio.coerceAtLeast(0.5f)).coerceAtLeast(1f),
        center = irisCenter
    )

    drawCircle(
        color = Color.White,
        radius = (irisRadius * 0.26f).coerceAtLeast(1f),
        center = Offset(irisCenter.x - irisRadius * 0.28f, irisCenter.y - irisRadius * 0.28f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = (irisRadius * 0.14f).coerceAtLeast(0.8f),
        center = Offset(irisCenter.x + irisRadius * 0.28f, irisCenter.y + irisRadius * 0.22f)
    )

    if (expression == AvatarExpression.SAD) {
        drawCircle(
            color = Color(0xFFB3E5FC).copy(alpha = 0.9f),
            radius = (irisRadius * 0.22f).coerceAtLeast(1f),
            center = Offset(centerX + (if (isLeft) -1 else 1) * radiusX * 0.4f, centerY + currentHeight * 0.42f)
        )
    }

    val lashPath = Path().apply {
        moveTo(centerX - radiusX * 1.08f, centerY - currentHeight * 0.2f)
        quadraticTo(
            centerX,
            centerY - currentHeight * 0.68f,
            centerX + radiusX * 1.08f,
            centerY - currentHeight * 0.2f
        )
    }
    drawPath(
        path = lashPath,
        color = Color(0xFF2A0E2E),
        style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCompanionEyebrow(
    centerX: Float,
    centerY: Float,
    width: Float,
    expression: AvatarExpression,
    isLeft: Boolean,
    color: Color
) {
    val halfW = width / 2f
    val innerYOffset = when (expression) {
        AvatarExpression.SAD -> -4.dp.toPx()
        AvatarExpression.SHY -> -2.5.dp.toPx()
        AvatarExpression.HAPPY -> -2.dp.toPx()
        else -> 0f
    }
    val outerYOffset = when (expression) {
        AvatarExpression.SAD -> 3.dp.toPx()
        AvatarExpression.SHY -> 2.dp.toPx()
        else -> 0f
    }

    val startY = centerY + if (isLeft) outerYOffset else innerYOffset
    val endY = centerY + if (isLeft) innerYOffset else outerYOffset

    val path = Path().apply {
        moveTo(centerX - halfW, startY)
        quadraticTo(
            centerX,
            centerY - 3.dp.toPx(),
            centerX + halfW,
            endY
        )
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCompanionMouth(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    expression: AvatarExpression,
    isSpeaking: Boolean,
    mouthOpenness: Float
) {
    val lipColor = Color(0xFFD8436B)
    val innerMouthColor = Color(0xFF6D1B3B)
    val tongueColor = Color(0xFFFF80AB)

    if (isSpeaking && mouthOpenness > 0.08f) {
        val mouthW = (baseRadius * (0.22f + 0.12f * (1f - mouthOpenness * 0.35f))).coerceAtLeast(4f)
        val mouthH = (baseRadius * 0.26f * mouthOpenness).coerceAtLeast(4.dp.toPx())
        val rect = Rect(
            left = centerX - mouthW / 2f,
            top = centerY - mouthH / 2f,
            right = centerX + mouthW / 2f,
            bottom = centerY + mouthH / 2f
        )

        drawRoundRect(
            color = innerMouthColor,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(mouthW / 2f, mouthH / 2f)
        )

        if (mouthOpenness > 0.28f) {
            drawOval(
                color = tongueColor,
                topLeft = Offset(centerX - mouthW * 0.32f, centerY + mouthH * 0.05f),
                size = Size((mouthW * 0.64f).coerceAtLeast(2f), (mouthH * 0.42f).coerceAtLeast(2f))
            )
        }

        drawRoundRect(
            color = lipColor,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(mouthW / 2f, mouthH / 2f),
            style = Stroke(width = 2.dp.toPx())
        )
        return
    }

    when (expression) {
        AvatarExpression.HAPPY -> {
            val w = baseRadius * 0.30f
            val h = baseRadius * 0.16f
            val path = Path().apply {
                moveTo(centerX - w / 2f, centerY - h * 0.2f)
                lineTo(centerX + w / 2f, centerY - h * 0.2f)
                cubicTo(
                    centerX + w * 0.45f, centerY + h,
                    centerX - w * 0.45f, centerY + h,
                    centerX - w / 2f, centerY - h * 0.2f
                )
                close()
            }
            drawPath(path = path, color = innerMouthColor)
            drawPath(path = path, color = lipColor, style = Stroke(width = 2.dp.toPx()))
        }

        AvatarExpression.SMILE -> {
            val w = baseRadius * 0.28f
            val path = Path().apply {
                moveTo(centerX - w / 2f, centerY - 2.dp.toPx())
                quadraticTo(
                    centerX,
                    centerY + baseRadius * 0.13f,
                    centerX + w / 2f,
                    centerY - 2.dp.toPx()
                )
            }
            drawPath(
                path = path,
                color = lipColor,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        AvatarExpression.SHY -> {
            val w = baseRadius * 0.20f
            val path = Path().apply {
                moveTo(centerX - w / 2f, centerY)
                quadraticTo(centerX - w * 0.25f, centerY + 4.dp.toPx(), centerX, centerY)
                quadraticTo(centerX + w * 0.25f, centerY + 4.dp.toPx(), centerX + w / 2f, centerY - 1.dp.toPx())
            }
            drawPath(
                path = path,
                color = lipColor,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        AvatarExpression.SAD -> {
            val w = baseRadius * 0.22f
            val path = Path().apply {
                moveTo(centerX - w / 2f, centerY + 4.dp.toPx())
                quadraticTo(
                    centerX,
                    centerY - 3.dp.toPx(),
                    centerX + w / 2f,
                    centerY + 4.dp.toPx()
                )
            }
            drawPath(
                path = path,
                color = lipColor,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        AvatarExpression.NORMAL -> {
            val w = baseRadius * 0.22f
            val path = Path().apply {
                moveTo(centerX - w / 2f, centerY)
                quadraticTo(
                    centerX,
                    centerY + baseRadius * 0.07f,
                    centerX + w / 2f,
                    centerY
                )
            }
            drawPath(
                path = path,
                color = lipColor,
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawMiniHeart(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        val half = size / 2f
        moveTo(center.x, center.y + half * 0.85f)
        cubicTo(
            center.x - size * 1.1f, center.y + half * 0.1f,
            center.x - half * 0.9f, center.y - size * 0.75f,
            center.x, center.y - half * 0.25f
        )
        cubicTo(
            center.x + half * 0.9f, center.y - size * 0.75f,
            center.x + size * 1.1f, center.y + half * 0.1f,
            center.x, center.y + half * 0.85f
        )
        close()
    }
    drawPath(path = path, color = color)
}
