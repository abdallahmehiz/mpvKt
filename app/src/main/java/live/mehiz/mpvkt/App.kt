package live.mehiz.mpvkt

import android.app.Application
import live.mehiz.mpvkt.di.PreferencesModule
import org.koin.core.context.startKoin

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      modules(
        PreferencesModule
      )
    }
  }
}
