package live.mehiz.mpvkt.di

import live.mehiz.mpvkt.preferences.AppearancePreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.AndroidPreferenceStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val PreferencesModule = module {
  single<AppearancePreferences> { AppearancePreferences(AndroidPreferenceStore(androidContext())) }
  single<PlayerPreferences> { PlayerPreferences(AndroidPreferenceStore(androidContext())) }
}
