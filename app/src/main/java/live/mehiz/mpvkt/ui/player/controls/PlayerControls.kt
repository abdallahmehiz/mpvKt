package live.mehiz.mpvkt.ui.player.controls

import android.os.Build
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.ui.custombuttons.getButtons
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.components.BrightnessSlider
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.MultipleSpeedPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.TextPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.VolumeSlider
import live.mehiz.mpvkt.ui.player.controls.components.sheets.toFixed
import live.mehiz.mpvkt.ui.theme.playerRippleConfiguration
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject
import kotlin.math.abs

@Suppress("CompositionLocalAllowlist")
val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3Api::class)
@Composable
@Suppress("CyclomaticComplexMethod", "ViewModelForwarding")
fun PlayerControls(
  viewModel: PlayerViewModel,
  onBackPress: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val spacing = MaterialTheme.spacing
  val playerPreferences = koinInject<PlayerPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val interactionSource = remember { MutableInteractionSource() }
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekBarShown by viewModel.seekBarShown.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val duration by viewModel.duration.collectAsState()
  val position by viewModel.pos.collectAsState()
  val paused by viewModel.paused.collectAsState()
  val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
  val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val showDoubleTapOvals by playerPreferences.showDoubleTapOvals.collectAsState()
  val showSeekIcon by playerPreferences.showSeekIcon.collectAsState()
  val showSeekTime by playerPreferences.showSeekTimeWhileSeeking.collectAsState()
  var isSeeking by remember { mutableStateOf(false) }
  var resetControls by remember { mutableStateOf(true) }
  val currentChapter by viewModel.currentChapter.collectAsState()
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val onOpenSheet: (Sheets) -> Unit = {
    viewModel.sheetShown.update { _ -> it }
    if (it == Sheets.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.panelShown.update { Panels.None }
    }
  }
  val onOpenPanel: (Panels) -> Unit = {
    viewModel.panelShown.update { _ -> it }
    if (it == Panels.None) {
      viewModel.showControls()
    } else {
      viewModel.hideControls()
      viewModel.sheetShown.update { Sheets.None }
    }
  }
  val customButtons by viewModel.customButtons.collectAsState()
  val primaryCustomButtonId by playerPreferences.primaryCustomButtonId.collectAsState()
  val customButtonsList by remember {
    derivedStateOf {
      customButtons.getButtons().filter { it.showInPlayer }
    }
  }
  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControls,
  ) {
    if (controlsShown && !paused && !isSeeking) {
      delay(playerTimeToDisappear.toLong())
      viewModel.hideControls()
    }
  }
  val transparentOverlay by animateFloatAsState(
    if (controlsShown && !areControlsLocked) .8f else 0f,
    animationSpec = playerControlsExitAnimationSpec(),
    label = "controls_transparent_overlay",
  )
  GestureHandler(
    viewModel = viewModel,
    interactionSource = interactionSource,
  )
  DoubleTapToSeekOvals(doubleTapSeekAmount, showDoubleTapOvals, showSeekIcon, showSeekTime, interactionSource)
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
        val (bottomRightControls, bottomCenterControls, bottomLeftControls) = createRefs()
        val playerPauseButton = createRef()
        val seekbar = createRef()
        val (playerUpdates) = createRefs()

        val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsState()
        val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsState()
        val brightness by viewModel.currentBrightness.collectAsState()
        val volume by viewModel.currentVolume.collectAsState()
        val mpvVolume by viewModel.currentMPVVolume.collectAsState()
        val swapVolumeAndBrightness by playerPreferences.swapVolumeAndBrightness.collectAsState()
        val reduceMotion by playerPreferences.reduceMotion.collectAsState()

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
          enter =
          if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) {
              if (swapVolumeAndBrightness) -it else it
            } +
              fadeIn(
                playerControlsEnterAnimationSpec(),
              )
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit =
          if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) {
              if (swapVolumeAndBrightness) -it else it
            } +
              fadeOut(
                playerControlsExitAnimationSpec(),
              )
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(brightnessSlider) {
            if (swapVolumeAndBrightness) {
              start.linkTo(parent.start, spacing.medium)
            } else {
              end.linkTo(parent.end, spacing.medium)
            }
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          },
        ) { BrightnessSlider(brightness, 0f..1f) }

        AnimatedVisibility(
          isVolumeSliderShown,
          enter =
          if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) {
              if (swapVolumeAndBrightness) it else -it
            } +
              fadeIn(
                playerControlsEnterAnimationSpec(),
              )
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit =
          if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) {
              if (swapVolumeAndBrightness) it else -it
            } +
              fadeOut(
                playerControlsExitAnimationSpec(),
              )
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(volumeSlider) {
            if (swapVolumeAndBrightness) {
              end.linkTo(parent.end, spacing.medium)
            } else {
              start.linkTo(parent.start, spacing.medium)
            }
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
        val holdForMultipleSpeed by playerPreferences.holdForMultipleSpeed.collectAsState()
        val currentPlayerUpdate by viewModel.playerUpdate.collectAsState()
        val aspectRatio by playerPreferences.videoAspect.collectAsState()
        LaunchedEffect(currentPlayerUpdate, aspectRatio) {
          if (currentPlayerUpdate is PlayerUpdates.MultipleSpeed || currentPlayerUpdate is PlayerUpdates.None) {
            return@LaunchedEffect
          }
          delay(2000)
          viewModel.playerUpdate.update { PlayerUpdates.None }
        }
        AnimatedVisibility(
          currentPlayerUpdate !is PlayerUpdates.None,
          enter = fadeIn(playerControlsEnterAnimationSpec()),
          exit = fadeOut(playerControlsExitAnimationSpec()),
          modifier = Modifier.constrainAs(playerUpdates) {
            linkTo(parent.start, parent.end)
            linkTo(parent.top, parent.bottom, bias = 0.2f)
          },
        ) {
          when (currentPlayerUpdate) {
            is PlayerUpdates.MultipleSpeed -> MultipleSpeedPlayerUpdate(currentSpeed = holdForMultipleSpeed)
            is PlayerUpdates.AspectRatio -> TextPlayerUpdate(stringResource(aspectRatio.titleRes))
            is PlayerUpdates.ShowText -> TextPlayerUpdate((currentPlayerUpdate as PlayerUpdates.ShowText).value)
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
                  shadow = Shadow(Color.Black, blurRadius = 5f),
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
                  onClick = viewModel::pauseUnpause,
                )
                .padding(MaterialTheme.spacing.medium),
              contentDescription = null,
            )
          }
        }
        AnimatedVisibility(
          visible = (controlsShown || seekBarShown) && !areControlsLocked,
          enter = if (!reduceMotion) {
            slideInVertically(playerControlsEnterAnimationSpec()) { it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutVertically(playerControlsExitAnimationSpec()) { it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
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
        val mediaTitle by viewModel.mediaTitle.collectAsState()
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(topLeftControls) {
            top.linkTo(parent.top, spacing.medium)
            start.linkTo(parent.start)
            width = Dimension.fillToConstraints
            end.linkTo(topRightControls.start)
          },
        ) {
          TopLeftPlayerControls(
            mediaTitle = mediaTitle,
            onBackClick = onBackPress,
          )
        }
        // Top right controls
        val decoder by viewModel.currentDecoder.collectAsState()
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) { it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(topRightControls) {
            top.linkTo(parent.top, spacing.medium)
            end.linkTo(parent.end)
          },
        ) {
          TopRightPlayerControls(
            decoder = decoder,
            onDecoderClick = { viewModel.cycleDecoders() },
            onDecoderLongClick = { onOpenSheet(Sheets.Decoders) },
            isChaptersVisible = viewModel.chapters.isNotEmpty(),
            onChaptersClick = { onOpenSheet(Sheets.Chapters) },
            onSubtitlesClick = { onOpenSheet(Sheets.SubtitleTracks) },
            onSubtitlesLongClick = { onOpenPanel(Panels.SubtitleSettings) },
            onAudioClick = { onOpenSheet(Sheets.AudioTracks) },
            onAudioLongClick = { onOpenPanel(Panels.AudioDelay) },
            onMoreClick = { onOpenSheet(Sheets.More) },
            onMoreLongClick = { onOpenPanel(Panels.VideoFilters) },
          )
        }
        // Bottom right controls
        val customButtonTitle by viewModel.primaryButtonTitle.collectAsState()
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) { it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(bottomRightControls) {
            bottom.linkTo(seekbar.top)
            end.linkTo(seekbar.end)
            width = Dimension.wrapContent
          },
        ) {
          val activity = LocalContext.current as PlayerActivity
          BottomRightPlayerControls(
            isPipAvailable = activity.isPipSupported,
            onPipClick = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.enterPictureInPictureMode(activity.createPipParams())
              } else {
                activity.enterPictureInPictureMode()
              }
            },
            onAspectClick = {
              viewModel.changeVideoAspect(
                when (aspectRatio) {
                  VideoAspect.Fit -> VideoAspect.Stretch
                  VideoAspect.Stretch -> VideoAspect.Crop
                  VideoAspect.Crop -> VideoAspect.Fit
                },
              )
            },
          )
        }
        // Bottom left controls
        val playbackSpeed by viewModel.playbackSpeed.collectAsState()
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) { -it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) { -it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(bottomLeftControls) {
            bottom.linkTo(seekbar.top)
            start.linkTo(seekbar.start)
            width = Dimension.wrapContent
          },
        ) {
          BottomLeftPlayerControls(
            playbackSpeed,
            currentChapter = currentChapter,
            onLockControls = viewModel::lockControls,
            onCycleRotation = viewModel::cycleScreenRotations,
            onPlaybackSpeedChange = {
              MPVLib.setPropertyDouble("speed", it.toDouble())
            },
            onOpenSheet = onOpenSheet,
          )
        }
        // Bottom center controls
        AnimatedVisibility(
          visible = controlsShown && !areControlsLocked,
          enter = if (!reduceMotion) {
            slideInHorizontally(playerControlsEnterAnimationSpec()) { it } +
              fadeIn(playerControlsEnterAnimationSpec())
          } else {
            fadeIn(playerControlsEnterAnimationSpec())
          },
          exit = if (!reduceMotion) {
            slideOutHorizontally(playerControlsExitAnimationSpec()) { it } +
              fadeOut(playerControlsExitAnimationSpec())
          } else {
            fadeOut(playerControlsExitAnimationSpec())
          },
          modifier = Modifier.constrainAs(bottomCenterControls) {
            start.linkTo(bottomLeftControls.end)
            end.linkTo(bottomRightControls.start)
            bottom.linkTo(seekbar.top)
            width = Dimension.fillToConstraints
          }
        ) {
          BottomCenterPlayerControls(
            customButtons = customButtonsList,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small)
          )
        }
      }
    }
    val sheetShown by viewModel.sheetShown.collectAsState()
    val subtitles by viewModel.subtitleTracks.collectAsState()
    val selectedSubtitles by viewModel.selectedSubtitles.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val selectedAudio by viewModel.selectedAudio.collectAsState()
    val decoder by viewModel.currentDecoder.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()
    PlayerSheets(
      sheetShown = sheetShown,
      subtitles = subtitles.toImmutableList(),
      selectedSubtitles = selectedSubtitles.toList().toImmutableList(),
      onAddSubtitle = viewModel::addSubtitle,
      onSelectSubtitle = viewModel::selectSub,
      audioTracks = audioTracks.toImmutableList(),
      selectedAudio = selectedAudio,
      onAddAudio = viewModel::addAudio,
      onSelectAudio = viewModel::selectAudio,
      chapter = currentChapter,
      chapters = viewModel.chapters.toImmutableList(),
      onSeekToChapter = {
        viewModel.selectChapter(it)
        viewModel.unpause()
      },
      decoder = decoder,
      onUpdateDecoder = viewModel::updateDecoder,
      speed = speed,
      onSpeedChange = { MPVLib.setPropertyDouble("speed", it.toFixed(2).toDouble()) },
      sleepTimerTimeRemaining = sleepTimerTimeRemaining,
      onStartSleepTimer = viewModel::startTimer,
      buttons = customButtons.getButtons().filter { it.showInMoreSheet }.toImmutableList(),
      onOpenPanel = onOpenPanel,
      onDismissRequest = { onOpenSheet(Sheets.None) },
    )
    val panel by viewModel.panelShown.collectAsState()
    PlayerPanels(
      panelShown = panel,
      onDismissRequest = { onOpenPanel(Panels.None) },
    )
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
