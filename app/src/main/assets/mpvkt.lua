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
function mpvkt.set_button_title(text)
   mp.set_property("user-data/mpvkt/set_button_title", text)
end
function mpvkt.reset_button_title(text)
    mp.set_property("user-data/mpvkt/reset_button_title", "unused")
end
function mpvkt.show_button()
    mp.set_property("user-data/mpvkt/toggle_button", "show")
end
function mpvkt.hide_button()
    mp.set_property("user-data/mpvkt/toggle_button", "hide")
end
function mpvkt.toggle_button()
    mp.set_property("user-data/mpvkt/toggle_button", "toggle")
end
function mpvkt.seek_by(value)
    mp.set_property("user-data/mpvkt/seek_by", value)
end
function mpvkt.seek_to(value)
    mp.set_property("user-data/mpvkt/seek_to", value)
end
function mpvkt.seek_by_with_text(value, text)
    mp.set_property("user-data/mpvkt/seek_by_with_text", value .. "|" .. text)
end
function mpvkt.seek_to_with_text(value, text)
    mp.set_property("user-data/mpvkt/seek_to_with_text", value .. "|" .. text)
end
function mpvkt.show_software_keyboard()
    mp.set_property("user-data/mpvkt/software_keyboard", "show")
end
function mpvkt.hide_software_keyboard()
    mp.set_property("user-data/mpvkt/software_keyboard", "hide")
end
function mpvkt.toggle_software_keyboard()
    mp.set_property("user-data/mpvkt/software_keyboard", "toggle")
end
return mpvkt
