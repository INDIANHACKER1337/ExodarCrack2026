/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

public enum ArrayListColorMode {
    RAINBOW("Rainbow"),
    RAINBOW_PASTEL("Rainbow Pastel"),
    TRANSGENDER("Transgender"),
    COLOR_FADE("Color Fade"),
    WHITE("White"),
    PURPLE("Purple"),
    TOXIC("Toxic"),
    PINK("Pink");

    private final String name;

    ArrayListColorMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ArrayListColorMode next() {
        ArrayListColorMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
