package live.mehiz.mpvkt.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlaybackStateEntity(
  @PrimaryKey val mediaTitle: String,
  val lastPosition: Int, // in seconds
  val sid: Int,
  val secondarySid: Int,
  val aid: Int,
)
