mpvkt = {}
function mpvkt.show_text(text)
    mp.set_property("user-data/mpvkt/show_text", text)
end
function mpvkt.hide_ui()
    mp.set_property("user-data/mpvkt/hide_ui", "unused")
end
return mpvkt
