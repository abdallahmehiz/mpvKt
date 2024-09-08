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
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.components.LeftSideOvalShape
import live.mehiz.mpvkt.presentation.components.RightSideOvalShape
import live.mehiz.mpvkt.ui.player.Panels
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
  val audioPreferences = koinInject<AudioPreferences>()
  val panelShown by viewModel.panelShown.collectAsState()
  val allowGesturesInPanels by playerPreferences.allowGesturesInPanels.collectAsState()
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
  val volumeGesture by playerPreferences.volumeGesture.collectAsState()
  val seekGesture by playerPreferences.horizontalSeekGesture.collectAsState()
  val showSeekbarWhenSeeking by playerPreferences.showSeekBarWhenSeeking.collectAsState()
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
    if (showSeekbarWhenSeeking) viewModel.showSeekBar()
  }
  var isLongPressing by remember { mutableStateOf(false) }
  val currentVolume by viewModel.currentVolume.collectAsState()
  val currentMPVVolume by viewModel.currentMPVVolume.collectAsState()
  val currentBrightness by viewModel.currentBrightness.collectAsState()
  val volumeBoostingCap = audioPreferences.volumeBoostCap.get()
  val haptics = LocalHapticFeedback.current
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
  Box(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeGestures)
      .pointerInput(Unit) {
        var originalSpeed = viewModel.playbackSpeed.value
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
            if (panelShown != Panels.None && !allowGesturesInPanels) {
              viewModel.panelShown.update { Panels.None }
            }
            val press = PressInteraction.Press(
              it.copy(x = if (it.x > size.width * 3 / 5) it.x - size.width * 0.6f else it.x),
            )
            interactionSource.emit(press)
            tryAwaitRelease()
            if (isLongPressing) {
              isLongPressing = false
              MPVLib.setPropertyDouble("speed", originalSpeed.toDouble())
              viewModel.playerUpdate.update { PlayerUpdates.None }
            }
            interactionSource.emit(PressInteraction.Release(press))
          },
          onLongPress = {
            if (areControlsLocked) return@detectTapGestures
            if (!isLongPressing && !viewModel.paused.value) {
              originalSpeed = viewModel.playbackSpeed.value
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              isLongPressing = true
              MPVLib.setPropertyDouble("speed", 2.0)
              viewModel.playerUpdate.update { PlayerUpdates.DoubleSpeed }
            }
          },
        )
      }
      .pointerInput(areControlsLocked) {
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
            viewModel.gestureSeekAmount.update { null }
            viewModel.hideSeekBar()
            if (!wasPlayerAlreadyPause) viewModel.unpause()
          },
        ) { change, dragAmount ->
          if (position >= duration && dragAmount > 0) return@detectHorizontalDragGestures
          if (position <= 0f && dragAmount < 0) return@detectHorizontalDragGestures
          val seekBy = ((dragAmount * 150f / size.width).coerceIn(0f - position, duration - position)).toInt()
          viewModel.seekBy(seekBy)
          viewModel.gestureSeekAmount.update {
            Pair(startingPosition.toInt(), (position - startingPosition).toInt())
          }
          if (showSeekbarWhenSeeking) viewModel.showSeekBar()
        }
      }
      .pointerInput(areControlsLocked) {
        if ((!brightnessGesture && !volumeGesture) || areControlsLocked) return@pointerInput
        var startingY = 0f
        var mpvVolumeStartingY = 0f
        var originalVolume = currentVolume
        var originalMPVVolume = currentMPVVolume
        var originalBrightness = currentBrightness
        val brightnessGestureSens = 0.001f
        val volumeGestureSens = 0.03f
        val mpvVolumeGestureSens = 0.02f
        val isIncreasingVolumeBoost: (Float) -> Boolean = {
          volumeBoostingCap > 0 && currentVolume == viewModel.maxVolume &&
            currentMPVVolume - 100 < volumeBoostingCap && it < 0
        }
        val isDecreasingVolumeBoost: (Float) -> Boolean = {
          volumeBoostingCap > 0 && currentVolume == viewModel.maxVolume &&
            currentMPVVolume - 100 in 1..volumeBoostingCap && it > 0
        }
        detectVerticalDragGestures(
          onDragEnd = { startingY = 0f },
          onDragStart = {
            startingY = 0f
            mpvVolumeStartingY = 0f
            originalVolume = currentVolume
            originalMPVVolume = currentMPVVolume
            originalBrightness = currentBrightness
          },
        ) { change, amount ->
          val changeVolume: () -> Unit = {
            if (isIncreasingVolumeBoost(amount) || isDecreasingVolumeBoost(amount)) {
              if (mpvVolumeStartingY == 0f) {
                startingY = 0f
                originalVolume = currentVolume
                mpvVolumeStartingY = change.position.y
              }
              viewModel.changeMPVVolumeTo(
                calculateNewValue(originalMPVVolume, mpvVolumeStartingY, change.position.y, mpvVolumeGestureSens)
                  .coerceIn(100..volumeBoostingCap + 100),
              )
            } else {
              if (startingY == 0f) {
                mpvVolumeStartingY = 0f
                originalMPVVolume = currentMPVVolume
                startingY = change.position.y
              }
              viewModel.changeVolumeTo(
                calculateNewValue(originalVolume, startingY, change.position.y, volumeGestureSens),
              )
            }
          }
          val changeBrightness: () -> Unit = {
            if (startingY == 0f) startingY = change.position.y
            viewModel.changeBrightnessTo(
              calculateNewValue(originalBrightness, startingY, change.position.y, brightnessGestureSens)
            )
          }
          when {
            volumeGesture && brightnessGesture -> {
              if (change.position.x < size.width / 2) changeBrightness() else changeVolume()
            }

            brightnessGesture -> changeBrightness()
            // it's not always true, AS is drunk
            volumeGesture -> changeVolume()
            else -> {}
          }
        }
      },
  )
}

fun calculateNewValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
  return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float {
  return originalValue + ((startingY - newY) * sensitivity)
}
