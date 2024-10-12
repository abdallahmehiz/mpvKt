package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.controls.components.BrightnessSlider
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.DoubleSpeedPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.TextPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.VolumeSlider
import live.mehiz.mpvkt.ui.theme.playerRippleConfiguration
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject
import kotlin.math.abs

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3Api::class)
@Composable
@Suppress("CyclomaticComplexMethod")
fun PlayerControls(
  modifier: Modifier = Modifier,
) {
  val viewModel = koinInject<PlayerViewModel>()
  val spacing = MaterialTheme.spacing
  val playerPreferences = koinInject<PlayerPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekBarShown by viewModel.seekBarShown.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val duration by viewModel.duration.collectAsState()
  val position by viewModel.pos.collectAsState()
  val paused by viewModel.paused.collectAsState()
  val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
  var isSeeking by remember { mutableStateOf(false) }
  var resetControls by remember { mutableStateOf(true) }
  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControls,
  ) {
    if (controlsShown && !paused && !isSeeking) {
      delay(4_000)
      viewModel.hideControls()
    }
  }
  val transparentOverlay by animateFloatAsState(
    if (controlsShown && !areControlsLocked) .9f else 0f,
    animationSpec = playerControlsExitAnimationSpec(),
    label = "controls_transparent_overlay",
  )
  GestureHandler()
  CompositionLocalProvider(
    LocalRippleConfiguration provides playerRippleConfiguration,
    LocalPlayerButtonsClickEvent provides { resetControls = !resetControls },
    LocalContentColor provides Color.White,
  ) {
    CompositionLocalProvider(
      LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
      ConstraintLayout(
        modifier = modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              Pair(0f, Color.Black),
              Pair(.2f, Color.Transparent),
              Pair(.7f, Color.Transparent),
              Pair(1f, Color.Black),
            ),
            alpha = transparentOverlay,
          )
          .padding(horizontal = MaterialTheme.spacing.medium),
      ) {
        val (topLeftControls, topRightControls) = createRefs()
        val (volumeSlider, brightnessSlider) = createRefs()
        val unlockControlsButton = createRef()
        val (bottomRightControls, bottomLeftControls) = createRefs()
        val playerPauseButton = createRef()
        val seekbar = createRef()
        val (playerUpdates) = createRefs()

        val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
        val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
        val brightness by viewModel.currentBrightness.collectAsState()
        val volume by viewModel.currentVolume.collectAsState()
        val mpvVolume by viewModel.currentMPVVolume.collectAsState()

        LaunchedEffect(volume, mpvVolume, isVolumeSliderShown) {
          delay(2000)
          if (isVolumeSliderShown) viewModel.isVolumeSliderShown.update { false }
        }
        LaunchedEffect(brightness, isBrightnessSliderShown) {
          delay(2000)
          if (isBrightnessSliderShown) viewModel.isBrightnessSliderShown.update { false }
        }
        AnimatedVisibility(
          isBrightnessSliderShown,
          enter = slideInHorizontally(playerControlsEnterAnimationSpec()) { it } + fadeIn(
            playerControlsEnterAnimationSpec(),
          ),
          exit = slideOutHorizontally(playerControlsExitAnimationSpec()) { it } + fadeOut(
            playerControlsExitAnimationSpec(),
          ),
          modifier = Modifier.constrainAs(brightnessSlider) {
            end.linkTo(parent.end, spacing.medium)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          },
        ) { BrightnessSlider(brightness, 0f..1f) }

        AnimatedVisibility(
          isVolumeSliderShown,
          enter = slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } + fadeIn(
            playerControlsEnterAnimationSpec(),
          ),
          exit = slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } + fadeOut(
            playerControlsExitAnimationSpec(),
          ),
          modifier = Modifier.constrainAs(volumeSlider) {
            start.linkTo(parent.start, spacing.medium)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          },
        ) {
          val boostCap by audioPreferences.volumeBoostCap.collectAsState()
          val displayVolumeAsPercentage by playerPreferences.displayVolumeAsPercentage.collectAsState()
          VolumeSlider(
            volume,
            mpvVolume = mpvVolume,
            range = 0..viewModel.maxVolume,
            boostRange = if (boostCap > 0) 0..audioPreferences.volumeBoostCap.get() else null,
            displayAsPercentage = displayVolumeAsPercentage,
          )
        }

        val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
        val aspectRatio by playerPreferences.videoAspect.collectAsState()
        LaunchedEffect(currentPlayerUpdate, aspectRatio) {
          if (currentPlayerUpdate == PlayerUpdates.DoubleSpeed || currentPlayerUpdate == PlayerUpdates.None) {
            return@LaunchedEffect
          }
          delay(2000)
          viewModel.playerUpdate.update { PlayerUpdates.None }
        }
        AnimatedVisibility(
          currentPlayerUpdate != PlayerUpdates.None,
          enter = fadeIn(playerControlsEnterAnimationSpec()),
          exit = fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(playerUpdates) {
            linkTo(parent.start, parent.end)
            linkTo(parent.top, parent.bottom, bias = 0.2f)
          },
        ) {
          when (currentPlayerUpdate) {
            PlayerUpdates.DoubleSpeed -> DoubleSpeedPlayerUpdate()
            PlayerUpdates.AspectRatio -> TextPlayerUpdate(stringResource(aspectRatio.titleRes))
            else -> {}
          }
        }

        AnimatedVisibility(
          controlsShown && areControlsLocked,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.constrainAs(unlockControlsButton) {
            top.linkTo(parent.top, spacing.medium)
            start.linkTo(parent.start, spacing.medium)
          },
        ) {
          ControlsButton(
            Icons.Filled.Lock,
            onClick = { viewModel.unlockControls() },
          )
        }
        AnimatedVisibility(
          visible = (controlsShown && !areControlsLocked || gestureSeekAmount != null) || isLoading,
          enter = fadeIn(playerControlsEnterAnimationSpec()),
          exit = fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(playerPauseButton) {
            end.linkTo(parent.absoluteRight)
            start.linkTo(parent.absoluteLeft)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          },
        ) {
          val showLoadingCircle by playerPreferences.showLoadingCircle.collectAsState()
          val icon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
          val interaction = remember { MutableInteractionSource() }
          when {
            gestureSeekAmount != null -> {
              Text(
                stringResource(
                  R.string.player_gesture_seek_indicator,
                  if (gestureSeekAmount!!.second >= 0) '+' else '-',
                  Utils.prettyTime(abs(gestureSeekAmount!!.second)),
                  Utils.prettyTime(gestureSeekAmount!!.first + gestureSeekAmount!!.second),
                ),
                style = MaterialTheme.typography.headlineMedium.copy(
                  shadow = Shadow(Color.Black, blurRadius = 5f)
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
            }

            isLoading && showLoadingCircle -> CircularProgressIndicator(Modifier.size(96.dp))
            controlsShown && !areControlsLocked -> Image(
              painter = rememberAnimatedVectorPainter(icon, !paused),
              modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .clickable(
                  interaction,
                  ripple(),
                ) { viewModel.pauseUnpause() }
                .padding(MaterialTheme.spacing.medium),
              contentDescription = null,
            )
          }
        }
        AnimatedVisibility(
          visible = (controlsShown || seekBarShown) && !areControlsLocked,
          enter = slideInVertically(playerControlsEnterAnimationSpec()) { it } +
            fadeIn(playerControlsEnterAnimationSpec()),
          exit = slideOutVertically(playerControlsExitAnimationSpec()) { it } +
            fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(seekbar) {
            bottom.linkTo(parent.bottom, spacing.medium)
          },
        ) {
          val invertDuration by playerPreferences.invertDuration.collectAsState()
          val readAhead by viewModel.readAhead.collectAsState()
          val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()
          SeekbarWithTimers(
            position = position,
            duration = duration,
            readAheadValue = readAhead,
            onValueChange = {
              isSeeking = true
              viewModel.updatePlayBackPos(it)
              viewModel.seekTo(it.toInt(), preciseSeeking)
            },
            onValueChangeFinished = { isSeeking = false },
            timersInverted = Pair(false, invertDuration),
            durationTimerOnCLick = { playerPreferences.invertDuration.set(!invertDuration) },
            positionTimerOnClick = {},
            chapters = viewModel.chapters.toImmutableList(),
          )
        }
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
            fadeIn(playerControlsEnterAnimationSpec()),
          exit = slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } +
            fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(topLeftControls) {
            top.linkTo(parent.top, spacing.medium)
            start.linkTo(parent.start)
            width = Dimension.fillToConstraints
            end.linkTo(topRightControls.start)
          },
        ) { TopLeftPlayerControls() }
        // Top right controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
            fadeIn(playerControlsEnterAnimationSpec()),
          exit = slideOutHorizontally(playerControlsExitAnimationSpec()) { it } +
            fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(topRightControls) {
            top.linkTo(parent.top, spacing.medium)
            end.linkTo(parent.end)
          },
        ) { TopRightPlayerControls() }
        // Bottom right controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
            fadeIn(playerControlsEnterAnimationSpec()),
          exit = slideOutHorizontally(playerControlsExitAnimationSpec()) { it } +
            fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(bottomRightControls) {
            bottom.linkTo(seekbar.top)
            end.linkTo(seekbar.end)
          },
        ) { BottomRightPlayerControls() }
        // Bottom left controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
            fadeIn(playerControlsEnterAnimationSpec()),
          exit = slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } +
            fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(bottomLeftControls) {
            bottom.linkTo(seekbar.top)
            start.linkTo(seekbar.start)
            width = Dimension.fillToConstraints
            end.linkTo(bottomRightControls.start)
          },
        ) { BottomLeftPlayerControls() }
      }
    }
    PlayerSheets()
    PlayerPanels()
  }
}

fun <T> playerControlsExitAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 300,
  easing = FastOutSlowInEasing,
)

fun <T> playerControlsEnterAnimationSpec(): FiniteAnimationSpec<T> = tween(
  durationMillis = 100,
  easing = LinearOutSlowInEasing,
)
