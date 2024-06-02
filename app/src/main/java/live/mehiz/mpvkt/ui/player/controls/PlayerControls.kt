package live.mehiz.mpvkt.ui.player.controls

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.LeftSideOvalShape
import live.mehiz.mpvkt.presentation.RightSideOvalShape
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.CurrentChapter
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.sheets.AudioTracksSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ChaptersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.Sheets
import live.mehiz.mpvkt.ui.player.controls.components.sheets.SubtitlesSheet
import live.mehiz.mpvkt.ui.theme.PlayerRippleTheme
import org.koin.compose.koinInject

val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

class PlayerControls(
  private val viewModel: PlayerViewModel,
) {
  @Composable
  fun Content() {
    val playerPreferences = koinInject<PlayerPreferences>()
    val controlsShown by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val seekBarShown by viewModel.seekBarShown.collectAsState()
    var gestureSeekAmount by remember { mutableIntStateOf(0) }
    LaunchedEffect(gestureSeekAmount) {
      if (gestureSeekAmount != 0) return@LaunchedEffect
      delay(2000)
      viewModel.hideSeekBar()
    }
    var sheetShown by remember { mutableStateOf(Sheets.None) }
    val position by viewModel.pos.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    Row(modifier = Modifier.fillMaxSize()) {
      repeat(2) { index ->
        var seekAmount by remember { mutableIntStateOf(0) }
        val alpha by animateFloatAsState(
          if (seekAmount != 0) 0.5f else 0f,
          label = "seekingAlpha",
        )
        LaunchedEffect(seekAmount) {
          delay(600)
          seekAmount = 0
          viewModel.hideSeekBar()
        }
        val interactionSource = remember { MutableInteractionSource() }
        val doubleTapToPause by playerPreferences.doubleTapToPause.collectAsState()
        val doubleTapToSeek by playerPreferences.doubleTapToSeek.collectAsState()
        val doubleTapToSeekDuration by playerPreferences.doubleTapToSeekDuration.collectAsState()
        val brightnessGesture = playerPreferences.brightnessGesture.get()
        val volumeGesture = playerPreferences.volumeGesture.get()
        val seekGesture = playerPreferences.horizontalSeekGesture.get()
        Box(
          modifier = Modifier
            .weight(0.5f)
            .fillMaxHeight()
            .graphicsLayer(alpha = alpha)
            .pointerInput(Unit) {
              detectTapGestures(
                onTap = {
                  if (controlsShown) viewModel.hideControls()
                  else viewModel.showControls()
                },
                onDoubleTap = {
                  if (areControlsLocked) return@detectTapGestures
                  if (doubleTapToPause) {
                    viewModel.pauseUnpause()
                    return@detectTapGestures
                  }
                  if (!doubleTapToSeek) return@detectTapGestures
                  // Don't seek backwards if we're on 0:00 or forward if we're at the end
                  val pos = position.toInt()
                  if ((viewModel.duration.toInt() == pos && index == 1) || (pos == 0 && index == 0)) {
                    return@detectTapGestures
                  }
                  val seekDuration = if (index == 0) {
                    -doubleTapToSeekDuration
                  } else {
                    doubleTapToSeekDuration
                  }
                  viewModel.seekBy(seekDuration)
                  seekAmount += seekDuration
                  viewModel.showSeekBar()
                },
                onPress = {
                  val press = PressInteraction.Press(it)
                  interactionSource.emit(press)
                  tryAwaitRelease()
                  interactionSource.emit(PressInteraction.Release(press))
                },
              )
            }
            .pointerInput(Unit) {
              if (!seekGesture || areControlsLocked) return@pointerInput
              var startingPosition = position
              detectHorizontalDragGestures(
                onDragStart = {
                  startingPosition = position
                  viewModel.pause()
                },
                onDragEnd = {
                  gestureSeekAmount = 0
                  viewModel.unpause()
                },
              ) { change, dragAmount ->
                if ((position >= viewModel.duration && dragAmount > 0) || (position <= 0f && dragAmount < 0)) {
                  return@detectHorizontalDragGestures
                }
                viewModel.showSeekBar()
                val seekBy = ((dragAmount * 150f / size.width).coerceIn(
                  0f - position,
                  viewModel.duration - position,
                )).toInt()
                viewModel.seekBy(seekBy)
                gestureSeekAmount = (position - startingPosition).toInt()
              }
            }
            .pointerInput(Unit) {
              if ((!brightnessGesture && !volumeGesture) || areControlsLocked) return@pointerInput
              var dragAmount = 0f
              detectVerticalDragGestures(
                onDragEnd = {
                  dragAmount = 0f
                },
              ) { change, amount ->
                dragAmount -= amount / 10
                when {
                  volumeGesture && brightnessGesture -> {
                    if (index == 0) viewModel.changeBrightnessWithDrag(dragAmount)
                    else viewModel.changeVolumeWithDrag(dragAmount)
                  }

                  brightnessGesture -> {
                    viewModel.changeBrightnessWithDrag(dragAmount)
                  }
                  // it's not always true, AS is drunk
                  volumeGesture -> {
                    viewModel.changeVolumeWithDrag(dragAmount)
                  }

                  else -> {}
                }
              }
            }
            .clip(if (index == 0) LeftSideOvalShape else RightSideOvalShape)
            .background(MaterialTheme.colorScheme.primary)
            .indication(
              interactionSource,
              rememberRipple(color = MaterialTheme.colorScheme.secondary),
            ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            (if (index == 1) "+" else "") + "${seekAmount}s",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.displayMedium,
          )
        }
      }
    }
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

    CompositionLocalProvider(
      LocalRippleTheme provides PlayerRippleTheme,
      LocalPlayerButtonsClickEvent provides { resetControls = !resetControls },
    ) {
      ConstraintLayout(
        modifier = Modifier
          .fillMaxSize()
          .background(transparentOverlay)
          .padding(horizontal = 16.dp),

      ) {
        val (
          seekbar,
          playerPauseButton,
          seekValue,
          topRightControls,
          bottomLeftControls,
          bottomRightControls,
          unlockControlsButton,
        ) = createRefs()
        AnimatedVisibility(
          controlsShown && areControlsLocked,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.constrainAs(unlockControlsButton) {
            top.linkTo(parent.top, 16.dp)
            start.linkTo(parent.start, 16.dp)
          },
        ) {
          ControlsButton(
            Icons.Filled.LockOpen,
          ) {
            viewModel.unlockControls()
          }
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
          visible = controlsShown && !areControlsLocked,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.constrainAs(playerPauseButton) {
            end.linkTo(parent.absoluteRight)
            start.linkTo(parent.absoluteLeft)
            top.linkTo(parent.top)
            bottom.linkTo(seekbar.top)
          },
        ) {
          val icon = if (!paused) Icons.Default.Pause else Icons.Default.PlayArrow
          val interaction = remember { MutableInteractionSource() }
          Icon(
            modifier = Modifier
              .size(96.dp)
              .clip(CircleShape)
              .clickable(
                interaction,
                rememberRipple()
              ) { viewModel.pauseUnpause() },
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
          )
        }
        AnimatedVisibility(
          visible = (controlsShown || seekBarShown) && !areControlsLocked,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          modifier = Modifier.constrainAs(seekbar) {
            bottom.linkTo(parent.bottom, 16.dp)
          },
        ) {
          val invertDuration by playerPreferences.invertDuration.collectAsState()
          val readAhead by viewModel.readAhead.collectAsState()
          SeekbarWithTimers(
            position = position,
            duration = viewModel.duration,
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
          )
        }
        // Top right controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
          exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
          modifier = Modifier.constrainAs(topRightControls) {
            top.linkTo(parent.top, 16.dp)
            end.linkTo(parent.end)
          },
        ) {
          Row {
            if(playerPreferences.showChaptersButton.get() && viewModel.chapters.isNotEmpty()) {
              ControlsButton(Icons.Default.Bookmarks) {
                sheetShown = Sheets.Chapters
              }
            }
            ControlsButton(Icons.Default.Subtitles) {
              sheetShown = Sheets.SubtitlesSheet
            }
            ControlsButton(Icons.Default.Audiotrack) {
              sheetShown = Sheets.AudioSheet
            }
          }
        }
        // Bottom right controls
        AnimatedVisibility(
          controlsShown && !areControlsLocked,
          enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
          exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
          modifier = Modifier.constrainAs(bottomRightControls) {
            bottom.linkTo(seekbar.top)
            end.linkTo(seekbar.end)
          },
        ) {
          val aspect by playerPreferences.videoAspect.collectAsState()
          Row {
            ControlsButton(Icons.Default.AspectRatio) {
              when (aspect) {
                VideoAspect.Fit -> viewModel.changeVideoAspect(VideoAspect.Crop)
                VideoAspect.Crop -> viewModel.changeVideoAspect(VideoAspect.Stretch)
                VideoAspect.Stretch -> viewModel.changeVideoAspect(VideoAspect.Fit)
              }
            }
          }
        }
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
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ControlsButton(Icons.Default.Lock) {
              viewModel.lockControls()
            }
            AnimatedVisibility(
              currentChapter != null && playerPreferences.currentChaptersIndicator.get(),
              enter = fadeIn(),
              exit = fadeOut()
            ) {
              CurrentChapter(
                currentChapter!!,
                modifier = Modifier.clickable {
                  sheetShown = Sheets.Chapters
                }
              )
            }
          }
        }
      }
      val subtitles by viewModel.subtitleTracks.collectAsState()
      val selectedSubs by viewModel.selectedSubtitles.collectAsState()
      val audioTracks by viewModel.audioTracks.collectAsState()
      val selectedAudio by viewModel.selectedAudio.collectAsState()
      when (sheetShown) {
        Sheets.None -> {}
        Sheets.SubtitlesSheet -> {
          viewModel.pause()
          SubtitlesSheet(
            subtitles,
            selectedSubs,
            { viewModel.selectSub(it) },
          ) { sheetShown = Sheets.None }
        }

        Sheets.AudioSheet -> {
          viewModel.pause()
          AudioTracksSheet(
            audioTracks,
            selectedAudio,
            { viewModel.selectAudio(it) },
          ) { sheetShown = Sheets.None }
        }

        Sheets.Chapters -> {
          viewModel.pause()
          ChaptersSheet(
            viewModel.chapters,
            currentChapter = currentChapter?.index?: 0,
            {
              viewModel.selectChapter(it)
              sheetShown = Sheets.None
              viewModel.unpause()
            }
          ) { sheetShown = Sheets.None }
        }
      }
    }
  }
}
