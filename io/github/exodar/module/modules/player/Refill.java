/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.module.modules.player;

import io.github.exodar.event.Render2DEvent;
import io.github.exodar.event.Subscribe;
import io.github.exodar.module.Module;
import io.github.exodar.module.ModuleCategory;
import io.github.exodar.setting.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

public class Refill extends Module {

  private final ModeSetting type;
  private final ModeSetting mode;
  private final ModeSetting order;
  private final TickSetting alwaysRefill;
  private final KeybindSetting forceRefillKey;

  private final SliderSetting startDelayMin;
  private final SliderSetting startDelayMax;
  private final SliderSetting actionDelayMin;
  private final SliderSetting actionDelayMax;
  private final SliderSetting mouseSpeed;
  private final SliderSetting curveFactor;
  private final SliderSetting jitterStrength;
  private final TickSetting blockMouse;
  private final TickSetting autoClose;
  private final SliderSetting closeDelayMin;
  private final SliderSetting closeDelayMax;

  private final SliderSetting spoofMinDelay;
  private final SliderSetting spoofMaxDelay;

  private final Random random = new Random();

  private long openTimestamp;
  private long nextActionTime;
  private Action currentAction;
  private Point currentPos = new Point(0.0D, 0.0D);
  private Point startPos = new Point(0.0D, 0.0D);
  private Point endPos = new Point(0.0D, 0.0D);
  private Point controlPos = new Point(0.0D, 0.0D);
  private long movementStartTime;
  private long movementDuration;
  private boolean active;
  private boolean finished;
  private long closeTime;

  private final List<Integer> slotsToRefill = new ArrayList<>();
  private int currentIndex = 0;
  private long lastClickTime = 0;
  private boolean serverInventoryOpen = false;

  private boolean forceRefillActive = false;
  private boolean forceRefillKeyWasPressed = false;
  private boolean forceOpenedInventory = false;
  private boolean didWork = false;

  public Refill() {
    super("Refill", ModuleCategory.PLAYER);

    this.registerSetting(new DescriptionSetting("Auto refill soup/pots to hotbar"));
    this.registerSetting(type = new ModeSetting("Type", new String[]{"Both", "Pots", "Soup"}));
    this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Legit", "Spoof", "Instant"})
        .onChange(this::updateSettingsVisibility));
    this.registerSetting(order = new ModeSetting("Order", new String[]{"Linear", "Random"}));
    this.registerSetting(alwaysRefill = new TickSetting("Always Refill", true));
    this.registerSetting(forceRefillKey = new KeybindSetting("Force Refill Key", 0));

    this.registerSetting(new DescriptionSetting("-- Legit Mode --"));
    this.registerSetting(startDelayMin = new SliderSetting("Start Delay Min", 150.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(startDelayMax = new SliderSetting("Start Delay Max", 300.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(actionDelayMin = new SliderSetting("Action Delay Min", 80.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(actionDelayMax = new SliderSetting("Action Delay Max", 150.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(mouseSpeed = new SliderSetting("Mouse Speed", 20.0D, 5.0D, 100.0D, 1.0D));
    this.registerSetting(curveFactor = new SliderSetting("Curve Factor", 2.5D, 0.0D, 5.0D, 0.1D));
    this.registerSetting(jitterStrength = new SliderSetting("Jitter", 0.5D, 0.0D, 2.0D, 0.1D));
    this.registerSetting(blockMouse = new TickSetting("Block Mouse", true));
    this.registerSetting(autoClose = new TickSetting("Auto Close", true));
    this.registerSetting(closeDelayMin = new SliderSetting("Close Delay Min", 100.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(closeDelayMax = new SliderSetting("Close Delay Max", 200.0D, 0.0D, 500.0D, 10.0D));

    this.registerSetting(new DescriptionSetting("-- Spoof Mode --"));
    this.registerSetting(spoofMinDelay = new SliderSetting("Spoof Min Delay", 10.0D, 10.0D, 100.0D, 5.0D));
    this.registerSetting(spoofMaxDelay = new SliderSetting("Spoof Max Delay", 50.0D, 50.0D, 200.0D, 5.0D));

    updateSettingsVisibility();
  }

  private void updateSettingsVisibility() {
    String selected = mode.getSelected();
    boolean isLegit = selected.equals("Legit");
    boolean isSpoof = selected.equals("Spoof");

    startDelayMin.setVisible(isLegit);
    startDelayMax.setVisible(isLegit);
    actionDelayMin.setVisible(isLegit);
    actionDelayMax.setVisible(isLegit);
    mouseSpeed.setVisible(isLegit);
    curveFactor.setVisible(isLegit);
    jitterStrength.setVisible(isLegit);
    blockMouse.setVisible(isLegit);
    closeDelayMin.setVisible(isLegit);
    closeDelayMax.setVisible(isLegit);

    spoofMinDelay.setVisible(isSpoof);
    spoofMaxDelay.setVisible(isSpoof);
  }

  @Override
  public void onEnable() {
    reset();
  }

  @Override
  public void onDisable() {
    closeServerInventory();
    InventoryModuleHelper.release("Refill");
    reset();
  }

  @Subscribe
  public void onRender(Render2DEvent event) {
    if (!enabled || mc.thePlayer == null) return;
    if (!mode.getSelected().equals("Legit")) return;

    checkForceRefillKey();

    if (!(mc.currentScreen instanceof GuiInventory)) {
      if (active || currentAction != null) {
        InventoryModuleHelper.release("Refill");
      }
      resetState();
      return;
    }

    if (!alwaysRefill.isEnabled() && !forceRefillActive) {
      return;
    }

    if (!InventoryModuleHelper.tryAcquire("Refill")) return;

    GuiInventory gui = (GuiInventory) mc.currentScreen;

    if (this.openTimestamp == 0L) {
      this.openTimestamp = System.currentTimeMillis();
      long startDelay = randomInt((int) this.startDelayMin.getValue(), (int) this.startDelayMax.getValue());
      this.nextActionTime = this.openTimestamp + startDelay;
      this.currentPos.x = (Mouse.getX() * gui.width / mc.displayWidth);
      this.currentPos.y = (gui.height - Mouse.getY() * gui.height / mc.displayHeight - 1);
      this.active = false;
      this.currentAction = null;
      this.finished = false;
      this.didWork = false;
    }

    if (this.blockMouse.isEnabled() && this.active && !this.finished && this.currentAction != null)
      Mouse.setCursorPosition((int) (this.currentPos.x * mc.displayWidth / gui.width), mc.displayHeight - (int) (this.currentPos.y * mc.displayHeight / gui.height) - 1);

    if (System.currentTimeMillis() < this.nextActionTime && !this.active) return;

    this.active = true;
    InventoryModuleHelper.keepAlive("Refill");

    if (this.currentAction == null && !this.finished) {
      this.currentAction = getNextAction();
      if (this.currentAction == null) {
        this.finished = true;
        long closeDelay = randomInt((int) this.closeDelayMin.getValue(), (int) this.closeDelayMax.getValue());
        this.closeTime = System.currentTimeMillis() + closeDelay;
        InventoryModuleHelper.release("Refill");
        return;
      }
      this.startPos = new Point(this.currentPos.x, this.currentPos.y);
      Slot slot = this.currentAction.slot;
      int targetX = (gui.width - 176) / 2 + slot.xDisplayPosition + 8;
      int targetY = (gui.height - 166) / 2 + slot.yDisplayPosition + 8;
      targetX += randomInt(-3, 3);
      targetY += randomInt(-3, 3);
      this.endPos = new Point(targetX, targetY);
      float dist = (float) Math.sqrt(Math.pow(this.endPos.x - this.startPos.x, 2.0D) + Math.pow(this.endPos.y - this.startPos.y, 2.0D));
      float curve = (float) this.curveFactor.getValue();
      float offset = curve > 0 ? dist / curve : 0;
      this.controlPos = new Point((this.startPos.x + this.endPos.x) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset), (this.startPos.y + this.endPos.y) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset));
      this.movementStartTime = System.currentTimeMillis();
      this.movementDuration = (long) (dist / (float) this.mouseSpeed.getValue() * 50.0F);
      this.movementDuration = Math.max(this.movementDuration, 1L);
    }

    if (this.currentAction != null) {
      long elapsed = System.currentTimeMillis() - this.movementStartTime;
      float progress = Math.min(1.0F, (float) elapsed / (float) this.movementDuration);
      float t = easeOutQuart(progress);
      double oneMinusT = (1.0F - t);
      double curX = oneMinusT * oneMinusT * this.startPos.x + 2.0D * oneMinusT * t * this.controlPos.x + (t * t) * this.endPos.x;
      double curY = oneMinusT * oneMinusT * this.startPos.y + 2.0D * oneMinusT * t * this.controlPos.y + (t * t) * this.endPos.y;
      if (progress < 1.0F) {
        curX += (this.random.nextFloat() - 0.5F) * this.jitterStrength.getValue();
        curY += (this.random.nextFloat() - 0.5F) * this.jitterStrength.getValue();
      }
      this.currentPos.x = curX;
      this.currentPos.y = curY;
      if (progress >= 1.0F) {
        performAction(this.currentAction);
        this.didWork = true;
        this.currentAction = null;
        long actionDelay = randomInt((int) this.actionDelayMin.getValue(), (int) this.actionDelayMax.getValue());
        this.nextActionTime = System.currentTimeMillis() + actionDelay;
      }
    }

    if (this.finished && this.didWork && (this.autoClose.isEnabled() || forceRefillActive) && System.currentTimeMillis() >= this.closeTime) {
      mc.thePlayer.closeScreen();
      forceRefillActive = false;
      forceOpenedInventory = false;
    }

    if (this.finished && !this.didWork) {
      forceRefillActive = false;
      forceOpenedInventory = false;
    }
  }

  @Override
  public void onUpdate() {
    if (!enabled || mc == null || mc.thePlayer == null) return;

    String selectedMode = mode.getSelected();

    checkForceRefillKey();

    if (selectedMode.equals("Legit")) {
      if (forceRefillActive && mc.currentScreen == null && !forceOpenedInventory) {
        mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        forceOpenedInventory = true;
      }
      return;
    }

    EntityPlayer player = mc.thePlayer;
    boolean isInventoryOpen = mc.currentScreen instanceof GuiInventory;

    if (mc.currentScreen != null && !isInventoryOpen) return;

    boolean shouldRefill = alwaysRefill.isEnabled() || forceRefillActive;

    if (selectedMode.equals("Instant")) {
      if (forceRefillActive && !isInventoryOpen) {
        mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        isInventoryOpen = true;
        forceOpenedInventory = true;
      }

      if (isInventoryOpen && !isHotbarFull(player) && shouldRefill) {
        openServerInventory();
        for (int slot = 9; slot < 36; slot++) {
          if (!hasHotbarSpace(player)) break;
          ItemStack stack = player.inventoryContainer.getSlot(slot).getStack();
          if (stack != null && isHealItem(stack)) {
            sendShiftClick(player, slot, stack);
          }
        }
        closeServerInventory();
        if (autoClose.isEnabled() || forceRefillActive) {
          mc.displayGuiScreen(null);
        }
        forceRefillActive = false;
        forceOpenedInventory = false;
      }
      return;
    }

    if (selectedMode.equals("Spoof")) {
      if (forceRefillActive && !isInventoryOpen) {
        mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
        isInventoryOpen = true;
        forceOpenedInventory = true;
      }

      if (slotsToRefill.isEmpty()) {
        if (isInventoryOpen && !isHotbarFull(player) && shouldRefill) {
          startSpoofRefill(player);
        }
        return;
      }

      if (currentIndex >= slotsToRefill.size()) {
        finishSpoofRefill(isInventoryOpen);
        return;
      }

      if (!hasHotbarSpace(player)) {
        finishSpoofRefill(isInventoryOpen);
        return;
      }

      long now = System.currentTimeMillis();
      int delayMs = (int) spoofMinDelay.getValue() + random.nextInt(Math.max(1, (int) (spoofMaxDelay.getValue() - spoofMinDelay.getValue())));
      if (now - lastClickTime < delayMs) return;

      int slot = slotsToRefill.get(currentIndex);
      ItemStack stack = player.inventoryContainer.getSlot(slot).getStack();

      if (stack != null && isHealItem(stack)) {
        if (!serverInventoryOpen) openServerInventory();
        sendShiftClick(player, slot, stack);
        lastClickTime = now;
      }
      currentIndex++;
    }
  }

  private void checkForceRefillKey() {
    if (forceRefillKey.getKeyCode() == 0) return;

    boolean isPressed = forceRefillKey.isKeyPressed();

    if (isPressed && !forceRefillKeyWasPressed && !forceRefillActive) {
      forceRefillActive = true;
      forceOpenedInventory = false;
    }

    forceRefillKeyWasPressed = isPressed;
  }

  private void performAction(Action action) {
    int windowId = mc.thePlayer.inventoryContainer.windowId;
    int slotId = action.slot.slotNumber;
    mc.playerController.windowClick(windowId, slotId, 0, 1, (EntityPlayer) mc.thePlayer);
  }

  private Action getNextAction() {
    EntityPlayer player = mc.thePlayer;
    if (isHotbarFull(player)) return null;

    List<Integer> slots = new ArrayList<>();
    for (int slot = 9; slot < 36; slot++) {
      ItemStack stack = player.inventoryContainer.getSlot(slot).getStack();
      if (stack != null && isHealItem(stack)) {
        slots.add(slot);
      }
    }

    if (slots.isEmpty()) return null;

    int slot;
    if (order.getSelected().equals("Random")) {
      slot = slots.get(random.nextInt(slots.size()));
    } else {
      slot = slots.get(0);
    }

    return new Action(mc.thePlayer.inventoryContainer.getSlot(slot));
  }

  private void startSpoofRefill(EntityPlayer player) {
    slotsToRefill.clear();
    currentIndex = 0;

    List<Integer> temp = new ArrayList<>();
    for (int slot = 9; slot < 36; slot++) {
      ItemStack stack = player.inventoryContainer.getSlot(slot).getStack();
      if (stack != null && isHealItem(stack)) {
        temp.add(slot);
      }
    }

    if (order.getSelected().equals("Random")) {
      Collections.shuffle(temp);
    }

    slotsToRefill.addAll(temp);
    lastClickTime = 0;

    if (!slotsToRefill.isEmpty()) {
      openServerInventory();
    }
  }

  private void finishSpoofRefill(boolean isInventoryOpen) {
    closeServerInventory();
    if (autoClose.isEnabled() || forceRefillActive) {
      if (isInventoryOpen) {
        mc.displayGuiScreen(null);
      }
    }
    slotsToRefill.clear();
    currentIndex = 0;
    forceRefillActive = false;
    forceOpenedInventory = false;
  }

  private void sendShiftClick(EntityPlayer player, int slot, ItemStack stack) {
    try {
      C0EPacketClickWindow clickPacket = new C0EPacketClickWindow(
          player.openContainer.windowId,
          slot,
          0,
          1,
          stack,
          player.openContainer.getNextTransactionID(player.inventory)
      );
      mc.getNetHandler().addToSendQueue(clickPacket);
    } catch (Exception e) {
    }
  }

  private void openServerInventory() {
    if (serverInventoryOpen) return;
    try {
      mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
      serverInventoryOpen = true;
    } catch (Exception e) {
    }
  }

  private void closeServerInventory() {
    if (!serverInventoryOpen) return;
    try {
      if (mc.thePlayer != null) {
        mc.getNetHandler().addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.openContainer.windowId));
      }
      serverInventoryOpen = false;
    } catch (Exception e) {
    }
  }

  private boolean isHotbarFull(EntityPlayer player) {
    for (int i = 36; i < 45; i++) {
      ItemStack stack = player.inventoryContainer.getSlot(i).getStack();
      if (stack == null) return false;
    }
    return true;
  }

  private boolean hasHotbarSpace(EntityPlayer player) {
    return !isHotbarFull(player);
  }

  private boolean isHealItem(ItemStack stack) {
    try {
      int itemId = stack.getItem().getIdFromItem(stack.getItem());
      int damage = stack.getMetadata();

      String selectedType = type.getSelected();

      boolean isHealthPot = itemId == 373 && (damage == 16453 || damage == 16421);
      boolean isSoup = itemId == 282;

      if (selectedType.equals("Both")) {
        return isHealthPot || isSoup;
      } else if (selectedType.equals("Pots")) {
        return isHealthPot;
      } else if (selectedType.equals("Soup")) {
        return isSoup;
      }
    } catch (Exception e) {
    }
    return false;
  }

  private float easeOutQuart(float x) {
    return 1.0F - (float) Math.pow((1.0F - x), 4.0D);
  }

  private void resetState() {
    this.openTimestamp = 0L;
    this.currentAction = null;
    this.active = false;
    this.finished = false;
    this.closeTime = 0L;
    this.didWork = false;
  }

  private void reset() {
    resetState();
    this.slotsToRefill.clear();
    this.currentIndex = 0;
    this.lastClickTime = 0;
    this.forceRefillActive = false;
    this.forceOpenedInventory = false;
  }

  private int randomInt(int min, int max) {
    if (min >= max) return min;
    return min + random.nextInt(max - min + 1);
  }

  @Override
  public String getDisplaySuffix() {
    return " §7" + mode.getSelected();
  }

  private static class Point {
    double x;
    double y;

    Point(double x, double y) {
      this.x = x;
      this.y = y;
    }
  }

  private static class Action {
    Slot slot;

    Action(Slot slot) {
      this.slot = slot;
    }
  }
}
