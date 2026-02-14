/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.setting;

public class DescriptionSetting extends Setting {
    private String description;

    public DescriptionSetting(String description) {
        super(description);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.setName(description); // Also update the name for display
    }

    @Override
    public void resetToDefault() {
        // No hay nada que resetear en una descripción
    }
}
