mpvkt = {}
function mpvkt.show_text(text)
    mp.set_property("user-data/mpvkt/show_text", text)
end
function mpvkt.hide_ui()
    mp.set_property("user-data/mpvkt/toggle_ui", "hide")
end
function mpvkt.show_ui()
    mp.set_property("user-data/mpvkt/toggle_ui", "show")
end
function mpvkt.toggle_ui()
    mp.set_property("user-data/mpvkt/toggle_ui", "toggle")
end
function mpvkt.show_subtitle_settings()
   mp.set_property("user-data/mpvkt/show_panel", "subtitle_settings")
end
function mpvkt.show_subtitle_delay()
    mp.set_property("user-data/mpvkt/show_panel", "subtitle_delay")
end
function mpvkt.show_audio_delay()
    mp.set_property("user-data/mpvkt/show_panel", "audio_delay")
end
function mpvkt.show_video_filters()
    mp.set_property("user-data/mpvkt/show_panel", "video_filters")
end
return mpvkt
