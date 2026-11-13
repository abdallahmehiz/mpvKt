package live.mehiz.mpvkt.di

import android.content.Context
import androidx.room.Room
import com.github.k1rakishou.fsaf.FileManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlinx.serialization.json.Json
import live.mehiz.mpvkt.database.Migrations
import live.mehiz.mpvkt.database.MpvKtDatabase
import live.mehiz.mpvkt.database.repository.CustomButtonRepositoryImpl
import live.mehiz.mpvkt.database.repository.PlaybackStateRepositoryImpl
import live.mehiz.mpvkt.domain.custombuttons.repository.CustomButtonRepository
import live.mehiz.mpvkt.domain.playbackstate.repository.PlaybackStateRepository
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.AppearancePreferences
import live.mehiz.mpvkt.preferences.AudioPreferences
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.GesturePreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.SubtitlesPreferences
import live.mehiz.mpvkt.preferences.preference.AndroidPreferenceStore
import live.mehiz.mpvkt.preferences.preference.PreferenceStore

@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
  val context: Context
  val json: Json
  val fileManager: FileManager

  val database: MpvKtDatabase
  val customButtonRepository: CustomButtonRepository
  val playbackStateRepository: PlaybackStateRepository

  val playerPreferences: PlayerPreferences
  val audioPreferences: AudioPreferences
  val subtitlesPreferences: SubtitlesPreferences
  val appearancePreferences: AppearancePreferences
  val decoderPreferences: DecoderPreferences
  val advancedPreferences: AdvancedPreferences
  val gesturePreferences: GesturePreferences

  @Binds val CustomButtonRepositoryImpl.bind: CustomButtonRepository

  @Binds val PlaybackStateRepositoryImpl.bind: PlaybackStateRepository

  @Provides
  @SingleIn(AppScope::class)
  fun provideJson(): Json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }

  @Provides
  @SingleIn(AppScope::class)
  fun provideFileManager(context: Context): FileManager = FileManager(context)

  @Provides
  @SingleIn(AppScope::class)
  fun provideDatabase(context: Context): MpvKtDatabase = Room
    .databaseBuilder(context, MpvKtDatabase::class.java, "mpvKt.db")
    .addMigrations(migrations = Migrations)
    .build()

  @Provides
  @SingleIn(AppScope::class)
  fun providePreferenceStore(context: Context): PreferenceStore = AndroidPreferenceStore(context)

  @DependencyGraph.Factory
  fun interface Factory {
    fun create(@Provides context: Context): AppGraph
  }
}
