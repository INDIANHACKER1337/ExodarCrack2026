/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.websocket.dto;

import java.util.List;

/**
 * Data Transfer Object for Setting serialization to JSON
 */
public class SettingDTO {
    public String id;
    public String name;
    public String type;
    public Object value;
    public boolean visible;

    // For number settings
    public Double min;
    public Double max;
    public String numberType; // "integer" or "double"

    // For mode settings
    public List<String> modes;
    public String currentMode;

    // For color settings
    public Integer red;
    public Integer green;
    public Integer blue;
    public Integer alpha;

    public SettingDTO() {}

    public SettingDTO(String id, String name, String type, Object value, boolean visible) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
        this.visible = visible;
    }
}
