package live.mehiz.mpvkt.ui.player.controls

import android.os.Build
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.graphics.toColorInt
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.Utils
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AudioChannels
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitleJustification
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.preferences.preference.minusAssign
import live.mehiz.mpvkt.preferences.preference.plusAssign
import live.mehiz.mpvkt.ui.custombuttons.getButtons
import live.mehiz.mpvkt.ui.player.DebandSettings
import live.mehiz.mpvkt.ui.player.Debanding
import live.mehiz.mpvkt.ui.player.Decoder.Companion.getDecoderFromValue
import live.mehiz.mpvkt.ui.player.Panels
import live.mehiz.mpvkt.ui.player.PlayerActivity
import live.mehiz.mpvkt.ui.player.PlayerUpdates
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.VideoFilters
import live.mehiz.mpvkt.ui.player.collectAsState
import live.mehiz.mpvkt.ui.player.controls.components.BrightnessSlider
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.MultipleSpeedPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.TextPlayerUpdate
import live.mehiz.mpvkt.ui.player.controls.components.VolumeSlider
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubColorType
import live.mehiz.mpvkt.ui.player.controls.components.panels.SubtitlesBorderStyle
import live.mehiz.mpvkt.ui.player.controls.components.panels.resetColors
import live.mehiz.mpvkt.ui.player.controls.components.panels.resetTypography
import live.mehiz.mpvkt.ui.player.controls.components.panels.toColorHexString
import live.mehiz.mpvkt.ui.player.controls.components.sheets.toFixed
import live.mehiz.mpvkt.ui.player.execute
import live.mehiz.mpvkt.ui.player.executeLongClick
import live.mehiz.mpvkt.ui.theme.playerRippleConfiguration
import live.mehiz.mpvkt.ui.theme.spacing
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt

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
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val subtitlesPreferences = koinInject<SubtitlesPreferences>()
  val decoderPreferences = koinInject<DecoderPreferences>()
  val audioPreferences = koinInject<AudioPreferences>()
  val interactionSource = remember { MutableInteractionSource() }
  val controlsShown by viewModel.controlsShown.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val seekBarShown by viewModel.seekBarShown.collectAsState()
  val pausedForCache by viewModel.mpv.propFlow<Boolean>("paused-for-cache").collectAsState()
  val paused by viewModel.mpv.propFlow<Boolean>("pause").collectAsState()
  val duration by viewModel.mpv.propFlow<Int>("duration").collectAsState()
  val position by viewModel.mpv.propFlow<Int>("time-pos").collectAsState()
  val playbackSpeed by viewModel.mpv.propFlow<Float>("speed").collectAsState()
  val gestureSeekAmount by viewModel.gestureSeekAmount.collectAsState()
  val doubleTapSeekAmount by viewModel.doubleTapSeekAmount.collectAsState()
  val showDoubleTapOvals by playerPreferences.showDoubleTapOvals.collectAsState()
  val showSeekIcon by playerPreferences.showSeekIcon.collectAsState()
  val showSeekTime by playerPreferences.showSeekTimeWhileSeeking.collectAsState()
  var isSeeking by remember { mutableStateOf(false) }
  var resetControls by remember { mutableStateOf(true) }
  val seekText by viewModel.seekText.collectAsState()
  val currentChapter by viewModel.mpv.propFlow<Int>("chapter").collectAsState()
  val mpvDecoder by viewModel.mpv.propFlow<String>("hwdec-current").collectAsState()
  val decoder by remember { derivedStateOf { getDecoderFromValue(mpvDecoder ?: "auto") } }
  val playerTimeToDisappear by playerPreferences.playerTimeToDisappear.collectAsState()
  val chapters by viewModel.chapters.collectAsState(persistentListOf())
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
  val customButton by viewModel.primaryButton.collectAsState()

  LaunchedEffect(
    controlsShown,
    paused,
    isSeeking,
    resetControls,
  ) {
    if (controlsShown && paused == false && !isSeeking) {
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
  DoubleTapToSeekOvals(doubleTapSeekAmount, seekText, showDoubleTapOvals, showSeekIcon, showSeekTime, interactionSource)
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
        val mpvVolume by viewModel.mpv.propFlow<Int>("volume").collectAsState()
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
            mpvVolume = mpvVolume ?: 100,
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
          visible = (controlsShown && !areControlsLocked || gestureSeekAmount != null) || pausedForCache == true,
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

            pausedForCache == true && showLoadingCircle -> {
              CircularProgressIndicator(
                Modifier.size(96.dp),
                strokeWidth = 6.dp,
              )
            }

            controlsShown && !areControlsLocked -> Image(
              painter = rememberAnimatedVectorPainter(icon, paused == false),
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
          val readAhead by viewModel.mpv.propFlow<Float>("demuxer-cache-duration").collectAsState()
          val preciseSeeking by playerPreferences.preciseSeeking.collectAsState()
          SeekbarWithTimers(
            position = position?.toFloat() ?: 0f,
            duration = duration?.toFloat() ?: 0f,
            readAheadValue = readAhead ?: 0f,
            onValueChange = {
              isSeeking = true
              viewModel.seekTo(it.roundToInt(), preciseSeeking)
            },
            onValueChangeFinished = { isSeeking = false },
            timersInverted = Pair(false, invertDuration),
            durationTimerOnCLick = { playerPreferences.invertDuration.set(!invertDuration) },
            positionTimerOnClick = {},
            chapters = chapters.map {
              Segment(
                name = it.name,
                start = it.start,
              )
            }.toImmutableList(),
          )
        }
        val mediaTitle by viewModel.mpv.propFlow<String>("media-title").collectAsState()
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
            mediaTitle = mediaTitle ?: "", // it'll be set when the video loads so no problem keeping it empty for now
            onBackClick = onBackPress,
          )
        }
        // Top right controls
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
          val showChaptersButton by playerPreferences.showChaptersButton.collectAsState()
          TopRightPlayerControls(
            decoder = decoder,
            onDecoderClick = { viewModel.cycleDecoders() },
            onDecoderLongClick = { onOpenSheet(Sheets.Decoders) },
            isChaptersVisible = showChaptersButton && chapters.isNotEmpty(),
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
          },
        ) {
          val activity = LocalActivity.current!! as PlayerActivity
          BottomRightPlayerControls(
            customButton = customButton,
            customButtonTitle = customButtonTitle,
            isPipAvailable = activity.isPipSupported,
            onPipClick = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.enterPictureInPictureMode(activity.createPipParams())
              } else {
                activity.enterPictureInPictureMode()
              }
            },
            onCustomButtonClick = {
              customButton?.execute(viewModel.mpv)
            },
            onCustomButtonLongClick = {
              customButton?.executeLongClick(viewModel.mpv)
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
            width = Dimension.fillToConstraints
            end.linkTo(bottomRightControls.start)
          },
        ) {
          val showChapterIndicator by playerPreferences.currentChaptersIndicator.collectAsState()
          BottomLeftPlayerControls(
            playbackSpeed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
            showChapterIndicator = showChapterIndicator,
            currentChapter = chapters.getOrNull(currentChapter ?: 0),
            onLockControls = viewModel::lockControls,
            onCycleRotation = viewModel::cycleScreenRotations,
            onPlaybackSpeedChange = {
              viewModel.mpv.prop["speed"] = it
              playerPreferences.defaultSpeed.set(it)
            },
            onOpenSheet = onOpenSheet,
          )
        }
      }
    }
    val sheetShown by viewModel.sheetShown.collectAsState()
    val subtitles by viewModel.subtitleTracks.collectAsState(persistentListOf())
    val audioTracks by viewModel.audioTracks.collectAsState(persistentListOf())
    val sleepTimerTimeRemaining by viewModel.remainingTime.collectAsState()
    val speedPresets by playerPreferences.speedPresets.collectAsState()
    val backgroundPlayback by playerPreferences.automaticBackgroundPlayback.collectAsState()
    val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()
    val audioChannels by audioPreferences.audioChannels.collectAsState()
    val pitchCorrection by audioPreferences.audioPitchCorrection.collectAsState()
    val mpvAudioPitchCorrection by viewModel.mpv.propFlow<Boolean>("audio-pitch-correction").collectAsState()

    PlayerSheets(
      sheetShown = sheetShown,
      subtitles = subtitles,
      onAddSubtitle = viewModel::addSubtitle,
      onSelectSubtitle = viewModel::selectSub,
      audioTracks = audioTracks,
      onAddAudio = viewModel::addAudio,
      onSelectAudio = {
        if (it.id == viewModel.mpv.getPropertyInt("aid")) {
          viewModel.mpv.prop["aid"] = false
        } else {
          viewModel.mpv.prop["aid"] = it.id
        }
      },
      chapter = chapters.getOrNull(currentChapter ?: 0),
      chapters = chapters,
      onSeekToChapter = {
        viewModel.mpv.prop["chapter"] = it
        viewModel.unpause()
      },
      decoder = decoder,
      onUpdateDecoder = { viewModel.mpv.prop["hwdec"] = it.value },
      speed = playbackSpeed ?: playerPreferences.defaultSpeed.get(),
      onSpeedChange = { viewModel.mpv.prop["speed"] = it.toFixed(2) },
      onMakeDefaultSpeed = { playerPreferences.defaultSpeed.set(it.toFixed(2)) },
      onAddSpeedPreset = { playerPreferences.speedPresets += it.toFixed(2).toString() },
      onRemoveSpeedPreset = { playerPreferences.speedPresets -= it.toFixed(2).toString() },
      onResetSpeedPresets = playerPreferences.speedPresets::delete,
      speedPresets = speedPresets.map { it.toFloat() }.sorted(),
      onResetDefaultSpeed = {
        viewModel.mpv.prop["speed"] = playerPreferences.defaultSpeed.deleteAndGet().toFixed(2)
      },
      // More sheet state
      backgroundPlayback = backgroundPlayback,
      statisticsPage = statisticsPage,
      audioChannels = audioChannels,
      sleepTimerTimeRemaining = sleepTimerTimeRemaining,
      onStartSleepTimer = viewModel::startTimer,
      onBackgroundPlaybackChange = playerPreferences.automaticBackgroundPlayback::set,
      onStatisticsPageChange = { page ->
        if ((page == 0) xor (statisticsPage == 0)) viewModel.mpv.command("script-binding", "stats/display-stats-toggle")
        if (page != 0) viewModel.mpv.command("script-binding", "stats/display-page-$page")
        advancedPreferences.enabledStatisticsPage.set(page)
      },
      onAudioChannelsChange = {
        audioPreferences.audioChannels.set(it)
        if (it == AudioChannels.ReverseStereo) {
          viewModel.mpv.prop[AudioChannels.AutoSafe.property] = AudioChannels.AutoSafe.value
        } else {
          viewModel.mpv.prop[AudioChannels.ReverseStereo.property] = ""
        }
        viewModel.mpv.prop[it.property] = it.value
      },
      onCustomButtonClick = { it.execute(viewModel.mpv) },
      onCustomButtonLongClick = { it.executeLongClick(viewModel.mpv) },
      buttons = customButtons.getButtons().toImmutableList(),
      onOpenPanel = onOpenPanel,
      onPitchCorrectionChange = {
        audioPreferences.audioPitchCorrection.set(it)
        viewModel.mpv.prop["audio-pitch-correction"] = it
      },
      pitchCorrection = pitchCorrection || mpvAudioPitchCorrection == true,
      onDismissRequest = { onOpenSheet(Sheets.None) },
    )
    val panel by viewModel.panelShown.collectAsState()
    val subDelayPref by subtitlesPreferences.defaultSubDelay.collectAsState()
    val subDelay by viewModel.mpv.propFlow<Double>("sub-delay").collectAsState()
    val subDelaySecondary by viewModel.mpv.propFlow<Double>("secondary-sub-delay").collectAsState()
    val subDelaySecondaryPref by subtitlesPreferences.defaultSecondarySubDelay.collectAsState()
    val subSpeed by viewModel.mpv.propFlow<Double>("sub-speed").collectAsState()
    val audioDelay by viewModel.mpv.propFlow<Double>("audio-delay").collectAsState()
    val isBold by viewModel.mpv.propFlow<Boolean>("sub-bold").collectAsState()
    val isItalic by viewModel.mpv.propFlow<Boolean>("sub-italic").collectAsState()
    val subJustify by viewModel.mpv.propFlow<String>("sub-justify").collectAsState()
    val subFont by viewModel.mpv.propFlow<String>("sub-font").collectAsState()
    val subFontSize by viewModel.mpv.propFlow<Int>("sub-font-size").collectAsState()
    val subBorderStyle by viewModel.mpv.propFlow<String>("sub-border-style").collectAsState()
    val subBorderSize by viewModel.mpv.propFlow<Int>("sub-border-size").collectAsState()
    val subShadowOffset by viewModel.mpv.propFlow<Int>("sub-shadow-offset").collectAsState()
    val subColor by viewModel.mpv.propFlow<String>("sub-color").collectAsState()
    val subBorderColor by viewModel.mpv.propFlow<String>("sub-border-color").collectAsState()
    val subBackgroundColor by viewModel.mpv.propFlow<String>("sub-background-color").collectAsState()
    val overrideAssSubs by viewModel.mpv.propFlow<Boolean>("sub-ass-override").collectAsState()
    val subScale by viewModel.mpv.propFlow<Float>("sub-scale").collectAsState()
    val subPos by viewModel.mpv.propFlow<Int>("sub-pos").collectAsState()
    val deband by decoderPreferences.debanding.collectAsState()
    val mpvGpuNext by viewModel.mpv.propFlow<String>("vo").collectAsState()
    val debandSettingsMap = DebandSettings.entries.associateWith { setting ->
      viewModel.mpv.propFlow<Int>(setting.mpvProperty).collectAsState().value ?: 0
    }
    val filterValuesMap = VideoFilters.entries.associateWith { filter ->
      viewModel.mpv.propFlow<Int>(filter.mpvProperty).collectAsState().value ?: 0
    }
    var subtitleColorType by remember { mutableStateOf(SubColorType.Text) }

    PlayerPanels(
      panelShown = panel,
      onDismissRequest = { onOpenPanel(Panels.None) },
      // Subtitle settings panel state
      isBold = isBold ?: subtitlesPreferences.bold.get(),
      isItalic = isItalic ?: subtitlesPreferences.italic.get(),
      subJustify = subJustify?.let { SubtitleJustification.byValue(it) } ?: subtitlesPreferences.justification.get(),
      subFont = subFont ?: subtitlesPreferences.font.get(),
      subFontSize = subFontSize ?: subtitlesPreferences.fontSize.get(),
      subBorderStyle = subBorderStyle?.let { SubtitlesBorderStyle.byValue(it) }
        ?: subtitlesPreferences.borderStyle.get(),
      subBorderSize = subBorderSize ?: subtitlesPreferences.borderSize.get(),
      subShadowOffset = subShadowOffset ?: subtitlesPreferences.shadowOffset.get(),
      subColor = subtitleColorType,
      currentSubtitleColor = when (subtitleColorType) {
        SubColorType.Text -> subColor?.toColorInt() ?: subtitlesPreferences.textColor.get()
        SubColorType.Border -> subBorderColor?.toColorInt() ?: subtitlesPreferences.borderColor.get()
        SubColorType.Background -> subBackgroundColor?.toColorInt() ?: subtitlesPreferences.backgroundColor.get()
      },
      overrideAssSubs = overrideAssSubs ?: subtitlesPreferences.overrideAssSubs.get(),
      subScale = subScale ?: subtitlesPreferences.subScale.get(),
      subPos = subPos ?: subtitlesPreferences.subPos.get(),
      onSubBoldChange = {
        viewModel.mpv.prop["sub-bold"] = it
        subtitlesPreferences.bold.set(it)
      },
      onSubItalicChange = {
        viewModel.mpv.prop["sub-italic"] = it
        subtitlesPreferences.italic.set(it)
      },
      onSubJustifyChange = {
        viewModel.mpv.prop["sub-justify"] = it.value
        subtitlesPreferences.justification.set(it)
      },
      onSubFontChange = {
        viewModel.mpv.prop["sub-font"] = it
        subtitlesPreferences.font.set(it)
      },
      onSubFontSizeChange = {
        viewModel.mpv.prop["sub-font-size"] = it
        subtitlesPreferences.fontSize.set(it)
      },
      onSubBorderStyleChange = {
        viewModel.mpv.prop["sub-border-style"] = it
        subtitlesPreferences.borderStyle.set(it)
      },
      onSubBorderSizeChange = {
        viewModel.mpv.prop["sub-border-size"] = it
        subtitlesPreferences.borderSize.set(it)
      },
      onSubShadowOffsetChange = {
        viewModel.mpv.prop["sub-shadow-offset"] = it
        subtitlesPreferences.shadowOffset.set(it)
      },
      onSubColorChange = {
        when (subtitleColorType) {
          SubColorType.Text -> {
            viewModel.mpv.prop["sub-color"] = it.toColorHexString()
            subtitlesPreferences.textColor.set(it)
          }

          SubColorType.Border -> {
            viewModel.mpv.prop["sub-border-color"] = it.toColorHexString()
            subtitlesPreferences.borderColor.set(it)
          }

          SubColorType.Background -> {
            viewModel.mpv.prop["sub-background-color"] = it.toColorHexString()
            subtitlesPreferences.backgroundColor.set(it)
          }
        }
      },
      onOverrideAssSubsChange = {
        viewModel.mpv.prop["sub-ass-override"] = it
        subtitlesPreferences.overrideAssSubs.set(it)
      },
      onSubScaleChange = {
        viewModel.mpv.prop["sub-scale"] = it
        subtitlesPreferences.subScale.set(it)
      },
      onSubPosChange = {
        viewModel.mpv.prop["sub-pos"] = it
        subtitlesPreferences.subPos.set(it)
      },
      onSubColorTypeChange = { subtitleColorType = it },
      onSubColorReset = {
        resetColors(subtitlesPreferences, viewModel.mpv, subtitleColorType)
      },
      onSubtitleSettingsReset = {
        resetTypography(viewModel.mpv, subtitlesPreferences)
      },
      onSubtitleMiscReset = {
        subtitlesPreferences.subPos.deleteAndGet().let {
          viewModel.mpv.prop["sub-pos"] = it
        }
        subtitlesPreferences.subScale.deleteAndGet().let {
          viewModel.mpv.prop["sub-scale"] = it
        }
        subtitlesPreferences.overrideAssSubs.delete()
        viewModel.mpv.prop["sub-ass-override"] = "scale"
      },
      subDelayMsPrimary = subDelay?.times(1000)?.roundToInt() ?: subDelayPref,
      subDelayMsSecondary = subDelaySecondary?.times(1000)?.roundToInt() ?: subDelaySecondaryPref,
      subSpeed = subSpeed ?: subtitlesPreferences.defaultSubSpeed.get().toDouble(),
      onSubDelayPrimaryChange = {
        viewModel.mpv.prop["sub-delay"] = it / 1000.0
      },
      onSubDelaySecondaryChange = {
        viewModel.mpv.prop["secondary-sub-delay"] = it / 1000.0
      },
      onSubSpeedChange = {
        viewModel.mpv.prop["sub-speed"] = it
      },
      onSubDelayApply = {
        subtitlesPreferences.defaultSubDelay.set((subDelay?.times(1000)?.roundToInt()) ?: 0)
        subtitlesPreferences.defaultSecondarySubDelay.set((subDelaySecondary?.times(1000)?.roundToInt()) ?: 0)
      },
      onSubDelayReset = {
        viewModel.mpv.prop["sub-delay"] = subtitlesPreferences.defaultSubDelay.get() / 1000.0
        viewModel.mpv.prop["secondary-sub-delay"] = subtitlesPreferences.defaultSecondarySubDelay.get() / 1000.0
        viewModel.mpv.prop["sub-speed"] = subtitlesPreferences.defaultSubSpeed.get().toDouble()
      },
      audioDelayMs = (audioDelay?.times(1000))?.roundToInt() ?: audioPreferences.defaultAudioDelay.get(),
      onAudioDelayChange = { viewModel.mpv.prop["audio-delay"] = it / 1000.0 },
      onAudioDelayApply = {
        audioPreferences.defaultAudioDelay.set((audioDelay?.times(1000)?.roundToInt()) ?: 0)
      },
      onAudioDelayReset = {
        viewModel.mpv.prop["audio-delay"] = audioPreferences.defaultAudioDelay.get() / 1000.0
      },
      onDebandChange = {
        decoderPreferences.debanding.set(it)
        when (it) {
          Debanding.None -> {
            viewModel.mpv.prop["deband"] = "no"
            viewModel.mpv.command("vf", "remove", "@deband")
          }

          Debanding.CPU -> {
            viewModel.mpv.prop["deband"] = "no"
            viewModel.mpv.command("vf", "add", "@deband:gradfun=radius=12")
          }

          Debanding.GPU -> {
            viewModel.mpv.prop["deband"] = "yes"
            viewModel.mpv.command("vf", "remove", "@deband")
          }
        }
      },
      onDebandReset = {
        viewModel.mpv.prop["deband"] = "no"
        viewModel.mpv.command("vf", "remove", "@deband")
        DebandSettings.entries.forEach {
          viewModel.mpv.prop[it.mpvProperty] = it.preference(decoderPreferences).deleteAndGet()
        }
      },
      onDebandSettingsChange = { setting, value ->
        setting.preference(decoderPreferences).set(value)
        viewModel.mpv.prop[setting.mpvProperty] = value
      },
      onVideoFilterChange = { filter, value ->
        filter.preference(decoderPreferences).set(value)
        viewModel.mpv.prop[filter.mpvProperty] = value
      },
      onFilterReset = {
        VideoFilters.entries.forEach {
          viewModel.mpv.prop[it.mpvProperty] = it.preference(decoderPreferences).deleteAndGet()
        }
      },
      deband = deband,
      isGpuNextEnabled = mpvGpuNext == "gpu-next",
      filterValue = { filterValuesMap[it] ?: 0 },
      debandSettings = { debandSettingsMap[it] ?: 0 },
      modifier = Modifier,
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
