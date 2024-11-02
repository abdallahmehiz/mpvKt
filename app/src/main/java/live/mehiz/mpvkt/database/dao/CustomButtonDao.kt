package live.mehiz.mpvkt.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import live.mehiz.mpvkt.database.entities.CustomButtonEntity

@Dao
interface CustomButtonDao {
  @Upsert
  suspend fun upsert(customButtonEntity: CustomButtonEntity)

  @Query("SELECT * FROM CustomButtonEntity ORDER BY `index`")
  fun getCustomButtons(): Flow<List<CustomButtonEntity>>

  @Query("SELECT * FROM CustomButtonEntity WHERE `index` > :currentIndex ORDER BY `index` ASC LIMIT 1")
  suspend fun getNextCustomButton(currentIndex: Int): CustomButtonEntity?

  @Query("SELECT * FROM CustomButtonEntity WHERE `index` < :currentIndex ORDER BY `index` DESC LIMIT 1")
  suspend fun getPreviousCustomButton(currentIndex: Int): CustomButtonEntity?

  @Transaction
  suspend fun increaseIndex(customButton: CustomButtonEntity) {
    val nextCustomButton = getNextCustomButton(customButton.index) ?: return

    val current = customButton.copy(index = nextCustomButton.index)
    val next = nextCustomButton.copy(index = customButton.index)

    updateCustomButton(current)
    updateCustomButton(next)
  }

  @Transaction
  suspend fun decreaseIndex(customButton: CustomButtonEntity) {
    val previousCustomButton = getPreviousCustomButton(customButton.index) ?: return

    val current = customButton.copy(index = previousCustomButton.index)
    val previous = previousCustomButton.copy(index = customButton.index)

    updateCustomButton(current)
    updateCustomButton(previous)
  }

  @Update
  suspend fun updateCustomButton(customButton: CustomButtonEntity)

  @Query("SELECT * FROM CustomButtonEntity WHERE `index` > :deletedIndex")
  suspend fun getNotesAfterOrder(deletedIndex: Int): List<CustomButtonEntity>

  @Transaction
  suspend fun deleteAndReindex(customButton: CustomButtonEntity) {
    val buttonsAfter = getNotesAfterOrder(customButton.index)

    deleteCustomButton(customButton)

    buttonsAfter.forEach { button ->
      val updatedButton = button.copy(index = button.index - 1)
      updateCustomButton(updatedButton)
    }
  }

  @Delete
  suspend fun deleteCustomButton(customButtonEntity: CustomButtonEntity)
}
