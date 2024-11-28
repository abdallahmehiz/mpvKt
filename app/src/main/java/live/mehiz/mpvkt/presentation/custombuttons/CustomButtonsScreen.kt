package live.mehiz.mpvkt.presentation.custombuttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import live.mehiz.mpvkt.R
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.presentation.custombuttons.components.CustomButtonListItem
import live.mehiz.mpvkt.ui.theme.spacing

@Suppress("ModifierNotUsedAtRoot", "ModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomButtonsScreen(
  buttons: List<CustomButtonEntity>,
  primaryId: Int,
  onClickAdd: () -> Unit,
  onClickRename: (CustomButtonEntity) -> Unit,
  onClickDelete: (CustomButtonEntity) -> Unit,
  onClickMoveUp: (CustomButtonEntity) -> Unit,
  onClickMoveDown: (CustomButtonEntity) -> Unit,
  onTogglePrimary: (CustomButtonEntity) -> Unit,
  onClickFaq: () -> Unit,
  onNavigateBack: () -> Unit,
) {
  val lazyListState = rememberLazyListState()
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(stringResource(R.string.pref_custom_buttons_title))
        },
        navigationIcon = {
          IconButton(onClick = { onNavigateBack() }) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
          }
        },
        actions = {
          IconButton(onClick = { onClickFaq() }) {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, null)
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { onClickAdd() },
        icon = { Icon(Icons.Filled.Add, null) },
        text = { Text(text = stringResource(id = R.string.pref_custom_button_action_add)) },
      )
    }
  ) { padding ->
    if (buttons.isEmpty()) {
      Column(
        modifier = Modifier
          .padding(padding)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = stringResource(id = R.string.pref_custom_button_empty),
          textAlign = TextAlign.Center,
        )
      }
      return@Scaffold
    }

    val layoutDirection = LocalLayoutDirection.current
    CustomButtonsContent(
      customButtons = buttons,
      primaryId = primaryId,
      lazyListState = lazyListState,
      paddingValues = PaddingValues(
        top = MaterialTheme.spacing.small + padding.calculateTopPadding(),
        start = MaterialTheme.spacing.medium + padding.calculateStartPadding(layoutDirection),
        end = MaterialTheme.spacing.medium + padding.calculateEndPadding(layoutDirection),
        bottom = padding.calculateBottomPadding(),
      ),
      onClickRename = onClickRename,
      onClickDelete = onClickDelete,
      onTogglePrimary = onTogglePrimary,
      onMoveUp = onClickMoveUp,
      onMoveDown = onClickMoveDown,
    )
  }
}

@Composable
private fun CustomButtonsContent(
  customButtons: List<CustomButtonEntity>,
  primaryId: Int,
  lazyListState: LazyListState,
  paddingValues: PaddingValues,
  onClickRename: (CustomButtonEntity) -> Unit,
  onClickDelete: (CustomButtonEntity) -> Unit,
  onTogglePrimary: (CustomButtonEntity) -> Unit,
  onMoveUp: (CustomButtonEntity) -> Unit,
  onMoveDown: (CustomButtonEntity) -> Unit,
) {
  LazyColumn(
    state = lazyListState,
    contentPadding = paddingValues,
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
  ) {
    itemsIndexed(
      items = customButtons,
      key = { _, button -> "button-${button.id}" },
    ) { index, button ->
      CustomButtonListItem(
        modifier = Modifier.animateItem(),
        customButton = button,
        isPrimary = button.id == primaryId,
        canMoveUp = index != 0,
        canMoveDown = index != customButtons.lastIndex,
        onMoveUp = onMoveUp,
        onMoveDown = onMoveDown,
        onRename = { onClickRename(button) },
        onDelete = { onClickDelete(button) },
        onTogglePrimary = { onTogglePrimary(button) },
      )
    }
  }
}
