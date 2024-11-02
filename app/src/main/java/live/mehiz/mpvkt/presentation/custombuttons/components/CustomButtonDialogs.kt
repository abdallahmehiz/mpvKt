package live.mehiz.mpvkt.presentation.custombuttons.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.ui.theme.spacing
import kotlin.time.Duration.Companion.seconds

@Composable
fun CustomButtonAddDialog(
  onDismissRequest: () -> Unit,
  onAdd: (String, String) -> Unit,
  buttonNames: ImmutableList<String>,
) {
  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }

  val focusRequester = remember { FocusRequester() }
  val titleAlreadyExists = remember(title) { buttonNames.contains(title) }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      TextButton(
        enabled = title.isNotEmpty() && content.isNotEmpty() && !titleAlreadyExists,
        onClick = {
          onAdd(title, content)
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
  onEdit: (String, String) -> Unit,
  buttonNames: ImmutableList<String>,
  initialState: CustomButtonEntity,
) {
  var title by remember { mutableStateOf(initialState.title) }
  var content by remember { mutableStateOf(initialState.content) }

  val focusRequester = remember { FocusRequester() }
  val titleAlreadyExists = remember(title) { buttonNames.contains(title) }

  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      TextButton(
        enabled = title.isNotEmpty() && content.isNotEmpty() && !titleAlreadyExists,
        onClick = {
          onEdit(title, content)
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
      Text(text = stringResource(id = R.string.pref_custom_button_edit_button))
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
