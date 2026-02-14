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
import io.github.exodar.setting.ModeSetting;
import io.github.exodar.setting.SliderSetting;
import io.github.exodar.setting.TickSetting;
import java.util.Random;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Mouse;

public class AutoArmor extends Module {
  private final TickSetting debug;
  private final ModeSetting mode;
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
  private final TickSetting respectKit;

  private static final int[] ARMOR_SLOTS = {5, 6, 7, 8};

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
  private boolean didWork;
  private int[] kitMaterials = new int[4];
  private boolean spoofedOpen;

  public AutoArmor() {
    super("AutoArmor", ModuleCategory.PLAYER);
    this.registerSetting(debug = new TickSetting("Debug", false));
    this.registerSetting(mode = new ModeSetting("Mode", new String[]{"Legit", "Spoof"})
        .onChange(this::updateSettingsVisibility));
    this.registerSetting(startDelayMin = new SliderSetting("Start Delay Min", 100.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(startDelayMax = new SliderSetting("Start Delay Max", 200.0D, 0.0D, 1000.0D, 10.0D));
    this.registerSetting(actionDelayMin = new SliderSetting("Action Delay Min", 80.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(actionDelayMax = new SliderSetting("Action Delay Max", 150.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(mouseSpeed = new SliderSetting("Mouse Speed", 20.0D, 5.0D, 60.0D, 1.0D));
    this.registerSetting(curveFactor = new SliderSetting("Curve Factor", 2.5D, 0.1D, 5.0D, 0.1D));
    this.registerSetting(jitterStrength = new SliderSetting("Jitter", 0.5D, 0.0D, 2.0D, 0.1D));
    this.registerSetting(blockMouse = new TickSetting("Block Mouse", true));
    this.registerSetting(autoClose = new TickSetting("Auto Close", false));
    this.registerSetting(closeDelayMin = new SliderSetting("Close Delay Min", 100.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(closeDelayMax = new SliderSetting("Close Delay Max", 200.0D, 0.0D, 500.0D, 10.0D));
    this.registerSetting(respectKit = new TickSetting("Respect Kit", false));
    updateSettingsVisibility();
  }

  private void updateSettingsVisibility() {
    boolean isLegit = mode.getSelected().equals("Legit");
    mouseSpeed.setVisible(isLegit);
    curveFactor.setVisible(isLegit);
    jitterStrength.setVisible(isLegit);
    blockMouse.setVisible(isLegit);
  }

  private void debug(String msg) {
    if (debug.isEnabled() && mc.thePlayer != null) {
      mc.thePlayer.addChatMessage(new ChatComponentText("§d[AutoArmor] §f" + msg));
    }
  }

  @Override
  public void onEnable() {
    reset();
    if (respectKit.isEnabled()) captureKitMaterials();
  }

  @Override
  public void onDisable() {
    if (spoofedOpen) closeSpoofedInventory();
    InventoryModuleHelper.release("AutoArmor");
    reset();
  }

  @Subscribe
  public void onRender(Render2DEvent event) {
    if (!enabled || mc.thePlayer == null) return;

    if (mode.getSelected().equals("Spoof")) {
      handleSpoofMode();
      return;
    }

    if (!(mc.currentScreen instanceof GuiInventory)) {
      if (active || currentAction != null) {
        InventoryModuleHelper.release("AutoArmor");
      }
      reset();
      return;
    }

    if (!InventoryModuleHelper.tryAcquire("AutoArmor")) return;

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
    InventoryModuleHelper.keepAlive("AutoArmor");

    if (this.currentAction == null && !this.finished) {
      this.currentAction = getNextAction();
      if (this.currentAction == null) {
        this.finished = true;
        long closeDelay = randomInt((int) this.closeDelayMin.getValue(), (int) this.closeDelayMax.getValue());
        this.closeTime = System.currentTimeMillis() + closeDelay;
        InventoryModuleHelper.release("AutoArmor");
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
      float offset = dist / (float) this.curveFactor.getValue();
      this.controlPos = new Point((this.startPos.x + this.endPos.x) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset), (this.startPos.y + this.endPos.y) / 2.0D + ((this.random.nextFloat() - 0.5F) * offset));
      this.movementStartTime = System.currentTimeMillis();
      this.movementDuration = (long) (dist / (float) this.mouseSpeed.getValue() * 50.0F);
      this.movementDuration = Math.max(this.movementDuration, 100L);
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

    if (this.finished && this.didWork && this.autoClose.isEnabled() && System.currentTimeMillis() >= this.closeTime) {
      mc.thePlayer.closeScreen();
    }
  }

  private void handleSpoofMode() {
    if (mc.currentScreen != null) return;
    if (!InventoryModuleHelper.tryAcquire("AutoArmor")) return;

    Action action = getNextAction();
    if (action == null) {
      InventoryModuleHelper.release("AutoArmor");
      return;
    }

    if (!spoofedOpen) {
      mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
      spoofedOpen = true;
    }

    performAction(action);

    action = getNextAction();
    if (action == null) {
      closeSpoofedInventory();
      InventoryModuleHelper.release("AutoArmor");
    }
  }

  private void closeSpoofedInventory() {
    if (spoofedOpen && mc.thePlayer != null) {
      mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
      spoofedOpen = false;
    }
  }

  private void performAction(Action action) {
    int windowId = mc.thePlayer.inventoryContainer.windowId;
    int slotId = action.slot.slotNumber;
    ItemStack stack = action.slot.getStack();
    debug("§aEquip: " + (stack != null ? stack.getDisplayName() : "null") + " slot=" + slotId);
    mc.playerController.windowClick(windowId, slotId, 0, 1, (EntityPlayer) mc.thePlayer);
  }

  private Action getNextAction() {
    for (int armorType = 0; armorType < 4; armorType++) {
      int armorSlot = ARMOR_SLOTS[armorType];
      ItemStack currentArmor = mc.thePlayer.inventoryContainer.getSlot(armorSlot).getStack();
      double currentValue = currentArmor != null ? getArmorValue(currentArmor) : -1;

      int requiredMaterial = -1;
      if (respectKit.isEnabled()) {
        if (currentArmor != null && currentArmor.getItem() instanceof ItemArmor) {
          requiredMaterial = getArmorMaterialId((ItemArmor) currentArmor.getItem());
        } else if (kitMaterials[armorType] != -1) {
          requiredMaterial = kitMaterials[armorType];
        }
      }

      int bestSlot = -1;
      double bestValue = currentValue;

      for (int i = 9; i < 45; i++) {
        ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
        if (stack != null && stack.getItem() instanceof ItemArmor) {
          ItemArmor armor = (ItemArmor) stack.getItem();
          if (armor.armorType == armorType) {
            if (respectKit.isEnabled() && requiredMaterial != -1) {
              int stackMaterial = getArmorMaterialId(armor);
              if (stackMaterial != requiredMaterial) continue;
            }
            double value = getArmorValue(stack);
            if (value > bestValue) {
              bestValue = value;
              bestSlot = i;
            }
          }
        }
      }

      if (bestSlot != -1) {
        // If there's armor in the slot, we need to unequip it first
        if (currentArmor != null) {
          // Check if there's space in inventory for current armor
          boolean hasSpace = false;
          for (int i = 9; i < 45; i++) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() == null) {
              hasSpace = true;
              break;
            }
          }
          if (hasSpace) {
            debug("§eUnequip first: " + currentArmor.getDisplayName() + " from slot=" + armorSlot);
            return new Action(mc.thePlayer.inventoryContainer.getSlot(armorSlot));
          } else {
            debug("§cNo space to unequip armor");
            continue; // Skip this armor type, try next
          }
        }

        ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(bestSlot).getStack();
        debug("§bFound better armor: " + (stack != null ? stack.getDisplayName() : "null") + " slot=" + bestSlot + " value=" + String.format("%.2f", bestValue));
        return new Action(mc.thePlayer.inventoryContainer.getSlot(bestSlot));
      }
    }
    debug("§aNo better armor found");
    return null;
  }

  private double getArmorValue(ItemStack stack) {
    if (stack == null || !(stack.getItem() instanceof ItemArmor)) return -1;
    ItemArmor armor = (ItemArmor) stack.getItem();
    double value = armor.damageReduceAmount;
    value += EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) * 0.5;
    value += Math.max(
        EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack),
        Math.max(
            EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack),
            EnchantmentHelper.getEnchantmentLevel(Enchantment.projectileProtection.effectId, stack)
        )
    ) * 0.25;
    value += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.1;
    return value;
  }

  private void captureKitMaterials() {
    if (mc.thePlayer == null) return;
    for (int i = 0; i < 4; i++) {
      ItemStack armor = mc.thePlayer.inventory.armorInventory[3 - i];
      if (armor != null && armor.getItem() instanceof ItemArmor) {
        kitMaterials[i] = getArmorMaterialId((ItemArmor) armor.getItem());
      } else {
        kitMaterials[i] = -1;
      }
    }
  }

  private int getArmorMaterialId(ItemArmor armor) {
    String material = armor.getArmorMaterial().name().toLowerCase();
    if (material.contains("leather")) return 0;
    if (material.contains("chain")) return 1;
    if (material.contains("iron")) return 2;
    if (material.contains("gold")) return 3;
    if (material.contains("diamond")) return 4;
    return -1;
  }

  private float easeOutQuart(float x) {
    return 1.0F - (float) Math.pow((1.0F - x), 4.0D);
  }

  private void reset() {
    this.openTimestamp = 0L;
    this.currentAction = null;
    this.active = false;
    this.finished = false;
    this.closeTime = 0L;
  }

  private int randomInt(int min, int max) {
    if (min >= max) return min;
    return min + random.nextInt(max - min + 1);
  }

  @Override
  public String getDisplaySuffix() {
    String suffix = mode.getSelected();
    if (respectKit.isEnabled()) suffix += " Kit";
    return " §7" + suffix;
  }

  private static class Point {
    double x, y;
    Point(double x, double y) { this.x = x; this.y = y; }
  }

  private static class Action {
    Slot slot;
    Action(Slot slot) { this.slot = slot; }
  }
}
