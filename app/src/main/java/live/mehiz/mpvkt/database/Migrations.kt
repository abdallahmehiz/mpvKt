package live.mehiz.mpvkt.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migrations: Array<Migration> = arrayOf(
  MIGRATION1to2
)

private object MIGRATION1to2 : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    listOf("subDelay", "secondarySubDelay", "audioDelay").forEach {
      db.execSQL("ALTER TABLE PlaybackStateEntity ADD COLUMN $it INTEGER NOT NULL DEFAULT 0")
    }
    db.execSQL("ALTER TABLE PlaybackStateEntity ADD COLUMN subSpeed REAL NOT NULL DEFAULT 0")
  }
}
