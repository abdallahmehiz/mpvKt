package live.mehiz.mpvkt.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.preferences.GesturePreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.ui.player.CustomKeyCodes
import live.mehiz.mpvkt.ui.player.SingleActionGesture
import me.zhanghai.compose.preference.FooterPreference
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

object GesturePreferencesScreen : Screen() {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<GesturePreferences>()
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(stringResource(R.string.pref_gesture))
          },
          navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
              Icon(Icons.AutoMirrored.Default.ArrowBack, null)
            }
          },
        )
      }
    ) { padding ->
      ProvidePreferenceLocals {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        ) {
          PreferenceCategory(
            title = { Text(text = stringResource(R.string.pref_gesture_double_tap_title)) }
          )
          val doubleTapSeekDuration by preferences.doubleTapToSeekDuration.collectAsState()
          ListPreference(
            value = doubleTapSeekDuration,
            onValueChange = preferences.doubleTapToSeekDuration::set,
            values = listOf(3, 5, 10, 15, 20, 25, 30),
            valueToText = { AnnotatedString("${it}s") },
            title = { Text(text = stringResource(id = R.string.pref_player_double_tap_seek_duration)) },
            summary = { Text(text = "${doubleTapSeekDuration}s") },
          )

          val leftDoubleTap by preferences.leftSingleActionGesture.collectAsState()
          ListPreference(
            value = leftDoubleTap,
            onValueChange = { preferences.leftSingleActionGesture.set(it) },
            values = SingleActionGesture.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_double_tap_left_title)) },
            summary = { Text(text = stringResource(leftDoubleTap.titleRes)) },
          )

          val centerDoubleTap by preferences.centerSingleActionGesture.collectAsState()
          ListPreference(
            value = centerDoubleTap,
            onValueChange = { preferences.centerSingleActionGesture.set(it) },
            values = listOf(
              SingleActionGesture.None,
              SingleActionGesture.PlayPause,
              SingleActionGesture.Custom,
            ),
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_double_tap_center_title)) },
            summary = { Text(text = stringResource(centerDoubleTap.titleRes)) },
          )

          val rightDoubleTap by preferences.rightSingleActionGesture.collectAsState()
          ListPreference(
            value = rightDoubleTap,
            onValueChange = { preferences.rightSingleActionGesture.set(it) },
            values = SingleActionGesture.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_double_tap_right_title)) },
            summary = { Text(text = stringResource(rightDoubleTap.titleRes)) },
          )

          val doubleTapKeyCodes = listOf(
            CustomKeyCodes.DoubleTapLeft,
            CustomKeyCodes.DoubleTapCenter,
            CustomKeyCodes.DoubleTapRight,
          ).map { it.keyCode }.toImmutableList()
          FooterPreference(
            summary = {
              var annotatedString = buildAnnotatedString {
                append(stringResource(R.string.pref_gesture_double_tap_custom_info))
              }

              doubleTapKeyCodes.forEach { keyCode ->
                annotatedString = buildAnnotatedString {
                  val startIndex = annotatedString.indexOf(keyCode)
                  val endIndex = startIndex + keyCode.length
                  append(annotatedString)
                  addStyle(style = SpanStyle(fontWeight = FontWeight.Bold), start = startIndex, end = endIndex)
                }
              }

              Text(text = annotatedString)
            }
          )

          PreferenceCategory(
            title = { Text(text = stringResource(R.string.pref_gesture_media_title)) }
          )

          val mediaPreviousGesture by preferences.mediaPreviousGesture.collectAsState()
          ListPreference(
            value = mediaPreviousGesture,
            onValueChange = { preferences.mediaPreviousGesture.set(it) },
            values = SingleActionGesture.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_media_previous)) },
            summary = { Text(text = stringResource(mediaPreviousGesture.titleRes)) },
          )
          val mediaPlayGesture by preferences.mediaPlayGesture.collectAsState()
          ListPreference(
            value = mediaPlayGesture,
            onValueChange = { preferences.mediaPlayGesture.set(it) },
            values = listOf(
              SingleActionGesture.None,
              SingleActionGesture.PlayPause,
              SingleActionGesture.Custom,
            ),
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_media_play)) },
            summary = { Text(text = stringResource(mediaPlayGesture.titleRes)) },
          )
          val mediaNextGesture by preferences.mediaNextGesture.collectAsState()
          ListPreference(
            value = mediaNextGesture,
            onValueChange = { preferences.mediaNextGesture.set(it) },
            values = SingleActionGesture.entries,
            valueToText = { AnnotatedString(context.getString(it.titleRes)) },
            title = { Text(text = stringResource(R.string.pref_gesture_media_next)) },
            summary = { Text(text = stringResource(mediaNextGesture.titleRes)) },
          )

          val mediaKeyCodes = listOf(
            CustomKeyCodes.MediaPrevious,
            CustomKeyCodes.MediaPlay,
            CustomKeyCodes.MediaNext,
          ).map { it.keyCode }.toImmutableList()
          FooterPreference(
            summary = {
              var annotatedString = buildAnnotatedString {
                append(stringResource(R.string.pref_gesture_media_custom_info))
              }

              mediaKeyCodes.forEach { keyCode ->
                annotatedString = buildAnnotatedString {
                  val startIndex = annotatedString.indexOf(keyCode)
                  val endIndex = startIndex + keyCode.length
                  append(annotatedString)
                  addStyle(style = SpanStyle(fontWeight = FontWeight.Bold), start = startIndex, end = endIndex)
                }
              }

              Text(text = annotatedString)
            }
          )
        }
      }
    }
  }
}
