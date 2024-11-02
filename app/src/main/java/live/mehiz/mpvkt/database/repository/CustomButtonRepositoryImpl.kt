package live.mehiz.mpvkt.database.repository

import kotlinx.coroutines.flow.Flow
import live.mehiz.mpvkt.database.MpvKtDatabase
import live.mehiz.mpvkt.database.entities.CustomButtonEntity
import live.mehiz.mpvkt.domain.custombuttons.repository.CustomButtonRepository

class CustomButtonRepositoryImpl(
  private val database: MpvKtDatabase,
) : CustomButtonRepository {
  override fun getCustomButtons(): Flow<List<CustomButtonEntity>> {
    return database.customButtonDao().getCustomButtons()
  }

  override suspend fun upsert(customButtonEntity: CustomButtonEntity) {
    database.customButtonDao().upsert(customButtonEntity)
  }

  override suspend fun deleteAndReindex(customButtonEntity: CustomButtonEntity) {
    database.customButtonDao().deleteAndReindex(customButtonEntity)
  }

  override suspend fun increaseIndex(customButtonEntity: CustomButtonEntity) {
    database.customButtonDao().increaseIndex(customButtonEntity)
  }

  override suspend fun decreaseIndex(customButtonEntity: CustomButtonEntity) {
    database.customButtonDao().decreaseIndex(customButtonEntity)
  }

  override suspend fun updateButton(customButtonEntity: CustomButtonEntity) {
    database.customButtonDao().updateCustomButton(customButtonEntity)
  }
}
