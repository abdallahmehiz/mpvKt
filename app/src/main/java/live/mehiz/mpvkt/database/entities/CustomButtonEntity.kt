package live.mehiz.mpvkt.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CustomButtonEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,
  val title: String,
  val content: String,
  val longPressContent: String,
  val index: Int,
)
