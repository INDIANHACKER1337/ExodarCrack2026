/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.exodar.Main;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleManager;
import io.github.exodar.setting.*;
import io.github.exodar.websocket.dto.ModuleDTO;
import io.github.exodar.websocket.dto.SettingDTO;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WebSocket server for external GUI communication
 * Based on client-master's GhostClientWebSocketServer
 */
public class ExodarWebSocketServer extends WebSocketServer {

    private static final Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .create();
    private final Set<WebSocket> clients = new HashSet<>();
    private static ExodarWebSocketServer instance;

    public ExodarWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        instance = this;
        System.out.println("[Exodar WebSocket] Server created on port " + port);
    }

    public static ExodarWebSocketServer getInstance() {
        return instance;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        System.out.println("[Exodar WebSocket] Client connected: " + conn.getRemoteSocketAddress() + " - Total: " + clients.size());

        // Send welcome message
        try {
            JsonObject welcome = new JsonObject();
            welcome.addProperty("type", "connection_established");
            welcome.addProperty("status", "ready");
            welcome.addProperty("message", "Exodar WebSocket Server");
            welcome.addProperty("timestamp", System.currentTimeMillis());
            conn.send(gson.toJson(welcome));

            // Send module list
            sendModuleListToClient(conn);
        } catch (Exception e) {
            System.err.println("[Exodar WebSocket] Failed to send welcome: " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        System.out.println("[Exodar WebSocket] Client disconnected - Total: " + clients.size());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.get("type").getAsString();

            switch (type) {
                case "toggle_module":
                    handleToggleModule(conn, json);
                    break;
                case "setting_update":
                    handleSettingUpdate(conn, json);
                    break;
                case "keybind_update":
                    handleKeybindUpdate(conn, json);
                    break;
                case "save_config":
                    handleSaveConfig(conn, json);
                    break;
                case "load_config":
                    handleLoadConfig(conn, json);
                    break;
                case "list_configs":
                    handleListConfigs(conn);
                    break;
                case "get_modules":
                    sendModuleListToClient(conn);
                    break;
                default:
                    System.out.println("[Exodar WebSocket] Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("[Exodar WebSocket] Error processing message: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(conn, "error", e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[Exodar WebSocket] Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[Exodar WebSocket] ========================================");
        System.out.println("[Exodar WebSocket]   Server Started on ws://localhost:" + getPort());
        System.out.println("[Exodar WebSocket] ========================================");
        setConnectionLostTimeout(0);
    }

    // ========== MESSAGE HANDLERS ==========

    private void handleToggleModule(WebSocket conn, JsonObject json) {
        String moduleId = json.get("moduleId").getAsString();
        ModuleManager manager = Main.getModuleManager();
        if (manager == null) return;

        Module module = manager.getModuleByName(moduleId);
        if (module != null) {
            module.toggle();
            System.out.println("[Exodar WebSocket] Toggled " + moduleId + " to " + module.isEnabled());

            // Send confirmation
            JsonObject response = new JsonObject();
            response.addProperty("type", "module_toggled");
            response.addProperty("status", "success");
            response.addProperty("moduleId", moduleId);
            response.addProperty("enabled", module.isEnabled());
            response.addProperty("timestamp", System.currentTimeMillis());
            conn.send(gson.toJson(response));

            // Broadcast update to all clients
            broadcastModuleUpdate(module);
        }
    }

    private void handleSettingUpdate(WebSocket conn, JsonObject json) {
        String moduleId = json.get("moduleId").getAsString();
        String settingId = json.get("settingId").getAsString();

        ModuleManager manager = Main.getModuleManager();
        if (manager == null) return;

        Module module = manager.getModuleByName(moduleId);
        if (module == null) return;

        for (Setting setting : module.getSettings()) {
            String id = setting.getName().toLowerCase().replace(" ", "_");
            if (id.equals(settingId)) {
                updateSettingValue(setting, json);

                // Send confirmation
                JsonObject response = new JsonObject();
                response.addProperty("type", "setting_update_received");
                response.addProperty("status", "success");
                response.addProperty("moduleId", moduleId);
                response.addProperty("settingId", settingId);
                response.addProperty("timestamp", System.currentTimeMillis());
                conn.send(gson.toJson(response));

                // Broadcast update
                broadcastModuleUpdate(module);
                return;
            }
        }
    }

    private void updateSettingValue(Setting setting, JsonObject json) {
        try {
            if (setting instanceof TickSetting) {
                boolean value = json.get("value").getAsBoolean();
                ((TickSetting) setting).setEnabled(value);
            } else if (setting instanceof SliderSetting) {
                double value = json.get("value").getAsDouble();
                ((SliderSetting) setting).setValue(value);
            } else if (setting instanceof ModeSetting) {
                String value = json.get("value").getAsString();
                ((ModeSetting) setting).setSelected(value);
            } else if (setting instanceof ColorSetting) {
                ColorSetting color = (ColorSetting) setting;
                if (json.has("red")) color.setRed(json.get("red").getAsInt());
                if (json.has("green")) color.setGreen(json.get("green").getAsInt());
                if (json.has("blue")) color.setBlue(json.get("blue").getAsInt());
                if (json.has("alpha")) color.setAlpha(json.get("alpha").getAsInt());
            } else if (setting instanceof TextSetting) {
                String value = json.get("value").getAsString();
                ((TextSetting) setting).setValue(value);
            }
        } catch (Exception e) {
            System.err.println("[Exodar WebSocket] Failed to update setting: " + e.getMessage());
        }
    }

    private void handleKeybindUpdate(WebSocket conn, JsonObject json) {
        String moduleId = json.get("moduleId").getAsString();
        int keyCode = json.get("keyCode").getAsInt();

        ModuleManager manager = Main.getModuleManager();
        if (manager == null) return;

        Module module = manager.getModuleByName(moduleId);
        if (module != null) {
            module.setKeyCode(keyCode);

            JsonObject response = new JsonObject();
            response.addProperty("type", "keybind_update_received");
            response.addProperty("status", "success");
            response.addProperty("moduleId", moduleId);
            response.addProperty("keyCode", keyCode);
            response.addProperty("keyName", ModuleSerializer.getKeyName(keyCode));
            response.addProperty("timestamp", System.currentTimeMillis());
            conn.send(gson.toJson(response));

            broadcastModuleUpdate(module);
        }
    }

    private void handleSaveConfig(WebSocket conn, JsonObject json) {
        String profileName = json.get("profileName").getAsString();
        try {
            // TODO: Implement config save
            sendSuccessResponse(conn, "save_config_response", profileName);
        } catch (Exception e) {
            sendErrorResponse(conn, "save_config_failed", e.getMessage());
        }
    }

    private void handleLoadConfig(WebSocket conn, JsonObject json) {
        String profileName = json.get("profileName").getAsString();
        try {
            // TODO: Implement config load
            sendSuccessResponse(conn, "load_config_response", profileName);
            // Resend module list after loading
            sendModuleListToClient(conn);
        } catch (Exception e) {
            sendErrorResponse(conn, "load_config_failed", e.getMessage());
        }
    }

    private void handleListConfigs(WebSocket conn) {
        try {
            // TODO: Implement config listing
            List<String> configs = new ArrayList<>();
            configs.add("default");

            JsonObject response = new JsonObject();
            response.addProperty("type", "list_configs_response");
            response.add("configs", gson.toJsonTree(configs));
            conn.send(gson.toJson(response));
        } catch (Exception e) {
            sendErrorResponse(conn, "list_configs_failed", e.getMessage());
        }
    }

    // ========== BROADCAST METHODS ==========

    private void sendModuleListToClient(WebSocket conn) {
        try {
            ModuleManager manager = Main.getModuleManager();
            if (manager == null) return;

            JsonObject message = new JsonObject();
            message.addProperty("type", "module_list");
            message.addProperty("timestamp", System.currentTimeMillis());

            JsonArray modulesArray = new JsonArray();
            for (Module module : manager.getModules()) {
                ModuleDTO dto = ModuleSerializer.serializeModule(module);
                modulesArray.add(moduleToJson(dto));
            }
            message.add("data", modulesArray);
            conn.send(gson.toJson(message));
            System.out.println("[Exodar WebSocket] Sent " + modulesArray.size() + " modules to client");
        } catch (Exception e) {
            System.err.println("[Exodar WebSocket] Failed to send module list: " + e.getMessage());
        }
    }

    public void broadcastModuleUpdate(Module module) {
        try {
            ModuleDTO dto = ModuleSerializer.serializeModule(module);
            JsonObject message = new JsonObject();
            message.addProperty("type", "module_update");
            message.addProperty("timestamp", System.currentTimeMillis());
            message.add("data", moduleToJson(dto));
            broadcastToAll(gson.toJson(message));
        } catch (Exception e) {
            System.err.println("[Exodar WebSocket] Failed to broadcast module update: " + e.getMessage());
        }
    }

    public void broadcastModuleList() {
        try {
            ModuleManager manager = Main.getModuleManager();
            if (manager == null) return;

            JsonObject message = new JsonObject();
            message.addProperty("type", "module_list");
            message.addProperty("timestamp", System.currentTimeMillis());

            JsonArray modulesArray = new JsonArray();
            for (Module module : manager.getModules()) {
                ModuleDTO dto = ModuleSerializer.serializeModule(module);
                modulesArray.add(moduleToJson(dto));
            }
            message.add("data", modulesArray);
            broadcastToAll(gson.toJson(message));
        } catch (Exception e) {
            System.err.println("[Exodar WebSocket] Failed to broadcast module list: " + e.getMessage());
        }
    }

    private void broadcastToAll(String message) {
        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }

    // ========== JSON CONVERSION ==========

    private JsonObject moduleToJson(ModuleDTO dto) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", dto.id);
        obj.addProperty("name", dto.name);
        obj.addProperty("displayName", dto.displayName);
        obj.addProperty("description", dto.description);
        obj.addProperty("enabled", dto.enabled);
        obj.addProperty("category", dto.category);
        obj.addProperty("keyCode", dto.keyCode);
        obj.addProperty("keyName", dto.keyName);

        if (dto.settings != null) {
            JsonArray settingsArray = new JsonArray();
            for (SettingDTO setting : dto.settings) {
                settingsArray.add(settingToJson(setting));
            }
            obj.add("settings", settingsArray);
        }

        return obj;
    }

    private JsonObject settingToJson(SettingDTO dto) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", dto.id);
        obj.addProperty("name", dto.name);
        obj.addProperty("type", dto.type);
        obj.addProperty("visible", dto.visible);

        // Serialize value based on type
        if (dto.value == null) {
            obj.add("value", null);
        } else if (dto.value instanceof Boolean) {
            obj.addProperty("value", (Boolean) dto.value);
        } else if (dto.value instanceof Number) {
            obj.addProperty("value", (Number) dto.value);
        } else {
            obj.addProperty("value", dto.value.toString());
        }

        if (dto.min != null) obj.addProperty("min", dto.min);
        if (dto.max != null) obj.addProperty("max", dto.max);
        if (dto.numberType != null) obj.addProperty("numberType", dto.numberType);

        if (dto.modes != null) {
            JsonArray modesArray = new JsonArray();
            for (String mode : dto.modes) {
                modesArray.add(mode);
            }
            obj.add("modes", modesArray);
        }

        if (dto.currentMode != null) obj.addProperty("currentMode", dto.currentMode);

        // Color properties
        if (dto.red != null) obj.addProperty("red", dto.red);
        if (dto.green != null) obj.addProperty("green", dto.green);
        if (dto.blue != null) obj.addProperty("blue", dto.blue);
        if (dto.alpha != null) obj.addProperty("alpha", dto.alpha);

        return obj;
    }

    // ========== RESPONSE HELPERS ==========

    private void sendSuccessResponse(WebSocket conn, String type, String data) {
        JsonObject response = new JsonObject();
        response.addProperty("type", type);
        response.addProperty("status", "success");
        response.addProperty("data", data);
        response.addProperty("timestamp", System.currentTimeMillis());
        conn.send(gson.toJson(response));
    }

    private void sendErrorResponse(WebSocket conn, String type, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", type);
        response.addProperty("status", "error");
        response.addProperty("message", error);
        response.addProperty("timestamp", System.currentTimeMillis());
        if (conn != null && conn.isOpen()) {
            conn.send(gson.toJson(response));
        }
    }

    public int getConnectedClientsCount() {
        return clients.size();
    }
}
