package com.nakwonelec.wallpaper.shinycolors

enum class intent(s: String) {
    Settings(this::class.java.toString() + ".Settings");
}

enum class Settings {
    ;
    enum class Control(s: String) {
        Reload("Reload"),
        Back("Back");
        companion object {
            override fun toString(): String {
                return "Control"
            }
        }


    }
    enum class Boolean(s: String) {
        Sound("Boolean.Sound");

        companion object {
            override fun toString(): String {
                return "Boolean"
            }
        }
    }
}