/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.misc;

import io.github.exodar.api.BuildInfo;
import io.github.exodar.config.UserConfig;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

import java.lang.reflect.Field;
import java.util.List;

/**
 * AutoRegister - Automatically responds to /register and /login prompts
 * Waits for the message within a timeout period after joining
 */
public class AutoRegister extends Module {

    // Settings
    private final TextSetting password;
    private final TickSetting debug;

    // Fixed values (not configurable)
    private static final long TIMEOUT_MS = 10000; // 10 seconds
    private static final long RESPONSE_DELAY_MS = 500; // 500ms delay before sending
    private static final int CHAT_HISTORY_CHECK = 5; // Check last 5 messages

    // State
    private long worldJoinTime = 0;
    private boolean hadWorld = false;
    private int lastWorldHash = 0;
    private boolean alreadyResponded = false;
    private String lastSavedPassword = "";
    private boolean checkedHistory = false;

    public AutoRegister() {
        super("AutoRegister", ModuleCategory.MISC);

        this.registerSetting(new DescriptionSetting("Auto /register and /login"));

        // Load password from UserConfig (not shared in config exports)
        String savedPassword = UserConfig.getInstance().getPassword("autoregister");
        if (savedPassword == null || savedPassword.isEmpty()) {
            savedPassword = "Exodar123"; // Default
        }

        this.registerSetting(password = new TextSetting("Password", savedPassword, 32));
        this.registerSetting(debug = new TickSetting("Debug", false));
        this.lastSavedPassword = savedPassword;

        // Debug only visible to developer
        debug.setVisible(BuildInfo.getInstance().isDeveloper());
    }

    @Override
    public void onEnable() {
        resetState();
        if (debug.isEnabled()) {
            System.out.println("[AutoRegister] Enabled - waiting for world");
        }
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        worldJoinTime = 0;
        hadWorld = false;
        lastWorldHash = 0;
        alreadyResponded = false;
        checkedHistory = false;
    }

    @Override
    public void onUpdate() {
        // Save password to UserConfig if changed (so it's not shared in config exports)
        String currentPassword = password.getValue();
        if (!currentPassword.equals(lastSavedPassword)) {
            UserConfig.getInstance().setPassword("autoregister", currentPassword);
            lastSavedPassword = currentPassword;
        }

        // Check if we have a world
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) {
            // No world - reset for next join
            if (hadWorld) {
                hadWorld = false;
                lastWorldHash = 0;
                alreadyResponded = false;
            }
            return;
        }

        // We have a world now
        int currentHash = System.identityHashCode(mc.theWorld);

        // First time entering world OR world changed
        if (!hadWorld || currentHash != lastWorldHash) {
            hadWorld = true;
            lastWorldHash = currentHash;
            worldJoinTime = System.currentTimeMillis();
            alreadyResponded = false;
            checkedHistory = false;
            if (debug.isEnabled()) {
                System.out.println("[AutoRegister] Entered world - listening for " + TIMEOUT_MS + "ms");
            }
        }

        // Check chat history once after entering world (with small delay to let chat load)
        if (!checkedHistory && !alreadyResponded && (System.currentTimeMillis() - worldJoinTime) > 200) {
            checkedHistory = true;
            checkChatHistory();
        }
    }

    /**
     * Check last few messages in chat history for login/register prompts
     */
    private void checkChatHistory() {
        try {
            if (mc.ingameGUI == null) return;
            GuiNewChat chatGui = mc.ingameGUI.getChatGUI();
            if (chatGui == null) return;

            // Use reflection to get chat lines
            List<ChatLine> chatLines = getChatLines(chatGui);
            if (chatLines == null || chatLines.isEmpty()) return;

            int toCheck = Math.min(CHAT_HISTORY_CHECK, chatLines.size());
            if (debug.isEnabled()) {
                System.out.println("[AutoRegister] Checking last " + toCheck + " chat messages");
            }

            for (int i = 0; i < toCheck; i++) {
                ChatLine line = chatLines.get(i);
                if (line != null && line.getChatComponent() != null) {
                    String text = line.getChatComponent().getUnformattedText();
                    if (text != null && !text.isEmpty()) {
                        if (debug.isEnabled()) {
                            System.out.println("[AutoRegister] History[" + i + "]: " + text);
                        }
                        processChat(text);
                        if (alreadyResponded) {
                            if (debug.isEnabled()) {
                                System.out.println("[AutoRegister] Found prompt in chat history!");
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (debug.isEnabled()) {
                System.out.println("[AutoRegister] Error checking chat history: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<ChatLine> getChatLines(GuiNewChat chatGui) {
        try {
            // Try to find the chatLines field (may have different names in different versions)
            for (Field field : chatGui.getClass().getDeclaredFields()) {
                if (field.getType() == List.class) {
                    field.setAccessible(true);
                    Object value = field.get(chatGui);
                    if (value instanceof List) {
                        List<?> list = (List<?>) value;
                        if (!list.isEmpty() && list.get(0) instanceof ChatLine) {
                            return (List<ChatLine>) value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (debug.isEnabled()) {
                System.out.println("[AutoRegister] Could not access chat lines: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Check if we're still within the timeout window (10 seconds)
     */
    private boolean isWithinTimeout() {
        return (System.currentTimeMillis() - worldJoinTime) < TIMEOUT_MS;
    }

    /**
     * Called when a packet is received - check for chat messages
     */
    @Override
    public boolean onReceivePacket(Object packet) {
        if (!enabled || packet == null) return true;

        // Check for chat packet
        if (packet instanceof S02PacketChat) {
            try {
                S02PacketChat chatPacket = (S02PacketChat) packet;
                IChatComponent chatComponent = chatPacket.getChatComponent();

                if (chatComponent != null) {
                    String text = chatComponent.getUnformattedText();
                    if (text != null && !text.isEmpty()) {
                        if (debug.isEnabled()) {
                            System.out.println("[AutoRegister] Chat: " + text);
                        }
                        processChat(text);
                    }
                }
            } catch (Exception e) {
                if (debug.isEnabled()) {
                    System.out.println("[AutoRegister] Error reading chat: " + e.getMessage());
                }
            }
        }

        return true;
    }

    private void processChat(String text) {
        if (text == null || text.isEmpty()) return;

        // Don't process if already responded this session
        if (alreadyResponded) {
            return;
        }

        // Don't process if timeout expired
        if (!isWithinTimeout()) {
            return;
        }

        String lowerText = text.toLowerCase();
        String pwd = password.getValue();

        // Check for login patterns
        boolean hasLogin = lowerText.contains("/login") ||
                          lowerText.contains("/l ") ||
                          lowerText.contains("/log ") ||
                          lowerText.contains("inicia sesi") ||
                          lowerText.contains("iniciar sesi") ||
                          lowerText.contains("logueate") ||
                          lowerText.contains("logeate") ||
                          lowerText.contains("please login") ||
                          lowerText.contains("please log in");

        // Check for register patterns
        boolean hasRegister = lowerText.contains("/register") ||
                             lowerText.contains("/reg ") ||
                             lowerText.contains("registrate") ||
                             lowerText.contains("registrar") ||
                             lowerText.contains("please register");

        // Determine which command to send
        if (hasRegister) {
            String registerCmd = "/register " + pwd + " " + pwd;
            if (debug.isEnabled()) {
                System.out.println("[AutoRegister] DETECTED REGISTER! Sending: " + registerCmd);
            }
            sendCommandWithDelay(registerCmd);
            alreadyResponded = true;
        } else if (hasLogin) {
            String loginCmd = "/login " + pwd;
            if (debug.isEnabled()) {
                System.out.println("[AutoRegister] DETECTED LOGIN! Sending: " + loginCmd);
            }
            sendCommandWithDelay(loginCmd);
            alreadyResponded = true;
        }
    }

    private void sendCommandWithDelay(String command) {
        // Send after delay in a new thread
        new Thread(() -> {
            try {
                Thread.sleep(RESPONSE_DELAY_MS);

                if (mc != null && mc.thePlayer != null) {
                    // Use the simple sendChatMessage method
                    mc.thePlayer.sendChatMessage(command);

                    if (debug.isEnabled()) {
                        System.out.println("[AutoRegister] Sent: " + command);
                    }
                }
            } catch (Exception e) {
                if (debug.isEnabled()) {
                    System.out.println("[AutoRegister] Error sending: " + e.getMessage());
                }
            }
        }, "AutoRegister-Delay").start();
    }

    @Override
    public String getDisplaySuffix() {
        if (alreadyResponded) {
            return " §a(Done)";
        }
        return null;
    }
}
