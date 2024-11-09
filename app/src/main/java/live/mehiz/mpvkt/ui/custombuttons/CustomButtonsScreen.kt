package live.mehiz.mpvkt.ui.custombuttons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.collectAsState
import live.mehiz.mpvkt.presentation.Screen
import live.mehiz.mpvkt.presentation.custombuttons.CustomButtonsScreen
import live.mehiz.mpvkt.presentation.custombuttons.components.CustomButtonAddDialog
import live.mehiz.mpvkt.presentation.custombuttons.components.CustomButtonDeleteDialog
import live.mehiz.mpvkt.presentation.custombuttons.components.CustomButtonEditDialog
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

object CustomButtonsScreen : Screen() {
  @Composable
  override fun Content() {
    val navigator = LocalNavigator.currentOrThrow
    val viewModel = koinViewModel<CustomButtonsScreenViewModel>()
    val playerPreferences = koinInject<PlayerPreferences>()

    val primaryButtonId by playerPreferences.primaryCustomButtonId.collectAsState()
    val customButtons by viewModel.customButtons.collectAsState()
    val dialog by viewModel.dialog.collectAsState()

    CustomButtonsScreen(
      buttons = customButtons,
      primaryId = primaryButtonId,
      onClickAdd = { viewModel.showDialog(CustomButtonDialog.Create) },
      onClickRename = { viewModel.showDialog(CustomButtonDialog.Edit(it)) },
      onClickDelete = { viewModel.showDialog(CustomButtonDialog.Delete(it)) },
      onTogglePrimary = viewModel::togglePrimary,
      onClickMoveUp = viewModel::moveUp,
      onClickMoveDown = viewModel::moveDown,
      onNavigateBack = navigator::pop,
    )

    when (dialog) {
      is CustomButtonDialog.None -> {}
      is CustomButtonDialog.Create -> {
        CustomButtonAddDialog(
          onDismissRequest = viewModel::dismissDialog,
          onAdd = viewModel::addCustomButton,
          buttonNames = customButtons.map { it.title }.toImmutableList(),
        )
      }
      is CustomButtonDialog.Edit -> {
        val button = (dialog as CustomButtonDialog.Edit).customButton
        CustomButtonEditDialog(
          onDismissRequest = viewModel::dismissDialog,
          onEdit = { title, content ->
            viewModel.editButton(
              button.copy(title = title, content = content)
            )
          },
          buttonNames = customButtons.filter { it.title != button.title }
            .map { it.title }
            .toImmutableList(),
          initialState = button,
        )
      }
      is CustomButtonDialog.Delete -> {
        val button = (dialog as CustomButtonDialog.Delete).customButton
        CustomButtonDeleteDialog(
          onDismissRequest = viewModel::dismissDialog,
          onDelete = { viewModel.removeButton(button) },
          title = button.title,
        )
      }
    }
  }
}

sealed interface CustomButtonDialog {
  data object None : CustomButtonDialog
  data object Create : CustomButtonDialog
  data class Edit(val customButton: CustomButtonEntity) : CustomButtonDialog
  data class Delete(val customButton: CustomButtonEntity) : CustomButtonDialog
}
