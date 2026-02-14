/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.ui;

import io.github.exodar.account.AccountManager;
import io.github.exodar.account.auth.MicrosoftAccount;
import io.github.exodar.account.auth.MicrosoftLogin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Full Account Manager Screen
 * Opens with INSERT key
 * Shows account list on the right, current account info on bottom-left
 * Features: Favorites, Generate Random, Microsoft (TODO)
 */
public class AccountManagerScreen extends GuiScreen {

    private final GuiScreen parentScreen;

    // Input state
    private boolean addingAccount = false;
    private String inputText = "";
    private int scrollOffset = 0;

    // Generated account state
    private String generatedName = null;

    // Original account (saved on first load)
    private static String originalUsername = null;
    private static String originalUUID = null;
    private static boolean originalSaved = false;

    // Session fields for account switching
    private static List<Field> sessionFields = new ArrayList<>();

    // Panel dimensions
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_MARGIN = 15;
    private static final int ITEM_HEIGHT = 32;
    private static final int MAX_VISIBLE = 8;
    private static final int HEADER_HEIGHT = 90;
    private static final int INPUT_HEIGHT = 35;

    // Colors - Dark theme with purple accent
    private static final int BG_OVERLAY = new Color(0, 0, 0, 180).getRGB();
    private static final int PANEL_BG = new Color(20, 20, 28, 245).getRGB();
    private static final int PANEL_HEADER = new Color(30, 30, 42, 255).getRGB();
    private static final int ITEM_BG = new Color(35, 35, 48, 255).getRGB();
    private static final int ITEM_HOVER = new Color(50, 50, 70, 255).getRGB();
    private static final int ITEM_SELECTED = new Color(60, 50, 90, 255).getRGB();
    private static final int ITEM_FAVORITE = new Color(50, 45, 35, 255).getRGB();
    private static final int ACCENT_COLOR = new Color(180, 130, 255).getRGB(); // Purple accent
    private static final int ACCENT_DARK = new Color(120, 80, 200).getRGB();
    private static final int TEXT_PRIMARY = 0xFFFFFF;
    private static final int TEXT_SECONDARY = 0xAAAAAA;
    private static final int TEXT_MUTED = 0x666666;
    private static final int SUCCESS_COLOR = 0x55FF55;
    private static final int DANGER_COLOR = 0xFF5555;
    private static final int WARNING_COLOR = 0xFFAA00;
    private static final int STAR_COLOR = 0xFFD700; // Gold
    private static final int MICROSOFT_COLOR = 0x00A4EF; // Microsoft blue
    private static final int INPUT_BG = new Color(25, 25, 35, 255).getRGB();
    private static final int INPUT_BORDER = new Color(60, 60, 80, 255).getRGB();

    // Steve skin texture
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    static {
        initSessionFields();
    }

    private static void initSessionFields() {
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

            // Also check superclasses
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

    public AccountManagerScreen(GuiScreen parent) {
        this.parentScreen = parent;
    }

    public GuiScreen getParentScreen() {
        return parentScreen;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        addingAccount = false;
        inputText = "";
        generatedName = null;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Dark overlay background
        drawRect(0, 0, width, height, BG_OVERLAY);

        FontRenderer fr = mc.fontRendererObj;
        List<String> accounts = AccountManager.getInstance().getAccounts();
        String currentUser = getCurrentUsername();

        // ============ BOTTOM LEFT - Current Account Info ============
        drawCurrentAccountInfo(fr, currentUser);

        // ============ RIGHT PANEL - Account List ============
        drawAccountPanel(fr, mouseX, mouseY, accounts, currentUser);

        // ============ CENTER - Title ============
        String title = "Account Manager";
        int titleWidth = fr.getStringWidth(title);
        fr.drawStringWithShadow(title, (width - PANEL_WIDTH - PANEL_MARGIN - titleWidth) / 2, 30, TEXT_PRIMARY);

        String subtitle = "Press INSERT or ESC to close";
        int subWidth = fr.getStringWidth(subtitle);
        fr.drawStringWithShadow(subtitle, (width - PANEL_WIDTH - PANEL_MARGIN - subWidth) / 2, 45, TEXT_MUTED);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Draw current account info at bottom-left
     */
    private void drawCurrentAccountInfo(FontRenderer fr, String currentUser) {
        int infoX = 20;
        int infoY = height - 60;
        int infoW = 200;
        int infoH = 45;

        // Background
        drawRect(infoX, infoY, infoX + infoW, infoY + infoH, PANEL_BG);

        // Left accent bar
        drawRect(infoX, infoY, infoX + 3, infoY + infoH, ACCENT_COLOR);

        // Steve head
        int headX = infoX + 12;
        int headY = infoY + 8;
        int headSize = 28;
        drawSteveHead(headX, headY, headSize);

        // Account info text
        fr.drawStringWithShadow("Account connected:", headX + headSize + 10, infoY + 10, TEXT_SECONDARY);

        // Username with accent color
        String displayName = currentUser.length() > 16 ? currentUser.substring(0, 14) + ".." : currentUser;
        fr.drawStringWithShadow(displayName, headX + headSize + 10, infoY + 24, ACCENT_COLOR);

        // Show if it's the original account
        if (originalUsername != null && originalUsername.equalsIgnoreCase(currentUser)) {
            fr.drawStringWithShadow("(Original)", infoX + infoW - 55, infoY + 17, TEXT_MUTED);
        }
    }

    /**
     * Draw the account list panel on the right
     */
    private void drawAccountPanel(FontRenderer fr, int mouseX, int mouseY, List<String> accounts, String currentUser) {
        int panelX = width - PANEL_WIDTH - PANEL_MARGIN;
        int panelY = PANEL_MARGIN;
        int panelH = height - PANEL_MARGIN * 2;

        // Panel background
        drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelH, PANEL_BG);

        // Panel header
        drawRect(panelX, panelY, panelX + PANEL_WIDTH, panelY + HEADER_HEIGHT, PANEL_HEADER);

        // Header title
        String headerTitle = "Accounts";
        fr.drawStringWithShadow(headerTitle, panelX + 15, panelY + 8, TEXT_PRIMARY);

        // Account count with Microsoft accounts
        int msCount = AccountManager.getInstance().getMicrosoftAccountCount();
        String countText = accounts.size() + " cracked" + (msCount > 0 ? ", " + msCount + " MS" : "");
        int countWidth = fr.getStringWidth(countText);
        fr.drawStringWithShadow(countText, panelX + PANEL_WIDTH - countWidth - 15, panelY + 8, TEXT_MUTED);

        // ============ BUTTON ROW 1: Add Account ============
        int btnY1 = panelY + 22;
        int btnH = 16;
        int fullBtnW = PANEL_WIDTH - 30;

        boolean overAddBtn = isMouseOver(mouseX, mouseY, panelX + 15, btnY1, fullBtnW, btnH);
        int addBtnColor = addingAccount ? DANGER_COLOR : (overAddBtn ? ITEM_HOVER : ITEM_BG);
        drawRect(panelX + 15, btnY1, panelX + 15 + fullBtnW, btnY1 + btnH, addBtnColor);

        String addText = addingAccount ? "Cancel" : "+ Add Cracked";
        int addTextColor = addingAccount ? TEXT_PRIMARY : SUCCESS_COLOR;
        int addTextW = fr.getStringWidth(addText);
        fr.drawStringWithShadow(addText, panelX + 15 + (fullBtnW - addTextW) / 2, btnY1 + 4, addTextColor);

        // ============ BUTTON ROW 2: Microsoft Login ============
        int btnY2 = btnY1 + btnH + 4;
        boolean overMsBtn = isMouseOver(mouseX, mouseY, panelX + 15, btnY2, fullBtnW, btnH);
        boolean msLoginInProgress = MicrosoftLogin.isLoginInProgress();
        int msBtnColor = msLoginInProgress ? WARNING_COLOR : (overMsBtn ? ITEM_HOVER : ITEM_BG);
        drawRect(panelX + 15, btnY2, panelX + 15 + fullBtnW, btnY2 + btnH, msBtnColor);
        // Microsoft icon indicator
        drawRect(panelX + 15, btnY2, panelX + 18, btnY2 + btnH, MICROSOFT_COLOR);

        String msText = msLoginInProgress ? "Logging in... (check browser)" : "+ Add Microsoft";
        int msTextColor = msLoginInProgress ? TEXT_PRIMARY : MICROSOFT_COLOR;
        int msTextW = fr.getStringWidth(msText);
        fr.drawStringWithShadow(msText, panelX + 15 + (fullBtnW - msTextW) / 2, btnY2 + 4, msTextColor);

        // ============ BUTTON ROW 3: Generate Random + Save ============
        int btnY3 = btnY2 + btnH + 4;
        int genBtnW = fullBtnW - 45; // Leave space for save button
        int saveBtnW = 40;

        // Generate button
        boolean overGenBtn = isMouseOver(mouseX, mouseY, panelX + 15, btnY3, genBtnW, btnH);
        drawRect(panelX + 15, btnY3, panelX + 15 + genBtnW, btnY3 + btnH, overGenBtn ? WARNING_COLOR : ITEM_BG);
        drawRect(panelX + 15, btnY3, panelX + 18, btnY3 + btnH, WARNING_COLOR);

        String genText = generatedName != null ? generatedName : "Generate Random";
        if (generatedName != null && fr.getStringWidth(genText) > genBtnW - 10) {
            genText = genText.substring(0, 12) + "..";
        }
        int genTextW = fr.getStringWidth(genText);
        fr.drawStringWithShadow(genText, panelX + 15 + (genBtnW - genTextW) / 2, btnY3 + 4,
                generatedName != null ? TEXT_PRIMARY : WARNING_COLOR);

        // Save button (only visible when name is generated)
        int saveBtnX = panelX + 15 + genBtnW + 5;
        if (generatedName != null) {
            boolean overSaveBtn = isMouseOver(mouseX, mouseY, saveBtnX, btnY3, saveBtnW, btnH);
            drawRect(saveBtnX, btnY3, saveBtnX + saveBtnW, btnY3 + btnH, overSaveBtn ? SUCCESS_COLOR : ACCENT_DARK);
            fr.drawStringWithShadow("Save", saveBtnX + 8, btnY3 + 4, TEXT_PRIMARY);
        } else {
            // Empty placeholder
            drawRect(saveBtnX, btnY3, saveBtnX + saveBtnW, btnY3 + btnH, INPUT_BG);
            fr.drawStringWithShadow("--", saveBtnX + 14, btnY3 + 4, TEXT_MUTED);
        }

        int contentY = panelY + HEADER_HEIGHT + 5;

        // Input field if adding account
        if (addingAccount) {
            contentY = drawInputField(fr, panelX, contentY, mouseX, mouseY);
        }

        // Return to Original button (if not on original)
        if (originalUsername != null && !originalUsername.equalsIgnoreCase(currentUser)) {
            int origBtnX = panelX + 10;
            int origBtnY = contentY;
            int origBtnW = PANEL_WIDTH - 20;
            int origBtnH = 22;

            boolean overOrigBtn = isMouseOver(mouseX, mouseY, origBtnX, origBtnY, origBtnW, origBtnH);
            drawRect(origBtnX, origBtnY, origBtnX + origBtnW, origBtnY + origBtnH,
                    overOrigBtn ? ITEM_HOVER : ITEM_BG);

            // Left accent
            drawRect(origBtnX, origBtnY, origBtnX + 3, origBtnY + origBtnH, WARNING_COLOR);

            String origText = "↩ Return to " + (originalUsername.length() > 12 ? originalUsername.substring(0, 10) + ".." : originalUsername);
            fr.drawStringWithShadow(origText, origBtnX + 10, origBtnY + 7, WARNING_COLOR);

            contentY += origBtnH + 6;
        }

        // Separator line
        drawRect(panelX + 10, contentY, panelX + PANEL_WIDTH - 10, contentY + 1, INPUT_BORDER);
        contentY += 6;

        // Get Microsoft accounts
        List<MicrosoftAccount> msAccounts = AccountManager.getInstance().getMicrosoftAccounts();

        // Calculate total items for scrolling
        int totalItems = msAccounts.size() + accounts.size();
        int listY = contentY;
        int listHeight = panelY + panelH - listY - 10;
        int visibleCount = Math.min(totalItems, listHeight / ITEM_HEIGHT);

        // Clamp scroll
        if (scrollOffset > Math.max(0, totalItems - visibleCount)) {
            scrollOffset = Math.max(0, totalItems - visibleCount);
        }

        int displayIndex = 0;

        // ============ Microsoft Accounts ============
        for (int i = 0; i < msAccounts.size(); i++) {
            int virtualIndex = i;
            if (virtualIndex < scrollOffset || virtualIndex >= scrollOffset + visibleCount) {
                displayIndex++;
                continue;
            }

            MicrosoftAccount msAcc = msAccounts.get(i);
            boolean isCurrentAccount = msAcc.username != null && msAcc.username.equalsIgnoreCase(currentUser)
                    && AccountManager.getInstance().isCurrentMicrosoft();

            int itemX = panelX + 10;
            int itemY = listY + (virtualIndex - scrollOffset) * ITEM_HEIGHT;
            int itemW = PANEL_WIDTH - 20;
            int itemH = ITEM_HEIGHT - 4;

            // Hit areas
            boolean overDelete = isMouseOver(mouseX, mouseY, itemX + itemW - 22, itemY, 22, itemH);
            boolean overItem = isMouseOver(mouseX, mouseY, itemX, itemY, itemW - 25, itemH);

            // Item background
            int bgColor = isCurrentAccount ? ITEM_SELECTED : (overItem || overDelete ? ITEM_HOVER : ITEM_BG);
            drawRect(itemX, itemY, itemX + itemW, itemY + itemH, bgColor);

            // Left accent - Microsoft blue
            drawRect(itemX, itemY, itemX + 3, itemY + itemH, MICROSOFT_COLOR);

            // MS icon indicator
            fr.drawStringWithShadow("M", itemX + 8, itemY + 10, MICROSOFT_COLOR);

            // Steve head
            int headX = itemX + 22;
            int headY_item = itemY + 4;
            int headSize = 20;
            drawSteveHead(headX, headY_item, headSize);

            // Account name
            String accDisplay = msAcc.username != null ?
                    (msAcc.username.length() > 12 ? msAcc.username.substring(0, 10) + ".." : msAcc.username)
                    : "Unknown";
            int accColor = isCurrentAccount ? ACCENT_COLOR : TEXT_PRIMARY;
            fr.drawStringWithShadow(accDisplay, headX + headSize + 6, itemY + 5, accColor);

            // Status text
            String statusText = isCurrentAccount ? "Connected (MS)" : "Microsoft Account";
            int statusColor = isCurrentAccount ? SUCCESS_COLOR : MICROSOFT_COLOR;
            fr.drawStringWithShadow(statusText, headX + headSize + 6, itemY + 16, statusColor);

            // Delete button (X)
            int delX = itemX + itemW - 18;
            int delY = itemY + (itemH - 12) / 2;
            int delColor = overDelete ? DANGER_COLOR : TEXT_MUTED;
            fr.drawStringWithShadow("×", delX, delY, delColor);

            displayIndex++;
        }

        // ============ Cracked Accounts ============
        for (int i = 0; i < accounts.size(); i++) {
            int virtualIndex = msAccounts.size() + i;
            if (virtualIndex < scrollOffset || virtualIndex >= scrollOffset + visibleCount) {
                continue;
            }

            String account = accounts.get(i);
            boolean isCurrentAccount = account.equalsIgnoreCase(currentUser) && !AccountManager.getInstance().isCurrentMicrosoft();
            boolean isFavorite = AccountManager.getInstance().isFavorite(account);

            int itemX = panelX + 10;
            int itemY = listY + (virtualIndex - scrollOffset) * ITEM_HEIGHT;
            int itemW = PANEL_WIDTH - 20;
            int itemH = ITEM_HEIGHT - 4;

            // Hit areas
            boolean overFavorite = isMouseOver(mouseX, mouseY, itemX, itemY, 20, itemH);
            boolean overDelete = isMouseOver(mouseX, mouseY, itemX + itemW - 22, itemY, 22, itemH);
            boolean overItem = isMouseOver(mouseX, mouseY, itemX + 20, itemY, itemW - 45, itemH);

            // Item background
            int bgColor;
            if (isCurrentAccount) {
                bgColor = ITEM_SELECTED;
            } else if (isFavorite) {
                bgColor = overItem || overFavorite || overDelete ? ITEM_HOVER : ITEM_FAVORITE;
            } else {
                bgColor = overItem || overFavorite || overDelete ? ITEM_HOVER : ITEM_BG;
            }
            drawRect(itemX, itemY, itemX + itemW, itemY + itemH, bgColor);

            // Left accent for current account
            if (isCurrentAccount) {
                drawRect(itemX, itemY, itemX + 3, itemY + itemH, ACCENT_COLOR);
            }

            // Favorite star (clickable)
            String star = isFavorite ? "★" : "☆";
            int starColor = isFavorite ? STAR_COLOR : (overFavorite ? STAR_COLOR : TEXT_MUTED);
            fr.drawStringWithShadow(star, itemX + 6, itemY + 10, starColor);

            // Steve head
            int headX = itemX + 22;
            int headY_item = itemY + 4;
            int headSize = 20;
            drawSteveHead(headX, headY_item, headSize);

            // Account name
            String accDisplay = account.length() > 12 ? account.substring(0, 10) + ".." : account;
            int accColor = isCurrentAccount ? ACCENT_COLOR : TEXT_PRIMARY;
            fr.drawStringWithShadow(accDisplay, headX + headSize + 6, itemY + 5, accColor);

            // Status text
            String statusText = isCurrentAccount ? "Connected" : (isFavorite ? "★ Favorite" : "Click to switch");
            int statusColor = isCurrentAccount ? SUCCESS_COLOR : (isFavorite ? STAR_COLOR : TEXT_MUTED);
            fr.drawStringWithShadow(statusText, headX + headSize + 6, itemY + 16, statusColor);

            // Delete button (X)
            int delX = itemX + itemW - 18;
            int delY = itemY + (itemH - 12) / 2;
            int delColor = overDelete ? DANGER_COLOR : TEXT_MUTED;
            fr.drawStringWithShadow("×", delX, delY, delColor);
        }

        // Scrollbar if needed
        if (totalItems > visibleCount && visibleCount > 0) {
            int scrollbarX = panelX + PANEL_WIDTH - 5;
            int scrollbarY = listY;
            int scrollbarH = listHeight;
            int thumbH = Math.max(20, (int)((float)visibleCount / totalItems * scrollbarH));
            int maxScroll = totalItems - visibleCount;
            int thumbY = scrollbarY + (maxScroll > 0 ? (int)((float)scrollOffset / maxScroll * (scrollbarH - thumbH)) : 0);

            // Scrollbar track
            drawRect(scrollbarX, scrollbarY, scrollbarX + 3, scrollbarY + scrollbarH, INPUT_BORDER);
            // Scrollbar thumb
            drawRect(scrollbarX, thumbY, scrollbarX + 3, thumbY + thumbH, ACCENT_COLOR);
        }

        // Empty state
        if (totalItems == 0) {
            String emptyText = "No accounts saved";
            int emptyW = fr.getStringWidth(emptyText);
            fr.drawStringWithShadow(emptyText, panelX + (PANEL_WIDTH - emptyW) / 2, listY + 20, TEXT_MUTED);

            String hintText = "Add cracked or Microsoft account";
            int hintW = fr.getStringWidth(hintText);
            fr.drawStringWithShadow(hintText, panelX + (PANEL_WIDTH - hintW) / 2, listY + 35, TEXT_MUTED);
        }
    }

    /**
     * Draw the input field for adding accounts
     */
    private int drawInputField(FontRenderer fr, int panelX, int startY, int mouseX, int mouseY) {
        int inputX = panelX + 10;
        int inputY = startY;
        int inputW = PANEL_WIDTH - 70;
        int inputH = 22;

        // Input background
        drawRect(inputX, inputY, inputX + inputW, inputY + inputH, INPUT_BG);
        drawRect(inputX, inputY, inputX + inputW, inputY + 1, ACCENT_COLOR); // Top accent

        // Input text
        String displayInput = inputText;
        if (System.currentTimeMillis() % 1000 < 500) {
            displayInput += "|";
        }
        if (inputText.isEmpty()) {
            fr.drawStringWithShadow("Enter username...", inputX + 5, inputY + 7, TEXT_MUTED);
        } else {
            fr.drawStringWithShadow(displayInput, inputX + 5, inputY + 7, TEXT_PRIMARY);
        }

        // Add button
        int addBtnX = inputX + inputW + 5;
        int addBtnW = 45;
        boolean overAdd = isMouseOver(mouseX, mouseY, addBtnX, inputY, addBtnW, inputH);

        drawRect(addBtnX, inputY, addBtnX + addBtnW, inputY + inputH,
                overAdd ? ACCENT_COLOR : ACCENT_DARK);

        String addBtnText = "Add";
        int addBtnTextW = fr.getStringWidth(addBtnText);
        fr.drawStringWithShadow(addBtnText, addBtnX + (addBtnW - addBtnTextW) / 2, inputY + 7, TEXT_PRIMARY);

        return startY + INPUT_HEIGHT;
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Close on ESC or INSERT
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_INSERT) {
            if (addingAccount) {
                addingAccount = false;
                inputText = "";
            } else {
                mc.displayGuiScreen(parentScreen);
            }
            return;
        }

        // Handle input when adding account
        if (addingAccount) {
            if (keyCode == Keyboard.KEY_RETURN) {
                if (!inputText.isEmpty()) {
                    AccountManager.getInstance().addAccount(inputText);
                    ModuleNotification.addNotification("Added: " + inputText, true);
                    inputText = "";
                    addingAccount = false;
                }
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
            } else if (typedChar >= 32 && typedChar < 127 && inputText.length() < 16) {
                inputText += typedChar;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        FontRenderer fr = mc.fontRendererObj;
        List<String> accounts = AccountManager.getInstance().getAccounts();
        String currentUser = getCurrentUsername();

        int panelX = width - PANEL_WIDTH - PANEL_MARGIN;
        int panelY = PANEL_MARGIN;
        int panelH = height - PANEL_MARGIN * 2;

        // Button dimensions
        int btnH = 16;
        int fullBtnW = PANEL_WIDTH - 30;
        int btnY1 = panelY + 22;
        int btnY2 = btnY1 + btnH + 4;
        int btnY3 = btnY2 + btnH + 4;
        int genBtnW = fullBtnW - 45;
        int saveBtnW = 40;
        int saveBtnX = panelX + 15 + genBtnW + 5;

        // Add Account button
        if (isMouseOver(mouseX, mouseY, panelX + 15, btnY1, fullBtnW, btnH)) {
            addingAccount = !addingAccount;
            inputText = "";
            generatedName = null;
            return;
        }

        // Microsoft button - start OAuth flow
        if (isMouseOver(mouseX, mouseY, panelX + 15, btnY2, fullBtnW, btnH)) {
            if (!MicrosoftLogin.isLoginInProgress()) {
                AccountManager.getInstance().startMicrosoftLogin();
                ModuleNotification.addNotification("Opening browser for Microsoft login...", true);
            } else {
                ModuleNotification.addNotification("Login already in progress!", false);
            }
            return;
        }

        // Generate Random button
        if (isMouseOver(mouseX, mouseY, panelX + 15, btnY3, genBtnW, btnH)) {
            generatedName = AccountManager.generateRandomUsername();
            // Switch to generated account immediately (but don't save)
            if (switchAccount(generatedName)) {
                ModuleNotification.addNotification("Using: " + generatedName, true);
            }
            return;
        }

        // Save button (only if name generated)
        if (generatedName != null && isMouseOver(mouseX, mouseY, saveBtnX, btnY3, saveBtnW, btnH)) {
            AccountManager.getInstance().addAccount(generatedName);
            ModuleNotification.addNotification("Saved: " + generatedName, true);
            generatedName = null;
            return;
        }

        int contentY = panelY + HEADER_HEIGHT + 5;

        // Input field Add button
        if (addingAccount) {
            int inputX = panelX + 10;
            int inputW = PANEL_WIDTH - 70;
            int inputH = 22;
            int btnX = inputX + inputW + 5;
            int btnW = 45;

            if (isMouseOver(mouseX, mouseY, btnX, contentY, btnW, inputH) && !inputText.isEmpty()) {
                AccountManager.getInstance().addAccount(inputText);
                ModuleNotification.addNotification("Added: " + inputText, true);
                inputText = "";
                addingAccount = false;
                return;
            }
            contentY += INPUT_HEIGHT;
        }

        // Return to Original button
        if (originalUsername != null && !originalUsername.equalsIgnoreCase(currentUser)) {
            int origBtnX = panelX + 10;
            int origBtnY = contentY;
            int origBtnW = PANEL_WIDTH - 20;
            int origBtnH = 22;

            if (isMouseOver(mouseX, mouseY, origBtnX, origBtnY, origBtnW, origBtnH)) {
                if (switchToOriginal()) {
                    ModuleNotification.addNotification("Restored: " + originalUsername, true);
                }
                return;
            }
            contentY += origBtnH + 6;
        }

        // Skip separator
        contentY += 7;

        // Account list clicks
        List<MicrosoftAccount> msAccounts = AccountManager.getInstance().getMicrosoftAccounts();
        int totalItems = msAccounts.size() + accounts.size();
        int listY = contentY;
        int listHeight = panelY + panelH - listY - 10;
        int visibleCount = Math.min(totalItems, listHeight / ITEM_HEIGHT);

        // ============ Microsoft Account clicks ============
        for (int i = 0; i < msAccounts.size(); i++) {
            int virtualIndex = i;
            if (virtualIndex < scrollOffset || virtualIndex >= scrollOffset + visibleCount) {
                continue;
            }

            MicrosoftAccount msAcc = msAccounts.get(i);
            boolean isCurrentAccount = msAcc.username != null && msAcc.username.equalsIgnoreCase(currentUser)
                    && AccountManager.getInstance().isCurrentMicrosoft();

            int itemX = panelX + 10;
            int itemY = listY + (virtualIndex - scrollOffset) * ITEM_HEIGHT;
            int itemW = PANEL_WIDTH - 20;
            int itemH = ITEM_HEIGHT - 4;

            boolean overDelete = isMouseOver(mouseX, mouseY, itemX + itemW - 22, itemY, 22, itemH);
            boolean overItem = isMouseOver(mouseX, mouseY, itemX, itemY, itemW - 25, itemH);

            if (overDelete) {
                AccountManager.getInstance().removeMicrosoftAccount(msAcc.uuid);
                ModuleNotification.addNotification("Removed MS: " + msAcc.username, false);
                return;
            } else if (overItem && !isCurrentAccount) {
                // Login with refresh token
                ModuleNotification.addNotification("Logging in as " + msAcc.username + "...", true);
                new Thread(() -> {
                    if (AccountManager.getInstance().loginMicrosoftAccount(msAcc)) {
                        ModuleNotification.addNotification("Logged in: " + msAcc.username, true);
                    } else {
                        ModuleNotification.addNotification("Login failed - try re-adding account", false);
                    }
                }).start();
                return;
            }
        }

        // ============ Cracked Account clicks ============
        for (int i = 0; i < accounts.size(); i++) {
            int virtualIndex = msAccounts.size() + i;
            if (virtualIndex < scrollOffset || virtualIndex >= scrollOffset + visibleCount) {
                continue;
            }

            String account = accounts.get(i);
            boolean isCurrentAccount = account.equalsIgnoreCase(currentUser) && !AccountManager.getInstance().isCurrentMicrosoft();

            int itemX = panelX + 10;
            int itemY = listY + (virtualIndex - scrollOffset) * ITEM_HEIGHT;
            int itemW = PANEL_WIDTH - 20;
            int itemH = ITEM_HEIGHT - 4;

            // Hit areas
            boolean overFavorite = isMouseOver(mouseX, mouseY, itemX, itemY, 20, itemH);
            boolean overDelete = isMouseOver(mouseX, mouseY, itemX + itemW - 22, itemY, 22, itemH);
            boolean overItem = isMouseOver(mouseX, mouseY, itemX + 20, itemY, itemW - 45, itemH);

            if (overFavorite) {
                // Toggle favorite
                AccountManager.getInstance().toggleFavorite(account);
                boolean nowFav = AccountManager.getInstance().isFavorite(account);
                ModuleNotification.addNotification(account + (nowFav ? " ★ favorited" : " unfavorited"), nowFav);
                return;
            } else if (overDelete) {
                AccountManager.getInstance().removeAccount(account);
                ModuleNotification.addNotification("Removed: " + account, false);
                return;
            } else if (overItem && !isCurrentAccount) {
                if (switchAccount(account)) {
                    AccountManager.getInstance().setCurrentIsMicrosoft(false);
                    ModuleNotification.addNotification("Switched: " + account, true);
                    generatedName = null; // Clear generated state
                }
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            List<String> accounts = AccountManager.getInstance().getAccounts();
            int msCount = AccountManager.getInstance().getMicrosoftAccountCount();
            int totalItems = msCount + accounts.size();
            int panelH = height - PANEL_MARGIN * 2;
            int listHeight = panelH - HEADER_HEIGHT - 50;
            int visibleCount = Math.min(totalItems, listHeight / ITEM_HEIGHT);
            int maxScroll = Math.max(0, totalItems - visibleCount);

            if (scroll > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ============ HELPER METHODS ============

    private String getCurrentUsername() {
        try {
            Session session = mc.getSession();
            return session != null ? session.getUsername() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private boolean switchToOriginal() {
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

    private boolean switchAccount(String username) {
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

            for (Field field : sessionFields) {
                try {
                    field.set(mc, newSession);
                } catch (Exception e) {
                    // Silent
                }
            }

            AccountManager.getInstance().setCurrentAccount(username);

            Session mcSession = mc.getSession();
            String verifiedName = mcSession != null ? mcSession.getUsername() : "null";

            return verifiedName.equals(username);
        } catch (Exception e) {
            // Silent
        }
        return false;
    }

    /**
     * Draw Steve head from the skin texture
     */
    private void drawSteveHead(int x, int y, int size) {
        try {
            mc.getTextureManager().bindTexture(STEVE_SKIN);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

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

            // Draw hat layer
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
            drawRect(x, y, x + size, y + size, new Color(60, 60, 70).getRGB());
        }
    }
}
