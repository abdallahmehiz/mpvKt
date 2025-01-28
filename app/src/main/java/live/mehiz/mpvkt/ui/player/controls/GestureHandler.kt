package live.mehiz.mpvkt.ui.player.controls

import android.view.View
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
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
import live.mehiz.mpvkt.ui.theme.playerRippleConfiguration
import org.koin.compose.koinInject

@Suppress("CyclomaticComplexMethod", "MultipleEmitters")
@Composable
fun GestureHandler(
  viewModel: PlayerViewModel,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val panelShown by viewModel.panelShown.collectAsState()
  val allowGesturesInPanels by playerPreferences.allowGesturesInPanels.collectAsState()
  val duration by viewModel.duration.collectAsState()
  val position by viewModel.pos.collectAsState()
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val isSeekingForwards by viewModel.isSeekingForwards.collectAsState()
  var isDoubleTapSeeking by remember { mutableStateOf(false) }
  LaunchedEffect(seekAmount) {
    delay(800)
    viewModel.updateSeekAmount(0)
    isDoubleTapSeeking = false
    delay(100)
    viewModel.hideSeekBar()
  }
  val doubleSpeedGesture by playerPreferences.holdForDoubleSpeed.collectAsState()
  val brightnessGesture = playerPreferences.brightnessGesture.get()
  val volumeGesture by playerPreferences.volumeGesture.collectAsState()
  val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
  val seekGesture by playerPreferences.horizontalSeekGesture.collectAsState()
  val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()
  val showSeekbarWhenSeeking by playerPreferences.showSeekBarWhenSeeking.collectAsState()
  var isLongPressing by remember { mutableStateOf(false) }
  val currentVolume by viewModel.currentVolume.collectAsState()
  val currentMPVVolume by viewModel.currentMPVVolume.collectAsState()
  val currentBrightness by viewModel.currentBrightness.collectAsState()
  val volumeBoostingCap = audioPreferences.volumeBoostCap.get()
  val haptics = LocalHapticFeedback.current
  Box(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.safeGestures)
      .pointerInput(Unit) {
        var originalSpeed = viewModel.playbackSpeed.value
        detectTapGestures(
          onTap = {
            if (controlsShown) viewModel.hideControls() else viewModel.showControls()
          },
          onDoubleTap = {
            if (areControlsLocked || isDoubleTapSeeking) return@detectTapGestures
            if (it.x > size.width * 3 / 5) {
              if (!isSeekingForwards) viewModel.updateSeekAmount(0)
              viewModel.handleRightDoubleTap()
              isDoubleTapSeeking = true
            } else if (it.x < size.width * 2 / 5) {
              if (isSeekingForwards) viewModel.updateSeekAmount(0)
              viewModel.handleLeftDoubleTap()
              isDoubleTapSeeking = true
            } else {
              viewModel.handleCenterDoubleTap()
            }
          },
          onPress = {
            if (panelShown != Panels.None && !allowGesturesInPanels) {
              viewModel.panelShown.update { Panels.None }
            }
            if (!areControlsLocked && isDoubleTapSeeking && seekAmount != 0) {
              if (it.x > size.width * 3 / 5) {
                if (!isSeekingForwards) viewModel.updateSeekAmount(0)
                viewModel.handleRightDoubleTap()
              } else if (it.x < size.width * 2 / 5) {
                if (isSeekingForwards) viewModel.updateSeekAmount(0)
                viewModel.handleLeftDoubleTap()
              } else {
                viewModel.handleCenterDoubleTap()
              }
            } else {
              isDoubleTapSeeking = false
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
            if (!doubleSpeedGesture || areControlsLocked) return@detectTapGestures
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
        var startingPosition = position.toInt()
        var startingX = 0f
        var wasPlayerAlreadyPause = false
        detectHorizontalDragGestures(
          onDragStart = {
            startingPosition = position.toInt()
            startingX = it.x
            wasPlayerAlreadyPause = viewModel.paused.value
            viewModel.pause()
          },
          onDragEnd = {
            viewModel.gestureSeekAmount.update { null }
            viewModel.hideSeekBar()
            if (!wasPlayerAlreadyPause) viewModel.unpause()
          },
        ) { change, dragAmount ->
          if (position <= 0f && dragAmount < 0) return@detectHorizontalDragGestures
          if (position >= duration && dragAmount > 0) return@detectHorizontalDragGestures
          calculateNewHorizontalGestureValue(startingPosition, startingX, change.position.x, 0.15f).let {
            viewModel.gestureSeekAmount.update { _ ->
              Pair(
                startingPosition,
                (it - startingPosition)
                  .coerceIn(0 - startingPosition, (duration - startingPosition).toInt()),
              )
            }
            viewModel.seekTo(it, preciseSeeking)
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
                calculateNewVerticalGestureValue(
                  originalMPVVolume,
                  mpvVolumeStartingY,
                  change.position.y,
                  mpvVolumeGestureSens,
                )
                  .coerceIn(100..volumeBoostingCap + 100),
              )
            } else {
              if (startingY == 0f) {
                mpvVolumeStartingY = 0f
                originalMPVVolume = currentMPVVolume
                startingY = change.position.y
              }
              viewModel.changeVolumeTo(
                calculateNewVerticalGestureValue(originalVolume, startingY, change.position.y, volumeGestureSens),
              )
            }
            viewModel.displayVolumeSlider()
          }
          val changeBrightness: () -> Unit = {
            if (startingY == 0f) startingY = change.position.y
            viewModel.changeBrightnessTo(
              calculateNewVerticalGestureValue(originalBrightness, startingY, change.position.y, brightnessGestureSens),
            )
            viewModel.displayBrightnessSlider()
          }
          when {
            volumeGesture && brightnessGesture -> {
              if (swapVolumeAndBrightness) {
                if (change.position.x > size.width / 2) changeBrightness() else changeVolume()
              } else {
                if (change.position.x < size.width / 2) changeBrightness() else changeVolume()
              }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoubleTapToSeekOvals(
  amount: Int,
  showOvals: Boolean,
  showSeekIcon: Boolean,
  showSeekTime: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  val alpha by animateFloatAsState(if (amount == 0) 0f else 0.2f, label = "double_tap_animation_alpha")
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = if (amount > 0) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    CompositionLocalProvider(
      LocalRippleConfiguration provides playerRippleConfiguration,
    ) {
      if (amount != 0) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.4f), // 2 fifths
          contentAlignment = Alignment.Center,
        ) {
          if (showOvals) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(if (amount > 0) RightSideOvalShape else LeftSideOvalShape)
                .background(Color.White.copy(alpha))
                .indication(interactionSource, ripple()),
            )
          }
          if (showSeekIcon || showSeekTime) {
            AndroidView(
              factory = { DoubleTapSeekSecondsView(it, showSeekIcon, showSeekTime, null) },
              update = {
                if (amount != 0) {
                  it.isForward = amount > 0
                  it.seconds = amount
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
}

fun calculateNewVerticalGestureValue(originalValue: Int, startingY: Float, newY: Float, sensitivity: Float): Int {
  return originalValue + ((startingY - newY) * sensitivity).toInt()
}

fun calculateNewVerticalGestureValue(originalValue: Float, startingY: Float, newY: Float, sensitivity: Float): Float {
  return originalValue + ((startingY - newY) * sensitivity)
}

fun calculateNewHorizontalGestureValue(originalValue: Int, startingX: Float, newX: Float, sensitivity: Float): Int {
  return originalValue + ((newX - startingX) * sensitivity).toInt()
}

fun calculateNewHorizontalGestureValue(originalValue: Float, startingX: Float, newX: Float, sensitivity: Float): Float {
  return originalValue + ((newX - startingX) * sensitivity)
}
