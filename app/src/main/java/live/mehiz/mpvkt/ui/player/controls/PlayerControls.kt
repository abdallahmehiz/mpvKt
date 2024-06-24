package live.mehiz.mpvkt.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.DoubleSpeedPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.TextPlayerUpdate
import live.mehiz.mpvkt.ui.theme.PlayerRippleTheme
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject

val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

class PlayerControls(private val viewModel: PlayerViewModel) {
  @OptIn(ExperimentalAnimationGraphicsApi::class)
  @Composable
  fun Content() {
    val spacing = MaterialTheme.spacing
    val playerPreferences = koinInject<PlayerPreferences>()
    val controlsShown by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val seekBarShown by viewModel.seekBarShown.collectAsState()
    val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val duration by viewModel.duration.collectAsState()
    LaunchedEffect(gestureSeekAmount) {
      if (gestureSeekAmount != 0) return@LaunchedEffect
      delay(3000)
      viewModel.hideSeekBar()
    }
    val position by viewModel.pos.collectAsState()
    val paused by viewModel.paused.collectAsState()
    var isSeeking by remember { mutableStateOf(false) }
    var resetControls by remember { mutableStateOf(true) }
    LaunchedEffect(
      controlsShown,
      paused,
      isSeeking,
      resetControls,
    ) {
      if (controlsShown && !paused && !isSeeking) {
        delay(3_000)
        viewModel.hideControls()
      }
    }
    val transparentOverlay by animateColorAsState(
      Color.Black.copy(if (controlsShown) 0.2f else 0f),
      label = "",
    )
    GestureHandler(viewModel)
    CompositionLocalProvider(
      LocalRippleTheme provides PlayerRippleTheme,
      LocalPlayerButtonsClickEvent provides { resetControls = !resetControls },
    ) {
      ConstraintLayout(
        modifier = Modifier
          .fillMaxSize()
          .background(transparentOverlay)
          .padding(horizontal = MaterialTheme.spacing.medium),
      ) {
        val (
          playerUpdates,
          seekbar,
          playerPauseButton,
          seekValue,
          topLeftControls,
          topRightControls,
          bottomLeftControls,
          bottomRightControls,
          unlockControlsButton,
        ) = createRefs()

        val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
        val aspectRatio by playerPreferences.videoAspect.collectAsState()
        LaunchedEffect(currentPlayerUpdate, aspectRatio) {
          if (currentPlayerUpdate == PlayerUpdates.DoubleSpeed || currentPlayerUpdate == PlayerUpdates.None)
            return@LaunchedEffect
          delay(2000)
          viewModel.playerUpdate.update { PlayerUpdates.None }
        }
        AnimatedVisibility(
          currentPlayerUpdate != PlayerUpdates.None,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.constrainAs(playerUpdates) {
            linkTo(parent.start, parent.end)
            linkTo(parent.top, parent.bottom, bias = 0.2f)
          },
        ) {
          val latestOne by remember { mutableStateOf(currentPlayerUpdate) }
          when (latestOne) {
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
            Icons.Filled.LockOpen,
            onClick = { viewModel.unlockControls() },
          )
        }
        AnimatedVisibility(
          visible = gestureSeekAmount != 0 && !areControlsLocked,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.constrainAs(seekValue) {
            top.linkTo(if (controlsShown) playerPauseButton.bottom else parent.top)
            start.linkTo(parent.absoluteLeft)
            end.linkTo(parent.absoluteRight)
            if (!controlsShown) bottom.linkTo(seekbar.top)
          },
        ) {
          Text(
            (if (gestureSeekAmount > 0) "+" else "") + gestureSeekAmount + "\n" + Utils.prettyTime(position.toInt()),
            style = MaterialTheme.typography.displayMedium.copy(shadow = Shadow(blurRadius = 5f)),
            color = Color.White,
            textAlign = TextAlign.Center,
          )
        }
        AnimatedVisibility(
          visible = (controlsShown && !areControlsLocked) || isLoading,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.constrainAs(playerPauseButton) {
            end.linkTo(parent.absoluteRight)
            start.linkTo(parent.absoluteLeft)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          },
        ) {
          val icon = AnimatedImageVector.animatedVectorResource(R.drawable.anim_play_to_pause)
          val interaction = remember { MutableInteractionSource() }
          if (isLoading) {
            CircularProgressIndicator(Modifier.size(96.dp))
          } else if (controlsShown && !areControlsLocked) {
            Image(
              painter = rememberAnimatedVectorPainter(icon, !paused),
              modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .clickable(
                  interaction,
                  rememberRipple(),
                ) { viewModel.pauseUnpause() },
              contentDescription = null,
            )
          }
        }
        AnimatedVisibility(
          visible = (controlsShown || seekBarShown) && !areControlsLocked,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          modifier = Modifier.constrainAs(seekbar) {
            bottom.linkTo(parent.bottom, spacing.medium)
          },
        ) {
          val invertDuration by playerPreferences.invertDuration.collectAsState()
          val readAhead by viewModel.readAhead.collectAsState()
          SeekbarWithTimers(
            position = position,
            duration = duration,
            readAheadValue = readAhead,
            onValueChange = {
              isSeeking = true
              viewModel.pause()
              viewModel.updatePlayBackPos(it)
              viewModel.seekTo(it.toInt())
            },
            onValueChangeFinished = {
              if (!paused) viewModel.unpause()
              isSeeking = false
            },
            timersInverted = Pair(false, invertDuration),
            durationTimerOnCLick = { playerPreferences.invertDuration.set(!invertDuration) },
            positionTimerOnClick = {},
            chapters = viewModel.chapters,
          )
        }
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally { -it } + fadeIn(),
          exit = slideOutHorizontally { -it } + fadeOut(),
          modifier = Modifier.constrainAs(topLeftControls) {
            top.linkTo(parent.top, spacing.medium)
            start.linkTo(parent.start)
            width = Dimension.fillToConstraints
            end.linkTo(topRightControls.start)
          },
        ) { TopLeftPlayerControls(viewModel) }
        // Top right controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
          exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
          modifier = Modifier.constrainAs(topRightControls) {
            top.linkTo(parent.top, spacing.medium)
            end.linkTo(parent.end)
          },
        ) { TopRightPlayerControls(viewModel) }
        // Bottom right controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
          exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
          modifier = Modifier.constrainAs(bottomRightControls) {
            bottom.linkTo(seekbar.top)
            end.linkTo(seekbar.end)
          },
        ) { BottomRightPlayerControls(viewModel) }
        // Bottom left controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
          exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
          modifier = Modifier.constrainAs(bottomLeftControls) {
            bottom.linkTo(seekbar.top)
            start.linkTo(seekbar.start)
            width = Dimension.fillToConstraints
            end.linkTo(bottomRightControls.start)
          },
        ) { BottomLeftPlayerControls(viewModel) }
      }
      PlayerSheets(viewModel)
    }
  }
}
