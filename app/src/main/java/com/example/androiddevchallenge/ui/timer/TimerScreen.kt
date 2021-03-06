/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androiddevchallenge.ui.timer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.androiddevchallenge.MainViewModel
import com.example.androiddevchallenge.R
import com.example.androiddevchallenge.Screen
import com.example.androiddevchallenge.TimerState
import com.example.androiddevchallenge.ui.theme.teal200
import dev.chrisbanes.accompanist.insets.systemBarsPadding
import kotlin.math.max
import kotlin.math.min

private val ToolbarHeight = 56.dp
private val RippleRadius = 24.dp
private val IconButtonSizeModifier = Modifier.size(48.dp)
private val TimerButtonSize = 96.dp
private val FabSize = 56.dp
private const val EditTimeFormatter = "%02d"
private const val CountDownTimeFormatter = "%02d:%02d:%02d"
private val timerViewColorAnimationSpec: AnimationSpec<Color> = tween(
    durationMillis = 300,
    delayMillis = 50,
    easing = LinearOutSlowInEasing
)

// -----------------------------------------------------------------------------------------
// Composable functions
// -----------------------------------------------------------------------------------------

@ExperimentalFoundationApi
@Composable
fun TimerScreen(mainViewModel: MainViewModel) {
    val timerState by mainViewModel.state.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimerTitleView()

            Crossfade(
                targetState = timerState.screen,
                modifier = Modifier.fillMaxHeight(fraction = 0.8f)
            ) { screen ->
                when (screen) {
                    Screen.Editing -> EditingTimerContent(
                        timerState = timerState,
                        onDeleteTimeClick = { mainViewModel.subtractTime() },
                        onClearTimeClick = { mainViewModel.clearTime() },
                        onTimeClick = { mainViewModel.addTime(it) }
                    )
                    Screen.CountDown -> CountDownTimerContent(
                        countDownTimeInSec = timerState.countDownTimeInSec,
                        elapsedTimeInSec = timerState.elapsedTimeInSec,
                        elapsedTimeTriple = timerState.elapsedTimeTriple
                    )
                }
            }

            TimerActionView(
                screen = timerState.screen,
                isTimeValid = timerState.isTimeValid,
                onStartCountingDownTimeClick = { mainViewModel.startCountingDownTime() }
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun EditingTimerContent(
    timerState: TimerState,
    onDeleteTimeClick: () -> Unit,
    onClearTimeClick: () -> Unit,
    onTimeClick: (Int) -> Unit
) {
    val textColor: Color by animateColorAsState(
        targetValue = if (timerState.isTimeValid) teal200 else Color.LightGray,
        animationSpec = timerViewColorAnimationSpec
    )
    val buttonColor: Color by animateColorAsState(
        targetValue = if (timerState.isTimeValid) Color.White else Color.LightGray,
        animationSpec = timerViewColorAnimationSpec
    )

    Column(modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp)) {
        TimeView(
            hoursText = timerState.countDownTimeTriple.first,
            minutesText = timerState.countDownTimeTriple.second,
            secondsText = timerState.countDownTimeTriple.third,
            isTimeValid = timerState.isTimeValid,
            textColor = textColor,
            buttonColor = buttonColor,
            onDeleteTimeClick = onDeleteTimeClick,
            onClearTimeClick = onClearTimeClick
        )

        TimerDivider()

        TimerButtonPad(onTimeClick = onTimeClick)
    }
}

@ExperimentalFoundationApi
@Composable
fun CountDownTimerContent(
    countDownTimeInSec: Int,
    elapsedTimeInSec: Int,
    elapsedTimeTriple: Triple<Int, Int, Int>
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                elevation = 4.dp
            ) {
                TimerCircle(countDownTime = countDownTimeInSec, elapsedTime = elapsedTimeInSec)
            }
            Text(
                text = CountDownTimeFormatter.format(*elapsedTimeTriple.toList().toTypedArray()),
                style = MaterialTheme.typography.h4,
                color = Color.White
            )
        }
    }
}

@Composable
fun TimerTitleView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ToolbarHeight),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            style = MaterialTheme.typography.h6,
            text = stringResource(id = R.string.app_name)
        )
    }
}

@ExperimentalFoundationApi
@Composable
fun TimeView(
    hoursText: Int,
    minutesText: Int,
    secondsText: Int,
    isTimeValid: Boolean,
    textColor: Color,
    buttonColor: Color,
    onDeleteTimeClick: () -> Unit,
    onClearTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TimerTextWithDescription(
            time = hoursText,
            description = stringResource(id = R.string.timer_hours_description),
            color = textColor
        )
        TimerTextWithDescription(
            time = minutesText,
            description = stringResource(id = R.string.timer_minutes_description),
            color = textColor
        )
        TimerTextWithDescription(
            time = secondsText,
            description = stringResource(id = R.string.timer_seconds_description),
            color = textColor,
            paddingEnd = 8.dp
        )
        TimerIconButton(
            modifier = Modifier.padding(top = 12.dp),
            onClick = { onDeleteTimeClick() },
            onLongClick = { onClearTimeClick() },
            enabled = isTimeValid
        ) {
            Icon(
                imageVector = Icons.Outlined.Backspace,
                contentDescription = null,
                tint = buttonColor
            )
        }
    }
}

@Composable
fun TimerDivider() {
    Divider(
        modifier = Modifier.padding(top = 32.dp, bottom = 16.dp, start = 32.dp, end = 32.dp),
        color = Color.Gray,
        thickness = 1.dp
    )
}

@ExperimentalFoundationApi
@Composable
fun TimerButtonPad(onTimeClick: (Int) -> Unit) {
    for (i in 0 until 4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (j in 0 until 3) {
                if (i == 3 && (j == 0 || j == 2)) {
                    Spacer(modifier = Modifier.size(TimerButtonSize))
                } else {
                    val time = if (i == 3) 0 else i * 3 + j + 1
                    TimerButton(
                        modifier = Modifier.size(TimerButtonSize),
                        onClick = { onTimeClick(time) },
                        shape = CircleShape,
                        colors = ButtonDefaults.textButtonColors(),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = time.toString(),
                            style = MaterialTheme.typography.h4,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimerTextWithDescription(
    time: Int,
    description: String,
    color: Color,
    paddingEnd: Dp = 16.dp
) {
    Text(
        modifier = Modifier.padding(end = 4.dp),
        text = EditTimeFormatter.format(time),
        style = MaterialTheme.typography.h3,
        color = color
    )
    Text(
        modifier = Modifier.padding(top = 28.dp, end = paddingEnd),
        text = description,
        style = MaterialTheme.typography.h6,
        color = color
    )
}

@ExperimentalFoundationApi
@Composable
fun TimerActionView(
    screen: Screen,
    isTimeValid: Boolean,
    onStartCountingDownTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (screen == Screen.Editing && isTimeValid) {
            FloatingActionButton(
                modifier = Modifier.size(FabSize),
                onClick = { onStartCountingDownTimeClick() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Custom Composable functions
// -----------------------------------------------------------------------------------------

@ExperimentalFoundationApi
@Composable
fun TimerIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, radius = RippleRadius)
            )
            .then(IconButtonSizeModifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

@ExperimentalFoundationApi
@Composable
fun TimerButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    Surface(
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onLongClick = onLongClick,
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactionSource,
            indication = null
        )
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = MaterialTheme.typography.button
            ) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minWidth = ButtonDefaults.MinWidth,
                            minHeight = ButtonDefaults.MinHeight
                        )
                        .indication(interactionSource, rememberRipple())
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun TimerCircle(countDownTime: Int, elapsedTime: Int) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val height = size.height
        val width = size.width
        val dotDiameter = 12.dp
        val strokeSize = 20.dp
        val radiusOffset = max(strokeSize.value, max(dotDiameter.value, 0f))

        val xCenter = width / 2f
        val yCenter = height / 2f
        val radius = min(xCenter, yCenter)
        val arcWidthHeight = ((radius - radiusOffset) * 2f)
        val arcSize = Size(arcWidthHeight, arcWidthHeight)

        val remainderPercent = min(1f, elapsedTime.toFloat() / countDownTime.toFloat())
        val completedPercent = 1 - remainderPercent

        drawArc(
            color = teal200,
            startAngle = 270f,
            sweepAngle = -360f * completedPercent,
            useCenter = false,
            topLeft = Offset(radiusOffset, radiusOffset),
            size = arcSize,
            style = Stroke(width = strokeSize.value, cap = StrokeCap.Round)
        )
    }
}
