package live.mehiz.mpvkt.domain.custombuttons.repository

import kotlinx.coroutines.flow.Flow
import live.mehiz.mpvkt.database.entities.CustomButtonEntity

interface CustomButtonRepository {
  fun getCustomButtons(): Flow<List<CustomButtonEntity>>

  suspend fun upsert(customButtonEntity: CustomButtonEntity)

  suspend fun deleteAndReindex(customButtonEntity: CustomButtonEntity)

  suspend fun increaseIndex(customButtonEntity: CustomButtonEntity)

  suspend fun decreaseIndex(customButtonEntity: CustomButtonEntity)

  suspend fun updateButton(customButtonEntity: CustomButtonEntity)
}
