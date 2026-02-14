/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.websocket;

import io.github.exodar.module.Module;
import io.github.exodar.setting.*;
import io.github.exodar.websocket.dto.ModuleDTO;
import io.github.exodar.websocket.dto.SettingDTO;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Serializes Module objects to DTOs for JSON transmission
 */
public class ModuleSerializer {

    /**
     * Serialize a module to a DTO
     */
    public static ModuleDTO serializeModule(Module module) {
        ModuleDTO dto = new ModuleDTO();
        dto.id = module.getName().toLowerCase().replace(" ", "_");
        dto.name = module.getName();
        dto.displayName = module.getName();
        dto.enabled = module.isEnabled();
        dto.category = module.getCategory().name();
        dto.keyCode = module.getKeyCode();
        dto.keyName = getKeyName(module.getKeyCode());

        // Get description from DescriptionSetting if available
        for (Setting setting : module.getSettings()) {
            if (setting instanceof DescriptionSetting) {
                dto.description = setting.getName();
                break;
            }
        }

        // Serialize settings
        dto.settings = new ArrayList<>();
        for (Setting setting : module.getSettings()) {
            SettingDTO settingDTO = serializeSetting(setting);
            if (settingDTO != null) {
                dto.settings.add(settingDTO);
            }
        }

        return dto;
    }

    /**
     * Serialize a setting to a DTO
     */
    public static SettingDTO serializeSetting(Setting setting) {
        if (setting instanceof DescriptionSetting) {
            return null; // Skip description settings
        }

        SettingDTO dto = new SettingDTO();
        dto.id = setting.getName().toLowerCase().replace(" ", "_");
        dto.name = setting.getName();
        dto.visible = setting.isVisible();

        if (setting instanceof TickSetting) {
            TickSetting tick = (TickSetting) setting;
            dto.type = "boolean";
            dto.value = tick.isEnabled();
        } else if (setting instanceof SliderSetting) {
            SliderSetting slider = (SliderSetting) setting;
            dto.type = "slider";
            dto.value = slider.getValue();
            dto.min = slider.getMin();
            dto.max = slider.getMax();
            dto.numberType = slider.getInterval() % 1 == 0 ? "integer" : "double";
        } else if (setting instanceof DoubleSliderSetting) {
            DoubleSliderSetting slider = (DoubleSliderSetting) setting;
            dto.type = "range";
            dto.value = slider.getValueMin() + "-" + slider.getValueMax();
            dto.min = slider.getMin();
            dto.max = slider.getMax();
        } else if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) setting;
            dto.type = "mode";
            dto.value = mode.getSelected();
            dto.modes = Arrays.asList(mode.getModes());
            dto.currentMode = mode.getSelected();
        } else if (setting instanceof ColorSetting) {
            ColorSetting color = (ColorSetting) setting;
            dto.type = "color";
            dto.value = color.getHexString();
            dto.red = color.getRed();
            dto.green = color.getGreen();
            dto.blue = color.getBlue();
            dto.alpha = color.getAlpha();
        } else if (setting instanceof KeybindSetting) {
            KeybindSetting keybind = (KeybindSetting) setting;
            dto.type = "keybind";
            dto.value = keybind.getKeyCode();
        } else if (setting instanceof TextSetting) {
            TextSetting text = (TextSetting) setting;
            dto.type = "text";
            dto.value = text.getValue();
        } else {
            dto.type = "unknown";
            dto.value = setting.getName();
        }

        return dto;
    }

    /**
     * Get key name from key code
     */
    public static String getKeyName(int keyCode) {
        if (keyCode == 0) return "NONE";
        try {
            String name = Keyboard.getKeyName(keyCode);
            return name != null ? name : "KEY_" + keyCode;
        } catch (Exception e) {
            return "KEY_" + keyCode;
        }
    }
}
