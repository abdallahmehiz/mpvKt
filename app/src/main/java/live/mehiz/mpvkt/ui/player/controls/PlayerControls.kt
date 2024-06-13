package live.mehiz.mpvkt.ui.player.controls

import android.app.Activity
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.LeftSideOvalShape
import live.mehiz.mpvkt.presentation.RightSideOvalShape
import live.mehiz.mpvkt.ui.player.PlayerViewModel
import live.mehiz.mpvkt.ui.player.Sheets
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.controls.components.ControlsButton
import live.mehiz.mpvkt.ui.player.controls.components.CurrentChapter
import live.mehiz.mpvkt.ui.player.controls.components.DoubleTapSecondsView
import live.mehiz.mpvkt.ui.player.controls.components.SeekbarWithTimers
import live.mehiz.mpvkt.ui.player.controls.components.sheets.AudioTracksSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.ChaptersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.DecodersSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.MoreSheet
import live.mehiz.mpvkt.ui.player.controls.components.sheets.SubtitlesSheet
import live.mehiz.mpvkt.ui.theme.PlayerRippleTheme
import org.koin.compose.koinInject

val LocalPlayerButtonsClickEvent = staticCompositionLocalOf { {} }

class PlayerControls(private val viewModel: PlayerViewModel) {
  @OptIn(ExperimentalAnimationGraphicsApi::class)
  @Composable
  fun Content() {
    val playerPreferences = koinInject<PlayerPreferences>()
    val controlsShown by viewModel.controlsShown.collectAsState()
    val areControlsLocked by viewModel.areControlsLocked.collectAsState()
    val seekBarShown by viewModel.seekBarShown.collectAsState()
    var gestureSeekAmount by remember { mutableIntStateOf(0) }
    val isLoading by viewModel.isLoading.collectAsState()
    val duration by viewModel.duration.collectAsState()
    LaunchedEffect(gestureSeekAmount) {
      if (gestureSeekAmount != 0) return@LaunchedEffect
      delay(2000)
      viewModel.hideSeekBar()
    }
    var sheetShown by remember { mutableStateOf(Sheets.None) }
    var pausedByASheet by remember { mutableStateOf(false) }
    LaunchedEffect(sheetShown) {
      if (sheetShown != Sheets.None) {
        pausedByASheet = !viewModel.paused.value
        viewModel.pause()
      } else {
        if (pausedByASheet) viewModel.unpause()
        pausedByASheet = false
      }
    }
    val position by viewModel.pos.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val currentDecoder by viewModel.currentDecoder.collectAsState()
    Row(
      modifier = Modifier.fillMaxSize(),
    ) {
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

      val doubleTapSeek: (Offset, IntSize) -> Unit = { offset, size ->
        targetAlpha = 0.2f
        val isForward = offset.x > 3 * size.width / 5
        if (isSeekingForwards != isForward) seekAmount = 0
        isSeekingForwards = isForward
        if (!((duration <= position && isSeekingForwards) || (position <= 0f && !isSeekingForwards))) {
          seekAmount += if (isSeekingForwards) doubleTapToSeekDuration else -doubleTapToSeekDuration
        }
        viewModel.seekBy(if (isSeekingForwards) doubleTapToSeekDuration else -doubleTapToSeekDuration)
        viewModel.showSeekBar()
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = {
                if (controlsShown) viewModel.hideControls()
                else viewModel.showControls()
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
              if ((position >= duration && dragAmount > 0) || (position <= 0f && dragAmount < 0)) {
                return@detectHorizontalDragGestures
              }
              viewModel.showSeekBar()
              val seekBy = ((dragAmount * 150f / size.width).coerceIn(
                0f - position,
                duration - position,
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
                  if (change.position.x < size.width / 2) viewModel.changeBrightnessWithDrag(dragAmount)
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
          },
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
                factory = { DoubleTapSecondsView(it, null) },
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
          topLeftControls,
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
            bottom.linkTo(parent.bottom, 16.dp)
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
          enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
          exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
          modifier = Modifier.constrainAs(topLeftControls) {
            top.linkTo(parent.top, 16.dp)
            start.linkTo(parent.start)
            width = Dimension.fillToConstraints
            end.linkTo(topRightControls.start)
          },
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            val activity = LocalContext.current as Activity
            ControlsButton(
              icon = Icons.AutoMirrored.Default.ArrowBack,
              onClick = { activity.finish() },
            )
            Text(
              viewModel.fileName,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = Color.White,
              style = MaterialTheme.typography.bodyLarge,
            )
          }
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
            ControlsButton(
              currentDecoder.title,
              onClick = { viewModel.cycleDecoders() },
              onLongClick = {
                sheetShown = Sheets.Decoders
              },
            )
            if (playerPreferences.showChaptersButton.get() && viewModel.chapters.isNotEmpty()) {
              ControlsButton(
                Icons.Default.Bookmarks,
                onClick = { sheetShown = Sheets.Chapters },
              )
            }
            ControlsButton(
              Icons.Default.Subtitles,
              onClick = { sheetShown = Sheets.SubtitlesSheet },
            )
            ControlsButton(
              Icons.Default.Audiotrack,
              onClick = { sheetShown = Sheets.AudioSheet },
            )
            ControlsButton(
              Icons.Default.MoreVert,
              onClick = { sheetShown = Sheets.More },
            )
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
            ControlsButton(
              Icons.Default.AspectRatio,
              onClick = {
                when (aspect) {
                  VideoAspect.Fit -> viewModel.changeVideoAspect(VideoAspect.Stretch)
                  VideoAspect.Stretch -> viewModel.changeVideoAspect(VideoAspect.Crop)
                  VideoAspect.Crop -> viewModel.changeVideoAspect(VideoAspect.Fit)
                }
              },
            )
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
            verticalAlignment = Alignment.CenterVertically,
          ) {
            ControlsButton(
              Icons.Default.Lock,
              onClick = { viewModel.lockControls() },
            )
            AnimatedVisibility(
              currentChapter != null && playerPreferences.currentChaptersIndicator.get(),
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              CurrentChapter(
                currentChapter!!,
                onClick = { sheetShown = Sheets.Chapters },
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
          val subtitlesPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
          ) {
            if (it == null) return@rememberLauncherForActivityResult
            viewModel.addSubtitle(it)
          }
          SubtitlesSheet(
            subtitles,
            selectedSubs,
            { viewModel.selectSub(it) },
            { subtitlesPicker.launch(arrayOf("*/*")) },
          ) { sheetShown = Sheets.None }
        }

        Sheets.AudioSheet -> {
          val audioPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
          ) {
            if (it == null) return@rememberLauncherForActivityResult
            viewModel.addAudio(it)
          }
          AudioTracksSheet(
            audioTracks,
            selectedAudio,
            { viewModel.selectAudio(it) },
            { audioPicker.launch(arrayOf("*/*")) },
          ) { sheetShown = Sheets.None }
        }

        Sheets.Chapters -> {
          ChaptersSheet(
            viewModel.chapters,
            currentChapter = currentChapter?.index ?: 0,
            {
              viewModel.selectChapter(it)
              sheetShown = Sheets.None
              viewModel.unpause()
            },
          ) { sheetShown = Sheets.None }
        }

        Sheets.Decoders -> {
          DecodersSheet(
            selectedDecoder = currentDecoder,
            onSelect = { viewModel.updateDecoder(it) },
          ) { sheetShown = Sheets.None }
        }

        Sheets.More -> {
          MoreSheet {
            sheetShown = Sheets.None
          }
        }
      }
    }
  }
}
