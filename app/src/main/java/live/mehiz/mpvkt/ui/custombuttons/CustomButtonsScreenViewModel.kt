package live.mehiz.mpvkt.ui.custombuttons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.database.repository.CustomButtonRepositoryImpl
import live.mehiz.mpvkt.preferences.PlayerPreferences
import org.koin.java.KoinJavaComponent.inject

class CustomButtonsScreenViewModel(
  private val customButtonsRepository: CustomButtonRepositoryImpl,
) : ViewModel() {
  private val playerPreferences: PlayerPreferences by inject(PlayerPreferences::class.java)

  private val _dialog = MutableStateFlow<CustomButtonDialog>(CustomButtonDialog.None)
  val dialog = _dialog.asStateFlow()

  private val primaryCustomButtonId = MutableStateFlow(playerPreferences.primaryCustomButtonId.get())
  val customButtons: StateFlow<List<CustomButtonEntity>> = customButtonsRepository.getCustomButtons()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  fun addCustomButton(title: String, content: String) {
    viewModelScope.launch(Dispatchers.IO) {
      customButtonsRepository.upsert(
        CustomButtonEntity(
          title = title,
          content = content,
          index = customButtons.value.size,
        )
      )
    }
  }

  fun moveUp(customButton: CustomButtonEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      customButtonsRepository.decreaseIndex(customButton)
    }
  }

  fun moveDown(customButton: CustomButtonEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      customButtonsRepository.increaseIndex(customButton)
    }
  }

  fun editButton(customButton: CustomButtonEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      customButtonsRepository.upsert(customButton)
    }
  }

  fun removeButton(customButton: CustomButtonEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      customButtonsRepository.deleteAndReindex(customButton)
    }

    if (customButton.id == primaryCustomButtonId.value) {
      playerPreferences.primaryCustomButtonId.set(0)
    }
  }

  fun showDialog(dialog: CustomButtonDialog) {
    _dialog.update { _ -> dialog }
  }

  fun dismissDialog() {
    _dialog.update { _ -> CustomButtonDialog.None }
  }
}

sealed interface CustomButtonsUiState {
  data object Loading : CustomButtonsUiState
  data class Success(val buttons: List<CustomButtonEntity>) : CustomButtonsUiState
  data class Error(val message: String) : CustomButtonsUiState
}

fun CustomButtonsUiState.getButtons(): List<CustomButtonEntity> {
  return when (this) {
    is CustomButtonsUiState.Success -> this.buttons
    else -> emptyList()
  }
}
