package live.mehiz.mpvkt.presentation.custombuttons.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.ui.theme.spacing
import me.zhanghai.compose.preference.SwitchPreference
import kotlin.time.Duration.Companion.seconds

@Composable
fun CustomButtonAddDialog(
  onDismissRequest: () -> Unit,
  onAdd: (String, String, String, Boolean, Boolean) -> Unit,
  buttonNames: ImmutableList<String>,
) {
  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var longPressContent by remember { mutableStateOf("") }
  var showInPlayer by remember { mutableStateOf(true) }
  var showInMoreSheet by remember { mutableStateOf(true) }

  val focusRequester = remember { FocusRequester() }
  val titleAlreadyExists = remember(title) { buttonNames.contains(title) }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      TextButton(
        enabled = title.isNotEmpty() && content.isNotEmpty() && !titleAlreadyExists,
        onClick = {
          onAdd(title, content, longPressContent, showInPlayer, showInMoreSheet)
          onDismissRequest()
        }
      ) {
        Text(text = stringResource(id = R.string.pref_custom_button_action_add))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(text = stringResource(id = R.string.generic_cancel))
      }
    },
    title = {
      Text(text = stringResource(id = R.string.pref_custom_button_add_button))
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
      ) {
        OutlinedTextField(
          modifier = Modifier
            .focusRequester(focusRequester),
          value = title,
          onValueChange = { title = it },
          label = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_title_text))
          },
          supportingText = {
            val msgRes = if (title.isNotEmpty() && titleAlreadyExists) {
              R.string.pref_custom_button_action_add_already_exists
            } else {
              R.string.pref_custom_button_action_add_required
            }
            Text(text = stringResource(id = msgRes))
          },
          isError = title.isNotEmpty() && titleAlreadyExists,
          singleLine = true,
        )

        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_content_text))
          },
          supportingText = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_required))
          },
          minLines = 3,
        )

        OutlinedTextField(
          value = longPressContent,
          onValueChange = { longPressContent = it },
          label = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_long_press_text))
          },
          supportingText = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_optional))
          },
          minLines = 3,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(id = R.string.pref_custom_button_show_in_player),
            modifier = Modifier.weight(1f)
          )
          Switch(
            checked = showInPlayer,
            onCheckedChange = { showInPlayer = it }
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(id = R.string.pref_custom_button_show_in_more_sheet),
            modifier = Modifier.weight(1f)
          )
          Switch(
            checked = showInMoreSheet,
            onCheckedChange = { showInMoreSheet = it }
          )
        }
      }
    }
  )

  @Suppress("ForbiddenComment")
  LaunchedEffect(focusRequester) {
    // TODO: https://issuetracker.google.com/issues/204502668
    delay(0.1.seconds)
    focusRequester.requestFocus()
  }
}

@Composable
fun CustomButtonDeleteDialog(
  onDismissRequest: () -> Unit,
  onDelete: () -> Unit,
  title: String,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      TextButton(onClick = {
        onDelete()
        onDismissRequest()
      }) {
        Text(text = stringResource(R.string.generic_ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(text = stringResource(R.string.generic_cancel))
      }
    },
    title = {
      Text(text = stringResource(R.string.pref_custom_button_delete_button))
    },
    text = {
      Text(text = stringResource(R.string.pref_custom_button_delete_confirmation, title))
    },
  )
}

@Composable
fun CustomButtonEditDialog(
  onDismissRequest: () -> Unit,
  onEdit: (String, String, String, Boolean, Boolean) -> Unit,
  buttonNames: ImmutableList<String>,
  initialState: CustomButtonEntity,
) {
  var title by remember { mutableStateOf(initialState.title) }
  var content by remember { mutableStateOf(initialState.content) }
  var longPressContent by remember { mutableStateOf(initialState.longPressContent) }
  var showInPlayer by remember { mutableStateOf(initialState.showInPlayer) }
  var showInMoreSheet by remember { mutableStateOf(initialState.showInMoreSheet) }

  val focusRequester = remember { FocusRequester() }
  val titleAlreadyExists = remember(title) { buttonNames.contains(title) }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      TextButton(
        enabled = title.isNotEmpty() && content.isNotEmpty() && !titleAlreadyExists,
        onClick = {
          onEdit(title, content, longPressContent, showInPlayer, showInMoreSheet)
          onDismissRequest()
        }
      ) {
        Text(text = stringResource(id = R.string.generic_ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) {
        Text(text = stringResource(id = R.string.generic_cancel))
      }
    },
    title = {
      Row {
        Text(text = stringResource(id = R.string.pref_custom_button_edit_button))
        Spacer(Modifier.weight(1f))
        Text(
          text = stringResource(id = R.string.pref_custom_button_edit_button_id, initialState.id),
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.alpha(0.38f),
        )
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
      ) {
        OutlinedTextField(
          modifier = Modifier
            .focusRequester(focusRequester),
          value = title,
          onValueChange = { title = it },
          label = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_title_text))
          },
          supportingText = {
            val msgRes = if (title.isNotEmpty() && titleAlreadyExists) {
              R.string.pref_custom_button_action_add_already_exists
            } else {
              R.string.pref_custom_button_action_add_required
            }
            Text(text = stringResource(id = msgRes))
          },
          isError = title.isNotEmpty() && titleAlreadyExists,
          singleLine = true,
        )

        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_content_text))
          },
          supportingText = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_required))
          },
          minLines = 3,
          maxLines = 6,
        )

        OutlinedTextField(
          value = longPressContent,
          onValueChange = { longPressContent = it },
          label = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_long_press_text))
          },
          supportingText = {
            Text(text = stringResource(id = R.string.pref_custom_button_action_add_optional))
          },
          minLines = 3,
          maxLines = 6,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(id = R.string.pref_custom_button_show_in_player),
            modifier = Modifier.weight(1f)
          )
          Switch(
            checked = showInPlayer,
            onCheckedChange = { showInPlayer = it }
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = stringResource(id = R.string.pref_custom_button_show_in_more_sheet),
            modifier = Modifier.weight(1f)
          )
          Switch(
            checked = showInMoreSheet,
            onCheckedChange = { showInMoreSheet = it }
          )
        }
      }
    }
  )

  @Suppress("ForbiddenComment")
  LaunchedEffect(focusRequester) {
    // TODO: https://issuetracker.google.com/issues/204502668
    delay(0.1.seconds)
    focusRequester.requestFocus()
  }
}
