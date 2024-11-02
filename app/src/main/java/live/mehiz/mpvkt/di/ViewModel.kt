package live.mehiz.mpvkt.di

import live.mehiz.mpvkt.ui.custombuttons.CustomButtonsScreenViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val ViewModelModule = module {
  viewModelOf(::CustomButtonsScreenViewModel)
}
