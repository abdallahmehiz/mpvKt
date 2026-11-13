package live.mehiz.mpvkt

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import live.mehiz.mpvkt.di.AppGraph
import live.mehiz.mpvkt.presentation.crash.CrashActivity
import live.mehiz.mpvkt.presentation.crash.GlobalExceptionHandler

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    graph = createGraphFactory<AppGraph.Factory>().create(this)
    Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(applicationContext, CrashActivity::class.java))
  }

  companion object {
    lateinit var graph: AppGraph
      private set
  }
}
