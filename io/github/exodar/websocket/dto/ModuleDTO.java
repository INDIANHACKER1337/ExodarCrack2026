/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.websocket.dto;

import java.util.List;

/**
 * Data Transfer Object for Module serialization to JSON
 */
public class ModuleDTO {
    public String id;
    public String name;
    public String displayName;
    public String description;
    public boolean enabled;
    public String category;
    public int keyCode;
    public String keyName;
    public List<SettingDTO> settings;

    public ModuleDTO() {}

    public ModuleDTO(String id, String name, String displayName, String description,
                     boolean enabled, String category, int keyCode, String keyName,
                     List<SettingDTO> settings) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.enabled = enabled;
        this.category = category;
        this.keyCode = keyCode;
        this.keyName = keyName;
        this.settings = settings;
    }
}
