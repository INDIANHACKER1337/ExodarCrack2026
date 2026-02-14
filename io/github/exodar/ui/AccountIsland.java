/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import io.github.exodar.account.AccountManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * Floating island UI for account switching
 * Appears in top-center of screen with full account management
 */
public class AccountIsland {
    private static boolean expanded = false;
    private static boolean addingAccount = false;
    private static String inputText = "";
    private static boolean wasMouseDown = false;
    private static int scrollOffset = 0;
    private static long lastKeyTime = 0;

    // Original account (saved on first load)
    private static String originalUsername = null;
    private static String originalUUID = null;
    private static boolean originalSaved = false;

    // Session fields for account switching (Lunar may have multiple)
    private static java.util.List<Field> sessionFields = new java.util.ArrayList<>();

    static {
        try {
            Minecraft mc = Minecraft.getMinecraft();

            // Save original account on first load
            if (!originalSaved) {
                try {
                    Session session = mc.getSession();
                    if (session != null) {
                        originalUsername = session.getUsername();
                        originalUUID = session.getPlayerID();
                        originalSaved = true;
                    }
                } catch (Exception e) {
                    // Silent
                }
            }

            // Find ALL session fields in Minecraft class
            for (Field f : mc.getClass().getDeclaredFields()) {
                if (f.getType() == Session.class) {
                    f.setAccessible(true);
                    sessionFields.add(f);
                }
            }

            // Also check superclasses (Lunar might extend Minecraft)
            Class<?> superClass = mc.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                for (Field f : superClass.getDeclaredFields()) {
                    if (f.getType() == Session.class) {
                        f.setAccessible(true);
                        sessionFields.add(f);
                    }
                }
                superClass = superClass.getSuperclass();
            }
        } catch (Exception e) {
            // Silent
        }
    }

    // Island dimensions
    private static final int COLLAPSED_WIDTH = 100;
    private static final int COLLAPSED_HEIGHT = 16;
    private static final int EXPANDED_WIDTH = 140;
    private static final int ITEM_HEIGHT = 20;
    private static final int MAX_VISIBLE = 5;

    // Colors
    private static final int BG_COLOR = new Color(15, 15, 20, 220).getRGB();
    private static final int PANEL_COLOR = new Color(25, 25, 35, 220).getRGB();
    private static final int BORDER_COLOR = new Color(80, 80, 120, 200).getRGB();
    private static final int HOVER_COLOR = new Color(50, 50, 70, 220).getRGB();
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int ACCENT_COLOR = 0x55FFFF;
    private static final int ADD_COLOR = 0x55FF55;

    // Steve skin texture
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    public static void render(Minecraft mc, int screenWidth, int screenHeight) {
        FontRenderer fr = mc.fontRendererObj;
        if (fr == null) return;

        int mouseX = Mouse.getX() * screenWidth / mc.displayWidth;
        int mouseY = screenHeight - Mouse.getY() * screenHeight / mc.displayHeight - 1;
        boolean mouseDown = Mouse.isButtonDown(0);

        // Position: top center
        int x = (screenWidth - COLLAPSED_WIDTH) / 2;
        int y = 3;

        String currentUser = getCurrentUsername(mc);
        List<String> accounts = AccountManager.getInstance().getAccounts();

        // Check if mouse is over collapsed island
        boolean overIsland = mouseX >= x && mouseX <= x + COLLAPSED_WIDTH && mouseY >= y && mouseY <= y + COLLAPSED_HEIGHT;

        // Draw collapsed island
        Gui.drawRect(x, y, x + COLLAPSED_WIDTH, y + COLLAPSED_HEIGHT, BG_COLOR);

        // Top accent line
        Gui.drawRect(x, y, x + COLLAPSED_WIDTH, y + 1, ACCENT_COLOR);

        // Current account name centered
        String displayName = currentUser.length() > 12 ? currentUser.substring(0, 10) + ".." : currentUser;
        int textWidth = fr.getStringWidth(displayName);
        fr.drawStringWithShadow(displayName, x + (COLLAPSED_WIDTH - textWidth) / 2, y + 4, TEXT_COLOR);

        // Handle click to toggle expand
        if (overIsland && mouseDown && !wasMouseDown && !addingAccount) {
            expanded = !expanded;
            scrollOffset = 0;
        }

        // Draw expanded panel
        if (expanded) {
            int panelX = (screenWidth - EXPANDED_WIDTH) / 2;
            int panelY = y + COLLAPSED_HEIGHT + 2;
            boolean showOrigBtn = originalUsername != null && !originalUsername.equalsIgnoreCase(currentUser);
            int panelHeight = 30 + Math.min(accounts.size(), MAX_VISIBLE) * ITEM_HEIGHT + (addingAccount ? 25 : 0) + (showOrigBtn ? 17 : 0);

            // Panel background
            Gui.drawRect(panelX, panelY, panelX + EXPANDED_WIDTH, panelY + panelHeight, PANEL_COLOR);
            Gui.drawRect(panelX, panelY, panelX + EXPANDED_WIDTH, panelY + 1, BORDER_COLOR);
            Gui.drawRect(panelX, panelY + panelHeight - 1, panelX + EXPANDED_WIDTH, panelY + panelHeight, BORDER_COLOR);

            int itemY = panelY + 5;

            // Add Account button
            int addBtnW = EXPANDED_WIDTH - 10;
            int addBtnH = 14;
            int addBtnX = panelX + 5;
            boolean overAddBtn = mouseX >= addBtnX && mouseX <= addBtnX + addBtnW &&
                                 mouseY >= itemY && mouseY <= itemY + addBtnH;

            int addBtnColor = overAddBtn ? HOVER_COLOR : new Color(35, 35, 45).getRGB();
            Gui.drawRect(addBtnX, itemY, addBtnX + addBtnW, itemY + addBtnH, addBtnColor);

            String addText = addingAccount ? "Cancel" : "+ Add Account";
            int addTextColor = addingAccount ? 0xFF5555 : ADD_COLOR;
            int addTextW = fr.getStringWidth(addText);
            fr.drawStringWithShadow(addText, addBtnX + (addBtnW - addTextW) / 2, itemY + 3, addTextColor);

            if (overAddBtn && mouseDown && !wasMouseDown) {
                addingAccount = !addingAccount;
                inputText = "";
            }

            itemY += addBtnH + 3;

            // Return to Original button (only show if not already on original)
            boolean isOnOriginal = originalUsername != null && originalUsername.equalsIgnoreCase(currentUser);
            if (originalUsername != null && !isOnOriginal) {
                boolean overOrigBtn = mouseX >= addBtnX && mouseX <= addBtnX + addBtnW &&
                                     mouseY >= itemY && mouseY <= itemY + addBtnH;

                int origBtnColor = overOrigBtn ? HOVER_COLOR : new Color(35, 35, 45).getRGB();
                Gui.drawRect(addBtnX, itemY, addBtnX + addBtnW, itemY + addBtnH, origBtnColor);

                String origText = "↩ " + originalUsername;
                if (origText.length() > 14) {
                    origText = "↩ " + originalUsername.substring(0, 10) + "..";
                }
                int origTextW = fr.getStringWidth(origText);
                fr.drawStringWithShadow(origText, addBtnX + (addBtnW - origTextW) / 2, itemY + 3, 0xFFAA00);

                if (overOrigBtn && mouseDown && !wasMouseDown) {
                    if (switchToOriginal(mc)) {
                        ModuleNotification.addNotification("Restored: " + originalUsername, true);
                        expanded = false;
                    }
                }

                itemY += addBtnH + 3;
            }

            itemY += 2;

            // Input field if adding
            if (addingAccount) {
                int inputX = panelX + 5;
                int inputW = EXPANDED_WIDTH - 40;
                int inputH = 14;

                // Input box
                Gui.drawRect(inputX, itemY, inputX + inputW, itemY + inputH, new Color(40, 40, 50).getRGB());
                Gui.drawRect(inputX, itemY, inputX + inputW, itemY + 1, ACCENT_COLOR);

                // Input text with cursor
                String displayInput = inputText;
                if (System.currentTimeMillis() % 1000 < 500) {
                    displayInput += "|";
                }
                if (displayInput.isEmpty()) {
                    fr.drawStringWithShadow("Username...", inputX + 3, itemY + 3, 0x666666);
                } else {
                    fr.drawStringWithShadow(displayInput, inputX + 3, itemY + 3, TEXT_COLOR);
                }

                // OK button
                int okX = inputX + inputW + 3;
                int okW = 27;
                boolean overOk = mouseX >= okX && mouseX <= okX + okW &&
                                 mouseY >= itemY && mouseY <= itemY + inputH;
                Gui.drawRect(okX, itemY, okX + okW, itemY + inputH, overOk ? HOVER_COLOR : new Color(35, 35, 45).getRGB());
                fr.drawStringWithShadow("OK", okX + 8, itemY + 3, ADD_COLOR);

                if (overOk && mouseDown && !wasMouseDown && !inputText.isEmpty()) {
                    AccountManager.getInstance().addAccount(inputText);
                    ModuleNotification.addNotification("Added: " + inputText, true);
                    inputText = "";
                    addingAccount = false;
                }

                // Handle keyboard input
                handleKeyboardInput();

                itemY += inputH + 5;
            }

            // Separator
            Gui.drawRect(panelX + 5, itemY, panelX + EXPANDED_WIDTH - 5, itemY + 1, new Color(50, 50, 60).getRGB());
            itemY += 5;

            // Accounts list
            int visibleCount = Math.min(accounts.size(), MAX_VISIBLE);
            for (int i = scrollOffset; i < Math.min(scrollOffset + visibleCount, accounts.size()); i++) {
                String account = accounts.get(i);
                boolean isCurrentAccount = account.equalsIgnoreCase(currentUser);

                boolean overItem = mouseX >= panelX + 5 && mouseX <= panelX + EXPANDED_WIDTH - 20 &&
                                   mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT - 2;
                boolean overDelete = mouseX >= panelX + EXPANDED_WIDTH - 18 && mouseX <= panelX + EXPANDED_WIDTH - 5 &&
                                     mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT - 2;

                // Item background
                if (overItem || overDelete) {
                    Gui.drawRect(panelX + 5, itemY, panelX + EXPANDED_WIDTH - 5, itemY + ITEM_HEIGHT - 2, HOVER_COLOR);
                }

                // Current account indicator
                if (isCurrentAccount) {
                    Gui.drawRect(panelX + 5, itemY, panelX + 7, itemY + ITEM_HEIGHT - 2, ACCENT_COLOR);
                }

                // Steve head from skin texture
                int headX = panelX + 10;
                int headY = itemY + 2;
                int headSize = 14;
                drawSteveHead(mc, headX, headY, headSize);

                // Account name
                String accDisplay = account.length() > 10 ? account.substring(0, 8) + ".." : account;
                int accColor = isCurrentAccount ? ACCENT_COLOR : TEXT_COLOR;
                fr.drawStringWithShadow(accDisplay, headX + headSize + 4, itemY + 5, accColor);

                // Delete button (X)
                int deleteX = panelX + EXPANDED_WIDTH - 15;
                fr.drawStringWithShadow("x", deleteX, itemY + 5, overDelete ? 0xFF5555 : 0x666666);

                // Handle clicks
                if (mouseDown && !wasMouseDown) {
                    if (overDelete) {
                        AccountManager.getInstance().removeAccount(account);
                        ModuleNotification.addNotification("Removed: " + account, false);
                    } else if (overItem && !isCurrentAccount) {
                        if (switchAccount(mc, account)) {
                            ModuleNotification.addNotification("Switched: " + account, true);
                            expanded = false;
                        }
                    }
                }

                itemY += ITEM_HEIGHT;
            }

            // Scroll indicator
            if (accounts.size() > MAX_VISIBLE) {
                int scrollY = panelY + 30 + (addingAccount ? 25 : 0);
                int scrollH = MAX_VISIBLE * ITEM_HEIGHT;
                int thumbH = (int)((float)MAX_VISIBLE / accounts.size() * scrollH);
                int thumbY = scrollY + (int)((float)scrollOffset / (accounts.size() - MAX_VISIBLE) * (scrollH - thumbH));

                Gui.drawRect(panelX + EXPANDED_WIDTH - 3, scrollY, panelX + EXPANDED_WIDTH - 1, scrollY + scrollH, new Color(40, 40, 50).getRGB());
                Gui.drawRect(panelX + EXPANDED_WIDTH - 3, thumbY, panelX + EXPANDED_WIDTH - 1, thumbY + thumbH, ACCENT_COLOR);
            }

            // Handle scroll
            int scroll = Mouse.getDWheel();
            if (scroll != 0 && accounts.size() > MAX_VISIBLE) {
                if (scroll > 0 && scrollOffset > 0) {
                    scrollOffset--;
                } else if (scroll < 0 && scrollOffset < accounts.size() - MAX_VISIBLE) {
                    scrollOffset++;
                }
            }

            // Close if clicked outside
            boolean overPanel = mouseX >= panelX && mouseX <= panelX + EXPANDED_WIDTH &&
                                mouseY >= panelY && mouseY <= panelY + panelHeight;
            if (!overIsland && !overPanel && mouseDown && !wasMouseDown) {
                expanded = false;
                addingAccount = false;
                inputText = "";
            }
        }

        wasMouseDown = mouseDown;
    }

    private static void handleKeyboardInput() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                int key = Keyboard.getEventKey();
                char c = Keyboard.getEventCharacter();

                if (key == Keyboard.KEY_ESCAPE) {
                    addingAccount = false;
                    inputText = "";
                } else if (key == Keyboard.KEY_RETURN) {
                    if (!inputText.isEmpty()) {
                        AccountManager.getInstance().addAccount(inputText);
                        ModuleNotification.addNotification("Added: " + inputText, true);
                        inputText = "";
                        addingAccount = false;
                    }
                } else if (key == Keyboard.KEY_BACK) {
                    if (!inputText.isEmpty()) {
                        inputText = inputText.substring(0, inputText.length() - 1);
                    }
                } else if (c >= 32 && c < 127 && inputText.length() < 16) {
                    inputText += c;
                }
            }
        }
    }

    private static String getCurrentUsername(Minecraft mc) {
        try {
            Session session = mc.getSession();
            return session != null ? session.getUsername() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static boolean switchToOriginal(Minecraft mc) {
        if (originalUsername == null || originalUUID == null) {
            return false;
        }

        try {
            Session newSession = new Session(
                originalUsername,
                originalUUID,
                "0",
                "legacy"
            );

            if (sessionFields.isEmpty()) {
                return false;
            }

            for (Field field : sessionFields) {
                try {
                    field.set(mc, newSession);
                } catch (Exception e) {
                    // Silent
                }
            }

            AccountManager.getInstance().setCurrentAccount(originalUsername);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean switchAccount(Minecraft mc, String username) {
        try {
            String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()).toString().replace("-", "");

            Session newSession = new Session(
                username,
                uuid,
                "0",
                "legacy"
            );

            if (sessionFields.isEmpty()) {
                return false;
            }

            // Update ALL session fields
            for (Field field : sessionFields) {
                try {
                    field.set(mc, newSession);
                } catch (Exception e) {
                    // Silent
                }
            }

            AccountManager.getInstance().setCurrentAccount(username);

            // Verify via mc.getSession()
            Session mcSession = mc.getSession();
            String verifiedName = mcSession != null ? mcSession.getUsername() : "null";

            return verifiedName.equals(username);
        } catch (Exception e) {
            // Silent
        }
        return false;
    }

    public static boolean isExpanded() {
        return expanded;
    }

    public static boolean isAddingAccount() {
        return addingAccount;
    }

    /**
     * Draw Steve head from the skin texture
     * The head in MC skin is at UV (8,8) to (16,16) in a 64x64 texture
     */
    private static void drawSteveHead(Minecraft mc, int x, int y, int size) {
        try {
            mc.getTextureManager().bindTexture(STEVE_SKIN);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            // Steve skin is 64x64, head face is at (8,8) to (16,16)
            float texSize = 64.0f;
            float u1 = 8.0f / texSize;
            float v1 = 8.0f / texSize;
            float u2 = 16.0f / texSize;
            float v2 = 16.0f / texSize;

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(u1, v1); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(u1, v2); GL11.glVertex2f(x, y + size);
            GL11.glTexCoord2f(u2, v2); GL11.glVertex2f(x + size, y + size);
            GL11.glTexCoord2f(u2, v1); GL11.glVertex2f(x + size, y);
            GL11.glEnd();

            // Draw hat layer (overlay) at (40,8) to (48,16)
            float hu1 = 40.0f / texSize;
            float hv1 = 8.0f / texSize;
            float hu2 = 48.0f / texSize;
            float hv2 = 16.0f / texSize;

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(hu1, hv1); GL11.glVertex2f(x - 1, y - 1);
            GL11.glTexCoord2f(hu1, hv2); GL11.glVertex2f(x - 1, y + size + 1);
            GL11.glTexCoord2f(hu2, hv2); GL11.glVertex2f(x + size + 1, y + size + 1);
            GL11.glTexCoord2f(hu2, hv1); GL11.glVertex2f(x + size + 1, y - 1);
            GL11.glEnd();

            GL11.glDisable(GL11.GL_BLEND);
        } catch (Exception e) {
            // Fallback: draw simple box
            Gui.drawRect(x, y, x + size, y + size, new Color(60, 60, 70).getRGB());
        }
    }
}
