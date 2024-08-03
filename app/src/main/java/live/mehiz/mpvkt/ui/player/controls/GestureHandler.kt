package live.mehiz.mpvkt.ui.player.controls

import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.components.LeftSideOvalShape
import live.mehiz.mpvkt.presentation.components.RightSideOvalShape
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.controls.components.DoubleTapSeekSecondsView
import live.mehiz.mpvkt.ui.theme.PlayerRippleTheme
import org.koin.compose.koinInject

@Suppress("CyclomaticComplexMethod", "MultipleEmitters")
@Composable
fun GestureHandler(modifier: Modifier = Modifier) {
  val viewModel = koinInject<PlayerViewModel>()
  val playerPreferences = koinInject<PlayerPreferences>()
  val duration by viewModel.duration.collectAsState()
  val position by viewModel.pos.collectAsState()
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  var seekAmount by remember { mutableIntStateOf(0) }
  var isSeekingForwards by remember { mutableStateOf(true) }
  var targetAlpha by remember { mutableFloatStateOf(0f) }
  val alpha by animateFloatAsState(
    targetAlpha,
    animationSpec = tween(300),
    label = "doubletapseekalpha",
  )
  LaunchedEffect(seekAmount) {
    delay(600)
    targetAlpha = 0f
    delay(200)
    seekAmount = 0
    delay(100)
    viewModel.hideSeekBar()
  }
  val interactionSource = remember { MutableInteractionSource() }
  val doubleTapToPause by playerPreferences.doubleTapToPause.collectAsState()
  val doubleTapToSeek by playerPreferences.doubleTapToSeek.collectAsState()
  val doubleTapToSeekDuration by playerPreferences.doubleTapToSeekDuration.collectAsState()
  val brightnessGesture = playerPreferences.brightnessGesture.get()
  val volumeGesture = playerPreferences.volumeGesture.get()
  val seekGesture = playerPreferences.horizontalSeekGesture.get()
  val defaultSpeed by playerPreferences.defaultSpeed.collectAsState()
  val doubleTapSeek: (Offset, IntSize) -> Unit = { offset, size ->
    targetAlpha = 0.2f
    val isForward = offset.x > 3 * size.width / 5
    if (isForward != isSeekingForwards) seekAmount = 0
    isSeekingForwards = isForward
    seekAmount += if (isSeekingForwards && position < duration) {
      doubleTapToSeekDuration
    } else if (!isSeekingForwards && position > 0) {
      -doubleTapToSeekDuration
    } else {
      0
    }
    viewModel.seekBy(if (isSeekingForwards) doubleTapToSeekDuration else -doubleTapToSeekDuration)
    viewModel.showSeekBar()
  }
  var isLongPressing by remember { mutableStateOf(false) }
  val currentVolume by viewModel.currentVolume.collectAsState()
  val currentBrightness by viewModel.currentBrightness.collectAsState()
  val haptics = LocalHapticFeedback.current
  Box(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeGestures)
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = {
            if (controlsShown) {
              viewModel.hideControls()
            } else {
              viewModel.showControls()
            }
          },
          onDoubleTap = {
            if (areControlsLocked) return@detectTapGestures
            if (!doubleTapToSeek && doubleTapToPause) {
              viewModel.pauseUnpause()
              return@detectTapGestures
            }
            // divided by 2 fifths
            if (doubleTapToSeek && (it.x > size.width * 3 / 5 || it.x < size.width * 2 / 5)) {
              doubleTapSeek(it, size)
            } else if (doubleTapToPause) {
              viewModel.pauseUnpause()
            }
          },
          onPress = {
            val press = PressInteraction.Press(
              it.copy(x = if (it.x > size.width * 3 / 5) it.x - size.width * 0.6f else it.x),
            )
            interactionSource.emit(press)
            tryAwaitRelease()
            if (isLongPressing) {
              isLongPressing = false
              MPVLib.setPropertyDouble("speed", defaultSpeed.toDouble())
              viewModel.playerUpdate.update { PlayerUpdates.None }
            }
            interactionSource.emit(PressInteraction.Release(press))
          },
          onLongPress = {
            if (!isLongPressing && !viewModel.paused.value) {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              isLongPressing = true
              MPVLib.setPropertyDouble("speed", 2.0)
              viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed }
            }
          },
        )
      }
      .pointerInput(Unit) {
        if (!seekGesture || areControlsLocked) return@pointerInput
        var startingPosition = position
        var wasPlayerAlreadyPause = false
        detectHorizontalDragGestures(
          onDragStart = {
            startingPosition = position
            wasPlayerAlreadyPause = viewModel.paused.value
            viewModel.pause()
          },
          onDragEnd = {
            viewModel.gestureSeekAmount.update { 0 }
            viewModel.hideSeekBar()
            if (!wasPlayerAlreadyPause) viewModel.unpause()
          },
        ) { change, dragAmount ->
          if (position >= duration && dragAmount > 0) return@detectHorizontalDragGestures
          if (position <= 0f && dragAmount < 0) return@detectHorizontalDragGestures
          viewModel.showSeekBar()
          val seekBy = ((dragAmount * 150f / size.width).coerceIn(0f - position, duration - position)).toInt()
          viewModel.seekBy(seekBy)
          viewModel.gestureSeekAmount.update { (position - startingPosition).toInt() }
        }
      }
      .pointerInput(Unit) {
        if ((!brightnessGesture && !volumeGesture) || areControlsLocked) return@pointerInput
        var startingY = 0f
        var originalVolume = currentVolume
        var originalBrightness = currentBrightness
        val brightnessGestureSens = 0.001f
        val volumeGestureSens = 0.03f
        detectVerticalDragGestures(
          onDragEnd = { startingY = 0f },
          onDragStart = {
            startingY = it.y
            originalVolume = currentVolume
            originalBrightness = currentBrightness
          },
        ) { change, amount ->
          when {
            volumeGesture && brightnessGesture -> {
              if (change.position.x < size.width / 2) {
                viewModel.changeBrightnessTo(
                  originalBrightness + ((startingY - change.position.y) * brightnessGestureSens),
                )
              } else {
                viewModel.changeVolumeTo(
                  originalVolume + ((startingY - change.position.y) * volumeGestureSens).toInt(),
                )
              }
            }

            brightnessGesture -> {
              viewModel.changeBrightnessTo(
                originalBrightness + ((startingY - change.position.y) * brightnessGestureSens),
              )
            }
            // it's not always true, AS is drunk
            volumeGesture -> {
              viewModel.changeVolumeTo(
                originalVolume + ((startingY - change.position.y) * volumeGestureSens).toInt(),
              )
            }

            else -> {}
          }
        }
      },
  )
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = if (isSeekingForwards) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    CompositionLocalProvider(
      LocalRippleTheme provides PlayerRippleTheme,
    ) {
      if (seekAmount != 0) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.4f), // 2 fifths
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clip(if (isSeekingForwards) RightSideOvalShape else LeftSideOvalShape)
              .background(Color.White.copy(alpha))
              .indication(interactionSource, rememberRipple()),
          )
          AndroidView(
            factory = { DoubleTapSeekSecondsView(it, null) },
            update = {
              if (seekAmount != 0) {
                it.isForward = isSeekingForwards
                it.seconds = seekAmount
                it.visibility = View.VISIBLE
                it.start()
              } else {
                it.visibility = View.GONE
              }
            },
          )
        }
      }
    }
  }
}
