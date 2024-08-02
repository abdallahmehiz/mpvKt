package live.mehiz.mpvkt.di

import androidx.room.Room
import live.mehiz.mpvkt.database.Migrations
import live.mehiz.mpvkt.database.MpvKtDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val DatabaseModule = module {
  single<MpvKtDatabase> {
    Room
      .databaseBuilder(androidContext(), MpvKtDatabase::class.java, "mpvKt.db")
      .addMigrations(migrations = Migrations)
      .build()
  }
}
