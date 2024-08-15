package live.mehiz.mpvkt

import android.app.Application
import live.mehiz.mpvkt.di.DatabaseModule
import live.mehiz.mpvkt.di.FileManagerModule
import live.mehiz.mpvkt.di.PreferencesModule
import live.mehiz.mpvkt.presentation.crash.CrashActivity
import live.mehiz.mpvkt.presentation.crash.GlobalExceptionHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(applicationContext, CrashActivity::class.java))
    startKoin {
      androidContext(this@App)
      modules(
        PreferencesModule,
        DatabaseModule,
        FileManagerModule,
      )
    }
  }
}
