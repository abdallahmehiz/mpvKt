package live.mehiz.mpvkt.di

import live.mehiz.mpvkt.preferences.BasePreferences
import live.mehiz.mpvkt.preferences.preference.AndroidPreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val PreferencesModule = module {
  single<BasePreferences> { BasePreferences(AndroidPreferenceStore(androidContext())) }
}
