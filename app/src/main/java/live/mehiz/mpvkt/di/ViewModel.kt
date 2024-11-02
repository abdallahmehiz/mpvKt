package live.mehiz.mpvkt.di

import live.mehiz.mpvkt.ui.custombuttons.CustomButtonsScreenViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ViewModelModule = module {
  viewModel { CustomButtonsScreenViewModel(get()) }
}
