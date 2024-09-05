package live.mehiz.mpvkt.preferences

import live.mehiz.mpvkt.preferences.preference.PreferenceStore
import live.mehiz.mpvkt.preferences.preference.getEnum
import live.mehiz.mpvkt.ui.player.Debanding

class DecoderPreferences(preferenceStore: PreferenceStore) {
  val tryHWDecoding = preferenceStore.getBoolean("try_hw_dec", true)
  val gpuNext = preferenceStore.getBoolean("gpu_next")
  val debanding = preferenceStore.getEnum("debanding", Debanding.None)
  val useYUV420P = preferenceStore.getBoolean("use_yuv420p", true)

  val brightnessFilter = preferenceStore.getInt("filter_brightness")
  val saturationFilter = preferenceStore.getInt("filter_saturation")
  val gammaFilter = preferenceStore.getInt("filter_gamma")
  val contrastFilter = preferenceStore.getInt("filter_contrast")
  val hueFilter = preferenceStore.getInt("filter_hue")
}
